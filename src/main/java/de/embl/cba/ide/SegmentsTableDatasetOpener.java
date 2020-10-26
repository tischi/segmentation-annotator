package de.embl.cba.ide;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SegmentsTableDatasetOpener
{
	public SegmentsTableDatasetOpener()
	{
	}

	public void open( String rootDirectory, String relativeTablePath, boolean isOneBasedTimePoint )
	{
		String tablePath = new File( rootDirectory, relativeTablePath ).toString();

		TableRowImageSegmentsFromTableCreator creator = new TableRowImageSegmentsFromTableCreator( tablePath, isOneBasedTimePoint );
		List< TableRowImageSegment > tableRows = creator.createTableRows();

		ImagePathsFromTableRowsExtractor< TableRowImageSegment > extractor = new ImagePathsFromTableRowsExtractor<>( tableRows, rootDirectory, "relative_path_" );

		List< String > imagePaths = extractor.extractImagePaths();

		SourceAndConverterOpener opener = new SourceAndConverterOpener();
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>( );
		for ( String imagePath : imagePaths )
		{
			List< SourceAndConverter< ? > > sources = opener.open( imagePath );
			sources.forEach( s -> sourceAndConverters.add( s ) );
		}

		BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

		sourceAndConverters.forEach( sac -> {
			SourceAndConverterServices.getSourceAndConverterDisplayService().show( bdvHandle, sac );
			new ViewerTransformAdjuster( bdvHandle, sac ).run();
			new BrightnessAutoAdjuster( sac, 0 ).run();
		} );
	}
}
