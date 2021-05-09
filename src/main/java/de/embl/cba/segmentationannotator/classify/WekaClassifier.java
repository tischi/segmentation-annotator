package de.embl.cba.segmentationannotator.classify;

import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowsModel;
import hr.irb.fastRandomForest.FastRandomForest;
import org.jetbrains.annotations.NotNull;
import trainableSegmentation.ReusableDenseInstance;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WekaClassifier
{
	public < T extends TableRow > Classifier trainClassifier( TableRowsModel< T > tableRowsModel, Set< String > featureColumns, String annotationColumn )
	{
		final List< String > annotations = getAnnotations( tableRowsModel, annotationColumn );
		ArrayList< Attribute > attributes = createAttributes( featureColumns );
		attributes.add( new Attribute( "annotations", annotations ) );

		final HashMap< String, Integer > annotationToIndex = new HashMap<>();
		for ( int i = 0; i < annotations.size(); i++ )
		{
			annotationToIndex.put( annotations.get( i ), i );
		}

		Instances trainingInstances = new Instances( "train", attributes, 1 );
		for ( T tableRow : tableRowsModel )
		{
			// annotation
			final String annotation = tableRow.getCell( annotationColumn );
			if ( annotation.equals( "None" ) ) continue;

			final DenseInstance instance = createTrainingInstance( attributes, annotationColumn, annotationToIndex, tableRow );

			trainingInstances.add( instance );
		}
		trainingInstances.setClassIndex( featureColumns.size() );

		// Train
		final FastRandomForest randomForest = new FastRandomForest();
		try
		{
			randomForest.buildClassifier( trainingInstances );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		return randomForest;
	}

	@NotNull
	private ArrayList< Attribute > createAttributes( Collection< String > featureColumns )
	{
		final List< String > sortedFeatureColumns = new ArrayList<>( featureColumns );
		Collections.sort( sortedFeatureColumns );

		ArrayList< Attribute > attributes = new ArrayList< Attribute >();

		for ( String feature : sortedFeatureColumns )
		{
			attributes.add( new Attribute( feature ) );
		}

		return attributes;
	}

	public < T extends TableRow > void predict( TableRowsModel< T > tableRowsModel, Set< String > featureColumns, String annotationColumn, String predictionColumn, Classifier classifier ) throws Exception
	{
		tableRowsModel.addColumn( predictionColumn );

		final List< String > annotations = getAnnotations( tableRowsModel, annotationColumn );
		ArrayList< Attribute > attributes = createAttributes( featureColumns );
		attributes.add( new Attribute( "predictions", annotations ) );

		Instances instances = new Instances( "predict", attributes, 1 );
		final ReusableDenseInstance reusableInstance = new ReusableDenseInstance( 1.0, new double[featureColumns.size() + 1] );
		reusableInstance.setDataset( instances );
		instances.setClassIndex( featureColumns.size() );

		for ( T tableRow : tableRowsModel )
		{
			setReusablePredictionInstance( attributes, tableRow, reusableInstance );
			final double predictionValue = classifier.classifyInstance( reusableInstance );
			final String prediction = reusableInstance.classAttribute().value( (int) predictionValue );
			tableRow.setCell( predictionColumn, prediction );
		}
	}

	@NotNull
	private < T extends TableRow > List< String > getAnnotations( TableRowsModel< T > tableRowsModel, String annotationColumn )
	{
		final List< String > annotations = new ArrayList<>( new LinkedHashSet<>( tableRowsModel.getColumn( annotationColumn ) ) );
		annotations.remove( "None" );
		Collections.sort( annotations );
		return annotations;
	}

	private < T extends TableRow > DenseInstance createTrainingInstance( ArrayList< Attribute > attributes, String annotationColumn, HashMap< String, Integer > annotationToIndex, T tableRow )
	{
		// feature values
		final int numAttributes = attributes.size();
		// allocate one extra space for the class annotation,s
		final double[] doubles = new double[ numAttributes + 1 ];
		for ( int i = 0; i < numAttributes; i++ )
		{
			doubles[ i ] = Double.parseDouble( tableRow.getCell( attributes.get( i ).name() ) );
		}

		// class index
		doubles[ doubles.length - 1 ] = annotationToIndex.get( tableRow.getCell( annotationColumn ) );

		final DenseInstance instance = new DenseInstance( 1.0, doubles );
		return instance;
	}

	private < T extends TableRow > void setReusablePredictionInstance( List< Attribute > attributes, T tableRow, ReusableDenseInstance reusableDenseInstance )
	{
		final int size = attributes.size();
		for ( int i = 0; i < size; i++ )
		{
			reusableDenseInstance.setValue( i, Double.parseDouble( tableRow.getCell( attributes.get( i ).name() ) )  );
		}
	}
}
