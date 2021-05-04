package de.embl.cba.segmentationannotator.label;

import ij.ImageStack;
import ij.measure.Calibration;

import java.util.HashMap;
import java.util.Map;

public class LabelAnalyzer
{
	public final static Map< Integer, SegmentFeatures > analyzeLabels( ImageStack labels, ImageStack intensities, Calibration calibration )
	{
		int sizeX = labels.getWidth();
		int sizeY = labels.getHeight();
		int sizeZ = labels.getSize();

		final HashMap< Integer, SegmentFeatures > indexToFeatures = new HashMap<>();

		for (int z = 0; z < sizeZ; z++)
		{
			for (int y = 0; y < sizeY; y++)
			{
				for (int x = 0; x < sizeX; x++)
				{
					final int index = ( int ) labels.getVoxel( x, y, z );

					if ( index == 0 ) continue;

					if ( ! indexToFeatures.containsKey( index ) )
					{
						final SegmentFeatures segmentFeatures = new SegmentFeatures();
						indexToFeatures.put( index, segmentFeatures );
					}
					final SegmentFeatures segmentFeatures = indexToFeatures.get( index );
					segmentFeatures.numPixels++;
					segmentFeatures.anchorX += x;
					segmentFeatures.anchorY += y;
					segmentFeatures.anchorZ += z;
					segmentFeatures.meanIntensity += intensities.getVoxel( x, y, z );
				}
			}
		}

		for ( SegmentFeatures features : indexToFeatures.values() )
		{
			features.anchorX *= calibration.pixelWidth / features.numPixels ;
			features.anchorY *= calibration.pixelHeight / features.numPixels;
			features.anchorZ *= calibration.pixelDepth / features.numPixels;
			features.meanIntensity /= features.numPixels;
		}

		return indexToFeatures;
	}
}
