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

import java.io.*;
import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Builder class for constructing {@link StreamResource} objects.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.StreamResource}
 * </ul>
 */
public class StreamResourceBuilder {
	ArrayList<Object> contents = new ArrayList<>();
	MediaType mediaType;
	Map<String,Object> headers = new LinkedHashMap<>();
	boolean cached;

	/**
	 * Specifies the resource media type string.
	 *
	 * @param mediaType The resource media type string.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public StreamResourceBuilder mediaType(String mediaType) {
		this.mediaType = MediaType.forString(mediaType);
		return this;
	}

	/**
	 * Specifies the resource media type string.
	 *
	 * @param mediaType The resource media type string.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public StreamResourceBuilder mediaType(MediaType mediaType) {
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
	 * 		<li><c>InputStream</c>
	 * 		<li><c>Reader</c> - Converted to UTF-8 bytes.
	 * 		<li><c>File</c>
	 * 		<li><c>CharSequence</c> - Converted to UTF-8 bytes.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public StreamResourceBuilder contents(Object...contents) {
		Collections.addAll(this.contents, contents);
		return this;
	}

	/**
	 * Specifies an HTTP response header value.
	 *
	 * @param name The HTTP header name.
	 * @param value
	 * 	The HTTP header value.
	 * 	<br>Will be converted to a <c>String</c> using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public StreamResourceBuilder header(String name, Object value) {
		this.headers.put(name, value);
		return this;
	}

	/**
	 * Specifies HTTP response header values.
	 *
	 * @param headers
	 * 	The HTTP headers.
	 * 	<br>Values will be converted to <c>Strings</c> using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public StreamResourceBuilder headers(Map<String,Object> headers) {
		this.headers.putAll(headers);
		return this;
	}

	/**
	 * Specifies HTTP response header values.
	 *
	 * @param headers
	 * 	The HTTP headers.
	 * 	<br>Values will be converted to <c>Strings</c> using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public StreamResourceBuilder headers(org.apache.http.Header...headers) {
		for (org.apache.http.Header h : headers)
			this.headers.put(h.getName(), h.getValue());
		return this;
	}

	/**
	 * Specifies that this resource is intended to be cached.
	 *
	 * <p>
	 * This will trigger the contents to be loaded into a byte array for fast serializing.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public StreamResourceBuilder cached() {
		this.cached = true;
		return this;
	}

	/**
	 * Create a new {@link StreamResource} using values in this builder.
	 *
	 * @return A new immutable {@link StreamResource} object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public StreamResource build() throws IOException {
		return new StreamResource(this);
	}

	// <FluentSetters>

	// </FluentSetters>
}