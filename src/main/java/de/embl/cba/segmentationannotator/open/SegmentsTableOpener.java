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
package de.embl.cba.segmentationannotator.open;

import de.embl.cba.tables.Logger;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.List;
import java.util.Map;

public class SegmentsTableOpener
{
	private final String tablePath;
	private String labelImageColumnName;

	public SegmentsTableOpener( String tablePath )
	{
		this.tablePath = tablePath;
	}

	public List< TableRowImageSegment > createSegments()
	{
		Logger.info("Creating image segments table from file: " + tablePath );

		final List< TableRowImageSegment > tableRowImageSegments = createTableRowImageSegments( tablePath );

		return tableRowImageSegments;
	}

	public String getLabelImageColumnName()
	{
		return labelImageColumnName;
	}

	private List< TableRowImageSegment > createTableRowImageSegments( String tablePath )
	{
		Map< String, List< String > > columnNameToColumnEntries = TableColumns.stringColumnsFromTableFile( tablePath );

		final InteractiveTableRowImageSegmentsFromColumnsCreator tableRowsCreator = new InteractiveTableRowImageSegmentsFromColumnsCreator( columnNameToColumnEntries );

		final List< TableRowImageSegment > segments = tableRowsCreator.getTableRowImageSegments();

		return segments;
	}
}
