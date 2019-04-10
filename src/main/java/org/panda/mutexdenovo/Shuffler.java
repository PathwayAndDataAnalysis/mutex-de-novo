package org.panda.mutexdenovo;

import java.util.List;
import java.util.Random;

public class Shuffler
{
	private static final int Q = 100;

	private Matrix matrix;

	private Random r;

	public Shuffler(Matrix matrix)
	{
		this.matrix = matrix;
		r = new Random();
	}

	public void shuffle()
	{
		List<Matrix.Edge> edges = matrix.getEdges();
		int E = edges.size();

		for (int i = 0; i < Q; i++)
		{
			for (int j = 0; j < E; j++)
			{
				Matrix.Edge edge1 = edges.get(r.nextInt(E));
				Matrix.Edge edge2 = edges.get(r.nextInt(E));

				if (edge1 != edge2)
				{
					boolean[] b1 = matrix.getRows().get(edge1.gene);
					boolean[] b2 = matrix.getRows().get(edge2.gene);

					if (!b1[edge2.sampleIndex] && !b2[edge1.sampleIndex])
					{
						b1[edge1.sampleIndex] = !b1[edge1.sampleIndex];
						b1[edge2.sampleIndex] = !b1[edge2.sampleIndex];
						b2[edge1.sampleIndex] = !b2[edge1.sampleIndex];
						b2[edge2.sampleIndex] = !b2[edge2.sampleIndex];

						int temp = edge1.sampleIndex;
						edge1.sampleIndex = edge2.sampleIndex;
						edge2.sampleIndex = temp;
					}
				}
			}
		}
	}
}
