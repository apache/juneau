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

import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;

/**
 * Standard predefined HTTP entities.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.HttpParts}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class HttpEntities {

	/**
	 * Creates a new {@link ByteArrayEntity} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link ByteArrayEntity} builder.
	 */
	public static final HttpEntityBuilder<ByteArrayEntity> byteArrayEntity(byte[] content) {
		return ByteArrayEntity.create().content(content);
	}

	/**
	 * Creates a new {@link ByteArrayEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ByteArrayEntity} builder.
	 */
	public static final HttpEntityBuilder<ByteArrayEntity> byteArrayEntity(byte[] content, ContentType contentType) {
		return ByteArrayEntity.create().content(content).contentType(contentType);
	}

	/**
	 * Creates a new {@link ByteArrayEntity} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content supplier.  Can be <jk>null</jk>.
	 * @return A new {@link ByteArrayEntity} builder.
	 */
	public static final HttpEntityBuilder<ByteArrayEntity> byteArrayEntity(Supplier<byte[]> content) {
		return ByteArrayEntity.create().contentSupplier(content);
	}

	/**
	 * Creates a new {@link ByteArrayEntity} builder.
	 *
	 * @param content The entity content supplier.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ByteArrayEntity} builder.
	 */
	public static final HttpEntityBuilder<ByteArrayEntity> byteArrayEntity(Supplier<byte[]> content, ContentType contentType) {
		return ByteArrayEntity.create().contentSupplier(content).contentType(contentType);
	}

	/**
	 * Creates a new {@link FileEntity} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link FileEntity} builder.
	 */
	public static final HttpEntityBuilder<FileEntity> fileEntity(File content) {
		return FileEntity.create().content(content);
	}

	/**
	 * Creates a new {@link FileEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link FileEntity} builder.
	 */
	public static final HttpEntityBuilder<FileEntity> fileEntity(File content, ContentType contentType) {
		return FileEntity.create().content(content).contentType(contentType);
	}

	/**
	 * Creates a new {@link ReaderEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link ReaderEntity} builder.
	 */
	public static final HttpEntityBuilder<ReaderEntity> readerEntity(Reader content) {
		return ReaderEntity.create().content(content);
	}

	/**
	 * Creates a new {@link ReaderEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ReaderEntity} builder.
	 */
	public static final HttpEntityBuilder<ReaderEntity> readerEntity(Reader content, ContentType contentType) {
		return ReaderEntity.create().content(content).contentType(contentType);
	}

	/**
	 * Creates a new {@link SerializedEntity} object.
	 *
	 * @param content
	 * 	The Java POJO representing the content.
	 * 	<br>Can be <jk>null</jk>.
	 * @param serializer
	 * 	The serializer to use to serialize the POJO.
	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
	 * @return A new {@link SerializedEntity} object.
	 */
	public static final SerializedEntityBuilder<SerializedEntity> serializedEntity(Object content, Serializer serializer) {
		return SerializedEntity.create().content(content).serializer(serializer);
	}

	/**
	 * Creates a new {@link SerializedEntity} object.
	 *
	 * @param content
	 * 	The supplier of a Java POJO representing the content.
	 * 	<br>Can be <jk>null</jk>.
	 * @param serializer
	 * 	The serializer to use to serialize the POJO.
	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
	 * @return A new {@link SerializedEntity} object.
	 */
	public static final SerializedEntityBuilder<SerializedEntity> serializedEntity(Supplier<?> content, Serializer serializer) {
		return SerializedEntity.create().contentSupplier(content).serializer(serializer);
	}

	/**
	 * Creates a new {@link SerializedEntity} object.
	 *
	 * @param content
	 * 	The Java POJO representing the content.
	 * 	<br>Can be <jk>null</jk>.
	 * @param serializer
	 * 	The serializer to use to serialize the POJO.
	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
	 * @param schema
	 * 	Optional HTTP-part schema for providing instructionst to the serializer on the format of the entity.
	 * @return A new {@link SerializedEntity} object.
	 */
	public static final SerializedEntityBuilder<SerializedEntity> serializedEntity(Object content, Serializer serializer, HttpPartSchema 	schema) {
		return SerializedEntity.create().content(content).serializer(serializer).schema(schema);
	}

	/**
	 * Creates a new {@link SerializedEntity} object.
	 *
	 * @param content
	 * 	The supplier of a Java POJO representing the content.
	 * 	<br>Can be <jk>null</jk>.
	 * @param serializer
	 * 	The serializer to use to serialize the POJO.
	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
	 * @param schema
	 * 	Optional HTTP-part schema for providing instructionst to the serializer on the format of the entity.
	 * @return A new {@link SerializedEntity} object.
	 */
	public static final SerializedEntityBuilder<SerializedEntity> serializedEntity(Supplier<?> content, Serializer serializer, HttpPartSchema schema) {
		return SerializedEntity.create().contentSupplier(content).serializer(serializer).schema(schema);
	}

	/**
	 * Creates a new {@link InputStreamEntity} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link InputStreamEntity} builder.
	 */
	public static final HttpEntityBuilder<InputStreamEntity> streamEntity(InputStream content) {
		return InputStreamEntity.create().content(content);
	}

	/**
	 * Creates a new {@link InputStreamEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 * @return A new {@link InputStreamEntity} builder.
	 */
	public static final HttpEntityBuilder<InputStreamEntity> streamEntity(InputStream content, long length, ContentType contentType) {
		return InputStreamEntity.create().content(content).contentLength(length).contentType(contentType);
	}

	/**
	 * Creates a new builder for a {@link StringEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringEntity} builder.
	 */
	public static final HttpEntityBuilder<StringEntity> stringEntity(String content) {
		return StringEntity.create().content(content);
	}

	/**
	 * Creates a new builder for a {@link StringEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link StringEntity} builder.
	 */
	public static final HttpEntityBuilder<StringEntity> stringEntity(String content, ContentType contentType) {
		return StringEntity.create().content(content).contentType(contentType);
	}

	/**
	 * Creates a new builder for a {@link StringEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringEntity} builder.
	 */
	public static final HttpEntityBuilder<StringEntity> stringEntity(Supplier<String> content) {
		return StringEntity.create().contentSupplier(content);
	}

	/**
	 * Creates a new builder for a {@link StringEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link StringEntity} builder.
	 */
	public static final HttpEntityBuilder<StringEntity> stringEntity(Supplier<String> content, ContentType contentType) {
		return StringEntity.create().contentSupplier(content).contentType(contentType);
	}
}
