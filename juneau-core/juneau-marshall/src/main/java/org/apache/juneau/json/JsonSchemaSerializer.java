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
package org.apache.juneau.json;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO metadata to HTTP responses as JSON-Schema.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>application/json+schema, text/json+schema</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/json</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Produces the JSON-schema for the JSON produced by the {@link JsonSerializer} class with the same properties.
 */
@ConfigurableContext
public class JsonSchemaSerializer extends JsonSerializer implements JsonSchemaMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsonSchemaSerializer DEFAULT = new JsonSchemaSerializer(create());

	/** Default serializer, all default settings.*/
	public static final JsonSchemaSerializer DEFAULT_READABLE = new Readable(create());

	/** Default serializer, single quotes, simple mode. */
	public static final JsonSchemaSerializer DEFAULT_SIMPLE = new Simple(create());

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final JsonSchemaSerializer DEFAULT_SIMPLE_READABLE = new SimpleReadable(create());

	//-------------------------------------------------------------------------------------------------------------------
	// Static subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected Readable(JsonSchemaSerializerBuilder builder) {
			super(builder.useWhitespace());
		}
	}

	/** Default serializer, single quotes, simple mode. */
	public static class Simple extends JsonSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected Simple(JsonSchemaSerializerBuilder builder) {
			super(builder.simpleMode().quoteChar('\''));
		}
	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class SimpleReadable extends JsonSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected SimpleReadable(JsonSchemaSerializerBuilder builder) {
			super(builder.simpleMode().quoteChar('\'').useWhitespace());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final JsonSchemaGenerator generator;

	private final Map<ClassMeta<?>,JsonSchemaClassMeta> jsonSchemaClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonSchemaBeanPropertyMeta> jsonSchemaBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JsonSchemaSerializer(JsonSchemaSerializerBuilder builder) {
		super(builder.detectRecursions().ignoreRecursions());

		generator = builder.generatorBuilder.build();
	}

	@Override /* Context */
	public JsonSchemaSerializerBuilder copy() {
		return new JsonSchemaSerializerBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link JsonSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> JsonSerializerBuilder()</code>.
	 *
	 * @return A new {@link JsonSerializerBuilder} object.
	 */
	public static JsonSchemaSerializerBuilder create() {
		return new JsonSchemaSerializerBuilder();
	}

	@Override /* Context */
	public JsonSchemaSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public JsonSchemaSerializerSession createSession(SerializerSessionArgs args) {
		return new JsonSchemaSerializerSession(this, args);
	}

	JsonSchemaGenerator getGenerator() {
		return generator;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* JsonSchemaMetaProvider */
	public JsonSchemaClassMeta getJsonSchemaClassMeta(ClassMeta<?> cm) {
		JsonSchemaClassMeta m = jsonSchemaClassMetas.get(cm);
		if (m == null) {
			m = new JsonSchemaClassMeta(cm, this);
			jsonSchemaClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* JsonSchemaMetaProvider */
	public JsonSchemaBeanPropertyMeta getJsonSchemaBeanPropertyMeta(BeanPropertyMeta bpm) {
		JsonSchemaBeanPropertyMeta m = jsonSchemaBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new JsonSchemaBeanPropertyMeta(bpm.getDelegateFor(), this);
			jsonSchemaBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"JsonSchemaSerializer",
				OMap
					.create()
					.filtered()
					.a("generator", generator)
			);
	}
}