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
package org.apache.juneau.marshall.json;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.marshall.stream.*;

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
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonSupport">JSON Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for JsonSchemaSerializer hierarchy
	"java:S115",  // Constants use UPPER_snakeCase convention (e.g., PROP_generator)
	"resource" // Closeable resources are owned by the caller's serializer session; Eclipse JDT @Owning warning is by design.
})
public class JsonSchemaSerializer extends JsonSerializer implements JsonSchemaMetaProvider {

	// Property name constants
	private static final String PROP_generator = "generator";

	/**
	 * Builder class.
	 */
	public static class Builder extends JsonSerializer.Builder<Builder> {

		private static final Cache<HashKey,JsonSchemaSerializer> CACHE = Cache.of(HashKey.class, JsonSchemaSerializer.class).build();

		JsonSchemaGenerator.Builder generatorBuilder;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/json");
			accept("application/json+schema,text/json+schema");
			generatorBuilder = JsonSchemaGenerator.create().marshallingContext(marshallingContext());
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			generatorBuilder = copyFrom.generatorBuilder.copy().marshallingContext(marshallingContext());
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(JsonSchemaSerializer copyFrom) {
			super(copyFrom);
			generatorBuilder = copyFrom.generator.copy().marshallingContext(marshallingContext());
		}

		/**
		 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Add descriptions.
		 *
		 * <p>
		 * Identifies which categories of types that descriptions should be automatically added to generated schemas.
		 * <p>
		 * The description is the result of calling {@link ClassMeta#getName()}.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#addDescriptionsTo(TypeCategory...)}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>The default is an empty string.
		 * @return This object.
		 */
		public Builder addDescriptionsTo(TypeCategory...values) {
			generatorBuilder.addDescriptionsTo(values);
			return this;
		}

		/**
		 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Add examples.
		 *
		 * <p>
		 * Identifies which categories of types that examples should be automatically added to generated schemas.
		 * <p>
		 * The examples come from calling {@link ClassMeta#getExample(MarshallingSession,JsonParserSession)} which in turn gets examples
		 * from the following:
		 * <ul class='javatree'>
		 * 	<li class='ja'>{@link Example}
		 * 	<li class='ja'>{@link Marshalled#example() Marshalled(example)}
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#addExamplesTo(TypeCategory...)}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>The default is an empty string.
		 * @return This object.
		 */
		public Builder addExamplesTo(TypeCategory...values) {
			generatorBuilder.addExamplesTo(values);
			return this;
		}

		/**
		 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested descriptions.
		 *
		 * <p>
		 * Identifies whether nested descriptions are allowed in schema definitions.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#allowNestedDescriptions()}
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder allowNestedDescriptions() {
			generatorBuilder.allowNestedDescriptions();
			return this;
		}

		/**
		 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested examples.
		 *
		 * <p>
		 * Identifies whether nested examples are allowed in schema definitions.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#allowNestedExamples()}
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder allowNestedExamples() {
			generatorBuilder.allowNestedExamples();
			return this;
		}

		/**
		 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Schema definition mapper.
		 *
		 * <p>
		 * Interface to use for converting Bean classes to definition IDs and URIs.
		 * <p>
		 * Used primarily for defining common definition sections for beans in Swagger JSON.
		 * <p>
		 * This setting is ignored if {@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#useBeanDefs()} is not enabled.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.marshall.jsonschema.JsonSchemaGenerator.Builder#beanDefMapper(Class)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link BasicBeanDefMapper}.
		 * @return This object.
		 */
		public Builder beanDefMapper(Class<? extends MarshallingDefMapper> value) {
			generatorBuilder.beanDefMapper(value);
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public JsonSchemaSerializer build() {
			return cache(CACHE).build(JsonSchemaSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), generatorBuilder.hashKey());
		}

		/**
		 * <i><l>JsonSchemaSerializer</l> configuration property:&emsp;</i>  Use bean definitions.
		 *
		 * <p>
		 * When enabled, schemas on beans will be serialized as the following:
		 * <p class='bjson'>
		 * 	{
		 * 		type: <js>'object'</js>,
		 * 		<js>'$ref'</js>: <js>'#/definitions/TypeId'</js>
		 * 	}
		 * </p>
		 *
		 * @return This object.
		 */
		public Builder useBeanDefs() {
			generatorBuilder.useBeanDefs();
			return this;
		}


	}

	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(Builder builder) {
			super(builder.useWhitespace());
		}
	}

	/** Default serializer, all default settings.*/
	public static final JsonSchemaSerializer DEFAULT = new JsonSchemaSerializer(create());
	/** Default serializer, all default settings.*/
	public static final JsonSchemaSerializer DEFAULT_READABLE = new Readable(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final JsonSchemaGenerator generator;

	private final Map<ClassMeta<?>,JsonSchemaClassMeta> jsonSchemaClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonSchemaBeanPropertyMeta> jsonSchemaBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonSchemaSerializer(Builder builder) {
		super(builder.detectRecursions().ignoreRecursions());

		generator = builder.generatorBuilder.build();
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public JsonSchemaSerializerSession.Builder createSession() {
		return JsonSchemaSerializerSession.create(this);
	}

	@Override /* Overridden from JsonSchemaMetaProvider */
	public JsonSchemaBeanPropertyMeta getJsonSchemaBeanPropertyMeta(BeanPropertyMeta bpm) {
		return jsonSchemaBeanPropertyMetas.computeIfAbsent(bpm, k -> new JsonSchemaBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from JsonSchemaMetaProvider */
	public JsonSchemaClassMeta getJsonSchemaClassMeta(ClassMeta<?> cm) {
		return jsonSchemaClassMetas.computeIfAbsent(cm, k -> new JsonSchemaClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public JsonSchemaSerializerSession getSession() { return createSession().build(); }

	/**
	 * Convenience delegator (default session args) for the raw-JSON token writer.  Real impl on
	 * {@link JsonSchemaSerializerSession#writeTokens(Object)}.
	 *
	 * <p>
	 * <b>Note:</b> the cursor's structural methods produce ordinary JSON regardless of the
	 * JsonSchema-aware {@link #write(Object)} path; the {@link TokenWriter#object(Object)
	 * object(Object)} bridge is <b>disabled</b>.  Use {@link #write(Object)} for schema
	 * generation, or compose the schema manually via the structural methods.
	 *
	 * @param output The output.
	 * @return A new {@link JsonTokenWriter} with {@code object(...)} disabled.
	 * @throws IOException If the output type is not supported or could not be opened.
	 */
	@Override /* TokenWritable */
	public TokenWriter writeTokens(Object output) throws IOException {
		return getSession().writeTokens(output);
	}

	/**
	 * Convenience delegator (default session args) for a record writer that emits the JSON Schema
	 * for each value passed to {@link RecordWriter#write(Object) write(...)}.  Real impl on
	 * {@link JsonSchemaSerializerSession#writeRecords(Object)}.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter}.
	 * @throws IOException If the output type is not supported or could not be opened.
	 */
	@Override /* RecordWritable */
	public RecordWriter writeRecords(Object output) throws IOException {
		return getSession().writeRecords(output);
	}

	@Override /* RecordWritable */
	public boolean isRecordStreaming() { return false; }

	@Override /* Overridden from JsonSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_generator, generator);
	}

	JsonSchemaGenerator getGenerator() { return generator; }
}