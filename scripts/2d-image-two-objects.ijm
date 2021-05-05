run("Close All");

// open intensity image
open("https://github.com/NEUBIAS/training-resources/raw/master/image_data/xy_8bit__two_cells.tif");
rename("intensities");

// create label mask image
setThreshold(49, 255);
setOption("BlackBackground", true);
run("Analyze Particles...", "  show=[Count Masks]");
rename("labels");
setMinAndMax(0, 2);

// open with SegmentationAnnotator
run("Open Intensity and Label Mask Images...", "intensityimageplus=intensities labelimageplus=labels");