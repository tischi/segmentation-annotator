/*-
 * #%L
 * TODO
 * %%
 * Copyright (C) 2018 - 2020 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.embl.cba.ide;

import de.embl.cba.tables.Logger;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentPropertyColumnsSelectionDialog;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.embl.cba.tables.imagesegment.SegmentPropertyColumnsSelectionDialog.NO_COLUMN_SELECTED;

public class TableRowImageSegmentsFromTableCreator
{
	private final String tablePath;
	private boolean isOneBasedTimePoint; // ...or zero based

	public TableRowImageSegmentsFromTableCreator( String tablePath, boolean isOneBasedTimePoint )
	{
		this.tablePath = tablePath;
		this.isOneBasedTimePoint = isOneBasedTimePoint;
	}

	public List< TableRowImageSegment > createTableRows()
	{
		Logger.info("Creating image segments table from file: " + tablePath );

		final List< TableRowImageSegment > tableRowImageSegments = createTableRowImageSegments( tablePath, isOneBasedTimePoint );

		return tableRowImageSegments;
	}

	private static List< TableRowImageSegment > createTableRowImageSegments( String tablePath, boolean isOneBasedTimePoint )
	{
		Map< String, List< String > > columnNameToColumnEntries = TableColumns.stringColumnsFromTableFile( tablePath );

		final SegmentPropertyColumnsSelectionDialog selectionDialog = new SegmentPropertyColumnsSelectionDialog( columnNameToColumnEntries.keySet() );
		Map< SegmentProperty, String > segmentPropertyToColumnName = selectionDialog.fetchUserInput();

		final Map< SegmentProperty, List< String > > segmentPropertyToColumnEntries = createSegmentPropertyToColumnEntriesMap( segmentPropertyToColumnName, columnNameToColumnEntries  );

		final List< TableRowImageSegment > segments = SegmentUtils.tableRowImageSegmentsFromColumns( columnNameToColumnEntries, segmentPropertyToColumnEntries, isOneBasedTimePoint );

		return segments;
	}

	private static Map< SegmentProperty, List< String > > createSegmentPropertyToColumnEntriesMap( Map< SegmentProperty, String > segmentPropertyToColumnName, Map< String, List< String > > columnNameToColumnEntries )
	{
		final Map< SegmentProperty, List< String > > segmentPropertyToColumnEntries = new LinkedHashMap<>();

		for( SegmentProperty property : segmentPropertyToColumnName.keySet() )
		{
			String columnName = segmentPropertyToColumnName.get( property );

			if ( columnName.equals( NO_COLUMN_SELECTED ) )
				continue;

			segmentPropertyToColumnEntries.put( property, columnNameToColumnEntries.get( columnName ) );
		}

		return segmentPropertyToColumnEntries;
	}
}
