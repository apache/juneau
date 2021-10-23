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

import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;

import java.io.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * HttpEntity for serializing POJOs as the body of HTTP requests.
 */
public class SerializedEntity extends BasicHttpEntity {
	final Serializer serializer;
	HttpPartSchema schema;

	/**
	 * Creates a new {@link SerializedEntity} builder.
	 *
	 * @return A new {@link SerializedEntity} builder.
	 */
	public static SerializedEntityBuilder<SerializedEntity> create() {
		return new SerializedEntityBuilder<>(SerializedEntity.class);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this bean.
	 */
	public SerializedEntity(SerializedEntityBuilder<?> builder) {
		super(builder);
		serializer = builder.serializer;
		schema = builder.schema;
	}

	/**
	 * Creates a copy of this object.
	 *
	 * @return A new copy of this object.
	 */
	@Override
	public SerializedEntityBuilder<SerializedEntity> copy() {
		return new SerializedEntityBuilder<>(this);
	}

	/**
	 * Copies this bean and sets the serializer and schema on it.
	 *
	 * @param serializer The new serializer for the bean.  Can be <jk>null</jk>.
	 * @param schema The new schema for the bean.  Can be <jk>null</jk>.
	 * @return Either a new bean with the serializer set, or this bean if
	 * 	both values are <jk>null</jk> or the serializer and schema were already set.
	 */
	public SerializedEntity copyWith(Serializer serializer, HttpPartSchema schema) {
		if ((this.serializer == null && serializer != null) || (this.schema == null && schema != null)) {
			SerializedEntityBuilder<SerializedEntity> h = copy();
			if (serializer != null)
				h.serializer(serializer);
			if (schema != null)
				h.schema(schema);
			return h.build();
		}
		return this;
	}

	@Override
	public Header getContentType() {
		Header x = super.getContentType();
		if (x == null && serializer != null)
			x = contentType(serializer.getPrimaryMediaType());
		return x;
	}

	@Override /* HttpEntity */
	public void writeTo(OutputStream os) throws IOException {
		try {
			os = new NoCloseOutputStream(os);
			Object o = contentOrElse(null);
			if (serializer == null) {
				try (Writer w = new OutputStreamWriter(os, UTF8)) {
					w.write(o.toString());
				}
			} else {
				SerializerSession session = serializer.createSession().schema(schema).build();
				try (Closeable c = session.isWriterSerializer() ? new OutputStreamWriter(os, UTF8) : os) {
					session.serialize(o, c);
				}
			}
		} catch (SerializeException e) {
			throw runtimeException(e, "Serialization error on request body.");
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
			throw runtimeException(e);
		}
	}
}
