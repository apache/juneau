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
import java.nio.*;
import java.util.*;

import org.apache.juneau.*;
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
public class StreamResource implements Streamable {

	private final MediaType mediaType;
	private final Object[] contents;
	private final Map<String,Object> headers;

	StreamResource(StreamResourceBuilder b) throws IOException {
		this(b.mediaType, b.headers, b.cached, b.contents.toArray());
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
	public StreamResource(MediaType mediaType, Map<String,Object> headers, boolean cached, Object...contents) throws IOException {
		this.mediaType = mediaType;
		this.headers = AMap.unmodifiable(headers);
		this.contents = cached ? new Object[]{readBytes(contents)} : contents;
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

	@ResponseBody
	@Override /* Streamable */
	public void streamTo(OutputStream os) throws IOException {
		for (Object c : contents)
			pipe(c, os);
		os.flush();
	}

	@ResponseHeader("Content-Type")
	@Override /* Streamable */
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
		if (contents.length == 1) {
			Object c = contents[0];
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
		}
		byte[][] bc = new byte[contents.length][];
		int c = 0;
		for (int i = 0; i < contents.length; i++) {
			Object o = contents[i];
			if (o == null)
				bc[i] = new byte[0];
			else if (o instanceof byte[])
				bc[i] = (byte[])o;
			else if (o instanceof InputStream)
				bc[i] = readBytes((InputStream)o, 1024);
			else if (o instanceof Reader)
				bc[i] = read((Reader)o).getBytes(UTF8);
			else if (o instanceof File)
				bc[i] = readBytes((File)o);
			else if (o instanceof CharSequence)
				bc[i] = ((CharSequence)o).toString().getBytes(UTF8);
			c += bc[i].length;
		}
		ByteBuffer bb = ByteBuffer.allocate(c);
		for (byte[] b : bc)
			bb.put(b);
		return new ByteArrayInputStream(bb.array());
	}
}
