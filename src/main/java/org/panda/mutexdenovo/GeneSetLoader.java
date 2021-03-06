package org.panda.mutexdenovo;

import org.panda.resource.ReactomePathway;
import org.panda.resource.autismdatasets.SFARI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GeneSetLoader
{
	// Constants to indicate built-in resources
	public static final String SFARI_SETS = "SFARI";
	public static final String REACTOME_SETS = "Reactome";

	/**
	 * Alteration matrices relevant for the gene sets.
	 */
	private Matrix[] matrices;

	/**
	 * Constructor with the relevant matrices.
	 * @param matrix the alteration matrices
	 */
	public GeneSetLoader(Matrix... matrix)
	{
		this.matrices = matrix;
	}

	/**
	 * Loads SFARI genes. First set is the most confident set of rank 1 genes. Second set is ranks 1
	 * and 2, third set is 1,2 and 3, and so on.
	 */
	public Map<String, Set<String>> loadSFARI()
	{
		Map<String, Set<String>> geneSets = new HashMap<>();

		for (int i = 1; i <= 6; i++)
		{
			Set<String> genes = SFARI.get().getGenesWithMaxScore(i);
			geneSets.put("SFARI-1-to-" + i, genes);
		}

		geneSets.put("SFARI-all", new HashSet<>(SFARI.get().getAllGenes()));

		cleanAndremoveRedundant(geneSets);
		return geneSets;
	}

	private Set<String> getRelevantGenes()
	{
		Set<String> genes = new HashSet<>();
		for (Matrix matrix : matrices)
		{
			genes.addAll(matrix.getGenes());
		}
		return genes;
	}

	/**
	 * Load Reactome gene sets.
	 */
	public Map<String, Set<String>> loadReactome()
	{
		Map<String, Set<String>> orig = ReactomePathway.get().getCroppedPathways(getRelevantGenes());

		Map<String, Set<String>> sets = new HashMap<>();

		orig.forEach((name, set) -> sets.put(name.substring(name.lastIndexOf("/") + 1), set));

		return sets;
	}

	public Map<String, Set<String>> loadFile(String filename) throws IOException
	{
		Map<String, Set<String>> geneSets = Files.lines(Paths.get(filename)).map(l -> l.split("\t"))
			.collect(Collectors.toMap(t -> t[0], t -> new HashSet<>(Arrays.asList(t[1].split(" ")))));

		cleanAndremoveRedundant(geneSets);
		return geneSets;
	}

	private void cleanAndremoveRedundant(Map<String, Set<String>> geneSets)
	{
		Set<String> genes = getRelevantGenes();
		geneSets.values().forEach(set -> set.retainAll(genes));

		Set<String> remove = new HashSet<>();
		Set<Set<String>> exists = new HashSet<>();

		geneSets.forEach((name, set) ->
		{
			if (set.size() < 2 || exists.contains(set))
			{
				remove.add(name);
			}
			else exists.add(set);
		});

		remove.forEach(geneSets::remove);
	}
}
