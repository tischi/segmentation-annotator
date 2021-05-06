package de.embl.cba.segmentationannotator.open;

import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentPropertyColumnsSelectionDialog;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.tables.imagesegment.SegmentPropertyColumnsSelectionDialog.NO_COLUMN_SELECTED;

public class InteractiveTableRowImageSegmentsFromColumnsCreator
{
	private final Map< String, List< String > > columnNameToColumns;
	private String labelImageColumnName;
	private List< TableRowImageSegment > tableRowImageSegments;

	public InteractiveTableRowImageSegmentsFromColumnsCreator( Map< String, List< String > > columnNameToColumns )
	{
		this.columnNameToColumns = columnNameToColumns;
		run();
	}

	private void run()
	{
		// TODO: Ask here about whether or not time points are one or zero based!
		final SegmentPropertyColumnsSelectionDialog selectionDialog = new SegmentPropertyColumnsSelectionDialog( columnNameToColumns.keySet() );
		boolean isOneBasedTimePoint = false;
		Map< SegmentProperty, String > segmentPropertyToColumnName = selectionDialog.fetchUserInput();

		labelImageColumnName = segmentPropertyToColumnName.get( SegmentProperty.LabelImage );

		final Map< SegmentProperty, List< String > > segmentPropertyToColumn = createSegmentPropertyToColumnMap( segmentPropertyToColumnName, columnNameToColumns );

		tableRowImageSegments = SegmentUtils.tableRowImageSegmentsFromColumns( columnNameToColumns, segmentPropertyToColumn, isOneBasedTimePoint );
	}

	private static Map< SegmentProperty, List< String > > createSegmentPropertyToColumnMap( Map< SegmentProperty, String > segmentPropertyToColumnName, Map< String, List< String > > columnNameToColumnEntries )
	{
		final Map< SegmentProperty, List< String > > segmentPropertyToColumn = new LinkedHashMap<>();

		for( SegmentProperty property : segmentPropertyToColumnName.keySet() )
		{
			String columnName = segmentPropertyToColumnName.get( property );

			if ( columnName.equals( NO_COLUMN_SELECTED ) )
				continue;

			segmentPropertyToColumn.put( property, columnNameToColumnEntries.get( columnName ) );
		}

		return segmentPropertyToColumn;
	}

	public List< TableRowImageSegment > getTableRowImageSegments()
	{
		return tableRowImageSegments;
	}

	public String getLabelImageColumnName()
	{
		return labelImageColumnName;
	}
}
