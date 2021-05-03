package de.embl.cba.segmentationannotator.command;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.segmentationannotator.LabelSource;
import de.embl.cba.segmentationannotator.SegmentedImagesView;
import de.embl.cba.segmentationannotator.converters.LabelConverter;
import de.embl.cba.segmentationannotator.labels.LabelAnalyzer;
import de.embl.cba.segmentationannotator.labels.SegmentFeatures;
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
import sc.fiji.bdvpg.bdv.MinimalBdvCreator;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.projector.Projector;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;
import spimdata.imageplus.SpimDataFromImagePlusGetter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins > Segmentation > Annotator > Open Intensity and Label Mask Images..." )
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
		final SourceAndConverter intensitySourceAndConverter = getIntensitySourceAndConverter();
		final SourceAndConverter labelSourceAndConverter = getLabelSourceAndConverter( );

		// create labels and features
		final Map< Integer, SegmentFeatures > labelToFeatures = LabelAnalyzer.analyzeLabels( labelImagePlus.getImageStack(), intensityImagePlus.getImageStack(), intensityImagePlus.getCalibration() );
		Map< String, List< String > > columns = getColumns( labelToFeatures, intensitySourceAndConverter.getSpimSource().getName() );
		Map< SegmentProperty, List< String > > segmentPropertyToColumnName = getSegmentPropertyToColumnName( columns );

		// create table
		final List< TableRowImageSegment > tableRowImageSegments = SegmentUtils.tableRowImageSegmentsFromColumns( columns, segmentPropertyToColumnName, true );

		// create selection and coloring models
		DefaultSelectionModel< TableRowImageSegment > selectionModel = new DefaultSelectionModel<>();
		LazyCategoryColoringModel< TableRowImageSegment > coloringModel = new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) );
		SelectionColoringModel< TableRowImageSegment > selectionColoringModel = new SelectionColoringModel<>( coloringModel, selectionModel );

		// create image view
		new SegmentedImagesView<>( tableRowImageSegments, selectionColoringModel,  )

//		final BdvHandle bdvHandle = new MinimalBdvCreator( "", intensityImagePlus.getNSlices() == 1, Projector.SUM_PROJECTOR, true, intensityImagePlus.getNFrames() ).get();
//		final SourceAndConverterBdvDisplayService displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
//		displayService.show( bdvHandle, intensitySourceAndConverter );
//		displayService.show( bdvHandle, labelSourceAndConverter );
//		new BrightnessAutoAdjuster( intensitySourceAndConverter,0 ).run();
//		new ViewerTransformAdjuster( bdvHandle, intensitySourceAndConverter ).run();
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
	private Map< String, List< String > > getColumns( Map< Integer, SegmentFeatures > indexToFeatures, String labelImageName )
	{
		Map< String, List< String > > columnNameToColumnEntries = new HashMap<>();
		columnNameToColumnEntries.put( X, new ArrayList< String >() );
		columnNameToColumnEntries.put( Y, new ArrayList< String >() );
		columnNameToColumnEntries.put( Z, new ArrayList< String >() );
		columnNameToColumnEntries.put( NAME, new ArrayList< String >() );
		columnNameToColumnEntries.put( N_PIXELS, new ArrayList< String >() );
		columnNameToColumnEntries.put( MEAN_INTENSITY, new ArrayList< String >() );
		columnNameToColumnEntries.put( INDEX, new ArrayList< String >() );

		for ( Integer labelIndex : indexToFeatures.keySet() )
		{
			final SegmentFeatures features = indexToFeatures.get( labelIndex );
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
