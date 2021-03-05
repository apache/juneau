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
package org.apache.juneau.http.entity;

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * HttpEntity for serializing POJOs as the body of HTTP requests.
 */
@FluentSetters
public class SerializedEntity extends AbstractHttpEntity {
	private final Supplier<?> content;
	private final Serializer serializer;
	private HttpPartSchema schema;

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
	public static SerializedEntity of(Object content, Serializer serializer) {
		return new SerializedEntity(content, serializer);
	}

	/**
	 * Creates a new {@link SerializedEntity} object.
	 *
	 * @param content
	 * 	The supplier for a Java POJO representing the content.
	 * 	<br>Can be <jk>null<jk>.
	 * @param serializer
	 * 	The serializer to use to serialize the POJO.
	 * 	<br>If <jk>null</jk>, POJO will be converted to a string using {@link Object#toString()}.
	 * @return A new {@link SerializedEntity} object.
	 */
	public static SerializedEntity of(Supplier<?> content, Serializer serializer) {
		return new SerializedEntity(content == null ? ()->null : content, serializer);
	}

	/**
	 * Constructor.
	 *
	 * @param content The POJO to serialize.  Can also be a {@link Reader} or {@link InputStream}.
	 * @param serializer The serializer to use to serialize this response.
	 */
	public SerializedEntity(Object content, Serializer serializer) {
		this(()->content, serializer);
	}

	/**
	 * Constructor.
	 *
	 * @param content The POJO to serialize.  Can also be a {@link Reader} or {@link InputStream}.
	 * @param serializer The serializer to use to serialize this response.
	 */
	public SerializedEntity(Supplier<?> content, Serializer serializer) {
		this.content = content;
		this.serializer = serializer;
		if (serializer != null)
			setContentType(ContentType.of(serializer.getPrimaryMediaType()));
	}

	/**
	 * Sets the schema to use to serialize the content.
	 *
	 * <p>
	 * Value is ignored if the serializer is not schema-aware.
	 *
	 * @param value The schema.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializedEntity schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	@Override /* HttpEntity */
	public void writeTo(OutputStream os) throws IOException {
		try {
			os = new NoCloseOutputStream(os);
			Object o = content.get();
			if (serializer == null) {
				try (Writer w = new OutputStreamWriter(os, UTF8)) {
					w.write(o.toString());
				}
			} else {
				SerializerSessionArgs sArgs = SerializerSessionArgs.create().schema(schema);
				SerializerSession session = serializer.createSession(sArgs);
				try (Closeable c = session.isWriterSerializer() ? new OutputStreamWriter(os, UTF8) : os) {
					session.serialize(o, c);
				}
			}
		} catch (SerializeException e) {
			throw new BasicRuntimeException(e, "Serialization error on request body.");
		}
	}

	@Override /* BasicHttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* BasicHttpEntity */
	public long getContentLength() {
		return -1;
	}

	@Override /* BasicHttpEntity */
	public InputStream getContent() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			writeTo(baos);
			return new ByteArrayInputStream(baos.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// <FluentSetters>

	@Override /* AbstractHttpEntity */
	public SerializedEntity cache() {
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedEntity chunked() {
		super.chunked();
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedEntity chunked(boolean value) {
		super.chunked(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedEntity contentEncoding(String value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedEntity contentEncoding(Header value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedEntity contentLength(long value) {
		super.contentLength(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedEntity contentType(String value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedEntity contentType(Header value) {
		super.contentType(value);
		return this;
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	// </FluentSetters>
}
