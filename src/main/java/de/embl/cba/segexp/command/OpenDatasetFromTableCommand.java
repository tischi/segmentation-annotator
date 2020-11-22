package de.embl.cba.segexp.command;

import de.embl.cba.segexp.SegmentsDatasetOpener;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Segmentations Explorer > Open Dataset from Table..." )
public class OpenDatasetFromTableCommand implements Command
{
	@Parameter( label = "Segments table" )
	File segmentsTableFile;

	@Override
	public void run()
	{
		String rootDirectory = segmentsTableFile.getParent();
		String relativeTablePath = segmentsTableFile.getName();
		SegmentsDatasetOpener datasetOpener = new SegmentsDatasetOpener( rootDirectory, relativeTablePath );
		datasetOpener.run();
	}
}
