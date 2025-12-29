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
 * A repeatable resource that obtains its content from a byte array.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
public class ByteArrayResource extends BasicResource {
	/**
	 * Constructor.
	 */
	public ByteArrayResource() {
		super(new ByteArrayEntity());
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.
	 * @param contents The entity contents.
	 */
	public ByteArrayResource(ContentType contentType, byte[] contents) {
		super(new ByteArrayEntity(contentType, contents));
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.
	 */
	protected ByteArrayResource(ByteArrayResource copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource addHeader(String name, String value) {
		super.addHeader(name, value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource addHeaders(Header...values) {
		super.addHeaders(values);
		return this;
	}

	@Override
	public ByteArrayResource copy() {
		return new ByteArrayResource(this);
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setCached() throws IOException {
		super.setCached();
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setChunked() {
		super.setChunked();
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setChunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setContent(Object value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setContent(Supplier<?> value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setContentEncoding(ContentEncoding value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setContentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setContentType(ContentType value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setContentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setHeader(String name, String value) {
		super.setHeader(name, value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setHeaders(Header...values) {
		super.setHeaders(values);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setHeaders(HeaderList value) {
		super.setHeaders(value);
		return this;
	}

	@Override /* Overridden from BasicResource */
	public ByteArrayResource setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}
}