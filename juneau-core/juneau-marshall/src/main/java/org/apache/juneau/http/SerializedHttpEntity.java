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
import org.apache.juneau.utils.*;

/**
 * HttpEntity for serializing POJOs as the body of HTTP requests.
 */
public class SerializedHttpEntity extends BasicHttpEntity {
	private final Serializer serializer;
	private HttpPartSchema schema;
	private byte[] cache;

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
		os = new NoCloseOutputStream(os);
		Object o = getRawContent();
		if (o instanceof InputStream || o instanceof Reader || o instanceof File) {
			IOPipe.create(o, os).run();
		} else {
			try {
				if (serializer == null) {
					// If no serializer specified, just close the stream.
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
		}
	}

	@Override /* BasicHttpEntity */
	public boolean isRepeatable() {
		Object o = getRawContent();
		return (! (o instanceof InputStream || o instanceof Reader));
	}

	@Override /* BasicHttpEntity */
	public long getContentLength() {
		return -1;
	}

	@Override /* BasicHttpEntity */
	public Header getContentType() {
		Header x = super.getContentType();
		if (x != null)
			return x;
		Object o = getRawContent();
		if (o instanceof InputStream || o instanceof Reader || o instanceof File)
			return null;
		return null;
	}

	@Override /* BasicHttpEntity */
	public InputStream getContent() {
		if (cache == null) {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				writeTo(baos);
				cache = baos.toByteArray();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return new ByteArrayInputStream(cache);
	}
}
