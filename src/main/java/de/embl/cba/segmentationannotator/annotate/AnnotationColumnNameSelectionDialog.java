package de.embl.cba.segmentationannotator.annotate;

import de.embl.cba.tables.Logger;
import ij.gui.GenericDialog;

import java.util.Set;

public class AnnotationColumnNameSelectionDialog
{
	final Set< String > columnNames;
	private String annotationColumnName;

	public AnnotationColumnNameSelectionDialog( Set< String > columnNames )
	{
		this.columnNames = columnNames;
	}

	public boolean show()
	{
		final GenericDialog gd = new GenericDialog( "" );
		gd.addStringField( "Annotation column name", "Annotation", 30 );
		gd.showDialog();
		if ( gd.wasCanceled() ) return false;
		final String columnName = gd.getNextString();
		if ( columnNames.contains( columnName ) )
		{
			Logger.error( "The column \"" + columnName + "\" exists already, please choose another one." );
			return false;
		}
		annotationColumnName = columnName;
		return true;
	}

	public String getAnnotationColumnName()
	{
		return annotationColumnName;
	}
}
