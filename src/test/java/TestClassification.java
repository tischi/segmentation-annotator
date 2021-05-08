import de.embl.cba.segmentationannotator.classify.WekaClassifier;
import de.embl.cba.segmentationannotator.command.OpenIntensityAndLabelImagePlusAndMorpholibJResultsTableCommand;
import de.embl.cba.tables.DefaultTableModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import net.imagej.ImageJ;
import weka.classifiers.Classifier;

import java.util.ArrayList;
import java.util.List;

public class TestClassification
{
	public static void main( String[] args ) throws Exception
	{
		String root = "/Users/tischer/Documents/segmentations-explorer/";

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// open results table
		IJ.open( root + "src/test/resources/golgi-cell-features.csv");

		final OpenIntensityAndLabelImagePlusAndMorpholibJResultsTableCommand command = new OpenIntensityAndLabelImagePlusAndMorpholibJResultsTableCommand();
		command.intensityImage = IJ.openImage( root + "src/test/resources/golgi-intensities.tif");
		command.labelImage = IJ.openImage( root + "src/test/resources/golgi-cell-labels.tif");
		command.resultsTableTitle = "golgi-cell-features.csv";
		command.run();

		final List< TableRowImageSegment > imageSegments = command.getTableRowImageSegments();

		final DefaultTableModel< TableRowImageSegment > tableModel = new DefaultTableModel<>( imageSegments );

		int rowIndex = 0;
		final String annotationColumn = "Annotation";
		tableModel.addColumn( annotationColumn );
		tableModel.getRow( rowIndex++ ).setCell( annotationColumn, "Class A" );
		tableModel.getRow( rowIndex++ ).setCell( annotationColumn, "Class A" );
		tableModel.getRow( rowIndex++  ).setCell( annotationColumn, "Class B" );
		tableModel.getRow( rowIndex++  ).setCell( annotationColumn, "Class B" );
		tableModel.getRow( rowIndex++  ).setCell( annotationColumn, "Class C" );
		tableModel.getRow( rowIndex++  ).setCell( annotationColumn, "Class C" );

		final WekaClassifier wekaClassifier = new WekaClassifier();

		final ArrayList< String > features = new ArrayList<>();
		features.add( "Golgi_Count" );
		features.add( "Golgi_Mean_Area" );
		final Classifier classifier = wekaClassifier.trainClassifier( tableModel, features, annotationColumn );
		wekaClassifier.predict( tableModel, features, annotationColumn, "Prediction", classifier );

		for ( TableRowImageSegment tableRowImageSegment : tableModel )
		{
			System.out.println( tableRowImageSegment.getCell( "Prediction" ) );
		}
	}
}
