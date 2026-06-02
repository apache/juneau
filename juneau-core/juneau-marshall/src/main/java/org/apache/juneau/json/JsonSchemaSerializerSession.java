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
package org.apache.juneau.json;

import java.io.*;
import org.apache.juneau.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.commons.bean.BeanPropertyMeta;

/**
 * Session object that lives for the duration of a single use of {@link JsonSchemaSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonBasics">JSON Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for JsonSchemaSerializerSession hierarchy
})
public class JsonSchemaSerializerSession extends JsonSerializerSession {

	/**
	 * Builder class.
	 */
	public static class Builder extends JsonSerializerSession.Builder<Builder> {

		private JsonSchemaSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(JsonSchemaSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public JsonSchemaSerializerSession build() {
			return new JsonSchemaSerializerSession(this);
		}

	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(JsonSchemaSerializer ctx) {
		return new Builder(ctx);
	}

	private final JsonSchemaGeneratorSession genSession;
	private final JsonSchemaSerializer ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JsonSchemaSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		genSession = ctx.getGenerator().getSession();
	}

	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		try {
			super.doSerialize(out, genSession.getSchema(o));
		} catch (MarshallingRecursionException e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	protected JsonSchemaBeanPropertyMeta getJsonSchemaBeanPropertyMeta(BeanPropertyMeta bpm) {
		return ctx.getJsonSchemaBeanPropertyMeta(bpm);
	}

	/**
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	protected JsonSchemaClassMeta getJsonSchemaClassMeta(ClassMeta<?> cm) {
		return ctx.getJsonSchemaClassMeta(cm);
	}
}