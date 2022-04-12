package de.embl.cba.segmentationannotator.command;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.segmentationannotator.ImageView;
import de.embl.cba.segmentationannotator.SourceMetadata;
import de.embl.cba.segmentationannotator.TableView;
import de.embl.cba.segmentationannotator.Utils;
import de.embl.cba.segmentationannotator.open.SegmentsDatasetOpener;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;

@Plugin(type = Command.class, menuPath = "Plugins > Segmentation > Annotator > Open Dataset from Table..." )
public class OpenDatasetFromTableCommand implements Command
{
	/**
	 * Each row must contain information about one image segment.
	 * Paths to images in the table must be relative to the parent directory of the table.
	 */
	@Parameter( label = "Segments table" )
	public File segmentsTableFile;

	@Parameter( label = "Image path column prefix" )
	public String imagePathColumnPrefix = "Path_";

	@Parameter( label = "Object positions are calibrated" )
	public boolean objectPositionsAreCalibrated = true;

	@Override
	public void run()
	{
		String rootDirectory = new File( segmentsTableFile.getAbsolutePath() ).getParent();
		String relativeTablePath = segmentsTableFile.getName();

		// open data
		SegmentsDatasetOpener opener = new SegmentsDatasetOpener( rootDirectory, relativeTablePath, imagePathColumnPrefix );
		opener.run();

		List< TableRowImageSegment > tableRowImageSegments = opener.getSegments();
		HashMap< SourceAndConverter< ? >, SourceMetadata > sourceToMetadata = opener.getSourceToMetadata();

		// create coloring and selection models
		DefaultSelectionModel< TableRowImageSegment > selectionModel = new DefaultSelectionModel<>();
		LazyCategoryColoringModel< TableRowImageSegment > coloringModel = new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) );
		SelectionColoringModel< TableRowImageSegment > selectionColoringModel = new SelectionColoringModel<>( coloringModel, selectionModel );

		// show data
		ImageView imageView = new ImageView( tableRowImageSegments, selectionColoringModel, sourceToMetadata );
		imageView.showImages();
		if ( ! objectPositionsAreCalibrated )
		{
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			imageView.getPrimaryLabelSource().getSpimSource().getSourceTransform( 0, 0, affineTransform3D );
			imageView.setSegmentPositionTransform( affineTransform3D );
		}
		Utils.centerComponentOnScreen( imageView.getWindow(), 10 );
		TableView< TableRowImageSegment > tableView = new TableView<>( tableRowImageSegments, selectionModel, selectionColoringModel );
		tableView.showTableAndMenu( imageView.getWindow() );
		imageView.setTableView( tableView );
	}

	public static void main( String[] args )
	{
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		// Run plugin from ImageJ Plugins menu.
	}
}
