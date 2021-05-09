package de.embl.cba.segmentationannotator.classify;

import ij.gui.GenericDialog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ClassifyDialog
{
	private final Supplier< Set< String > > columnNames;
	private String annotationColumn;
	private Set< String > featureColumns;
	private boolean[] isColumnSelectedArray;
	private String[] columnNamesArray;
	private int numColumns;

	public ClassifyDialog( Supplier< Set< String > > columnNameSupplier )
	{
		this.columnNames = columnNameSupplier;
		init();
	}

	private void init()
	{
		annotationColumn = columnNames.get().iterator().next();
		columnNamesArray = columnNames.get().toArray( new String[ 0 ] );
		Arrays.sort( columnNamesArray );
		numColumns = columnNamesArray.length;
		isColumnSelectedArray = new boolean[ numColumns ];
	}

	public boolean show()
	{
		updateColumnNamesIfNecessary();

		final GenericDialog gd = new GenericDialog( "Classify" );

		// annotation column
		gd.addChoice( "Annotation column", columnNamesArray, annotationColumn );

		// feature columns
		final int sqrtN = (int) Math.ceil( Math.sqrt( numColumns ) );
		gd.addCheckboxGroup( sqrtN, sqrtN, columnNamesArray, isColumnSelectedArray );

		gd.showDialog();
		if ( gd.wasCanceled() ) return false;

		// annotation column
		annotationColumn = gd.getNextChoice();

		// feature columns
		featureColumns = new HashSet< >();
		for ( int i = 0; i < numColumns; i++ )
		{
			final boolean nextBoolean = gd.getNextBoolean();
			isColumnSelectedArray[ i ] = nextBoolean;
			if ( nextBoolean )
			{
				featureColumns.add( columnNamesArray[ i ] );
			}
		}
		return true;
	}

	private void updateColumnNamesIfNecessary()
	{
		final String[] currentColumnNames = columnNames.get().toArray( new String[ 0 ] );
		Arrays.sort( currentColumnNames );

		if ( ! Arrays.equals( currentColumnNames, columnNamesArray ) )
		{
			init();
		}
	}

	public String getAnnotationColumn()
	{
		return annotationColumn;
	}

	public Set< String > getFeatureColumns()
	{
		// the order matter for the classification!
		return featureColumns;
	}
}
