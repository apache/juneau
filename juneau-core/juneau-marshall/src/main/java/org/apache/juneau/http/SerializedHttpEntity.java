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

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * HttpEntity for serializing POJOs as the body of HTTP requests.
 */
public class SerializedHttpEntity extends BasicHttpEntity {
	private Serializer serializer;
	private HttpPartSchema schema;
	private byte[] cache;

	/**
	 * Creator.
	 *
	 * @param content The POJO to serialize.  Can also be a {@link Reader} or {@link InputStream}.
	 * @return A new {@link SerializedHttpEntity} with uninitialized serializer and schema.
	 */
	public static SerializedHttpEntity of(Object content) {
		return new SerializedHttpEntity(content, null, null, null);
	}

	/**
	 * Constructor.
	 *
	 * @param content The POJO to serialize.  Can also be a {@link Reader} or {@link InputStream}.
	 * @param serializer The serializer to use to serialize this response.
	 * @param schema The optional schema information about the serialized part.
	 * @param contentType Override the content type defined on the serializer.
	 */
	public SerializedHttpEntity(Object content, Serializer serializer, HttpPartSchema schema, String contentType) {
		content(content);
		this.serializer = serializer;
		this.schema = schema;
		if (serializer != null && serializer.getResponseContentType() != null)
			setContentType(new BasicHeader("Content-Type", contentType != null ? contentType : serializer.getResponseContentType().toString()));
	}

	/**
	 * Sets the serializer to use to serialize the content.
	 *
	 * <p>
	 * Value is ignored if the content is a stream or reader.
	 *
	 * @param value The serializer.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SerializedHttpEntity serializer(Serializer value) {
		this.serializer = value;
		return this;
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
		Object content = getRawContent();
		if (content instanceof InputStream || content instanceof Reader || content instanceof File) {
			IOPipe.create(content, os).run();
		} else {
			try {
				if (serializer == null) {
					// If no serializer specified, just close the stream.
					os.close();
				} else {
					SerializerSessionArgs sArgs = SerializerSessionArgs.create().schema(schema);
					SerializerSession session = serializer.createSession(sArgs);
					try (Closeable c = session.isWriterSerializer() ? new OutputStreamWriter(os, UTF8) : os) {
						session.serialize(content, c);
					}
				}
			} catch (SerializeException e) {
				throw new BasicRuntimeException(e, "Serialization error on request body.");
			}
		}
	}

	@Override /* BasicHttpEntity */
	public boolean isRepeatable() {
		Object content = getRawContent();
		return (! (content instanceof InputStream || content instanceof Reader));
	}

	@Override
	public long getContentLength() {
		return -1;
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
