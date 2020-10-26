package de.embl.cba.ide;

import net.imagej.ImageJ;

public class TestRunner
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		SegmentsTableDatasetOpener datasetOpener = new SegmentsTableDatasetOpener();

		datasetOpener.open( "/Users/tischer/Documents/joanna-zukowska-golgi-morphology/src/test/resources/image-data", "table.csv", false );
	}

}
