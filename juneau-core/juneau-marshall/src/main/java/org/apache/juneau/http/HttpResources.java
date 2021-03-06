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
 */
public class HttpResources {

	/**
	 * Creates a new {@link ByteArrayResource} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link ByteArrayResource} builder.
	 */
	public static final HttpResourceBuilder<ByteArrayResource> byteArrayResource(byte[] content) {
		return ByteArrayResource.create().content(content);
	}

	/**
	 * Creates a new {@link ByteArrayResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ByteArrayResource} builder.
	 */
	public static final HttpResourceBuilder<ByteArrayResource> byteArrayResource(byte[] content, ContentType contentType) {
		return ByteArrayResource.create().content(content).contentType(contentType);
	}

	/**
	 * Creates a new {@link ByteArrayResource} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content supplier.  Can be <jk>null<jk>.
	 * @return A new {@link ByteArrayResource} builder.
	 */
	public static final HttpResourceBuilder<ByteArrayResource> byteArrayResource(Supplier<byte[]> content) {
		return ByteArrayResource.create().contentSupplier(content);
	}

	/**
	 * Creates a new {@link ByteArrayResource} builder.
	 *
	 * @param content The entity content supplier.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ByteArrayResource} builder.
	 */
	public static final HttpResourceBuilder<ByteArrayResource> byteArrayResource(Supplier<byte[]> content, ContentType contentType) {
		return ByteArrayResource.create().contentSupplier(content).contentType(contentType);
	}

	/**
	 * Creates a new {@link FileResource} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link FileResource} builder.
	 */
	public static final HttpResourceBuilder<FileResource> fileResource(File content) {
		return FileResource.create().content(content);
	}

	/**
	 * Creates a new {@link FileResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link FileResource} builder.
	 */
	public static final HttpResourceBuilder<FileResource> fileResource(File content, ContentType contentType) {
		return FileResource.create().content(content).contentType(contentType);
	}

	/**
	 * Creates a new {@link ReaderResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link ReaderResource} builder.
	 */
	public static final HttpResourceBuilder<ReaderResource> readerResource(Reader content) {
		return ReaderResource.create().content(content);
	}

	/**
	 * Creates a new {@link ReaderResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ReaderResource} builder.
	 */
	public static final HttpResourceBuilder<ReaderResource> readerResource(Reader content, ContentType contentType) {
		return ReaderResource.create().content(content).contentType(contentType);
	}

//	/**
//	 * Creates a new {@link SerializedResource} object.
//	 *
//	 * @param content
//	 * 	The Java POJO representing the content.
//	 * 	<br>Can be <jk>null<jk>.
//	 * @param serializer
//	 * 	The serializer to use to serialize the POJO.
//	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
//	 * @return A new {@link SerializedResource} object.
//	 */
//	public static final SerializedResourceBuilder<SerializedResource> serializedResource(Object content, Serializer serializer) {
//		return SerializedResource.create().content(content).serializer(serializer);
//	}
//
//	/**
//	 * Creates a new {@link SerializedResource} object.
//	 *
//	 * @param content
//	 * 	The supplier of a Java POJO representing the content.
//	 * 	<br>Can be <jk>null<jk>.
//	 * @param serializer
//	 * 	The serializer to use to serialize the POJO.
//	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
//	 * @return A new {@link SerializedResource} object.
//	 */
//	public static final SerializedResourceBuilder<SerializedResource> serializedResource(Supplier<?> content, Serializer serializer) {
//		return SerializedResource.create().contentSupplier(content).serializer(serializer);
//	}
//
//	/**
//	 * Creates a new {@link SerializedResource} object.
//	 *
//	 * @param content
//	 * 	The Java POJO representing the content.
//	 * 	<br>Can be <jk>null<jk>.
//	 * @param serializer
//	 * 	The serializer to use to serialize the POJO.
//	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
//	 * @param schema
//	 * 	Optional HTTP-part schema for providing instructionst to the serializer on the format of the entity.
//	 * @return A new {@link SerializedResource} object.
//	 */
//	public static final SerializedResourceBuilder<SerializedResource> serializedResource(Object content, Serializer serializer, HttpPartSchema 	schema) {
//		return SerializedResource.create().content(content).serializer(serializer).schema(schema);
//	}
//
//	/**
//	 * Creates a new {@link SerializedResource} object.
//	 *
//	 * @param content
//	 * 	The supplier of a Java POJO representing the content.
//	 * 	<br>Can be <jk>null<jk>.
//	 * @param serializer
//	 * 	The serializer to use to serialize the POJO.
//	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
//	 * @param schema
//	 * 	Optional HTTP-part schema for providing instructionst to the serializer on the format of the entity.
//	 * @return A new {@link SerializedResource} object.
//	 */
//	public static final SerializedResourceBuilder<SerializedResource> serializedResource(Supplier<?> content, Serializer serializer, HttpPartSchema schema) {
//		return SerializedResource.create().contentSupplier(content).serializer(serializer).schema(schema);
//	}

	/**
	 * Creates a new {@link InputStreamResource} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link InputStreamResource} builder.
	 */
	public static final HttpResourceBuilder<InputStreamResource> streamResource(InputStream content) {
		return InputStreamResource.create().content(content);
	}

	/**
	 * Creates a new {@link InputStreamResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 * @return A new {@link InputStreamResource} builder.
	 */
	public static final HttpResourceBuilder<InputStreamResource> streamResource(InputStream content, long length, ContentType contentType) {
		return InputStreamResource.create().content(content).contentLength(length).contentType(contentType);
	}

	/**
	 * Creates a new builder for a {@link StringResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringResource} builder.
	 */
	public static final HttpResourceBuilder<StringResource> stringResource(String content) {
		return StringResource.create().content(content);
	}

	/**
	 * Creates a new builder for a {@link StringResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link StringResource} builder.
	 */
	public static final HttpResourceBuilder<StringResource> stringResource(String content, ContentType contentType) {
		return StringResource.create().content(content).contentType(contentType);
	}

	/**
	 * Creates a new builder for a {@link StringResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringResource} builder.
	 */
	public static final HttpResourceBuilder<StringResource> stringResource(Supplier<String> content) {
		return StringResource.create().contentSupplier(content);
	}

	/**
	 * Creates a new builder for a {@link StringResource} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link StringResource} builder.
	 */
	public static final HttpResourceBuilder<StringResource> stringResource(Supplier<String> content, ContentType contentType) {
		return StringResource.create().contentSupplier(content).contentType(contentType);
	}
}
