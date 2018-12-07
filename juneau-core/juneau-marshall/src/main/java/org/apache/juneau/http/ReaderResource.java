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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;

/**
 * Represents the contents of a text file with convenience methods for resolving SVL variables and adding
 * HTTP response headers.
 *
 * <p>
 * <br>These objects can be returned as responses by REST methods.
 *
 * <p>
 * <l>ReaderResources</l> are meant to be thread-safe and reusable objects.
 * <br>The contents of the request passed into the constructor are immediately converted to read-only strings.
 *
 * <p>
 * Instances of this class can be built using {@link Builder}.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.ReaderResource}
 * </ul>
 */
@Response
public class ReaderResource implements Writable {

	private final MediaType mediaType;
	private final Map<String,Object> headers;

	@SuppressWarnings("javadoc")
	protected final Object[] contents;

	/**
	 * Constructor.
	 *
	 * @param b Builder containing values to initialize this object with.
	 * @throws IOException
	 */
	protected ReaderResource(Builder b) throws IOException {
		this(b.mediaType, b.headers, b.cached, b.contents.toArray());
	}

	/**
	 * Constructor.
	 *
	 * @param mediaType The resource media type.
	 * @param headers The HTTP response headers for this streamed resource.
	 * @param cached
	 * 	Identifies if this resource is cached in memory.
	 * 	<br>If <jk>true</jk>, the contents will be loaded into a String for fast retrieval.
	 * @param contents
	 * 	The resource contents.
	 * 	<br>If multiple contents are specified, the results will be concatenated.
	 * 	<br>Contents can be any of the following:
	 * 	<ul>
	 * 		<li><code>InputStream</code>
	 * 		<li><code>Reader</code> - Converted to UTF-8 bytes.
	 * 		<li><code>File</code>
	 * 		<li><code>CharSequence</code> - Converted to UTF-8 bytes.
	 * 	</ul>
	 * @throws IOException
	 */
	public ReaderResource(MediaType mediaType, Map<String,Object> headers, boolean cached, Object...contents) throws IOException {
		this.mediaType = mediaType;
		this.headers = immutableMap(headers);
		this.contents = cached ? new Object[]{readAll(contents)} : contents;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of a {@link Builder} for this class.
	 *
	 * @return A new instance of a {@link Builder}.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder class for constructing {@link ReaderResource} objects.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.ReaderResource}
	 * </ul>
	 */
	@SuppressWarnings("javadoc")
	public static class Builder {

		public ArrayList<Object> contents = new ArrayList<>();
		public MediaType mediaType;
		public Map<String,Object> headers = new LinkedHashMap<>();
		public boolean cached;

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
		 * @param value
		 * 	The HTTP header value.
		 * 	<br>Will be converted to a <code>String</code> using {@link Object#toString()}.
		 * @return This object (for method chaining).
		 */
		public Builder header(String name, Object value) {
			this.headers.put(name, value);
			return this;
		}

		/**
		 * Specifies HTTP response header values.
		 *
		 * @param headers
		 * 	The HTTP headers.
		 * 	<br>Values will be converted to <code>Strings</code> using {@link Object#toString()}.
		 * @return This object (for method chaining).
		 */
		public Builder headers(Map<String,Object> headers) {
			this.headers.putAll(headers);
			return this;
		}

		/**
		 * Specifies that this resource is intended to be cached.
		 *
		 * <p>
		 * This will trigger the contents to be loaded into a String for fast serializing.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder cached() {
			this.cached = true;
			return this;
		}

		/**
		 * Create a new {@link ReaderResource} using values in this builder.
		 *
		 * @return A new immutable {@link ReaderResource} object.
		 * @throws IOException
		 */
		public ReaderResource build() throws IOException {
			return new ReaderResource(this);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

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
	@Override /* Writeable */
	public Writer writeTo(Writer w) throws IOException {
		for (Object o : contents)
			pipe(o, w);
		return w;
	}

	@ResponseHeader("Content-Type")
	@Override /* Writeable */
	public MediaType getMediaType() {
		return mediaType;
	}

	@Override /* Object */
	public String toString() {
		try {
			if (contents.length == 1)
				return read(contents[0]);
			return writeTo(new StringWriter()).toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Same as {@link #toString()} but strips comments from the text before returning it.
	 *
	 * <p>
	 * Supports stripping comments from the following media types: HTML, XHTML, XML, JSON, Javascript, CSS.
	 *
	 * @return The resource contents stripped of any comments.
	 */
	public String toCommentStrippedString() {
		String s = toString();
		String subType = mediaType.getSubType();
		if ("html".equals(subType) || "xhtml".equals(subType) || "xml".equals(subType))
			s = s.replaceAll("(?s)<!--(.*?)-->\\s*", "");
		else if ("json".equals(subType) || "javascript".equals(subType) || "css".equals(subType))
			s = s.replaceAll("(?s)\\/\\*(.*?)\\*\\/\\s*", "");
		return s;
	}

	/**
	 * Returns the contents of this resource.
	 *
	 * @return The contents of this resource.
	 */
	public Reader getContents() {
		if (contents.length == 1 && contents[0] instanceof Reader) {
			return (Reader)contents[0];
		}
		return new StringReader(toString());
	}
}
