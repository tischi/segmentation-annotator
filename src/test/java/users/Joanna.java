package users;

import de.embl.cba.segmentationannotator.SegmentationAnnotator;
import net.imagej.ImageJ;

public class Joanna
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		String rootDirectory = "/Users/tischer/Documents/joanna-zukowska-golgi-morphology/src/test/resources/image-data";
		String relativeTablePath = "table.csv";

		new SegmentationAnnotator( rootDirectory, relativeTablePath ).run();
	}
}
