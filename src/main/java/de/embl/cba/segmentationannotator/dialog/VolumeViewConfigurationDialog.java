package de.embl.cba.segmentationannotator.dialog;

import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;

public class VolumeViewConfigurationDialog
{
	private static boolean showSelectedSegmentsIn3D = false;

	public static boolean showDialog()
	{
		final GenericDialog gd = new GenericDialog( "3D View Preferences" );
		gd.addCheckbox( "Show selected segments in 3D", showSelectedSegmentsIn3D );
		gd.showDialog();
		if ( gd.wasCanceled() ) return false;
		showSelectedSegmentsIn3D = gd.getNextBoolean();
		return true;
	}

	public static boolean isShowSelectedSegmentsIn3D()
	{
		return showSelectedSegmentsIn3D;
	}
}
