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
package org.apache.juneau.json5;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * JSON5-flavored {@link MarshalledMap}.
 *
 * <p>
 * Carries the JSON5-specific surface that historically lived on {@link JsonMap}: a JSON5
 * {@link #toString()}, JSON5 default-parser constructors, and convenience methods for working with
 * JSON5 text. Use this class when you want a {@code Map}-shaped value whose textual form (and
 * default parsing) is JSON5 ("simplified JSON" with unquoted keys, single-quoted strings, etc.).
 *
 * <p>
 * For strict (RFC 8259) JSON behavior, use {@link JsonMap}. For a neutral, marshaller-agnostic
 * base with no language coupling, use {@link MarshalledMap}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct an empty Map</jc>
 * 	Json5Map <jv>map</jv> = Json5Map.<jsm>of</jsm>();
 *
 * 	<jc>// Construct a Map from JSON5</jc>
 * 	<jv>map</jv> = Json5Map.<jsm>ofString</jsm>(<js>"{a:'A',b:{c:'C',d:123}}"</js>);
 *
 * 	<jc>// JSON5 round-trip via toString()</jc>
 * 	String <jv>s</jv> = <jv>map</jv>.toString();  <jc>// "{a:'A',b:{c:'C',d:123}}"</jc>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 */
@SuppressWarnings({
	"java:S2160" // equals() / hashCode() inherited from MarshalledMap; map equality is element-based
})
public class Json5Map extends MarshalledMap {

	@SuppressWarnings({
		"java:S2160", // equals() / hashCode() inherited from Json5Map; map equality is element-based
		"java:S110"   // Inheritance depth inherited from MarshalledMap -> LinkedHashMap chain; intentional
	})
	private static class UnmodifiableJson5Map extends Json5Map {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings({
			"synthetic-access" // Access to outer class members is intentional
		})
		UnmodifiableJson5Map(Json5Map contents) {
			if (nn(contents))
				contents.forEach(super::put);
		}

		@Override
		public boolean isUnmodifiable() { return true; }

		@Override
		public Object put(String key, Object val) {
			throw unsupportedOpReadOnly();
		}

		@Override
		public Object remove(Object key) {
			throw unsupportedOpReadOnly();
		}
	}

	private static final long serialVersionUID = 1L;

	/**
	 * An empty read-only Json5Map.
	 *
	 * @serial exclude
	 */
	@SuppressWarnings({
		"java:S2386", // Public static final field accessed externally, cannot be protected
		"java:S110"   // Anonymous subclass of Json5Map inherits the MarshalledMap -> LinkedHashMap chain; intentional
	})
	public static final Json5Map EMPTY_MAP = new Json5Map() {

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
	public static Json5Map create() {
		return new Json5Map();
	}

	/**
	 * Construct an empty filtered map.
	 *
	 * @return An empty map.
	 */
	public static Json5Map filteredMap() {
		return create().filtered();
	}

	/**
	 * Construct a filtered map initialized with the specified key/value pairs.
	 *
	 * <p>
	 * Same as {@link #of(Object...)} but calls {@link #filtered()} on the created map.
	 *
	 * @param keyValuePairs A list of key/value pairs to add to this map.
	 * @return A new map, never <jk>null</jk>.
	 */
	public static Json5Map filteredMap(Object...keyValuePairs) {
		return new Json5Map(keyValuePairs).filtered();
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
	public static Json5Map of(Map<?,?> values) {
		return values == null ? null : new Json5Map(values);
	}

	/**
	 * Construct a map initialized with the specified key/value pairs.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	Json5Map <jv>map</jv> = <jk>new</jk> Json5Map(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>);
	 * </p>
	 *
	 * @param keyValuePairs A list of key/value pairs to add to this map.
	 * @return A new map, never <jk>null</jk>.
	 */
	public static Json5Map of(Object...keyValuePairs) {
		return new Json5Map(keyValuePairs);
	}

	/**
	 * Construct a map initialized with the specified JSON5 string.
	 *
	 * @param json5
	 * 	The JSON5 text to parse.
	 * @return A new map (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static Json5Map ofString(CharSequence json5) throws ParseException {
		return json5 == null ? new Json5Map() : new Json5Map(json5);
	}

	/**
	 * Construct a map initialized with the specified reader containing JSON5.
	 *
	 * @param json5
	 * 	The reader containing JSON5 text to parse.
	 * @return A new map (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static Json5Map ofString(Reader json5) throws ParseException {
		return json5 == null ? new Json5Map() : new Json5Map(json5);
	}

	/**
	 * Construct a map initialized with the specified string and parser.
	 *
	 * @param in The input being parsed.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>If <jk>null</jk>, uses {@link Json5Parser}.
	 * @return A new map (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static Json5Map ofString(CharSequence in, Parser p) throws ParseException {
		return in == null ? new Json5Map() : new Json5Map(in, p);
	}

	/**
	 * Construct a map initialized with the specified reader and parser.
	 *
	 * @param in
	 * 	The reader containing the input being parsed.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>If <jk>null</jk>, uses {@link Json5Parser}.
	 * @return A new map (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static Json5Map ofString(Reader in, Parser p) throws ParseException {
		return in == null ? new Json5Map() : new Json5Map(in, p);
	}

	/**
	 * Construct an empty map.
	 */
	public Json5Map() {}

	/**
	 * Construct an empty map with the specified bean context.
	 *
	 * @param session The bean session to use for creating beans.
	 */
	public Json5Map(MarshallingSession session) {
		super(session);
	}

	/**
	 * Construct a map initialized with the specified JSON5 text.
	 *
	 * @param json5 The JSON5 text to parse.
	 * @throws ParseException Malformed input encountered.
	 */
	public Json5Map(CharSequence json5) throws ParseException {
		this(json5, Json5Parser.DEFAULT);
	}

	/**
	 * Construct a map initialized with the specified string.
	 *
	 * @param in The input being parsed. Can be <jk>null</jk>.
	 * @param p The parser to use. If <jk>null</jk>, uses {@link Json5Parser}.
	 * @throws ParseException Malformed input encountered.
	 */
	public Json5Map(CharSequence in, Parser p) throws ParseException {
		super(in, p == null ? Json5Parser.DEFAULT : p);
	}

	/**
	 * Construct a map initialized with the specified map.
	 *
	 * @param in
	 * 	The map to copy.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Keys will be converted to strings using {@link Object#toString()}.
	 */
	public Json5Map(Map<?,?> in) {
		super(in);
	}

	/**
	 * Construct a map initialized with the specified key/value pairs.
	 *
	 * @param keyValuePairs A list of key/value pairs to add to this map.
	 */
	public Json5Map(Object...keyValuePairs) {
		super(keyValuePairs);
	}

	/**
	 * Construct a map initialized with the specified reader containing JSON5.
	 *
	 * @param json5 The reader containing JSON5 text to parse.
	 * @throws ParseException Malformed input encountered.
	 */
	public Json5Map(Reader json5) throws ParseException {
		this(json5, Json5Parser.DEFAULT);
	}

	/**
	 * Construct a map initialized with the specified reader and parser.
	 *
	 * @param in The reader containing the input being parsed.
	 * @param p The parser to use. If <jk>null</jk>, uses {@link Json5Parser}.
	 * @throws ParseException Malformed input encountered.
	 */
	public Json5Map(Reader in, Parser p) throws ParseException {
		super(in, p == null ? Json5Parser.DEFAULT : p);
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map append(Map<String,Object> values) {
		super.append(values);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map append(String key, Object value) {
		super.append(key, value);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	@SuppressWarnings({
		"unchecked", // Generic varargs is safe here: values are only iterated, not stored into a typed array.
		"varargs"
	})
	public <T> Json5Map appendFirst(Predicate<T> test, String key, T...values) {
		super.appendFirst(test, key, values);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map appendIf(boolean flag, String key, Object value) {
		super.appendIf(flag, key, value);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public <T> Json5Map appendIf(Predicate<T> test, String key, T value) {
		super.appendIf(test, key, value);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map appendIfAbsent(String key, Object value) {
		super.appendIfAbsent(key, value);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public <T> Json5Map appendIfAbsentIf(Predicate<T> predicate, String key, T value) {
		super.appendIfAbsentIf(predicate, key, value);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map exclude(String...keys) {
		var m2 = new Json5Map();
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

	@Override /* Overridden from MarshalledMap */
	public Json5Map filtered() {
		super.filtered();
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map filtered(Predicate<Object> value) {
		super.filtered(value);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public Json5List findList(String...keys) {
		return find(Json5List.class, keys);
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map findMap(String...keys) {
		return find(Json5Map.class, keys);
	}

	@Override /* Overridden from MarshalledMap */
	public Json5List getList(String key) {
		return get(key, Json5List.class);
	}

	@Override /* Overridden from MarshalledMap */
	public Json5List getList(String key, boolean createIfNotExists) {
		Json5List m = getWithDefault(key, null, Json5List.class);
		if (m == null && createIfNotExists) {
			m = new Json5List();
			put(key, m);
		}
		return m;
	}

	/**
	 * Returns the specified entry value converted to a {@link Json5List}.
	 *
	 * @param key The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Json5List getList(String key, Json5List defVal) {
		return getWithDefault(key, defVal, Json5List.class);
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map getMap(String key) {
		return get(key, Json5Map.class);
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map getMap(String key, boolean createIfNotExists) {
		var m = getWithDefault(key, null, Json5Map.class);
		if (m == null && createIfNotExists) {
			m = new Json5Map();
			put(key, m);
		}
		return m;
	}

	/**
	 * Returns the specified entry value converted to a {@link Json5Map}.
	 *
	 * @param key The key.
	 * @param defVal The default value if the map doesn't contain the specified mapping.
	 * @return The converted value, or the default value if the map contains no mapping for this key.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Json5Map getMap(String key, Json5Map defVal) {
		return getWithDefault(key, defVal, Json5Map.class);
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map include(String...keys) {
		var m2 = new Json5Map();
		this.forEach((k, v) -> {
			for (var kk : keys)
				if (kk.equals(k))
					m2.put(kk, v);
		});
		return m2;
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map inner(Map<String,Object> inner) {
		super.inner(inner);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map keepAll(String...keys) {
		super.keepAll(keys);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map modifiable() {
		if (isUnmodifiable())
			return new Json5Map(this);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	protected Json5Map newMarshalledMap(MarshallingSession bs) {
		return new Json5Map(bs);
	}

	/**
	 * Convenience method for inserting JSON5 directly into an attribute on this object.
	 *
	 * <p>
	 * The JSON5 text can be an object (i.e. <js>"{...}"</js>) or an array (i.e. <js>"[...]"</js>).
	 *
	 * @param key The key.
	 * @param json5 The JSON5 text that will be parsed into an Object and then inserted into this map.
	 * @throws ParseException Malformed input encountered.
	 */
	public void putJson5(String key, String json5) throws ParseException {
		this.put(key, Json5Parser.DEFAULT.parse(json5, Object.class));
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map session(MarshallingSession session) {
		super.session(session);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map setBeanSession(MarshallingSession value) {
		super.setBeanSession(value);
		return this;
	}

	/**
	 * Serializes this map to a JSON5 string.
	 *
	 * <p>
	 * A synonym for {@link #toString()}.
	 *
	 * @return This object as a JSON5 string.
	 */
	public String toJson5() {
		return Json5.of(this);
	}

	/**
	 * Serializes this map to a readable (indented) JSON5 string using {@link Json5Serializer#DEFAULT_READABLE}.
	 *
	 * @return This object serialized as a readable JSON5 string.
	 */
	public String toReadableJson5() {
		if (Json5Serializer.DEFAULT_READABLE == null)
			return s(this);
		return Json5Serializer.DEFAULT_READABLE.toString(this);
	}

	@Override /* Overridden from Object */
	public String toString() {
		return Json5.of(this);
	}

	@Override /* Overridden from MarshalledMap */
	public Json5Map unmodifiable() {
		if (this instanceof UnmodifiableJson5Map this2)
			return this2;
		return new UnmodifiableJson5Map(this);
	}

	/**
	 * Convenience method for serializing this map to the specified <c>Writer</c> using the
	 * {@link Json5Serializer#DEFAULT} serializer.
	 *
	 * @param w The writer to serialize this object to.
	 * @return This object.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public Json5Map writeTo(Writer w) throws IOException, SerializeException {
		Json5Serializer.DEFAULT.serialize(this, w);
		return this;
	}
}
