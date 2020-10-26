/*-
 * #%L
 * TODO
 * %%
 * Copyright (C) 2018 - 2020 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.embl.cba.ide;

import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.image.FileImageSourcesModel;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.nio.file.Path;
import java.util.*;


public class ImagePathsFromTableRowsExtractor< T extends TableRowImageSegment >
{
	public static final String PATH_COLUMN_ID = "Path_";
	public ArrayList< String > labelMaskColumnIds;

	private final List< T > tableRowImageSegments;
	private Set< String > columnNames;
	private Map< String, String > imageNameToPathColumnName;
	private FileImageSourcesModel imageSourcesModel;
	private final String imageRootFolder;
	private final boolean is2D;
	private Set< String > excludeImages;

	public ImagePathsFromTableRowsExtractor(
			final List< T > tableRowImageSegments,
			final String imageRootFolder,
			boolean is2D )
	{
		this.tableRowImageSegments = tableRowImageSegments;
		this.imageRootFolder = imageRootFolder;
		this.is2D = is2D;
		this.excludeImages = new HashSet<>(  );

		columnNames = tableRowImageSegments.get( 0 ).getColumnNames();
	}

	public void excludeImage( String excludeImages )
	{
		this.excludeImages.add( excludeImages );
	}

	public void createLabelMaskIds()
	{
		labelMaskColumnIds = new ArrayList< >();
		labelMaskColumnIds.add( "Objects_" );
		labelMaskColumnIds.add( "labelMasks" );
		labelMaskColumnIds.add( "LabelMask" );
		labelMaskColumnIds.add( "LabelImage" );
		labelMaskColumnIds.add( "Label" );
		labelMaskColumnIds.add( "label" );
	}

	public FileImageSourcesModel getImageSourcesModel()
	{
		createLabelMaskIds(); // TODO: how to handle this? could be anything...
		imageNameToPathColumnName = getImageNameToPathColumnName();
		imageSourcesModel = createImageSourcesModel();
		return imageSourcesModel;
	}

	private FileImageSourcesModel createImageSourcesModel( )
	{
		imageSourcesModel = new FileImageSourcesModel( is2D );

		final Set< String > allImageNames = imageNameToPathColumnName.keySet();

		final HashSet< String > imageNames = new HashSet<>();
		for ( String imageName : allImageNames )
			if ( ! excludeImages.contains( imageName ) )
				imageNames.add( imageName );

		for ( TableRowImageSegment tableRowImageSegment : tableRowImageSegments )
		{
			final List< String > imageSetIds = getImageSetIds( tableRowImageSegment, imageNames );

			for ( String imageName : imageNames )
			{
				final String imagePath = getImagePath( tableRowImageSegment, imageName );

				final String imageId = imagePath;

				if ( ! imageSourcesModel.sources().containsKey( imageId ) )
				{
					final Path absoluteImagePath = Tables.getAbsolutePath( imageRootFolder, imagePath );

					if ( absoluteImagePath.toFile().exists() )
					{
						final String imageDisplayName = absoluteImagePath.getFileName().toString();

						imageSourcesModel.addSourceAndMetadata(
								imageId,
								imageDisplayName,
								absoluteImagePath.toString(),
								imageSetIds,
								getImageModality( imageName ) );
					}
					else
					{
						Logger.warn( "Image file not found: " + absoluteImagePath );
					}
				}
			}
		}

		return imageSourcesModel;
	}

	private String getImagePath( TableRow tableRow, String imageName )
	{
		final String imagePathColumn = imageNameToPathColumnName.get( imageName );
		return tableRow.getCell( imagePathColumn );
	}

	private List< String > getImageSetIds( TableRow tableRow, Set< String > imageNames )
	{
		ArrayList< String > imageSetIds = new ArrayList<>(  );

		for ( String imageName : imageNames )
			imageSetIds.add( getImagePath( tableRow, imageName ) );

		return imageSetIds;
	}

	private Metadata.Modality getImageModality( String imageName )
	{
		final Metadata.Modality modality;

		if ( FileUtils.stringContainsItemFromList( imageName, labelMaskColumnIds ) )
			modality = Metadata.Modality.Segmentation;
		else
			modality = Metadata.Modality.FM;

		return modality;
	}

	public Map< String, String > getImageNameToPathColumnName( )
	{
		final HashMap< String, String > imageNameToPathColumnName = new HashMap<>();

		for ( String column : columnNames )
		{
			if ( ! column.contains( PATH_COLUMN_ID ) ) continue;

			final String image = column.split( PATH_COLUMN_ID )[ 1 ];

			imageNameToPathColumnName.put( image, column );

		}

		return imageNameToPathColumnName;
	}


}
