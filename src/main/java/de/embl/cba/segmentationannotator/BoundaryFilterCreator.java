package de.embl.cba.segmentationannotator;

import de.embl.cba.lazyalgorithm.RandomAccessibleIntervalNeighborhoodFilter;
import de.embl.cba.lazyalgorithm.converter.NeighborhoodNonZeroBoundariesConverter;
import ij.gui.GenericDialog;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.type.numeric.RealType;

public class BoundaryFilterCreator< R extends RealType< R > >
{
	public BoundaryFilterCreator( R type )
	{
	}

	public RandomAccessibleIntervalNeighborhoodFilter< R > createFilter()
	{
		final GenericDialog gd = new GenericDialog( "Boundary thickness" );
		gd.addNumericField( "Boundary thickness [pixels]", 1, 1 );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		int labelMaskBoundaryThickness = (int) gd.getNextNumber();

		final RandomAccessibleIntervalNeighborhoodFilter< R > boundaryFilter = new RandomAccessibleIntervalNeighborhoodFilter(
				new NeighborhoodNonZeroBoundariesConverter( ),
				new HyperSphereShape( labelMaskBoundaryThickness ) );

		return boundaryFilter;
	}
}
