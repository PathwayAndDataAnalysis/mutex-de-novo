package org.panda.mutexdenovo;

import org.panda.resource.autismdatasets.DenovoDB;
import org.panda.utility.ArrayUtil;
import org.panda.utility.FileUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents an alteration matrix of binary values.
 */
public class Matrix
{
	/**
	 * The delimiter used in the text file representation of the alteration matrix.
	 */
	private static final String DELIM = "\t";

	/**
	 * Name of the samples in the matrix.
	 */
	private String[] colNames;

	/**
	 * Rows of the matrix, unordered.
	 */
	private Map<String, boolean[]> rowMap;

	/**
	 * Bipartite graph representation of the matrix.
	 */
	private List<Edge> edges;

	/**
	 * Loads an alteration matrix from file.
	 * @param filename file name
	 * @throws IOException if file is not found
	 */
	public Matrix(String filename) throws IOException
	{
		// Read column names
		String line = Files.lines(Paths.get(filename)).findFirst().get();
		colNames = line.substring(line.indexOf(DELIM) + 1).split(DELIM);

		// Read the rest
		rowMap = new HashMap<>();
		Files.lines(Paths.get(filename)).skip(1).forEach(l ->
		{
			String gene = l.substring(0, l.indexOf(DELIM));
			l = l.substring(l.indexOf(DELIM) + 1);
			Boolean[] B = Arrays.stream(l.split(DELIM)).map(s -> s.isEmpty() || s.equals("0")).toArray(Boolean[]::new);
			boolean[] b = ArrayUtil.convertToBasicBooleanArray(B);
			rowMap.put(gene, b);
		});
	}

	/**
	 * Constructor with values.
	 * @param rowMap rows
	 * @param colNames column names
	 */
	public Matrix(String[] colNames, Map<String, boolean[]> rowMap)
	{
		this.colNames = colNames;
		this.rowMap = rowMap;
	}

	/**
	 * Constructs the matrix by reading from denovo-db.
	 * @param filter data filter for denovo-db. Choose from DenovoDB.DataFilterEnum, or define new.
	 */
	public Matrix(DenovoDB.DataFilter filter)
	{
		colNames = DenovoDB.get().getDataStream(filter).map(e -> e.sampleID).distinct().sorted().toArray(String[]::new);
		Map<String, Set<String>> hitMap = new HashMap<>();

		DenovoDB.get().getDataStream(filter).forEach(e ->
		{
			if (!hitMap.containsKey(e.gene)) hitMap.put(e.gene, new HashSet<>());
			hitMap.get(e.gene).add(e.sampleID);
		});

		rowMap = new HashMap<>();
		hitMap.forEach((gene, hits) ->
		{
			boolean[] b = new boolean[colNames.length];
			for (int i = 0; i < b.length; i++)
			{
				b[i] = hits.contains(colNames[i]);
			}
			rowMap.put(gene, b);
		});
	}

	/**
	 * Generates an independent copy of the array. Shuffling the copy will not alter the original matrix.
	 * @return a copy
	 */
	public Matrix copy()
	{
		// Copy column names
		Matrix c = new Matrix(new String[colNames.length], new HashMap<>());
		System.arraycopy(colNames, 0, c.colNames, 0, colNames.length);

		// Copy values
		rowMap.forEach((gene, vals) ->
		{
			boolean[] b = new boolean[vals.length];
			System.arraycopy(vals, 0, b, 0, b.length);
			c.rowMap.put(gene, b);
		});

		return c;
	}

	/**
	 * Write the matrix to a file.
	 * @param filename file name
	 * @throws IOException if file cannot be written
	 */
	public void write(String filename) throws IOException
	{
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename));

		Arrays.stream(colNames).forEach(s -> FileUtil.tab_write(s, writer));

		rowMap.forEach((gene, b) ->
		{
			FileUtil.lnwrite(gene, writer);
			for (boolean v : b) FileUtil.tab_write(v ? "1" : "0", writer);
		});

		writer.close();
	}

	public boolean hasAllGenes(Collection<String> genes)
	{
		return rowMap.keySet().containsAll(genes);
	}

	public int countCoverage(Set<String> genes)
	{
		int n = rowMap.values().iterator().next().length;
		int cov = 0;
		for (int i = 0; i < n; i++)
		{
			for (String gene : genes)
			{
				boolean[] b = rowMap.get(gene);
				if (b != null && b[i])
				{
					cov++;
					break;
				}
			}
		}
		return cov;
	}

	public Map<String, Integer> countIndividualCoverage(Set<String> genes)
	{
		return genes.stream().collect(Collectors.toMap(Function.identity(), this::getMutationCount));
	}

	public int getMutationCount(String gene)
	{
		return ArrayUtil.countValue(rowMap.get(gene), true);
	}

	public int countOverlap(Set<String> genes)
	{
		int n = rowMap.values().iterator().next().length;
		int ov = 0;

		for (int i = 0; i < n; i++)
		{
			boolean covered = false;
			for (String gene : genes)
			{
				boolean[] b = rowMap.get(gene);
				if (b != null && b[i])
				{
					if (!covered) covered = true;
					else ov++;
				}
			}
		}
		return ov;
	}

	/**
	 * For each member of the gene set, this method calculates their overlaps with other members.
	 * @param genes the gene set
	 * @return pairwise overlap counts
	 */
	public Map<String, Map<String, Integer>> countOverlapPairwise(Set<String> genes)
	{
		Map<String, Map<String, Integer>> map = new HashMap<>();

		for (String gene1 : genes)
		{
			// Init the gene entry
			map.put(gene1, new HashMap<>());

			// Calculate overlap with other genes
			for (String gene2 : genes)
			{
				if (!gene1.equals(gene2))
				{
					map.get(gene1).put(gene2, countOverlap(gene1, gene2));
				}
			}
		}
		return map;
	}

	public int countOverlap(String gene1, String gene2)
	{
		boolean[] b1 = rowMap.get(gene1);
		boolean[] b2 = rowMap.get(gene2);

		int cnt = 0;
		for (int i = 0; i < b1.length; i++)
		{
			if (b1[i] && b2[i]) cnt++;
		}
		return cnt;
	}

	/**
	 * Generates a gene-to-indices representation for the matrix.
	 * @return gene-to-indices map
	 */
	public Map<String, Set<Integer>> getGeneToIndices()
	{
		Map<String, Set<Integer>> map = rowMap.keySet().stream().collect(Collectors.toMap(
			Function.identity(), gene -> new HashSet<>()));

		map.forEach((gene, inds) ->
		{
			boolean[] b = rowMap.get(gene);
			for (int i = 0; i < b.length; i++)
			{
				if (b[i]) inds.add(i);
			}
		});

		return map;
	}

	/**
	 * @return graph representation of the matrix
	 */
	public List<Edge> getEdges()
	{
		if (edges == null)
		{
			edges = generateEdges();
		}
		return edges;
	}

	public Map<String, boolean[]> getRows()
	{
		return rowMap;
	}

	public Set<String> getGenes()
	{
		return rowMap.keySet();
	}

	/**
	 * Generates the graph representation of the matrix.
	 * @return matrix as a graph
	 */
	public List<Edge> generateEdges()
	{
		List<Edge> edges = new ArrayList<>();
		for (String gene : rowMap.keySet())
		{
			boolean[] b = rowMap.get(gene);
			for (int i = 0; i < b.length; i++)
			{
				if (b[i]) edges.add(new Edge(gene, i));
			}
		}
		return edges;
	}

	/**
	 * Class to use for bipartite graph representation of the alteration matrix.
	 */
	static class Edge
	{
		String gene;
		int sampleIndex;

		public Edge(String gene, int sampleIndex)
		{
			this.gene = gene;
			this.sampleIndex = sampleIndex;
		}
	}
}
