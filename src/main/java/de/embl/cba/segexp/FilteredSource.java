package de.embl.cba.segexp;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.lazyalgorithm.RandomAccessibleIntervalFilter;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

/**
 * A {@link Source} that wraps another {@link Source} and allows to decorate it
 * with an extra {@link RandomAccessibleIntervalFilter}.
 * <p>
 * This extra transformation is made to capture manual editing of the actual
 * transform in the SpimViewer.
 *
 * @author Christian Tischer - Oct 2020
 *
 * @param <T>
 *            the type of the original source.
 */
public class FilteredSource< T extends NumericType< T > > implements Source< T >
{
	protected final Source< T > source;
	private final DefaultInterpolators< T > interpolators;
	private RandomAccessibleIntervalFilter< T > filter;

	public FilteredSource( final Source< T > source )
	{
		this( source, null);
	}

	public FilteredSource( final Source< T > source, RandomAccessibleIntervalFilter< T > filter )
	{
		this.source = source;
		this.filter = filter;
		this.interpolators = new DefaultInterpolators();
	}

	public void setFilter( RandomAccessibleIntervalFilter< T > filter )
	{
		this.filter = filter;
	}

	@Override
	public boolean doBoundingBoxCulling()
	{
		return source.doBoundingBoxCulling();
	}


	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		source.getSourceTransform( t, level, transform );
	}

	@Override
	public boolean isPresent( final int t )
	{
		return source.isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		if ( filter == null  )
			return source.getSource( t, level );
		else
			return filter.filter( source.getSource( t, level ) );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		if ( filter == null  )
		{
			return source.getInterpolatedSource( t, level, method );
		}
		else
		{
			RandomAccessibleInterval< T > rai = getSource( t, level );
			RealRandomAccessible< T > interpolate = Views.interpolate( Views.extendZero( rai ), interpolators.get( method ) );
			return interpolate;
		}
	}

	@Override
	public T getType()
	{
		return source.getType();
	}

	@Override
	public String getName()
	{
		return source.getName();
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return source.getVoxelDimensions();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return source.getNumMipmapLevels();
	}

	public Source< T > getWrappedSource()
	{
		return source;
	}
}
