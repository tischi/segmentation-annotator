package de.embl.cba.segmentationannotator.command;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.segmentationannotator.ImagePlusToSourceAndConverter;
import de.embl.cba.segmentationannotator.SourceMetadata;
import de.embl.cba.segmentationannotator.SourcesAndSegmentsViewer;
import de.embl.cba.segmentationannotator.label.LabelAnalyzer;
import de.embl.cba.segmentationannotator.label.SegmentFeatures;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.ImagePlus;
import org.jetbrains.annotations.NotNull;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins > Segmentation > Annotator > View Intensity and Label Mask Image..." )
public class ViewIntensityAndLabelsImagePlusCommand implements Command
{
	public static final String X = "mean_x";
	public static final String Y = "mean_y";
	public static final String Z = "mean_z";
	public static final String NAME = "image_name";
	public static final String N_PIXELS = "n_pixels";
	public static final String MEAN_INTENSITY = "mean_intensity";
	public static final String VOLUME = "volume";
	public static final String INDEX = "label_index";

	@Parameter( label = "Label Mask Image" )
	public ImagePlus[] labelImages;

	@Parameter( label = "Intensity Images" )
	public ImagePlus[] intensityImages;

	@Override
	public void run()
	{
		// prepare images
		Map< SourceAndConverter< ? >, SourceMetadata > sources = new HashMap<>();

		// label image
		final ImagePlus primaryLabelImage = labelImages[ 0 ];
		final String labelImageId = ImagePlusToSourceAndConverter.addPrimaryLabelSource( sources, primaryLabelImage );

		// intensity images
		for ( ImagePlus intensityImage : intensityImages )
		{
			ImagePlusToSourceAndConverter.addIntensitySource( sources, intensityImage );
		}

		// compute labels and features
		final Map< Integer, SegmentFeatures > labelToSegmentFeatures = LabelAnalyzer.analyzeLabels( primaryLabelImage.getImageStack(), primaryLabelImage.getCalibration() );
		Map< String, List< String > > segmentFeatureColumns = createColumns( labelToSegmentFeatures, labelImageId );
		Map< SegmentProperty, List< String > > segmentPropertyToColumnName = getSegmentPropertyToColumnName( segmentFeatureColumns );

		// create feature table
		final List< TableRowImageSegment > tableRowImageSegments = SegmentUtils.tableRowImageSegmentsFromColumns( segmentFeatureColumns, segmentPropertyToColumnName, true );

		SourcesAndSegmentsViewer.view( sources, tableRowImageSegments, primaryLabelImage.getNSlices() == 1, primaryLabelImage.getNFrames() );
	}

	@NotNull
	private Map< SegmentProperty, List< String > > getSegmentPropertyToColumnName( Map< String, List< String > > columnNameToColumnEntries )
	{
		Map< SegmentProperty, List< String > > segmentPropertyToColumnName = new HashMap<>();
		segmentPropertyToColumnName.put( SegmentProperty.LabelImage, columnNameToColumnEntries.get( NAME ) );
		segmentPropertyToColumnName.put( SegmentProperty.ObjectLabel, columnNameToColumnEntries.get( INDEX ) );
		segmentPropertyToColumnName.put( SegmentProperty.X, columnNameToColumnEntries.get( X ) );
		segmentPropertyToColumnName.put( SegmentProperty.Y, columnNameToColumnEntries.get( Y ) );
		segmentPropertyToColumnName.put( SegmentProperty.Z, columnNameToColumnEntries.get( Z ) );
		return segmentPropertyToColumnName;
	}

	@NotNull
	private Map< String, List< String > > createColumns( Map< Integer, SegmentFeatures > indexToFeatures, String labelImageName )
	{
		Map< String, List< String > > columnNameToColumnEntries = new LinkedHashMap<>();
		columnNameToColumnEntries.put( INDEX, new ArrayList< String >() );
		columnNameToColumnEntries.put( NAME, new ArrayList< String >() );
		columnNameToColumnEntries.put( X, new ArrayList< String >() );
		columnNameToColumnEntries.put( Y, new ArrayList< String >() );
		columnNameToColumnEntries.put( Z, new ArrayList< String >() );
		columnNameToColumnEntries.put( N_PIXELS, new ArrayList< String >() );
		columnNameToColumnEntries.put( VOLUME, new ArrayList< String >() );

		for ( Integer labelIndex : indexToFeatures.keySet() )
		{
			final SegmentFeatures features = indexToFeatures.get( labelIndex );
			columnNameToColumnEntries.get( INDEX ).add( String.valueOf( labelIndex ) );
			columnNameToColumnEntries.get( NAME ).add( labelImageName );
			columnNameToColumnEntries.get( X ).add( String.valueOf( features.anchorX ) );
			columnNameToColumnEntries.get( Y ).add( String.valueOf( features.anchorY ) );
			columnNameToColumnEntries.get( Z ).add( String.valueOf( features.anchorZ ) );
			columnNameToColumnEntries.get( N_PIXELS ).add( String.valueOf( features.numPixels ) );
			columnNameToColumnEntries.get( VOLUME ).add( String.valueOf( features.volume ) );
		}
		return columnNameToColumnEntries;
	}
}
