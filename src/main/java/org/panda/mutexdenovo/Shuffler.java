package org.panda.mutexdenovo;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Implements the degree-preserving randomization of graphs from
 * <a href="https://arxiv.org/abs/cond-mat/0312028">https://arxiv.org/abs/cond-mat/0312028</a>.
 */
public class Shuffler
{
	/**
	 * Magic constant.
	 */
	private static final int Q = 100;

	/**
	 * The alteration matrix.
	 */
	private Matrix matrix;

	/**
	 * Random number generator.
	 */
	private Random r;

	/**
	 * Constructor with the matrix.
	 * @param matrix the alteration matrix
	 */
	public Shuffler(Matrix matrix)
	{
		this.matrix = matrix;
		r = new Random();
	}

	/**
	 * One round of randomization of the matrix.
	 */
	public void shuffle()
	{
		// Get the matrix as a graph
		List<Matrix.Edge> edges = matrix.getEdges();

		// Find edge size
		int E = edges.size();

		// Get matrix rows for later access
		Map<String, boolean[]> rows = matrix.getRows();

		// Do Q times
		for (int i = 0; i < Q; i++)
		{
			// Do E times
			for (int j = 0; j < E; j++)
			{
				// Select two random edges
				Matrix.Edge edge1 = edges.get(r.nextInt(E));
				Matrix.Edge edge2 = edges.get(r.nextInt(E));

				if (edge1 != edge2)
				{
					boolean[] b1 = rows.get(edge1.gene);
					boolean[] b2 = rows.get(edge2.gene);

					// If swapping the targets of those edges does not generate redundant edges, swap them.
					if (!b1[edge2.sampleIndex] && !b2[edge1.sampleIndex])
					{
						// Swap in the matrix representation
						b1[edge1.sampleIndex] = !b1[edge1.sampleIndex];
						b1[edge2.sampleIndex] = !b1[edge2.sampleIndex];
						b2[edge1.sampleIndex] = !b2[edge1.sampleIndex];
						b2[edge2.sampleIndex] = !b2[edge2.sampleIndex];

						// Swap in the graph representation
						int temp = edge1.sampleIndex;
						edge1.sampleIndex = edge2.sampleIndex;
						edge2.sampleIndex = temp;
					}
				}
			}
		}
	}
}
