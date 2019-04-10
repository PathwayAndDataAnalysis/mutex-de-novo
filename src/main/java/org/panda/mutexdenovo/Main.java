package org.panda.mutexdenovo;

import org.panda.resource.DenovoDB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * This class is the main execution point. It understands the command line arguments and calls necessary functions.
 */
public class Main
{
	public static void main(String[] args) throws IOException
	{
		if (args.length < 1)
		{
			printUsage();
			return;
		}

		ActionEnum action = ActionEnum.get(args[0]);

		if (action == null)
		{
			System.err.println("Unrecognized command: " + args[0]);
			printUsage();
			return;
		}

		if (action.minArguments > args.length - 1)
		{
			System.err.println("At least " + action.minArguments + " arguments needed for the command \"" + args[0] +
				"\".");
			printUsage();
			return;
		}

		action.run(args);
	}

	private static void printUsage()
	{
		System.out.println("Below are possible commands:");
		for (ActionEnum action : ActionEnum.values())
		{
			System.out.println("\n(" + action.description + ")");
			System.out.println(">   " + action.getName() + " " + action.usage);
		}
		System.out.println("\nIn each case, the first token is the command name (should be used as is) and the " +
			"following tokens indicate specific parameters (should be customized).\n" +
			"A parameter in <brackets> indicates an optional parameter. Below are explanation for some of these.\n\n" +
			"matrix-indicator: Can be a filename that has the matrix, or can be a predefined filter for building" +
			" the matrix from denovo-db.\n" +
			"gene-sets-indicator: Can be a filename that has the gene sets, or can be either SFARI or Reactome.\n" +
			"pattern-type: Can be either mutex or cooc, meaning mutual exclusivity or co-occurrence, respectively.");
	}

	interface Action
	{
		void run(String[] args) throws IOException;
	}

	enum ActionEnum implements Action
	{
		GENERATE_MATRIX_FROM_DENOVODB("Generate an alteration matrix from denovo-db. Filter names are pre-defined in " +
			"the code.",
			args ->
		{
			String filterStr = args[1];
			String outFile = args[2];

			DenovoDB.DataFilterEnum filter = DenovoDB.DataFilterEnum.get(filterStr);

			if (filter == null)
			{
				System.err.println("Filter name not defined in DenovoDB.DataFilterEnum: " + filterStr);
				return;
			}

			Matrix matrix = new Matrix(filter);

			if (outFile.contains(File.separator))
			{
				Files.createDirectories(Paths.get(outFile.substring(0, outFile.lastIndexOf(File.separator))));
			}

			matrix.write(outFile);
		}, "  data-filter-name   output-filename", 2),
		CALCULATE("Compute mutual exclusivity and co-occurrence.",
			args ->
		{
			// Read parameters
			String matrixFile = args[1];
			String groupsFile = args[2];
			String outDir = args[3];
			int iterations = Integer.valueOf(args[4]);

			// Load the matrix
			Matrix matrix = loadMatrix(matrixFile);

			// Read gene sets
			GeneSetLoader loader = new GeneSetLoader(matrix);

			// Load gene sets
			Map<String, Set<String>> geneSets = groupsFile.equals(GeneSetLoader.SFARI_SETS) ? loader.loadSFARI() :
				groupsFile.equals(GeneSetLoader.REACTOME_SETS) ? loader.loadReactome() : loader.loadFile(groupsFile);

			// Test exclusivity
			MutexTester tester = new MutexTester(matrix, geneSets, outDir, iterations);
			tester.run();

			if (groupsFile.equals(GeneSetLoader.REACTOME_SETS))
			{
				ReactomeNameAdder.add(outDir + "/results.txt", outDir + "/results-with-names.txt");
			}

		}, "  matrix-indicator   gene-sets-indicator   output-directory   random-iterations", 4),
		ANNOTATE_SET_MEMBERS("Generate a table for members of a gene set in the results.",
			args ->
		{
			String inFile = args[1];
			String matrixFile = args[2];
			String outFile = args[3];

			// Load the matrix
			Matrix matrix = loadMatrix(matrixFile);

			// Annotate the gene set and write into the result file
			ResultGeneSetAnnotator.annotate(inFile, matrix, outFile);
		}, "  input-filename   matrix-indicator   output-filename", 3),
		EXPLORE_SIGNIFICANCE_IN_RESULTS("Explore the effect of filtering the gene sets with their mutation counts on " +
			"the result size under different FDR cutoffs.",
			args ->
		{
			String inFile = args[1];
			String outFile = args[2];
			PatternType pType = PatternType.get(args[3]);

			if (pType == null)
			{
				throw new RuntimeException("Unknown pattern type: " + args[3] + ". Possible values: " +
					PatternType.MUTEX.toString().toLowerCase() + ", " + PatternType.COOC.toString().toLowerCase());
			}

			// Read FDRs
			double[] fdrs = new double[args.length - 4];
			for (int i = 0; i < fdrs.length; i++)
			{
				fdrs[i] = Double.valueOf(args[i + 4]);
			}

			// Explore significances and write results
			SignificanceExplorer.explore(inFile, outFile, pType, fdrs);
		}, "  input-filename   output-filename   pattern-type   FDR1   <FDR2>   <FDR3>   ...", 4),
		FILTER_RESULTS_TO_MOST_HIT("Generate a table from the results, filtering the gene sets to most mutated, " +
			"ordering by p-values.",
			args ->
		{
			String inFile = args[1];
			String outFile = args[2];
			PatternType pType = PatternType.get(args[3]);
			int topX = Integer.valueOf(args[4]);

			if (pType == null)
			{
				throw new RuntimeException("Unknown pattern type: " + args[3] + ". Possible values: " +
					PatternType.MUTEX.toString().toLowerCase() + ", " + PatternType.COOC.toString().toLowerCase());
			}

			// Explore significances and write results
			SignificanceExplorer.filterToTopHit(inFile, outFile, pType, topX);
		}, "  input-filename   output-filename   pattern-type   top-how-many", 4),
		;

		Action action;
		String usage;
		int minArguments;
		String description;

		ActionEnum(String description, Action action, String usage, int minArguments)
		{
			this.description = description;
			this.action = action;
			this.usage = usage;
			this.minArguments = minArguments;
		}

		@Override
		public void run(String[] args) throws IOException
		{
			action.run(args);
		}

		public String getName()
		{
			return toString().toLowerCase().replaceAll("_", "-");
		}

		public static ActionEnum get(String name)
		{
			name = name.toUpperCase().replaceAll("-", "_");
			return valueOf(name);
		}
	}

	private static Matrix loadMatrix(String matrixFileOrDenovoDBFilterName) throws IOException
	{
		// Read matrix
		Matrix matrix = null;
		if (Files.exists(Paths.get(matrixFileOrDenovoDBFilterName)) &&
			!Files.isDirectory(Paths.get(matrixFileOrDenovoDBFilterName)))
		{
			matrix = new Matrix(matrixFileOrDenovoDBFilterName);
		}
		else
		{
			DenovoDB.DataFilterEnum filter = DenovoDB.DataFilterEnum.get(matrixFileOrDenovoDBFilterName);
			if (filter != null)
			{
				matrix = new Matrix(filter);
			}
		}

		// Check if matrix is here
		if (matrix == null)
		{
			throw new RuntimeException("Cannot load alteration matrix. It is not a file nor a predefined " +
				"DenovoDB filter name: " + matrixFileOrDenovoDBFilterName);
		}
		return matrix;
	}
}
