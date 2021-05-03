package de.embl.cba.segmentationannotator.labels;

import ij.ImageStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

public class LabelAnalyzer
{

	public final static Map< Integer, VolumeAndAnchor > analyzeLabels( ImageStack image, double[] voxelSize )
	{
		int sizeX = image.getWidth();
		int sizeY = image.getHeight();
		int sizeZ = image.getSize();

		final HashMap< Integer, VolumeAndAnchor > indexToVolumeAndAnchor = new HashMap<>();

		// iterate on image pixels
		for (int z = 0; z < sizeZ; z++)
		{
			for (int y = 0; y < sizeY; y++)
			{
				for (int x = 0; x < sizeX; x++)
				{
					final int index = ( int ) image.getVoxel( x, y, z );

					if ( index == 0 ) continue;

					if ( ! indexToVolumeAndAnchor.containsKey( index ) )
					{
						final VolumeAndAnchor volumeAndAnchor = new VolumeAndAnchor();
						indexToVolumeAndAnchor.put( index, volumeAndAnchor );
						volumeAndAnchor.anchor = new double[]{ x * voxelSize[ 0 ], y * voxelSize[ 1 ], z * voxelSize[ 2 ] };
					}
					indexToVolumeAndAnchor.get( index ).numPixels++;
				}
			}
		}
		return indexToVolumeAndAnchor;
	}
}
