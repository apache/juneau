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
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to JSON.
 * {@review}
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>application/json, text/json</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/json</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
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
 * {@link org.apache.juneau.transform.PojoSwap PojoSwaps} associated through the
 * {@link BeanContextBuilder#swaps(Class...)} method.
 * Several default transforms are provided for transforming Dates, Enums, Iterators, etc...
 *
 * <p>
 * This serializer provides several serialization options.
 * Typically, one of the predefined DEFAULT serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 *
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link SimpleJsonSerializer} - Default serializer, single quotes, simple mode.
 * 	<li>
 * 		{@link SimpleJsonSerializer.Readable} - Default serializer, single quotes, simple mode, with whitespace.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(someObject);
 *
 * 	<jc>// Create a custom serializer for lax syntax using single quote characters</jc>
 * 	JsonSerializer serializer = JsonSerializer.<jsm>create</jsm>().simple().sq().build();
 *
 * 	<jc>// Clone an existing serializer and modify it to use single-quotes</jc>
 * 	JsonSerializer serializer = JsonSerializer.<jsf>DEFAULT</jsf>.copy().sq().build();
 *
 * 	<jc>// Serialize a POJO to JSON</jc>
 * 	String json = serializer.serialize(someObject);
 * </p>
 */
@ConfigurableContext
public class JsonSerializer extends WriterSerializer implements JsonMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT = new JsonSerializer(create());

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT_READABLE = new Readable(create());

	//-------------------------------------------------------------------------------------------------------------------
	// Static subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected Readable(JsonSerializerBuilder builder) {
			super(builder.useWhitespace());
		}
	}

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static class ReadableSafe extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected ReadableSafe(JsonSerializerBuilder builder) {
			super(builder.simpleMode().quoteChar('\'').useWhitespace().detectRecursions());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean addBeanTypesJson, escapeSolidus, simpleMode;

	private final boolean addBeanTypes;
	private final Map<ClassMeta<?>,JsonClassMeta> jsonClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonBeanPropertyMeta> jsonBeanPropertyMetas = new ConcurrentHashMap<>();

	private volatile JsonSchemaSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JsonSerializer(JsonSerializerBuilder builder) {
		super(builder);
		addBeanTypesJson = builder.addBeanTypesJson;
		simpleMode = builder.simpleMode;
		escapeSolidus = builder.escapeSolidus;

		addBeanTypes = addBeanTypesJson || super.isAddBeanTypes();
	}

	@Override /* Context */
	public JsonSerializerBuilder copy() {
		return new JsonSerializerBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link JsonSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> JsonSerializerBuilder()</code>.
	 *
	 * @return A new {@link JsonSerializerBuilder} object.
	 */
	public static JsonSerializerBuilder create() {
		return new JsonSerializerBuilder();
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
		if (schemaSerializer == null)
			schemaSerializer = JsonSchemaSerializer.create().apply(getContextProperties()).build();
		return schemaSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Entry point methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public JsonSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public JsonSerializerSession createSession(SerializerSessionArgs args) {
		return new JsonSerializerSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* JsonMetaProvider */
	public JsonClassMeta getJsonClassMeta(ClassMeta<?> cm) {
		JsonClassMeta m = jsonClassMetas.get(cm);
		if (m == null) {
			m = new JsonClassMeta(cm, this);
			jsonClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* JsonMetaProvider */
	public JsonBeanPropertyMeta getJsonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return JsonBeanPropertyMeta.DEFAULT;
		JsonBeanPropertyMeta m = jsonBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new JsonBeanPropertyMeta(bpm.getDelegateFor(), this);
			jsonBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see JsonSerializerBuilder#addBeanTypesJson()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return addBeanTypes;
	}

	/**
	 * Prefix solidus <js>'/'</js> characters with escapes.
	 *
	 * @see JsonSerializerBuilder#escapeSolidus()
	 * @return
	 * 	<jk>true</jk> if solidus (e.g. slash) characters should be escaped.
	 */
	protected final boolean isEscapeSolidus() {
		return escapeSolidus;
	}

	/**
	 * Simple JSON mode.
	 *
	 * @see JsonSerializerBuilder#simpleMode()
	 * @return
	 * 	<jk>true</jk> if JSON attribute names will only be quoted when necessary.
	 * 	<br>Otherwise, they are always quoted.
	 */
	protected final boolean isSimpleMode() {
		return simpleMode;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"JsonSerializer",
				OMap
					.create()
					.filtered()
					.a("simpleMode", simpleMode)
					.a("escapeSolidus", escapeSolidus)
					.a("addBeanTypesJson", addBeanTypesJson)
			);
	}
}