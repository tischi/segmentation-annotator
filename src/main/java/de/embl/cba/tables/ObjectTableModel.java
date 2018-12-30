package de.embl.cba.tables;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

public class ObjectTableModel extends DefaultTableModel
{
	ArrayList< Class > columnClasses;

	@Override
	public Class getColumnClass( int column )
	{
		return columnClasses.get( column );
	}

	@Override
	public boolean isCellEditable( int row, int column )
	{
		return false;
	}

	public void setColumnClassesFromFirstRow()
	{
		columnClasses = new ArrayList<>(  );

		for ( int column = 0; column < getColumnCount(); column++ )
		{
			final Object value = this.getValueAt( 1, column );

			columnClasses.add( value.getClass() );
		}


	}

}
