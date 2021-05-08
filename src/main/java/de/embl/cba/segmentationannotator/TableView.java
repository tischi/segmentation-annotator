/*-
 * #%L
 * Various Java code for ImageJ
 * %%
 * Copyright (C) 2018 - 2021 EMBL
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

import bdv.tools.HelpDialog;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.segmentationannotator.annotate.Annotator;
import de.embl.cba.segmentationannotator.classify.ClassifyDialog;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.TableRows;
import de.embl.cba.tables.TableUIs;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.color.CategoryTableRowColumnColoringModel;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.ColumnColoringModelCreator;
import de.embl.cba.tables.color.NumericColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.measure.MeasureDistance;
import de.embl.cba.tables.plot.ScatterPlotDialog;
import de.embl.cba.tables.plot.TableRowsScatterPlot;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowListener;
import ij.gui.GenericDialog;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.io.FilenameUtils;

import javax.activation.UnsupportedDataTypeException;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.embl.cba.tables.FileUtils.selectPathFromProjectOrFileSystem;
import static de.embl.cba.tables.TableRows.setTableCell;
import static de.embl.cba.tables.color.CategoryTableRowColumnColoringModel.DARK_GREY;

public class TableView< T extends TableRow > extends JPanel
{
	private final List< T > tableRows;
	private final SelectionModel< T > selectionModel;
	private final SelectionColoringModel< T > selectionColoringModel;
	private final String tableName;

	private JFrame frame;
    private JScrollPane scrollPane;
    private JMenuBar menuBar;

	private JTable jTable;
	private int recentlySelectedRowInView;
	private ColumnColoringModelCreator< T > columnColoringModelCreator;
	private MeasureDistance< T > measureDistance;
	private Component parentComponent;

	private String mergeByColumnName; // for loading additional columns
	private String tablesDirectory; // for loading additional columns
	private ArrayList<String> additionalTables; // tables from which additional columns are loaded

	private SelectionMode selectionMode = SelectionMode.FocusOnly;
	private Map< String, CategoryTableRowColumnColoringModel< T > > columnNameToColoringModel = new HashMap<>(  );
	private boolean controlDown;

	public enum SelectionMode
	{
		None,
		FocusOnly,
		ToggleSelectionAndFocusIfSelected
	}

	// TODO: clean this up for what is really needed
	public TableView(
			final List< T > tableRows,
			final SelectionModel< T > selectionModel,
			final SelectionColoringModel< T > selectionColoringModel )
	{
		this( tableRows, selectionModel, selectionColoringModel, "" );
	}

	public TableView(
			final List< T > tableRows,
			final SelectionModel< T > selectionModel,
			final SelectionColoringModel< T > selectionColoringModel,
			String tableName )
	{
		super( new GridLayout(1, 0 ) );
		this.tableRows = tableRows;

		// TODO: this should be listening to the TableModel instead
		registerAsTableRowListener( tableRows );

		this.selectionColoringModel = selectionColoringModel;
		this.selectionModel = selectionModel;
		this.tableName = tableName;
		this.recentlySelectedRowInView = -1;
		this.additionalTables = new ArrayList<>();

		if ( selectionModel != null )
			registerAsSelectionListener( selectionModel );

		if ( selectionColoringModel != null )
			registerAsColoringListener( selectionColoringModel );
	}

	public void registerAsTableRowListener( List< T > tableRows )
	{
		for ( T tableRow : tableRows )
		{
			tableRow.listeners().add( new TableRowListener()
			{
				@Override
				public void cellChanged( String columnName, String value )
				{
					synchronized ( jTable )
					{
						if ( ! Tables.getColumnNames( jTable ).contains( columnName ) )
						{

							Tables.addColumn( jTable.getModel(), columnName, "None" );
						}

						setTableCell( tableRow.rowIndex(), columnName, value, getjTable() );
					}
				}
			} );
		}
	}

	public List< T > getTableRows()
	{
		return tableRows;
	}

	public void showTableAndMenu( Component parentComponent )
	{
		this.parentComponent = SwingUtilities.getWindowAncestor( parentComponent );
		showTableAndMenu();
	}

	public void showTableAndMenu()
	{
		configureJTable();

		if ( selectionModel != null )
			installSelectionModelNotification();

		if ( selectionColoringModel != null)
			configureTableRowColoring();

		createMenuBar();
		showMenu( menuBar );
	}

	private void configureTableRowColoring()
	{
		jTable.setDefaultRenderer( Double.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {

				Component c = super.getTableCellRendererComponent(
						table,
						value,
						isSelected,
						hasFocus,
						row,
						column);

				c.setBackground( getColor(row, column) );

				return c;
			}
		} );

		jTable.setDefaultRenderer( String.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {

				Component c = super.getTableCellRendererComponent(
						table,
						value,
						isSelected,
						hasFocus,
						row,
						column);

				c.setBackground( getColor(row, column) );

				return c;
			}

		} );

		jTable.setDefaultRenderer( Long.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column )
			{
				Component c = super.getTableCellRendererComponent(
						table,
						value,
						isSelected,
						hasFocus,
						row,
						column );

				c.setBackground( getColor( row, column ) );

				return c;
			}
		});

		jTable.setDefaultRenderer( Integer.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				Component c = super.getTableCellRendererComponent(
						table,
						value,
						isSelected,
						hasFocus,
						row,
						column);

				c.setBackground( getColor(row, column) );

				return c;
			}
		} );

		jTable.setDefaultRenderer( Object.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column )
			{
				Component c = super.getTableCellRendererComponent(
						table,
						value,
						isSelected,
						hasFocus,
						row,
						column );

				c.setBackground( getColor( row, column ) );

				return c;
			}
		});
	}

	private Color getColor( int rowInView, int columnInView )
	{
		final int row = jTable.convertRowIndexToModel( rowInView );

//		if ( selectionModel.isFocused( tableRows.getTableRows().get( row ) ) )
//		{
//			return Color.BLUE;
//		}

		final ARGBType argbType = new ARGBType();
		selectionColoringModel.convert( tableRows.get( row ), argbType );

		if ( ARGBType.alpha( argbType.get() ) == 0 )
			return Color.WHITE;
		else
			return ColorUtils.getColor( argbType );
	}

	private void registerAsColoringListener( SelectionColoringModel< T > selectionColoringModel )
	{
		selectionColoringModel.listeners().add( () -> SwingUtilities.invokeLater( () -> repaintTable() ) );
	}

	private synchronized void repaintTable()
	{
		jTable.repaint();
	}

	private void configureJTable()
	{
		jTable = Tables.jTableFromTableRows( tableRows );
		jTable.setPreferredScrollableViewportSize( new Dimension(500, 200) );
		jTable.setFillsViewportHeight( true );
		jTable.setAutoCreateRowSorter( true );
		jTable.setRowSelectionAllowed( true );
		jTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

		scrollPane = new JScrollPane(
				jTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		add( scrollPane );

		jTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

		columnColoringModelCreator = new ColumnColoringModelCreator( jTable );

		updateUI();
	}

	// TODO: factor out the whole menu into an own class
	private void createMenuBar()
	{
		menuBar = new JMenuBar();
		menuBar.add( createTableMenu() );

		if ( selectionModel != null )
		{
			menuBar.add( createSelectionMenu() );
		}

		if ( selectionColoringModel != null )
		{
			menuBar.add( createColoringMenu() );
			menuBar.add( createAnnotateMenu() );
			menuBar.add( createClassifyMenu() );
			menuBar.add( createPlotMenu() );
		}


		menuBar.add( createHelpMenu() );
	}

	private JMenu createSelectionMenu()
	{
		JMenu menu = new JMenu( "Select" );

		menu.add( createSelectAllMenuItem() );

		return menu;
	}

	private JMenu createClassifyMenu()
	{
		JMenu menu = new JMenu( "Classify" );

		menu.add( createStartNewAnnotationMenuItem() );

		menu.add( createContinueAnnotationMenuItem() );

		return menu;
	}

	private JMenu createAnnotateMenu()
	{
		JMenu menu = new JMenu( "Annotate" );

		menu.add( createStartNewAnnotationMenuItem() );

		menu.add( createContinueAnnotationMenuItem() );

		return menu;
	}

	private JMenu createPlotMenu()
	{
		JMenu menu = new JMenu( "Plot" );

		menu.add( createScatterPlotMenuItem() );

		return menu;
	}

	private JMenu createHelpMenu()
	{
		JMenu menu = new JMenu( "Help" );

		menu.add( createShowSegmentationHelpMenuItem() );
		menu.add( createShowNavigationHelpMenuItem() );

		return menu;
	}

	private JMenuItem createShowSegmentationHelpMenuItem()
	{
		initHelpDialog();
		final JMenuItem menuItem = new JMenuItem( "Show Segmentation Image Help" );
		menuItem.addActionListener( e ->
			{
				final HelpDialog helpDialog = new HelpDialog(
					frame,
					Tables.class.getResource( "/SegmentationImageActionsHelp.html" ) );
				helpDialog.setVisible( true );
			}
		);
		return menuItem;
	}

	private JMenuItem createScatterPlotMenuItem()
	{
		initHelpDialog();
		final JMenuItem menuItem = new JMenuItem( "2D Scatter Plot..." );
		menuItem.addActionListener( e ->
			{
				SwingUtilities.invokeLater( () ->
				{
					String[] columnNames = getColumnNames().stream().toArray( String[]::new );
					ScatterPlotDialog dialog = new ScatterPlotDialog( columnNames, new String[]{ columnNames[ 0 ], columnNames[ 1 ] }, new double[]{ 1.0, 1.0 }, 1.0 );

					if ( dialog.show() )
					{
						TableRowsScatterPlot< T > scatterPlot = new TableRowsScatterPlot<>( tableRows, selectionColoringModel, dialog.getSelectedColumns(), dialog.getScaleFactors(), dialog.getDotSizeScaleFactor() );
						scatterPlot.show( null );
					}
				});
			}
		);
		return menuItem;
	}
	// TODO: This does not always make sense. Should be added only on demand
	private JMenuItem createShowNavigationHelpMenuItem()
	{
		initHelpDialog();
		final JMenuItem menuItem = new JMenuItem( "Show Navigation Help" );
		menuItem.addActionListener( e ->
		{
			final HelpDialog helpDialog = new HelpDialog(
				frame,
				Tables.class.getResource( "/MultiImageSetNavigationHelp.html" ) );
				helpDialog.setVisible( true );
			}
		);
		return menuItem;
	}

	public void addMenu( JMenuItem menuItem )
	{
		SwingUtilities.invokeLater( () ->
		{
			menuBar.add( menuItem );
			if ( frame != null ) SwingUtilities.updateComponentTreeUI( frame );
		});
	}

	private JMenu createTableMenu()
    {
        JMenu menu = new JMenu( "Table" );

        menu.add( createSaveTableAsMenuItem() );

		menu.add( createSaveColumnsAsMenuItem() );

		menu.add( createLoadColumnsMenuItem() );

		return menu;
    }

    public void addAdditionalTable(String tablePath) {
		String tableName  = FilenameUtils.getBaseName(tablePath);
		additionalTables.add(tableName);
	}

	private JMenuItem createLoadColumnsMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Load Columns..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
				{
					try
					{
						String mergeByColumnName = getMergeByColumnName();
						String tablePath = selectPathFromProjectOrFileSystem( tablesDirectory, "Table");
						addAdditionalTable(tablePath);
						Map< String, List< String > > newColumnsOrdered = TableUIs.loadColumns( jTable, tablePath, mergeByColumnName );
						if ( newColumnsOrdered == null ) return;
						newColumnsOrdered.remove( mergeByColumnName );
						addColumns( newColumnsOrdered );
					} catch ( IOException ioOException )
					{
						ioOException.printStackTrace();
					}
				} ) );

		return menuItem;
	}

	private String getMergeByColumnName()
	{
		String aMergeByColumnName;
		if ( mergeByColumnName == null )
			aMergeByColumnName = TableUIs.selectColumnNameUI( jTable, "Merge by " );
		else
			aMergeByColumnName = mergeByColumnName;
		return aMergeByColumnName;
	}

	public ArrayList<String> getAdditionalTables() {
		return additionalTables;
	}

	public double[] getColorByColumnValueLimits() {
		ColoringModel coloringModel = selectionColoringModel.getColoringModel();
		if (coloringModel instanceof NumericColoringModel) {
			double[] valueLimits = new double[2];
			NumericColoringModel numericColoringModel = (NumericColoringModel) coloringModel;
			valueLimits[0] = numericColoringModel.getMin();
			valueLimits[1] = numericColoringModel.getMax();
			return valueLimits;
		} else {
			return null;
		}
	}

	public ArrayList<T> getSelectedLabelIds () {
		if (selectionModel.getSelected().size() > 0) {
			ArrayList<T> selectedIDsArray = new ArrayList<>(selectionModel.getSelected());
			return selectedIDsArray;
		} else {
			return null;
		}
	}

	public void setMergeByColumnName(String mergeByColumnName )
	{
		this.mergeByColumnName = mergeByColumnName;
	}

	public void setTablesDirectory( String tablesDirectory )
	{
		this.tablesDirectory = tablesDirectory;
	}

	private JMenuItem createSaveTableAsMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Save Table as..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
						TableUIs.saveTableUI( jTable ) ) );

		return menuItem;
	}

	private JMenuItem createSaveColumnsAsMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Save Columns as..." );
		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () -> TableUIs.saveColumns( jTable ) ) );

		return menuItem;
	}

	private JMenuItem createSelectAllMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Select all" );

		menuItem.addActionListener( e ->
				SwingUtilities.invokeLater( () ->
						selectAll() ) );

		return menuItem;
	}

	private JMenuItem createStartNewAnnotationMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Start new annotation..." );

		menuItem.addActionListener( e -> showNewAnnotationDialog() );

		return menuItem;
	}

	private JMenuItem createClassifyMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Classify..." );

		menuItem.addActionListener( e ->
		{
			new ClassifyDialog().showDialog();
		} );

		return menuItem;
	}

	private JMenuItem createContinueAnnotationMenuItem()
	{
		final JMenuItem menuItem = new JMenuItem( "Continue annotation..." );

		menuItem.addActionListener( e -> showContinueAnnotationDialog() );

		return menuItem;
	}

	public void showContinueAnnotationDialog()
	{
		SwingUtilities.invokeLater( () ->
		{
			final String annotationColumn = TableUIs.selectColumnNameUI( jTable, "Annotation column" );
			continueAnnotation( annotationColumn );
		});
	}

	private void selectAll()
	{
		selectionModel.setSelected( tableRows, true );
//		for ( T tableRow : tableRows )
//		{
//			selectionModel.setSelected( tableRow, true );
//			selectionModel.focus( tableRow );
//		}

	}

	public void showNewAnnotationDialog()
	{
		final GenericDialog gd = new GenericDialog( "" );
		gd.addStringField( "Annotation column name", "", 30 );
		gd.showDialog();
		if( gd.wasCanceled() ) return;
		final String columnName = gd.getNextString();
		if ( getColumnNames().contains( columnName ) )
		{
			Logger.error( "\"" +columnName + "\" exists already as a column name, please choose another one." );
			return;
		}
		this.addColumn( columnName, "None" );

		continueAnnotation( columnName );
	}

	public void continueAnnotation( String columnName )
	{
		if ( ! columnNameToColoringModel.containsKey( columnName ) )
		{
			final CategoryTableRowColumnColoringModel< T > categoricalColoringModel = columnColoringModelCreator.createCategoricalColoringModel( columnName, false, new GlasbeyARGBLut(), DARK_GREY );
			columnNameToColoringModel.put( columnName, categoricalColoringModel );
		}

		selectionColoringModel.setSelectionColoringMode( SelectionColoringModel.SelectionColoringMode.SelectionColor, 1.0 );
		selectionColoringModel.setColoringModel( columnNameToColoringModel.get( columnName ) );

		final RowSorter< ? extends TableModel > rowSorter = jTable.getRowSorter();
		final Annotator annotator = new Annotator(
				columnName,
				tableRows,
				selectionColoringModel.getSelectionModel(),
				columnNameToColoringModel.get( columnName ),
				rowSorter
		);

		annotator.showDialog();
	}

	private void showMenu( JMenuBar menuBar )
	{
		final String appleMenuBarLaf = System.getProperty( "apple.laf.useScreenMenuBar" );
		System.setProperty("apple.laf.useScreenMenuBar", "false");

		frame = new JFrame( tableName );
		frame.setJMenuBar( menuBar );
		this.setOpaque( true );
		frame.setContentPane( this );

		if ( parentComponent != null )
		{
			frame.setLocation(
					parentComponent.getLocationOnScreen().x,
					parentComponent.getLocationOnScreen().y + parentComponent.getHeight() + 30
			);


			final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

			frame.setPreferredSize( new Dimension(
					parentComponent.getWidth(),
					screenSize.height - ( parentComponent.getHeight() + parentComponent.getLocationOnScreen().y ) - 50  ) );
		}

		//Display the window.
		frame.pack();
		SwingUtilities.invokeLater( () -> frame.setVisible( true ) );

		// reset
		System.setProperty("apple.laf.useScreenMenuBar", appleMenuBarLaf);
	}

	public void addColumn( String column, Object defaultValue )
	{
		if ( getColumnNames().contains( column ) )
			throw new RuntimeException( column + " exists already, please choose another name." );
		// Tables.addColumn( jTable.getModel(), column, defaultValue ); // done by listener
		// TODO: add to TableModel
		TableRows.addColumn( tableRows, column, defaultValue );
	}

	public void addColumn( String column, Object[] values )
	{
		Tables.addColumn( jTable.getModel(), column, values );
		TableRows.addColumn( tableRows, column, values );
	}

	public void addColumns( Map< String, List< String > > columns )
	{
		for ( String columnName : columns.keySet() )
		{
			try
			{
				final Object[] values = TableColumns.asTypedArray( columns.get( columnName ) );
				addColumn( columnName, values );
			} catch ( UnsupportedDataTypeException e )
			{
				Logger.error( "Could not add column " + columnName + ", because the" +
						" data type could not be determined.");
			}
		}
	}

	public Set< String > getColumnNames()
	{
		return tableRows.get( 0 ).getColumnNames();
	}

	public JTable getjTable()
	{
		return jTable;
	}

	private synchronized void moveToRowInView( int rowInView )
	{
		setRecentlySelectedRowInView( rowInView );
		//table.getSelectionModel().setSelectionInterval( rowInView, rowInView );
		final Rectangle visibleRect = jTable.getVisibleRect();
		final Rectangle cellRect = jTable.getCellRect( rowInView, 0, true );
		visibleRect.y = cellRect.y;
		jTable.scrollRectToVisible( visibleRect );
		jTable.repaint();
	}

	public void installSelectionModelNotification()
	{
		jTable.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				controlDown = e.isControlDown();
			}
		} );

		jTable.getSelectionModel().addListSelectionListener( e ->
			SwingUtilities.invokeLater( () ->
			{
				if ( selectionMode.equals( SelectionMode.None ) ) return;

				if ( e.getValueIsAdjusting() ) return;

				final int selectedRowInView = jTable.getSelectedRow();

				if ( selectedRowInView == -1 ) return;

				if ( selectedRowInView == recentlySelectedRowInView ) return;

				setRecentlySelectedRowInView( selectedRowInView );

				final int row = jTable.convertRowIndexToModel( recentlySelectedRowInView );

				final T object = tableRows.get( row );

				selectionMode = controlDown ? SelectionMode.ToggleSelectionAndFocusIfSelected : SelectionMode.FocusOnly;

				if ( selectionMode.equals( SelectionMode.FocusOnly ) )
				{
					selectionModel.focus( object );
				}
				else if ( selectionMode.equals( SelectionMode.ToggleSelectionAndFocusIfSelected ) )
				{
					selectionModel.toggle( object );
					if ( selectionModel.isSelected( object ) )
						selectionModel.focus( object );
				}
			})
		);
	}

	public void registerAsSelectionListener( SelectionModel< T > selectionModel )
	{
		selectionModel.listeners().add( new SelectionListener< T >()
		{
			@Override
			public synchronized void selectionChanged()
			{
				if ( selectionModel.isEmpty() )
				{
					setRecentlySelectedRowInView( -1 );
					jTable.getSelectionModel().clearSelection();
				}
				SwingUtilities.invokeLater( () -> repaintTable() );
			}

			@Override
			public synchronized void focusEvent( T selection )
			{
				SwingUtilities.invokeLater( () -> moveToSelectedTableRow( selection ) );
			}
		} );
	}

	private synchronized void setRecentlySelectedRowInView( int r )
	{
		recentlySelectedRowInView = r;
	}

	private synchronized void moveToSelectedTableRow( TableRow selection )
	{
		final int rowInView = jTable.convertRowIndexToView( selection.rowIndex() );

		if ( rowInView == recentlySelectedRowInView ) return;

		moveToRowInView( rowInView );
	}

	private JMenu createColoringMenu()
	{
		JMenu coloringMenu = new JMenu( "Color" );

		addColorByColumnMenuItem( coloringMenu );

		// TODO: add menu item to configure values that should be transparent

		addColorLoggingMenuItem( coloringMenu );

		return coloringMenu;
	}

	private void addColorLoggingMenuItem( JMenu coloringMenu )
	{
		final JMenuItem menuItem = new JMenuItem( "Log Current Value to Color Map" );

		menuItem.addActionListener( e ->
				new Thread( () ->
						logCurrentValueToColorMap() ).start() );

		coloringMenu.add( menuItem );
	}

	private void logCurrentValueToColorMap()
	{
		String coloringColumnName = getColoringColumnName();

		if ( coloringColumnName == null )
		{
			Logger.error( "Please first use the [ Color > Color by Column ] menu item to configure the coloring." );
			return;
		}

		Logger.info( " "  );
		Logger.info( "Column used for coloring: " + coloringColumnName );
		Logger.info( " "  );
		Logger.info( "Value, R, G, B"  );


		for ( T tableRow : tableRows )
		{
			final String value = tableRow.getCell( coloringColumnName );

			final ARGBType argbType = new ARGBType();
			selectionColoringModel.convert( tableRow, argbType );
			final int colorIndex = argbType.get();
			Logger.info( value + ": " + ARGBType.red( colorIndex ) + ", " + ARGBType.green( colorIndex ) + ", " + ARGBType.blue( colorIndex ) );
		}
	}

	public String getColoringColumnName()
	{
		final ColoringModel< T > coloringModel = selectionColoringModel.getColoringModel();

		if ( coloringModel instanceof ColumnColoringModel )
		{
			return ((ColumnColoringModel) coloringModel).getColumnName();
		}
		else
		{
			return null;
		}
	}

	private void addColorByColumnMenuItem( JMenu coloringMenu )
	{
		final JMenuItem menuItem = new JMenuItem( "Color by Column..." );

		menuItem.addActionListener( e ->
				new Thread( () -> this.showColorByColumnDialog()
				).start() );

		coloringMenu.add( menuItem );
	}

	public void showColorByColumnDialog()
	{
		final ColoringModel< T > coloringModel = columnColoringModelCreator.showDialog();

		if ( coloringModel != null )
			selectionColoringModel.setColoringModel( coloringModel );
	}

	public void initHelpDialog()
	{
		new HelpDialog(
				frame,
				Tables.class.getResource( "/MultiImageSetNavigationHelp.html" ) );
	}

	public void close()
	{
		frame.dispose();
		this.setVisible( false );
	}
}
