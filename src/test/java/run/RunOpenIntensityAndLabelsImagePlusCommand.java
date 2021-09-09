package run;

import de.embl.cba.segmentationannotator.command.ViewIntensityAndLabelsImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

public class RunOpenIntensityAndLabelsImagePlusCommand
{
	public static void main( String[] args )
	{
		String root = "/Users/tischer/Documents/segmentations-explorer/";

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ViewIntensityAndLabelsImagePlusCommand command = new ViewIntensityAndLabelsImagePlusCommand();
		command.intensityImage = IJ.openImage( root + "src/test/resources/intensity_image_3d.zip");
		command.intensityImage2 = IJ.openImage( root + "src/test/resources/label_image_3d.zip");

		command.run();
	}
}
