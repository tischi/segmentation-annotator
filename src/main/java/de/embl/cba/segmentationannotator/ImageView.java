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
package de.embl.cba.segmentationannotator;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.DisplayMode;
import bdv.viewer.*;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.cba.segmentationannotator.bdv.SourcesAtMousePositionSupplier;
import de.embl.cba.segmentationannotator.converter.LabelConverter;
import de.embl.cba.segmentationannotator.converter.SegmentsConverter;
import de.embl.cba.segmentationannotator.label.LabelMaskDisplayDialog;
import de.embl.cba.segmentationannotator.volume.VolumeViewConfigurationDialog;
import de.embl.cba.segmentationannotator.label.LabelSource;
import de.embl.cba.segmentationannotator.volume.SegmentsVolumeView;
import de.embl.cba.tables.color.CategoryColoringModel;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.gui.GenericDialog;
import net.imglib2.RealPoint;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.jetbrains.annotations.NotNull;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getViewerTransformWithNewCenter;

// TODO: reconsider what a "segment" needs to be here
public class ImageView< T extends ImageSegment, R extends NumericType< R > & RealType< R > >
{
	public static final int BACKGROUND = 0;

	private final SelectionColoringModel< T > selectionColoringModel;
	private final SelectionModel< T > selectionModel;
	private final Map< SourceAndConverter< R >, SourceMetadata > rawSourceToMetadata;
	private Map< SourceAndConverter< R >, SourceMetadata > sourceToMetadata;

	private static String selectTrigger = "ctrl button1";
	private static final String selectNoneTrigger = "ctrl shift N";
	private static final String shuffleRandomColorTrigger = "ctrl L";

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
	private TableView< TableRowImageSegment > tableView;
	private SegmentsVolumeView< TableRowImageSegment > volumeView;
	private SourceAndConverterService sacService;
	private SourceAndConverter< R > primaryLabelSource;
	private RealTransform segmentPositionTransform;

	public ImageView( final List< T > imageSegments, final SelectionColoringModel< T > selectionColoringModel, final Map< SourceAndConverter< R >, SourceMetadata > rawSourceToMetadata )
	{
		this.selectionColoringModel = selectionColoringModel;
		this.selectionModel = selectionColoringModel.getSelectionModel();
		this.rawSourceToMetadata = rawSourceToMetadata;

		this.name = imageSegments.toString();

		initSegments( imageSegments );
	}

	public void showImages()
	{
		int tMax = 0;
		for ( T segments : labelFrameAndImageToSegment.values() )
		{
			if ( segments.timePoint() > tMax )
				tMax = segments.timePoint();
		}

		long zDimMax = 1;
		for ( SourceAndConverter< R > sourceAndConverter : rawSourceToMetadata.keySet() )
		{
			final long[] dimensions = sourceAndConverter.getSpimSource().getSource( 0, 0 ).dimensionsAsLongArray();
			if ( dimensions[ 2 ] > zDimMax )
				zDimMax = dimensions[ 2 ];
		}

		showImages( zDimMax == 1, tMax + 1 );
	}

	public void showImages( boolean is2D, int numTimePoints )
	{
		this.is2D = is2D; // TODO: fetch this some other way, also the numTimePoints

		createNewBdv( numTimePoints );

		new HashSet<>( rawSourceToMetadata.keySet() );

		initSources();
		showSources();

		registerAsSelectionListener( this.selectionModel );
		registerAsColoringListener( this.selectionColoringModel );

		installBdvBehavioursAndPopupMenu();
	}

	public void setVolumeView( SegmentsVolumeView< TableRowImageSegment > volumeView )
	{
		this.volumeView = volumeView;
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
				source = asPrimaryLabelSource( source, metadata.imageId );
				primaryLabelSource = source;
			}
			else if ( metadata.isLabelSource )
			{
				source = asLabelSource( source );
			}

			sourceToMetadata.put( source, metadata );
		} );
	}

	public SourceAndConverter< R > getPrimaryLabelSource()
	{
		return primaryLabelSource;
	}

	private SourceAndConverter< R > asPrimaryLabelSource( SourceAndConverter< R > source, String labelImageId )
	{
		SegmentsConverter segmentsConverter = new SegmentsConverter(
			labelFrameAndImageToSegment,
			labelImageId,
			selectionColoringModel );

		bdvHandle.getViewerPanel().addTimePointListener( segmentsConverter );

		SourceAndConverter sourceAndConverter = LabelSource.asLabelSourceAndConverter( source, segmentsConverter );

		return sourceAndConverter;
	}

	private SourceAndConverter< R > asLabelSource( SourceAndConverter< R > source )
	{
		LabelConverter labelConverter = new LabelConverter();

		SourceAndConverter sourceAndConverter = LabelSource.asLabelSourceAndConverter( source, labelConverter );

		return sourceAndConverter;
	}

	private void showSources()
	{
		Set< SourceAndConverter< R > > sources = sourceToMetadata.keySet();

		// focus on one of the sources
		AffineTransform3D transform = new ViewerTransformAdjuster( bdvHandle, sources.iterator().next() ).getTransform();
		if ( is2D ) transform.set( 0.0, 2, 3); // zero z offset
		state.setViewerTransform( transform );

		HashMap< String, SourceGroup > groupIdToSourceGroup = new HashMap< String, SourceGroup >();
		sources.forEach( source ->
		{
			addSourceToBdv( source );
			if( sourceToMetadata.get( source ).groupId != null )
			{
				addSourceToGroup( groupIdToSourceGroup, source, sourceToMetadata.get( source ).groupId );
			}
		} );

		if ( ! groupIdToSourceGroup.isEmpty() )
		{
			state.setDisplayMode( DisplayMode.FUSEDGROUP );
			// remove default groups
			for ( int i = 0; i < 10; i++ )
			{
				state.removeGroup( state.getCurrentGroup() );
			}
		}
	}

	// TODO: the group Ids do not contain the channel name
	private void addSourceToGroup( HashMap< String, SourceGroup > groupIdToSourceGroup, SourceAndConverter< R > source, String groupId )
	{

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
		ConverterSetup converterSetup = SourceAndConverterHelper.createConverterSetup( source );
		bdvHandle.getConverterSetups().put( source, converterSetup );
	}

	private void  initSegments( List< T > segments )
	{
		labelFrameAndImageToSegment = SegmentUtils.createSegmentMap( segments );
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
		if ( segmentPositionTransform != null )
			segmentPositionTransform.apply( position, position );

		final int currentTimepoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		if ( currentTimepoint != imageSegment.timePoint() )
			bdvHandle.getViewerPanel().state().setCurrentTimepoint( imageSegment.timePoint() );

		new ViewerTransformChanger(
				bdvHandle,
				getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				segmentFocusAnimationDurationMillis ).run();

	}

	public void setSegmentFocusAnimationDurationMillis( int duration )
	{
		this.segmentFocusAnimationDurationMillis = duration;
	}

	private void installBdvBehavioursAndPopupMenu()
	{
		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getBdvHandle().getTriggerbindings(), name + "-bdv-select-handler" );

		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();

		final ArrayList< String > actions = new ArrayList<>();
		actions.add( installUndoSegmentSelectionBehaviour() );
		actions.add( installRandomColorShufflingBehaviour() );
		actions.add( installSegmentSelectionBehaviour() );
		actions.add( installConfigureSegmentsVolumeViewBehaviour() );
		actions.add( installSelectionColoringModeBehaviour() );
		actions.add( installStartNewAnnotationBehaviour() );
		actions.add( installContinueAnnotationBehaviour() );
		actions.add( installConfigureLabelMaskDisplayBehaviour() );
		//actions.add( installReportIssueBehaviour() );
		//addMoveToPopupMenu();
		//addAnimationSettingsPopupMenu();

		SourceAndConverterContextMenuClickBehaviour contextMenu = new SourceAndConverterContextMenuClickBehaviour( bdvHandle, new SourcesAtMousePositionSupplier( bdvHandle, is2D ), actions.toArray( new String[ 0 ] ) );
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.behaviour( contextMenu, "Context menu", "button3", "shift P");
		behaviours.install( bdvHandle.getTriggerbindings(), "ContextMenu" );
	}

	private String installUndoSegmentSelectionBehaviour()
	{
		final String actionName = "Undo Segment Selections" + BdvUtils.getShortCutString( selectNoneTrigger );
		sacService.registerAction( actionName, sourceAndConverters -> {
			selectNone();
		} );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> selectNone() ).start(),
				name + "-select-none", selectNoneTrigger );
		return actionName;
	}

	private String installConfigureSegmentsVolumeViewBehaviour()
	{
		final String actionName = "Configure Segments 3D View";
		sacService.registerAction( actionName, sourceAndConverters -> {
			if ( VolumeViewConfigurationDialog.showDialog() )
			{
				volumeView.showSegments( VolumeViewConfigurationDialog.isShowSelectedSegmentsIn3D() );
			}
		} );

		return actionName;
	}

	private String installStartNewAnnotationBehaviour()
	{
		final String actionName = "Start New Annotation...";
		sacService.registerAction( actionName, sourceAndConverters -> {
			tableView.showNewAnnotationDialog();}  );
		return actionName;
	}

	private String installContinueAnnotationBehaviour()
	{
		final String actionName = "Continue Annotation...";
		sacService.registerAction( actionName, sourceAndConverters -> {tableView.showContinueAnnotationDialog();}  );
		return actionName;
	}

	private void addAnimationSettingsPopupMenu()
	{
		final ArrayList< String > menuNames = new ArrayList<>();
		menuNames.add( getSegmentsMenuName() );
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
	private void addSelectionPopupMenu()
	{
		final ArrayList< String > menuNames = new ArrayList<>();
		menuNames.add( getSegmentsMenuName() );

		final String actionName = "Toggle Segment Selection" + BdvUtils.getShortCutString( selectTrigger );
		popupActionNames.add( BdvPopupMenus.getCombinedMenuActionName(  menuNames, actionName ) );

		BdvPopupMenus.addAction(
				bdvHandle,
				menuNames,
				actionName,
				( x, y ) -> new Thread( () -> toggleSelectionAtMousePosition() ).start()
		);
	}

	private void addUndoSelectionPopupMenu()
	{
		final ArrayList< String > menuNames = new ArrayList<>();
		menuNames.add( getSegmentsMenuName() );

		final String actionName = "Undo Selection" + BdvUtils.getShortCutString( selectNoneTrigger );
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
		menuNames.add( getSegmentsMenuName() );
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

	private String installConfigureLabelMaskDisplayBehaviour()
	{
		final String actionName = "Configure Label Mask Display";

		sacService.registerAction( actionName, sourceAndConverters -> { labelMasksAsBoundaryDialog(); }  );

		return actionName;
	}

//	private String installReportIssueBehaviour()
//	{
//		final String actionName = "Report an Issue...";
//		sacService.registerAction( actionName, sourceAndConverters -> { reportIssueDialog(); }  );
//		return actionName;
//	}

	private void addMoveToPopupMenu()
	{
		BdvPopupMenus.addAction(
				bdvHandle,
				"Move to Location",
				( x, y ) -> { moveToDialog(); }
		);
	}

	private void moveToDialog()
	{
		final GenericDialog gd = new GenericDialog( "Move to location" );
		gd.addStringField( "Location (x,y,z,t)", "", 50 );

		gd.showDialog();
		if ( gd.wasCanceled() ) return;

		String location = gd.getNextString();

		if ( location.contains( "(" ))
			location = location.replace( "(", "" );

		if ( location.contains( ")" ))
			location = location.replace( ")", "" );

		String[] split = location.split( "," );
		double[] xyz = Arrays.stream( split ).limit( 3 ).mapToDouble( Double::parseDouble ).toArray();
		BdvUtils.moveToPosition( bdvHandle, xyz, Integer.parseInt( split[ 3 ]), segmentFocusAnimationDurationMillis );

	}

	private void addShuffleRandomColorsPopupMenu()
	{
		final ArrayList< String > menuNames = new ArrayList<>();
		menuNames.add( getSegmentsMenuName() ); // TODO: populate with the different label mask groups

		BdvPopupMenus.addAction(
				bdvHandle,
				menuNames,
				"Shuffle Random Label Colors",
				( x, y ) -> { shuffleRandomColors(); }
		);
	}

	@NotNull
	private String getSegmentsMenuName()
	{
		return "Segments";
	}

	private void changeAnimationSettingsUI()
	{
		final GenericDialog genericDialog = new GenericDialog( "Segment animation settings" );
		genericDialog.addNumericField( "Segment Focus Animation Duration [ms]", segmentFocusAnimationDurationMillis, 0 );
		genericDialog.showDialog();
		if ( genericDialog.wasCanceled() ) return;
		setSegmentFocusAnimationDurationMillis( ( int ) genericDialog.getNextNumber() );
	}

	private String installRandomColorShufflingBehaviour()
	{
		final String actionName = "Shuffle Random Label Colors" + BdvUtils.getShortCutString( shuffleRandomColorTrigger );
		sacService.registerAction( actionName, sourceAndConverters -> { shuffleRandomColors(); } );

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> shuffleRandomColors() ).start(),
					name + "-change-color-random-seed",
				shuffleRandomColorTrigger );
		return actionName;
	}

	private synchronized void shuffleRandomColors()
	{
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

	private synchronized void labelMasksAsBoundaryDialog()
	{
		LabelMaskDisplayDialog dialog = new LabelMaskDisplayDialog();
		dialog.showDialog();
		final int boundaryThickness = dialog.getBoundaryThickness();
		final boolean showAsBoundary = dialog.isShowAsBoundary();

		Set< SourceAndConverter< R > > labelSources = getLabelSources();
		labelSources.forEach( source ->
		{
			if ( ! ( source.getSpimSource() instanceof LabelSource ) ) return;

			(( LabelSource ) source.getSpimSource() ).showAsBoundary( showAsBoundary, boundaryThickness );
			(( LabelSource ) source.asVolatile().getSpimSource() ).showAsBoundary( showAsBoundary, boundaryThickness );
		});

		labelMasksShownAsBoundaries = ! labelMasksShownAsBoundaries;

		BdvUtils.repaint( bdvHandle );
	}

	private synchronized void reportIssueDialog()
	{
//		final RealPoint location = new RealPoint( 3 );
//		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( location );
//
//		GenericDialog gd = new GenericDialog( "Report issue" );
//		gd.addTextAreas( "", null, 5, 60 );
//
//		T segment = getSegmentAtMouseCoordinates();
//		if ( segment != null && segment instanceof TableRow )
//			gd.addCheckbox( "Add issue to image segment in table", true );
//
//		gd.showDialog();
//		if ( gd.wasCanceled() ) return;
//
//		IJ.log( "### Issue");
//		IJ.log( "Location (x,y,z,t):" );
//		IJ.log( "" + location.getFloatPosition( 0 ) + "," + location.getFloatPosition( 1 ) + "," + location.getFloatPosition( 2 ) + "," + bdvHandle.getViewerPanel().state().getCurrentTimepoint() );
//		IJ.log( "Issue:" );
//		String issue = gd.getNextText();
//		IJ.log( issue );
//
//		// TODO: below is a mess! Think of better separation of concerns
//		//  Maybe an image segment needs to have option to have more fields?
//		//  Such as Annotation and Issue? Or maybe a featureMap?!
//		if ( segment != null && segment instanceof TableRow && gd.getNextBoolean() )
//		{
//			if ( tableView != null )
//			{
//				if ( ! tableView.getColumnNames().contains( "Issue" ) )
//				{
//					tableView.addColumn( "Issue", "None" );
//				}
//			}
//
//			((TableRow) segment).setCell( "Issue", issue );
//		}
	}

	private synchronized void selectNone()
	{
		selectionModel.clearSelection( );

		BdvUtils.repaint( bdvHandle );
	}

	private String installSegmentSelectionBehaviour()
	{
		final String actionName = "Toggle Segment Selection" + BdvUtils.getShortCutString( selectTrigger );
		sacService.registerAction( actionName, sourceAndConverters -> {toggleSelectionAtMousePosition();}  );

		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> toggleSelectionAtMousePosition() ).start(),
				name + "-toggle-select", selectTrigger ) ;
		return actionName;
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
				.filter( source -> SourceAndConverterHelper.isPositionWithinSourceInterval( source, globalMouseCoordinates, currentTimepoint, is2D ) )
				.collect( Collectors.toSet() );

		Set< SourceAndConverter< R > > labelSources = getLabelSources();

		for ( SourceAndConverter< ? > source : sourcesAtMousePosition )
		{
			if ( labelSources.contains( source ) )
			{
				Source< R > spimSource = ( Source< R > ) source.getSpimSource();
				if ( spimSource instanceof LabelSource )
					spimSource = ( ( LabelSource ) spimSource ).getWrappedSource();
				
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

	private String installSelectionColoringModeBehaviour( )
	{
		final String actionName = "Adjust Unselected Segments Brightness";
		sacService.registerAction( actionName, sourceAndConverters -> {
			new Thread( () -> {
				final GenericDialog genericDialog = new GenericDialog( actionName );
				genericDialog.addNumericField( "Brightness [0,1]", selectionColoringModel.getBrightnessNotSelected() );
				genericDialog.showDialog();
				if ( genericDialog.wasCanceled() ) return;
				selectionColoringModel.setSelectionColoringMode( SelectionColoringModel.SelectionColoringMode.DimNotSelected, genericDialog.getNextNumber()  );
			}).start();
		});
		return actionName;
	}

	public Component getWindow()
	{
		return bdvHandle.getViewerPanel();
	}

	public void setTableView( TableView< TableRowImageSegment > tableView )
	{
		this.tableView = tableView;
	}

	public void setSegmentPositionTransform( RealTransform transform )
	{
		segmentPositionTransform = transform;
	}
}
