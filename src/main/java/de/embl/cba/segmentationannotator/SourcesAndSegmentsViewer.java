package de.embl.cba.segmentationannotator;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.segmentationannotator.volume.SegmentsVolumeView;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.List;
import java.util.Map;

public class SourcesAndSegmentsViewer
{
	public static void view( Map< SourceAndConverter< ? >, SourceMetadata > sources, List< TableRowImageSegment > tableRowImageSegments, boolean is2D, int nFrames )
	{
		// create selection and coloring models
		DefaultSelectionModel< TableRowImageSegment > selectionModel = new DefaultSelectionModel<>();
		LazyCategoryColoringModel< TableRowImageSegment > coloringModel = new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) );
		SelectionColoringModel< TableRowImageSegment > selectionColoringModel = new SelectionColoringModel<>( coloringModel, selectionModel );

		// create image view
		final SegmentedImagesView< ?, ? > imagesView = new SegmentedImagesView( tableRowImageSegments, selectionColoringModel, sources );
		imagesView.showImages( is2D, nFrames );

		// table view
		TableView< TableRowImageSegment > tableView = new TableView<>( tableRowImageSegments, selectionModel, selectionColoringModel );
		tableView.showTableAndMenu( imagesView.getWindow() );
		imagesView.setTableView( tableView );

		// volume view
		final SegmentsVolumeView< TableRowImageSegment > volumeView = new SegmentsVolumeView<>( selectionModel, selectionColoringModel, sources.keySet() );
		selectionModel.listeners().add( volumeView );
		selectionColoringModel.listeners().add( volumeView );
		imagesView.setVolumeView( volumeView );
	}
}
