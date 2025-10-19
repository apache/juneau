/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.resource;

import java.io.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;

/**
 * A self contained, repeatable resource that obtains its content from a {@link String}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
public class StringResource extends BasicResource {
	/**
	 * Constructor.
	 */
	public StringResource() {
		super(new StringEntity());
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.
	 * @param contents The entity contents.
	 */
	public StringResource(ContentType contentType, String contents) {
		super(new StringEntity(contentType, contents));
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.
	 */
	protected StringResource(StringResource copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from BasicResource */
	public StringResource addHeader(String name, String value) {
		super.addHeader(name, value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource addHeaders(Header...values) {
		super.addHeaders(values);
		return this;
	}

	@Override
	public StringResource copy() {
		return new StringResource(this);
	}

	@Override /* Overridden from BasicResource */
	public StringResource setCached() throws IOException {
		super.setCached();
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setChunked() {
		super.setChunked();
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setChunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setContent(Object value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setContent(Supplier<?> value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setContentEncoding(ContentEncoding value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setContentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setContentType(ContentType value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setContentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setHeader(String name, String value) {
		super.setHeader(name, value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setHeaders(Header...values) {
		super.setHeaders(values);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setHeaders(HeaderList value) {
		super.setHeaders(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public StringResource setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}
}