package de.embl.cba.segmentationannotator.classify;

import ij.IJ;
import net.haesleinhuepf.clijx.weka.TrainWekaFromTable;

public class CLIJAvailabilityChecker
{
	public boolean isAvailable()
	{
		try
		{
			final TrainWekaFromTable trainWekaFromTable = new TrainWekaFromTable();
			return true;
		}
		catch (final NoClassDefFoundError err)
		{
			IJ.error( "This functionality requires the CLIJ and CLIJ2 update sites.\n" +
					"[ Help > Update > Manage Update Sites ]" );
			return false;
		}
	}

}
