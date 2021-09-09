package run;

import de.embl.cba.segmentationannotator.command.ViewLabelMaskAndTwoIntensityImagePlusCommand;
import ij.IJ;
import net.imagej.ImageJ;

public class RunOpenLabelMaskAndTwoIntensityImagePlusCommand
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ViewLabelMaskAndTwoIntensityImagePlusCommand command = new ViewLabelMaskAndTwoIntensityImagePlusCommand();
		String root = "/Users/tischer/Documents/segmentations-explorer/";
		command.labelImage = IJ.openImage( root + "src/test/resources/label_image_3d.zip");
		command.intensityImage = IJ.openImage( root + "src/test/resources/intensity_image_3d.zip");
		command.intensityImage2 = IJ.openImage( root + "src/test/resources/intensity_image_3d_downscaled.zip");
		
		command.run();
	}
}
