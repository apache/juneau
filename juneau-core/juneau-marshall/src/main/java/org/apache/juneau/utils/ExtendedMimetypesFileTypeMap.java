// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.utils;

import javax.activation.*;

/**
 * An extension of {@link javax.activation.MimetypesFileTypeMap} that includes many more media types.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ExtendedMimetypesFileTypeMap extends MimetypesFileTypeMap {

	/**
	 * Reusable map since this object is somewhat expensive to create.
	 */
	public static final ExtendedMimetypesFileTypeMap DEFAULT = new ExtendedMimetypesFileTypeMap();

	/**
	 * Constructor.
	 */
	public ExtendedMimetypesFileTypeMap() {
		super();
		addMimeTypes("application/epub+zip epub");
		addMimeTypes("application/java-archive jar");
		addMimeTypes("application/javascript js");
		addMimeTypes("application/json json");
		addMimeTypes("application/msword doc");
		addMimeTypes("application/ogg ogx");
		addMimeTypes("application/pdf pdf");
		addMimeTypes("application/rtf rtf");
		addMimeTypes("application/vnd.amazon.ebook azw");
		addMimeTypes("application/vnd.apple.installer+xml mpkg");
		addMimeTypes("application/vnd.mozilla.xul+xml xul");
		addMimeTypes("application/vnd.ms-excel xls");
		addMimeTypes("application/vnd.ms-powerpoint ppt");
		addMimeTypes("application/vnd.oasis.opendocument.presentation odp");
		addMimeTypes("application/vnd.oasis.opendocument.spreadsheet ods");
		addMimeTypes("application/vnd.oasis.opendocument.text odt");
		addMimeTypes("application/vnd.visio vsd");
		addMimeTypes("application/x-7z-compressed 7z");
		addMimeTypes("application/x-abiword abw");
		addMimeTypes("application/x-bzip bz");
		addMimeTypes("application/x-bzip2 bz2");
		addMimeTypes("application/x-csh csh");
		addMimeTypes("application/x-rar-compressed rar");
		addMimeTypes("application/x-sh sh");
		addMimeTypes("application/x-shockwave-flash swf");
		addMimeTypes("application/x-tar tar");
		addMimeTypes("application/xhtml+xml xhtml");
		addMimeTypes("application/xml xml");
		addMimeTypes("application/zip zip");
		addMimeTypes("audio/aac aac");
		addMimeTypes("audio/midi mid midi");
		addMimeTypes("audio/ogg oga");
		addMimeTypes("audio/webm weba");
		addMimeTypes("audio/x-wav wav");
		addMimeTypes("font/ttf ttf");
		addMimeTypes("font/woff woff");
		addMimeTypes("font/woff2 woff2");
		addMimeTypes("image/gif gif");
		addMimeTypes("image/jpeg jpeg jpg");
		addMimeTypes("image/png png");
		addMimeTypes("image/svg+xml svg");
		addMimeTypes("image/tiff tif tiff");
		addMimeTypes("image/webp webp");
		addMimeTypes("image/x-icon ico");
		addMimeTypes("text/calendar ics");
		addMimeTypes("text/css css");
		addMimeTypes("text/csv csv");
		addMimeTypes("text/html htm html");
		addMimeTypes("text/plain txt");
		addMimeTypes("video/3gpp 3gp");
		addMimeTypes("video/3gpp2 3g2");
		addMimeTypes("video/mpeg mpeg");
		addMimeTypes("video/ogg ogv");
		addMimeTypes("video/webm webm");
		addMimeTypes("video/x-msvideo avi");
	}
}
