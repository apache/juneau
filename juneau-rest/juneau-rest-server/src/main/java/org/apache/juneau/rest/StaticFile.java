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
package org.apache.juneau.rest;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.annotation.*;

/**
 * Instance of a static file sent as an HTTP response.
 */
@Response
public class StaticFile {

	private final byte[] contents;
	private final String mediaType;
	private final Map<String,Object> headers;

	/**
	 * Constructor.
	 *
	 * @param contents Contents of the file, or <jk>null</jk> if file does not exist.
	 * @param mediaType The media type of the file.
	 * @param headers Arbitrary response headers to set when sending this file as an HTTP response.
	 */
	public StaticFile(byte[] contents, String mediaType, Map<String,Object> headers) {
		this.contents = contents;
		this.mediaType = mediaType;
		this.headers = headers;
	}

	/**
	 * Does this file exist?
	 *
	 * @return <jk>true</jk> if this file exists.
	 */
	public boolean exists() {
		return contents != null;
	}

	/**
	 * Get the HTTP response headers.
	 *
	 * @return
	 * 	The HTTP response headers.
	 * 	<br>An unmodifiable map.
	 * 	<br>Never <jk>null</jk>.
	 */
	@ResponseHeader("*")
	public Map<String,Object> getHeaders() {
		return headers;
	}

	/**
	 * Returns the contents of this static file as an input stream.
	 *
	 * @return This file as an input stream.
	 * @throws IOException Should never happen.
	 */
	@ResponseBody
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(contents);
	}

	/**
	 * Returns the content type for this static file.
	 *
	 * @return The content type for this static file.
	 */
	@ResponseHeader("Content-Type")
	public String getContentType() {
		return mediaType == null ? null : mediaType.toString();
	}
}
