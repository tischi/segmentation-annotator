package run;

import de.embl.cba.segmentationannotator.command.ViewLabelMaskAndIntensityImagePlusAndMorpholibJResultsTableCommand;
import de.embl.cba.segmentationannotator.command.ViewLabelMaskAndMultipleIntensityImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

import java.io.File;

public class OpenMicrogliaLabelsAndImageAndTable
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// open results table
		final String resultTablePath = new File( "/Users/tischer/Documents/segmentation-annotator/src/test/resources/microglia/MAX_pg6-3CF1_20-labelMasks--t1-3.csv" ).getPath();
		IJ.open( resultTablePath );

		final ViewLabelMaskAndIntensityImagePlusAndMorpholibJResultsTableCommand command = new ViewLabelMaskAndIntensityImagePlusAndMorpholibJResultsTableCommand();
		command.intensityImage = IJ.openImage( new File( "src/test/resources/microglia/MAX_pg6-3CF1_20--t1-3.tif" ).getPath() );
		command.labelImage = IJ.openImage( new File( "src/test/resources/microglia/MAX_pg6-3CF1_20--t1-3-labelMasks.tif").getPath() );
		command.resultsTableTitle = "MAX_pg6-3CF1_20-labelMasks--t1-3.csv";

		command.run();
	}
}
