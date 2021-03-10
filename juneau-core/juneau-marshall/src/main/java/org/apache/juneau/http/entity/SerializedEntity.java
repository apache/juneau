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
import org.apache.juneau.*;
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
}
