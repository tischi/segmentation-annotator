package de.embl.cba.segmentationannotator.command;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.segmentationannotator.LabelSource;
import de.embl.cba.segmentationannotator.converters.LabelConverter;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import org.jetbrains.annotations.NotNull;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.MinimalBdvCreator;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.projector.Projector;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;
import spimdata.imageplus.SpimDataFromImagePlusGetter;

@Plugin(type = Command.class, menuPath = "Plugins > Segmentation > Annotator > Open Intensity and Label Mask Images..." )
public class OpenIntensityAndLabelsImagePlusCommand implements Command
{
	@Parameter( label = "Intensity Image" )
	ImagePlus intensityImagePlus;

	@Parameter( label = "Label Mask Image" )
	ImagePlus labelImagePlus;

	@Override
	public void run()
	{
		final SourceAndConverter intensitySourceAndConverter = getIntensitySourceAndConverter();
		final SourceAndConverter labelSourceAndConverter = getLabelSourceAndConverter( );

		final BdvHandle bdvHandle = new MinimalBdvCreator( "", intensityImagePlus.getNSlices() == 1, Projector.SUM_PROJECTOR, true, intensityImagePlus.getNFrames() ).get();

		final SourceAndConverterBdvDisplayService displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
		displayService.show( bdvHandle, intensitySourceAndConverter );
		displayService.show( bdvHandle, labelSourceAndConverter );
		new BrightnessAutoAdjuster( intensitySourceAndConverter,0 ).run();
		new ViewerTransformAdjuster( bdvHandle, intensitySourceAndConverter ).run();
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

	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		// Run plugin from ImageJ Plugins menu.
	}
}
