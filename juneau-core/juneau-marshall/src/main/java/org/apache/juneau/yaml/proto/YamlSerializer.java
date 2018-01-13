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
package org.apache.juneau.yaml.proto;

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
public class YamlSerializer extends WriterSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "YamlSerializer.";

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"JsonSerializer.addBeanTypeProperties.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link YamlSerializerBuilder#addBeanTypeProperties(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
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
	public static final String YAML_addBeanTypeProperties = PREFIX + "addBeanTypeProperties.b";

	/**
	 * Configuration property:  Prefix solidus <js>'/'</js> characters with escapes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"JsonSerializer.escapeSolidus.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link YamlSerializerBuilder#escapeSolidus(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, solidus (e.g. slash) characters should be escaped.
	 * The JSON specification allows for either format.
	 * However, if you're embedding JSON in an HTML script tag, this setting prevents confusion when trying to serialize
	 * <xt>&lt;\/script&gt;</xt>.
	 */
	public static final String YAML_escapeSolidus = PREFIX + "escapeSolidus.b";

	/**
	 * Configuration property:  Simple JSON mode.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"JsonSerializer.simpleMode.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link YamlSerializerBuilder#simple()}
	 * 			<li class='jm'>{@link YamlSerializerBuilder#simple(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, JSON attribute names will only be quoted when necessary.
	 * Otherwise, they are always quoted.
	 */
	public static final String YAML_simpleMode = PREFIX + "simpleMode.b";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final YamlSerializer DEFAULT = new YamlSerializer(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean
		simpleMode,
		escapeSolidus,
		addBeanTypeProperties;

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public YamlSerializer(PropertyStore ps) {
		this(ps, "application/yaml", "application/yaml", "application/yaml+*", "text/yaml", "text/yaml+*");
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
	public YamlSerializer(PropertyStore ps, String produces, String...accept) {
		super(ps, produces, accept);
		simpleMode = getProperty(YAML_simpleMode, boolean.class, false);
		escapeSolidus = getProperty(YAML_escapeSolidus, boolean.class, false);
		addBeanTypeProperties = getProperty(YAML_addBeanTypeProperties, boolean.class, getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true));
	}

	@Override /* Context */
	public YamlSerializerBuilder builder() {
		return new YamlSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link YamlSerializerBuilder} object.
	 * 
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> YamlSerializerBuilder()</code>.
	 * 
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies 
	 * the settings of the object called on.
	 * 
	 * @return A new {@link YamlSerializerBuilder} object.
	 */
	public static YamlSerializerBuilder create() {
		return new YamlSerializerBuilder();
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new YamlSerializerSession(this, args);
	}
	
	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("YamlSerializer", new ObjectMap()
				.append("simpleMode", simpleMode)
				.append("escapeSolidus", escapeSolidus)
				.append("addBeanTypeProperties", addBeanTypeProperties)
			);
	}
}