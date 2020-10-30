package de.embl.cba.segexp;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RealPoint;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.List;
import java.util.stream.Collectors;

public class SourcesGetter
{
	private final BdvHandle bdvHandle;

	public SourcesGetter( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	public List< SourceAndConverter< ? > > getVisibleSourcesAtCurrentMousePosition()
	{
		final RealPoint mousePosInBdv = new RealPoint( 3 );
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( mousePosInBdv );
		int timePoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		List< SourceAndConverter< ? > > sourceAndConverters = SourceAndConverterServices.getSourceAndConverterDisplayService().getSourceAndConverterOf( bdvHandle )
				.stream()
				.filter( sac -> SourceAndConverterUtils.isSourcePresentAt( sac, timePoint, mousePosInBdv ) )
				.filter( sac -> SourceAndConverterServices.getSourceAndConverterDisplayService().isVisible( sac, bdvHandle ) )
				.collect( Collectors.toList() );

		return sourceAndConverters;
	}
}
