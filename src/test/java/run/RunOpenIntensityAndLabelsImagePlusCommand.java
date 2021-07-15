package run;

import de.embl.cba.segmentationannotator.command.ViewIntensityAndLabelsImagePlusCommand;
import ij.IJ;
import net.imagej.ImageJ;

public class RunOpenIntensityAndLabelsImagePlusCommand
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ViewIntensityAndLabelsImagePlusCommand command = new ViewIntensityAndLabelsImagePlusCommand();
		String root = "/Users/tischer/Documents/segmentations-explorer/";
		command.intensityImage = IJ.openImage( root + "src/test/resources/intensity_image_3d.zip");
		command.labelImage = IJ.openImage( root + "src/test/resources/label_image_3d.zip");

		command.intensityImage = IJ.openImage( "/Users/tischer/Desktop/Silvija/stackreg_20210324_PCNA_20x_1um_amst_aligned-crop.tif");
		command.labelImage = IJ.openImage( "/Users/tischer/Desktop/Silvija/stackreg_20210324_PCNA_20x_1um_amst_aligned-crop-fg-probability-8bit-cellpose-d10_cp-6_ai2.tif");

		command.run();
	}
}
