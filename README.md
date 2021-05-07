# Segmentation annotator

Fiji plugin for exploration and annotation of segmented images.

Explore
<a href="https://youtu.be/EvAvqQKFkMQ"><img src="doc/segmented-image-exploration-youtube.png" width="185"></a>
Render
<a href="https://youtu.be/SOw_QtP0DsM"><img src="doc/3d-object-rendering-youtube.png" width="220"></a>
Annotate
<a href="https://youtu.be/PMe4d6EpqGk"><img src="doc/object-annotation-youtube.png" width="200"></a>

## Install

1. Please install [Fiji](https://fiji.sc) onto your computer.
1. Start Fiji and add the Update Site **SegmentationAnnotator** like this:
   - [ Help > Update.. ]
   - [ Manage update sites ]
      - [X] **SegmentationAnnotator** 
   - [ Close ]
1. Restart Fiji

## Quick start

After installation (see above). You can run below ImageJ macros to launch the application:

- [Minimal 2D segmented image exploration](https://raw.githubusercontent.com/tischi/segmentation-annotator/master/scripts/2d-image-two-objects.ijm)
- [A bit larger 3D segmented image exploration](https://raw.githubusercontent.com/tischi/segmentation-annotator/master/scripts/3d-image-many-objects.ijm)

## Run

### Open label and intensity images

- `[ Plugins > Segmentation > Annotator > Open Intensity and Label Mask Images... ]`



### Open data set from table

- Download a demo data set:
   - [Two segmented images (cells and golgi)](https://oc.embl.de/index.php/s/L4Kv5YgKgiMCFUe)
- `[ Plugins > Segmentation > Annotator > Open Dataset from Table... ]`

<img src="https://user-images.githubusercontent.com/2157566/101176937-7ba63480-3647-11eb-9952-543e153a99e5.png" width="700">

## Use

- Mouse: `Right click`: Shows a context menu through with most functionality can be accessed.
- Mouse: `Ctrl + Left-Click`: Selects/Deselects the image segment at the location of the mouse pointer.

## Annotate

<img src="./doc/annotate.png" width="700">

