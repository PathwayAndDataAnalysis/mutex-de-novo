package org.panda.mutexdenovo;

import org.panda.utility.FileUtil;
import org.panda.utility.statistics.FDR;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * To detect significant member genes in the results.
 */
public class MemberGeneContributionAnalyzer
{
	public static void findAndDocument(String dir, String outFile, String inFileSuffix, double fdrThr) throws IOException
	{
		// Load files as p-value maps
		Map<Path, Map<String, Double>> pMap =  Files.list(Paths.get(dir))
			.filter(p -> p.toString().endsWith(inFileSuffix))
			.collect(Collectors.toMap(p -> p, p-> FileUtil.lines(p).map(l -> l.split("\t"))
				.collect(Collectors.toMap(t -> t[0], t -> Double.valueOf(t[1])))));

		// Find significant members for each gene set analysis results
		Map<Path, List<String>> select = pMap.keySet().stream()
			.collect(Collectors.toMap(p -> p, p -> FDR.select(pMap.get(p), null, fdrThr)));

		// Write output

		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile));

		select.keySet().stream().filter(p -> !select.get(p).isEmpty()).forEach(p ->
			FileUtil.writeln(removeSuffix(p.getName(p.getNameCount()-1).toString(), inFileSuffix) + "\t" +
				select.get(p), writer));

		writer.close();
	}

	private static String removeSuffix(String s, String suffix)
	{
		return s.substring(0, s.lastIndexOf(suffix));
	}
}
