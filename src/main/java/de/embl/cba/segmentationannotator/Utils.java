package de.embl.cba.segmentationannotator;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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

	@NotNull
	public static String removeFilenameExtension( String imagePath )
	{
		return FilenameUtils.removeExtension( new File( imagePath ).getName() ).replace( ".ome", "" );
	}
}
