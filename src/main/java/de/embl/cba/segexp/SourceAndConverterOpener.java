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
import java.util.List;

public class SourceAndConverterOpener
{
	public SourceAndConverterOpener()
	{
		DebugTools.setRootLevel("OFF");
	}

	public List< SourceAndConverter< ? > > open( final String imagePath )
	{
		OpenFilesWithBigdataviewerBioformatsBridgeCommand bridgeCommand = new OpenFilesWithBigdataviewerBioformatsBridgeCommand();
		bridgeCommand.useBioFormatsCacheBlockSize = true;
		BioFormatsBdvOpener opener = bridgeCommand.getOpener( new File( imagePath ) );

		AbstractSpimData< ? > spimData = BioFormatsConvertFilesToSpimData.getSpimData( opener );
		List< ConverterSetup > converterSetups = new ArrayList<>();
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();
		BigDataViewer.initSetups( spimData, converterSetups, sourceAndConverters );

		return sourceAndConverters;
	}
}
