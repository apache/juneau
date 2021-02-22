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
public class SerializedHttpEntity extends BasicHttpEntity {
	private final Serializer serializer;
	private HttpPartSchema schema;

	/**
	 * Creator.
	 *
	 * @param content The POJO to serialize.  Can also be a {@link Reader} or {@link InputStream}.
	 * @param serializer The serializer to use to serialize this response.
	 * @return A new {@link SerializedHttpEntity} with uninitialized serializer and schema.
	 */
	public static SerializedHttpEntity of(Object content, Serializer serializer) {
		return new SerializedHttpEntity(content, serializer);
	}

	/**
	 * Creator.
	 *
	 * @param content The POJO to serialize.  Can also be a {@link Reader} or {@link InputStream}.
	 * @param serializer The serializer to use to serialize this response.
	 * @return A new {@link SerializedHttpEntity} with uninitialized serializer and schema.
	 */
	public static SerializedHttpEntity of(Supplier<?> content, Serializer serializer) {
		return new SerializedHttpEntity(content, serializer);
	}

	/**
	 * Constructor.
	 *
	 * @param content The POJO to serialize.  Can also be a {@link Reader} or {@link InputStream}.
	 * @param serializer The serializer to use to serialize this response.
	 */
	public SerializedHttpEntity(Object content, Serializer serializer) {
		super(content, ContentType.of(serializer == null ? null : serializer.getResponseContentType()), null);
		this.serializer = serializer;
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
	public SerializedHttpEntity schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	@Override /* BasicHttpEntity */
	public void writeTo(OutputStream os) throws IOException {
		if (isSerializable()) {
			try {
				os = new NoCloseOutputStream(os);
				Object o = getRawContent();
				if (serializer == null) {
					os.write(o.toString().getBytes());
					os.close();
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
		} else {
			super.writeTo(os);
		}
	}

	@Override /* BasicHttpEntity */
	public boolean isRepeatable() {
		if (isSerializable())
			return true;
		return super.isRepeatable();
	}

	@Override /* BasicHttpEntity */
	public long getContentLength() {
		if (isSerializable())
			return -1;
		return super.getContentLength();
	}

	@Override /* BasicHttpEntity */
	public InputStream getContent() {
		if (isSerializable()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				writeTo(baos);
				return new ByteArrayInputStream(baos.toByteArray());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return super.getContent();
	}

	private boolean isSerializable() {
		Object o = getRawContent();
		return ! (o instanceof InputStream || o instanceof Reader || o instanceof File);
	}

	// <FluentSetters>

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedHttpEntity cache() {
		super.cache();
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedHttpEntity cache(boolean value) {
		super.cache(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedHttpEntity chunked() {
		super.chunked();
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedHttpEntity chunked(boolean value) {
		super.chunked(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedHttpEntity contentEncoding(String value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedHttpEntity contentEncoding(Header value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedHttpEntity contentLength(long value) {
		super.contentLength(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedHttpEntity contentType(String value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public SerializedHttpEntity contentType(Header value) {
		super.contentType(value);
		return this;
	}

	// </FluentSetters>
}
