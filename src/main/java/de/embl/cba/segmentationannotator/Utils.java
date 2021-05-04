package de.embl.cba.segmentationannotator;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
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

	public static String removeFilenameExtension( String imagePath )
	{
		return FilenameUtils.removeExtension( new File( imagePath ).getName() ).replace( ".ome", "" );
	}

	public static void centerComponentOnScreen( Component window, int y )
	{
		Window windowAncestor = SwingUtilities.getWindowAncestor( window );

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		windowAncestor.setLocation( screenSize.width / 2 - window.getWidth() / 2, y );
	}
}
