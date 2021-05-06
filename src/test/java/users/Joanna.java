package users;

import de.embl.cba.segmentationannotator.command.OpenDatasetFromTableCommand;
import net.imagej.ImageJ;

import java.io.File;

public class Joanna
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final OpenDatasetFromTableCommand command = new OpenDatasetFromTableCommand();
		command.segmentsTableFile = new File( "/Users/tischer/Documents/joanna-zukowska-golgi-morphology/src/test/resources/image-data/table.csv" );
		command.run();
	}
}
