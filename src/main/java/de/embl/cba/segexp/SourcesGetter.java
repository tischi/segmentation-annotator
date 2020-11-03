package de.embl.cba.segexp;

import bdv.util.Bdv;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.state.SourceState;
import de.embl.cba.bdv.utils.BdvUtils;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SourcesGetter
{
	private final BdvHandle bdvHandle;

	public SourcesGetter( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	public List< SourceAndConverter< ? > > getVisibleSourcesAtMousePosition()
	{
		final RealPoint mousePosInBdv = new RealPoint( 3 );
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( mousePosInBdv );
		int timePoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		BdvUtils.getSourceIndicesAtSelectedPoint()
		List< SourceAndConverter< ? > > sourceAndConverters = SourceAndConverterServices.getSourceAndConverterDisplayService().getSourceAndConverterOf( bdvHandle )
				.stream()
				.filter( sac -> SourceAndConverterUtils.isSourcePresentAt( sac, timePoint, mousePosInBdv ) )
				.filter( sac -> SourceAndConverterServices.getSourceAndConverterDisplayService().isVisible( sac, bdvHandle ) )
				.collect( Collectors.toList() );

		return sourceAndConverters;
	}

	public static ArrayList< Integer > getSourceIndicesAtSelectedPoint( Bdv bdv, RealPoint selectedPoint, boolean evalSourcesAtPointIn2D )
	{
		final ArrayList< Integer > sourceIndicesAtSelectedPoint = new ArrayList<>();

		final int numSources = bdv.getBdvHandle().getViewerPanel()
				.getState().getSources().size();

		for ( int sourceIndex = 0; sourceIndex < numSources; sourceIndex++ )
		{
			final SourceState< ? > sourceState =
					bdv.getBdvHandle().getViewerPanel()
							.getState().getSources().get( sourceIndex );

			final Source< ? > source = sourceState.getSpimSource();

			final long[] positionInSource = getPositionInSource( source, selectedPoint, 0, 0 );

			Interval interval = source.getSource( 0, 0 );
			final Point point = new Point( positionInSource );

			if ( evalSourcesAtPointIn2D )
			{
				final long[] min = new long[ 2 ];
				final long[] max = new long[ 2 ];
				final long[] positionInSource2D = new long[ 2 ];
				for ( int d = 0; d < 2; d++ )
				{
					min[ d ] = interval.min( d );
					max[ d ] = interval.max( d );
					positionInSource2D[ d ] = positionInSource[ d ];
				}

				final FinalInterval interval2D = new FinalInterval( min, max );
				final Point point2D = new Point( positionInSource2D );

				if ( Intervals.contains( interval2D, point2D ) )
					sourceIndicesAtSelectedPoint.add( sourceIndex );
			}
			else
			{
				if ( Intervals.contains( interval, point ) )
					sourceIndicesAtSelectedPoint.add( sourceIndex );
			}
		}

		return sourceIndicesAtSelectedPoint;
	}

	private static long[] getPositionInSource(
			Source source,
			RealPoint positionInViewer,
			int t,
			int level )
	{
		int n = 3;

		final AffineTransform3D sourceTransform =
				BdvUtils.getSourceTransform( source, t, level );

		final RealPoint positionInSourceInPixelUnits = new RealPoint( n );

		sourceTransform.inverse().apply(
				positionInViewer, positionInSourceInPixelUnits );

		final long[] longPosition = new long[ n ];

		for ( int d = 0; d < n; ++d )
			longPosition[ d ] = (long) positionInSourceInPixelUnits.getFloatPosition( d );

		return longPosition;
	}

}
