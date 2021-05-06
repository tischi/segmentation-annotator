package de.embl.cba.segmentationannotator;

import bdv.viewer.SourceAndConverter;
import ij.ImagePlus;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterFromSpimDataCreator;
import spimdata.imageplus.SpimDataFromImagePlusGetter;

import java.util.Map;

public class ImagePlusToSourceAndConverter
{
	public static String addPrimaryLabelSource( Map< SourceAndConverter< ? >, SourceMetadata > sources, ImagePlus labelImagePlus )
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

	public static String addIntensitySource( Map< SourceAndConverter< ? >, SourceMetadata > sources, ImagePlus intensityImagePlus )
	{
		final SpimDataFromImagePlusGetter getter = new SpimDataFromImagePlusGetter();

		final SourceAndConverter intensitySourceAndConverter = new SourceAndConverterFromSpimDataCreator( getter.apply( intensityImagePlus ) ).getSetupIdToSourceAndConverter().get( 0 );

		final SourceMetadata intensitySourceMetadata = new SourceMetadata();
		intensitySourceMetadata.isLabelSource = false;
		intensitySourceMetadata.isPrimaryLabelSource = false;
		intensitySourceMetadata.imageId = intensitySourceAndConverter.getSpimSource().getName();
		sources.put( intensitySourceAndConverter, intensitySourceMetadata );
		return intensitySourceMetadata.imageId;
	}
}
