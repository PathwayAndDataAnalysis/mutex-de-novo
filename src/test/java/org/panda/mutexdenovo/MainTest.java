package org.panda.mutexdenovo;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MainTest
{
	public static void main(String[] args) throws IOException
	{
		testThePipeline();
	}

	public static void testThePipeline() throws IOException
	{
		String outDir = "/home/ozgun/Documents/Temp/";
		String matrixFilterName = "yuen-turner-autism";
		String[] args;

		args = new String[]{"calculate", matrixFilterName, "Reactome", outDir, "100"};
		Main.main(args);

		args = new String[]{"explore-significance-in-results", outDir + "results.txt", outDir + "results-significance-explored-mutex.txt", "mutex", "0.1", "0.2", "0.3", "0.4"};
		Main.main(args);

		args = new String[]{"filter-results-to-most-hit", outDir + "results-with-names.txt", outDir + "results-top50-mutex.txt", "mutex", "50"};
		Main.main(args);

		args = new String[]{"annotate-set-members", outDir + "R-HSA-400253-mutex.txt", matrixFilterName, outDir + "results-circadian-members-table.txt"};
		Main.main(args);

		args = new String[]{"annotate-set-members", outDir + "R-HSA-1257604-mutex.txt", matrixFilterName, outDir + "results-pi3k-members-table.txt"};
		Main.main(args);
	}
}