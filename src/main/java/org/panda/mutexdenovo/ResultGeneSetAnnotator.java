package org.panda.mutexdenovo;

import org.panda.resource.SFARI;
import org.panda.utility.ArrayUtil;
import org.panda.utility.CollectionUtil;
import org.panda.utility.FileUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads a gene set result (member p-values), prepares a table by adding in other information about the genes in the
 * context.
 */
public class ResultGeneSetAnnotator
{
	public static void annotate(String inFile, Matrix matrix, String outFile) throws IOException
	{
		// Read p-values
		Map<String, Double> pvals = Files.lines(Paths.get(inFile)).map(l -> l.split("\t"))
			.collect(Collectors.toMap(t -> t[0], t -> Double.valueOf(t[1])));

		// Read the ordered list
		List<String> geneList = Files.lines(Paths.get(inFile)).map(l -> l.split("\t")[0])
			.collect(Collectors.toList());

		// Check if the matrix contains all the genes in the file. It has to.
		if (!matrix.hasAllGenes(geneList))
		{
			throw new RuntimeException("Some genes are missing in the matrix. The given result file cannot be " +
				"generated from the given matrix.");
		}

		// Get individual overlap
		Map<String, Map<String, Integer>> overlapMapPairwise = matrix.countOverlapPairwise(new HashSet<>(geneList));

		// Calculate collective overlap map for each gene
		Map<String, Integer> overlapMapCollective = overlapMapPairwise.keySet().stream().collect(Collectors.toMap(
			Function.identity(), gene -> overlapMapPairwise.get(gene).values().stream().reduce((o1, o2) -> o1 + o2).get()));

		// Get the coverages
		Map<String, Integer> coverageMap = matrix.countIndividualCoverage(pvals.keySet());

		// Write output

		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile));
		writer.write("Rank\tGene\tMut#\tOv\tP-val\tSpecific overlaps");

		geneList.forEach(gene -> FileUtil.lnwrite(ArrayUtil.getString("\t",
			SFARI.get().getClassification(gene),
			gene,
			coverageMap.get(gene),
			overlapMapCollective.get(gene),
			pvals.get(gene),
			CollectionUtil.merge(overlapMapPairwise.get(gene).keySet().stream()
				.filter(g2 -> overlapMapPairwise.get(gene).get(g2) > 0)
				.sorted(Comparator.comparing(overlapMapPairwise.get(gene)::get).reversed().thenComparing(Object::toString))
				.map(g2 -> g2 + "=" + overlapMapPairwise.get(gene).get(g2)).collect(Collectors.toList()), " ")),
			writer));

		writer.close();
	}
}
