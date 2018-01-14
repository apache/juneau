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
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.response.*;

/**
 * Represents the contents of a byte stream file with convenience methods for adding HTTP response headers.
 * 
 * <p>
 * The purpose of this class is to maintain an in-memory reusable byte array of a streamed resource for the fastest
 * possible streaming.
 * Therefore, this object is designed to be reused and thread-safe.
 * 
 * <p>
 * This class is handled special by the {@link StreamableHandler} class.
 * This allows these objects to be returned as responses by REST methods.
 */
public class StreamResource implements Streamable {

	private final MediaType mediaType;
	private final byte[][] contents;
	private final Map<String,String> headers;

	/**
	 * Constructor.
	 * 
	 * @param mediaType The resource media type.
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
	public StreamResource(MediaType mediaType, Object...contents) throws IOException {
		this(mediaType, null, contents);
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

		Map<String,String> m = new LinkedHashMap<>();
		if (headers != null)
			for (Map.Entry<String,Object> e : headers.entrySet())
				m.put(e.getKey(), StringUtils.toString(e.getValue()));
		this.headers = Collections.unmodifiableMap(m);

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
	 * Builder class for constructing {@link StreamResource} objects.
	 */
	public static final class Builder {
		ArrayList<Object> contents = new ArrayList<>();
		MediaType mediaType;
		Map<String,String> headers = new LinkedHashMap<>();

		/**
		 * Specifies the resource media type string.
		 * 
		 * @param mediaType The resource media type string.
		 * @return This object (for method chaining).
		 */
		public Builder mediaType(String mediaType) {
			this.mediaType = MediaType.forString(mediaType);
			return this;
		}

		/**
		 * Specifies the resource media type string.
		 * 
		 * @param mediaType The resource media type string.
		 * @return This object (for method chaining).
		 */
		public Builder mediaType(MediaType mediaType) {
			this.mediaType = mediaType;
			return this;
		}

		/**
		 * Specifies the contents for this resource.
		 * 
		 * <p>
		 * This method can be called multiple times to add more content.
		 * 
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
		 * @return This object (for method chaining).
		 */
		public Builder contents(Object...contents) {
			this.contents.addAll(Arrays.asList(contents));
			return this;
		}

		/**
		 * Specifies an HTTP response header value.
		 * 
		 * @param name The HTTP header name.
		 * @param value The HTTP header value.  Will be converted to a <code>String</code> using {@link Object#toString()}.
		 * @return This object (for method chaining).
		 */
		public Builder header(String name, Object value) {
			this.headers.put(name, StringUtils.toString(value));
			return this;
		}

		/**
		 * Specifies HTTP response header values.
		 * 
		 * @param headers The HTTP headers.  Values will be converted to <code>Strings</code> using {@link Object#toString()}.
		 * @return This object (for method chaining).
		 */
		public Builder headers(Map<String,Object> headers) {
			for (Map.Entry<String,Object> e : headers.entrySet())
				header(e.getKey(), e.getValue());
			return this;
		}

		/**
		 * Create a new {@link StreamResource} using values in this builder.
		 * 
		 * @return A new immutable {@link StreamResource} object.
		 * @throws IOException
		 */
		public StreamResource build() throws IOException {
			return new StreamResource(mediaType, headers, contents.toArray());
		}
	}

	/**
	 * Get the HTTP response headers.
	 * 
	 * @return The HTTP response headers.  An unmodifiable map.  Never <jk>null</jk>.
	 */
	public Map<String,String> getHeaders() {
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
