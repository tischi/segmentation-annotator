package de.embl.cba.segexp;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.io.File;
import java.util.*;

public class SegmentsDatasetOpener
{
	private HashMap< String, Set< SourceAndConverter< ? > > > columnNameToSources;
	private List< TableRowImageSegment > segments;
	private HashMap< SourceAndConverter< ? >, String > sourceToLabelImageId;

	public SegmentsDatasetOpener()
	{
	}

	public void open( String rootDirectory, String relativeTablePath, boolean isOneBasedTimePoint )
	{
		String tablePath = new File( rootDirectory, relativeTablePath ).toString();

		SegmentsCreator segmentsCreator = new SegmentsCreator( tablePath, isOneBasedTimePoint );
		segments = segmentsCreator.createSegments();
		String labelImageColumnName = segmentsCreator.getLabelImageColumnName();

		ImagePathsFromTableRowsExtractor< TableRowImageSegment > imagePathsExtractor = new ImagePathsFromTableRowsExtractor( segments, rootDirectory, labelImageColumnName, "image_path_" );
		Map< String, Set< String > > columnNameToImagePaths = imagePathsExtractor.getColumnNameToImagePaths();
		Set< String > labelImagePaths = imagePathsExtractor.getLabelImagePaths();

		SourceAndConverterOpener opener = new SourceAndConverterOpener();

		columnNameToSources = new HashMap<>( );
		columnNameToImagePaths.keySet().forEach( columnName ->
		{
			final HashSet< SourceAndConverter< ? > > sources = new HashSet<>();
			columnNameToImagePaths.get( columnName ).forEach( imagePath ->
			{
				String absolutePath = Utils.createAbsolutePath( rootDirectory, imagePath );
				List< SourceAndConverter< ? > > sourcesFromImagePath = opener.open( absolutePath );
				sourcesFromImagePath.forEach( sourceFromImagePath -> sources.add( sourceFromImagePath ) );
			} );
			columnNameToSources.put( columnName, sources );
		} );

		sourceToLabelImageId = new HashMap<>();
		labelImagePaths.forEach( labelImagePath -> {
			String absolutePath = Utils.createAbsolutePath( rootDirectory, labelImagePath );
			List< SourceAndConverter< ? > > sourcesFromImagePath = opener.open( absolutePath );
			SourceAndConverter< ? > labelsSource = sourcesFromImagePath.get( 0 );
			sourceToLabelImageId.put( labelsSource, labelImagePath );
		} );

	}

	public HashMap< String, Set< SourceAndConverter< ? > > > getColumnNameToSources()
	{
		return columnNameToSources;
	}

	public List< TableRowImageSegment > getSegments()
	{
		return segments;
	}

	public HashMap< SourceAndConverter< ? >, String > getSourceToLabelImageId()
	{
		return sourceToLabelImageId;
	}
}
