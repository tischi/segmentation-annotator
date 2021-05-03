package run;

import de.embl.cba.segmentationannotator.command.OpenIntensityAndLabelsImagePlusCommand;
import ij.IJ;
import net.imagej.ImageJ;

public class RunOpenIntensityAndLabelsImagePlusCommand
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final OpenIntensityAndLabelsImagePlusCommand command = new OpenIntensityAndLabelsImagePlusCommand();
		String root = "/Users/tischer/Documents/segmentations-explorer/";
		command.intensityImagePlus = IJ.openImage( root + "src/test/resources/intensity_image_3d.zip");
		command.labelImagePlus = IJ.openImage( root + "src/test/resources/label_image_3d.zip");
		command.run();
	}
}
