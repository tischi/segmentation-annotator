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
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.*;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.tables.color.CategoryColoringModel;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import ij.gui.GenericDialog;
import net.imglib2.RealPoint;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.jetbrains.annotations.NotNull;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

// TODO: reconsider what a "segment" needs to be here
public class SegmentedImagesView< T extends ImageSegment, R extends NumericType< R > & RealType< R > >
{
	public static final int BACKGROUND = 0;

	private final SelectionColoringModel< T > selectionColoringModel;
	private final SelectionModel< T > selectionModel;
	private final HashMap< SourceAndConverter< R >, SourceMetadata > rawSourceToMetadata;
	private HashMap< SourceAndConverter< R >, SourceMetadata > sourceToMetadata;

	private static String selectTrigger = "ctrl button1";
	private static final String selectNoneTrigger = "ctrl shift N";
	private static final String incrementCategoricalLutRandomSeedTrigger = "ctrl L";
	private static String labelMaskAsBinaryMaskTrigger = "ctrl M";
	private static String labelMaskAsBoundaryTrigger = "ctrl B";
	private static String iterateSelectionModeTrigger = "ctrl S";
	private static String viewIn3DTrigger = "ctrl shift button1";

	private Behaviours behaviours;
	private BdvHandle bdvHandle;
	private String name;
	private T recentFocus;
	private HashMap< LabelFrameAndImage, T > labelFrameAndImageToSegment;
	private int segmentFocusAnimationDurationMillis = 750;
	private boolean labelMasksShownAsBoundaries = false;
	private Set< String > popupActionNames = new HashSet<>(  );
	private boolean is2D;
	private SynchronizedViewerState state;

	public SegmentedImagesView( final List< T > tableRowImageSegments, final SelectionColoringModel< T > selectionColoringModel, final HashMap< SourceAndConverter< R >, SourceMetadata > sourceToMetadata )
	{
		this.selectionColoringModel = selectionColoringModel;
		this.selectionModel = selectionColoringModel.getSelectionModel();
		this.rawSourceToMetadata = sourceToMetadata;

		this.name = tableRowImageSegments.toString();
		initSegments( tableRowImageSegments );
	}

	public void showImages( boolean is2D, int numTimePoints )
	{
		this.is2D = is2D; // TODO: fetch this some other way, also the numTimePoints

		createNewBdv( numTimePoints );

		new HashSet<>( rawSourceToMetadata.keySet() );

		initSources();
		showSources(); // TODO focus on some source

		registerAsSelectionListener( this.selectionColoringModel.getSelectionModel() );
		registerAsColoringListener( this.selectionColoringModel );

		installBdvBehavioursAndPopupMenu();
	}

	private void createNewBdv( int numTimePoints )
	{
		// bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();

		ArrayImg dummyImg = ArrayImgs.bytes(2, 2, 2);

		BdvOptions bdvOptions = new BdvOptions().sourceTransform( new AffineTransform3D() );
		if ( is2D ) bdvOptions = bdvOptions.is2D();

		BdvStackSource bss = BdvFunctions.show( dummyImg, "dummy", bdvOptions );

		BdvHandle bdv = bss.getBdvHandle();

		bdv.getViewerPanel().setInterpolation( Interpolation.NEARESTNEIGHBOR );
		bdv.getViewerPanel().state().removeSource( bdv.getViewerPanel().state().getCurrentSource() );
		bdv.getViewerPanel().setNumTimepoints( numTimePoints );

		bdvHandle = bdv;
		state = bdvHandle.getViewerPanel().state();
	}

	private void initSources()
	{
		Logger.log("Initializing sources...");

		sourceToMetadata = new HashMap<>(  );

		rawSourceToMetadata.keySet().forEach( source ->
		{
			SourceMetadata metadata = rawSourceToMetadata.get( source );

			if ( is2D )
			{
				AffineTransform3D sourceTransform = new AffineTransform3D();
				source.getSpimSource().getSourceTransform( 0, 0, sourceTransform );
				double offset = sourceTransform.get( 2, 3 );
				double zScale = sourceTransform.get( 2, 2 );
				AffineTransform3D offsetToZeroTransform = new AffineTransform3D();
				offsetToZeroTransform.translate( new double[]{ 0, 0, -offset } );
				SourceAffineTransformer transformer = new SourceAffineTransformer( source, offsetToZeroTransform );
				source = transformer.getSourceOut();
			}

			if ( metadata.isPrimaryLabelSource )
			{
				source = asLabelMaskSource( source, metadata.imageId );
			}

			sourceToMetadata.put( source, metadata );
		} );
	}

	@NotNull
	private SourceAndConverter< R > asLabelMaskSource( SourceAndConverter< R > source, String labelImageId )
	{
		SegmentsRealTypeConverter segmentsConverter = new SegmentsRealTypeConverter(
			labelFrameAndImageToSegment,
			labelImageId,
			selectionColoringModel );

		bdvHandle.getViewerPanel().addTimePointListener( segmentsConverter );

		LabelMaskSource< R > labelMaskVolatileSource = new LabelMaskSource( source.asVolatile().getSpimSource() );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter<>( labelMaskVolatileSource , segmentsConverter );
		LabelMaskSource< R > labelMaskSource = new LabelMaskSource( source.getSpimSource() );
		return new SourceAndConverter( labelMaskSource, segmentsConverter, volatileSourceAndConverter );
	}

	private void showSources()
	{
		Set< SourceAndConverter< R > > sources = sourceToMetadata.keySet();

		// focus on one of the sources
		AffineTransform3D transform = new ViewerTransformAdjuster( bdvHandle, sources.iterator().next() ).getTransform();
		if ( is2D ) transform.set( 0.0, 2, 3); // zero z offset
		state.setViewerTransform( transform );

		HashMap< String, SourceGroup > groupIdToSourceGroup = new HashMap< String, SourceGroup >();

		// display all
		sources.forEach( source ->
		{
			//SourceAndConverterServices.getSourceAndConverterDisplayService().show( bdvHandle, source );

			addSourceToBdv( source );
			addSourceToGroup( groupIdToSourceGroup, source );

			//new BrightnessAutoAdjuster( source, 0 ).run(); // TODO: maybe for whole group?
		} );

		// remove all the default groups
		for ( int i = 0; i < 10; i++ )
		{
			state.removeGroup( state.getCurrentGroup() );
		}
	}

	// TODO: does not show up in UI
	private void addSourceToGroup( HashMap< String, SourceGroup > groupIdToSourceGroup, SourceAndConverter< R > source )
	{
		String groupId = sourceToMetadata.get( source ).groupId;
		if ( ! groupIdToSourceGroup.keySet().contains( groupId ) )
		{
			SourceGroup sourceGroup = new SourceGroup();
			groupIdToSourceGroup.put( groupId, sourceGroup );
			state.addGroup( sourceGroup );
			state.setGroupName( sourceGroup, groupId );
			state.setGroupActive( sourceGroup, true );
		}

		SourceGroup sourceGroup = groupIdToSourceGroup.get( groupId );
		state.addSourceToGroup( source, sourceGroup );
	}

	private void addSourceToBdv( SourceAndConverter< R > source )
	{
		// SourceAndConverterServices.getSourceAndConverterDisplayService().show( source );
		bdvHandle.getViewerPanel().state().addSource( source );
		bdvHandle.getViewerPanel().state().setSourceActive( source, true );
		ConverterSetup converterSetup = SourceAndConverterUtils.createConverterSetup( source );
		bdvHandle.getConverterSetups().put( source, converterSetup );
	}

	private void initSegments( List< T > segments )
	{
		this.labelFrameAndImageToSegment = SegmentUtils.createSegmentMap( segments );
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

		new ViewerTransformChanger(
				bdvHandle,
				sc.fiji.bdvpg.bdv.BdvUtils.getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				segmentFocusAnimationDurationMillis ).run();

	}

	public void setSegmentFocusAnimationDurationMillis( int duration )
	{
		this.segmentFocusAnimationDurationMillis = duration;
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

	private void installBdvBehavioursAndPopupMenu()
	{
		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getBdvHandle().getTriggerbindings(), name + "-bdv-select-handler" );

		installSelectionBehaviour();
		installUndoSelectionBehaviour();
		installSelectionColoringModeBehaviour();
		installRandomColorShufflingBehaviour();
		installShowLabelMaskAsBinaryMaskBehaviour();
		installShowLabelMaskAsBoundaryBehaviour();

		addUndoSelectionPopupMenu();
		addSelectionColoringModePopupMenu();
		addShowLabelMaskAsBoundaryPopupMenu();
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

	private void addShowLabelMaskAsBoundaryPopupMenu()
	{
		final ArrayList< String > menuNames = new ArrayList<>();
		menuNames.add( getLabelImageMenuName() );

		BdvPopupMenus.addAction(
				bdvHandle,
				menuNames,
				"Show as Boundary",
				( x, y ) -> { labelMasksAsBoundaryDialog(); }
			);
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
		setSegmentFocusAnimationDurationMillis( ( int ) genericDialog.getNextNumber() );
	}

	private void installRandomColorShufflingBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> shuffleRandomColors() ).start(),
					name + "-change-color-random-seed",
						incrementCategoricalLutRandomSeedTrigger );
	}

	private void installShowLabelMaskAsBinaryMaskBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> toggleLabelMaskAsBinaryMask() ).start(),
				name + "-asMask",
				labelMaskAsBinaryMaskTrigger );
	}

	private void installShowLabelMaskAsBoundaryBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> labelMasksAsBoundaryDialog() ).start(),
				name + "-asBoundaries",
				labelMaskAsBoundaryTrigger );
	}

	private synchronized void shuffleRandomColors()
	{
		if ( ! isLabelSourceActive() ) return;

		final ColoringModel< T > coloringModel = selectionColoringModel.getColoringModel();

		if ( coloringModel instanceof CategoryColoringModel )
		{
			( ( CategoryColoringModel ) coloringModel ).incRandomSeed();
			BdvUtils.repaint( bdvHandle );
		}
	}

	private synchronized void toggleLabelMaskAsBinaryMask()
	{
		// TODO: loop through all label sources
//		if ( ! isLabelSourceActive() ) return;
//
//		if ( isLabelMaskShownAsBinaryMask )
//			labelsSourceConverter.setSingleColor( null );
//		else
//			labelsSourceConverter.setSingleColor( labelSourceSingleColor );
//
//		isLabelMaskShownAsBinaryMask = !isLabelMaskShownAsBinaryMask;
//
//		BdvUtils.repaint( bdvHandle );
	}

	// TODO: add to context menu?
	private synchronized void labelMasksAsBoundaryDialog()
	{
		LabelMaskAsBoundaryDialog dialog = new LabelMaskAsBoundaryDialog();
		dialog.showDialog();
		final int boundaryThickness = dialog.getBoundaryThickness();
		final boolean showAsBoundary = dialog.isShowAsBoundary();

		Set< SourceAndConverter< R > > labelSources = getLabelSources();
		labelSources.forEach( source ->
		{
			if ( ! ( source.getSpimSource() instanceof LabelMaskSource ) ) return;

			(( LabelMaskSource ) source.getSpimSource() ).showAsBoundary( showAsBoundary, boundaryThickness );
			(( LabelMaskSource ) source.asVolatile().getSpimSource() ).showAsBoundary( showAsBoundary, boundaryThickness );
		});

		labelMasksShownAsBoundaries = ! labelMasksShownAsBoundaries;

		BdvUtils.repaint( bdvHandle );
	}


	public void setLabelSourceSingleColor( ARGBType labelSourceSingleColor )
	{
		// TODO: loop through all label sources
//		this.labelSourceSingleColor = labelSourceSingleColor;
//		labelsSourceConverter.setSingleColor( labelSourceSingleColor );
//		isLabelMaskShownAsBinaryMask = true;
//		BdvUtils.repaint( bdvHandle );
	}

	private void installUndoSelectionBehaviour( )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
				new Thread( () -> selectNone() ).start(),
				name + "-select-none", selectNoneTrigger );
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
				name + "-toggle-select", selectTrigger ) ;
	}

	private synchronized void toggleSelectionAtMousePosition()
	{
		final T segment = getSegmentAtMouseCoordinates();

		if ( segment == null ) return;

		toggleSegmentSelectionAndFocus( segment );
	}

	private void toggleSegmentSelectionAndFocus( T segment )
	{
		selectionModel.toggle( segment );

		if ( selectionModel.isSelected( segment ) )
		{
			recentFocus = segment;
			selectionModel.focus( segment );
		}
	}

//	public void select( List< Double > labelIds )
//	{
//		List< T > segments = getSegments( labelIds );
//
//		selectionModel.setSelected( segments, true );
//	}
//
//	private List< T > getSegments( List< Double > labelIds )
//	{
//		final String labelImageId = labelsSource.metadata().imageId;
//
//		ArrayList< T > segments = new ArrayList<>(  );
//
//		for ( Double labelId : labelIds )
//		{
//			final LabelFrameAndImage labelFrameAndImage =
//					new LabelFrameAndImage( labelId, getCurrentTimePoint(), labelImageId );
//
//			segments.add( labelFrameAndImageToSegment.get( labelFrameAndImage ) );
//		}
//		return segments;
//	}

	private boolean isLabelSourceActive()
	{
		return true;
//		final Source< R > source = labelsSource.metadata().bdvStackSource.getSources().get( 0 ).getSpimSource();
//
//		final boolean active = BdvUtils.isActive( bdvHandle, source );
//
//		return active;
	}

	private Set< SourceAndConverter< R > > getLabelSources()
	{
		Set< SourceAndConverter< R > > primaryLabelSources = sourceToMetadata.keySet().stream()
				.filter( source -> sourceToMetadata.get( source ).isPrimaryLabelSource )
				.collect( Collectors.toSet() );

		return primaryLabelSources;
	}

	private T getSegmentAtMouseCoordinates()
	{
		final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdvHandle );
		List< SourceAndConverter< ? > > sources = bdvHandle.getViewerPanel().state().getSources();
		final int currentTimepoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		Set< SourceAndConverter< ? > > sourcesAtMousePosition = sources.stream()
				.filter( source -> SourceAndConverterUtils.isPositionWithinSourceInterval( source, globalMouseCoordinates, currentTimepoint, is2D ) )
				.collect( Collectors.toSet() );

		Set< SourceAndConverter< R > > labelSources = getLabelSources();

		for ( SourceAndConverter< ? > source : sourcesAtMousePosition )
		{
			if ( labelSources.contains( source ) )
			{
				Source< R > spimSource = ( Source< R > ) source.getSpimSource();
				if ( spimSource instanceof LabelMaskSource )
					spimSource = ( ( LabelMaskSource ) spimSource ).getWrappedSource();
				
				final Double labelIndex = BdvUtils.getPixelValue( spimSource, globalMouseCoordinates, currentTimepoint );

				if ( labelIndex == BACKGROUND ) return null;

				final String labelImageId = sourceToMetadata.get( source ).imageId;

				final LabelFrameAndImage labelFrameAndImage = new LabelFrameAndImage( labelIndex, currentTimepoint, labelImageId );

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
				name + "-iterate-select", iterateSelectionModeTrigger );
	}

	public Component getWindow()
	{
		return bdvHandle.getViewerPanel();
	}

	public void close()
	{
		for ( String popupActionName : popupActionNames )
		{
			BdvPopupMenus.removeAction( bdvHandle, popupActionName );
		}
	}
}
