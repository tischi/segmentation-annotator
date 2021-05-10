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

public class WekaClassifier < T extends TableRow >
{
	private final TableRowsModel< T > tableRowsModel;
	private final Set< String > featureColumns;
	private final String annotationColumn;
	private final ArrayList< Attribute > attributes;
	private final List< String > annotations;

	public WekaClassifier( TableRowsModel< T > tableRowsModel, Set< String > featureColumns, String annotationColumn )
	{
		this.tableRowsModel = tableRowsModel;
		this.featureColumns = featureColumns;
		this.annotationColumn = annotationColumn;

		annotations = getSortedAnnotations( tableRowsModel, annotationColumn );
		attributes = createSortedAttributes( featureColumns, annotations );
	}

	public Classifier train()
	{
		Instances trainingInstances = new Instances( "train", attributes, 1 );
		for ( T tableRow : tableRowsModel )
		{
			// annotation
			final String annotation = tableRow.getCell( annotationColumn );
			if ( annotation.equals( "None" ) ) continue;

			final DenseInstance instance = createTrainingInstance( attributes, annotationColumn, tableRow );

			trainingInstances.add( instance );
		}
		trainingInstances.setClassIndex( attributes.size() - 1 );

		// Train
		final FastRandomForest randomForest = new FastRandomForest();
		try
		{
			randomForest.buildClassifier( trainingInstances );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( "Could not build the classifier." );
		}
		return randomForest;
	}

	public void predict( String predictionColumn, Classifier classifier )
	{
		Instances instances = new Instances( "predict", attributes, 1 );
		final ReusableDenseInstance reusableInstance = new ReusableDenseInstance( 1.0, new double[ attributes.size() ] );
		reusableInstance.setDataset( instances );
		instances.setClassIndex( attributes.size() - 1 );

		// predict
		try
		{
			tableRowsModel.addColumn( predictionColumn, "None" );
			for ( T tableRow : tableRowsModel )
			{
				setReusablePredictionInstance( attributes, tableRow, reusableInstance );
				final double predictionValue = classifier.classifyInstance( reusableInstance );
				final String prediction = reusableInstance.classAttribute().value( ( int ) predictionValue );
				tableRow.setCell( predictionColumn, prediction );
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException("Error during classification");
		}
	}

	@NotNull
	private ArrayList< Attribute > createSortedAttributes( Collection< String > featureColumns, List< String > annotations )
	{
		final List< String > sortedFeatureColumns = new ArrayList<>( featureColumns );
		Collections.sort( sortedFeatureColumns );

		ArrayList< Attribute > attributes = new ArrayList< Attribute >();
		for ( String feature : sortedFeatureColumns )
		{
			attributes.add( new Attribute( feature ) );
		}
		attributes.add( new Attribute( "class", annotations ) );

		return attributes;
	}



	@NotNull
	private < T extends TableRow > List< String > getSortedAnnotations( TableRowsModel< T > tableRowsModel, String annotationColumn )
	{
		final List< String > annotations = new ArrayList<>( new LinkedHashSet<>( tableRowsModel.getColumn( annotationColumn ) ) );
		annotations.remove( "None" );
		Collections.sort( annotations );
		return annotations;
	}

	private < T extends TableRow > DenseInstance createTrainingInstance( ArrayList< Attribute > attributes, String annotationColumn, T tableRow )
	{
		final int numAttributes = attributes.size();

		final double[] doubles = new double[ numAttributes ];

		// class
		final String annotation = tableRow.getCell( annotationColumn );
		doubles[ numAttributes - 1 ] = attributes.indexOf( annotation );

		// features
		for ( int i = 0; i < numAttributes - 1; i++ )
		{
			doubles[ i ] = Double.parseDouble( tableRow.getCell( attributes.get( i ).name() ) );
		}

		final DenseInstance instance = new DenseInstance( 1.0, doubles );

		return instance;
	}

	private < T extends TableRow > void setReusablePredictionInstance( List< Attribute > attributes, T tableRow, ReusableDenseInstance reusableDenseInstance )
	{
		final int numAttributes = attributes.size();
		// the first is the class attribute, which will be predicted
		for ( int i = 0; i < numAttributes - 1; i++ )
		{
			reusableDenseInstance.setValue( i, Double.parseDouble( tableRow.getCell( attributes.get( i ).name() ) )  );
		}
	}
}
