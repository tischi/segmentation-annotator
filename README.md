# Segmentation annotator

Fiji plugin for exploration and annotation of segmented images.

## Install

This plugin is still a beta version!

This plugin ships very recent versions of bigdataviewer and imglib2 which are currently not compatible with other Fiji plugins!

Thus, please download a dedicated Fiji for this plugin.

1. Please [download](https://fiji.sc) a fresh Fiji onto your computer.
1. Extract the downloaded Fiji, e.g. onto your Desktop
1. Rename the folder `Fiji.app` to `Fiji-SegmentationAnnotator.app`
1. Start Fiji and add the Update Site `SegmentationAnnotator` like this:
   - [ Help > Update.. ]
   - [ Manage update sites ]
   - [ Add update site ] and manually fill in the following fields:
      - Name: `SegmentationAnnotator` 
      - URL: `https://sites.imagej.net/SegmentationAnnotator`
      - It should look like this: 
      - <img width="500" alt="image" src="https://user-images.githubusercontent.com/2157566/101176333-9e841900-3646-11eb-8673-a821e9129627.png">
   - [ Close ]
1. Restart Fiji


## Run

- (Optional) Download a demo data set:
   - [Two segmented images (cells and golgi)](https://oc.embl.de/index.php/s/L4Kv5YgKgiMCFUe)
- `[ Plugins > Segmentation > Annotator > Open Dataset from Table... ]`

BigDataViewer and a table should appear:

![image](https://user-images.githubusercontent.com/2157566/101176937-7ba63480-3647-11eb-9952-543e153a99e5.png)

## Use

- Mouse: `Right click`: Shows a context menu through with most functionality can be accessed.
- Mouse: `Ctrl + Left-Click`: Selects/Deselects the image segment at the location of the mouse pointer.

## Annotate

<img src="./doc/annotate.png" width="700">

