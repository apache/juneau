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

import org.apache.http.Header;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.StreamResource}
 * </ul>
 */
@Response
public class StreamResource extends BasicHttpResource {

	/**
	 * Creator.
	 *
	 * @return A new empty {@link ReaderResource} object.
	 */
	public static StreamResource create() {
		return new StreamResource();
	}

	/**
	 * Constructor.
	 */
	public StreamResource() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param contentType
	 * 	The content type of the contents.
	 * 	<br>Can be <jk>null</jk>.
	 * @param contentEncoding
	 * 	The content encoding of the contents.
	 * 	<br>Can be <jk>null</jk>.
	 * @param content
	 * 	The content.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>InputStream</c>
	 * 		<li><c>Reader</c> - Converted to UTF-8 bytes.
	 * 		<li><c>File</c>
	 * 		<li><c>CharSequence</c> - Converted to UTF-8 bytes.
	 * 		<li><c><jk>byte</jk>[]</c>.
	 * 	</ul>
	 * </ul>
	 */
	public StreamResource(ContentType contentType, ContentEncoding contentEncoding, Object content) {
		super(contentType, contentEncoding, content);
	}

	/**
	 * Converts the contents of this entity as a byte array.
	 *
	 * @return The contents of this entity as a byte array.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public byte[] asBytes() throws IOException {
		return IOUtils.readBytes(getRawContent());
	}

	/**
	 * Returns an assertion on the contents of this resource.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentByteArrayAssertion<StreamResource> assertBytes() throws IOException {
		return new FluentByteArrayAssertion<>(asBytes(), this);
	}

	// <FluentSetters>

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource cache() {
		super.cache();
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource chunked() {
		super.chunked();
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource content(Object value) {
		super.content(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource contentEncoding(String value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource contentEncoding(Header value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource contentType(String value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource contentType(Header value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource header(Header value) {
		super.header(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource headers(Header...headers) {
		super.headers(headers);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public StreamResource headers(List<Header> headers) {
		super.headers(headers);
		return this;
	}

	// </FluentSetters>
}
