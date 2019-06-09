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
package org.apache.juneau.rest.client;

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;

import org.apache.http.entity.*;
import org.apache.http.message.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * HttpEntity for serializing POJOs as the body of HTTP requests.
 */
public final class RestRequestEntity extends BasicHttpEntity {
	final Object output;
	final Serializer serializer;
	final HttpPartSchema schema;
	byte[] outputBytes;

	/**
	 * Constructor.
	 *
	 * @param input The POJO to serialize.  Can also be a {@link Reader} or {@link InputStream}.
	 * @param serializer The serializer to use to serialize this response.
	 * @param schema The optional schema information about the serialized part.
	 */
	public RestRequestEntity(Object input, Serializer serializer, HttpPartSchema schema) {
		this.output = input;
		this.serializer = serializer;
		this.schema = schema;
		if (serializer != null && serializer.getResponseContentType() != null)
			setContentType(new BasicHeader("Content-Type", serializer.getResponseContentType().toString()));
	}

	@Override /* BasicHttpEntity */
	public void writeTo(OutputStream os) throws IOException {
		os = new NoCloseOutputStream(os);
		if (output instanceof InputStream) {
			IOPipe.create(output, os).run();
		} else if (output instanceof Reader) {
			try (OutputStreamWriter osw = new OutputStreamWriter(os, UTF8)) {
				IOPipe.create(output, osw).run();
			}
		} else {
			try {
				if (serializer == null) {
					// If no serializer specified, just close the stream.
					os.close();
				} else {
					SerializerSessionArgs sArgs = SerializerSessionArgs.create().schema(schema);
					SerializerSession session = serializer.createSession(sArgs);
					try (Closeable c = session.isWriterSerializer() ? new OutputStreamWriter(os, UTF8) : os) {
						session.serialize(output, c);
					}
				}
			} catch (SerializeException e) {
				throw new org.apache.juneau.rest.client.RestCallException(e);
			}
		}
	}

	@Override /* BasicHttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* BasicHttpEntity */
	public InputStream getContent() {
		if (outputBytes == null) {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				writeTo(baos);
				outputBytes = baos.toByteArray();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return new ByteArrayInputStream(outputBytes);
	}
}
