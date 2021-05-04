package run;

import de.embl.cba.segmentationannotator.command.OpenIntensityAndLabelsImagePlusCommand;
import ij.IJ;
import net.imagej.ImageJ;

public class RunImageJ
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}
