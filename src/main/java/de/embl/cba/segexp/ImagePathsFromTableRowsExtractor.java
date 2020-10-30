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
package de.embl.cba.segexp;

import de.embl.cba.tables.tablerow.TableRow;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImagePathsFromTableRowsExtractor < T extends TableRow >
{
	private final List< T > tableRows;
	private final String imageRootDirectory;
	private final String labelImageColumnName;
	private final Set< String > imageColumnNames;
	private Set< String > imagePaths;
	private HashMap< String, String > imagePathToLabelImageId;

	public ImagePathsFromTableRowsExtractor( final List< T > tableRows, final String imagesRootFolder, String labelImageColumnName, final String imageColumnsIdentifier )
	{
		this.tableRows = tableRows;
		this.imageRootDirectory = imagesRootFolder;
		this.labelImageColumnName = labelImageColumnName;
		this.imageColumnNames = fetchImageColumnNames( tableRows, imageColumnsIdentifier );
		this.imageColumnNames.add( labelImageColumnName );
		extractImagePaths();
	}

	public ImagePathsFromTableRowsExtractor( List< T > tableRows, String imageRootDirectory, String labelImageColumnName, Set< String > imageColumnNames )
	{
		this.tableRows = tableRows;
		this.imageRootDirectory = imageRootDirectory;
		this.labelImageColumnName = labelImageColumnName;
		this.imageColumnNames = imageColumnNames;
		this.imageColumnNames.add( labelImageColumnName );
		extractImagePaths();
	}

	public Set< String > getImagePaths()
	{
		return imagePaths; // includes labelImagePaths
	}

	public HashMap< String, String > getImagePathToLabelImageId()
	{
		return imagePathToLabelImageId;
	}

	private void extractImagePaths()
	{
		imagePaths = new HashSet<>(  );
		imagePathToLabelImageId = new HashMap< String, String >(  );

		for ( final T tableRow : tableRows )
		{
			for ( final String imageColumnName : imageColumnNames )
			{
				final String relativeImagePath = tableRow.getCell( imageColumnName );
				String absolutePath = createAbsolutePath( imageRootDirectory, relativeImagePath ).toString();
				imagePaths.add( absolutePath );

				if ( imageColumnName.equals( labelImageColumnName ) )
					imagePathToLabelImageId.put( absolutePath, relativeImagePath ); // relativePath is what the segment ImageId contains
			}
		}
	}

	private Set< String > fetchImageColumnNames( List< T > tableRowImageSegments, String imagePathColumIdentifier )
	{
		Set< String > columnNames = tableRowImageSegments.get( 0 ).getColumnNames();

		Set< String > imageColumnNames = new HashSet<>( );
		for ( String columnName : columnNames )
		{
			if ( columnName.contains( imagePathColumIdentifier ) )
				imageColumnNames.add( columnName );
		}

		return imageColumnNames;
	}

	private static Path createAbsolutePath( String rootPath, String relativePath )
	{
		final Path path = Paths.get( rootPath, relativePath );
		final Path normalize = path.normalize();
		return normalize;
	}
}
