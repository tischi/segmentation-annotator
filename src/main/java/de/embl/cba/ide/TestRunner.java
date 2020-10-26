package de.embl.cba.ide;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.bdv.ScreenShotMaker;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ProjectionModeChanger;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.ArrayList;
import java.util.List;

public class TestRunner
{
	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		String rootDir = "/Users/tischer/Documents/joanna-zukowska-golgi-morphology/src/test/resources/image-data/";

		TableRowImageSegmentsFromTableCreator creator = new TableRowImageSegmentsFromTableCreator( rootDir + "table.csv", false );
		List< TableRowImageSegment > tableRows = creator.createTableRows();

		ImagePathsFromTableRowsExtractor< TableRowImageSegment > extractor = new ImagePathsFromTableRowsExtractor<>( tableRows, rootDir, "relative_path_" );
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
