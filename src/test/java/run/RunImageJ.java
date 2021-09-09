package run;

import ij.IJ;
import net.imagej.ImageJ;

public class RunImageJ
{
	public static void main( String[] args )
	{
		String root = "/Users/tischer/Documents/segmentations-explorer/";

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		IJ.open( root + "src/test/resources/intensity_image_3d.zip");
		IJ.open( root + "src/test/resources/intensity_image_3d_downscaled.zip");
		IJ.open( root + "src/test/resources/label_image_3d.zip");
	}
}
