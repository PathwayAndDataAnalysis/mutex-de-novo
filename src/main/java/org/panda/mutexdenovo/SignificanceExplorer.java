package org.panda.mutexdenovo;

import org.panda.utility.ArrayUtil;
import org.panda.utility.FileUtil;
import org.panda.utility.statistics.FDR;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class processes the resulting p-values of the gene sets and finds the optimum number of most-mutated gene sets
 * to test to maximize the number of significant gene sets, for several given FDR.
 */
public class SignificanceExplorer
{
	public static void explore(String inFile, String outFile, PatternType pType, double[] fdrs) throws IOException
	{
		// Read p-values
		Map<String, Double> pvals = Files.lines(Paths.get(inFile))
			.skip(1).map(l -> l.split("\t"))
			.collect(Collectors.toMap(t -> t[0], t -> Double.valueOf(t[pType == PatternType.MUTEX ? 4 : 5])));

		// Read number mutations (coverage + overlap) on each gene set
		Map<String, Integer> hits = Files.lines(Paths.get(inFile))
			.skip(1).map(l -> l.split("\t"))
			.collect(Collectors.toMap(t -> t[0], t -> Integer.valueOf(t[2]) + Integer.valueOf(t[3])));

		// Find unique hit thresholds, ordered descending
		List<Integer> orderedUniqueHits = hits.values().stream().distinct().sorted(Comparator.reverseOrder())
			.collect(Collectors.toList());

		// Start writing the output
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile));
		writer.write("Tested size\tHit thr");
		Arrays.stream(fdrs).forEach(fdr -> FileUtil.tab_write("FDR=" + fdr, writer));

		Map<Double, Integer> maximums = new HashMap<>();
		Arrays.stream(fdrs).forEach(fdr -> maximums.put(fdr, 0));

		// For each hit threshold, find the number of significant results for each FDR threshold
		for (int thr : orderedUniqueHits)
		{
			// Get the subset using the threshold
			Map<String, Double> filteredP = pvals.keySet().stream().filter(name -> hits.get(name) >= thr)
				.collect(Collectors.toMap(Function.identity(), pvals::get));

			writer.write("\n" + filteredP.size() + "\t" + thr);

			// Find the result size
			for (double fdr : fdrs)
			{
				List<String> select = FDR.select(filteredP, null, fdr);
				writer.write("\t" + select.size());

				if (maximums.get(fdr) < select.size())
				{
					maximums.put(fdr, select.size());
				}
			}
		}

		// Write maximum result sizes for each FDR
		writer.write("\n\nmaximums\t");
		Arrays.stream(fdrs).forEach(fdr -> FileUtil.tab_write(maximums.get(fdr), writer));

		writer.close();
	}

	/**
	 *
	 * @param inFile
	 * @param outFile
	 * @param pType
	 * @param topX
	 * @throws IOException
	 */
	public static void filterToTopHit(String inFile, String outFile, PatternType pType, int topX) throws IOException
	{
		String headerLine = Files.lines(Paths.get(inFile)).findFirst().get();
		String[] header = headerLine.split("\t");
		int cvgInd = ArrayUtil.indexOf(header, "Coverage");
		int ovInd = ArrayUtil.indexOf(header, "Overlap");
		int pInd = ArrayUtil.indexOf(header, pType == PatternType.MUTEX ? "Mutex p-value" : "Cooc p-value");

		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile));
		writer.write(headerLine);

		Files.lines(Paths.get(inFile)).skip(1).map(l -> l.split("\t"))
			.sorted((t1, t2) -> Integer.compare(
				Integer.valueOf(t2[cvgInd]) + Integer.valueOf(t2[ovInd]),
				Integer.valueOf(t1[cvgInd]) + Integer.valueOf(t1[ovInd])))
			.limit(topX)
			.sorted(Comparator.comparing(t -> Double.valueOf(t[pInd])))
			.forEach(t -> FileUtil.lnwrite(ArrayUtil.merge("\t", t), writer));

		writer.close();
	}

}
