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

import bdv.tools.brightness.ConverterSetup;
import bdv.util.*;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.bdv.utils.sources.ImagePlusFileSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.bdv.utils.sources.ModifiableRandomAccessibleIntervalSource4D;
import de.embl.cba.lazyalgorithm.RandomAccessibleIntervalNeighborhoodFilter;
import de.embl.cba.lazyalgorithm.converter.NeighborhoodNonZeroBoundariesConverter;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.color.*;
import de.embl.cba.tables.image.SourceAndMetadata;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import ij.gui.GenericDialog;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.jetbrains.annotations.NotNull;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;

import java.util.*;

import static de.embl.cba.bdv.utils.converters.SelectableVolatileARGBConverter.BACKGROUND;

// TODO: reconsider what a "segment" needs to be here
public class SegmentedImagesViewer< T extends ImageSegment >
{
	private final SelectionColoringModel< T > selectionColoringModel;
	private HashMap< String, Set< SourceAndConverter< ? > > > groupNameToSources;
	private HashMap< SourceAndConverter< ? >, String > sourceToLabelImageId;
	private final SelectionModel< T > selectionModel;

	private String selectTrigger = "ctrl button1";
	private String selectNoneTrigger = "ctrl shift N";
	private String incrementCategoricalLutRandomSeedTrigger = "ctrl L";
	private String labelMaskAsBinaryMaskTrigger = "ctrl M";
	private String labelMaskAsBoundaryTrigger = "ctrl B";
	private String iterateSelectionModeTrigger = "ctrl S";
	private String viewIn3DTrigger = "ctrl shift button1";

	private Behaviours behaviours;

	private BdvHandle bdvHandle;
	private String segmentsName;
	private BdvOptions bdvOptions;
	private LabelsARGBConverter labelsSourceConverter;

	private T recentFocus;
	private ViewerState recentViewerState;
	private List< ConverterSetup > recentConverterSetups;
	private double voxelSpacing3DView;
	private Set< SourceAndMetadata< ? extends RealType< ? > > > currentSources;
	private boolean grayValueOverlayWasFirstSource;
	private HashMap< LabelFrameAndImage, T > labelFrameAndImageToSegment;
	private List< T > segments;
	private int segmentFocusAnimationDurationMillis;
	private ARGBType labelSourceSingleColor;
	private boolean isLabelMaskShownAsBinaryMask;
	private boolean isLabelMaskShownAsBoundaries;
	private int labelMaskBoundaryThickness;
	private Set< String > popupActionNames;
	private Set< SourceAndConverter< ? > > labelSources;

	public SegmentedImagesViewer(
			final List segments,
			final SelectionColoringModel selectionColoringModel,
			final HashMap< String, Set< SourceAndConverter< ? > > > groupNameToSources,
			final HashMap< SourceAndConverter< ? >, String > sourceToLabelImageId )
	{
		this.segments = segments;
		this.selectionColoringModel = selectionColoringModel;
		this.selectionModel = selectionColoringModel.getSelectionModel();
		this.groupNameToSources = groupNameToSources;
		this.sourceToLabelImageId = sourceToLabelImageId;

		this.labelSourceSingleColor = new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) );;
		this.isLabelMaskShownAsBinaryMask = false;

		this.voxelSpacing3DView = 0.1;
		this.segmentFocusAnimationDurationMillis = 750;
		this.currentSources = new HashSet<>( );
		this.popupActionNames = new HashSet<>( );

		initSegments( segments );
		configureLabelMaskSources();
		initBdvOptions();
		showImages();

		registerAsSelectionListener( this.selectionColoringModel.getSelectionModel() );
		registerAsColoringListener( this.selectionColoringModel );

		installBdvBehavioursAndPopupMenu();
	}

	private void configureLabelMaskSources()
	{
		Set< SourceAndConverter< ? > > labelsSources = new HashSet<>( sourceToLabelImageId.keySet() );

		labelsSources.stream().forEach( source ->
		{
				String labelImageId = sourceToLabelImageId.get( source );
				SegmentsARGBConverter labelsARGBConverter = new SegmentsARGBConverter(
						labelFrameAndImageToSegment,
						labelImageId,
						selectionColoringModel );

				SourceAndConverter< ? > labelMaskSource = new SourceAndConverter<>( source.getSpimSource(), labelsARGBConverter );

				// the source object has changed => replace in the map
				sourceToLabelImageId.remove( source );
				sourceToLabelImageId.put(labelMaskSource, labelImageId );
		} );
	}

	private void showImages()
	{
		bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

		groupNameToSources.keySet().forEach( groupName ->
		{
			groupNameToSources.get( groupName ).forEach( source ->
			{
				// group in terms of B&C
				SourceAndConverterServices.getSourceAndConverterDisplayService().show( bdvHandle, source );
				new ViewerTransformAdjuster( bdvHandle, source ).run();
				new BrightnessAutoAdjuster( source, 0 ).run();
			} );
		} );

		sourceToLabelImageId.keySet().forEach( source -> {
			SourceAndConverterServices.getSourceAndConverterDisplayService().show( bdvHandle, source );
			new ViewerTransformAdjuster( bdvHandle, source ).run();
			new BrightnessAutoAdjuster( source, 0 ).run();
		} );
	}

	private void initSegments( List< T > segments )
	{
		if ( segments != null )
		{
			this.segments = segments;
			this.labelFrameAndImageToSegment = SegmentUtils.createSegmentMap( this.segments );
		}
	}

	private void registerAsColoringListener( ColoringModel< T > coloringModel )
	{
		coloringModel.listeners().add( () -> BdvUtils.repaint( bdvHandle ) );
	}

	private void registerAsSelectionListener( SelectionModel< T > selectionModel )
	{
		selectionModel.listeners().add( new SelectionListener< T >()
		{
			@Override
			public void selectionChanged()
			{
				BdvUtils.repaint( bdvHandle );
			}

			@Override
			public void focusEvent( T selection )
			{
				if ( recentFocus != null && selection == recentFocus )
				{
					return;
				}
				else
				{
					recentFocus = selection;
					focusSegment( selection );
				}
			}
		} );
	}

	public synchronized void focusSegment( ImageSegment imageSegment )
	{
		final double[] position = new double[ 3 ];
		imageSegment.localize( position );

		BdvUtils.moveToPosition(
				bdvHandle,
				position,
				imageSegment.timePoint(),
				segmentFocusAnimationDurationMillis );
	}

	public void setSegmentFocusAnimationDurationMillis( int duration )
	{
		this.segmentFocusAnimationDurationMillis = duration;
	}

	public BdvStackSource showSource( SourceAndMetadata sourceAndMetadata, int numTimePoints )
	{
		final Metadata metadata = sourceAndMetadata.metadata();
		Source< ? > source = sourceAndMetadata.source();

		if ( isLabelSource( metadata) )
		{
			source = asLabelsSource( sourceAndMetadata );
			if ( isLabelMaskShownAsBoundaries ) showLabelMaskAsBoundaries();
		}

		final BdvStackSource bdvStackSource = BdvFunctions.show(
				source,
				numTimePoints,
				bdvOptions );

		bdvStackSource.setActive( true );

		if ( metadata.contrastLimits != null )
			bdvStackSource.setDisplayRange( metadata.contrastLimits[ 0 ], metadata.contrastLimits[ 1 ] );

		// TODO: Implement auto-contrast; Take code from bdp2

		bdvHandle = bdvStackSource.getBdvHandle();

		bdvOptions = bdvOptions.addTo( bdvHandle );

		metadata.bdvStackSource = bdvStackSource;

		currentSources.add( sourceAndMetadata );

		if ( labelsSourceConverter != null )
			bdvHandle.getViewerPanel().addTimePointListener( labelsSourceConverter );

		return bdvStackSource;
	}

	// TODO: clean this up! There should only be one way to decide whether this is a labelSource
	public boolean isLabelSource( Metadata metadata )
	{
		if ( metadata.type != null && metadata.type.equals( Metadata.Type.Segmentation ) )
		{
				return true;
		}
		else if ( metadata.modality == Metadata.Modality.Segmentation  )
		{
			return true;
		}
		else
		{
			return false;
		}

	}

	public int getNumTimePoints( Source< ? > source )
	{
		int numTimePoints = 0;
		while ( source.isPresent( numTimePoints++ ) ){}
		return numTimePoints - 1;
	}

	public void removeSource( BdvStackSource bdvStackSource )
	{
		currentSources.remove( bdvStackSource );
		BdvUtils.removeSource( bdvHandle, bdvStackSource );
	}

	public List< SourceAndMetadata< ? extends RealType< ? > > > getCurrentSources()
	{
		return new ArrayList<>( currentSources );
	}

	private void removeSources()
	{
		recentViewerState = bdvHandle.getViewerPanel().getState();
		recentConverterSetups = new ArrayList<>( bdvHandle.getSetupAssignments().getConverterSetups() );

		final List< SourceState< ? > > sources = recentViewerState.getSources();
		final int numSources = sources.size();

		for ( int i = 0; i < numSources; ++i )
		{
			final Source< ? > source = sources.get( i ).getSpimSource();
			if ( source instanceof PlaceHolderSource )
			{
				if ( i == 0 )
					grayValueOverlayWasFirstSource = true;
			}
			else
			{
				bdvHandle.getViewerPanel().removeSource( source );
				bdvHandle.getSetupAssignments().removeSetup( recentConverterSetups.get( i ) );
			}
		}
	}


	/**
	 * Currently, the logic is that there can be only one labels source
	 * within this SegmentsBdvView class. Thus, creation of the labels source
	 * also registers it as "the" labelsSource
	 *
	 * @param sourceAndMetadata
	 * @return
	 */
	private Source asLabelsSource( SourceAndMetadata< ? extends RealType< ? > > sourceAndMetadata )
	{
		this.labelsSource = sourceAndMetadata;

		final LabelsARGBConverter labelsARGBConverter = createLabelsARGBConverter( labelsSource );

		this.labelsSourceConverter = labelsARGBConverter;

		final ARGBConvertedRealSource convertedRealSource = new ARGBConvertedRealSource( sourceAndMetadata.source(), labelsARGBConverter );

		return convertedRealSource;
	}

	private LabelsARGBConverter createLabelsARGBConverter( SourceAndMetadata labelsSource )
	{
		LabelsARGBConverter labelsARGBConverter;

		if ( labelFrameAndImageToSegment == null )
		{
			labelsARGBConverter = new LazyLabelsARGBConverter();
		}
		else
		{
			labelsARGBConverter =
					new SegmentsARGBConverter(
							labelFrameAndImageToSegment,
							labelsSource.metadata().imageId,
							selectionColoringModel );

		}

		return labelsARGBConverter;
	}

	private void initBdvOptions( )
	{
		bdvOptions = BdvOptions.options();

		//if ( sourceAndConverters.is2D() )
			bdvOptions = bdvOptions.is2D();

		if ( bdvHandle != null )
			bdvOptions = bdvOptions.addTo( bdvHandle );
	}

	private void installBdvBehavioursAndPopupMenu()
	{
		segmentsName = segments.toString();

		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getBdvHandle().getTriggerbindings(), segmentsName + "-bdv-select-handler" );

		installSelectionBehaviour();
		installUndoSelectionBehaviour();
		installSelectionColoringModeBehaviour(); // TODO: maybe move to popup menu
		installRandomColorShufflingBehaviour();
		installShowLabelMaskAsBinaryMaskBehaviour();
		installShowLabelMaskAsBoundaryBehaviour();
		// install3DViewBehaviour(); // TODO: maybe move to popup menu

		addUndoSelectionPopupMenu();
		addSelectionColoringModePopupMenu();
		addAnimationSettingsPopupMenu();
	}

	private void addAnimationSettingsPopupMenu()
	{
		final ArrayList< String > menuNames = new ArrayList<>();
		menuNames.add( getLabelImageMenuName() );
		final String actionName = "Segment Animation Settings...";
		popupActionNames.add( BdvPopupMenus.getCombinedMenuActionName(  menuNames, actionName ) );
		BdvPopupMenus.addAction(
				bdvHandle,
				menuNames,
				actionName,
				( x, y ) -> new Thread( () -> changeAnimationSettingsUI() ).start()
			);
	}

	// TODO: put all the menu stuff into an own class at some point
	private void addUndoSelectionPopupMenu()
	{
		final ArrayList< String > menuNames = new ArrayList<>();
		menuNames.add( getLabelImageMenuName() );

		final String actionName = "Undo Segment Selections" + BdvUtils.getShortCutString( selectNoneTrigger );
		popupActionNames.add( BdvPopupMenus.getCombinedMenuActionName(  menuNames, actionName ) );
		BdvPopupMenus.addAction(
				bdvHandle,
				menuNames,
				actionName,
				( x, y ) -> new Thread( () -> selectNone() ).start()
		);
	}

	private void addSelectionColoringModePopupMenu()
	{
		final ArrayList< String > menuNames = new ArrayList<>();
		menuNames.add( getLabelImageMenuName() );
		menuNames.add( "Segment Selection Coloring Mode" );

		final SelectionColoringModel.SelectionColoringMode[] selectionColoringModes = SelectionColoringModel.SelectionColoringMode.values();

		for ( SelectionColoringModel.SelectionColoringMode mode : selectionColoringModes )
		{
			final String actionName = mode.toString();
			popupActionNames.add( BdvPopupMenus.getCombinedMenuActionName( menuNames, actionName ) );

			BdvPopupMenus.addAction(
					bdvHandle,
					menuNames,
					actionName,
					( x, y ) -> new Thread( () -> selectionColoringModel.setSelectionColoringMode( mode ) ).start()
			);
		}
	}

	@NotNull
	private String getLabelImageMenuName()
	{
		return "Label Masks";
	}


	private void changeAnimationSettingsUI()
	{
		final GenericDialog genericDialog = new GenericDialog( "Segment animation settings" );
		genericDialog.addNumericField( "Segment Focus Animation Duration [ms]", segmentFocusAnimationDurationMillis, 0 );
		genericDialog.showDialog();
		if ( genericDialog.wasCanceled() ) return;
		segmentFocusAnimationDurationMillis = ( int ) genericDialog.getNextNumber();
	}

	private void installRandomColorShufflingBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> shuffleRandomColors() ).start(),
					segmentsName + "-change-color-random-seed",
						incrementCategoricalLutRandomSeedTrigger );
	}

	private void installShowLabelMaskAsBinaryMaskBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> toggleLabelMaskAsBinaryMask() ).start(),
				segmentsName + "-asMask",
				labelMaskAsBinaryMaskTrigger );
	}

	private void installShowLabelMaskAsBoundaryBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> toggleLabelMaskAsBoundaries() ).start(),
				segmentsName + "-asBoundaries",
				labelMaskAsBoundaryTrigger );
	}

	private synchronized void shuffleRandomColors()
	{
		if ( ! isLabelSourceActive() ) return;

		final ColoringModel< T > coloringModel =
				selectionColoringModel.getColoringModel();

		if ( coloringModel instanceof CategoryColoringModel )
		{
			( ( CategoryColoringModel ) coloringModel ).incRandomSeed();
			BdvUtils.repaint( bdvHandle );
		}
	}

	private synchronized void toggleLabelMaskAsBinaryMask()
	{
		if ( ! isLabelSourceActive() ) return;

		if ( isLabelMaskShownAsBinaryMask )
			labelsSourceConverter.setSingleColor( null );
		else
			labelsSourceConverter.setSingleColor( labelSourceSingleColor );

		isLabelMaskShownAsBinaryMask = !isLabelMaskShownAsBinaryMask;

		BdvUtils.repaint( bdvHandle );
	}

	private synchronized void toggleLabelMaskAsBoundaries()
	{
		if ( ! isLabelSourceActive() ) return;

		final ModifiableRandomAccessibleIntervalSource4D modifiableSource = getModifiableSource( labelsSource.source() );

		if ( modifiableSource == null ) return;

		if ( ! isLabelMaskShownAsBoundaries )
		{
			final GenericDialog gd = new GenericDialog( "Boundary thickness" );
			gd.addNumericField( "Boundary thickness [pixels]", 1, 1 );
			gd.showDialog();
			if ( gd.wasCanceled() ) return;
			labelMaskBoundaryThickness = (int) gd.getNextNumber();
		}

		final RandomAccessibleIntervalNeighborhoodFilter filter = new RandomAccessibleIntervalNeighborhoodFilter(
				new NeighborhoodNonZeroBoundariesConverter( ),
				new HyperSphereShape( labelMaskBoundaryThickness ) );

		if ( isLabelMaskShownAsBoundaries )
			modifiableSource.setFilter( null );
		else
			modifiableSource.setFilter( filter );

		isLabelMaskShownAsBoundaries = ! isLabelMaskShownAsBoundaries;

		BdvUtils.repaint( bdvHandle );
	}

	private void showLabelMaskAsBoundaries()
	{
		final ModifiableRandomAccessibleIntervalSource4D modifiableSource = getModifiableSource( labelsSource.source() );
		if ( modifiableSource == null ) return;
		final RandomAccessibleIntervalNeighborhoodFilter filter = new RandomAccessibleIntervalNeighborhoodFilter(
				new NeighborhoodNonZeroBoundariesConverter( ),
				new HyperSphereShape( labelMaskBoundaryThickness ) );
		modifiableSource.setFilter( filter );
	}

	public ModifiableRandomAccessibleIntervalSource4D getModifiableSource( Source< ? > source )
	{
		if ( source instanceof ModifiableRandomAccessibleIntervalSource4D )
			return ( ModifiableRandomAccessibleIntervalSource4D ) source;
		else if ( source instanceof ImagePlusFileSource )
			return  ( ModifiableRandomAccessibleIntervalSource4D ) ( ( ImagePlusFileSource ) source ).getWrappedSource()   ;
		else
		{
			Logger.warn( "Cannot create boundaries of label mask of type: " + labelsSource.source().getClass().toString() );
			return null;
		}
	}

	public void setLabelSourceSingleColor( ARGBType labelSourceSingleColor )
	{
		this.labelSourceSingleColor = labelSourceSingleColor;
		labelsSourceConverter.setSingleColor( labelSourceSingleColor );
		isLabelMaskShownAsBinaryMask = true;
		BdvUtils.repaint( bdvHandle );
	}

	private void installUndoSelectionBehaviour( )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
				new Thread( () -> selectNone() ).start(),
				segmentsName + "-select-none", selectNoneTrigger );
	}

	public synchronized void selectNone()
	{
		if ( ! isLabelSourceActive() ) return;

		selectionModel.clearSelection( );

		BdvUtils.repaint( bdvHandle );
	}

	private void installSelectionBehaviour()
	{
		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> toggleSelectionAtMousePosition() ).start(),
				segmentsName + "-toggle-select", selectTrigger ) ;
	}

	private synchronized void toggleSelectionAtMousePosition()
	{
		final T segment = getSegmentAtCurrentMouseCoordinates( labelsSource );

		if ( segment == null ) return;

		toggleSegmenSelectionAndFocus( segment );
	}

	private void toggleSegmenSelectionAndFocus( T segment )
	{
		selectionModel.toggle( segment );

		if ( selectionModel.isSelected( segment ) )
		{
			recentFocus = segment;
			selectionModel.focus( segment );
		}
	}

	public void select( List< Double > labelIds )
	{
		List< T > segments = getSegments( labelIds );

		selectionModel.setSelected( segments, true );
	}

	private List< T > getSegments( List< Double > labelIds )
	{
		final String labelImageId = labelsSource.metadata().imageId;

		ArrayList< T > segments = new ArrayList<>(  );

		for ( Double labelId : labelIds )
		{
			final LabelFrameAndImage labelFrameAndImage =
					new LabelFrameAndImage( labelId, getCurrentTimePoint(), labelImageId );

			segments.add( labelFrameAndImageToSegment.get( labelFrameAndImage ) );
		}
		return segments;
	}

	private boolean isLabelSourceActive()
	{
		final Source< ? > source
				= labelsSource.metadata().bdvStackSource
				.getSources().get( 0 ).getSpimSource();

		final boolean active = BdvUtils.isActive( bdvHandle, source );

		return active;
	}

	private T getSegmentAtCurrentMouseCoordinates()
	{
		final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdvHandle );

		List< SourceAndConverter< ? > > sources = new SourcesGetter( bdvHandle ).getVisibleSourcesAtCurrentMousePosition();

		for ( SourceAndConverter< ? > source : sources )
		{
			if ( labelSources.contains( source ) )
			{
				final Double labelIndex = BdvUtils.getPixelValue( source.getSpimSource(), globalMouseCoordinates, getCurrentTimePoint() );

				if ( labelIndex == null )
					throw new RuntimeException( "Could not determine the pixel value of source " + source.getSpimSource().getName() + " at position " + globalMouseCoordinates );

				if ( labelIndex == BACKGROUND ) return null;

				final String labelImageId = source.getSpimSource().getName();

				final LabelFrameAndImage labelFrameAndImage = new LabelFrameAndImage( labelIndex, getCurrentTimePoint(), labelImageId );

				final T segment = labelFrameAndImageToSegment.get( labelFrameAndImage );

				return segment;
			}
		}

		return null;
	}

	private void installSelectionColoringModeBehaviour( )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
				new Thread( () ->
				{
					if ( ! isLabelSourceActive() ) return;
					selectionColoringModel.iterateSelectionMode();
					BdvUtils.repaint( bdvHandle );
				} ).start(),
				segmentsName + "-iterate-select", iterateSelectionModeTrigger );
	}

	private int getCurrentTimePoint()
	{
		return bdvHandle.getViewerPanel().state().getCurrentTimepoint();
	}

	public void close()
	{
		for ( String popupActionName : popupActionNames )
		{
			BdvPopupMenus.removeAction( bdvHandle, popupActionName );
		}
	}
}
