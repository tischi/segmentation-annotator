package de.embl.cba.segexp;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.command.OpenFilesWithBigdataviewerBioformatsBridgeCommand;
import ch.epfl.biop.bdv.bioformats.export.spimdata.BioFormatsConvertFilesToSpimData;
import loci.common.DebugTools;
import mpicbg.spim.data.generic.AbstractSpimData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	public HashMap< SourceAndConverter< ? >, SourceMetadata > open( final String imagePath )
	{
		BioFormatsBdvOpener opener = bridgeCommand.getOpener( new File( imagePath ) );

		AbstractSpimData< ? > spimData = BioFormatsConvertFilesToSpimData.getSpimData( opener );
		List< ConverterSetup > converterSetups = new ArrayList<>();
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();
		BigDataViewer.initSetups( spimData, converterSetups, sourceAndConverters );

		HashMap< SourceAndConverter< ? >, SourceMetadata > sourceToMetadata = new HashMap<>();

		sourceAndConverters.forEach( source ->
		{
			SourceMetadata sourceMetadata = new SourceMetadata();
			// TODO: maybe put some metadata, e.g., 2D here, but it could be expensive to access the source
			sourceToMetadata.put( source, sourceMetadata );
		});

		return sourceToMetadata;
	}
}
