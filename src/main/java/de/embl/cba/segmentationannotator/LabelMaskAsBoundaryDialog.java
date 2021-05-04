package de.embl.cba.segmentationannotator;

import ij.gui.GenericDialog;

public class LabelMaskAsBoundaryDialog
{
	private static boolean showAsBoundary = false;
	private static int boundaryThickness = 1;

	public void showDialog()
	{
		final GenericDialog gd = new GenericDialog( "Boundary thickness" );
		gd.addCheckbox( "Show label mask as boundary", showAsBoundary );
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
