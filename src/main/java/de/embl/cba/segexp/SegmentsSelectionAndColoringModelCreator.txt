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
package de.embl.cba.segexp;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.SegmentsBdvView;
import de.embl.cba.tables.view.TableRowsTableView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.List;

public class SegmentsSelectionAndColoringModelCreator< R extends RealType< R > & NativeType< R > >
{
	private final List< TableRowImageSegment > tableRowImageSegments;
	private final List< SourceAndConverter< ? > > imageSourcesModel;
	private final String name;
	private SegmentsBdvView< TableRowImageSegment > segmentsBdvView;
	private TableRowsTableView< TableRowImageSegment > tableRowsTableView;
	private SelectionColoringModel< TableRowImageSegment > selectionColoringModel;
	private SelectionModel< TableRowImageSegment > selectionModel;
	private LazyCategoryColoringModel< TableRowImageSegment > coloringModel;

	public SegmentsSelectionAndColoringModelCreator(
			List< TableRowImageSegment > tableRowImageSegments,
			ImageSourcesModel imageSourcesModel,
			String name )
	{
		this( tableRowImageSegments, imageSourcesModel, name, null );
	}

	public SegmentsSelectionAndColoringModelCreator(
			List< TableRowImageSegment > tableRowImageSegments,
			List< SourceAndConverter< ? > > sourceAndConverters,
			String name,
			BdvHandle bdvHandle // in order to attach this view to an already existing bdv
	)
	{
		this.tableRowImageSegments = tableRowImageSegments;
		this.imageSourcesModel = sourceAndConverters;
		this.name = name;
		show( bdvHandle );
	}

	private void show( BdvHandle bdv )
	{
		selectionModel = new DefaultSelectionModel<>();
		coloringModel = new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) );
		selectionColoringModel = new SelectionColoringModel<>( coloringModel, selectionModel );

		segmentsBdvView = new SegmentsBdvView< TableRowImageSegment >(
				tableRowImageSegments,
				selectionModel,
				selectionColoringModel,
				imageSourcesModel,
				bdv );

		tableRowsTableView = new TableRowsTableView< TableRowImageSegment >(
				tableRowImageSegments,
				selectionModel,
				selectionColoringModel );

		tableRowsTableView.showTableAndMenu( segmentsBdvView.getBdv().getViewerPanel() );
	}

	public SelectionModel< TableRowImageSegment > getSelectionModel()
	{
		return selectionModel;
	}

	public SelectionColoringModel< TableRowImageSegment > getSelectionColoringModel()
	{
		return selectionColoringModel;
	}

	public SegmentsBdvView< TableRowImageSegment > getSegmentsBdvView()
	{
		return segmentsBdvView;
	}

	public TableRowsTableView< TableRowImageSegment > getTableRowsTableView()
	{
		return tableRowsTableView;
	}

	public void close()
	{
		segmentsBdvView.close();
		tableRowsTableView.close();

		segmentsBdvView = null;
		tableRowsTableView = null;

		System.gc();
	}
}
