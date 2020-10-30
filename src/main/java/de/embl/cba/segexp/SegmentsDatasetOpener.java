package de.embl.cba.segexp;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SegmentsDatasetOpener
{
	private List< SourceAndConverter< ? > > sources;
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

		ImagePathsFromTableRowsExtractor< TableRowImageSegment > imagePathsExtractor = new ImagePathsFromTableRowsExtractor( segments, rootDirectory, "image_path_", labelImageColumnName );
		Set< String > imagePaths = imagePathsExtractor.getImagePaths();
		HashMap< String, String > imagePathToLabelImageId = imagePathsExtractor.getImagePathToLabelImageId();

		SourceAndConverterOpener opener = new SourceAndConverterOpener();
		sources = new ArrayList<>( );
		sourceToLabelImageId = new HashMap<>();
		imagePaths.forEach( imagePath ->
		{
			List< SourceAndConverter< ? > > sourcesFromImagePath = opener.open( imagePath );

			if ( imagePathToLabelImageId.keySet().contains( imagePath ) )
			{
				if ( sourcesFromImagePath.size() > 1 )
					throw new UnsupportedOperationException( "Label mask images must not contain multiple channels!" );

				sourceToLabelImageId.put( sourcesFromImagePath.get( 0 ), imagePathToLabelImageId.get( imagePath ) );
			}

			sourcesFromImagePath.forEach( sourceFromImagePath ->
			{
				sources.add( sourceFromImagePath );
			} );
		} );
	}

	public List< SourceAndConverter< ? > > getSources()
	{
		return sources;
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
