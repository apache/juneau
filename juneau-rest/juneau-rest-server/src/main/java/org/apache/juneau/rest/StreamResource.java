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

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.response.*;

/**
 * Represents the contents of a byte stream file with convenience methods for adding HTTP response headers.
 * 
 * <p>
 * This class is handled special by the {@link StreamableHandler} class.
 * <br>This allows these objects to be returned as responses by REST methods.
 * 
 * <p>
 * <l>StreamResources</l> are meant to be thread-safe and reusable objects.
 * <br>The contents of the request passed into the constructor are immediately converted to read-only byte arrays.
 * 
 * <p>
 * Instances of this class can be built using {@link StreamResourceBuilder}.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../overview-summary.html#juneau-rest-server.StreamResource">Overview &gt; StreamResource</a>
 * </ul>
 */
public class StreamResource implements Streamable {

	private final MediaType mediaType;
	private final byte[][] contents;
	private final Map<String,Object> headers;

	/**
	 * Creates a new instance of a {@link StreamResourceBuilder}
	 * 
	 * @return A new instance of a {@link StreamResourceBuilder}
	 */
	public static StreamResourceBuilder create() {
		return new StreamResourceBuilder();
	}
	
	/**
	 * Constructor.
	 * 
	 * @param mediaType The resource media type.
	 * @param headers The HTTP response headers for this streamed resource.
	 * @param contents
	 * 	The resource contents.
	 * 	<br>If multiple contents are specified, the results will be concatenated.
	 * 	<br>Contents can be any of the following:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li><code>InputStream</code>
	 * 		<li><code>Reader</code> - Converted to UTF-8 bytes.
	 * 		<li><code>File</code>
	 * 		<li><code>CharSequence</code> - Converted to UTF-8 bytes.
	 * 	</ul>
	 * @throws IOException
	 */
	public StreamResource(MediaType mediaType, Map<String,Object> headers, Object...contents) throws IOException {
		this.mediaType = mediaType;

		this.headers = headers == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(new LinkedHashMap<>(headers));

		this.contents = new byte[contents.length][];
		for (int i = 0; i < contents.length; i++) {
			Object c = contents[i];
			if (c == null)
				this.contents[i] = new byte[0];
			else if (c instanceof byte[])
				this.contents[i] = (byte[])c;
			else if (c instanceof InputStream)
				this.contents[i] = readBytes((InputStream)c, 1024);
			else if (c instanceof File)
				this.contents[i] = readBytes((File)c);
			else if (c instanceof Reader)
				this.contents[i] = read((Reader)c).getBytes(UTF8);
			else if (c instanceof CharSequence)
				this.contents[i] = ((CharSequence)c).toString().getBytes(UTF8);
			else
				throw new IOException("Invalid class type passed to StreamResource: " + c.getClass().getName());
		}
	}

	/**
	 * Get the HTTP response headers.
	 * 
	 * @return 
	 * 	The HTTP response headers.  
	 * 	<br>An unmodifiable map.  
	 * 	<br>Never <jk>null</jk>.
	 */
	public Map<String,Object> getHeaders() {
		return headers;
	}

	@Override /* Streamable */
	public void streamTo(OutputStream os) throws IOException {
		for (byte[] b : contents)
			os.write(b);
	}

	@Override /* Streamable */
	public MediaType getMediaType() {
		return mediaType;
	}
}
