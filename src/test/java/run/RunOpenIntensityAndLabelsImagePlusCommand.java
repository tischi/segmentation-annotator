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

		command.intensityImagePlus = IJ.openImage( "/Users/tischer/Desktop/Silvija/stackreg_20210324_PCNA_20x_1um_amst_aligned-crop.tif");
		command.labelImagePlus = IJ.openImage( "/Users/tischer/Desktop/Silvija/stackreg_20210324_PCNA_20x_1um_amst_aligned-crop-fg-probability-8bit-cellpose-d10_cp-6_ai2.tif");

		command.run();
	}
}
