package de.embl.cba.segmentationannotator.classify;

import ij.gui.GenericDialog;

public class ClassifyDialog
{
	public void showDialog()
	{
		final GenericDialog gd = new GenericDialog( "Classify" );
		gd.showDialog();
		if ( gd.wasCanceled() ) return;

	}
}
