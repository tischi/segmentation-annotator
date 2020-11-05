package de.embl.cba.segexp;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SegmentsDatasetOpener implements Runnable
{
	private HashMap< String, Set< SourceAndConverter< ? > > > columnNameToSources;
	private List< TableRowImageSegment > segments;
	private HashMap< SourceAndConverter< ? >, String > labelSourceToLabelImageId;
	private String rootDirectory;
	private String relativeTablePath;
	private boolean isOneBasedTimePoint;
	private boolean is2d;

	public SegmentsDatasetOpener( String rootDirectory, String relativeTablePath, boolean isOneBasedTimePoint, boolean is2d )
	{
		this.rootDirectory = rootDirectory;
		this.relativeTablePath = relativeTablePath;
		this.isOneBasedTimePoint = isOneBasedTimePoint;
		this.is2d = is2d;
	}

	@Override
	public void run()
	{
		String tablePath = new File( rootDirectory, relativeTablePath ).toString();
		SegmentsCreator segmentsCreator = new SegmentsCreator( tablePath, isOneBasedTimePoint );
		segments = segmentsCreator.createSegments();
		String labelImageColumnName = segmentsCreator.getLabelImageColumnName();

		ImagePathsFromTableRowsExtractor< TableRowImageSegment > imagePathsExtractor = new ImagePathsFromTableRowsExtractor( segments, rootDirectory, labelImageColumnName, "image_path_" );
		Map< String, Set< String > > columnNameToImagePaths = imagePathsExtractor.getColumnNameToImagePaths();
		Set< String > labelImagePaths = imagePathsExtractor.getLabelImagePaths();
		SourceAndConverterOpener opener = new SourceAndConverterOpener();
		openLabelSources( rootDirectory, labelImagePaths, opener, is2d );
		openOtherSources( rootDirectory, columnNameToImagePaths, opener, is2d );
	}

	private void openLabelSources( String rootDirectory, Set< String > labelImagePaths, SourceAndConverterOpener opener, final boolean is2d )
	{
		Logger.log("Opening label sources...");
		labelSourceToLabelImageId = new HashMap<>();
		labelImagePaths.forEach( labelImagePath -> {
			String absolutePath = Utils.createAbsolutePath( rootDirectory, labelImagePath );
			List< SourceAndConverter< ? > > sourcesFromImagePath = opener.open( absolutePath, is2d );
			SourceAndConverter< ? > labelsSource = sourcesFromImagePath.get( 0 );
			labelSourceToLabelImageId.put( labelsSource, labelImagePath );
		} );
	}

	private void openOtherSources( String rootDirectory, Map< String, Set< String > > columnNameToImagePaths, SourceAndConverterOpener opener, final boolean is2d )
	{
		Logger.log("Opening other sources...");
		columnNameToSources = new HashMap<>( );
		columnNameToImagePaths.keySet().forEach( columnName ->
		{
			final HashSet< SourceAndConverter< ? > > sources = new HashSet<>();
			columnNameToImagePaths.get( columnName ).forEach( imagePath ->
			{
				String absolutePath = Utils.createAbsolutePath( rootDirectory, imagePath );
				List< SourceAndConverter< ? > > sourcesFromImagePath = opener.open( absolutePath, is2d );
				sourcesFromImagePath.forEach( sourceFromImagePath -> sources.add( sourceFromImagePath ) );
			} );
			columnNameToSources.put( columnName, sources );
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

	public HashMap< SourceAndConverter< ? >, String > getLabelSourceToLabelImageId()
	{
		return labelSourceToLabelImageId;
	}


}
