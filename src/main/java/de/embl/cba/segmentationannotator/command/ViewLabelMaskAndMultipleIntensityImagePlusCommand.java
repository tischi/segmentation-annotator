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

@Plugin(type = Command.class, menuPath = "Plugins > Segmentation > Annotator > View Label Mask and Intensity Images..." )
public class ViewLabelMaskAndMultipleIntensityImagePlusCommand implements Command
{
	public static final String X = "mean_x";
	public static final String Y = "mean_y";
	public static final String Z = "mean_z";
	public static final String NAME = "image_name";
	public static final String N_PIXELS = "n_pixels";
	public static final String MEAN_INTENSITY = "mean_intensity";
	public static final String VOLUME = "volume";
	public static final String INDEX = "label_index";

	// autoFill = false is necessary, because pre-processors otherwise prevent the rendering of the UI element
	@Parameter( label = "Label Mask Image", persist = false, autoFill = false )
	public ImagePlus labelImage;

	// persist = false is necessary, because pre-processors do not know how to handle ImagePlus[]
	@Parameter( label = "Intensity Image", persist = false, autoFill = false )
	public ImagePlus[] intensityImages;

	@Override
	public void run()
	{
		final ArrayList< ImagePlus > intensityImageList = new ArrayList<>();
		for ( ImagePlus intensityImage : intensityImages )
			intensityImageList.add( intensityImage );

		showImages( labelImage, intensityImageList );
	}

	public static void showImages( ImagePlus labelImage, ArrayList< ImagePlus > intensityImages )
	{
		Map< SourceAndConverter< ? >, SourceMetadata > sources = new HashMap<>();

		final String labelImageId = ImagePlusToSourceAndConverter.addPrimaryLabelSource( sources, labelImage );

		for ( ImagePlus intensityImage : intensityImages )
		{
			ImagePlusToSourceAndConverter.addIntensitySource( sources, intensityImage );
		}

		// create labels and features
		final Map< Integer, SegmentFeatures > labelToFeatures = LabelAnalyzer.analyzeLabels( labelImage.getImageStack(), labelImage.getCalibration() );
		Map< String, List< String > > segmentFeatureColumns = createColumns( labelToFeatures, labelImageId );
		Map< SegmentProperty, List< String > > segmentPropertyToColumnName = getSegmentPropertyToColumnName( segmentFeatureColumns );

		// create feature table
		final List< TableRowImageSegment > tableRowImageSegments = SegmentUtils.tableRowImageSegmentsFromColumns( segmentFeatureColumns, segmentPropertyToColumnName, true );

		SourcesAndSegmentsViewer.view( sources, tableRowImageSegments, labelImage.getNSlices() == 1, labelImage.getNFrames() );
	}

	@NotNull
	private static Map< SegmentProperty, List< String > > getSegmentPropertyToColumnName( Map< String, List< String > > columnNameToColumnEntries )
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
	private static Map< String, List< String > > createColumns( Map< Integer, SegmentFeatures > indexToFeatures, String labelImageName )
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
