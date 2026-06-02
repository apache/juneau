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

import org.apache.juneau.swap.ObjectSwap;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.json5.Json5Serializer;
import org.apache.juneau.serializer.*;
import org.apache.juneau.commons.bean.BeanPropertyMeta;

/**
 * Serializes POJO models to RFC 8259 JSON.
 *
 * <p>
 * Produces strict JSON with double-quoted strings by default. For JSON5-style output (single quotes,
 * etc.), use {@link Json5Serializer}.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>application/json, text/json</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/json</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * The conversion is as follows...
 * <ul class='spaced-list'>
 * 	<li>
 * 		Maps (e.g. {@link HashMap HashMaps}, {@link TreeMap TreeMaps}) are converted to JSON objects.
 * 	<li>
 * 		Collections (e.g. {@link HashSet HashSets}, {@link LinkedList LinkedLists}) and Java arrays are converted to
 * 		JSON arrays.
 * 	<li>
 * 		{@link String Strings} are converted to JSON strings.
 * 	<li>
 * 		{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to JSON numbers.
 * 	<li>
 * 		{@link Boolean Booleans} are converted to JSON booleans.
 * 	<li>
 * 		{@code nulls} are converted to JSON nulls.
 * 	<li>
 * 		{@code arrays} are converted to JSON arrays.
 * 	<li>
 * 		{@code beans} are converted to JSON objects.
 * </ul>
 *
 * <p>
 * The types above are considered "JSON-primitive" object types.
 * Any non-JSON-primitive object types are transformed into JSON-primitive object types through
 * {@link ObjectSwap ObjectSwaps} associated through the
 * {@link org.apache.juneau.MarshallingContext.Builder<?>#swaps(Class...)} method.
 * Several default transforms are provided for transforming Dates, Enums, Iterators, etc...
 *
 * <p>
 * This serializer provides several serialization options.
 * Typically, one of the predefined DEFAULT serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 * <p>
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link Json5Serializer} - Default serializer, single quotes, simple mode.
 * 	<li>
 * 		{@link org.apache.juneau.json5.Json5Serializer.Readable} - Default serializer, single quotes, simple mode, with whitespace.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a custom serializer for lax syntax using single quote characters</jc>
 * 	JsonSerializer <jv>serializer</jv> = JsonSerializer.<jsm>create</jsm>().simple().sq().build();
 *
 * 	<jc>// Clone an existing serializer and modify it to use single-quotes</jc>
 * 	<jv>serializer</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.copy().sq().build();
 *
 * 	<jc>// Serialize a POJO to JSON</jc>
 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jv>someObject</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bjson'>
 * 	{<js>"name"</js>:<js>"Alice"</js>,<js>"age"</js>:30}
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bjson'>
 * 	{<js>"name"</js>:<js>"Alice"</js>,<js>"age"</js>:30,<js>"address"</js>:{<js>"street"</js>:<js>"123 Main St"</js>,<js>"city"</js>:<js>"Boston"</js>,<js>"state"</js>:<js>"MA"</js>},<js>"tags"</js>:[<js>"a"</js>,<js>"b"</js>,<js>"c"</js>]}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonBasics">JSON Basics</a>
 * 	<li class='link'>{@link Json5Serializer} - For JSON5-style output (single quotes, etc.)
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class JsonSerializer extends WriterSerializer implements JsonMetaProvider {

	// Property name constants
	private static final String PROP_addBeanTypesJson = "addBeanTypesJson";
	private static final String PROP_escapeSolidus = "escapeSolidus";

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public abstract static class Builder<SELF extends Builder<SELF>> extends WriterSerializer.Builder<SELF> {

		private static final Cache<HashKey,JsonSerializer> CACHE = Cache.of(HashKey.class, JsonSerializer.class).build();

		private boolean addBeanTypesJson;
		private boolean escapeSolidus;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/json");
			accept("application/json,text/json");
			addBeanTypesJson = env("JsonSerializer.addBeanTypes", false);
			escapeSolidus = env("JsonSerializer.escapeSolidus", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesJson = copyFrom.addBeanTypesJson;
			escapeSolidus = copyFrom.escapeSolidus;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(JsonSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesJson = copyFrom.addBeanTypesJson;
			escapeSolidus = copyFrom.escapeSolidus;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * <p>
		 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
		 * through reflection.
		 *
		 * <p>
		 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder<?>#addBeanTypes()} setting and is
		 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
		 *
		 * @return This object.
		 */
		public SELF addBeanTypesJson() {
			return addBeanTypesJson(true);
		}

		/**
		 * Same as {@link #addBeanTypesJson()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF addBeanTypesJson(boolean value) {
			addBeanTypesJson = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public JsonSerializer build() {
			return cache(CACHE).build(JsonSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();

		/**
		 * Prefix solidus <js>'/'</js> characters with escapes.
		 *
		 * <p>
		 * If enabled, solidus (e.g. slash) characters should be escaped.
		 *
		 * <p>
		 * The JSON specification allows for either format.
		 * <br>However, if you're embedding JSON in an HTML script tag, this setting prevents confusion when trying to serialize
		 * <xt>&lt;\/script&gt;</xt>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a JSON serializer that escapes solidus characters.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.simple()
		 * 		.escapeSolidus()
		 * 		.build();
		 *
		 * 	<jc>// Produces: "{foo:'&lt;\/bar&gt;'"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(JsonMap.<jsm>of</jsm>(<js>"foo"</js>, <js>"&lt;/bar&gt;"</js>);
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF escapeSolidus() {
			return escapeSolidus(true);
		}

		/**
		 * Same as {@link #escapeSolidus()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF escapeSolidus(boolean value) {
			escapeSolidus = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				addBeanTypesJson,
				escapeSolidus
			);
			// @formatter:on
		}


	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link JsonSerializer#create()} / {@link JsonSerializer#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(JsonSerializer copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(Builder<?> builder) {
			super(builder.useWhitespace());
		}
	}

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT = new JsonSerializer(create());

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT_READABLE = new Readable(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

	protected final boolean addBeanTypesJson;
	protected final boolean escapeSolidus;

	private final boolean addBeanTypes2;
	private final Map<BeanPropertyMeta,JsonBeanPropertyMeta> jsonBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,JsonClassMeta> jsonClassMetas = new ConcurrentHashMap<>();

	private final AtomicReference<JsonSchemaSerializer> schemaSerializer = new AtomicReference<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonSerializer(Builder<?> builder) {
		super(builder);
		addBeanTypesJson = builder.addBeanTypesJson;
		escapeSolidus = builder.escapeSolidus;

		addBeanTypes2 = addBeanTypesJson || super.isAddBeanTypes();
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public JsonSerializerSession.Builder<?> createSession() {
		return JsonSerializerSession.create(this);
	}

	@Override /* Overridden from JsonMetaProvider */
	public JsonBeanPropertyMeta getJsonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return JsonBeanPropertyMeta.DEFAULT;
		return jsonBeanPropertyMetas.computeIfAbsent(bpm, k -> new JsonBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from JsonMetaProvider */
	public JsonClassMeta getJsonClassMeta(ClassMeta<?> cm) {
		return jsonClassMetas.computeIfAbsent(cm, k -> new JsonClassMeta(k, this));
	}

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return The schema serializer.
	 */
	public JsonSchemaSerializer getSchemaSerializer() {
		JsonSchemaSerializer result = schemaSerializer.get();
		if (result == null) {
			result = JsonSchemaSerializer.create().marshallingContext(getMarshallingContext()).build();
			if (! schemaSerializer.compareAndSet(null, result)) {
				result = schemaSerializer.get();
			}
		}
		return result;
	}

	@Override /* Overridden from Context */
	public JsonSerializerSession getSession() { return createSession().build(); }

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Builder#addBeanTypesJson()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() { return addBeanTypes2; }

	/**
	 * Prefix solidus <js>'/'</js> characters with escapes.
	 *
	 * @see Builder#escapeSolidus()
	 * @return
	 * 	<jk>true</jk> if solidus (e.g. slash) characters should be escaped.
	 */
	protected final boolean isEscapeSolidus() { return escapeSolidus; }

	@Override /* Overridden from WriterSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addBeanTypesJson, addBeanTypesJson)
			.a(PROP_escapeSolidus, escapeSolidus);
	}
}