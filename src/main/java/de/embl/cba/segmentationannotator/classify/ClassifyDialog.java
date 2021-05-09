package de.embl.cba.segmentationannotator.classify;

import ij.gui.GenericDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassifyDialog
{
	private final Collection< String > columnNames;
	private String annotationColumnName;
	private Collection< String > featureColumns;
	private boolean[] isColumnSelectedArray;
	private String[] columnNamesArray;
	private int numColumns;

	public ClassifyDialog( Collection< String > columnNames )
	{
		this.columnNames = columnNames;
		init();
	}

	private void init()
	{
		annotationColumnName = columnNames.iterator().next();
		columnNamesArray = columnNames.toArray( new String[ 0 ] );
		numColumns = columnNamesArray.length;
		isColumnSelectedArray = new boolean[ numColumns ];
	}

	public void showDialog()
	{
		if ( ! Arrays.equals( columnNames.toArray( new String[ 0 ] ), columnNamesArray ) )
		{
			// column names have changed => reinitialize
			init();
		}

		final GenericDialog gd = new GenericDialog( "Classify" );

		// annotation column
		gd.addChoice( "Annotation column", columnNamesArray, annotationColumnName );

		// feature columns
		final int sqrtN = (int) Math.ceil( Math.sqrt( numColumns ) );
		gd.addCheckboxGroup( sqrtN, sqrtN, columnNamesArray, isColumnSelectedArray );

		gd.showDialog();
		if ( gd.wasCanceled() ) return;

		// annotation column
		annotationColumnName = gd.getNextChoice();

		// feature columns
		featureColumns = new HashSet<>();
		for ( int i = 0; i < numColumns; i++ )
		{
			final boolean nextBoolean = gd.getNextBoolean();
			isColumnSelectedArray[ i ] = nextBoolean;
			if ( nextBoolean )
			{
				featureColumns.add( columnNamesArray[ i ] );
			}
		}
	}

	public String getAnnotationColumnName()
	{
		return annotationColumnName;
	}

	public Collection< String > getFeatureColumns()
	{
		return featureColumns;
	}
}
