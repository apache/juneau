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
package org.apache.juneau.http;

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;

/**
 * Represents the contents of a byte stream file with convenience methods for adding HTTP response headers.
 *
 * <p>
 * <br>These objects can to be returned as responses by REST methods.
 *
 * <p>
 * <l>StreamResources</l> are meant to be thread-safe and reusable objects.
 * <br>The contents of the request passed into the constructor are immediately converted to read-only byte arrays.
 *
 * <p>
 * Instances of this class can be built using {@link StreamResourceBuilder}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.StreamResource}
 * </ul>
 */
@Response
public class StreamResource {

	private final MediaType mediaType;
	private final Object contents;
	private final Map<String,Object> headers;

	StreamResource(StreamResourceBuilder b) throws IOException {
		this(b.mediaType, b.headers, b.cached, b.contents);
	}

	/**
	 * Constructor.
	 *
	 * @param mediaType The resource media type.
	 * @param headers The HTTP response headers for this streamed resource.
	 * @param cached
	 * 	Identifies if this stream resource is cached in memory.
	 * 	<br>If <jk>true</jk>, the contents will be loaded into a byte array for fast retrieval.
	 * @param contents
	 * 	The resource contents.
	 * 	<br>If multiple contents are specified, the results will be concatenated.
	 * 	<br>Contents can be any of the following:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li><c>InputStream</c>
	 * 		<li><c>Reader</c> - Converted to UTF-8 bytes.
	 * 		<li><c>File</c>
	 * 		<li><c>CharSequence</c> - Converted to UTF-8 bytes.
	 * 	</ul>
	 * @throws IOException Thrown by underlying stream.
	 */
	public StreamResource(MediaType mediaType, Map<String,Object> headers, boolean cached, Object contents) throws IOException {
		this.mediaType = mediaType;
		this.headers = AMap.unmodifiable(headers);
		this.contents = cached ? readBytes(contents) : contents;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of a {@link StreamResourceBuilder} for this class.
	 *
	 * @return A new instance of a {@link StreamResourceBuilder}.
	 */
	public static StreamResourceBuilder create() {
		return new StreamResourceBuilder();
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
	 * TODO
	 *
	 * @param os TODO
	 * @throws IOException TODO
	 */
	@ResponseBody
	public void streamTo(OutputStream os) throws IOException {
		if (contents != null)
			pipe(contents, os);
		os.flush();
	}

	/**
	 * TODO
	 *
	 * @return TODO
	 */
	@ResponseHeader("Content-Type")
	public String getMediaType() {
		return mediaType == null ? null : mediaType.toString();
	}

	/**
	 * Returns the contents of this stream resource.
	 *
	 * @return The contents of this stream resource.
	 * @throws IOException Thrown by underlying stream.
	 */
	public InputStream getContents() throws IOException {
		Object c = contents;
		if (c != null) {
			if (c instanceof byte[])
				return new ByteArrayInputStream((byte[])c);
			else if (c instanceof InputStream)
				return (InputStream)c;
			else if (c instanceof File)
				return new FileInputStream((File)c);
			else if (c instanceof CharSequence)
				return new ByteArrayInputStream((((CharSequence)c).toString().getBytes(UTF8)));
		}
		return null;
	}
}
