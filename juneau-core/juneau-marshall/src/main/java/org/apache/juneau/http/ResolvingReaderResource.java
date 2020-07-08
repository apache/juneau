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

import org.apache.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;

/**
 * An extension of {@link ReaderResource} that allows automatic resolution of SVL variables.
 */
public class ResolvingReaderResource extends ReaderResource {

	private VarResolverSession varSession;

	/**
	 * Creator.
	 *
	 * @return A new empty {@link ReaderResource} object.
	 */
	public static ResolvingReaderResource create() {
		return new ResolvingReaderResource();
	}

	/**
	 * Constructor.
	 */
	public ResolvingReaderResource() {
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
	 * @param varSession Var resolver session for resolving SVL variables.
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
	public ResolvingReaderResource(ContentType contentType, ContentEncoding contentEncoding, VarResolverSession varSession, Object content) {
		super(contentType, contentEncoding, content);
		this.varSession = varSession;
	}

	/**
	 * Converts the contents of this entity as a byte array.
	 *
	 * @return The contents of this entity as a byte array.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	@Override
	public String asString() throws IOException {
		if (varSession == null)
			return super.asString();
		StringWriter sw = new StringWriter();
		String s = IOUtils.read(getRawContent());
		varSession.resolveTo(s, sw);
		return sw.toString();
	}

	@Override
	public void writeTo(OutputStream os) throws IOException {
		if (varSession == null)
			super.writeTo(os);
		else {
			try (OutputStreamWriter osw = new OutputStreamWriter(os, IOUtils.UTF8)) {
				String s = IOUtils.read(getRawContent());
				varSession.resolveTo(s, osw);
				osw.flush();
			}
		}
		os.flush();
	}

	/**
	 * Sets the var resolver for resolving SVL variables.
	 *
	 * @param varSession - The var resolver session.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ResolvingReaderResource varResolver(VarResolverSession varSession) {
		this.varSession = varSession;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource cache() {
		super.cache();
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource chunked() {
		super.chunked();
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource content(Object value) {
		super.content(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource contentEncoding(String value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource contentEncoding(Header value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource contentType(String value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource contentType(Header value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource header(Header value) {
		super.header(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource headers(Header...headers) {
		super.headers(headers);
		return this;
	}

	@Override /* GENERATED - BasicHttpResource */
	public ResolvingReaderResource headers(List<Header> headers) {
		super.headers(headers);
		return this;
	}

	// </FluentSetters>
}
