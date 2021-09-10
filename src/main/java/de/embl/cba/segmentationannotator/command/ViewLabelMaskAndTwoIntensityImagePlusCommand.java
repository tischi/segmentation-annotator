package de.embl.cba.segmentationannotator.command;

import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;

import static de.embl.cba.segmentationannotator.command.ViewLabelMaskAndIntensityImagePlusCommand.showImages;


@Plugin(type = Command.class, menuPath = "Plugins > Segmentation > Annotator > View Label Mask and Two Intensity Images..." )
public class ViewLabelMaskAndTwoIntensityImagePlusCommand implements Command
{
	@Parameter( label = "Label Mask Image" )
	public ImagePlus labelImage;

	@Parameter( label = "Intensity Image" )
	public ImagePlus intensityImage;

	@Parameter( label = "Intensity Image 2" )
	public ImagePlus intensityImage2;

	@Override
	public void run()
	{
		final ArrayList< ImagePlus > intensityImages = new ArrayList<>();
		intensityImages.add( intensityImage );
		intensityImages.add( intensityImage2 );
		showImages( labelImage, intensityImages );
	}
}
