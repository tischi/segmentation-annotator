package de.embl.cba.segmentationannotator.command;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.segmentationannotator.label.LabelSource;
import de.embl.cba.segmentationannotator.SegmentedImagesView;
import de.embl.cba.segmentationannotator.SourceMetadata;
import de.embl.cba.segmentationannotator.TableView;
import de.embl.cba.segmentationannotator.converter.LabelConverter;
import de.embl.cba.segmentationannotator.label.LabelAnalyzer;
import de.embl.cba.segmentationannotator.label.SegmentFeatures;
import de.embl.cba.segmentationannotator.volume.SegmentsVolumeView;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.jetbrains.annotations.NotNull;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;
import spimdata.imageplus.SpimDataFromImagePlusGetter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins > Segmentation > Annotator > Open Intensity and Label Mask Image..." )
public class OpenIntensityAndLabelsImagePlusCommand implements Command
{
	public static final String X = "mean_x";
	public static final String Y = "mean_y";
	public static final String Z = "mean_z";
	public static final String NAME = "image_name";
	public static final String N_PIXELS = "n_pixels";
	public static final String MEAN_INTENSITY = "mean_intensity";
	public static final String INDEX = "label_index";

	@Parameter( label = "Intensity Image" )
	public ImagePlus intensityImagePlus;

	@Parameter( label = "Label Mask Image" )
	public ImagePlus labelImagePlus;

	@Override
	public void run()
	{
		// create images
		Map< SourceAndConverter< ? >, SourceMetadata > sources = new HashMap<>();
		final String labelImageId = addLabelSource( sources );
		addIntensitySource( sources );

		// create labels and features
		final Map< Integer, SegmentFeatures > labelToFeatures = LabelAnalyzer.analyzeLabels( labelImagePlus.getImageStack(), intensityImagePlus.getImageStack(), intensityImagePlus.getCalibration() );
		Map< String, List< String > > columns = createColumns( labelToFeatures, labelImageId );
		Map< SegmentProperty, List< String > > segmentPropertyToColumnName = getSegmentPropertyToColumnName( columns );

		// create table
		final List< TableRowImageSegment > tableRowImageSegments = SegmentUtils.tableRowImageSegmentsFromColumns( columns, segmentPropertyToColumnName, true );

		// create selection and coloring models
		DefaultSelectionModel< TableRowImageSegment > selectionModel = new DefaultSelectionModel<>();
		LazyCategoryColoringModel< TableRowImageSegment > coloringModel = new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) );
		SelectionColoringModel< TableRowImageSegment > selectionColoringModel = new SelectionColoringModel<>( coloringModel, selectionModel );

		// create image view
		final SegmentedImagesView< ?, ? > imagesView = new SegmentedImagesView( tableRowImageSegments, selectionColoringModel, sources );
		imagesView.showImages( intensityImagePlus.getNSlices() == 1, intensityImagePlus.getNFrames() );

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

	public void addIntensitySource( Map< SourceAndConverter< ? >, SourceMetadata > sources )
	{
		final SpimDataFromImagePlusGetter getter = new SpimDataFromImagePlusGetter();

		final SourceAndConverter intensitySourceAndConverter = new SourceAndConverterFromSpimDataCreator( getter.apply( intensityImagePlus ) ).getSetupIdToSourceAndConverter().get( 0 );

		final SourceMetadata intensitySourceMetadata = new SourceMetadata();
		intensitySourceMetadata.isLabelSource = false;
		intensitySourceMetadata.isPrimaryLabelSource = false;
		intensitySourceMetadata.imageId = intensitySourceAndConverter.getSpimSource().getName();
		sources.put( intensitySourceAndConverter, intensitySourceMetadata );
	}

	public String addLabelSource( Map< SourceAndConverter< ? >, SourceMetadata > sources )
	{
		final SpimDataFromImagePlusGetter getter = new SpimDataFromImagePlusGetter();

		final SourceAndConverter labelSourceAndConverter = new SourceAndConverterFromSpimDataCreator( getter.apply( labelImagePlus ) ).getSetupIdToSourceAndConverter().get( 0 );

		final SourceMetadata labelSourceMetadata = new SourceMetadata();
		labelSourceMetadata.isLabelSource = true;
		labelSourceMetadata.isPrimaryLabelSource = true;
		labelSourceMetadata.imageId = labelSourceAndConverter.getSpimSource().getName();
		sources.put( labelSourceAndConverter, labelSourceMetadata );
		return labelSourceMetadata.imageId;
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
		columnNameToColumnEntries.put( MEAN_INTENSITY, new ArrayList< String >() );

		for ( Integer labelIndex : indexToFeatures.keySet() )
		{
			final SegmentFeatures features = indexToFeatures.get( labelIndex );
			columnNameToColumnEntries.get( INDEX ).add( String.valueOf( labelIndex ) );
			columnNameToColumnEntries.get( NAME ).add( labelImageName );
			columnNameToColumnEntries.get( X ).add( String.valueOf( features.anchorX ) );
			columnNameToColumnEntries.get( Y ).add( String.valueOf( features.anchorY ) );
			columnNameToColumnEntries.get( Z ).add( String.valueOf( features.anchorZ ) );
			columnNameToColumnEntries.get( MEAN_INTENSITY ).add( String.valueOf( features.meanIntensity ) );
			columnNameToColumnEntries.get( N_PIXELS ).add( String.valueOf( features.numPixels ) );
		}
		return columnNameToColumnEntries;
	}

	private SourceAndConverter getIntensitySourceAndConverter()
	{
		final SpimDataFromImagePlusGetter getter = new SpimDataFromImagePlusGetter();
		final AbstractSpimData< ? > intensity = getter.apply( intensityImagePlus );
		final SourceAndConverter intensitySourceAndConverter = new SourceAndConverterFromSpimDataCreator( intensity ).getSetupIdToSourceAndConverter().get( 0 );
		return intensitySourceAndConverter;
	}

	@NotNull
	private SourceAndConverter getLabelSourceAndConverter()
	{
		final SpimDataFromImagePlusGetter getter = new SpimDataFromImagePlusGetter();
		final AbstractSpimData< ? > labels = getter.apply( labelImagePlus );
		final SourceAndConverter labelSourceAndConverter = new SourceAndConverterFromSpimDataCreator( labels ).getSetupIdToSourceAndConverter().get( 0 );
		LabelConverter labelConverter = new LabelConverter();
		final LabelSource labelSource = new LabelSource<>( labelSourceAndConverter.getSpimSource() );
		final SourceAndConverter sourceAndConverter = new SourceAndConverter<>( labelSource, labelConverter );
		return sourceAndConverter;
	}
}
