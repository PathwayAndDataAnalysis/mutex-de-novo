package org.panda.mutexdenovo;

import org.panda.utility.FileUtil;
import org.panda.utility.Progress;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tests if the given gene sets are mutually exclusive or co-occurred in the given matrix.
 */
public class MutexTester
{
	/**
	 * Alteration matrix.
	 */
	private Matrix matrix;

	/**
	 * Gene sets to be tested for mutual exclusivity and co-occurrence.
	 */
	private Map<String, Set<String>> geneSets;

	/**
	 * The output directory where the result will be written.
	 */
	private String outDir;

	/**
	 * Number of randomizations for testing. Run time is proportional to this number.
	 */
	private int iteration;

	public MutexTester(Matrix matrix, Map<String, Set<String>> geneSets, String outDir, int iteration)
	{
		this.matrix = matrix;
		this.geneSets = geneSets;
		this.outDir = outDir;
		this.iteration = iteration;
	}

	public void run() throws IOException
	{
		// Generate output directories if do not exist
		FileUtil.mkdirs(outDir);

		// Record current coverages and overlaps

		Map<String, Integer> currentCovMap = new HashMap<>();
		Map<String, Integer> currentOvMap = new HashMap<>();

		geneSets.forEach((id, genes) ->
		{
			currentCovMap.put(id, matrix.countCoverage(genes));
			currentOvMap.put(id, matrix.countOverlap(genes));
		});

		// Calculate mutex and cooc p-values by shuffling
		Map<String, Double>[] pvals = getMutexCoocPvals();

		// Write results as a list

		BufferedWriter writer = FileUtil.newBufferedWriter(outDir + "/results.txt");

		writer.write("ID\tGenes size\tCoverage\tOverlap\tMutex p-value\tCooc p-value");
		geneSets.keySet().stream().sorted(Comparator.comparing(n -> pvals[0].get(n))).forEach(id -> FileUtil.lnwrite(
			id + "\t" + geneSets.get(id).size() + "\t" + currentCovMap.get(id) + "\t" +
				currentOvMap.get(id) + "\t" + pvals[0].get(id) + "\t" + pvals[1].get(id), writer));

		writer.close();
	}

	/**
	 * Calculates mutex and cooc p-values by shuffling the matrix iteratively. Also generates individual p-values for
	 * members of each gene set in the given output directory.
	 *
	 * @return p-values
	 * @throws IOException if cannot write to the output directory
	 */
	private Map<String, Double>[] getMutexCoocPvals() throws IOException
	{
		// Get edge representation of the matrix
		List<Matrix.Edge> edges = matrix.getEdges();

		// Initiate current coverage maps
		Map<String, Map<Integer, Long>> coverageMap = geneSets.keySet().stream().collect(
			Collectors.toMap(Function.identity(), name -> getSampleHitCounts(edges, geneSets.get(name))));

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
		Map<String, Map<String, Set<Integer>>> origGeneToIndices = geneSets.keySet().stream().collect(Collectors.toMap(Function.identity(),
			name -> getGeneToIndices(edges, geneSets.get(name))));

		// Initiate gene-specific current sample hits
		Map<String, Map<String, Long>> geneSampleHitMaps = geneSets.keySet().stream().collect(Collectors.toMap(Function.identity(),
			name -> geneSets.get(name).stream().collect(Collectors.toMap(Function.identity(),
				gene -> getSampleHitsForGeneInGroup(origGeneToIndices.get(name), gene, coverageMap.get(name))))));

		Shuffler shuffler = new Shuffler(matrix);

		// Start shuffling and recording
		Progress prg = new Progress(iteration, "Shuffling the matrix " + iteration + " times");
		for (int i = 0; i < iteration; i++)
		{
			shuffler.shuffle();

			coverageMap.forEach((name, origCov) ->
			{
				Map<Integer, Long> sampleHitCounts = getSampleHitCounts(edges, geneSets.get(name));
				int cov = sampleHitCounts.size();
				if (cov >= origCov.size()) mutexMeetMap.put(name, mutexMeetMap.get(name) + 1);
				if (cov <= origCov.size()) coocMeetMap.put(name, coocMeetMap.get(name) + 1);

				Map<String, Set<Integer>> geneToIndices = getGeneToIndices(edges, geneSets.get(name));

				updateGeneMutexMeetMap(geneToIndices, sampleHitCounts, geneMutexMeetMaps.get(name), geneSampleHitMaps.get(name));
				updateGeneCoocMeetMap(geneToIndices, sampleHitCounts, geneCoocMeetMaps.get(name), geneSampleHitMaps.get(name));
			});
			prg.tick();
		}

		// Calculate gene p-values
		Map<String, Map<String, Double>> geneMutexPvalMaps = geneSets.keySet().stream().collect(Collectors.toMap(Function.identity(),
			name -> geneSets.get(name).stream().collect(Collectors.toMap(Function.identity(),
				gene -> geneMutexMeetMaps.get(name).get(gene) / (double) iteration))));
		Map<String, Map<String, Double>> geneCoocPvalMaps = geneSets.keySet().stream().collect(Collectors.toMap(Function.identity(),
			name -> geneSets.get(name).stream().collect(Collectors.toMap(Function.identity(),
				gene -> geneCoocMeetMaps.get(name).get(gene) / (double) iteration))));

		// Write gene p-values
		for (String name : geneSets.keySet())
		{
			BufferedWriter writer1 = Files.newBufferedWriter(Paths.get(outDir + "/" + name + "-mutex.txt"));
			geneMutexPvalMaps.get(name).keySet().stream().sorted(Comparator.comparing(geneMutexPvalMaps.get(name)::get))
				.forEach(gene -> FileUtil.writeln(gene + "\t" + geneMutexPvalMaps.get(name).get(gene), writer1));
			writer1.close();

			BufferedWriter writer2 = Files.newBufferedWriter(Paths.get(outDir + "/" + name + "-cooc.txt"));
			geneCoocPvalMaps.get(name).keySet().stream().sorted(Comparator.comparing(geneCoocPvalMaps.get(name)::get))
				.forEach(gene -> FileUtil.writeln(gene + "\t" + geneCoocPvalMaps.get(name).get(gene), writer2));
			writer2.close();
		}

		// Calculate and return group p-values
		return new Map[]{
			mutexMeetMap.keySet().stream().collect(Collectors.toMap(
				Function.identity(), name -> mutexMeetMap.get(name) / (double) iteration)),
			coocMeetMap.keySet().stream().collect(Collectors.toMap(
				Function.identity(), name -> coocMeetMap.get(name) / (double) iteration))
		};
	}

	private Map<Integer, Long> getSampleHitCounts(List<Matrix.Edge> edges, Set<String> genes)
	{
		return edges.stream().filter(e -> genes.contains(e.gene)).map(e -> e.sampleIndex)
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}

	private Map<String, Set<Integer>> getGeneToIndices(List<Matrix.Edge> edges, Set<String> genes)
	{
		Map<String, Set<Integer>> map = new HashMap<>();
		edges.stream().filter(e -> genes.contains(e.gene)).forEach(edge ->
		{
			if (!map.containsKey(edge.gene)) map.put(edge.gene, new HashSet<>());
			map.get(edge.gene).add(edge.sampleIndex);
		});
		return map;
	}

	private long getSampleHitsForGeneInGroup(Map<String, Set<Integer>> geneToInds, String gene,
		Map<Integer, Long> sampleHitCounts)
	{
		return geneToInds.get(gene).stream().map(sampleHitCounts::get).reduce((h1, h2) -> h1 + h2).get();
	}

	private void updateGeneMutexMeetMap(Map<String, Set<Integer>> geneToIndices, Map<Integer, Long> sampleHitCounts,
		Map<String, Integer> meetMap, Map<String, Long> origGeneSampleHits)
	{
		geneToIndices.keySet().forEach(gene ->
		{
			long hit = getSampleHitsForGeneInGroup(geneToIndices, gene, sampleHitCounts);
			if (hit <= origGeneSampleHits.get(gene)) meetMap.put(gene, meetMap.get(gene) + 1);
		});
	}

	private void updateGeneCoocMeetMap(Map<String, Set<Integer>> geneToIndices, Map<Integer, Long> sampleHitCounts,
		Map<String, Integer> meetMap, Map<String, Long> origGeneSampleHits)
	{
		geneToIndices.keySet().forEach(gene ->
		{
			long hit = getSampleHitsForGeneInGroup(geneToIndices, gene, sampleHitCounts);
			if (hit >= origGeneSampleHits.get(gene)) meetMap.put(gene, meetMap.get(gene) + 1);
		});
	}

}
