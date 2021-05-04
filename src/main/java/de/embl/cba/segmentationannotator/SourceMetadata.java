package de.embl.cba.segmentationannotator;

public class SourceMetadata
{
	public String groupId; // may be null, in which cases no groupings will be defined in BDV
	public String imageId;
	public boolean isLabelSource;
	public boolean isPrimaryLabelSource;
	public String channelName;
}
