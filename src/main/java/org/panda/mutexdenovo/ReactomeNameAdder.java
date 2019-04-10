package org.panda.mutexdenovo;

import org.panda.resource.ReactomePathway;
import org.panda.utility.ArrayUtil;
import org.panda.utility.FileUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * An analysis with Reactome gene sets uses IDs of Reactome pathways as identifiers. This class adds the names of the
 * pathways to the result file.
 */
public class ReactomeNameAdder
{
	public static final String ID_HEADER = "http://identifiers.org/reactome/";

	public static void add(String inFile, String outFile) throws IOException
	{
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile));
		Files.lines(Paths.get(inFile)).forEach(l -> FileUtil.writeln(transformLine(l), writer));
		writer.close();
	}

	private static String transformLine(String line)
	{
		String[] t = line.split("\t");
		String id = ID_HEADER + t[0];
		String name = ReactomePathway.get().getName(id);

		if (name != null) t[0] += "\t" + name;
		else if (t[0].equals("ID")) t[0] += "\tName";
		else t[0] += "\t";

		return ArrayUtil.merge("\t", t);
	}
}
