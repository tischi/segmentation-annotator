package run;

import de.embl.cba.segmentationannotator.command.ViewLabelMaskAndIntensityImagePlusCommand;
import ij.IJ;
import net.imagej.ImageJ;

public class RunOpenLabelMaskAndIntensityImagePlusCommand
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ViewLabelMaskAndIntensityImagePlusCommand command = new ViewLabelMaskAndIntensityImagePlusCommand();
		String root = "/Users/tischer/Documents/segmentations-explorer/";
		command.labelImage = IJ.openImage( root + "src/test/resources/label_image_3d.zip");
		command.intensityImage = IJ.openImage( root + "src/test/resources/intensity_image_3d.zip");
		command.run();
	}
}
