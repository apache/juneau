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
package org.apache.juneau.config;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.BinaryFormat.*;
import static java.util.Optional.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;

/**
 * A single entry in a {@link Config} file.
 */
public class Entry {

	private final ConfigMapEntry configEntry;
	private final Config config;
	private final String value;

	/**
	 * Constructor.
	 *
	 * @param config The config that this entry belongs to.
	 * @param configMap The map that this belongs to.
	 * @param sectionName The section name of this entry.
	 * @param entryName The name of this entry.
	 */
	protected Entry(Config config, ConfigMap configMap, String sectionName, String entryName) {
		this.configEntry = configMap.getEntry(sectionName, entryName);
		this.config = config;
		this.value = configEntry == null ? null : config.removeMods(configEntry.getModifiers(), configEntry.getValue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Value retrievers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this entry exists in the config.
	 *
	 * @return <jk>true</jk> if this entry exists in the config.
	 */
	public boolean isPresent() {
		return ! isNull();
	}

	/**
	 * Returns <jk>true</jk> if this entry exists in the config and is not empty.
	 *
	 * @return <jk>true</jk> if this entry exists in the config and is not empty.
	 */
	public boolean isNotEmpty() {
		return ! isEmpty();
	}

	/**
	 * Returns this entry as a string.
	 *
	 * @return <jk>true</jk> if this entry exists in the config and is not empty.
	 * @throws NullPointerException if value was <jk>null</jk>.
	 */
	public String get() {
		if (isNull()) throw new NullPointerException("Value was null");
		return toString();
	}

	/**
	 * Returns this entry converted to the specified type or returns the default value.
	 *
	 * <p>
	 * This is equivalent to calling <c>as(<jv>def</jv>.getClass()).orElse(<jv>def</jv>)</c> but is simpler and
	 * avoids the creation of an {@link Optional} object.
	 *
	 * @param def The default value to return if value does not exist.
	 * @return This entry converted to the specified type or returns the default value.
	 */
	public String orElse(String def) {
		return isNull() ? def : get();
	}

	/**
	 * Returns this entry converted to the specified type or returns the default value.
	 *
	 * <p>
	 * This is equivalent to calling <c>as(<jv>def</jv>.getClass()).orElse(<jv>def</jv>)</c> but is simpler and
	 * avoids the creation of an {@link Optional} object.
	 *
	 * @param def The default value to return if value does not exist.
	 * @return This entry converted to the specified type or returns the default value.
	 */
	public String orElseGet(Supplier<String> def) {
		return isNull() ? def.get() : get();
	}

	/**
	 * Returns this entry converted to the specified type.
	 *
	 * @param type The type to convert the value to.
	 * @return This entry converted to the specified type.
	 */
	public <T> T to(Class<T> type) {
		return to((Type)type);
	}

	/**
	 * Returns this entry converted to the specified type.
	 *
	 * @param type The type to convert the value to.
	 * @return This entry converted to the specified type.
	 */
	public <T> Optional<T> as(Class<T> type) {
		return ofNullable(to((Type)type));
	}

	/**
	 * Returns this entry converted to the specified value.
	 *
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	Config <jv>config</jv> = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
	 *
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List <jv>list</jv> = <jv>config</jv>.get(<js>"MySection/myListOfStrings"</js>).to(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List <jv>list</jv> = <jv>config</jv>.get(<js>"MySection/myListOfBeans"</js>).to(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List <jv>list</jv> = <jv>config</jv>.get(<js>"MySection/my2dListOfStrings"</js>).to(LinkedList.<jk>class</jk>,
	 * 		LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map <jv>map</jv> = <jv>config</jv>.get(<js>"MySection/myMap"</js>).to(TreeMap.<jk>class</jk>, String.<jk>class</jk>,
	 * 		String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map <jv>map</jv> = <jv>config</jv>.get(<js>"MySection/myMapOfListsOfBeans"</js>).to(TreeMap.<jk>class</jk>, String.<jk>class</jk>,
	 * 		List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * <c>Collection</c> classes are assumed to be followed by zero or one objects indicating the element type.
	 *
	 * <p>
	 * <c>Map</c> classes are assumed to be followed by zero or two meta objects indicating the key and value
	 * types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Use the {@link #to(Class)} method instead if you don't need a parameterized map/collection.
	 * </ul>
	 *
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public <T> T to(Type type, Type...args) {
		return to(config.parser, type, args);
	}

	/**
	 * Returns this entry converted to the specified value.
	 *
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	Config <jv>config</jv> = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
	 *
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List <jv>list</jv> = <jv>config</jv>.get(<js>"MySection/myListOfStrings"</js>).to(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List <jv>list</jv> = <jv>config</jv>.get(<js>"MySection/myListOfBeans"</js>).to(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List <jv>list</jv> = <jv>config</jv>.get(<js>"MySection/my2dListOfStrings"</js>).to(LinkedList.<jk>class</jk>,
	 * 		LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map <jv>map</jv> = <jv>config</jv>.get(<js>"MySection/myMap"</js>).to(TreeMap.<jk>class</jk>, String.<jk>class</jk>,
	 * 		String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map <jv>map</jv> = <jv>config</jv>.get(<js>"MySection/myMapOfListsOfBeans"</js>).to(TreeMap.<jk>class</jk>, String.<jk>class</jk>,
	 * 		List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * <c>Collection</c> classes are assumed to be followed by zero or one objects indicating the element type.
	 *
	 * <p>
	 * <c>Map</c> classes are assumed to be followed by zero or two meta objects indicating the key and value
	 * types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Use the {@link #to(Class)} method instead if you don't need a parameterized map/collection.
	 * </ul>
	 *
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The value, or {@link Optional#empty()} if the section or key does not exist.
	 */
	public <T> Optional<T> as(Type type, Type...args) {
		return ofNullable(to(type, args));
	}

	/**
	 * Same as {@link #to(Type, Type...)} but specifies the parser to use to parse the entry.
	 *
	 * @param parser
	 * 	The parser to use to parse the entry.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	@SuppressWarnings("unchecked")
	public <T> T to(Parser parser, Type type, Type...args) {
		if (isNull()) return null;

		try {
			String v = toString();
			if (type == String.class) return (T)toString();
			if (type == String[].class) return (T)toStringArray();
			if (type == byte[].class) return (T)toBytes();
			if (type == int.class) return (T)toInteger();
			if (type == int.class || type == Integer.class) return (T)toInteger();
			if (type == long.class || type == Long.class) return (T)toLong();
			if (type == OMap.class) return (T)toMap();
			if (type == OList.class) return (T)toList();
			if (isEmpty()) return null;
			if (isSimpleType(type)) return (T)config.beanSession.convertToType(v, (Class<?>)type);

			if (parser instanceof JsonParser) {
				char s1 = firstNonWhitespaceChar(v);
				if (isArray(type) && s1 != '[')
					v = '[' + v + ']';
				else if (s1 != '[' && s1 != '{' && ! "null".equals(v))
					v = '\'' + v + '\'';
			}
			return parser.parse(v, type, args);
		} catch (ParseException e) {
			throw new BeanRuntimeException(e, null, "Value could not be parsed.");
		}
	}

	/**
	 * Same as {@link #as(Type, Type...)} but specifies the parser to use to parse the entry.
	 *
	 * @param parser
	 * 	The parser to use to parse the entry.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The value, or {@link Optional#empty()} if the section or key does not exist.
	 */
	public <T> Optional<T> as(Parser parser, Type type, Type...args) {
		return ofNullable(to(parser, type, args));
	}

	/**
	 * Returns this entry converted to the specified type.
	 *
	 * @param parser The parser to use to parse the entry value.
	 * @param type The type to convert the value to.
	 * @return This entry converted to the specified type, or <jk>null</jk> if the entry does not exist.
	 */
	public <T> T to(Parser parser, Class<T> type) {
		return to(parser, (Type)type);
	}

	/**
	 * Returns this entry converted to the specified type.
	 *
	 * @param parser The parser to use to parse the entry value.
	 * @param type The type to convert the value to.
	 * @return This entry converted to the specified type, or {@link Optional#empty()} if the entry does not exist.
	 */
	public <T> Optional<T> as(Parser parser, Class<T> type) {
		return ofNullable(to(parser, type));
	}

	/**
	 * Returns this entry as a string.
	 *
	 * @return This entry as a string, or <jk>null</jk> if the entry does not exist.
	 */
 	@Override
	public String toString() {
 		return isPresent() ? config.varSession.resolve(value) : null;
	}

	/**
	 * Returns this entry as a string.
	 *
	 * @return This entry as a string, or {@link Optional#empty()} if the entry does not exist.
	 */
	public Optional<String> asString() {
 		return ofNullable(toString());
	}

 	/**
	 * Returns this entry as a string array.
	 *
	 * <p>
	 * If the value exists, splits the value on commas and returns the values as trimmed strings.
	 *
	 * @return This entry as a string array, or <jk>null</jk> if the entry does not exist.
	 */
	public String[] toStringArray() {
		if (! isPresent()) return null;
		String v = toString();
		char s1 = firstNonWhitespaceChar(v), s2 = lastNonWhitespaceChar(v);
		if (s1 == '[' && s2 == ']' && config.parser instanceof JsonParser) {
			try {
				return config.parser.parse(v, String[].class);
			} catch (ParseException e) {
				throw new BeanRuntimeException(e);
			}
		}
		return split(v);
	}

	/**
	 * Returns this entry as a string array.
	 *
	 * <p>
	 * If the value exists, splits the value on commas and returns the values as trimmed strings.
	 *
	 * @return This entry as a string array, or {@link Optional#empty()} if the entry does not exist.
	 */
	public Optional<String[]> asStringArray() {
		return ofNullable(toStringArray());
	}

	/**
	 * Returns this entry as an integer.
	 *
	 * <p>
	 * <js>"K"</js>, <js>"M"</js>, and <js>"G"</js> can be used to identify kilo, mega, and giga in base 2.
	 * <br><js>"k"</js>, <js>"m"</js>, and <js>"g"</js> can be used to identify kilo, mega, and giga in base 10.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<code><js>"100K"</js> => 1024000</code>
	 * 	<li>
	 * 		<code><js>"100M"</js> => 104857600</code>
	 * 	<li>
	 * 		<code><js>"100k"</js> => 1000000</code>
	 * 	<li>
	 * 		<code><js>"100m"</js> => 100000000</code>
	 * </ul>
	 *
	 * <p>
	 * Uses {@link Integer#decode(String)} underneath, so any of the following integer formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @return The value, or <jk>null</jk> if the value does not exist or the value is empty.
	 */
	public Integer toInteger() {
		return isNotEmpty() ? parseIntWithSuffix(toString()) : null;
	}

	/**
	 * Returns this entry as an integer.
	 *
	 * <p>
	 * <js>"K"</js>, <js>"M"</js>, and <js>"G"</js> can be used to identify kilo, mega, and giga in base 2.
	 * <br><js>"k"</js>, <js>"m"</js>, and <js>"g"</js> can be used to identify kilo, mega, and giga in base 10.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<code><js>"100K"</js> => 1024000</code>
	 * 	<li>
	 * 		<code><js>"100M"</js> => 104857600</code>
	 * 	<li>
	 * 		<code><js>"100k"</js> => 1000000</code>
	 * 	<li>
	 * 		<code><js>"100m"</js> => 100000000</code>
	 * </ul>
	 *
	 * <p>
	 * Uses {@link Integer#decode(String)} underneath, so any of the following integer formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @return The value, or {@link Optional#empty()} if the value does not exist or the value is empty.
	 */
	public Optional<Integer> asInteger() {
		return ofNullable(toInteger());
	}

	/**
	 * Returns this entry as a parsed boolean.
	 *
	 * <p>
	 * Uses {@link Boolean#parseBoolean(String)} to parse value.
	 *
	 * @return The value, or <jk>null</jk> if the value does not exist or the value is empty.
	 */
	public Boolean toBoolean() {
		return isNotEmpty() ? Boolean.parseBoolean(toString()) : null;
	}

	/**
	 * Returns this entry as a parsed boolean.
	 *
	 * <p>
	 * Uses {@link Boolean#parseBoolean(String)} to parse value.
	 *
	 * @return The value, or {@link Optional#empty()} if the value does not exist or the value is empty.
	 */
	public Optional<Boolean> asBoolean() {
		return ofNullable(toBoolean());
	}

	/**
	 * Returns this entry as a long.
	 *
	 * <p>
	 * <js>"K"</js>, <js>"M"</js>, <js>"G"</js>, <js>"T"</js>, and <js>"P"</js> can be used to identify kilo, mega, giga, tera, and penta in base 2.
	 * <br><js>"k"</js>, <js>"m"</js>, <js>"g"</js>, <js>"t"</js>, and <js>"p"</js> can be used to identify kilo, mega, giga, tera, and p in base 10.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<code><js>"100K"</js> => 1024000</code>
	 * 	<li>
	 * 		<code><js>"100M"</js> => 104857600</code>
	 * 	<li>
	 * 		<code><js>"100k"</js> => 1000000</code>
	 * 	<li>
	 * 		<code><js>"100m"</js> => 100000000</code>
	 * </ul>
	 *
	 * <p>
	 * Uses {@link Long#decode(String)} underneath, so any of the following integer formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @return The value, or <jk>null</jk> if the value does not exist or the value is empty.
	 */
	public Long toLong() {
		return isNotEmpty() ? parseLongWithSuffix(toString()) : null;
	}

	/**
	 * Returns this entry as a long.
	 *
	 * <p>
	 * <js>"K"</js>, <js>"M"</js>, <js>"G"</js>, <js>"T"</js>, and <js>"P"</js> can be used to identify kilo, mega, giga, tera, and penta in base 2.
	 * <br><js>"k"</js>, <js>"m"</js>, <js>"g"</js>, <js>"t"</js>, and <js>"p"</js> can be used to identify kilo, mega, giga, tera, and p in base 10.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<code><js>"100K"</js> => 1024000</code>
	 * 	<li>
	 * 		<code><js>"100M"</js> => 104857600</code>
	 * 	<li>
	 * 		<code><js>"100k"</js> => 1000000</code>
	 * 	<li>
	 * 		<code><js>"100m"</js> => 100000000</code>
	 * </ul>
	 *
	 * <p>
	 * Uses {@link Long#decode(String)} underneath, so any of the following integer formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @return The value, or {@link Optional#empty()} if the value does not exist or the value is empty.
	 */
	public Optional<Long> asLong() {
		return ofNullable(toLong());
	}

	/**
	 * Returns this entry as a double.
	 *
	 * <p>
	 * Uses {@link Double#valueOf(String)} underneath, so any of the following number formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @return The value, or <jk>null</jk> if the value does not exist or the value is empty.
	 */
	public Double toDouble() {
		return isNotEmpty() ? Double.valueOf(toString()) : null;
	}

	/**
	 * Returns this entry as a double.
	 *
	 * <p>
	 * Uses {@link Double#valueOf(String)} underneath, so any of the following number formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @return The value, or {@link Optional#empty()} if the value does not exist or the value is empty.
	 */
	public Optional<Double> asDouble() {
		return ofNullable(toDouble());
	}

	/**
	 * Returns this entry as a float.
	 *
	 * <p>
	 * Uses {@link Float#valueOf(String)} underneath, so any of the following number formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @return The value, or <jk>null</jk> if the value does not exist or the value is empty.
	 */
	public Float toFloat() {
		return isNotEmpty() ? Float.valueOf(toString()) : null;
	}

	/**
	 * Returns this entry as a float.
	 *
	 * <p>
	 * Uses {@link Float#valueOf(String)} underneath, so any of the following number formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @return The value, or {@link Optional#empty()} if the value does not exist or the value is empty.
	 */
	public Optional<Float> asFloat() {
		return ofNullable(toFloat());
	}

	/**
	 * Returns this entry as a byte array.
	 *
	 * <p>
	 * Byte arrays are stored as encoded strings, typically BASE64, but dependent on the {@link Config.Builder#binaryFormat(BinaryFormat)} setting.
	 *
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public byte[] toBytes() {
		if (isNull()) return null;
		String s = toString();
		if (s.indexOf('\n') != -1) s = s.replaceAll("\n", "");
		try {
			if (config.binaryFormat == HEX)
				return fromHex(s);
			if (config.binaryFormat == SPACED_HEX)
				return fromSpacedHex(s);
			return base64Decode(s);
		} catch (Exception e) {
			throw new BeanRuntimeException(e, null, "Value could not be converted to a byte array.");
		}
	}

	/**
	 * Returns this entry as a byte array.
	 *
	 * <p>
	 * Byte arrays are stored as encoded strings, typically BASE64, but dependent on the {@link Config.Builder#binaryFormat(BinaryFormat)} setting.
	 *
	 * @return The value, or {@link Optional#empty()} if the section or key does not exist.
	 */
	public Optional<byte[]> asBytes() {
		return ofNullable(toBytes());
	}

	/**
	 * Returns this entry as a parsed map.
	 *
	 * <p>
	 * Uses the parser registered on the {@link Config} to parse the entry.
	 *
	 * <p>
	 * If the parser is a JSON parser, the starting/trailing <js>"{"</js>/<js>"}"</js> in the value are optional.
	 *
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 * @throws ParseException If value could not be parsed.
	 */
	public OMap toMap() throws ParseException {
		return toMap(null);
	}

	/**
	 * Returns this entry as a parsed map.
	 *
	 * <p>
	 * Uses the parser registered on the {@link Config} to parse the entry.
	 *
	 * <p>
	 * If the parser is a JSON parser, the starting/trailing <js>"{"</js>/<js>"}"</js> in the value are optional.
	 *
	 * @return The value, or {@link Optional#empty()} if the section or key does not exist.
	 * @throws ParseException If value could not be parsed.
	 */
	public Optional<OMap> asMap() throws ParseException {
		return ofNullable(toMap());
	}

	/**
	 * Returns this entry as a parsed map.
	 *
	 * <p>
	 * If the parser is a JSON parser, the starting/trailing <js>"{"</js>/<js>"}"</js> in the value are optional.
	 *
	 * @param parser The parser to use to parse the value, or <jk>null</jk> to use the parser defined on the config.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 * @throws ParseException If value could not be parsed.
	 */
	public OMap toMap(Parser parser) throws ParseException {
		if (isNull()) return null;
		if (parser == null) parser = config.parser;
		String s = toString();
		if (parser instanceof JsonParser) {
			char s1 = firstNonWhitespaceChar(s);
			if (s1 != '{' && ! "null".equals(s))
				s = '{' + s + '}';
		}
		return OMap.ofText(s, parser);
	}

	/**
	 * Returns this entry as a parsed map.
	 *
	 * <p>
	 * If the parser is a JSON parser, the starting/trailing <js>"{"</js>/<js>"}"</js> in the value are optional.
	 *
	 * @param parser The parser to use to parse the value, or {@link Optional#empty()} to use the parser defined on the config.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 * @throws ParseException If value could not be parsed.
	 */
	public Optional<OMap> asMap(Parser parser) throws ParseException {
		return ofNullable(toMap(parser));
	}

	/**
	 * Returns this entry as a parsed list.
	 *
	 * <p>
	 * Uses the parser registered on the {@link Config} to parse the entry.
	 *
	 * <p>
	 * If the parser is a JSON parser, the starting/trailing <js>"["</js>/<js>"]"</js> in the value are optional.
	 *
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 * @throws ParseException If value could not be parsed.
	 */
	public OList toList() throws ParseException {
		return toList(config.parser);
	}

	/**
	 * Returns this entry as a parsed list.
	 *
	 * <p>
	 * Uses the parser registered on the {@link Config} to parse the entry.
	 *
	 * <p>
	 * If the parser is a JSON parser, the starting/trailing <js>"["</js>/<js>"]"</js> in the value are optional.
	 *
	 * @return The value, or {@link Optional#empty()} if the section or key does not exist.
	 * @throws ParseException If value could not be parsed.
	 */
	public Optional<OList> asList() throws ParseException {
		return ofNullable(toList());
	}

	/**
	 * Returns this entry as a parsed list.
	 *
	 * <p>
	 * If the parser is a JSON parser, the starting/trailing <js>"["</js>/<js>"]"</js> in the value are optional.
	 *
	 * @param parser The parser to use to parse the value, or {@link Optional#empty()} to use the parser defined on the config.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 * @throws ParseException If value could not be parsed.
	 */
	public OList toList(Parser parser) throws ParseException {
		if (isNull()) return null;
		if (parser == null) parser = config.parser;
		String s = toString();
		if (parser instanceof JsonParser) {
			char s1 = firstNonWhitespaceChar(s);
			if (s1 != '[' && ! "null".equals(s))
				s = '[' + s + ']';
		}
		return OList.ofText(s, parser);
	}

	/**
	 * Returns this entry as a parsed list.
	 *
	 * <p>
	 * If the parser is a JSON parser, the starting/trailing <js>"["</js>/<js>"]"</js> in the value are optional.
	 *
	 * @param parser The parser to use to parse the value, or {@link Optional#empty()} to use the parser defined on the config.
	 * @return The value, or {@link Optional#empty()} if the section or key does not exist.
	 * @throws ParseException If value could not be parsed.
	 */
	public Optional<OList> asList(Parser parser) throws ParseException {
		return ofNullable(toList(parser));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Metadata retrievers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the name of this entry.
	 *
	 * @return The name of this entry.
	 */
	public String getKey() {
		return configEntry.getKey();
	}

	/**
	 * Returns the raw value of this entry.
	 *
	 * @return The raw value of this entry.
	 */
	public String getValue() {
		return configEntry.getValue();
	}

	/**
	 * Returns the same-line comment of this entry.
	 *
	 * @return The same-line comment of this entry.
	 */
	public String getComment() {
		return configEntry.getComment();
	}

	/**
	 * Returns the pre-lines of this entry.
	 *
	 * @return The pre-lines of this entry as an unmodifiable list.
	 */
	public List<String> getPreLines() {
		return configEntry.getPreLines();
	}

	/**
	 * Returns the modifiers for this entry.
	 *
	 * @return The modifiers for this entry, or <jk>null</jk> if it has no modifiers.
	 */
	public String getModifiers() {
		return configEntry.getModifiers();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private boolean isEmpty() {
		return StringUtils.isEmpty(value);
	}

	private boolean isNull() {
		return value == null;
	}

	private boolean isArray(Type t) {
		if (! (t instanceof Class))
			return false;
		Class<?> c = (Class<?>)t;
		return (c.isArray());
	}

	private boolean isSimpleType(Type t) {
		if (! (t instanceof Class))
			return false;
		Class<?> c = (Class<?>)t;
		return (c == String.class || c.isPrimitive() || c.isAssignableFrom(Number.class) || c == Boolean.class || c.isEnum());
	}
}
