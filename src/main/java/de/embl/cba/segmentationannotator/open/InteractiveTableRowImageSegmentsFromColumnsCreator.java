package de.embl.cba.segmentationannotator.open;

import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentPropertyColumnsSelectionDialog;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.gui.GenericDialog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.tables.imagesegment.SegmentPropertyColumnsSelectionDialog.NO_COLUMN_SELECTED;

public class InteractiveTableRowImageSegmentsFromColumnsCreator
{
	private final Map< String, List< String > > columnNameToColumns;
	private String labelImageColumnName;
	private List< TableRowImageSegment > segments;

	public InteractiveTableRowImageSegmentsFromColumnsCreator( Map< String, List< String > > columnNameToColumns )
	{
		this.columnNameToColumns = columnNameToColumns;
		run();
	}

	private void run()
	{
		// TODO: Ask here about whether or not time points are one or zero based!
		final SegmentPropertyColumnsSelectionDialog selectionDialog = new SegmentPropertyColumnsSelectionDialog( columnNameToColumns.keySet() );
		Map< SegmentProperty, String > segmentPropertyToColumnName = selectionDialog.fetchUserInput();

		boolean isOneBasedTimePoint = isOneBasedTimePoint( segmentPropertyToColumnName );
		labelImageColumnName = segmentPropertyToColumnName.get( SegmentProperty.LabelImage );

		final Map< SegmentProperty, List< String > > segmentPropertyToColumn = createSegmentPropertyToColumnMap( segmentPropertyToColumnName, columnNameToColumns );

		segments = SegmentUtils.tableRowImageSegmentsFromColumns( columnNameToColumns, segmentPropertyToColumn, isOneBasedTimePoint );
	}

	private boolean isOneBasedTimePoint( Map< SegmentProperty, String > segmentPropertyToColumnName )
	{
		boolean isOneBasedTimePoint = false;
		if ( ! segmentPropertyToColumnName.get( SegmentProperty.T ).equals( NO_COLUMN_SELECTED ) )
		{
			final Double minTimePoint = columnNameToColumns.get( segmentPropertyToColumnName.get( SegmentProperty.T ) ).stream().map( s -> Double.parseDouble( s ) ).min( Double::compare ).get();
			isOneBasedTimePoint = minTimePoint == 0 ? false : true;
		}
		return isOneBasedTimePoint;
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

	public List< TableRowImageSegment > getSegments()
	{
		return segments;
	}

	public String getLabelImageColumnName()
	{
		return labelImageColumnName;
	}
}
