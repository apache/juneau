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
package org.apache.juneau.html;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Context object that lives for the duration of a single serialization of {@link HtmlSchemaSerializer} and its subclasses.
 *
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class HtmlSchemaSerializerSession extends HtmlSerializerSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(HtmlSchemaSerializer ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends HtmlSerializerSession.Builder {

		HtmlSchemaSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(HtmlSchemaSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public HtmlSchemaSerializerSession build() {
			return new HtmlSchemaSerializerSession(this);
		}

		// <FluentSetters>

		@Override /* GENERATED - SessionArgs */
		public <T> Builder ifType(Class<T> type, Consumer<T> apply) {
			super.ifType(type, apply);
			return this;
		}

		@Override
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override
		public Builder useWhitespace(Boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - SessionArgs */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - SessionArgs */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - SessionArgs */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - SessionArgs */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - SessionArgs */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - SessionArgs */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - SessionArgs */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - BeanSessionArgs */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final JsonSchemaGeneratorSession genSession;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected HtmlSchemaSerializerSession(Builder builder) {
		super(builder);
		this.genSession = builder.ctx.getGenerator().getSession();
	}

	@Override /* SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		try {
			super.doSerialize(out, genSession.getSchema(o));
		} catch (BeanRecursionException e) {
			throw new SerializeException(e);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* ContextSession */
	public OMap toMap() {
		return super.toMap()
			.a(
				"HtmlSchemaSerializerSession",
				OMap
					.create()
					.filtered()
			);
	}
}
