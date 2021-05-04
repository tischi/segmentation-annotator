package de.embl.cba.segmentationannotator;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.TableRowsTableView;

import java.util.HashMap;
import java.util.List;

public class SegmentationAnnotator
{
	private final String rootDirectory;
	private final String relativeTablePath;

	public SegmentationAnnotator( String rootDirectory, String relativeTablePath )
	{

		this.rootDirectory = rootDirectory;
		this.relativeTablePath = relativeTablePath;
	}

	public void run()
	{
		SegmentsDatasetOpener opener = new SegmentsDatasetOpener( rootDirectory, relativeTablePath );
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
		Utils.centerComponentOnScreen( imagesView.getWindow(), 10 );
		TableView< TableRowImageSegment > tableView = new TableView<>( tableRowImageSegments, selectionModel, selectionColoringModel );
		tableView.showTableAndMenu( imagesView.getWindow() );
		imagesView.setTableView( tableView );
	}
}
