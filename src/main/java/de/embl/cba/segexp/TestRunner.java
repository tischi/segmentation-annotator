package de.embl.cba.segexp;

import net.imagej.ImageJ;

public class TestRunner
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

//		new SourceAndConverterOpener().open( "/Users/tischer/Documents/joanna-zukowska-golgi-morphology/src/test/resources/image-data/frag-cells-label-mask.ome.tif" );

		SegmentsDatasetOpener datasetOpener = new SegmentsDatasetOpener();

		datasetOpener.open( "/Users/tischer/Documents/joanna-zukowska-golgi-morphology/src/test/resources/image-data", "table.csv", false );

	}

}
