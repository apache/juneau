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

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * HttpEntity for serializing POJOs as the body of HTTP requests.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
@FluentSetters
public class SerializedEntity extends BasicHttpEntity {

	//-----------------------------------------------------------------------------------------------------------------
	// Instances
	//-----------------------------------------------------------------------------------------------------------------

	Serializer serializer;
	HttpPartSchema schema;

	/**
	 * Constructor.
	 */
	public SerializedEntity() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.
	 * @param content The entity content.
	 * @param serializer The entity serializer.
	 * @param schema The entity schema.  Can be <jk>null</jk>.
	 */
	public SerializedEntity(ContentType contentType, Object content, Serializer serializer, HttpPartSchema schema) {
		super(contentType, content);
		this.serializer = serializer;
		this.schema = schema;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.
	 */
	public SerializedEntity(SerializedEntity copyFrom) {
		super(copyFrom);
		this.serializer = copyFrom.serializer;
		this.schema = copyFrom.schema;
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * <p>
	 * Allows you to create a modifiable copy of this bean.
	 *
	 * @return A new builder bean.
	 */
	@Override
	public SerializedEntity copy() {
		return new SerializedEntity(this);
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
			SerializedEntity h = copy();
			if (serializer != null)
				h.setSerializer(serializer);
			if (schema != null)
				h.setSchema(schema);
			return h;
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the serializer on this entity bean.
	 *
	 * @param value The entity serializer, can be <jk>null</jk>.
	 * @return This object.
	 */
	@FluentSetter
	public SerializedEntity setSerializer(Serializer value) {
		assertModifiable();
		this.serializer = value;
		return this;
	}

	/**
	 * Sets the schema on this entity bean.
	 *
	 * <p>
	 * Used to provide instructions to the serializer on how to serialize this object.
	 *
	 * @param value The entity schema, can be <jk>null</jk>.
	 * @return This object.
	 */
	@FluentSetter
	public SerializedEntity setSchema(HttpPartSchema value) {
		assertModifiable();
		this.schema = value;
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
			throw asRuntimeException(e);
		}
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setCached() throws IOException{
		super.setCached();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setCharset(Charset value) {
		super.setCharset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setChunked() {
		super.setChunked();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setChunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setContent(Object value) {
		super.setContent(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setContent(Supplier<?> value) {
		super.setContent(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setContentEncoding(ContentEncoding value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setContentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setContentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setContentType(ContentType value) {
		super.setContentType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setMaxLength(int value) {
		super.setMaxLength(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public SerializedEntity setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}

	// </FluentSetters>
}
