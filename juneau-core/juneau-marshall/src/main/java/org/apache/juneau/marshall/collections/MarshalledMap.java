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
package org.apache.juneau.marshall.collections;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.toStringArray;
import static org.apache.juneau.commons.utils.PredicateUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.objecttools.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;

/**
 * Neutral, marshaller-agnostic map of string-to-object entries.
 *
 * <p>
 * This class is the base for {@link JsonMap} and the per-marshaller {@code XMap} variants. It carries
 * all marshaller-neutral functionality such as typed accessors, fluent setters, bean integration,
 * and {@link ObjectRest}-based path navigation. The default {@link #toString()} comes from
 * {@link LinkedHashMap}; subclasses should override it to produce the appropriate marshalled form.
 *
 * <p>
 * Note that the use of this class is optional. The serializers will accept any objects that implement the
 * {@link Map} interface. But this class provides useful additional functionality when working with
 * data models constructed from Java Collections Framework objects.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 */
@SuppressWarnings({
	"java:S2160" // equals() / hashCode() inherited from LinkedHashMap; map equality is element-based
})
public class MarshalledMap extends LinkedHashMap<String,Object> {

	@SuppressWarnings({
		"java:S2160" // equals() / hashCode() inherited from MarshalledMap; map equality is element-based
	})
	private static class UnmodifiableMarshalledMap extends MarshalledMap {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings({
			"synthetic-access" // Access to outer class members is intentional
		})
		UnmodifiableMarshalledMap(MarshalledMap contents) {
			if (nn(contents))
				contents.forEach(super::put);
		}

		@Override
		public void clear() {
			throw unsupportedOpReadOnly();
		}

		@Override
		public Object compute(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public Set<Map.Entry<String,Object>> entrySet() {
			return Collections.unmodifiableSet(super.entrySet());
		}

		@Override
		public boolean isUnmodifiable() { return true; }

		@Override
		public Set<String> keySet() {
			return Collections.unmodifiableSet(super.keySet());
		}

		@Override
		public Object merge(String key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public Object put(String key, Object val) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public void putAll(Map<? extends String, ?> m) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public Object putIfAbsent(String key, Object value) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public Object remove(Object key) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public boolean remove(Object key, Object value) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public Object replace(String key, Object value) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public boolean replace(String key, Object oldValue, Object newValue) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public Collection<Object> values() {
			return Collections.unmodifiableCollection(super.values());
		}
	}

	private static final long serialVersionUID = 1L;
	private static final Type[] EMPTY_TYPE_ARRAY = {};

	/**
	 * An empty read-only MarshalledMap.
	 *
	 * @serial exclude
	 */
	@SuppressWarnings({
		"java:S2386" // Public static final field accessed externally, cannot be protected
	})
	public static final MarshalledMap EMPTY_MAP = new MarshalledMap() {

		private static final long serialVersionUID = 1L;

		@Override /* Overridden from Map */
		public Set<Map.Entry<String,Object>> entrySet() {
			return Collections.<String,Object>emptyMap().entrySet();
		}

		@Override /* Overridden from Map */
		public Set<String> keySet() {
			return Collections.<String,Object>emptyMap().keySet();
		}

		@Override /* Overridden from Map */
		public Object put(String key, Object value) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Map */
		public Object remove(Object key) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Map */
		public Collection<Object> values() {
			return mape().values();
		}
	};

	/**
	 * Construct an empty map.
	 *
	 * @return An empty map.
	 */
	public static MarshalledMap create() {
		return new MarshalledMap();
	}

	/**
	 * Construct an empty filtered map.
	 *
	 * @return An empty map.
	 */
	public static MarshalledMap filteredMap() {
		return create().filtered();
	}

	/**
	 * Construct a filtered map initialized with the specified key/value pairs.
	 *
	 * @param keyValuePairs A list of key/value pairs to add to this map.
	 * @return A new map, never <jk>null</jk>.
	 */
	public static MarshalledMap filteredMap(Object...keyValuePairs) {
		return new MarshalledMap(keyValuePairs).filtered();
	}

	/**
	 * Construct a map initialized with the specified map.
	 *
	 * @param values
	 * 	The map to copy.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Keys will be converted to strings using {@link Object#toString()}.
	 * @return A new map or <jk>null</jk> if the map was <jk>null</jk>.
	 */
	public static MarshalledMap of(Map<?,?> values) {
		return values == null ? null : new MarshalledMap(values);
	}

	/**
	 * Construct a map initialized with the specified key/value pairs.
	 *
	 * @param keyValuePairs A list of key/value pairs to add to this map.
	 * @return A new map, never <jk>null</jk>.
	 */
	public static MarshalledMap of(Object...keyValuePairs) {
		return new MarshalledMap(keyValuePairs);
	}

	/**
	 * Construct a map initialized by parsing the specified string with the specified parser.
	 *
	 * @param in The input being parsed.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return A new map (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static MarshalledMap ofString(CharSequence in, Parser p) throws ParseException {
		return in == null ? new MarshalledMap() : new MarshalledMap(in, p);
	}

	/**
	 * Construct a map initialized by parsing the specified reader with the specified parser.
	 *
	 * @param in The reader containing the input being parsed.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return A new map (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static MarshalledMap ofString(java.io.Reader in, Parser p) throws ParseException {
		return in == null ? new MarshalledMap() : new MarshalledMap(in, p);
	}

	/*
	 * If c1 is a child of c2 or the same as c2, returns c1.
	 * Otherwise, returns c2.
	 */
	private static ClassMeta<?> getNarrowedClassMeta(ClassMeta<?> c1, ClassMeta<?> c2) {
		if (c2 == null || c2.isAssignableFrom(c1.inner()))
			return c1;
		return c2;
	}

	private transient MarshallingSession session;
	private transient Map<String,Object> inner;

	private transient ObjectRest objectRest;

	private transient Predicate<Object> valueFilter = x -> true;

	/**
	 * Construct an empty map.
	 */
	public MarshalledMap() {}

	/**
	 * Construct an empty map with the specified bean context.
	 *
	 * @param session The bean session to use for creating beans.
	 */
	public MarshalledMap(MarshallingSession session) {
		this.session = session;
	}

	/**
	 * Construct a map initialized by parsing the specified string with the specified parser.
	 *
	 * @param in
	 * 	The input being parsed.
	 * 	<br>Can be <jk>null</jk>.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>Must not be <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public MarshalledMap(CharSequence in, Parser p) throws ParseException {
		this(assertArgNotNull("p", p).getMarshallingContext().getSession());
		if (ne(in))
			p.parseIntoMap(in, this, bs().string(), bs().object());
	}

	/**
	 * Construct a map initialized with the specified map.
	 *
	 * @param in
	 * 	The map to copy.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Keys will be converted to strings using {@link Object#toString()}.
	 */
	public MarshalledMap(Map<?,?> in) {
		this();
		if (nn(in))
			in.forEach((k, v) -> put(k.toString(), v));
	}

	/**
	 * Construct a map initialized with the specified key/value pairs.
	 *
	 * @param keyValuePairs A list of key/value pairs to add to this map.
	 */
	public MarshalledMap(Object...keyValuePairs) {
		assertArg(keyValuePairs.length % 2 == 0, "Odd number of parameters passed into MarshalledMap(Object...)");
		for (var i = 0; i < keyValuePairs.length; i += 2)
			put(s(keyValuePairs[i]), keyValuePairs[i + 1]);
	}

	/**
	 * Construct a map initialized by parsing the specified reader with the specified parser.
	 *
	 * @param in
	 * 	The reader containing the input being parsed.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>Must not be <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public MarshalledMap(java.io.Reader in, Parser p) throws ParseException {
		this(assertArgNotNull("p", p).getMarshallingContext().getSession());
		p.parseIntoMap(in, this, bs().string(), bs().object());
	}

	/**
	 * Appends all the entries in the specified map to this map.
	 *
	 * @param values The map to copy.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public MarshalledMap append(Map<String,Object> values) {
		if (nn(values))
			super.putAll(values);
		return this;
	}

	/**
	 * Adds an entry to this map.
	 *
	 * @param key The key.
	 * @param value The value.
	 * @return This object.
	 */
	public MarshalledMap append(String key, Object value) {
		put(key, value);
		return this;
	}

	/**
	 * Adds the first value that matches the specified predicate.
	 *
	 * @param <T> The value types.
	 * @param test The predicate to match against.
	 * @param key The key.
	 * @param values The values to test.
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked", // Generic varargs is safe here: values are only iterated, not stored into a typed array.
		"varargs"
	})
	public <T> MarshalledMap appendFirst(Predicate<T> test, String key, T...values) {
		for (var v : values)
			if (test(test, v))
				return append(key, v);
		return this;
	}

	/**
	 * Add if flag is <jk>true</jk>.
	 *
	 * @param flag The flag to check.
	 * @param key The key.
	 * @param value The value.
	 * @return This object.
	 */
	public MarshalledMap appendIf(boolean flag, String key, Object value) {
		if (flag)
			append(key, value);
		return this;
	}

	/**
	 * Add if predicate matches value.
	 *
	 * @param <T> The value type.
	 * @param test The predicate to match against.
	 * @param key The key.
	 * @param value The value.
	 * @return This object.
	 */
	public <T> MarshalledMap appendIf(Predicate<T> test, String key, T value) {
		return appendIf(test(test, value), key, value);
	}

	/**
	 * Adds a value in this map if the entry does not exist or the current value is <jk>null</jk>.
	 *
	 * @param key The map key.
	 * @param value The value to set if the current value does not exist or is <jk>null</jk>.
	 * @return This object.
	 */
	public MarshalledMap appendIfAbsent(String key, Object value) {
		return appendIfAbsentIf(x -> true, key, value);
	}

	/**
	 * Adds a value in this map if the entry does not exist or the current value is <jk>null</jk> and the value matches the specified predicate.
	 *
	 * @param <T> The value type.
	 * @param predicate The predicate to test the value with.
	 * @param key The map key.
	 * @param value The value to set if the current value does not exist or is <jk>null</jk>.
	 * @return This object.
	 */
	public <T> MarshalledMap appendIfAbsentIf(Predicate<T> predicate, String key, T value) {
		Object o = get(key);
		if (o == null && predicate.test(value))
			put(key, value);
		return this;
	}

	/**
	 * Serializes this map to a string using the specified serializer.
	 *
	 * @param serializer The serializer to use to convert this object to a string.
	 * @return This object serialized as a string.
	 */
	public String toString(WriterSerializer serializer) {
		return serializer.toString(this);
	}

	/**
	 * Converts this map into an object of the specified type.
	 *
	 * <p>
	 * If this map contains a <js>"_type"</js> entry, it must be the same as or a subclass of the <c>type</c>.
	 *
	 * @param <T> The class type to convert this map object to.
	 * @param type The class type to convert this map object to.
	 * @return The new object.
	 * @throws ClassCastException
	 * 	If the <js>"_type"</js> entry is present and not assignable from <c>type</c>
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts
	})
	public <T> T cast(Class<T> type) {
		MarshallingSession bs = bs();
		ClassMeta<?> c2 = bs.getClassMeta(type);
		String typePropertyName = bs.getBeanTypePropertyName(c2);
		ClassMeta<?> c1 = bs.getBeanRegistry().getClassMeta((String)get(typePropertyName));
		ClassMeta<?> c = c1 == null ? c2 : narrowClassMeta(c1, c2);
		if (c.isObject())
			return (T)this;
		return (T)cast2(c);
	}

	/**
	 * Same as {@link #cast(Class)}, except allows you to specify a {@link ClassMeta} parameter.
	 *
	 * @param <T> The class type to convert this map object to.
	 * @param cm The class type to convert this map object to.
	 * @return The new object.
	 * @throws ClassCastException
	 * 	If the <js>"_type"</js> entry is present and not assignable from <c>type</c>
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for ClassMeta<T>
	})
	public <T> T cast(ClassMeta<T> cm) {
		MarshallingSession bs = bs();
		var c1 = bs.getBeanRegistry().getClassMeta((String)get(bs.getBeanTypePropertyName(cm)));
		var c = narrowClassMeta(c1, cm);
		return (T)cast2(c);
	}

	@Override /* Overridden from Map */
	public boolean containsKey(Object key) {
		if (super.containsKey(key))
			return true;
		if (nn(inner))
			return inner.containsKey(key);
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the map contains the specified entry and the value is not null nor an empty string.
	 *
	 * <p>
	 * Always returns <jk>false</jk> if the value is not a {@link CharSequence}.
	 *
	 * @param key The key.
	 * @return <jk>true</jk> if the map contains the specified entry and the value is not null nor an empty string.
	 */
	public boolean containsKeyNotEmpty(String key) {
		Object val = get(key);
		if (val == null)
			return false;
		if (val instanceof CharSequence val2)
			return isNotBlank(val2);
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this map contains the specified key, ignoring the inner map if it exists.
	 *
	 * @param key The key to look up.
	 * @return <jk>true</jk> if this map contains the specified key.
	 */
	public boolean containsOuterKey(Object key) {
		return super.containsKey(key);
	}

	/**
	 * Similar to {@link #remove(Object) remove(Object)}, but the key is a slash-delimited path used to traverse entries
	 * in this POJO.
	 *
	 * <p>
	 * This method uses the {@link ObjectRest} class to perform the lookup, so the map can contain any of the various
	 * class types that the {@link ObjectRest} class supports (e.g. beans, collections, arrays).
	 *
	 * @param path The path to the entry.
	 * @return The previous value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public Object deleteAt(String path) {
		return getObjectRest().delete(path);
	}

	@Override /* Overridden from Map */
	public Set<Map.Entry<String,Object>> entrySet() {
		if (inner == null)
			return super.entrySet();

		final Set<String> keySet = keySet();
		final Iterator<String> keys = keySet.iterator();

		return new AbstractSet<>() {

			@Override /* Overridden from Iterable */
			public Iterator<Map.Entry<String,Object>> iterator() {

				return new Iterator<>() {

					@Override /* Overridden from Iterator */
					public boolean hasNext() {
						return keys.hasNext();
					}

					@Override /* Overridden from Iterator */
					public Map.Entry<String,Object> next() {
						return new Map.Entry<>() {
							String key = keys.next();

							@Override /* Overridden from Map.Entry */
							public String getKey() { return key; }

							@Override /* Overridden from Map.Entry */
							public Object getValue() { return get(key); }

							@Override /* Overridden from Map.Entry */
							public Object setValue(Object object) {
								return put(key, object);
							}
						};
					}

					@Override /* Overridden from Iterator */
					public void remove() {
						throw unsupportedOpReadOnly();
					}
				};
			}

			@Override /* Overridden from Set */
			public int size() {
				return keySet.size();
			}
		};
	}

	/**
	 * Returns a copy of this map without the specified keys.
	 *
	 * @param keys The keys of the entries not to copy.
	 * @return A new map without the keys and values from this map.
	 */
	public MarshalledMap exclude(String...keys) {
		var m2 = new MarshalledMap();
		this.forEach((k, v) -> {
			var exclude = false;
			for (var kk : keys)
				if (kk.equals(k))
					exclude = true;
			if (! exclude)
				m2.put(k, v);
		});
		return m2;
	}

	/**
	 * Enables filtering based on default values.
	 *
	 * <p>
	 * Any of the following types will be ignored when set as values in this map:
	 * <ul>
	 * 	<li><jk>null</jk>
	 * 	<li><jk>false</jk>
	 * 	<li><c>-1</c> (any Number type)
	 * 	<li>Empty arrays/collections/maps.
	 * </ul>
	 * @return This object.
	 */
	public MarshalledMap filtered() {
		// @formatter:off
		return filtered(x -> ! (
			x == null
			|| (x instanceof Boolean x2 && x2.equals(false))
			|| (x instanceof Number x3 && x3.intValue() == -1)
		|| (isArray(x) && Array.getLength(x) == 0)
		|| (x instanceof Map<?,?> x2 && x2.isEmpty())
		|| (x instanceof Collection<?> x3 && x3.isEmpty())
		));
		// @formatter:on
	}

	/**
	 * Enables filtering based on a predicate test.
	 *
	 * <p>
	 * If the predicate evaluates to <jk>false</jk> on values added to this map, the entry will be skipped.
	 *
	 * @param value The value tester predicate.
	 * @return This object.
	 */
	public MarshalledMap filtered(Predicate<Object> value) {
		valueFilter = value;
		return this;
	}

	/**
	 * Returns the value for the first key in the list that has an entry in this map.
	 *
	 * <p>
	 * Casts or converts the value to the specified class type.
	 *
	 * <p>
	 * See {@link MarshallingSession#convertToType(Object, ClassMeta)} for the list of valid data conversions.
	 *
	 * @param type The class type to convert the value to.
	 * @param <T> The class type to convert the value to.
	 * @param keys The keys to look up in order.
	 * @return The value of the first entry whose key exists, or <jk>null</jk> if none of the keys exist in this map.
	 */
	public <T> T find(Class<T> type, String...keys) {
		for (var key : keys)
			if (containsKey(key))
				return get(key, type);
		return null;
	}

	/**
	 * Returns the value for the first key in the list that has an entry in this map.
	 *
	 * @param keys The keys to look up in order.
	 * @return The value of the first entry whose key exists, or <jk>null</jk> if none of the keys exist in this map.
	 */
	public Object find(String...keys) {
		for (var key : keys)
			if (containsKey(key))
				return get(key);
		return null;
	}

	/**
	 * Returns the first entry that exists converted to a {@link Boolean}.
	 *
	 * @param keys The list of keys to look for.
	 * @return
	 * 	The converted value of the first key in the list that has an entry in this map, or <jk>null</jk> if the map
	 * 	contains no mapping for any of the keys.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Boolean findBoolean(String...keys) {
		return find(Boolean.class, keys);
	}

	/**
	 * Returns the first entry that exists converted to an {@link Integer}.
	 *
	 * @param keys The list of keys to look for.
	 * @return
	 * 	The converted value of the first key in the list that has an entry in this map, or <jk>null</jk> if the map
	 * 	contains no mapping for any of the keys.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Integer findInt(String...keys) {
		return find(Integer.class, keys);
	}

	/**
	 * Searches for the specified key in this map ignoring case.
	 *
	 * @param key
	 * 	The key to search for.
	 * 	For performance reasons, it's preferable that the key be all lowercase.
	 * @return The key, or <jk>null</jk> if map does not contain this key.
	 */
	public String findKeyIgnoreCase(String key) {
		for (var k : keySet())
			if (eqic(key, k))
				return k;
		return null;
	}

	/**
	 * Returns the first entry that exists converted to a {@link MarshalledList}.
	 *
	 * @param keys The list of keys to look for.
	 * @return
	 * 	The converted value of the first key in the list that has an entry in this map, or <jk>null</jk> if the map
	 * 	contains no mapping for any of the keys.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public MarshalledList findList(String...keys) {
		return find(MarshalledList.class, keys);
	}

	/**
	 * Returns the first entry that exists converted to a {@link Long}.
	 *
	 * @param keys The list of keys to look for.
	 * @return
	 * 	The converted value of the first key in the list that has an entry in this map, or <jk>null</jk> if the map
	 * 	contains no mapping for any of the keys.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Long findLong(String...keys) {
		return find(Long.class, keys);
	}

	/**
	 * Returns the first entry that exists converted to a {@link MarshalledMap}.
	 *
	 * @param keys The list of keys to look for.
	 * @return
	 * 	The converted value of the first key in the list that has an entry in this map, or <jk>null</jk> if the map
	 * 	contains no mapping for any of the keys.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public MarshalledMap findMap(String...keys) {
		return find(MarshalledMap.class, keys);
	}

	/**
	 * Returns the first entry that exists converted to a {@link String}.
	 *
	 * @param keys The list of keys to look for.
	 * @return
	 * 	The converted value of the first key in the list that has an entry in this map, or <jk>null</jk> if the map
	 * 	contains no mapping for any of the keys.
	 */
	public String findString(String...keys) {
		return find(String.class, keys);
	}

	@Override /* Overridden from Map */
	public Object get(Object key) {
		Object o = super.get(key);
		if (o == null && nn(inner))
			o = inner.get(key);
		return o;
	}

	/**
	 * Same as {@link Map#get(Object) get()}, but casts or converts the value to the specified class type.
	 *
	 * <p>
	 * See {@link MarshallingSession#convertToType(Object, ClassMeta)} for the list of valid data conversions.
	 *
	 * @param key The key.
	 * @param <T> The class type returned.
	 * @param type The class type returned.
	 * @return The value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public <T> T get(String key, Class<T> type) {
		return getWithDefault(key, (T)null, type);
	}

	/**
	 * Same as {@link #get(String,Class)}, but allows for complex data types consisting of collections or maps.
	 *
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <p>
	 * See {@link MarshallingSession#convertToType(Object, ClassMeta)} for the list of valid data conversions.
	 *
	 * @param key The key.
	 * @param <T> The class type returned.
	 * @param type The class type returned.
	 * @param args The class type parameters.
	 * @return The value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public <T> T get(String key, Type type, Type...args) {
		return getWithDefault(key, null, type, args);
	}

	/**
	 * Same as {@link #get(String,Class) get(String,Class)}, but the key is a slash-delimited path used to traverse
	 * entries in this POJO.
	 *
	 * @param path The path to the entry.
	 * @param type The class type.
	 *
	 * @param <T> The class type.
	 * @return The value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public <T> T getAt(String path, Class<T> type) {
		return getObjectRest().get(path, type);
	}

	/**
	 * Same as {@link #getAt(String,Class)}, but allows for conversion to complex maps and collections.
	 *
	 * @param path The path to the entry.
	 * @param type The class type.
	 * @param args The class parameter types.
	 *
	 * @param <T> The class type.
	 * @return The value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public <T> T getAt(String path, Type type, Type...args) {
		return getObjectRest().get(path, type, args);
	}

	/**
	 * Returns the specified entry value converted to a {@link Boolean}.
	 *
	 * @param key The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Boolean getBoolean(String key) {
		return get(key, Boolean.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Boolean}.
	 *
	 * @param key The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Boolean getBoolean(String key, Boolean defVal) {
		return getWithDefault(key, defVal, Boolean.class);
	}

	/**
	 * Returns the class type of the object at the specified index.
	 *
	 * @param key The key into this map.
	 * @return
	 * 	The data type of the object at the specified key, or <jk>null</jk> if the value is null or does not exist.
	 */
	@SuppressWarnings({
		"java:S1452"  // Wildcard required - ClassMeta<?> for value type metadata
	})
	public ClassMeta<?> getClassMeta(String key) {
		return bs().getClassMetaForObject(get(key));
	}

	/**
	 * Returns the first key in the map.
	 *
	 * @return The first key in the map, or <jk>null</jk> if the map is empty.
	 */
	public String getFirstKey() { return first(keySet()).orElse(null); }

	/**
	 * Returns the specified entry value converted to an {@link Integer}.
	 *
	 * @param key The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Integer getInt(String key) {
		return get(key, Integer.class);
	}

	/**
	 * Returns the specified entry value converted to an {@link Integer}.
	 *
	 * @param key The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Integer getInt(String key, Integer defVal) {
		return getWithDefault(key, defVal, Integer.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link MarshalledList}.
	 *
	 * @param key The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public MarshalledList getList(String key) {
		return get(key, MarshalledList.class);
	}

	/**
	 * Same as {@link #getList(String)} but creates a new empty {@link MarshalledList} if it doesn't already exist.
	 *
	 * @param key The key.
	 * @param createIfNotExists If mapping doesn't already exist, create one with an empty {@link MarshalledList}.
	 * @return The converted value, or an empty value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public MarshalledList getList(String key, boolean createIfNotExists) {
		var m = getWithDefault(key, null, MarshalledList.class);
		if (m == null && createIfNotExists) {
			m = new MarshalledList();
			put(key, m);
		}
		return m;
	}

	/**
	 * Same as {@link #getList(String, MarshalledList)} except converts the elements to the specified types.
	 *
	 * @param <E> The element type.
	 * @param key The key.
	 * @param elementType The element type class.
	 * @param def The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public <E> List<E> getList(String key, Class<E> elementType, List<E> def) {
		Object o = get(key);
		if (o == null)
			return def;
		return bs().convertToType(o, List.class, elementType);
	}

	/**
	 * Returns the specified entry value converted to a {@link MarshalledList}.
	 *
	 * @param key The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public MarshalledList getList(String key, MarshalledList defVal) {
		return getWithDefault(key, defVal, MarshalledList.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Long}.
	 *
	 * @param key The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Long getLong(String key) {
		return get(key, Long.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Long}.
	 *
	 * @param key The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Long getLong(String key, Long defVal) {
		return getWithDefault(key, defVal, Long.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link Map}.
	 *
	 * @param key The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public MarshalledMap getMap(String key) {
		return get(key, MarshalledMap.class);
	}

	/**
	 * Same as {@link #getMap(String)} but creates a new empty {@link MarshalledMap} if it doesn't already exist.
	 *
	 * @param key The key.
	 * @param createIfNotExists If mapping doesn't already exist, create one with an empty {@link MarshalledMap}.
	 * @return The converted value, or an empty value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public MarshalledMap getMap(String key, boolean createIfNotExists) {
		var m = getWithDefault(key, null, MarshalledMap.class);
		if (m == null && createIfNotExists) {
			m = new MarshalledMap();
			put(key, m);
		}
		return m;
	}

	/**
	 * Same as {@link #getMap(String, MarshalledMap)} except converts the keys and values to the specified types.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param key The key.
	 * @param keyType The key type class.
	 * @param valType The value type class.
	 * @param def The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public <K,V> Map<K,V> getMap(String key, Class<K> keyType, Class<V> valType, Map<K,V> def) {
		Object o = get(key);
		if (o == null)
			return def;
		return bs().convertToType(o, Map.class, keyType, valType);
	}

	/**
	 * Returns the specified entry value converted to a {@link MarshalledMap}.
	 *
	 * @param key The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public MarshalledMap getMap(String key, MarshalledMap defVal) {
		return getWithDefault(key, defVal, MarshalledMap.class);
	}

	/**
	 * Returns the {@link MarshallingSession} currently associated with this map.
	 *
	 * @return The {@link MarshallingSession} currently associated with this map.
	 */
	public MarshallingSession getMarshallingSession() { return session; }

	/**
	 * Returns the specified entry value converted to a {@link String}.
	 *
	 * @param key The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 */
	public String getString(String key) {
		return get(key, String.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link String}.
	 *
	 * @param key The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 */
	public String getString(String key, String defVal) {
		return getWithDefault(key, defVal, String.class);
	}

	/**
	 * Returns the specified entry value converted to a {@link String}[] array.
	 *
	 * @param key The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 */
	public String[] getStringArray(String key) {
		return getStringArray(key, null);
	}

	/**
	 * Same as {@link #getStringArray(String)} but returns a default value if the value cannot be found.
	 *
	 * @param key The map key.
	 * @param def The default value if value is not found.
	 * @return The value converted to a string array.
	 */
	public String[] getStringArray(String key, String[] def) {
		Object s = get(key, Object.class);
		if (s == null)
			return def;
		String[] r = null;
		if (s instanceof Collection<?> s2)
			r = toStringArray(s2);
		else if (s instanceof String[] s2)
			r = s2;
		else if (s instanceof Object[] s3)
			r = toStringArray(l(s3));
		else
			r = StringUtils.splita(s(s));
		return (r == null || r.length == 0) ? def : r;
	}

	/**
	 * Same as {@link Map#get(Object) get()}, but converts the raw value to the specified class type using the specified
	 * POJO swap.
	 *
	 * @param key The key.
	 * @param objectSwap The swap class used to convert the raw type to a transformed type.
	 * @param <T> The transformed class type.
	 * @return The value, or <jk>null</jk> if the entry doesn't exist.
	 * @throws ParseException Malformed input encountered.
	 */
	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic type handling for generic type handling
		"unchecked", // Type erasure requires unchecked casts in ObjectSwap operations
	})
	public <T> T getSwapped(String key, ObjectSwap<T,?> objectSwap) throws ParseException {
		try {
			Object o = super.get(key);
			if (o == null)
				return null;
			ObjectSwap swap = objectSwap;
			return (T)swap.unswap(bs(), o, null);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Same as {@link Map#get(Object) get()}, but returns the default value if the key could not be found.
	 *
	 * @param key The key.
	 * @param def The default value if the entry doesn't exist.
	 * @return The value, or the default value if the entry doesn't exist.
	 */
	public Object getWithDefault(String key, Object def) {
		Object o = get(key);
		return (o == null ? def : o);
	}

	/**
	 * Same as {@link #get(String,Class)} but returns a default value if the value does not exist.
	 *
	 * @param key The key.
	 * @param def The default value.  Can be <jk>null</jk>.
	 * @param <T> The class type returned.
	 * @param type The class type returned.
	 * @return The value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public <T> T getWithDefault(String key, T def, Class<T> type) {
		return getWithDefault(key, def, type, EMPTY_TYPE_ARRAY);
	}

	/**
	 * Same as {@link #get(String,Type,Type...)} but returns a default value if the value does not exist.
	 *
	 * @param key The key.
	 * @param def The default value.  Can be <jk>null</jk>.
	 * @param <T> The class type returned.
	 * @param type The class type returned.
	 * @param args The class type parameters.
	 * @return The value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public <T> T getWithDefault(String key, T def, Type type, Type...args) {
		Object o = get(key);
		if (o == null)
			return def;
		T t = bs().convertToType(o, type, args);
		return t == null ? def : t;
	}

	/**
	 * Returns a copy of this map with only the specified keys.
	 *
	 * @param keys The keys of the entries to copy.
	 * @return A new map with just the keys and values from this map.
	 */
	public MarshalledMap include(String...keys) {
		var m2 = new MarshalledMap();
		this.forEach((k, v) -> {
			for (var kk : keys)
				if (kk.equals(k))
					m2.put(kk, v);
		});
		return m2;
	}

	/**
	 * Set an inner map in this map to allow for chained get calls.
	 *
	 * <p>
	 * If {@link #get(Object)} returns <jk>null</jk>, then {@link #get(Object)} will be called on the inner map.
	 *
	 * @param inner
	 * 	The inner map.
	 * 	Can be <jk>null</jk> to remove the inner map from an existing map.
	 * @return This object.
	 */
	public MarshalledMap inner(Map<String,Object> inner) {
		this.inner = inner;
		return this;
	}

	/**
	 * Shortcut for <code>getWithDefault(key, defVal, Boolean.<jk>class</jk>)</code>.
	 *
	 * @param key The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public boolean is(String key, boolean defVal) {
		Boolean result = getWithDefault(key, defVal, Boolean.class);
		return result != null ? result : defVal;
	}

	/**
	 * Returns <jk>true</jk> if this map is unmodifiable.
	 *
	 * @return <jk>true</jk> if this map is unmodifiable.
	 */
	public boolean isUnmodifiable() { return false; }

	/**
	 * The opposite of {@link #removeAll(String...)}.
	 *
	 * <p>
	 * Discards all keys from this map that aren't in the specified list.
	 *
	 * @param keys The keys to keep.
	 * @return This map.
	 */
	public MarshalledMap keepAll(String...keys) {
		for (var i = keySet().iterator(); i.hasNext();) {
			var remove = true;
			var key = i.next();
			for (var k : keys) {
				if (k.equals(key)) {
					remove = false;
					break;
				}
			}
			if (remove)
				i.remove();
		}
		return this;
	}

	@Override /* Overridden from Map */
	public Set<String> keySet() {
		if (inner == null)
			return super.keySet();
		Set<String> s = set();
		s.addAll(inner.keySet());
		s.addAll(super.keySet());
		return s;
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return super.equals(o);
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * Returns a modifiable copy of this map if it's unmodifiable.
	 *
	 * @return A modifiable copy of this map if it's unmodifiable, or this map if it is already modifiable.
	 */
	public MarshalledMap modifiable() {
		if (isUnmodifiable())
			return new MarshalledMap(this);
		return this;
	}

	/**
	 * Similar to {@link #putAt(String,Object) putAt(String,Object)}, but used to append to collections and arrays.
	 *
	 * <p>
	 * This method uses the {@link ObjectRest} class to perform the lookup, so the map can contain any of the various
	 * class types that the {@link ObjectRest} class supports (e.g. beans, collections, arrays).
	 *
	 * @param path The path to the entry.
	 * @param o The new value.
	 * @return The previous value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public Object postAt(String path, Object o) {
		return getObjectRest().post(path, o);
	}

	@Override
	public Object put(String key, Object value) {
		if (valueFilter.test(value))
			return super.put(key, value);
		return null;
	}

	/**
	 * Same as <c>put(String,Object)</c>, but the key is a slash-delimited path used to traverse entries in this
	 * POJO.
	 *
	 * <p>
	 * This method uses the {@link ObjectRest} class to perform the lookup, so the map can contain any of the various
	 * class types that the {@link ObjectRest} class supports (e.g. beans, collections, arrays).
	 *
	 * @param path The path to the entry.
	 * @param o The new value.
	 * @return The previous value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public Object putAt(String path, Object o) {
		return getObjectRest().put(path, o);
	}

	/**
	 * Convenience method for removing several keys at once.
	 *
	 * @param keys The list of keys to remove.
	 */
	public void removeAll(Collection<String> keys) {
		keys.forEach(this::remove);
	}

	/**
	 * Convenience method for removing several keys at once.
	 *
	 * @param keys The list of keys to remove.
	 */
	public void removeAll(String...keys) {
		for (var k : keys)
			remove(k);
	}

	/**
	 * Equivalent to calling <code>removeWithDefault(key,<jk>null</jk>,Boolean.<jk>class</jk>)</code>.
	 *
	 * @param key The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Boolean removeBoolean(String key) {
		return removeBoolean(key, null);
	}

	/**
	 * Equivalent to calling <code>removeWithDefault(key,def,Boolean.<jk>class</jk>)</code>.
	 *
	 * @param key The key.
	 * @param def The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Boolean removeBoolean(String key, Boolean def) {
		return removeWithDefault(key, def, Boolean.class);
	}

	/**
	 * Equivalent to calling <code>removeWithDefault(key,<jk>null</jk>,Integer.<jk>class</jk>)</code>.
	 *
	 * @param key The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Integer removeInt(String key) {
		return removeInt(key, null);
	}

	/**
	 * Equivalent to calling <code>removeWithDefault(key,def,Integer.<jk>class</jk>)</code>.
	 *
	 * @param key The key.
	 * @param def The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Integer removeInt(String key, Integer def) {
		return removeWithDefault(key, def, Integer.class);
	}

	/**
	 * Equivalent to calling <code>removeWithDefault(key,<jk>null</jk>,String.<jk>class</jk>)</code>.
	 *
	 * @param key The key.
	 * @return The converted value, or <jk>null</jk> if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public String removeString(String key) {
		return removeString(key, null);
	}

	/**
	 * Equivalent to calling <code>removeWithDefault(key,def,String.<jk>class</jk>)</code>.
	 *
	 * @param key The key.
	 * @param def The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public String removeString(String key, String def) {
		return removeWithDefault(key, def, String.class);
	}

	/**
	 * Equivalent to calling <c>get(class,key,def)</c> followed by <c>remove(key);</c>
	 * @param key The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @param type The class type.
	 *
	 * @param <T> The class type.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public <T> T removeWithDefault(String key, T defVal, Class<T> type) {
		T t = getWithDefault(key, defVal, type);
		remove(key);
		return t;
	}

	/**
	 * Override the default bean session used for converting POJOs.
	 *
	 * <p>
	 * Default is {@link MarshallingContext#DEFAULT}, which is sufficient in most cases.
	 *
	 * <p>
	 * Useful if you're serializing/parsing beans with transforms defined.
	 *
	 * @param session The new bean session.
	 * @return This object.
	 */
	public MarshalledMap session(MarshallingSession session) {
		this.session = session;
		return this;
	}

	/**
	 * Sets the {@link MarshallingSession} currently associated with this map.
	 *
	 * @param value The {@link MarshallingSession} currently associated with this map.
	 * @return This object.
	 */
	public MarshalledMap setBeanSession(MarshallingSession value) {
		session = value;
		return this;
	}

	/**
	 * Returns an unmodifiable copy of this map if it's modifiable.
	 *
	 * @return An unmodifiable copy of this map if it's modifiable, or this map if it is already unmodifiable.
	 */
	public MarshalledMap unmodifiable() {
		if (this instanceof UnmodifiableMarshalledMap this2)
			return this2;
		return new UnmodifiableMarshalledMap(this);
	}

	/**
	 * Returns the {@link MarshallingSession} associated with this map (lazily defaults to {@link MarshallingContext#DEFAULT_SESSION}).
	 *
	 * @return The {@link MarshallingSession} for this map.
	 */
	protected MarshallingSession bs() {
		if (session == null)
			session = MarshallingContext.DEFAULT_SESSION;
		return session;
	}

	/**
	 * Factory hook used by {@link #cast(Class)} / {@link #cast(ClassMeta)} when the target map type cannot be
	 * instantiated directly (e.g. when the caller asks for the {@link Map} interface).
	 *
	 * <p>
	 * Subclasses should override to return an instance of the matching {@code XMap} variant so that callers chaining
	 * off of <c>cast(Map.<jk>class</jk>)</c> still see their flavored map type.
	 *
	 * @param bs The marshalling session to associate with the new map.
	 * @return A new map instance.
	 */
	protected MarshalledMap newMarshalledMap(MarshallingSession bs) {
		return new MarshalledMap(bs);
	}

	/*
	 * Converts this map to the specified class type.
	 */
	@SuppressWarnings({
		"unchecked", // Type erasure requires unchecked casts in dynamic map operations
		"rawtypes", // Raw types necessary for generic type handling
		"java:S3776", // Cognitive complexity acceptable for this specific logic
	})
	private <T> T cast2(ClassMeta<T> cm) {

		MarshallingSession bs = bs();
		try {
			Object value = get("value");

			if (cm.isMap()) {
				Map m2 = (cm.canCreateNewInstance() ? (Map)cm.newInstance() : newMarshalledMap(bs));
				ClassMeta<?> kType = cm.getKeyType();
				ClassMeta<?> vType = cm.getValueType();
				forEach((k, v) -> {
					if (! k.equals(bs.getBeanTypePropertyName(cm))) {

						// Attempt to recursively cast child maps.
						if (v instanceof MarshalledMap v2)
							v = v2.cast(vType);

						Object k2 = (kType.isString() ? k : bs.convertToType(k, kType));
						v = (vType.isObject() ? v : bs.convertToType(v, vType));

						m2.put(k2, v);
					}
				});
				return (T)m2;

			} else if (cm.isBean()) {
				BeanMap<? extends T> bm = bs.newBeanMap(cm.inner());

				// Iterate through all the entries in the map and set the individual field values.
				forEach((k, v) -> {
				if (! k.equals(bs.getBeanTypePropertyName(cm))) {

					// Attempt to recursively cast child maps.
					if (v instanceof MarshalledMap v2) {
						var pm = bm.getProperty(k);
						if (pm != null)
							v = v2.cast((ClassMeta<?>) pm.getMeta().getBeanInfo());
					}

					bm.put(k, v);
				}
				});

				return bm.getBean();

			} else if (cm.isCollectionOrArray()) {
				var items = (List)get("items");
				return bs.convertToType(items, cm);

			} else if (nn(value)) {
				return bs.convertToType(value, cm);
			}

		} catch (Exception e) {
			throw bex(e, cm.inner(), "Error occurred attempting to cast to an object of type ''{0}''", cn(cm));
		}

		throw bex(cm.inner(), "Cannot convert to class type ''{0}''.  Only beans and maps can be converted using this method.", cn(cm));
	}

	private ObjectRest getObjectRest() {
		if (objectRest == null)
			objectRest = new ObjectRest(this);
		return objectRest;
	}

	/*
	 * Combines the class specified by a "_type" attribute with the ClassMeta
	 * passed in through the cast(ClassMeta) method.
	 * The rule is that child classes supersede parent classes, and c2 supersedes c1
	 * if one isn't the parent of another.
	 */
	private ClassMeta<?> narrowClassMeta(ClassMeta<?> c1, ClassMeta<?> c2) {
		if (c1 == null)
			return c2;
		ClassMeta<?> c = getNarrowedClassMeta(c1, c2);
		if (c1.isMap()) {
			ClassMeta<?> k = getNarrowedClassMeta(c1.getKeyType(), c2.getKeyType());
			ClassMeta<?> v = getNarrowedClassMeta(c1.getValueType(), c2.getValueType());
			return bs().getClassMeta(c.inner(), k, v);
		}
		if (c1.isCollection()) {
			ClassMeta<?> e = getNarrowedClassMeta(c1.getElementType(), c2.getElementType());
			return bs().getClassMeta(c.inner(), e);
		}
		return c;
	}
}
