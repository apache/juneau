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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#juneau-rest-common">juneau-rest-common</a>
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
	public static final ByteArrayEntity byteArrayEntity(byte[] content) {
		return new ByteArrayEntity().setContent(content);
	}

	/**
	 * Creates a new {@link ByteArrayEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ByteArrayEntity} builder.
	 */
	public static final ByteArrayEntity byteArrayEntity(byte[] content, ContentType contentType) {
		return new ByteArrayEntity().setContent(content).setContentType(contentType);
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
	public static final ByteArrayEntity byteArrayEntity(Supplier<byte[]> content) {
		return new ByteArrayEntity().setContent(content);
	}

	/**
	 * Creates a new {@link ByteArrayEntity} builder.
	 *
	 * @param content The entity content supplier.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ByteArrayEntity} builder.
	 */
	public static final ByteArrayEntity byteArrayEntity(Supplier<byte[]> content, ContentType contentType) {
		return new ByteArrayEntity().setContent(content).setContentType(contentType);
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
	public static final FileEntity fileEntity(File content) {
		return new FileEntity().setContent(content);
	}

	/**
	 * Creates a new {@link FileEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link FileEntity} builder.
	 */
	public static final FileEntity fileEntity(File content, ContentType contentType) {
		return new FileEntity().setContent(content).setContentType(contentType);
	}

	/**
	 * Creates a new {@link ReaderEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link ReaderEntity} builder.
	 */
	public static final ReaderEntity readerEntity(Reader content) {
		return new ReaderEntity().setContent(content);
	}

	/**
	 * Creates a new {@link ReaderEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ReaderEntity} builder.
	 */
	public static final ReaderEntity readerEntity(Reader content, ContentType contentType) {
		return new ReaderEntity().setContent(content).setContentType(contentType);
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
	public static final SerializedEntity serializedEntity(Object content, Serializer serializer) {
		return new SerializedEntity().setContent(content).setSerializer(serializer);
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
	public static final SerializedEntity serializedEntity(Supplier<?> content, Serializer serializer) {
		return new SerializedEntity().setContent(content).setSerializer(serializer);
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
	public static final SerializedEntity serializedEntity(Object content, Serializer serializer, HttpPartSchema schema) {
		return new SerializedEntity().setContent(content).setSerializer(serializer).setSchema(schema);
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
	public static final SerializedEntity serializedEntity(Supplier<?> content, Serializer serializer, HttpPartSchema schema) {
		return new SerializedEntity().setContent(content).setSerializer(serializer).setSchema(schema);
	}

	/**
	 * Creates a new {@link StreamEntity} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StreamEntity} builder.
	 */
	public static final StreamEntity streamEntity(InputStream content) {
		return new StreamEntity().setContent(content);
	}

	/**
	 * Creates a new {@link StreamEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 * @return A new {@link StreamEntity} builder.
	 */
	public static final StreamEntity streamEntity(InputStream content, long length, ContentType contentType) {
		return new StreamEntity().setContent(content).setContentLength(length).setContentType(contentType);
	}

	/**
	 * Creates a new builder for a {@link StringEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringEntity} builder.
	 */
	public static final StringEntity stringEntity(String content) {
		return new StringEntity().setContent(content);
	}

	/**
	 * Creates a new builder for a {@link StringEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link StringEntity} builder.
	 */
	public static final StringEntity stringEntity(String content, ContentType contentType) {
		return new StringEntity().setContent(content).setContentType(contentType);
	}

	/**
	 * Creates a new builder for a {@link StringEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringEntity} builder.
	 */
	public static final StringEntity stringEntity(Supplier<String> content) {
		return new StringEntity().setContent(content);
	}

	/**
	 * Creates a new builder for a {@link StringEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link StringEntity} builder.
	 */
	public static final StringEntity stringEntity(Supplier<String> content, ContentType contentType) {
		return new StringEntity().setContent(content).setContentType(contentType);
	}
}
