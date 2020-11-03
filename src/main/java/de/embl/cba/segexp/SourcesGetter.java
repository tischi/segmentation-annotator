package de.embl.cba.segexp;

import bdv.util.Bdv;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

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


		return sourceAndConverters;
	}

	private static List< SourceAndConverter< ? > > getSourcesAtPosition( Bdv bdv, RealPoint position, boolean is2D )
	{
		final ArrayList< Integer > sourceIndicesAtSelectedPoint = new ArrayList<>();

		List< SourceAndConverter< ? > > sources = bdv.getBdvHandle().getViewerPanel().state().getSources();

		List< SourceAndConverter< ? > > collect = sources.stream().filter( source -> {

			Source< ? > spimSource = source.getSpimSource();

			final long[] positionInSource = getPositionInSource( spimSource, position, 0, 0 );
			Interval sourceInterval = spimSource.getSource( 0, 0 );

			Interval interval;
			Point point;

			if ( is2D )
			{
				final long[] min = new long[ 2 ];
				final long[] max = new long[ 2 ];
				final long[] positionInSource2D = new long[ 2 ];
				for ( int d = 0; d < 2; d++ )
				{
					min[ d ] = sourceInterval.min( d );
					max[ d ] = sourceInterval.max( d );
					positionInSource2D[ d ] = positionInSource[ d ];
				}

				interval = new FinalInterval( min, max );
				point = new Point( positionInSource2D );
			}
			else
			{
				interval = sourceInterval;
				point = new Point( positionInSource );
			}

			if ( Intervals.contains( interval, point ) )
				return true;
			else
				return false;

		} ).collect( Collectors.toList() );

		return collect;
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
