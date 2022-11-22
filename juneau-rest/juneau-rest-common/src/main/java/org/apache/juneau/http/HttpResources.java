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
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;

/**
 * Standard predefined HTTP resources.
 *
 * <p>
 * Resources are simply {@link HttpEntity} objects with arbitrary additional headers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
public class HttpResources {

	/**
	 * Creates a new {@link ByteArrayResource} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link ByteArrayResource} builder.
	 */
	public static final ByteArrayResource byteArrayResource(byte[] content) {
		return new ByteArrayResource().setContent(content);
	}

	/**
	 * Creates a new {@link ByteArrayResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ByteArrayResource} builder.
	 */
	public static final ByteArrayResource byteArrayResource(byte[] content, ContentType contentType) {
		return new ByteArrayResource().setContent(content).setContentType(contentType);
	}

	/**
	 * Creates a new {@link ByteArrayResource} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content supplier.  Can be <jk>null</jk>.
	 * @return A new {@link ByteArrayResource} builder.
	 */
	public static final ByteArrayResource byteArrayResource(Supplier<byte[]> content) {
		return new ByteArrayResource().setContent(content);
	}

	/**
	 * Creates a new {@link ByteArrayResource} builder.
	 *
	 * @param content The entity content supplier.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ByteArrayResource} builder.
	 */
	public static final ByteArrayResource byteArrayResource(Supplier<byte[]> content, ContentType contentType) {
		return new ByteArrayResource().setContent(content).setContentType(contentType);
	}

	/**
	 * Creates a new {@link FileResource} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link FileResource} builder.
	 */
	public static final FileResource fileResource(File content) {
		return new FileResource().setContent(content);
	}

	/**
	 * Creates a new {@link FileResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link FileResource} builder.
	 */
	public static final FileResource fileResource(File content, ContentType contentType) {
		return new FileResource().setContent(content).setContentType(contentType);
	}

	/**
	 * Creates a new {@link ReaderResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link ReaderResource} builder.
	 */
	public static final ReaderResource readerResource(Reader content) {
		return new ReaderResource().setContent(content);
	}

	/**
	 * Creates a new {@link ReaderResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ReaderResource} builder.
	 */
	public static final ReaderResource readerResource(Reader content, ContentType contentType) {
		return new ReaderResource().setContent(content).setContentType(contentType);
	}

	/**
	 * Creates a new {@link StreamResource} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StreamResource} builder.
	 */
	public static final StreamResource streamResource(InputStream content) {
		return new StreamResource().setContent(content);
	}

	/**
	 * Creates a new {@link StreamResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 * @return A new {@link StreamResource} builder.
	 */
	public static final StreamResource streamResource(InputStream content, long length, ContentType contentType) {
		return new StreamResource().setContent(content).setContentLength(length).setContentType(contentType);
	}

	/**
	 * Creates a new builder for a {@link StringResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringResource} builder.
	 */
	public static final StringResource stringResource(String content) {
		return new StringResource().setContent(content);
	}

	/**
	 * Creates a new builder for a {@link StringResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link StringResource} builder.
	 */
	public static final StringResource stringResource(String content, ContentType contentType) {
		return new StringResource().setContent(content).setContentType(contentType);
	}

	/**
	 * Creates a new builder for a {@link StringResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringResource} builder.
	 */
	public static final StringResource stringResource(Supplier<String> content) {
		return new StringResource().setContent(content);
	}

	/**
	 * Creates a new builder for a {@link StringResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link StringResource} builder.
	 */
	public static final StringResource stringResource(Supplier<String> content, ContentType contentType) {
		return new StringResource().setContent(content).setContentType(contentType);
	}
}
