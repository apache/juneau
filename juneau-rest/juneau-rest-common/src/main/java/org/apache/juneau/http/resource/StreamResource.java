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
package org.apache.juneau.http.resource;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A streamed, non-repeatable resource that obtains its content from an {@link InputStream}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
@FluentSetters
public class StreamResource extends BasicResource {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public StreamResource() {
		super(new StreamEntity());
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.
	 * @param contents The entity contents.
	 */
	public StreamResource(ContentType contentType, InputStream contents) {
		super(new StreamEntity(contentType, contents));
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.
	 */
	protected StreamResource(StreamResource copyFrom) {
		super(copyFrom);
	}

	@Override
	public StreamResource copy() {
		return new StreamResource(this);
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setCached() throws IOException{
		super.setCached();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setChunked() {
		super.setChunked();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setChunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setContent(Object value) {
		super.setContent(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setContent(Supplier<?> value) {
		super.setContent(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setContentEncoding(ContentEncoding value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setContentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setContentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setContentType(ContentType value) {
		super.setContentType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setHeaders(HeaderList value) {
		super.setHeaders(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.resource.BasicResource */
	public StreamResource setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}

	// </FluentSetters>
}
