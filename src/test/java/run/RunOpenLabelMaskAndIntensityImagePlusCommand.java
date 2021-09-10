package run;

import de.embl.cba.segmentationannotator.command.ViewLabelMaskAndMultipleIntensityImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

public class RunOpenLabelMaskAndIntensityImagePlusCommand
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ViewLabelMaskAndMultipleIntensityImagePlusCommand command = new ViewLabelMaskAndMultipleIntensityImagePlusCommand();
		String root = "/Users/tischer/Documents/segmentations-explorer/";
		command.labelImage = IJ.openImage( root + "src/test/resources/label_image_3d.zip");
		command.intensityImages = new ImagePlus[]{ IJ.openImage( root + "src/test/resources/intensity_image_3d.zip" ), IJ.openImage( root + "src/test/resources/intensity_image_3d_downscaled.zip" ) };
		command.run();
	}
}
