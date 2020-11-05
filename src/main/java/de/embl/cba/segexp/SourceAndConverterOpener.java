package de.embl.cba.segexp;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.command.OpenFilesWithBigdataviewerBioformatsBridgeCommand;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import loci.common.DebugTools;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SourceAndConverterOpener
{
	private OpenFilesWithBigdataviewerBioformatsBridgeCommand bridgeCommand;

	public SourceAndConverterOpener()
	{
		DebugTools.setRootLevel("OFF");
		bridgeCommand = new OpenFilesWithBigdataviewerBioformatsBridgeCommand();
		bridgeCommand.useBioFormatsCacheBlockSize = true;
		bridgeCommand.unit = "MICROMETER";
	}

	public List< SourceAndConverter< ? > > open( final String imagePath, boolean is2D )
	{
		BioFormatsBdvOpener opener = bridgeCommand.getOpener( new File( imagePath ) );

		AbstractSpimData< ? > spimData = BioFormatsConvertFilesToSpimData.getSpimData( opener );
		List< ConverterSetup > converterSetups = new ArrayList<>();
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();
		BigDataViewer.initSetups( spimData, converterSetups, sourceAndConverters );

		if ( is2D )
		{
			List< SourceAndConverter< ? > > zeroAxialOffsetSources = sourceAndConverters.stream().map( sac ->
			{
				AffineTransform3D sourceTransform = new AffineTransform3D();
				sac.getSpimSource().getSourceTransform( 0, 0, sourceTransform );
				double offset = sourceTransform.get( 2, 3 );
				double zScale = sourceTransform.get( 2, 2 );
				AffineTransform3D offsetToZeroTransform = new AffineTransform3D();
				offsetToZeroTransform.translate( new double[]{ 0, 0, -offset } );
				SourceAffineTransformer transformer = new SourceAffineTransformer( sac, offsetToZeroTransform );
				return ( SourceAndConverter< ? > ) transformer.getSourceOut();
			} ).collect( Collectors.toList() );

			return zeroAxialOffsetSources;
		}
		else
		{
			return sourceAndConverters;
		}
	}
}
