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
 * Refer to {@link org.apache.juneau.ini} for more information.
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
	 * @param encoded
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract String put(String sectionName, String sectionKey, Object value, boolean encoded);

	/**
	 * Removes an antry from this config file.
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
	 * Reloads ths config file object from the persisted file contents if the modified timestamp on the file has changed.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If file could not be read, or file is not associated with this object.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile loadIfModified() throws IOException;

	/**
	 * Loads ths config file object from the persisted file contents.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If file could not be read, or file is not associated with this object.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public abstract ConfigFile load() throws IOException;

	/**
	 * Loads ths config file object from the specified reader.
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
	 * Header comments are defined as lines that start with <jk>"#"</jk> immediately preceding a section header <jk>"[section]"</jk>.
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
	 * Returns the serializer in use for this config file.
	 *
	 * @return This object (for method chaining).
	 * @throws SerializeException If no serializer is defined on this config file.
	 */
	protected abstract WriterSerializer getSerializer() throws SerializeException;

	/**
	 * Returns the parser in use for this config file.
	 *
	 * @return This object (for method chaining).
	 * @throws ParseException If no parser is defined on this config file.
	 */
	protected abstract ReaderParser getParser() throws ParseException;

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
	 * If the class type is an array, the value is split on commas and converted individually.
	 * <p>
	 * If you specify a primitive element type using this method (e.g. <code><jk>int</jk>.<jk>class</jk></code>,
	 * 	you will get an array of wrapped objects (e.g. <code>Integer[].<jk>class</jk></code>.
	 *
	 * @param c The class to convert the value to.
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	@SuppressWarnings("unchecked")
	public final <T> T getObject(Class<T> c, String key) throws ParseException {
		assertFieldNotNull(c, "c");
		return getObject(c, key, c.isArray() ? (T)Array.newInstance(c.getComponentType(), 0) : null);
	}

	/**
	 * Gets the entry with the specified key and converts it to the specified value..
	 * <p>
	 * The key can be in one of the following formats...
	 * <ul class='spaced-list'>
	 * 	<li><js>"key"</js> - A value in the default section (i.e. defined above any <code>[section]</code> header).
	 * 	<li><js>"section/key"</js> - A value from the specified section.
	 * </ul>
	 *
	 * @param c The class to convert the value to.
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param def The default value if section or key does not exist.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public final <T> T getObject(Class<T> c, String key, T def) throws ParseException {
		assertFieldNotNull(c, "c");
		assertFieldNotNull(key, "key");
		return getObject(c, getSectionName(key), getSectionKey(key), def);
	}

	/**
	 * Same as {@link #getObject(Class, String, Object)}, but value is referenced through section name and key instead of full key.
	 *
	 * @param c The class to convert the value to.
	 * @param sectionName The section name.  Must not be <jk>null</jk>.
	 * @param sectionKey The section key.  Must not be <jk>null</jk>.
	 * @param def The default value if section or key does not exist.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or the default value if the section or value doesn't exist.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getObject(Class<T> c, String sectionName, String sectionKey, T def) throws ParseException {
		String s = get(sectionName, sectionKey);
		if (s == null)
			return def;
		if (c == String.class)
			return (T)s;
		if (c == Integer.class || c == int.class)
			return (T)(StringUtils.isEmpty(s) ? def : Integer.valueOf(parseIntWithSuffix(s)));
		if (c == Boolean.class || c == boolean.class)
			return (T)(StringUtils.isEmpty(s) ? def : Boolean.valueOf(Boolean.parseBoolean(s)));
		if (c == String[].class) {
			String[] r = StringUtils.isEmpty(s) ? new String[0] : StringUtils.split(s, ',');
			return (T)(r.length == 0 ? def : r);
		}
		if (c.isArray()) {
			Class<?> ce = c.getComponentType();
			if (StringUtils.isEmpty(s))
				return def;
			String[] r = StringUtils.split(s, ',');
			Object o = Array.newInstance(ce, r.length);
			for (int i = 0; i < r.length; i++)
				Array.set(o, i, getParser().parse(r[i], ce));
			return (T)o;
		}
		if (StringUtils.isEmpty(s))
			return def;
		return getParser().parse(s, c);
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
		String[] r = StringUtils.isEmpty(s) ? new String[0] : StringUtils.split(s, ',');
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
	 * Adds or replaces an entry with the specified key with a POJO serialized to a string using the registered serializer.
	 *	<p>
	 *	Equivalent to calling <code>put(key, value, isEncoded(key))</code>.
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param value The new value POJO.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException If serializer could not serialize the value or if a serializer is not registered with this config file.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public final String put(String key, Object value) throws SerializeException {
		return put(key, value, isEncoded(key));
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
	 * 	<li>Arrays and collections of other types are serialized as comma-delimited lists of serialized strings of each entry.
	 * </ul>
	 *
	 * @param key The key.  See {@link #getString(String)} for a description of the key.
	 * @param value The new value.
	 *	@param encoded If <jk>true</jk>, value is encoded by the registered encoder when the config file is persisted to disk.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException If serializer could not serialize the value or if a serializer is not registered with this config file.
	 * @throws UnsupportedOperationException If config file is read only.
	 */
	public final String put(String key, Object value, boolean encoded) throws SerializeException {
		assertFieldNotNull(key, "key");
		if (value == null)
			value = "";
		Class<?> c = value.getClass();
		if (isSimpleType(c))
			return put(getSectionName(key), getSectionKey(key), value.toString(), encoded);
		if (c.isAssignableFrom(Collection.class)) {
			Collection<?> c2 = (Collection<?>)value;
			String[] r = new String[c2.size()];
			int i = 0;
			for (Object o2 : c2) {
				boolean isSimpleType = o2 == null ? true : isSimpleType(o2.getClass());
				r[i++] = (isSimpleType ? Array.get(value, i).toString() : getSerializer().toString(Array.get(value, i)));
			}
			return put(getSectionName(key), getSectionKey(key), StringUtils.join(r, ','), encoded);
		} else if (c.isArray()) {
			boolean isSimpleType = isSimpleType(c.getComponentType());
			String[] r = new String[Array.getLength(value)];
			for (int i = 0; i < r.length; i++) {
				r[i] = (isSimpleType ? Array.get(value, i).toString() : getSerializer().toString(Array.get(value, i)));
			}
			return put(getSectionName(key), getSectionKey(key), StringUtils.join(r, ','), encoded);
		}
		return put(getSectionName(key), getSectionKey(key), getSerializer().toString(value), encoded);
	}

	private final boolean isSimpleType(Class<?> c) {
		return (c == String.class || c.isPrimitive() || c.isAssignableFrom(Number.class) || c == Boolean.class);
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
	 *	@param sectionName The section name to write from.
	 * @param bean The bean to set the properties on.
	 * @param ignoreUnknownProperties If <jk>true</jk>, don't throw an {@link IllegalArgumentException} if this section
	 * 	contains a key that doesn't correspond to a setter method.
	 * @param permittedPropertyTypes If specified, only look for setters whose property types
	 * 	are those listed.  If not specified, use all setters.
	 * @return An object map of the changes made to the bean.
	 * @throws ParseException If parser was not set on this config file or invalid properties were found in the section.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public final ObjectMap writeProperties(String sectionName, Object bean, boolean ignoreUnknownProperties, Class<?>...permittedPropertyTypes) throws ParseException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
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
					if (permittedPropertyTypes == null || permittedPropertyTypes.length == 0 || ArrayUtils.contains(pt, permittedPropertyTypes)) {
						String propName = Introspector.decapitalize(m.getName().substring(3));
						Object value = getObject(pt, sectionName, propName, null);
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
	 * Shortcut for calling <code>asBean(sectionName, c, <jk>false</jk>)</code>.
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
	 *
	 *	@param sectionName The section name to write from.
	 * @param c The bean class to create.
	 * @param ignoreUnknownProperties If <jk>false</jk>, throws a {@link ParseException} if
	 * 	the section contains an entry that isn't a bean property name.
	 * @return A new bean instance.
	 * @throws ParseException
	 */
	public final <T> T getSectionAsBean(String sectionName, Class<T> c, boolean ignoreUnknownProperties) throws ParseException {
		assertFieldNotNull(c, "c");
		readLock();
		try {
			BeanMap<T> bm = getParser().getBeanContext().createSession().newBeanMap(c);
			for (String k : getSectionKeys(sectionName)) {
				BeanPropertyMeta bpm = bm.getPropertyMeta(k);
				if (bpm == null) {
					if (! ignoreUnknownProperties)
						throw new ParseException("Unknown property {0} encountered", k);
				} else {
					bm.put(k, getObject(bpm.getClassMeta().getInnerClass(), sectionName + '/' + k));
				}
			}
			return bm.getBean();
		} finally {
			readUnlock();
		}
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
	 * @throws UnsupportedOperationException If config file is read only and section doesn't exist and <code>create</code> is <jk>true</jk>.
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
	 * Same as {@link #serializeTo(Writer)}, except allows you to explicitely specify a format.
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
	 * Pretty much identical to just replacing this config file, but
	 * 	causes the {@link ConfigFileListener#onChange(ConfigFile, Set)} method to be invoked
	 * 	on differences between the file.
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
	 * Returns a wrapped instance of this config file where calls to getters
	 * 	have their values first resolved by the specified {@link VarResolver}.
	 *
	 * @param vr The {@link VarResolver} for resolving variables in values.
	 * @return This config file wrapped in an instance of {@link ConfigFileWrapped}.
	 */
	public abstract ConfigFile getResolving(VarResolver vr);

	/**
	 * Returns a wrapped instance of this config file where calls to getters
	 * 	have their values first resolved by the specified {@link VarResolverSession}.
	 *
	 * @param vs The {@link VarResolverSession} for resolving variables in values.
	 * @return This config file wrapped in an instance of {@link ConfigFileWrapped}.
	 */
	public abstract ConfigFile getResolving(VarResolverSession vs);

	/**
	 * Returns a wrapped instance of this config file where calls to getters have their values
	 * 	first resolved by a default {@link VarResolver}.
	 *
	 *  The default {@link VarResolver} is registered with the following {@link Var StringVars}:
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


	private int parseIntWithSuffix(String s) {
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
