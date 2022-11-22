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
package org.apache.juneau.objecttools;

import static java.net.HttpURLConnection.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;

/**
 * POJO REST API.
 *
 * <p>
 * Provides the ability to perform standard REST operations (GET, PUT, POST, DELETE) against nodes in a POJO model.
 * Nodes in the POJO model are addressed using URLs.
 *
 * <p>
 * A POJO model is defined as a tree model where nodes consist of consisting of the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link Map Maps} and Java beans representing JSON objects.
 * 	<li>
 * 		{@link Collection Collections} and arrays representing JSON arrays.
 * 	<li>
 * 		Java beans.
 * </ul>
 *
 * <p>
 * Leaves of the tree can be any type of object.
 *
 * <p>
 * Use {@link #get(String) get()} to retrieve an element from a JSON tree.
 * <br>Use {@link #put(String,Object) put()} to create (or overwrite) an element in a JSON tree.
 * <br>Use {@link #post(String,Object) post()} to add an element to a list in a JSON tree.
 * <br>Use {@link #delete(String) delete()} to remove an element from a JSON tree.
 *
 * <p>
 * Leading slashes in URLs are ignored.
 * So <js>"/xxx/yyy/zzz"</js> and <js>"xxx/yyy/zzz"</js> are considered identical.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct an unstructured POJO model</jc>
 * 	JsonMap <jv>map</jv> = JsonMap.<jsm>ofJson</jsm>(<js>""</js>
 * 		+ <js>"{"</js>
 * 		+ <js>"	name:'John Smith', "</js>
 * 		+ <js>"	address:{ "</js>
 * 		+ <js>"		streetAddress:'21 2nd Street', "</js>
 * 		+ <js>"		city:'New York', "</js>
 * 		+ <js>"		state:'NY', "</js>
 * 		+ <js>"		postalCode:10021 "</js>
 * 		+ <js>"	}, "</js>
 * 		+ <js>"	phoneNumbers:[ "</js>
 * 		+ <js>"		'212 555-1111', "</js>
 * 		+ <js>"		'212 555-2222' "</js>
 * 		+ <js>"	], "</js>
 * 		+ <js>"	additionalInfo:null, "</js>
 * 		+ <js>"	remote:false, "</js>
 * 		+ <js>"	height:62.4, "</js>
 * 		+ <js>"	'fico score':' &gt; 640' "</js>
 * 		+ <js>"} "</js>
 * 	);
 *
 * 	<jc>// Wrap Map inside an ObjectRest object</jc>
 * 	ObjectRest <jv>johnSmith</jv> = ObjectRest.<jsm>create</jsm>(<jv>map</jv>);
 *
 * 	<jc>// Get a simple value at the top level</jc>
 * 	<jc>// "John Smith"</jc>
 * 	String <jv>name</jv> = <jv>johnSmith</jv>.getString(<js>"name"</js>);
 *
 * 	<jc>// Change a simple value at the top level</jc>
 * 	<jv>johnSmith</jv>.put(<js>"name"</js>, <js>"The late John Smith"</js>);
 *
 * 	<jc>// Get a simple value at a deep level</jc>
 * 	<jc>// "21 2nd Street"</jc>
 * 	String <jv>streetAddress</jv> = <jv>johnSmith</jv>.getString(<js>"address/streetAddress"</js>);
 *
 * 	<jc>// Set a simple value at a deep level</jc>
 * 	<jv>johnSmith</jv>.put(<js>"address/streetAddress"</js>, <js>"101 Cemetery Way"</js>);
 *
 * 	<jc>// Get entries in a list</jc>
 * 	<jc>// "212 555-1111"</jc>
 * 	String <jv>firstPhoneNumber</jv> = <jv>johnSmith</jv>.getString(<js>"phoneNumbers/0"</js>);
 *
 * 	<jc>// Add entries to a list</jc>
 * 	<jv>johnSmith</jv>.post(<js>"phoneNumbers"</js>, <js>"212 555-3333"</js>);
 *
 * 	<jc>// Delete entries from a model</jc>
 * 	<jv>johnSmith</jv>.delete(<js>"fico score"</js>);
 *
 * 	<jc>// Add entirely new structures to the tree</jc>
 * 	JsonMap <jv>medicalInfo</jv> = JsonMap.<jsm>ofJson</jsm>(<js>""</js>
 * 		+ <js>"{"</js>
 * 		+ <js>"	currentStatus: 'deceased',"</js>
 * 		+ <js>"	health: 'non-existent',"</js>
 * 		+ <js>"	creditWorthiness: 'not good'"</js>
 * 		+ <js>"}"</js>
 * 	);
 * 	<jv>johnSmith</jv>.put(<js>"additionalInfo/medicalInfo"</js>, <jv>medicalInfo</jv>);
 * </p>
 *
 * <p>
 * In the special case of collections/arrays of maps/beans, a special XPath-like selector notation can be used in lieu
 * of index numbers on GET requests to return a map/bean with a specified attribute value.
 * <br>The syntax is {@code @attr=val}, where attr is the attribute name on the child map, and val is the matching value.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Get map/bean with name attribute value of 'foo' from a list of items</jc>
 * 	Map <jv>map</jv> = <jv>objectRest</jv>.getMap(<js>"/items/@name=foo"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@SuppressWarnings({"unchecked","rawtypes"})
public final class ObjectRest {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** The list of possible request types. */
	private static final int GET=1, PUT=2, POST=3, DELETE=4;

	/**
	 * Static creator.
	 * @param o The object being wrapped.
	 * @return A new {@link ObjectRest} object.
	 */
	public static ObjectRest create(Object o) {
		return new ObjectRest(o);
	}

	/**
	 * Static creator.
	 * @param o The object being wrapped.
	 * @param parser The parser to use for parsing arguments and converting objects to the correct data type.
	 * @return A new {@link ObjectRest} object.
	 */
	public static ObjectRest create(Object o, ReaderParser parser) {
		return new ObjectRest(o, parser);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private ReaderParser parser = JsonParser.DEFAULT;
	final BeanSession session;

	/** If true, the root cannot be overwritten */
	private boolean rootLocked = false;

	/** The root of the model. */
	private JsonNode root;

	/**
	 * Create a new instance of a REST interface over the specified object.
	 *
	 * <p>
	 * Uses {@link BeanContext#DEFAULT} for working with Java beans.
	 *
	 * @param o The object to be wrapped.
	 */
	public ObjectRest(Object o) {
		this(o, null);
	}

	/**
	 * Create a new instance of a REST interface over the specified object.
	 *
	 * <p>
	 * The parser is used as the bean context.
	 *
	 * @param o The object to be wrapped.
	 * @param parser The parser to use for parsing arguments and converting objects to the correct data type.
	 */
	public ObjectRest(Object o, ReaderParser parser) {
		this.session = parser == null ? BeanContext.DEFAULT_SESSION : parser.getBeanContext().getSession();
		if (parser == null)
			parser = JsonParser.DEFAULT;
		this.parser = parser;
		this.root = new JsonNode(null, null, o, session.object());
	}

	/**
	 * Call this method to prevent the root object from being overwritten on <c>put("", xxx);</c> calls.
	 *
	 * @return This object.
	 */
	public ObjectRest setRootLocked() {
		this.rootLocked = true;
		return this;
	}

	/**
	 * The root object that was passed into the constructor of this method.
	 *
	 * @return The root object.
	 */
	public Object getRootObject() {
		return root.o;
	}

	/**
	 * Retrieves the element addressed by the URL.
	 *
	 * @param url
	 * 	The URL of the element to retrieve.
	 * 	<br>If <jk>null</jk> or blank, returns the root.
	 * @return The addressed element, or <jk>null</jk> if that element does not exist in the tree.
	 */
	public Object get(String url) {
		return getWithDefault(url, null);
	}

	/**
	 * Retrieves the element addressed by the URL.
	 *
	 * @param url
	 * 	The URL of the element to retrieve.
	 * 	<br>If <jk>null</jk> or blank, returns the root.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The addressed element, or null if that element does not exist in the tree.
	 */
	public Object getWithDefault(String url, Object defVal) {
		Object o = service(GET, url, null);
		return o == null ? defVal : o;
	}

	/**
	 * Retrieves the element addressed by the URL as the specified object type.
	 *
	 * <p>
	 * Will convert object to the specified type per {@link BeanSession#convertToType(Object, Class)}.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	ObjectRest <jv>objectRest</jv> = <jk>new</jk> ObjectRest(<jv>object</jv>);
	 *
	 * 	<jc>// Value converted to a string.</jc>
	 * 	String <jv>string</jv> = <jv>objectRest</jv>.get(<js>"path/to/string"</js>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a bean.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>objectRest</jv>.get(<js>"path/to/bean"</js>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a bean array.</jc>
	 * 	MyBean[] <jv>beanArray</jv> = <jv>objectRest</jv>.get(<js>"path/to/beanarray"</js>, MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a linked-list of objects.</jc>
	 * 	List <jv>list</jv> = <jv>objectRest</jv>.get(<js>"path/to/list"</js>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a map of object keys/values.</jc>
	 * 	Map <jv>map</jv> = <jv>objectRest</jv>.get(<js>"path/to/map"</js>, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param url
	 * 	The URL of the element to retrieve.
	 * 	If <jk>null</jk> or blank, returns the root.
	 * @param type The specified object type.
	 *
	 * @param <T> The specified object type.
	 * @return The addressed element, or null if that element does not exist in the tree.
	 */
	public <T> T get(String url, Class<T> type) {
		return getWithDefault(url, null, type);
	}

	/**
	 * Retrieves the element addressed by the URL as the specified object type.
	 *
	 * <p>
	 * Will convert object to the specified type per {@link BeanSession#convertToType(Object, Class)}.
	 *
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	ObjectRest <jv>objectRest</jv> = <jk>new</jk> ObjectRest(<jv>object</jv>);
	 *
	 * 	<jc>// Value converted to a linked-list of strings.</jc>
	 * 	List&lt;String&gt; <jv>list1</jv> = <jv>objectRest</jv>.get(<js>"path/to/list1"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a linked-list of beans.</jc>
	 * 	List&lt;MyBean&gt; <jv>list2</jv> = <jv>objectRest</jv>.get(<js>"path/to/list2"</js>, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; <jv>list3</jv> = <jv>objectRest</jv>.get(<js>"path/to/list3"</js>, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; <jv>map1</jv> = <jv>objectRest</jv>.get(<js>"path/to/map1"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; <jv>map2</jv> = <jv>objectRest</jv>.get(<js>"path/to/map2"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * <c>Collection</c> classes are assumed to be followed by zero or one objects indicating the element type.
	 *
	 * <p>
	 * <c>Map</c> classes are assumed to be followed by zero or two meta objects indicating the key and value types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Use the {@link #get(String, Class)} method instead if you don't need a parameterized map/collection.
	 * </ul>
	 *
	 * @param url
	 * 	The URL of the element to retrieve.
	 * 	If <jk>null</jk> or blank, returns the root.
	 * @param type The specified object type.
	 * @param args The specified object parameter types.
	 *
	 * @param <T> The specified object type.
	 * @return The addressed element, or null if that element does not exist in the tree.
	 */
	public <T> T get(String url, Type type, Type...args) {
		return getWithDefault(url, null, type, args);
	}

	/**
	 * Same as {@link #get(String, Class)} but returns a default value if the addressed element is null or non-existent.
	 *
	 * @param url
	 * 	The URL of the element to retrieve.
	 * 	If <jk>null</jk> or blank, returns the root.
	 * @param def The default value if addressed item does not exist.
	 * @param type The specified object type.
	 *
	 * @param <T> The specified object type.
	 * @return The addressed element, or null if that element does not exist in the tree.
	 */
	public <T> T getWithDefault(String url, T def, Class<T> type) {
		Object o = service(GET, url, null);
		if (o == null)
			return def;
		return session.convertToType(o, type);
	}

	/**
	 * Same as {@link #get(String,Type,Type[])} but returns a default value if the addressed element is null or non-existent.
	 *
	 * @param url
	 * 	The URL of the element to retrieve.
	 * 	If <jk>null</jk> or blank, returns the root.
	 * @param def The default value if addressed item does not exist.
	 * @param type The specified object type.
	 * @param args The specified object parameter types.
	 *
	 * @param <T> The specified object type.
	 * @return The addressed element, or null if that element does not exist in the tree.
	 */
	public <T> T getWithDefault(String url, T def, Type type, Type...args) {
		Object o = service(GET, url, null);
		if (o == null)
			return def;
		return session.convertToType(o, type, args);
	}

	/**
	 * Returns the specified entry value converted to a {@link String}.
	 *
	 * <p>
	 * Shortcut for <code>get(String.<jk>class</jk>, key)</code>.
	 *
	 * @param url The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 */
	public String getString(String url) {
		return get(url, String.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link String}.
	 *
	 * <p>
	 * Shortcut for <code>get(String.<jk>class</jk>, key, defVal)</code>.
	 *
	 * @param url The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 */
	public String getString(String url, String defVal) {
		return getWithDefault(url, defVal, String.class);
	}

	/**
	 * Returns the specified entry value converted to an {@link Integer}.
	 *
	 * <p>
	 * Shortcut for <code>get(Integer.<jk>class</jk>, key)</code>.
	 *
	 * @param url The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Integer getInt(String url) {
		return get(url, Integer.class);
	}

	/**
	 * Returns the specified entry value converted to an {@link Integer}.
	 *
	 * <p>
	 * Shortcut for <code>get(Integer.<jk>class</jk>, key, defVal)</code>.
	 *
	 * @param url The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Integer getInt(String url, Integer defVal) {
		return getWithDefault(url, defVal, Integer.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Long}.
	 *
	 * <p>
	 * Shortcut for <code>get(Long.<jk>class</jk>, key)</code>.
	 *
	 * @param url The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Long getLong(String url) {
		return get(url, Long.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Long}.
	 *
	 * <p>
	 * Shortcut for <code>get(Long.<jk>class</jk>, key, defVal)</code>.
	 *
	 * @param url The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Long getLong(String url, Long defVal) {
		return getWithDefault(url, defVal, Long.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Boolean}.
	 *
	 * <p>
	 * Shortcut for <code>get(Boolean.<jk>class</jk>, key)</code>.
	 *
	 * @param url The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Boolean getBoolean(String url) {
		return get(url, Boolean.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Boolean}.
	 *
	 * <p>
	 * Shortcut for <code>get(Boolean.<jk>class</jk>, key, defVal)</code>.
	 *
	 * @param url The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Boolean getBoolean(String url, Boolean defVal) {
		return getWithDefault(url, defVal, Boolean.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Map}.
	 *
	 * <p>
	 * Shortcut for <code>get(Map.<jk>class</jk>, key)</code>.
	 *
	 * @param url The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Map<?,?> getMap(String url) {
		return get(url, Map.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Map}.
	 *
	 * <p>
	 * Shortcut for <code>get(Map.<jk>class</jk>, key, defVal)</code>.
	 *
	 * @param url The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Map<?,?> getMap(String url, Map<?,?> defVal) {
		return getWithDefault(url, defVal, Map.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link List}.
	 *
	 * <p>
	 * Shortcut for <code>get(List.<jk>class</jk>, key)</code>.
	 *
	 * @param url The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public List<?> getList(String url) {
		return get(url, List.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link List}.
	 *
	 * <p>
	 * Shortcut for <code>get(List.<jk>class</jk>, key, defVal)</code>.
	 *
	 * @param url The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public List<?> getList(String url, List<?> defVal) {
		return getWithDefault(url, defVal, List.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Map}.
	 *
	 * <p>
	 * Shortcut for <code>get(JsonMap.<jk>class</jk>, key)</code>.
	 *
	 * @param url The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public JsonMap getJsonMap(String url) {
		return get(url, JsonMap.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link JsonMap}.
	 *
	 * <p>
	 * Shortcut for <code>get(JsonMap.<jk>class</jk>, key, defVal)</code>.
	 *
	 * @param url The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public JsonMap getJsonMap(String url, JsonMap defVal) {
		return getWithDefault(url, defVal, JsonMap.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link JsonList}.
	 *
	 * <p>
	 * Shortcut for <code>get(JsonList.<jk>class</jk>, key)</code>.
	 *
	 * @param url The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public JsonList getJsonList(String url) {
		return get(url, JsonList.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link JsonList}.
	 *
	 * <p>
	 * Shortcut for <code>get(JsonList.<jk>class</jk>, key, defVal)</code>.
	 *
	 * @param url The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public JsonList getJsonList(String url, JsonList defVal) {
		return getWithDefault(url, defVal, JsonList.class);
	}

	/**
	 * Executes the specified method with the specified parameters on the specified object.
	 *
	 * @param url The URL of the element to retrieve.
	 * @param method
	 * 	The method signature.
	 * 	<p>
	 * 	Can be any of the following formats:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			Method name only.  e.g. <js>"myMethod"</js>.
	 * 		<li>
	 * 			Method name with class names.  e.g. <js>"myMethod(String,int)"</js>.
	 * 		<li>
	 * 			Method name with fully-qualified class names.  e.g. <js>"myMethod(java.util.String,int)"</js>.
	 * 	</ul>
	 * 	<p>
	 * 	As a rule, use the simplest format needed to uniquely resolve a method.
	 * @param args
	 * 	The arguments to pass as parameters to the method.
	 * 	These will automatically be converted to the appropriate object type if possible.
	 * 	This must be an array, like a JSON array.
	 * @return The returned object from the method call.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Thrown by underlying stream.
	 */
	public Object invokeMethod(String url, String method, String args) throws ExecutableException, ParseException, IOException {
		try {
			return new ObjectIntrospector(get(url), parser).invokeMethod(method, args);
		} catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
			throw new ExecutableException(e);
		}
	}

	/**
	 * Returns the list of available methods that can be passed to the {@link #invokeMethod(String, String, String)}
	 * for the object addressed by the specified URL.
	 *
	 * @param url The URL.
	 * @return The list of methods.
	 */
	public Collection<String> getPublicMethods(String url) {
		Object o = get(url);
		if (o == null)
			return null;
		return session.getClassMeta(o.getClass()).getPublicMethods().keySet();
	}

	/**
	 * Returns the class type of the object at the specified URL.
	 *
	 * @param url The URL.
	 * @return The class type.
	 */
	public ClassMeta getClassMeta(String url) {
		JsonNode n = getNode(normalizeUrl(url), root);
		if (n == null)
			return null;
		return n.cm;
	}

	/**
	 * Sets/replaces the element addressed by the URL.
	 *
	 * <p>
	 * This method expands the POJO model as necessary to create the new element.
	 *
	 * @param url
	 * 	The URL of the element to create.
	 * 	If <jk>null</jk> or blank, the root itself is replaced with the specified value.
	 * @param val The value being set.  Value can be of any type.
	 * @return The previously addressed element, or <jk>null</jk> the element did not previously exist.
	 */
	public Object put(String url, Object val) {
		return service(PUT, url, val);
	}

	/**
	 * Adds a value to a list element in a POJO model.
	 *
	 * <p>
	 * The URL is the address of the list being added to.
	 *
	 * <p>
	 * If the list does not already exist, it will be created.
	 *
	 * <p>
	 * This method expands the POJO model as necessary to create the new element.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		You can only post to three types of nodes:
	 * 		<ul>
	 * 			<li>{@link List Lists}
	 * 			<li>{@link Map Maps} containing integers as keys (i.e sparse arrays)
	 * 			<li>arrays
	 * 		</ul>
	 * </ul>
	 *
	 * @param url
	 * 	The URL of the element being added to.
	 * 	If <jk>null</jk> or blank, the root itself (assuming it's one of the types specified above) is added to.
	 * @param val The value being added.
	 * @return The URL of the element that was added.
	 */
	public String post(String url, Object val) {
		return (String)service(POST, url, val);
	}

	/**
	 * Remove an element from a POJO model.
	 *
	 * <p>
	 * If the element does not exist, no action is taken.
	 *
	 * @param url
	 * 	The URL of the element being deleted.
	 * 	If <jk>null</jk> or blank, the root itself is deleted.
	 * @return The removed element, or null if that element does not exist.
	 */
	public Object delete(String url) {
		return service(DELETE, url, null);
	}

	@Override /* Object */
	public String toString() {
		return String.valueOf(root.o);
	}

	/** Handle nulls and strip off leading '/' char. */
	private static String normalizeUrl(String url) {

		// Interpret nulls and blanks the same (i.e. as addressing the root itself)
		if (url == null)
			url = "";

		// Strip off leading slash if present.
		if (url.length() > 0 && url.charAt(0) == '/')
			url = url.substring(1);

		return url;
	}


	/*
	 * Workhorse method.
	 */
	private Object service(int method, String url, Object val) throws ObjectRestException {

		url = normalizeUrl(url);

		if (method == GET) {
			JsonNode p = getNode(url, root);
			return p == null ? null : p.o;
		}

		// Get the url of the parent and the property name of the addressed object.
		int i = url.lastIndexOf('/');
		String parentUrl = (i == -1 ? null : url.substring(0, i));
		String childKey = (i == -1 ? url : url.substring(i + 1));

		if (method == PUT) {
			if (url.length() == 0) {
				if (rootLocked)
					throw new ObjectRestException(HTTP_FORBIDDEN, "Cannot overwrite root object");
				Object o = root.o;
				root = new JsonNode(null, null, val, session.object());
				return o;
			}
			JsonNode n = (parentUrl == null ? root : getNode(parentUrl, root));
			if (n == null)
				throw new ObjectRestException(HTTP_NOT_FOUND, "Node at URL ''{0}'' not found.", parentUrl);
			ClassMeta cm = n.cm;
			Object o = n.o;
			if (cm.isMap())
				return ((Map)o).put(childKey, convert(val, cm.getValueType()));
			if (cm.isCollection() && o instanceof List)
				return ((List)o).set(parseInt(childKey), convert(val, cm.getElementType()));
			if (cm.isArray()) {
				o = setArrayEntry(n.o, parseInt(childKey), val, cm.getElementType());
				ClassMeta pct = n.parent.cm;
				Object po = n.parent.o;
				if (pct.isMap()) {
					((Map)po).put(n.keyName, o);
					return url;
				}
				if (pct.isBean()) {
					BeanMap m = session.toBeanMap(po);
					m.put(n.keyName, o);
					return url;
				}
				throw new ObjectRestException(HTTP_BAD_REQUEST, "Cannot perform PUT on ''{0}'' with parent node type ''{1}''", url, pct);
			}
			if (cm.isBean())
				return session.toBeanMap(o).put(childKey, val);
			throw new ObjectRestException(HTTP_BAD_REQUEST, "Cannot perform PUT on ''{0}'' whose parent is of type ''{1}''", url, cm);
		}

		if (method == POST) {
			// Handle POST to root special
			if (url.length() == 0) {
				ClassMeta cm = root.cm;
				Object o = root.o;
				if (cm.isCollection()) {
					Collection c = (Collection)o;
					c.add(convert(val, cm.getElementType()));
					return (c instanceof List ? url + "/" + (c.size()-1) : null);
				}
				if (cm.isArray()) {
					Object[] o2 = addArrayEntry(o, val, cm.getElementType());
					root = new JsonNode(null, null, o2, null);
					return url + "/" + (o2.length-1);
				}
				throw new ObjectRestException(HTTP_BAD_REQUEST, "Cannot perform POST on ''{0}'' of type ''{1}''", url, cm);
			}
			JsonNode n = getNode(url, root);
			if (n == null)
				throw new ObjectRestException(HTTP_NOT_FOUND, "Node at URL ''{0}'' not found.", url);
			ClassMeta cm = n.cm;
			Object o = n.o;
			if (cm.isArray()) {
				Object[] o2 = addArrayEntry(o, val, cm.getElementType());
				ClassMeta pct = n.parent.cm;
				Object po = n.parent.o;
				if (pct.isMap()) {
					((Map)po).put(childKey, o2);
					return url + "/" + (o2.length-1);
				}
				if (pct.isBean()) {
					BeanMap m = session.toBeanMap(po);
					m.put(childKey, o2);
					return url + "/" + (o2.length-1);
				}
				throw new ObjectRestException(HTTP_BAD_REQUEST, "Cannot perform POST on ''{0}'' with parent node type ''{1}''", url, pct);
			}
			if (cm.isCollection()) {
				Collection c = (Collection)o;
				c.add(convert(val, cm.getElementType()));
				return (c instanceof List ? url + "/" + (c.size()-1) : null);
			}
			throw new ObjectRestException(HTTP_BAD_REQUEST, "Cannot perform POST on ''{0}'' of type ''{1}''", url, cm);
		}

		if (method == DELETE) {
			if (url.length() == 0) {
				if (rootLocked)
					throw new ObjectRestException(HTTP_FORBIDDEN, "Cannot overwrite root object");
				Object o = root.o;
				root = new JsonNode(null, null, null, session.object());
				return o;
			}
			JsonNode n = (parentUrl == null ? root : getNode(parentUrl, root));
			ClassMeta cm = n.cm;
			Object o = n.o;
			if (cm.isMap())
				return ((Map)o).remove(childKey);
			if (cm.isCollection() && o instanceof List)
				return ((List)o).remove(parseInt(childKey));
			if (cm.isArray()) {
				int index = parseInt(childKey);
				Object old = ((Object[])o)[index];
				Object[] o2 = removeArrayEntry(o, index);
				ClassMeta pct = n.parent.cm;
				Object po = n.parent.o;
				if (pct.isMap()) {
					((Map)po).put(n.keyName, o2);
					return old;
				}
				if (pct.isBean()) {
					BeanMap m = session.toBeanMap(po);
					m.put(n.keyName, o2);
					return old;
				}
				throw new ObjectRestException(HTTP_BAD_REQUEST, "Cannot perform POST on ''{0}'' with parent node type ''{1}''", url, pct);
			}
			if (cm.isBean())
				return session.toBeanMap(o).put(childKey, null);
			throw new ObjectRestException(HTTP_BAD_REQUEST, "Cannot perform PUT on ''{0}'' whose parent is of type ''{1}''", url, cm);
		}

		return null;	// Never gets here.
	}

	private Object[] setArrayEntry(Object o, int index, Object val, ClassMeta componentType) {
		Object[] a = (Object[])o;
		if (a.length <= index) {
			// Expand out the array.
			Object[] a2 = (Object[])Array.newInstance(a.getClass().getComponentType(), index+1);
			System.arraycopy(a, 0, a2, 0, a.length);
			a = a2;
		}
		a[index] = convert(val, componentType);
		return a;
	}

	private Object[] addArrayEntry(Object o, Object val, ClassMeta componentType) {
		Object[] a = (Object[])o;
		// Expand out the array.
		Object[] a2 = (Object[])Array.newInstance(a.getClass().getComponentType(), a.length+1);
		System.arraycopy(a, 0, a2, 0, a.length);
		a2[a.length] = convert(val, componentType);
		return a2;
	}

	private static Object[] removeArrayEntry(Object o, int index) {
		Object[] a = (Object[])o;
		// Shrink the array.
		Object[] a2 = (Object[])Array.newInstance(a.getClass().getComponentType(), a.length-1);
		System.arraycopy(a, 0, a2, 0, index);
		System.arraycopy(a, index+1, a2, index, a.length-index-1);
		return a2;
	}

	class JsonNode {
		Object o;
		ClassMeta cm;
		JsonNode parent;
		String keyName;

		JsonNode(JsonNode parent, String keyName, Object o, ClassMeta cm) {
			this.o = o;
			this.keyName = keyName;
			this.parent = parent;
			if (cm == null || cm.isObject()) {
				if (o == null)
					cm = session.object();
				else
					cm = session.getClassMetaForObject(o);
			}
			this.cm = cm;
		}
	}

	JsonNode getNode(String url, JsonNode n) {
		if (url == null || url.isEmpty())
			return n;
		int i = url.indexOf('/');
		String parentKey, childUrl = null;
		if (i == -1) {
			parentKey = url;
		} else {
			parentKey = url.substring(0, i);
			childUrl = url.substring(i + 1);
		}

		Object o = n.o;
		Object o2 = null;
		ClassMeta cm = n.cm;
		ClassMeta ct2 = null;
		if (o == null)
			return null;
		if (cm.isMap()) {
			o2 = ((Map)o).get(parentKey);
			ct2 = cm.getValueType();
		} else if (cm.isCollection() && o instanceof List) {
			int key = parseInt(parentKey);
			List l = ((List)o);
			if (l.size() <= key)
				return null;
			o2 = l.get(key);
			ct2 = cm.getElementType();
		} else if (cm.isArray()) {
			int key = parseInt(parentKey);
			Object[] a = ((Object[])o);
			if (a.length <= key)
				return null;
			o2 = a[key];
			ct2 = cm.getElementType();
		} else if (cm.isBean()) {
			BeanMap m = session.toBeanMap(o);
			o2 = m.get(parentKey);
			BeanPropertyMeta pMeta = m.getPropertyMeta(parentKey);
			if (pMeta == null)
				throw new ObjectRestException(HTTP_BAD_REQUEST,
					"Unknown property ''{0}'' encountered while trying to parse into class ''{1}''",
					parentKey, m.getClassMeta()
				);
			ct2 = pMeta.getClassMeta();
		}

		if (childUrl == null)
			return new JsonNode(n, parentKey, o2, ct2);

		return getNode(childUrl, new JsonNode(n, parentKey, o2, ct2));
	}

	private Object convert(Object in, ClassMeta cm) {
		if (cm == null)
			return in;
		if (cm.isBean() && in instanceof Map)
			return session.convertToType(in, cm);
		return in;
	}

	private static int parseInt(String key) {
		try {
			return Integer.parseInt(key);
		} catch (NumberFormatException e) {
			throw new ObjectRestException(HTTP_BAD_REQUEST,
				"Cannot address an item in an array with a non-integer key ''{0}''", key
			);
		}
	}
}
