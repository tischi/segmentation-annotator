package de.embl.cba.segmentationannotator.classify;

import de.embl.cba.tables.TableModel;
import de.embl.cba.tables.tablerow.TableRow;
import hr.irb.fastRandomForest.FastRandomForest;
import org.jetbrains.annotations.NotNull;
import trainableSegmentation.ReusableDenseInstance;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

public class WekaClassifier
{
	public < T extends TableRow > Classifier trainClassifier( TableModel< T > tableModel, List< String > featureColumns, String annotationColumn ) throws Exception
	{
		final List< String > annotations = getAnnotations( tableModel, annotationColumn );
		ArrayList< Attribute > attributes = createAttributes( featureColumns );
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

			final DenseInstance instance = createTrainingInstance( featureColumns, annotationColumn, annotationToIndex, tableRow );

			trainingInstances.add( instance );
		}
		trainingInstances.setClassIndex( featureColumns.size() );

		// Train
		final FastRandomForest randomForest = new FastRandomForest();
		randomForest.buildClassifier( trainingInstances );
		return randomForest;
	}

	@NotNull
	private ArrayList< Attribute > createAttributes( List< String > featureColumns )
	{
		ArrayList< Attribute > attributes = new ArrayList< Attribute >();

		for ( String feature : featureColumns )
		{
			attributes.add( new Attribute( feature ) );
		}
		return attributes;
	}

	public < T extends TableRow > void predict( TableModel< T > tableModel, List< String > featureColumns, String annotationColumn, String predictionColumn, Classifier classifier ) throws Exception
	{
		tableModel.addColumn( predictionColumn );

		final List< String > annotations = getAnnotations( tableModel, annotationColumn );
		ArrayList< Attribute > attributes = createAttributes( featureColumns );
		attributes.add( new Attribute( "predictions", annotations ) );

		Instances instances = new Instances( "predict", attributes, 1 );
		final ReusableDenseInstance reusableInstance = new ReusableDenseInstance( 1.0, new double[featureColumns.size() + 1] );
		reusableInstance.setDataset( instances );
		instances.setClassIndex( featureColumns.size() );

		for ( T tableRow : tableModel )
		{
			setReusablePredictionInstance( featureColumns, tableRow, reusableInstance );
			final double predictionValue = classifier.classifyInstance( reusableInstance );
			final String prediction = reusableInstance.classAttribute().value( (int) predictionValue );
			tableRow.setCell( predictionColumn, prediction );
		}
	}

	@NotNull
	private < T extends TableRow > List< String > getAnnotations( TableModel< T > tableModel, String annotationColumn )
	{
		final List< String > annotations = new ArrayList<>( new LinkedHashSet<>( tableModel.getColumn( annotationColumn ) ) );
		annotations.remove( "None" );
		Collections.sort( annotations );
		return annotations;
	}


	private < T extends TableRow > DenseInstance createTrainingInstance( List< String > featureColumns, String annotationColumn, HashMap< String, Integer > annotationToIndex, T tableRow )
	{
		// feature values
		final int numFeatures = featureColumns.size();
		// allocate one extra space for the class annotation,s
		final double[] doubles = new double[ numFeatures + 1 ];
		for ( int i = 0; i < numFeatures; i++ )
		{
			doubles[ i ] = Double.parseDouble( tableRow.getCell( featureColumns.get( i ) ) );
		}

		// class index
		doubles[ doubles.length - 1 ] = annotationToIndex.get( tableRow.getCell( annotationColumn ) );

		final DenseInstance instance = new DenseInstance( 1.0, doubles );
		return instance;
	}

	private < T extends TableRow > void setReusablePredictionInstance( List< String > featureColumns, T tableRow, ReusableDenseInstance reusableDenseInstance )
	{
		final int size = featureColumns.size();
		for ( int i = 0; i < size; i++ )
		{
			reusableDenseInstance.setValue( i, Double.parseDouble( tableRow.getCell( featureColumns.get( i ) ) )  );
		}
	}
}
