run("Close All");

// open intensity image
open("https://raw.githubusercontent.com/tischi/segmentation-annotator/master/src/test/resources/intensity_image_3d.zip");
rename("intensities");

// open label image (beware that this segmentation is not very good as many nuclei are connected)
open("https://raw.githubusercontent.com/tischi/segmentation-annotator/master/src/test/resources/label_image_3d.zip");
rename("labels");

// open with SegmentationAnnotator
run("Open Intensity and Label Mask Images...", "intensityimageplus=intensities labelimageplus=labels");