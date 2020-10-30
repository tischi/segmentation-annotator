package de.embl.cba.segexp;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import net.imagej.ImageJ;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class TestRunner
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

//		new SourceAndConverterOpener().open( "/Users/tischer/Documents/joanna-zukowska-golgi-morphology/src/test/resources/image-data/frag-cells-label-mask.ome.tif" );

		SegmentsDatasetOpener opener = new SegmentsDatasetOpener();
		opener.open( "/Users/tischer/Documents/joanna-zukowska-golgi-morphology/src/test/resources/image-data", "table.csv", false );
		HashMap< String, Set< SourceAndConverter< ? > > > columnNameToSources = opener.getColumnNameToSources();
		List< TableRowImageSegment > segments = opener.getSegments();
		HashMap< SourceAndConverter< ? >, String > sourceToLabelImageId = opener.getSourceToLabelImageId();


		// create coloring and selection models
		DefaultSelectionModel< TableRowImageSegment > selectionModel = new DefaultSelectionModel<>();
		LazyCategoryColoringModel< TableRowImageSegment > coloringModel = new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) );
		SelectionColoringModel< TableRowImageSegment > selectionColoringModel = new SelectionColoringModel<>( coloringModel, selectionModel );

		new SegmentedImagesViewer( segments, selectionColoringModel, columnNameToSources, sourceToLabelImageId );
		// also return make a map from sourceName to imageSegmentId, which is what is in the table

		//new SegmentedImagesViewer<>(  )

//		HashSet< String > labelImageIds = new HashSet<>();
//		for ( TableRowImageSegment segment : segments )
//		{
//			labelImageIds.add( segment.imageId() =  );
//		}

	}

}
