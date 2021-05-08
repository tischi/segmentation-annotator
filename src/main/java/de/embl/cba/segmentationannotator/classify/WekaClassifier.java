package de.embl.cba.segmentationannotator.classify;

import de.embl.cba.tables.TableModel;
import de.embl.cba.tables.tablerow.TableRow;
import hr.irb.fastRandomForest.FastRandomForest;
import org.jetbrains.annotations.NotNull;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class WekaClassifier
{
	public < T extends TableRow > void trainClassifier( TableModel< T > tableModel, List< String > featureColumns, String annotationColumn ) throws Exception
	{
		ArrayList< Attribute > attributes = new ArrayList< Attribute >();

		for ( String feature : featureColumns )
		{
			attributes.add( new Attribute( feature ) );
		}

		final List< String > annotations = new ArrayList<>( new HashSet<>( tableModel.getColumn( annotationColumn ) ) );
		annotations.remove( "None" );
		attributes.add( new Attribute( "annotations", annotations ) );

		final HashMap< String, Integer > annotationToIndex = new HashMap<>();
		for ( int i = 0; i < annotations.size(); i++ )
		{
			annotationToIndex.put( annotations.get( i ), i );
		}

		Instances trainingInstances = new Instances( "train", attributes, 1 );
		for ( T tableRow : tableModel )
		{
			// annotation
			final String annotation = tableRow.getCell( annotationColumn );
			if ( annotation.equals( "None" ) ) continue;

			final DenseInstance instance = getDenseInstance( featureColumns, annotationColumn, annotationToIndex, tableRow );

			trainingInstances.add( instance );
		}

		Instances predictionInstances = new Instances( "predict", attributes, 1 );
		for ( T tableRow : tableModel )
		{
			// annotation
			final String annotation = tableRow.getCell( annotationColumn );
			if ( !annotation.equals( "None" ) ) continue;

			final DenseInstance instance = getDenseInstance( featureColumns, annotationColumn, annotationToIndex, tableRow );

			predictionInstances.add( instance );
		}

		// Train
		final FastRandomForest randomForest = new FastRandomForest();
		randomForest.buildClassifier( trainingInstances );

		// Predict
		for ( Instance predictionInstance : predictionInstances )
		{
			final double prediction = randomForest.classifyInstance( predictionInstance );
			final String predictedAnnotation = predictionInstance.classAttribute().value( predictionInstance.classIndex() );
		}
	}


	private < T extends TableRow > DenseInstance getDenseInstance( List< String > featureColumns, String annotationColumn, HashMap< String, Integer > annotationToIndex, T tableRow )
	{
		// feature values
		final double[] doubles = new double[ featureColumns.size() + 1 ];
		for ( int i = 0; i < doubles.length; i++ )
		{
			doubles[ i ] = Double.parseDouble( tableRow.getCell( featureColumns.get( i ) ) );
		}

		// class index
		doubles[ doubles.length - 1 ] = annotationToIndex.get( tableRow.getCell( annotationColumn ) );

		final DenseInstance instance = new DenseInstance( 1.0, doubles );
		return instance;
	}
}
