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
import org.apache.juneau.serializer.*;

/**
 * Standard predefined HTTP entities.
 */
public class HttpEntities {

	/**
	 * Creates a new {@link ByteArrayEntity} object.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link ByteArrayEntity} object.
	 */
	public static final ByteArrayEntity byteArrayEntity(byte[] content) {
		return ByteArrayEntity.of(content);
	}

	/**
	 * Creates a new {@link ByteArrayEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ByteArrayEntity} object.
	 */
	public static final ByteArrayEntity byteArrayEntity(byte[] content, ContentType contentType) {
		return ByteArrayEntity.of(content, contentType);
	}

	/**
	 * Creates a new {@link FileEntity} object.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link FileEntity} object.
	 */
	public static final FileEntity fileEntity(File content) {
		return FileEntity.of(content);
	}

	/**
	 * Creates a new {@link FileEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link FileEntity} object.
	 */
	public static final FileEntity fileEntity(File content, ContentType contentType) {
		return FileEntity.of(content, contentType);
	}

	/**
	 * Creates a new {@link ReaderEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link ReaderEntity} object.
	 */
	public static final ReaderEntity readerEntity(Reader content) {
		return ReaderEntity.of(content);
	}

	/**
	 * Creates a new {@link ReaderEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ReaderEntity} object.
	 */
	public static final ReaderEntity readerEntity(Reader content, ContentType contentType) {
		return ReaderEntity.of(content, -1, contentType);
	}

	/**
	 * Creates a new {@link SerializedEntity} object.
	 *
	 * @param content
	 * 	The Java POJO representing the content.
	 * 	<br>Can be <jk>null<jk>.
	 * @param serializer
	 * 	The serializer to use to serialize the POJO.
	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
	 * @return A new {@link SerializedEntity} object.
	 */
	public static final SerializedEntity serializedEntity(Object content, Serializer serializer) {
		return SerializedEntity.of(content, serializer);
	}

	/**
	 * Creates a new {@link SerializedEntity} object.
	 *
	 * @param content
	 * 	The supplier of a Java POJO representing the content.
	 * 	<br>Can be <jk>null<jk>.
	 * @param serializer
	 * 	The serializer to use to serialize the POJO.
	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
	 * @return A new {@link SerializedEntity} object.
	 */
	public static final SerializedEntity serializedEntity(Supplier<?> content, Serializer serializer) {
		return SerializedEntity.of(content, serializer);
	}

	/**
	 * Creates a new {@link InputStreamEntity} object.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link InputStreamEntity} object.
	 */
	public static final InputStreamEntity streamEntity(InputStream content) {
		return InputStreamEntity.of(content);
	}

	/**
	 * Creates a new {@link InputStreamEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 * @return A new {@link InputStreamEntity} object.
	 */
	public static final InputStreamEntity streamEntity(InputStream content, long length, ContentType contentType) {
		return InputStreamEntity.of(content, length, contentType);
	}

	/**
	 * Creates a new {@link StringEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringEntity} object.
	 */
	public static final StringEntity stringEntity(String content) {
		return StringEntity.of(content);
	}

	/**
	 * Creates a new {@link StringEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link StringEntity} object.
	 */
	public static final StringEntity stringEntity(String content, ContentType contentType) {
		return StringEntity.of(content, contentType);
	}
}
