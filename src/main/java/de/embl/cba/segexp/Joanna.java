package de.embl.cba.segexp;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.TableRowsTableView;
import net.imagej.ImageJ;

import java.util.HashMap;
import java.util.List;

public class Joanna
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		String rootDirectory = "/Users/tischer/Documents/joanna-zukowska-golgi-morphology/src/test/resources/image-data";
		// rootDirectory = "/Users/tischer/Desktop/STX_treated_cells_1_1exp";

		// open data table (= segments) and images
		SegmentsDatasetOpener opener = new SegmentsDatasetOpener( rootDirectory, "table.csv", false );
		opener.run();
		List< TableRowImageSegment > tableRowImageSegments = opener.getSegments();
		HashMap< SourceAndConverter< ? >, SourceMetadata > sourceToMetadata = opener.getSourceToMetadata();

		// create coloring and selection models
		DefaultSelectionModel< TableRowImageSegment > selectionModel = new DefaultSelectionModel<>();
		LazyCategoryColoringModel< TableRowImageSegment > coloringModel = new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) );
		SelectionColoringModel< TableRowImageSegment > selectionColoringModel = new SelectionColoringModel<>( coloringModel, selectionModel );

		// show data
		SegmentedImagesView imagesView = new SegmentedImagesView( tableRowImageSegments, selectionColoringModel, sourceToMetadata );
		imagesView.showImages( true, 1 );
		TableRowsTableView< TableRowImageSegment > tableView = new TableRowsTableView<>( tableRowImageSegments, selectionModel, selectionColoringModel );
		tableView.setSelectionMode( TableRowsTableView.SelectionMode.FocusOnly );
		tableView.showTableAndMenu( imagesView.getWindow() );
	}
}
