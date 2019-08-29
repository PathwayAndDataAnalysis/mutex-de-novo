package org.panda.mutexdenovo;

import org.panda.utility.FileUtil;
import org.panda.utility.Progress;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tests if the given gene sets are differentially mutually exclusive between the given two matrices.
 */
public class DifferentialMutexTester extends MutexTester
{
	/**
	 * Second alteration matrix used as control.
	 */
	private Matrix ctrlMatrix;

	public DifferentialMutexTester(Matrix testMatrix, Matrix ctrlMatrix, Map<String, Set<String>> geneSets,
		String outDir, int iteration)
	{
		super(testMatrix, geneSets, outDir, iteration);
		this.ctrlMatrix = ctrlMatrix;
	}

	public void run() throws IOException
	{
		// Generate output directories if do not exist
		FileUtil.mkdirs(outDir);

		// Record current coverages and overlaps

		Map<String, Integer> currentTestCovMap = new HashMap<>();
		Map<String, Integer> currentCtrlCovMap = new HashMap<>();
		Map<String, Integer> currentTestOvMap = new HashMap<>();
		Map<String, Integer> currentCtrlOvMap = new HashMap<>();

		geneSets.forEach((id, genes) ->
		{
			currentTestCovMap.put(id, matrix.countCoverage(genes));
			currentCtrlCovMap.put(id, ctrlMatrix.countCoverage(genes));
			currentTestOvMap.put(id, matrix.countOverlap(genes));
			currentCtrlOvMap.put(id, ctrlMatrix.countOverlap(genes));
		});

		// Calculate differential mutex and cooc p-values by shuffling
		Map<String, Double>[] pvals = getMutexCoocPvals();

		// Write results as a list

		BufferedWriter writer = FileUtil.newBufferedWriter(outDir + "/results.txt");

		writer.write("ID\tGenes size\tCoverage Test\tCoverage Ctrl\tOverlap Test\tOverlap Ctrl\tDifferential mutex p-value\tDifferential cooc p-value");
		geneSets.keySet().stream().sorted(Comparator.comparing(n -> pvals[0].get(n))).forEach(id -> FileUtil.lnwrite(
			id + "\t" + geneSets.get(id).size() + "\t" + currentTestCovMap.get(id) + "\t" + currentCtrlCovMap.get(id) + "\t" +
				currentTestOvMap.get(id) + "\t" + currentCtrlOvMap.get(id) + "\t" + pvals[0].get(id) + "\t" + pvals[1].get(id), writer));

		writer.close();
	}

	/**
	 * Calculates differential mutex and cooc p-values by shuffling the matrices iteratively. Also generates individual
	 * p-values for members of each gene set in the given output directory.
	 *
	 * @return p-values
	 * @throws IOException if cannot write to the output directory
	 */
	protected Map<String, Double>[] getMutexCoocPvals() throws IOException
	{
		// Get edge representation of the matrices
		List<Matrix.Edge> edgesTest = matrix.getEdges();
		List<Matrix.Edge> edgesCtrl = ctrlMatrix.getEdges();

		// Initiate current coverage maps
		Map<String, Map<Integer, Long>> testCoverageMap = geneSets.keySet().stream().collect(
			Collectors.toMap(Function.identity(), name -> getSampleHitCounts(edgesTest, geneSets.get(name))));

		Map<String, Map<Integer, Long>> ctrlCoverageMap = geneSets.keySet().stream().collect(
			Collectors.toMap(Function.identity(), name -> getSampleHitCounts(edgesCtrl, geneSets.get(name))));

		// Initiate group meet maps

		Map<String, Integer> mutexMeetMap = geneSets.keySet().stream().collect(
			Collectors.toMap(Function.identity(), name -> 0));

		Map<String, Integer> coocMeetMap = geneSets.keySet().stream().collect(
			Collectors.toMap(Function.identity(), name -> 0));

		// Initiate gene-specific meet maps

		Map<String, Map<String, Integer>> geneMutexMeetMaps = geneSets.keySet().stream().collect(Collectors.toMap(Function.identity(),
			name -> geneSets.get(name).stream().collect(Collectors.toMap(Function.identity(), gene -> 0))));

		Map<String, Map<String, Integer>> geneCoocMeetMaps = geneSets.keySet().stream().collect(Collectors.toMap(Function.identity(),
			name -> geneSets.get(name).stream().collect(Collectors.toMap(Function.identity(), gene -> 0))));

		// Get current gene to sample indices

		Map<String, Map<String, Set<Integer>>> origGeneToIndicesTest = geneSets.keySet().stream().collect(Collectors.toMap(Function.identity(),
			name -> getGeneToIndices(edgesTest, geneSets.get(name))));

		Map<String, Map<String, Set<Integer>>> origGeneToIndicesCtrl = geneSets.keySet().stream().collect(Collectors.toMap(Function.identity(),
			name -> getGeneToIndices(edgesCtrl, geneSets.get(name))));

		// Initiate gene-specific current sample hits

		Map<String, Map<String, Long>> geneSampleHitMaps = geneSets.keySet().stream().collect(
			Collectors.toMap(Function.identity(), name -> geneSets.get(name).stream().collect(Collectors.toMap(
				Function.identity(), gene ->
					getSampleHitsForGeneInGroup(origGeneToIndicesTest.get(name), gene, testCoverageMap.get(name)) -
						getSampleHitsForGeneInGroup(origGeneToIndicesCtrl.get(name), gene, ctrlCoverageMap.get(name))))));

		Shuffler shufflerTest = new Shuffler(matrix);
		Shuffler shufflerCtrl = new Shuffler(ctrlMatrix);

		// Start shuffling and recording
		Progress prg = new Progress(iteration, "Shuffling the matrices " + iteration + " times");
		for (int i = 0; i < iteration; i++)
		{
			shufflerTest.shuffle();
			shufflerCtrl.shuffle();

			testCoverageMap.forEach((name, origTestCov) ->
			{
				Map<Integer, Long> origCtrlCov = ctrlCoverageMap.get(name);
				int origCov = origTestCov.size() - origCtrlCov.size();

				Map<Integer, Long> sampleHitCountsTest = getSampleHitCounts(edgesTest, geneSets.get(name));
				Map<Integer, Long> sampleHitCountsCtrl = getSampleHitCounts(edgesCtrl, geneSets.get(name));

				int cov = sampleHitCountsTest.size() - sampleHitCountsCtrl.size();

				if (cov >= origCov) mutexMeetMap.put(name, mutexMeetMap.get(name) + 1);
				if (cov <= origCov) coocMeetMap.put(name, coocMeetMap.get(name) + 1);

				Map<String, Set<Integer>> geneToIndicesTest = getGeneToIndices(edgesTest, geneSets.get(name));
				Map<String, Set<Integer>> geneToIndicesCtrl = getGeneToIndices(edgesCtrl, geneSets.get(name));

				updateGeneMutexMeetMap(geneToIndicesTest, geneToIndicesCtrl, sampleHitCountsTest, sampleHitCountsCtrl, geneMutexMeetMaps.get(name), geneSampleHitMaps.get(name));
				updateGeneCoocMeetMap(geneToIndicesTest, geneToIndicesCtrl, sampleHitCountsTest, sampleHitCountsCtrl, geneCoocMeetMaps.get(name), geneSampleHitMaps.get(name));
			});
			prg.tick();
		}

		return calculateAndWritePvalues(mutexMeetMap, coocMeetMap, geneMutexMeetMaps, geneCoocMeetMaps);
	}

	private void updateGeneMutexMeetMap(
		Map<String, Set<Integer>> geneToIndicesTest, Map<String, Set<Integer>> geneToIndicesCtrl,
		Map<Integer, Long> sampleHitCountsTest, Map<Integer, Long> sampleHitCountsCtrl,
		Map<String, Integer> meetMap, Map<String, Long> origGeneSampleHits)
	{
		geneToIndicesTest.keySet().forEach(gene ->
		{
			long hit = getSampleHitsForGeneInGroup(geneToIndicesTest, gene, sampleHitCountsTest) - getSampleHitsForGeneInGroup(geneToIndicesCtrl, gene, sampleHitCountsCtrl);
			if (hit <= origGeneSampleHits.get(gene)) meetMap.put(gene, meetMap.get(gene) + 1);
		});
	}

	private void updateGeneCoocMeetMap(
		Map<String, Set<Integer>> geneToIndicesTest, Map<String, Set<Integer>> geneToIndicesCtrl,
		Map<Integer, Long> sampleHitCountsTest, Map<Integer, Long> sampleHitCountsCtrl,
		Map<String, Integer> meetMap, Map<String, Long> origGeneSampleHits)
	{
		geneToIndicesTest.keySet().forEach(gene ->
		{
			long hit = getSampleHitsForGeneInGroup(geneToIndicesTest, gene, sampleHitCountsTest) - getSampleHitsForGeneInGroup(geneToIndicesCtrl, gene, sampleHitCountsCtrl);
			if (hit >= origGeneSampleHits.get(gene)) meetMap.put(gene, meetMap.get(gene) + 1);
		});
	}

}
