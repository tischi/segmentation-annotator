package de.embl.cba.segexp;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils
{
	public static String createAbsolutePath( String rootPath, String relativePath )
	{
		final Path path = Paths.get( rootPath, relativePath );
		final Path normalize = path.normalize();
		return normalize.toString();
	}
}
