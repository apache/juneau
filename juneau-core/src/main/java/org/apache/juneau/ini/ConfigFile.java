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
package org.apache.juneau.ini;

import static java.lang.reflect.Modifier.*;
import static org.apache.juneau.ini.ConfigFileFormat.*;
import static org.apache.juneau.ini.ConfigUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Implements the API for accessing the contents of a config file.
 * <p>
 * Refer to <a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.ini</a> for usage information.
 */
public abstract class ConfigFile implements Map<String,Section> {

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	/**
	 * Retrieves an entry value from this config file.
	 *
	 * @param sectionName The section name.  Must not be <jk>null</jk>.
	 * @param sectionKey The section key.  Must not be <jk>null</jk>.
	 * @return The value, or the default value if the section or value doesn't exist.
	 */
	public abstract String get(String sectionName, String sectionKey);

	/**
	 * Sets an entry value in this config file.
	 *
	 * @param sectionName The section name.  Must not be <jk>null</jk>.
	 * @param sectionKey The section key.  Must not be <jk>null</jk>.
	 * @param value The new value.
	 * @param serializer The serializer to use for serializing the object.
	 * 	If <jk>null</jk>, then uses the predefined serializer on the config file.
	 * @param encoded If <jk>true</jk>, then encode the value using the encoder associated with this config file.
	 * @param newline If <jk>true</jk>, then put serialized output on a separate line from the key.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException If the object value could not be converted to a JSON string for some reason.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract String put(String sectionName, String sectionKey, Object value, Serializer serializer,
			boolean encoded, boolean newline) throws SerializeException;

	/**
	 * Identical to {@link #put(String, String, Object, Serializer, boolean, boolean)} except used when the value is a
	 * simple string to avoid having to catch a {@link SerializeException}.
	 *
	 * @param sectionName The section name.  Must not be <jk>null</jk>.
	 * @param sectionKey The section key.  Must not be <jk>null</jk>.
	 * @param value The new value.
	 * @param encoded
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract String put(String sectionName, String sectionKey, String value, boolean encoded);


	/**
	 * Removes an entry from this config file.
	 *
	 * @param sectionName The section name.  Must not be <jk>null</jk>.
	 * @param sectionKey The section key.  Must not be <jk>null</jk>.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract String remove(String sectionName, String sectionKey);

	/**
	 * Returns the current set of keys in the specified section.
	 *
	 * @param sectionName The section name.  Must not be <jk>null</jk>.
	 * @return The list of keys in the specified section, or <jk>null</jk> if section does not exist.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract Set<String> getSectionKeys(String sectionName);

	/**
	 * Reloads this config file object from the persisted file contents if the modified timestamp on the file has changed.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If file could not be read, or file is not associated with this object.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile loadIfModified() throws IOException;

	/**
	 * Loads this config file object from the persisted file contents.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If file could not be read, or file is not associated with this object.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile load() throws IOException;

	/**
	 * Loads this config file object from the specified reader.
	 *
	 * @param r The reader to read from.
	 * @return This object (for method chaining).
	 * @throws IOException If file could not be read, or file is not associated with this object.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile load(Reader r) throws IOException;

	/**
	 * Adds arbitrary lines to the specified config file section.
	 * <p>
	 * The lines can be any of the following....
	 * <ul class='spaced-list'>
	 * 	<li><js>"# comment"</js> - A comment line.
	 * 	<li><js>"key=val"</js> - A key/value pair (equivalent to calling {@link #put(String,Object)}.
	 * 	<li><js>" foobar "</js> - Anything else (interpreted as a comment).
	 * </ul>
	 * <p>
	 * If the section does not exist, it will automatically be created.
	 *
	 * @param section The name of the section to add lines to, or <jk>null</jk> to add to the beginning unnamed section.
	 * @param lines The lines to add to the section.
	 * @return This object (for method chaining).
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile addLines(String section, String...lines);

	/**
	 * Adds header comments to the specified section.
	 * <p>
	 * Header comments are defined as lines that start with <jk>"#"</jk> immediately preceding a section header
	 * <jk>"[section]"</jk>.
	 * These are handled as part of the section itself instead of being interpreted as comments in the previous section.
	 * <p>
	 * Header comments can be of the following formats...
	 * <ul class='spaced-list'>
	 * 	<li><js>"# comment"</js> - A comment line.
	 * 	<li><js>"comment"</js> - Anything else (will automatically be prefixed with <js>"# "</js>).
	 * </ul>
	 * <p>
	 * If the section does not exist, it will automatically be created.
	 *
	 * @param section The name of the section to add lines to, or <jk>null</jk> to add to the default section.
	 * @param headerComments The comment lines to add to the section.
	 * @return This object (for method chaining).
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile addHeaderComments(String section, String...headerComments);

	/**
	 * Removes any header comments from the specified section.
	 *
	 * @param section The name of the section to remove header comments from.
	 * @return This object (for method chaining).
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile clearHeaderComments(String section);

	/**
	 * Returns the reusable bean session associated with this config file.
	 * <p>
	 * Used for performing simple datatype conversions.
	 *
	 * @return The reusable bean session associated with this config file.
	 */
	protected abstract BeanSession getBeanSession();

	/**
	 * Converts the specified object to a string.
	 * <p>
	 * The serialized output is identical to LAX JSON (JSON with unquoted attributes) except for the following
	 * exceptions:
	 * <ul>
	 * 	<li>Top level strings are not quoted.
	 * </ul>
	 *
	 * @param o The object to serialize.
	 * @param serializer The serializer to use for serializing the object.
	 * 	If <jk>null</jk>, then uses the predefined serializer on the config file.
	 * @param newline If <jk>true</jk>, add a newline at the beginning of the value.
	 * @return The serialized object.
	 * @throws SerializeException
	 */
	protected abstract String serialize(Object o, Serializer serializer, boolean newline) throws SerializeException;

	/**
	 * Converts the specified string to an object of the specified type.
	 *
	 * @param s The string to parse.
	 * @param parser The parser to use for parsing the object.
	 * 	If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param type The data type to create.
	 * @param args The generic type arguments if the type is a {@link Collection} or {@link Map}
	 * @return The parsed object.
	 * @throws ParseException
	 */
	protected abstract <T> T parse(String s, Parser parser, Type type, Type...args) throws ParseException;

	/**
	 * Places a read lock on this config file.
	 */
	protected abstract void readLock();

	/**
	 * Removes the read lock on this config file.
	 */
	protected abstract void readUnlock();


	//--------------------------------------------------------------------------------
	// API methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the specified value as a string from the config file.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param def The default value if the section or value does not exist.
	 * @return The value, or the default value if the section or value doesn't exist.
	 */
	public final String getString(String key, String def) {
		assertFieldNotNull(key, "key");
		String s = get(getSectionName(key), getSectionKey(key));
		return (StringUtils.isEmpty(s) && def != null ? def : s);
	}

	/**
	 * Removes an entry with the specified key.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public final String removeString(String key) {
		assertFieldNotNull(key, "key");
		return remove(getSectionName(key), getSectionKey(key));
	}

	/**
	 * Gets the entry with the specified key and converts it to the specified value.
	 * <p>
	 * The key can be in one of the following formats...
	 * <ul class='spaced-list'>
	 * 	<li><js>"key"</js> - A value in the default section (i.e. defined above any <code>[section]</code> header).
	 * 	<li><js>"section/key"</js> - A value from the specified section.
	 * </ul>
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	ConfigFile cf = <jk>new</jk> ConfigFileBuilder().build(<js>"MyConfig.cfg"</js>);
	 *
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List l = cf.getObject(<js>"MySection/myListOfStrings"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List l = cf.getObject(<js>"MySection/myListOfBeans"</js>, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List l = cf.getObject(<js>"MySection/my2dListOfStrings"</js>, LinkedList.<jk>class</jk>,
	 * 		LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map m = cf.getObject(<js>"MySection/myMap"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>,
	 * 		String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map m = cf.getObject(<js>"MySection/myMapOfListsOfBeans"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>,
	 * 		List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 * <p>
	 * <code>Collection</code> classes are assumed to be followed by zero or one objects indicating the element type.
	 * <p>
	 * <code>Map</code> classes are assumed to be followed by zero or two meta objects indicating the key and value
	 * types.
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Use the {@link #getObject(String, Class)} method instead if you don't need a parameterized map/collection.
	 * </ul>
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param type The object type to create.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * <br>Ignored if the main type is not a map or collection.
	 *
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public final <T> T getObject(String key, Type type, Type...args) throws ParseException {
		return getObject(key, (Parser)null, type, args);
	}

	/**
	 * Same as {@link #getObject(String, Type, Type...)} but allows you to specify the parser to use to parse the value.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param parser The parser to use for parsing the object.
	 * If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param type The object type to create.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * <br>Ignored if the main type is not a map or collection.
	 *
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public final <T> T getObject(String key, Parser parser, Type type, Type...args) throws ParseException {
		assertFieldNotNull(key, "key");
		assertFieldNotNull(type, "type");
		return parse(getString(key), parser, type, args);
	}

	/**
	 * Same as {@link #getObject(String, Type, Type...)} except optimized for a non-parameterized class.
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	ConfigFile cf = <jk>new</jk> ConfigFileBuilder().build(<js>"MyConfig.cfg"</js>);
	 *
	 * 	<jc>// Parse into a string.</jc>
	 * 	String s = cf.getObject(<js>"MySection/mySimpleString"</js>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean b = cf.getObject(<js>"MySection/myBean"</js>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] b = cf.getObject(<js>"MySection/myBeanArray"</js>, MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List l = cf.getObject(<js>"MySection/myList"</js>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map m = cf.getObject(<js>"MySection/myMap"</js>, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class type of the object being created.
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified
	 * type.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public final <T> T getObject(String key, Class<T> type) throws ParseException {
		return getObject(key, (Parser)null, type);
	}

	/**
	 * Same as {@link #getObject(String, Class)} but allows you to specify the parser to use to parse the value.
	 *
	 * @param <T> The class type of the object being created.
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param parser The parser to use for parsing the object.
	 * If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified
	 * type.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public final <T> T getObject(String key, Parser parser, Class<T> type) throws ParseException {
		assertFieldNotNull(key, "key");
		assertFieldNotNull(type, "c");
		return parse(getString(key), parser, type);
	}

	/**
	 * Gets the entry with the specified key and converts it to the specified value.
	 * <p>
	 * Same as {@link #getObject(String, Class)}, but with a default value.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param def The default value if section or key does not exist.
	 * @param type The class to convert the value to.
	 *
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public final <T> T getObjectWithDefault(String key, T def, Class<T> type) throws ParseException {
		return getObjectWithDefault(key, null, def, type);
	}

	/**
	 * Same as {@link #getObjectWithDefault(String, Object, Class)} but allows you to specify the parser to use to parse
	 * the value.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param parser The parser to use for parsing the object.
	 * If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param def The default value if section or key does not exist.
	 * @param type The class to convert the value to.
	 *
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public final <T> T getObjectWithDefault(String key, Parser parser, T def, Class<T> type) throws ParseException {
		assertFieldNotNull(key, "key");
		assertFieldNotNull(type, "c");
		T t = parse(getString(key), parser, type);
		return (t == null ? def : t);
	}

	/**
	 * Gets the entry with the specified key and converts it to the specified value.
	 * <p>
	 * Same as {@link #getObject(String, Type, Type...)}, but with a default value.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param def The default value if section or key does not exist.
	 * @param type The object type to create.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * <br>Ignored if the main type is not a map or collection.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public final <T> T getObjectWithDefault(String key, T def, Type type, Type...args) throws ParseException {
		return getObjectWithDefault(key, null, def, type, args);
	}

	/**
	 * Same as {@link #getObjectWithDefault(String, Object, Type, Type...)} but allows you to specify the parser to use
	 * to parse the value.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param parser The parser to use for parsing the object.
	 * If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param def The default value if section or key does not exist.
	 * @param type The object type to create.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * <br>Ignored if the main type is not a map or collection.
	 *
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public final <T> T getObjectWithDefault(String key, Parser parser, T def, Type type, Type...args) throws ParseException {
		assertFieldNotNull(key, "key");
		assertFieldNotNull(type, "type");
		T t = parse(getString(key), parser, type, args);
		return (t == null ? def : t);
	}

	/**
	 * Gets the entry with the specified key and converts it to the specified value.
	 * <p>
	 * Same as {@link #getObject(String, Class)}, but used when key is already broken into section/key.
	 *
	 * @param sectionName The section name.  Must not be <jk>null</jk>.
	 * @param sectionKey The section key.  Must not be <jk>null</jk>.
	 * @param c The class to convert the value to.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or the default value if the section or value doesn't exist.
	 */
	public final <T> T getObject(String sectionName, String sectionKey, Class<T> c) throws ParseException {
		return getObject(sectionName, sectionKey, null, c);
	}

	/**
	 * Same as {@link #getObject(String, String, Class)} but allows you to specify the parser to use to parse the value.
	 *
	 * @param sectionName The section name.  Must not be <jk>null</jk>.
	 * @param sectionKey The section key.  Must not be <jk>null</jk>.
	 * @param parser The parser to use for parsing the object.
	 * 	If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param c The class to convert the value to.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or the default value if the section or value doesn't exist.
	 */
	public final <T> T getObject(String sectionName, String sectionKey, Parser parser, Class<T> c) throws ParseException {
		assertFieldNotNull(sectionName, "sectionName");
		assertFieldNotNull(sectionKey, "sectionKey");
		return parse(get(sectionName, sectionKey), parser, c);
	}

	/**
	 * Gets the entry with the specified key and converts it to the specified value.
	 * <p>
	 * Same as {@link #getObject(String, Type, Type...)}, but used when key is already broken into section/key.
	 *
	 * @param sectionName The section name.  Must not be <jk>null</jk>.
	 * @param sectionKey The section key.  Must not be <jk>null</jk>.
	 * @param type The object type to create.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * <br>Ignored if the main type is not a map or collection.
	 *
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public final <T> T getObject(String sectionName, String sectionKey, Type type, Type...args) throws ParseException {
		return getObject(sectionName, sectionKey, null, type, args);
	}

	/**
	 * Same as {@link #getObject(String, String, Type, Type...)} but allows you to specify the parser to use to parse
	 * the value.
	 *
	 * @param sectionName The section name.  Must not be <jk>null</jk>.
	 * @param sectionKey The section key.  Must not be <jk>null</jk>.
	 * @param parser The parser to use for parsing the object.
	 * If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param type The object type to create.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * <br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * {@link GenericArrayType}
	 * <br>Ignored if the main type is not a map or collection.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public final <T> T getObject(String sectionName, String sectionKey, Parser parser, Type type, Type...args)
			throws ParseException {
		assertFieldNotNull(sectionName, "sectionName");
		assertFieldNotNull(sectionKey, "sectionKey");
		return parse(get(sectionName, sectionKey), parser, type, args);
	}

	/**
	 * Gets the entry with the specified key.
	 * <p>
	 * The key can be in one of the following formats...
	 * <ul class='spaced-list'>
	 * 	<li><js>"key"</js> - A value in the default section (i.e. defined above any <code>[section]</code> header).
	 * 	<li><js>"section/key"</js> - A value from the specified section.
	 * </ul>
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public final String getString(String key) {
		return getString(key, null);
	}

	/**
	 * Gets the entry with the specified key, splits the value on commas, and returns the values as trimmed strings.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @return The value, or an empty list if the section or key does not exist.
	 */
	public final String[] getStringArray(String key) {
		return getStringArray(key, new String[0]);
	}

	/**
	 * Same as {@link #getStringArray(String)} but returns a default value if the value cannot be found.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param def The default value if section or key does not exist.
	 * @return The value, or an empty list if the section or key does not exist.
	 */
	public final String[] getStringArray(String key, String[] def) {
		String s = getString(key);
		if (s == null)
			return def;
		String[] r = StringUtils.isEmpty(s) ? new String[0] : split(s);
		return r.length == 0 ? def : r;
	}

	/**
	 * Convenience method for getting int config values.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @return The value, or <code>0</code> if the section or key does not exist or cannot be parsed as an integer.
	 */
	public final int getInt(String key) {
		return getInt(key, 0);
	}

	/**
	 * Convenience method for getting int config values.
	 * <p>
	 * <js>"M"</js> and <js>"K"</js> can be used to identify millions and thousands.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><code><js>"100K"</js> => 1024000</code>
	 * 	<li><code><js>"100M"</js> => 104857600</code>
	 * </ul>
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param def The default value if config file or value does not exist.
	 * @return The value, or the default value if the section or key does not exist or cannot be parsed as an integer.
	 */
	public final int getInt(String key, int def) {
		String s = getString(key);
		if (StringUtils.isEmpty(s))
			return def;
		return parseIntWithSuffix(s);
	}

	/**
	 * Convenience method for getting boolean config values.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @return The value, or <jk>false</jk> if the section or key does not exist or cannot be parsed as a boolean.
	 */
	public final boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	/**
	 * Convenience method for getting boolean config values.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param def The default value if config file or value does not exist.
	 * @return The value, or the default value if the section or key does not exist or cannot be parsed as a boolean.
	 */
	public final boolean getBoolean(String key, boolean def) {
		String s = getString(key);
		return StringUtils.isEmpty(s) ? def : Boolean.parseBoolean(s);
	}

	/**
	 * Adds or replaces an entry with the specified key with a POJO serialized to a string using the registered
	 * serializer.
	 * <p>
	 * Equivalent to calling <code>put(key, value, isEncoded(key))</code>.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param value The new value POJO.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException If serializer could not serialize the value or if a serializer is not registered with
	 * this config file.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public final String put(String key, Object value) throws SerializeException {
		return put(key, value, null, isEncoded(key), false);
	}

	/**
	 * Same as {@link #put(String, Object)} but allows you to specify the serializer to use to serialize the value.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param value The new value POJO.
	 * @param serializer The serializer to use for serializing the object.
	 * If <jk>null</jk>, then uses the predefined serializer on the config file.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException If serializer could not serialize the value or if a serializer is not registered with
	 * this config file.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public final String put(String key, Object value, Serializer serializer) throws SerializeException {
		return put(key, value, serializer, isEncoded(key), false);
	}

	/**
	 * Adds or replaces an entry with the specified key with the specified value.
	 * <p>
	 * The format of the entry depends on the data type of the value.
	 * <ul class='spaced-list'>
	 * 	<li>Simple types (<code>String</code>, <code>Number</code>, <code>Boolean</code>, primitives)
	 * 		are serialized as plain strings.
	 * 	<li>Arrays and collections of simple types are serialized as comma-delimited lists of plain strings.
	 * 	<li>Other types (e.g. beans) are serialized using the serializer registered with this config file.
	 * 	<li>Arrays and collections of other types are serialized as comma-delimited lists of serialized strings of
	 * 		each entry.
	 * </ul>
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param value The new value.
	 * @param encoded If <jk>true</jk>, value is encoded by the registered encoder when the config file is persisted to
	 * disk.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException If serializer could not serialize the value or if a serializer is not registered with
	 * this config file.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public final String put(String key, Object value, boolean encoded) throws SerializeException {
		return put(key, value, null, encoded, false);
	}

	/**
	 * Same as {@link #put(String, Object, boolean)} but allows you to specify the serializer to use to serialize the
	 * value.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param value The new value.
	 * @param serializer The serializer to use for serializing the object.
	 * If <jk>null</jk>, then uses the predefined serializer on the config file.
	 * @param encoded If <jk>true</jk>, value is encoded by the registered encoder when the config file is persisted
	 * to disk.
	 * @param newline If <jk>true</jk>, a newline is added to the beginning of the input.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException If serializer could not serialize the value or if a serializer is not registered
	 * with this config file.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public final String put(String key, Object value, Serializer serializer, boolean encoded, boolean newline)
			throws SerializeException {
		assertFieldNotNull(key, "key");
		return put(getSectionName(key), getSectionKey(key), serialize(value, serializer, newline), encoded);
	}

	/**
	 * Returns the specified section as a map of key/value pairs.
	 *
	 * @param sectionName The section name to retrieve.
	 * @return A map of the section, or <jk>null</jk> if the section was not found.
	 */
	public final ObjectMap getSectionMap(String sectionName) {
		readLock();
		try {
			Set<String> keys = getSectionKeys(sectionName);
			if (keys == null)
				return null;
			ObjectMap m = new ObjectMap();
			for (String key : keys)
				m.put(key, get(sectionName, key));
			return m;
		} finally {
			readUnlock();
		}
	}

	/**
	 * Copies the entries in a section to the specified bean by calling the public setters on that bean.
	 *
	 * @param sectionName The section name to write from.
	 * @param bean The bean to set the properties on.
	 * @param ignoreUnknownProperties If <jk>true</jk>, don't throw an {@link IllegalArgumentException} if this section
	 * contains a key that doesn't correspond to a setter method.
	 * @param permittedPropertyTypes If specified, only look for setters whose property types
	 * are those listed.  If not specified, use all setters.
	 * @return An object map of the changes made to the bean.
	 * @throws ParseException If parser was not set on this config file or invalid properties were found in the section.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public final ObjectMap writeProperties(String sectionName, Object bean, boolean ignoreUnknownProperties,
			Class<?>...permittedPropertyTypes) throws ParseException, IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		assertFieldNotNull(bean, "bean");
		ObjectMap om = new ObjectMap();
		readLock();
		try {
			Set<String> keys = getSectionKeys(sectionName);
			if (keys == null)
				throw new IllegalArgumentException("Section not found");
			keys = new LinkedHashSet<String>(keys);
			for (Method m : bean.getClass().getMethods()) {
				int mod = m.getModifiers();
				if (isPublic(mod) && (!isStatic(mod)) && m.getName().startsWith("set") && m.getParameterTypes().length == 1) {
					Class<?> pt = m.getParameterTypes()[0];
					if (permittedPropertyTypes == null || permittedPropertyTypes.length == 0 || contains(pt, permittedPropertyTypes)) {
						String propName = Introspector.decapitalize(m.getName().substring(3));
						Object value = getObject(sectionName, propName, pt);
						if (value != null) {
							m.invoke(bean, value);
							om.put(propName, value);
							keys.remove(propName);
						}
					}
				}
			}
			if (! (ignoreUnknownProperties || keys.isEmpty()))
				throw new ParseException("Invalid properties found in config file section ["+sectionName+"]: " + JsonSerializer.DEFAULT_LAX.toString(keys));
			return om;
		} finally {
			readUnlock();
		}
	}

	/**
	 * Shortcut for calling <code>getSectionAsBean(sectionName, c, <jk>false</jk>)</code>.
	 *
	 * @param sectionName The section name to write from.
	 * @param c The bean class to create.
	 * @return A new bean instance.
	 * @throws ParseException
	 */
	public final <T> T getSectionAsBean(String sectionName, Class<T>c) throws ParseException {
		return getSectionAsBean(sectionName, c, false);
	}

	/**
	 * Converts this config file section to the specified bean instance.
	 * <p>
	 * Key/value pairs in the config file section get copied as bean property values to the specified bean class.
	 * <p>
	 * <h6 class='figure'>Example config file</h6>
	 * <p class='bcode'>
	 * 	<cs>[MyAddress]</cs>
	 * 	<ck>name</ck> = <cv>John Smith</cv>
	 * 	<ck>street</ck> = <cv>123 Main Street</cv>
	 * 	<ck>city</ck> = <cv>Anywhere</cv>
	 * 	<ck>state</ck> = <cv>NY</cv>
	 * 	<ck>zip</ck> = <cv>12345</cv>
	 * </p>
	 *
	 * <h6 class='figure'>Example bean</h6>
	 * <p class='bcode'>
	 * 	<jk>public class</jk> Address {
	 * 		public String name, street, city;
	 * 		public StateEnum state;
	 * 		public int zip;
	 * 	}
	 * </p>
	 *
	 * <h6 class='figure'>Example usage</h6>
	 * <p class='bcode'>
	 * 	ConfigFile cf = <jk>new</jk> ConfigFileBuilder().build(<js>"MyConfig.cfg"</js>);
	 * 	Address myAddress = cf.getSectionAsBean(<js>"MySection"</js>, Address.<jk>class</jk>);
	 * </p>
	 *
	 * @param sectionName The section name to write from.
	 * @param c The bean class to create.
	 * @param ignoreUnknownProperties If <jk>false</jk>, throws a {@link ParseException} if the section contains an
	 * entry that isn't a bean property name.
	 * @return A new bean instance.
	 * @throws ParseException
	 */
	public final <T> T getSectionAsBean(String sectionName, Class<T> c, boolean ignoreUnknownProperties)
			throws ParseException {
		assertFieldNotNull(c, "c");
		readLock();
		try {
			BeanMap<T> bm = getBeanSession().newBeanMap(c);
			for (String k : getSectionKeys(sectionName)) {
				BeanPropertyMeta bpm = bm.getPropertyMeta(k);
				if (bpm == null) {
					if (! ignoreUnknownProperties)
						throw new ParseException("Unknown property {0} encountered", k);
				} else {
					bm.put(k, getObject(sectionName + '/' + k, bpm.getClassMeta().getInnerClass()));
				}
			}
			return bm.getBean();
		} finally {
			readUnlock();
		}
	}

	/**
	 * Wraps a config file section inside a Java interface so that values in the section can be read and
	 * write using getters and setters.
	 * <p>
	 * <h6 class='figure'>Example config file</h6>
	 * <p class='bcode'>
	 * 	<cs>[MySection]</cs>
	 * 	<ck>string</ck> = <cv>foo</cv>
	 * 	<ck>int</ck> = <cv>123</cv>
	 * 	<ck>enum</ck> = <cv>ONE</cv>
	 * 	<ck>bean</ck> = <cv>{foo:'bar',baz:123}</cv>
	 * 	<ck>int3dArray</ck> = <cv>[[[123,null],null],null]</cv>
	 * 	<ck>bean1d3dListMap</ck> = <cv>{key:[[[[{foo:'bar',baz:123}]]]]}</cv>
	 * </p>
	 *
	 * <h6 class='figure'>Example interface</h6>
	 * <p class='bcode'>
	 * 	<jk>public interface</jk> MyConfigInterface {
	 *
	 * 		String getString();
	 * 		<jk>void</jk> setString(String x);
	 *
	 * 		<jk>int</jk> getInt();
	 * 		<jk>void</jk> setInt(<jk>int</jk> x);
	 *
	 * 		MyEnum getEnum();
	 * 		<jk>void</jk> setEnum(MyEnum x);
	 *
	 * 		MyBean getBean();
	 * 		<jk>void</jk> setBean(MyBean x);
	 *
	 * 		<jk>int</jk>[][][] getInt3dArray();
	 * 		<jk>void</jk> setInt3dArray(<jk>int</jk>[][][] x);
	 *
	 * 		Map&lt;String,List&lt;MyBean[][][]&gt;&gt; getBean1d3dListMap();
	 * 		<jk>void</jk> setBean1d3dListMap(Map&lt;String,List&lt;MyBean[][][]&gt;&gt; x);
	 * 	}
	 * </p>
	 *
	 * <h6 class='figure'>Example usage</h6>
	 * <p class='bcode'>
	 * 	ConfigFile cf = <jk>new</jk> ConfigFileBuilder().build(<js>"MyConfig.cfg"</js>);
	 *
	 * 	MyConfigInterface ci = cf.getSectionAsInterface(<js>"MySection"</js>, MyConfigInterface.<jk>class</jk>);
	 *
	 * 	<jk>int</jk> myInt = ci.getInt();
	 *
	 * 	ci.setBean(<jk>new</jk> MyBean());
	 *
	 * 	cf.save();
	 * </p>
	 *
	 * @param sectionName The section name to retrieve as an interface proxy.
	 * @param c The proxy interface class.
	 * @return The proxy interface.
	 */
	@SuppressWarnings("unchecked")
	public final <T> T getSectionAsInterface(final String sectionName, final Class<T> c) {
		assertFieldNotNull(c, "c");

		if (! c.isInterface())
			throw new UnsupportedOperationException("Class passed to getSectionAsInterface is not an interface.");

		InvocationHandler h = new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				BeanInfo bi = Introspector.getBeanInfo(c, null);
				for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
					Method rm = pd.getReadMethod(), wm = pd.getWriteMethod();
					if (method.equals(rm))
						return ConfigFile.this.getObject(sectionName, pd.getName(), rm.getGenericReturnType());
					if (method.equals(wm))
						return ConfigFile.this.put(sectionName, pd.getName(), args[0], null, false, false);
				}
				throw new UnsupportedOperationException("Unsupported interface method.  method=[ " + method + " ]");
			}
		};

		return (T)Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, h);
	}

	/**
	 * Returns <jk>true</jk> if this section contains the specified key and the key has a non-blank value.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @return <jk>true</jk> if this section contains the specified key and the key has a non-blank value.
	 */
	public final boolean containsNonEmptyValue(String key) {
		return ! StringUtils.isEmpty(getString(key, null));
	}

	/**
	 * Gets the section with the specified name.
	 *
	 * @param name The section name.
	 * @return The section, or <jk>null</jk> if section does not exist.
	 */
	protected abstract Section getSection(String name);

	/**
	 * Gets the section with the specified name and optionally creates it if it's not there.
	 *
	 * @param name The section name.
	 * @param create Create the section if it's not there.
	 * @return The section, or <jk>null</jk> if section does not exist.
	 * @throws UnsupportedOperationException If config file is read only and section doesn't exist and
	 * <code>create</code> is <jk>true</jk>.
	 */
	protected abstract Section getSection(String name, boolean create);

	/**
	 * Appends a section to this config file if it does not already exist.
	 * <p>
	 * Returns the existing section if it already exists.
	 *
	 * @param name The section name, or <jk>null</jk> for the default section.
	 * @return The appended or existing section.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile addSection(String name);

	/**
	 * Creates or overwrites the specified section.
	 *
	 * @param name The section name, or <jk>null</jk> for the default section.
	 * @param contents The contents of the new section.
	 * @return The appended or existing section.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile setSection(String name, Map<String,String> contents);

	/**
	 * Removes the section with the specified name.
	 *
	 * @param name The name of the section to remove, or <jk>null</jk> for the default section.
	 * @return The removed section, or <jk>null</jk> if named section does not exist.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile removeSection(String name);

	/**
	 * Returns <jk>true</jk> if the encoding flag is set on the specified entry.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @return <jk>true</jk> if the encoding flag is set on the specified entry.
	 */
	public abstract boolean isEncoded(String key);

	/**
	 * Saves this config file to disk.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to save file to disk, or file is not associated with this object.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile save() throws IOException;

	/**
	 * Saves this config file to the specified writer as an INI file.
	 * <p>
	 * The writer will automatically be closed.
	 *
	 * @param out The writer to send the output to.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to send contents to the writer.
	 */
	public final ConfigFile serializeTo(Writer out) throws IOException {
		return serializeTo(out, INI);
	}

	/**
	 * Same as {@link #serializeTo(Writer)}, except allows you to explicitly specify a format.
	 *
	 * @param out The writer to send the output to.
	 * @param format The {@link ConfigFileFormat} of the output.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to send contents to the writer.
	 */
	public abstract ConfigFile serializeTo(Writer out, ConfigFileFormat format) throws IOException;

	/**
	 * Add a listener to this config file to react to modification events.
	 *
	 * @param listener The new listener to add.
	 * @return This object (for method chaining).
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile addListener(ConfigFileListener listener);

	/**
	 * Merges the contents of the specified config file into this config file.
	 * <p>
	 * Pretty much identical to just replacing this config file, but causes the
	 * {@link ConfigFileListener#onChange(ConfigFile, Set)} method to be invoked on differences between the file.
	 *
	 * @param cf The config file whose values should be copied into this config file.
	 * @return This object (for method chaining).
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile merge(ConfigFile cf);

	/**
	 * Returns the config file contents as a string.
	 * <p>
	 * The contents of the string are the same as the contents that would be serialized to disk.
	 */
	@Override /* Object */
	public abstract String toString();

	/**
	 * Returns a wrapped instance of this config file where calls to getters have their values first resolved by the
	 * specified {@link VarResolver}.
	 *
	 * @param vr The {@link VarResolver} for resolving variables in values.
	 * @return This config file wrapped in an instance of {@link ConfigFileWrapped}.
	 */
	public abstract ConfigFile getResolving(VarResolver vr);

	/**
	 * Returns a wrapped instance of this config file where calls to getters have their values first resolved by the
	 * specified {@link VarResolverSession}.
	 *
	 * @param vs The {@link VarResolverSession} for resolving variables in values.
	 * @return This config file wrapped in an instance of {@link ConfigFileWrapped}.
	 */
	public abstract ConfigFile getResolving(VarResolverSession vs);

	/**
	 * Returns a wrapped instance of this config file where calls to getters have their values first resolved by a
	 * default {@link VarResolver}.
	 *
	 * The default {@link VarResolver} is registered with the following {@link Var StringVars}:
	 * <ul class='spaced-list'>
	 * 	<li><code>$S{key}</code>,<code>$S{key,default}</code> - System properties.
	 * 	<li><code>$E{key}</code>,<code>$E{key,default}</code> - Environment variables.
	 * 	<li><code>$C{key}</code>,<code>$C{key,default}</code> - Values in this configuration file.
	 * </ul>
	 *
	 * @return A new config file that resolves string variables.
	 */
	public abstract ConfigFile getResolving();

	/**
	 * Wraps this config file in a {@link Writable} interface that renders it as plain text.
	 *
	 * @return This config file wrapped in a {@link Writable}.
	 */
	public abstract Writable toWritable();

	/**
	 * @return The string var resolver associated with this config file.
	 */
	protected VarResolver getVarResolver() {
		// Only ConfigFileWrapped returns a value.
		return null;
	}

	private static int parseIntWithSuffix(String s) {
		assertFieldNotNull(s, "s");
		int m = 1;
		if (s.endsWith("M")) {
			m = 1024*1024;
			s = s.substring(0, s.length()-1).trim();
		} else if (s.endsWith("K")) {
			m = 1024;
			s = s.substring(0, s.length()-1).trim();
		}
		return Integer.parseInt(s) * m;
	}
}
