package de.embl.cba.segexp;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SegmentsDatasetOpener
{
	private List< SourceAndConverter< ? > > sourceAndConverters;
	private List< TableRowImageSegment > tableRows;

	public SegmentsDatasetOpener()
	{
	}

	public void open( String rootDirectory, String relativeTablePath, boolean isOneBasedTimePoint )
	{
		String tablePath = new File( rootDirectory, relativeTablePath ).toString();

		TableRowImageSegmentsFromTableCreator creator = new TableRowImageSegmentsFromTableCreator( tablePath, isOneBasedTimePoint );
		tableRows = creator.createTableRows();

		ImagePathsFromTableRowsExtractor< TableRowImageSegment > imagePathsExtractor = new ImagePathsFromTableRowsExtractor<>( tableRows, rootDirectory, "image_path_" );
		List< String > imagePaths = imagePathsExtractor.extractImagePaths();

		SourceAndConverterOpener opener = new SourceAndConverterOpener();
		sourceAndConverters = new ArrayList<>( );
		for ( String imagePath : imagePaths )
		{
			List< SourceAndConverter< ? > > sources = opener.open( imagePath );
			sources.forEach( s -> sourceAndConverters.add( s ) );
		}
	}
}
