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
package org.apache.juneau.bean.jsonschema;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.marshall.parser.*;

/**
 * Bridge for generating typed {@link JsonSchema} beans from Java types.
 *
 * <p>
 * {@link JsonSchemaGenerator} in <c>juneau-marshall</c> produces <c>JsonMap</c> output.  This class wraps that
 * generator and converts the generated map into typed <c>JsonSchema</c> beans in <c>juneau-bean-jsonschema</c>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Default generation.</jc>
 * 	JsonSchema <jv>s1</jv> = JsonSchemaBeanGenerator.<jsf>DEFAULT</jsf>.generate(MyBean.<jk>class</jk>);
 *
 * 	<jc>// Custom generation with bean defs and descriptions.</jc>
 * 	JsonSchema <jv>s2</jv> = JsonSchemaBeanGenerator.<jsm>create</jsm>()
 * 		.useBeanDefs()
 * 		.addDescriptionsTo(TypeCategory.<jsf>ANY</jsf>)
 * 		.build()
 * 		.generate(MyBean.<jk>class</jk>);
 * </p>
 */
public final class JsonSchemaBeanGenerator {

	/** Reusable default instance. */
	public static final JsonSchemaBeanGenerator DEFAULT = create().build();

	/** Creates a new builder. */
	public static Builder create() {
		return new Builder();
	}

	private final JsonSchemaGenerator generator;

	private JsonSchemaBeanGenerator(Builder builder) {
		this.generator = builder.generatorBuilder.build();
	}

	/**
	 * Generates a schema bean from a Java type.
	 *
	 * @param type The Java type.
	 * @return The generated schema bean.
	 */
	public JsonSchema generate(Type type) {
		assertArgNotNull("type", type);
		try {
			var session = generator.getSession();
			var root = session.getSchema(type);
			if (root == null)
				return null;
			var defs = session.getBeanDefs();
			if (defs != null && ! defs.isEmpty())
				root.append("$defs", defs);
			return toBean(root);
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	/**
	 * Generates a schema bean from a Java class.
	 *
	 * @param type The Java class.
	 * @return The generated schema bean.
	 */
	public JsonSchema generate(Class<?> type) {
		return generate((Type)type);
	}

	/**
	 * Generates a schema bean from an object.
	 *
	 * <p>
	 * The value can be a POJO or a <c>Class</c>/<c>Type</c>.
	 *
	 * @param o The value to infer a schema from.
	 * @return The generated schema bean.
	 */
	public JsonSchema generate(Object o) {
		assertArgNotNull("o", o);
		try {
			var session = generator.getSession();
			var root = session.getSchema(o);
			if (root == null)
				return null;
			var defs = session.getBeanDefs();
			if (defs != null && ! defs.isEmpty())
				root.append("$defs", defs);
			return toBean(root);
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	/**
	 * Converts a generated schema map into a typed bean.
	 *
	 * <p>
	 * Conversion uses a JSON roundtrip to ensure the swaps declared on {@link JsonSchema} are honored.
	 *
	 * @param schemaMap The generated schema map.
	 * @return The typed schema bean.
	 */
	public static JsonSchema toBean(JsonMap schemaMap) {
		assertArgNotNull("schemaMap", schemaMap);
		try {
			var json = Json.of(schemaMap);
			return JsonParser.create().ignoreUnknownBeanProperties().build().parse(json, JsonSchema.class);
		} catch (ParseException e) {
			throw toRex(e);
		}
	}

	/** Builder for {@link JsonSchemaBeanGenerator}. */
	public static final class Builder {
		private final JsonSchemaGenerator.Builder generatorBuilder = JsonSchemaGenerator.create();

		private Builder() {}

		/** Mirrors {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#addDescriptionsTo(TypeCategory...)}. */
		public Builder addDescriptionsTo(TypeCategory...values) {
			generatorBuilder.addDescriptionsTo(values);
			return this;
		}

		/** Mirrors {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#addExamplesTo(TypeCategory...)}. */
		public Builder addExamplesTo(TypeCategory...values) {
			generatorBuilder.addExamplesTo(values);
			return this;
		}

		/** Mirrors {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#allowNestedDescriptions()}. */
		public Builder allowNestedDescriptions() {
			generatorBuilder.allowNestedDescriptions();
			return this;
		}

		/** Mirrors {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#allowNestedDescriptions(boolean)}. */
		public Builder allowNestedDescriptions(boolean value) {
			generatorBuilder.allowNestedDescriptions(value);
			return this;
		}

		/** Mirrors {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#allowNestedExamples()}. */
		public Builder allowNestedExamples() {
			generatorBuilder.allowNestedExamples();
			return this;
		}

		/** Mirrors {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#allowNestedExamples(boolean)}. */
		public Builder allowNestedExamples(boolean value) {
			generatorBuilder.allowNestedExamples(value);
			return this;
		}

		/** Mirrors {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#beanDefMapper(Class)}. */
		public Builder beanDefMapper(Class<? extends MarshallingDefMapper> value) {
			generatorBuilder.beanDefMapper(value);
			return this;
		}

		/** Mirrors {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#ignoreTypes(String...)}. */
		public Builder ignoreTypes(String...values) {
			generatorBuilder.ignoreTypes(values);
			return this;
		}

		/** Mirrors {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#useBeanDefs()}. */
		public Builder useBeanDefs() {
			generatorBuilder.useBeanDefs();
			return this;
		}

		/** Mirrors {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#useBeanDefs(boolean)}. */
		public Builder useBeanDefs(boolean value) {
			generatorBuilder.useBeanDefs(value);
			return this;
		}

		/** Builds a new generator. */
		public JsonSchemaBeanGenerator build() {
			return new JsonSchemaBeanGenerator(this);
		}
	}
}
