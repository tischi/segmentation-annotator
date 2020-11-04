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

import java.util.List;
import java.util.stream.Collectors;

public class SpatialSourceAndConverterSubsetter
{
	private final List< SourceAndConverter< ? > > sources;
	private final boolean sourcesAre2d;

	public SpatialSourceAndConverterSubsetter( List< SourceAndConverter< ? > > sources,  boolean sourcesAre2d )
	{
		this.sources = sources;
		this.sourcesAre2d = sourcesAre2d;
	}

	public List< SourceAndConverter< ? > > getSourcesAtPosition( final RealPoint point, int timepoint )
	{
		List< SourceAndConverter< ? > > collect = sources.stream().filter( source -> {

			Source< ? > spimSource = source.getSpimSource();

			final long[] voxelPositionInSource = getVoxelPositionInSource( spimSource, point, timepoint, 0 );
			Interval sourceInterval = spimSource.getSource( 0, 0 );

			if ( sourcesAre2d )
			{
				final long[] min = new long[ 2 ];
				final long[] max = new long[ 2 ];
				final long[] positionInSource2D = new long[ 2 ];
				for ( int d = 0; d < 2; d++ )
				{
					min[ d ] = sourceInterval.min( d );
					max[ d ] = sourceInterval.max( d );
					positionInSource2D[ d ] = voxelPositionInSource[ d ];
				}

				Interval interval2d = new FinalInterval( min, max );
				Point point2d = new Point( positionInSource2D );

				return Intervals.contains( interval2d, point2d ) ? true : false;
			}
			else
			{
				Interval interval3d = sourceInterval;
				Point point3d = new Point( voxelPositionInSource );

				return Intervals.contains( interval3d, point3d ) ? true : false;
			}
		} ).collect( Collectors.toList() );

		return collect;
	}

	private static long[] getVoxelPositionInSource(
			Source source,
			RealPoint globalPosition,
			int t,
			int level )
	{
		int n = 3;

		final AffineTransform3D sourceTransform = BdvUtils.getSourceTransform( source, t, level );

		final RealPoint voxelPositionInSource = new RealPoint( n );

		sourceTransform.inverse().apply( globalPosition, voxelPositionInSource );

		final long[] longPosition = new long[ n ];

		for ( int d = 0; d < n; ++d )
			longPosition[ d ] = (long) voxelPositionInSource.getFloatPosition( d );

		return longPosition;
	}
}
