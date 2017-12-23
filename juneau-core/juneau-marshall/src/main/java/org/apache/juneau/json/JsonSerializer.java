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

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to JSON.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Accept</code> types: <code>application/json, text/json</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>application/json</code>
 *
 * <h5 class='section'>Description:</h5>
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
 * {@link BeanContextBuilder#pojoSwaps(Class...)} method.
 * Several default transforms are provided for transforming Dates, Enums, Iterators, etc...
 *
 * <p>
 * This serializer provides several serialization options.
 * Typically, one of the predefined DEFAULT serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 *
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link Simple} - Default serializer, single quotes, simple mode.
 * 	<li>
 * 		{@link SimpleReadable} - Default serializer, single quotes, simple mode, with whitespace.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(someObject);
 *
 * 	<jc>// Create a custom serializer for lax syntax using single quote characters</jc>
 * 	JsonSerializer serializer = <jk>new</jk> JsonSerializerBuilder().simple().sq().build();
 *
 * 	<jc>// Clone an existing serializer and modify it to use single-quotes</jc>
 * 	JsonSerializer serializer = JsonSerializer.<jsf>DEFAULT</jsf>.builder().sq().build();
 *
 * 	<jc>// Serialize a POJO to JSON</jc>
 * 	String json = serializer.serialize(someObject);
 * </p>
 */
public class JsonSerializer extends WriterSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "JsonSerializer.";

	/**
	 * <b>Configuration property:</b>  Simple JSON mode.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"JsonSerializer.simpleMode.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>true</jk>, JSON attribute names will only be quoted when necessary.
	 * Otherwise, they are always quoted.
	 */
	public static final String JSON_simpleMode = PREFIX + "simpleMode.b";

	/**
	 * <b>Configuration property:</b>  Prefix solidus <js>'/'</js> characters with escapes.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"JsonSerializer.escapeSolidus.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>true</jk>, solidus (e.g. slash) characters should be escaped.
	 * The JSON specification allows for either format.
	 * However, if you're embedding JSON in an HTML script tag, this setting prevents confusion when trying to serialize
	 * <xt>&lt;\/script&gt;</xt>.
	 */
	public static final String JSON_escapeSolidus = PREFIX + "escapeSolidus.b";

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"JsonSerializer.addBeanTypeProperties.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined from
	 * the value type.
	 *
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypeProperties} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String JSON_addBeanTypeProperties = PREFIX + "addBeanTypeProperties.b";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT = new JsonSerializer(PropertyStore.DEFAULT);

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT_READABLE = new Readable(PropertyStore.DEFAULT);

	/** Default serializer, single quotes, simple mode. */
	public static final JsonSerializer DEFAULT_LAX = new Simple(PropertyStore.DEFAULT);

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final JsonSerializer DEFAULT_LAX_READABLE = new SimpleReadable(PropertyStore.DEFAULT);

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static final JsonSerializer DEFAULT_LAX_READABLE_SAFE = new SimpleReadableSafe(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Readable(PropertyStore ps) {
			super(
				ps.builder().set(SERIALIZER_useWhitespace, true).build()
			);
		}
	}

	/** Default serializer, single quotes, simple mode. */
	public static class Simple extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Simple(PropertyStore ps) {
			super(
				ps.builder()
					.set(JSON_simpleMode, true)
					.set(SERIALIZER_quoteChar, '\'')
					.build(),
				"application/json",
				"application/json+simple", "application/json+simple+*", "text/json+simple", "text/json+simple+*"
			);
		}
	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class SimpleReadable extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public SimpleReadable(PropertyStore ps) {
			super(
				ps.builder()
					.set(JSON_simpleMode, true)
					.set(SERIALIZER_quoteChar, '\'')
					.set(SERIALIZER_useWhitespace, true)
					.build()
			);
		}
	}

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static class SimpleReadableSafe extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public SimpleReadableSafe(PropertyStore ps) {
			super(
				ps.builder()
					.set(JSON_simpleMode, true)
					.set(SERIALIZER_quoteChar, '\'')
					.set(SERIALIZER_useWhitespace, true)
					.set(SERIALIZER_detectRecursions, true)
					.build()
			);
		}
	}


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean
		simpleMode,
		escapeSolidus,
		addBeanTypeProperties;

	private volatile JsonSchemaSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public JsonSerializer(PropertyStore ps) {
		this(ps, "application/json", "application/json", "application/json+*", "text/json", "text/json+*");
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	<a class="doclink" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">RFC2616/14.1</a>
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <code>produces</code>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"application/json"</js>, <js>"text/json"</js>);</code>
	 * 	<br>...or...
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);</code>
	 */
	public JsonSerializer(PropertyStore ps, String produces, String...accept) {
		super(ps, produces, accept);
		
		simpleMode = getProperty(JSON_simpleMode, boolean.class, false);
		escapeSolidus = getProperty(JSON_escapeSolidus, boolean.class, false);
		addBeanTypeProperties = getProperty(JSON_addBeanTypeProperties, boolean.class, getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true));
	}

	@Override /* Context */
	public JsonSerializerBuilder builder() {
		return new JsonSerializerBuilder(getPropertyStore());
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
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies 
	 * the settings of the object called on.
	 *
	 * @return The schema serializer.
	 */
	public JsonSchemaSerializer getSchemaSerializer() {
		if (schemaSerializer == null)
			schemaSerializer = builder().build(JsonSchemaSerializer.class);
		return schemaSerializer;
	}

	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new JsonSerializerSession(this, args);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("JsonSerializer", new ObjectMap()
				.append("simpleMode", simpleMode)
				.append("escapeSolidus", escapeSolidus)
				.append("addBeanTypeProperties", addBeanTypeProperties)
			);
	}
}