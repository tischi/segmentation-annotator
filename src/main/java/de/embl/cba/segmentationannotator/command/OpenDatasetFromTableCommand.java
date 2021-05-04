package de.embl.cba.segmentationannotator.command;

import de.embl.cba.segmentationannotator.SegmentationAnnotator;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins > Segmentation > Annotator > Open Dataset from Table..." )
public class OpenDatasetFromTableCommand implements Command
{
	/**
	 * Each row must contain information about one image segment.
	 * Paths to images in the table must be relative to the parent directory of the table.
	 */
	@Parameter( label = "Segments table" )
	File segmentsTableFile;

	@Override
	public void run()
	{
		String rootDirectory = segmentsTableFile.getParent();
		String relativeTablePath = segmentsTableFile.getName();
		new SegmentationAnnotator( rootDirectory, relativeTablePath ).run();
	}

	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		// Run plugin from ImageJ Plugins menu.
	}
}
