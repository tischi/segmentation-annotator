package de.embl.cba.segmentationannotator.dialog;

import ij.gui.GenericDialog;

public class LabelMaskDisplayDialog
{
	private static boolean showAsBoundary = false;
	private static int boundaryThickness = 1;

	public void showDialog()
	{
		final GenericDialog gd = new GenericDialog( "Boundary thickness" );
		gd.addCheckbox( "Configure Label Mask Display", showAsBoundary );
		gd.addNumericField( "Boundary thickness [pixels]", boundaryThickness, 1 );
		gd.showDialog();
		if ( gd.wasCanceled() ) return;
		showAsBoundary = gd.getNextBoolean();
		boundaryThickness = (int) gd.getNextNumber();
	}

	public boolean isShowAsBoundary()
	{
		return showAsBoundary;
	}

	public int getBoundaryThickness()
	{
		return boundaryThickness;
	}
}
