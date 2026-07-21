/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.classic.entity;

import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.http.classic.HttpHeaders.*;

import java.io.*;

import org.apache.http.*;
import org.apache.juneau.commons.io.*;
import org.apache.juneau.http.UnmodifiableBean;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * HttpEntity for serializing POJOs as the body of HTTP requests.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>

 * </ul>
 */
public class SerializedEntity extends BasicHttpEntity<SerializedEntity> {
	Serializer serializer;
	HttpPartSchema schema;

	/**
	 * Constructor.
	 */
	public SerializedEntity() {}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.  Can be <jk>null</jk>.
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param serializer The entity serializer.  Can be <jk>null</jk>.
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
	 * @param copyFrom The bean being copied.  Must not be <jk>null</jk>.
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
	@Override /* Overridden from BasicHttpEntity */
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
		if ((this.serializer == null && nn(serializer)) || (this.schema == null && nn(schema))) {
			SerializedEntity h = copy();
			if (nn(serializer))
				h.setSerializer(serializer);
			if (nn(schema))
				h.setSchema(schema);
			return h;
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Overridden from BasicHttpEntity */
	public InputStream getContent() {
		var baos = new ByteArrayOutputStream();
		try {
			writeTo(baos);
			return new ByteArrayInputStream(baos.toByteArray());
		} catch (IOException e) {
			throw toRex(e);
		}
	}

	@Override /* Overridden from BasicHttpEntity */
	public long getContentLength() { return -1; }

	@Override
	public Header getContentType() {
		Header x = super.getContentType();
		if (x == null && nn(serializer))
			x = contentType(serializer.getPrimaryMediaType());
		return x;
	}

	@Override /* Overridden from BasicHttpEntity */
	public boolean isRepeatable() { return true; }

	/**
	 * Sets the schema on this entity bean.
	 *
	 * <p>
	 * Used to provide instructions to the serializer on how to serialize this object.
	 *
	 * @param value The entity schema, can be <jk>null</jk>.
	 * @return This object.
	 */
	public SerializedEntity setSchema(HttpPartSchema value) {
		return modify(() -> schema = value);
	}

	/**
	 * Sets the serializer on this entity bean.
	 *
	 * @param value The entity serializer, can be <jk>null</jk>.
	 * @return This object.
	 */
	public SerializedEntity setSerializer(Serializer value) {
		return modify(() -> serializer = value);
	}

	@Override /* Overridden from BasicHttpEntity */
	public SerializedEntity unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	@Override /* Overridden from HttpEntity */
	public void writeTo(OutputStream os) throws IOException {
		try {
			os = new NoCloseOutputStream(os);
			Object o = contentOrElse(null);
			if (serializer == null) {
				try (Writer w = new OutputStreamWriter(os, UTF8)) {
					w.write(o == null ? "" : o.toString());
				}
			} else {
				SerializerSession session = serializer.createSession().schema(schema).build();
				try (Closeable c = session.isWriterSerializer() ? new OutputStreamWriter(os, UTF8) : os) {
					session.write(o, c);
				}
			}
		} catch (SerializeException e) {
			throw rex(e, "Serialization error on request body.");
		}
	}

	/**
	 * Unmodifiable point-in-time snapshot of the enclosing {@link SerializedEntity}.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled through
	 * {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends SerializedEntity implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The entity to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(SerializedEntity copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicHttpEntity */
		protected SerializedEntity modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}
