package run;

import de.embl.cba.segmentationannotator.command.ViewLabelMaskAndMultipleIntensityImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

import java.io.File;

public class OpenMicrogliaLabelsAndImage
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		final ViewLabelMaskAndMultipleIntensityImagePlusCommand command = new ViewLabelMaskAndMultipleIntensityImagePlusCommand();
		final String intensityImagePath = new File( "src/test/resources/microglia/MAX_pg6-3CF1_20--t1-3.tif" ).getPath();
		final ImagePlus intensityImage = IJ.openImage( intensityImagePath );
		command.intensityImages = new ImagePlus[]{ intensityImage };
		command.labelImage = IJ.openImage( new File( "src/test/resources/microglia/MAX_pg6-3CF1_20--t1-3-labelMasks.tif").getPath() );
		command.run();
	}
}
