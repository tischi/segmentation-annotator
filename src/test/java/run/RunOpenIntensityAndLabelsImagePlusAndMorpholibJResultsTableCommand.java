package run;

import de.embl.cba.segmentationannotator.command.ViewIntensityAndLabelImagePlusAndMorpholibJResultsTableCommand;
import ij.IJ;
import net.imagej.ImageJ;

public class RunOpenIntensityAndLabelsImagePlusAndMorpholibJResultsTableCommand
{
	public static void main( String[] args )
	{
		String root = "/Users/tischer/Documents/segmentations-explorer/";

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// open results table
		IJ.open( root + "src/test/resources/golgi-cell-features.csv");

		final ViewIntensityAndLabelImagePlusAndMorpholibJResultsTableCommand command = new ViewIntensityAndLabelImagePlusAndMorpholibJResultsTableCommand();
		command.intensityImage = IJ.openImage( root + "src/test/resources/golgi-intensities.tif");
		command.labelImage = IJ.openImage( root + "src/test/resources/golgi-cell-labels.tif");
		command.resultsTableTitle = "golgi-cell-features.csv";
		command.run();
	}
}
