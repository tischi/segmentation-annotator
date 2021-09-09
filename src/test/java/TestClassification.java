import de.embl.cba.segmentationannotator.classify.WekaClassifier;
import de.embl.cba.segmentationannotator.command.ViewLabelMaskAndIntensityImagePlusAndMorpholibJResultsTableCommand;
import de.embl.cba.tables.tablerow.DefaultTableRowsModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import net.imagej.ImageJ;
import weka.classifiers.Classifier;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TestClassification
{
	public static void main( String[] args ) throws Exception
	{
		String root = "/Users/tischer/Documents/segmentations-explorer/";

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// open results table
		IJ.open( root + "src/test/resources/golgi-cell-features.csv");

		final ViewLabelMaskAndIntensityImagePlusAndMorpholibJResultsTableCommand command = new ViewLabelMaskAndIntensityImagePlusAndMorpholibJResultsTableCommand();
		command.intensityImage = IJ.openImage( root + "src/test/resources/golgi-intensities.tif");
		command.labelImage = IJ.openImage( root + "src/test/resources/golgi-cell-labels.tif");
		command.resultsTableTitle = "golgi-cell-features.csv";
		command.run();

		final List< TableRowImageSegment > imageSegments = command.getTableRowImageSegments();

		final DefaultTableRowsModel< TableRowImageSegment > tableRowsModel = new DefaultTableRowsModel<>( imageSegments );

		final String annotationColumn = "Annotation";
		tableRowsModel.addColumn( annotationColumn, "None" );
		final Iterator< TableRowImageSegment > iterator = tableRowsModel.iterator();
		iterator.next().setCell( annotationColumn, "Class A" );
		iterator.next().setCell( annotationColumn, "Class A" );
		iterator.next().setCell( annotationColumn, "Class B" );
		iterator.next().setCell( annotationColumn, "Class B" );
		iterator.next().setCell( annotationColumn, "Class C" );
		iterator.next().setCell( annotationColumn, "Class C" );

		final Set< String > features = new HashSet<>();
		features.add( "Golgi_Count" );
		features.add( "Golgi_Mean_Area" );

		final WekaClassifier wekaClassifier = new WekaClassifier( tableRowsModel, features, annotationColumn );

		final Classifier classifier = wekaClassifier.train();
		wekaClassifier.predict( "Prediction", classifier );

		for ( TableRowImageSegment tableRowImageSegment : tableRowsModel )
		{
			System.out.println( tableRowImageSegment.getCell( "Prediction" ) );
		}
	}
}
