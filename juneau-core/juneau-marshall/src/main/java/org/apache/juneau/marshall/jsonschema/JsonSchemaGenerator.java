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
package org.apache.juneau.marshall.jsonschema;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.SystemUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;

/**
 * Generates JSON-schema metadata about POJOs.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <p>
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonSchemaDetails">JSON-Schema Support</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class JsonSchemaGenerator extends MarshallingTraverseContext implements JsonSchemaMetaProvider {

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";
	private static final String ARG_values = "values";
	private static final String ARG_copyFrom = "copyFrom";

	// Property name constants
	private static final String PROP_addDescriptionsTo = "addDescriptionsTo";
	private static final String PROP_addExamplesTo = "addExamplesTo";
	private static final String PROP_allowNestedDescriptions = "allowNestedDescriptions";
	private static final String PROP_allowNestedExamples = "allowNestedExamples";
	private static final String PROP_beanDefMapper = "beanDefMapper";
	private static final String PROP_ignoreTypes = "ignoreTypes";
	private static final String PROP_useBeanDefs = "useBeanDefs";

	/**
	 * Builder class.
	 */
	public static class Builder extends MarshallingTraverseContext.Builder<Builder> {

		private static final Cache<HashKey,JsonSchemaGenerator> CACHE = Cache.of(HashKey.class, JsonSchemaGenerator.class).build();

		protected final JsonParser.Builder<?> jsonParserBuilder;
		protected final JsonSerializer.Builder<?> jsonSerializerBuilder;

		private boolean allowNestedDescriptions;
		private boolean allowNestedExamples;
		private boolean useBeanDefs;
		private Class<? extends MarshallingDefMapper> beanDefMapper;
		private SortedSet<TypeCategory> addDescriptionsTo;
		private SortedSet<TypeCategory> addExamplesTo;
		private SortedSet<String> ignoreTypes;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			MarshallingContext.Builder bc = marshallingContext();
			jsonSerializerBuilder = JsonSerializer.create().marshallingContext(bc);
			jsonParserBuilder = Json5Parser.create().marshallingContext(bc);
			registerBuilders(jsonSerializerBuilder, jsonParserBuilder);
			addDescriptionsTo = null;
			addExamplesTo = null;
			allowNestedDescriptions = env("JsonSchemaGenerator.allowNestedDescriptions", false);
			allowNestedExamples = env("JsonSchemaGenerator.allowNestedExamples", false);
			useBeanDefs = env("JsonSchemaGenerator.useBeanDefs", false);
			beanDefMapper = BasicBeanDefMapper.class;
			ignoreTypes = null;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			MarshallingContext.Builder bc = marshallingContext();
			jsonSerializerBuilder = copyFrom.jsonSerializerBuilder.copy().marshallingContext(bc);
			jsonParserBuilder = copyFrom.jsonParserBuilder.copy().marshallingContext(bc);
			registerBuilders(jsonSerializerBuilder, jsonParserBuilder);
			addDescriptionsTo = copyFrom.addDescriptionsTo == null ? null : new TreeSet<>(copyFrom.addDescriptionsTo);
			addExamplesTo = copyFrom.addExamplesTo == null ? null : new TreeSet<>(copyFrom.addExamplesTo);
			allowNestedDescriptions = copyFrom.allowNestedDescriptions;
			allowNestedExamples = copyFrom.allowNestedExamples;
			beanDefMapper = copyFrom.beanDefMapper;
			ignoreTypes = copyFrom.ignoreTypes == null ? null : new TreeSet<>(copyFrom.ignoreTypes);
			useBeanDefs = copyFrom.useBeanDefs;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(JsonSchemaGenerator copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			MarshallingContext.Builder bc = marshallingContext();
			jsonSerializerBuilder = copyFrom.jsonSerializer.copy().marshallingContext(bc);
			jsonParserBuilder = copyFrom.jsonParser.copy().marshallingContext(bc);
			registerBuilders(jsonSerializerBuilder, jsonParserBuilder);
			addDescriptionsTo = copyFrom.addDescriptionsTo.isEmpty() ? null : new TreeSet<>(copyFrom.addDescriptionsTo);
			addExamplesTo = copyFrom.addExamplesTo.isEmpty() ? null : new TreeSet<>(copyFrom.addExamplesTo);
			allowNestedDescriptions = copyFrom.allowNestedDescriptions;
			allowNestedExamples = copyFrom.allowNestedExamples;
			beanDefMapper = copyFrom.beanDefMapper;
			ignoreTypes = copyFrom.ignoreTypes.isEmpty() ? null : new TreeSet<>(copyFrom.ignoreTypes);
			useBeanDefs = copyFrom.useBeanDefs;
		}

		/**
		 * Add descriptions.
		 *
		 * <p>
		 * Identifies which categories of types that descriptions should be automatically added to generated schemas.
		 * The description is the result of calling {@link ClassMeta#getName()}.
		 * The format is a comma-delimited list of any of the following values:
		 *
		 * <ul class='javatree'>
		 * 	<li class='jf'>{@link TypeCategory#BEAN BEAN}
		 * 	<li class='jf'>{@link TypeCategory#COLLECTION COLLECTION}
		 * 	<li class='jf'>{@link TypeCategory#ARRAY ARRAY}
		 * 	<li class='jf'>{@link TypeCategory#MAP MAP}
		 * 	<li class='jf'>{@link TypeCategory#STRING STRING}
		 * 	<li class='jf'>{@link TypeCategory#NUMBER NUMBER}
		 * 	<li class='jf'>{@link TypeCategory#BOOLEAN BOOLEAN}
		 * 	<li class='jf'>{@link TypeCategory#ANY ANY}
		 * 	<li class='jf'>{@link TypeCategory#OTHER OTHER}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>The default is an empty string.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder addDescriptionsTo(TypeCategory...values) {
			assertArgNoNulls(ARG_values, values);
			addDescriptionsTo = addAll(addDescriptionsTo, values);
			return this;
		}

		/**
		 * Add examples.
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
		 * <p>
		 * The format is a comma-delimited list of any of the following values:
		 *
		 * <ul class='javatree'>
		 * 	<li class='jf'>{@link TypeCategory#BEAN BEAN}
		 * 	<li class='jf'>{@link TypeCategory#COLLECTION COLLECTION}
		 * 	<li class='jf'>{@link TypeCategory#ARRAY ARRAY}
		 * 	<li class='jf'>{@link TypeCategory#MAP MAP}
		 * 	<li class='jf'>{@link TypeCategory#STRING STRING}
		 * 	<li class='jf'>{@link TypeCategory#NUMBER NUMBER}
		 * 	<li class='jf'>{@link TypeCategory#BOOLEAN BOOLEAN}
		 * 	<li class='jf'>{@link TypeCategory#ANY ANY}
		 * 	<li class='jf'>{@link TypeCategory#OTHER OTHER}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>The default is an empty string.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder addExamplesTo(TypeCategory...values) {
			assertArgNoNulls(ARG_values, values);
			addExamplesTo = addAll(addExamplesTo, values);
			return this;
		}

		/**
		 * Allow nested descriptions.
		 *
		 * <p>
		 * Identifies whether nested descriptions are allowed in schema definitions.
		 *
		 * @return This object.
		 */
		public Builder allowNestedDescriptions() {
			return allowNestedDescriptions(true);
		}

		/**
		 * Same as {@link #allowNestedDescriptions()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder allowNestedDescriptions(boolean value) {
			allowNestedDescriptions = value;
			return this;
		}

		/**
		 * Allow nested examples.
		 *
		 * <p>
		 * Identifies whether nested examples are allowed in schema definitions.
		 *
		 * @return This object.
		 */
		public Builder allowNestedExamples() {
			return allowNestedExamples(true);
		}

		/**
		 * Same as {@link #allowNestedExamples()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder allowNestedExamples(boolean value) {
			allowNestedExamples = value;
			return this;
		}

		/**
		 * Schema definition mapper.
		 *
		 * <p>
		 * Interface to use for converting Bean classes to definition IDs and URIs.
		 * <p>
		 * Used primarily for defining common definition sections for beans in Swagger JSON.
		 * <p>
		 * This setting is ignored if {@link JsonSchemaGenerator.Builder#useBeanDefs()} is not enabled.
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is {@link BasicBeanDefMapper}.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder beanDefMapper(Class<? extends MarshallingDefMapper> value) {
			beanDefMapper = assertArgNotNull(ARG_value, value);
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public JsonSchemaGenerator build() {
			return cache(CACHE).build(JsonSchemaGenerator.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		/**
		 * Gives access to the inner JSON parser builder if you want to modify the parser settings.
		 *
		 * @return The JSON serializer builder.
		 */
		@SuppressWarnings({
			"java:S1452" // Builder<?> wildcard return intentional; callers chain via fluent API without needing the concrete type
		})
		public JsonParser.Builder<?> getJsonParserBuilder() { return jsonParserBuilder; }

		/**
		 * Gives access to the inner JSON serializer builder if you want to modify the serializer settings.
		 *
		 * @return The JSON serializer builder.
		 */
		@SuppressWarnings({
			"java:S1452" // Builder<?> wildcard return intentional; callers chain via fluent API without needing the concrete type
		})
		public JsonSerializer.Builder<?> getJsonSerializerBuilder() { return jsonSerializerBuilder; }

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				jsonSerializerBuilder.hashKey(),
				jsonParserBuilder.hashKey(),
				addDescriptionsTo,
				addExamplesTo,
				allowNestedDescriptions,
				allowNestedExamples,
				useBeanDefs,
				beanDefMapper,
				ignoreTypes
			);
			// @formatter:on
		}

		/**
		 * Ignore types from schema definitions.
		 *
		 * <h5 class='section'>Description:</h5>
		 * <p>
		 * Defines class name patterns that should be ignored when generating schema definitions in the generated
		 * Swagger documentation.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Don't generate schema for any prototype packages or the class named 'Swagger'.</jc>
		 * 	<ja>@JsonSchemaConfig</ja>(
		 * 		ignoreTypes=<js>"Swagger,*.proto.*"</js>
		 * 	)
		 * 	<jk>public class</jk> MyResource {...}
		 * </p>
		 *
		 * @param values
		 * 	The values to add.
		 * 	<br>Cannot contain <jk>null</jk> values.
		 * @return This object.
		 */
		public Builder ignoreTypes(String...values) {
			assertArgNoNulls(ARG_values, values);
			ignoreTypes = addAll(ignoreTypes, values);
			return this;
		}

		/**
		 * Use bean definitions.
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
		 * <p>
		 * The definitions can then be retrieved from the session using {@link JsonSchemaGeneratorSession#getBeanDefs()}.
		 * <p>
		 * Definitions can also be added programmatically using {@link JsonSchemaGeneratorSession#addBeanDef(String, JsonMap)}.
		 *
		 * @return This object.
		 */
		public Builder useBeanDefs() {
			return useBeanDefs(true);
		}

		/**
		 * Same as {@link #useBeanDefs()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder useBeanDefs(boolean value) {
			useBeanDefs = value;
			return this;
		}


	}

	/** Default serializer, all default settings.*/
	public static final JsonSchemaGenerator DEFAULT = new JsonSchemaGenerator(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	protected final boolean allowNestedDescriptions;
	protected final boolean allowNestedExamples;
	protected final boolean useBeanDefs;
	protected final Class<? extends MarshallingDefMapper> beanDefMapper;
	protected final JsonParser jsonParser;
	protected final JsonSerializer jsonSerializer;
	protected final Set<TypeCategory> addDescriptionsTo;
	protected final Set<TypeCategory> addExamplesTo;
	protected final Set<String> ignoreTypes;
	private final MarshallingDefMapper beanDefMapperBean;
	private final Map<BeanPropertyMeta,JsonSchemaBeanPropertyMeta> jsonSchemaBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,JsonSchemaClassMeta> jsonSchemaClassMetas = new ConcurrentHashMap<>();
	private final List<Pattern> ignoreTypePatterns;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonSchemaGenerator(Builder builder) {
		super(builder.detectRecursions().ignoreRecursions());

		addDescriptionsTo = builder.addDescriptionsTo == null ? Collections.emptySet() : new TreeSet<>(builder.addDescriptionsTo);
		addExamplesTo = builder.addExamplesTo == null ? Collections.emptySet() : new TreeSet<>(builder.addExamplesTo);
		allowNestedDescriptions = builder.allowNestedDescriptions;
		allowNestedExamples = builder.allowNestedExamples;
		beanDefMapper = builder.beanDefMapper;
		ignoreTypes = builder.ignoreTypes == null ? Collections.emptySet() : new TreeSet<>(builder.ignoreTypes);
		useBeanDefs = builder.useBeanDefs;

		Set<Pattern> ignoreTypePatterns2 = set();
		ignoreTypes.forEach(y -> split(y, x -> ignoreTypePatterns2.add(Pattern.compile(x.replace(".", "\\.").replace("*", ".*")))));
		this.ignoreTypePatterns = u(new ArrayList<>(ignoreTypePatterns2));

		try {
			beanDefMapperBean = beanDefMapper.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw toRex(e);
		}

		jsonSerializer = builder.jsonSerializerBuilder.build();
		jsonParser = builder.jsonParserBuilder.marshallingContext(getMarshallingContext()).build();
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public JsonSchemaGeneratorSession.Builder createSession() {
		return JsonSchemaGeneratorSession.create(this);
	}

	/**
	 * Ignore types from schema definitions.
	 *
	 * @see Builder#ignoreTypes(String...)
	 * @return
	 * 	Custom schema information for particular class types.
	 * 	<br>Never <jk>null</jk>.
	 * 	<br>List is unmodifiable.
	 */
	public List<Pattern> getIgnoreTypes() { return ignoreTypePatterns; }

	@Override
	public JsonSchemaBeanPropertyMeta getJsonSchemaBeanPropertyMeta(BeanPropertyMeta bpm) {
		return jsonSchemaBeanPropertyMetas.computeIfAbsent(bpm, k -> new JsonSchemaBeanPropertyMeta(k, this));
	}

	@Override
	public JsonSchemaClassMeta getJsonSchemaClassMeta(ClassMeta<?> cm) {
		return jsonSchemaClassMetas.computeIfAbsent(cm, k -> new JsonSchemaClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public JsonSchemaGeneratorSession getSession() { return createSession().build(); }

	/**
	 * Returns <jk>true</jk> if the specified type is ignored.
	 *
	 * <p>
	 * The type is ignored if it's specified in the {@link Builder#ignoreTypes(String...)} setting.
	 * <br>Ignored types return <jk>null</jk> on the call to {@link JsonSchemaGeneratorSession#getSchema(ClassMeta)}.
	 *
	 * @param cm The type to check.
	 * @return <jk>true</jk> if the specified type is ignored.
	 */
	public boolean isIgnoredType(ClassMeta<?> cm) {
		for (var p : ignoreTypePatterns)
			if (p.matcher(cm.getNameSimple()).matches() || p.matcher(cm.getName()).matches())
				return true;
		return false;
	}

	/**
	 * Add descriptions to types.
	 *
	 * @see Builder#addDescriptionsTo(TypeCategory...)
	 * @return
	 * 	Set of categories of types that descriptions should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddDescriptionsTo() { return addDescriptionsTo; }

	/**
	 * Add examples.
	 *
	 * @see Builder#addExamplesTo(TypeCategory...)
	 * @return
	 * 	Set of categories of types that examples should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddExamplesTo() { return addExamplesTo; }

	/**
	 * Bean schema definition mapper.
	 *
	 * @see Builder#beanDefMapper(Class)
	 * @return
	 * 	Interface to use for converting Bean classes to definition IDs and URIs.
	 */
	protected final MarshallingDefMapper getBeanDefMapper() { return beanDefMapperBean; }

	/**
	 * Allow nested descriptions.
	 *
	 * @see Builder#allowNestedDescriptions()
	 * @return
	 * 	<jk>true</jk> if nested descriptions are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedDescriptions() { return allowNestedDescriptions; }

	/**
	 * Allow nested examples.
	 *
	 * @see Builder#allowNestedExamples()
	 * @return
	 * 	<jk>true</jk> if nested examples are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedExamples() { return allowNestedExamples; }

	/**
	 * Use bean definitions.
	 *
	 * @see Builder#useBeanDefs()
	 * @return
	 * 	<jk>true</jk> if schemas on beans will be serialized with <js>'$ref'</js> tags.
	 */
	protected final boolean isUseBeanDefs() { return useBeanDefs; }

	@Override /* Overridden from MarshallingTraverseContext */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addDescriptionsTo, addDescriptionsTo)
			.a(PROP_addExamplesTo, addExamplesTo)
			.a(PROP_allowNestedDescriptions, allowNestedDescriptions)
			.a(PROP_allowNestedExamples, allowNestedExamples)
			.a(PROP_beanDefMapper, beanDefMapper)
			.a(PROP_ignoreTypes, ignoreTypes)
			.a(PROP_useBeanDefs, useBeanDefs);
	}

	JsonParser getJsonParser() { return jsonParser; }

	JsonSerializer getJsonSerializer() { return jsonSerializer; }
}