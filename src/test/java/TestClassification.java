import de.embl.cba.segmentationannotator.classify.WekaClassifier;
import de.embl.cba.segmentationannotator.command.OpenIntensityAndLabelImagePlusAndMorpholibJResultsTableCommand;
import de.embl.cba.tables.tablerow.DefaultTableRowsModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import ij.IJ;
import net.imagej.ImageJ;
import weka.classifiers.Classifier;

import java.util.HashSet;
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

		final OpenIntensityAndLabelImagePlusAndMorpholibJResultsTableCommand command = new OpenIntensityAndLabelImagePlusAndMorpholibJResultsTableCommand();
		command.intensityImage = IJ.openImage( root + "src/test/resources/golgi-intensities.tif");
		command.labelImage = IJ.openImage( root + "src/test/resources/golgi-cell-labels.tif");
		command.resultsTableTitle = "golgi-cell-features.csv";
		command.run();

		final List< TableRowImageSegment > imageSegments = command.getTableRowImageSegments();

		final DefaultTableRowsModel< TableRowImageSegment > tableRowsModel = new DefaultTableRowsModel<>( imageSegments );

		int rowIndex = 0;
		final String annotationColumn = "Annotation";
		tableRowsModel.addColumn( annotationColumn );
		tableRowsModel.getRow( rowIndex++ ).setCell( annotationColumn, "Class A" );
		tableRowsModel.getRow( rowIndex++ ).setCell( annotationColumn, "Class A" );
		tableRowsModel.getRow( rowIndex++  ).setCell( annotationColumn, "Class B" );
		tableRowsModel.getRow( rowIndex++  ).setCell( annotationColumn, "Class B" );
		tableRowsModel.getRow( rowIndex++  ).setCell( annotationColumn, "Class C" );
		tableRowsModel.getRow( rowIndex++  ).setCell( annotationColumn, "Class C" );

		final WekaClassifier wekaClassifier = new WekaClassifier();

		final Set< String > features = new HashSet<>();
		features.add( "Golgi_Count" );
		features.add( "Golgi_Mean_Area" );
		final Classifier classifier = wekaClassifier.trainClassifier( tableRowsModel, features, annotationColumn );
		wekaClassifier.predict( tableRowsModel, features, annotationColumn, "Prediction", classifier );

		for ( TableRowImageSegment tableRowImageSegment : tableRowsModel )
		{
			System.out.println( tableRowImageSegment.getCell( "Prediction" ) );
		}
	}
}
