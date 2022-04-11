package run;

import de.embl.cba.segmentationannotator.command.OpenDatasetFromTableCommand;
import de.embl.cba.segmentationannotator.command.ViewLabelMaskAndIntensityImagePlusAndMorpholibJResultsTableCommand;
import ij.IJ;
import net.imagej.ImageJ;

import java.io.File;

public class OpenMicrogliaDatasetFromTable
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final OpenDatasetFromTableCommand command = new OpenDatasetFromTableCommand();
		command.segmentsTableFile = new File( "src/test/resources/microglia/MAX_pg6-3CF1_20--t1-3.csv" );
		command.imagePathColumnPrefix = "Path_";
		command.run();
	}
}
