package de.embl.cba.ide;

import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.io.File;
import java.util.List;

public class TestRunner
{
	public static void main( String[] args )
	{
		String rootDir = "/Users/tischer/Documents/joanna-zukowska-golgi-morphology/src/test/resources/image-data/";

		TableRowImageSegmentsFromTableCreator creator = new TableRowImageSegmentsFromTableCreator( rootDir + "table.csv", false );

		List< TableRowImageSegment > tableRows = creator.createTableRows();

		ImagePathsFromTableRowsExtractor< TableRowImageSegment > extractor = new ImagePathsFromTableRowsExtractor<>( tableRows, rootDir, "relative_path_" );

		List< String > imagePaths = extractor.extractImagePaths();

	}
}
