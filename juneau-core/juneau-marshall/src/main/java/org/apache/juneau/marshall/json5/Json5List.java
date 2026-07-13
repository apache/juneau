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
package org.apache.juneau.marshall.json5;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * JSON5-flavored {@link MarshalledList}.
 *
 * <p>
 * Carries the JSON5-specific surface that historically lived on {@link JsonList}: a JSON5
 * {@link #toString()}, JSON5 default-parser constructors, and convenience methods for working with
 * JSON5 array text.
 *
 * <p>
 * For strict (RFC 8259) JSON behavior, use {@link JsonList}. For a neutral, marshaller-agnostic
 * base with no language coupling, use {@link MarshalledList}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct an empty list</jc>
 * 	Json5List <jv>list</jv> = Json5List.<jsm>of</jsm>();
 *
 * 	<jc>// Construct a list of generic Json5Map objects from JSON5</jc>
	 * 	<jv>list</jv> = Json5List.<jsm>ofString</jsm>(<js>"[{foo:'bar'},{baz:'bing'}]"</js>);
 *
 * 	<jc>// JSON5 round-trip via toString()</jc>
 * 	String <jv>s</jv> = <jv>list</jv>.toString();  <jc>// "[{foo:'bar'},{baz:'bing'}]"</jc>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S110",  // Class has many fields, acceptable for collection implementation
	"java:S1206", // Inherits equals/hashCode from MarshalledList; List equality is element-based
	"java:S2160"  // equals() inherited from MarshalledList; element-based equality is correct
})
public class Json5List extends MarshalledList {
	@SuppressWarnings({
		"java:S110" // Inner class has many fields, acceptable for collection implementation
	})
	private static class UnmodifiableJson5List extends Json5List {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings({
			"synthetic-access" // Access to outer class members is intentional
		})
		UnmodifiableJson5List(Json5List contents) {
			if (nn(contents))
				contents.forEach(super::add);
		}

		@Override /* Overridden from List */
		public void add(int location, Object object) {
			throw uoroex();
		}

		@Override
		public boolean isUnmodifiable() { return true; }

		@Override /* Overridden from List */
		public Object remove(int location) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public Object set(int location, Object object) {
			throw uoroex();
		}
	}

	private static final long serialVersionUID = 1L;

	/**
	 * An empty read-only Json5List.
	 *
	 * @serial exclude
	 */
	@SuppressWarnings({
		"java:S110", // Anonymous class has many fields, acceptable for collection implementation
		"java:S2386" // Public static final field accessed externally, cannot be protected
	})
	public static final Json5List EMPTY_LIST = new Json5List() {
		private static final long serialVersionUID = 1L;

		@Override /* Overridden from List */
		public void add(int location, Object object) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public ListIterator<Object> listIterator(int location) {
			return Collections.emptyList().listIterator(location);
		}

		@Override /* Overridden from List */
		public Object remove(int location) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public Object set(int location, Object object) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public List<Object> subList(int start, int end) {
			return Collections.emptyList().subList(start, end);
		}
	};

	/**
	 * Construct an empty list.
	 *
	 * @return An empty list.
	 */
	public static Json5List create() {
		return new Json5List();
	}

	/**
	 * Construct a list initialized with the specified list.
	 *
	 * @param values The list to copy. Can be <jk>null</jk>.
	 * @return A new list or <jk>null</jk> if the list was <jk>null</jk>.
	 */
	public static Json5List of(Collection<?> values) {
		return values == null ? null : new Json5List(values);
	}

	/**
	 * Construct a list initialized with the specified values.
	 *
	 * @param values The values to add to this list.
	 * @return A new list, never <jk>null</jk>.
	 */
	public static Json5List of(Object...values) {
		return new Json5List(values);
	}

	/**
	 * Convenience method for creating a list of array objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static Json5List ofArrays(Object[]...values) {
		var l = new Json5List();
		Collections.addAll(l, values);
		return l;
	}

	/**
	 * Convenience method for creating a list of collection objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static Json5List ofCollections(Collection<?>...values) {
		var l = new Json5List();
		Collections.addAll(l, values);
		return l;
	}

	/**
	 * Construct a list initialized with the specified JSON5 string.
	 *
	 * @param json5 The JSON5 text to parse.
	 * @return A new list (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static Json5List ofString(CharSequence json5) throws ParseException {
		return json5 == null ? new Json5List() : new Json5List(json5);
	}

	/**
	 * Construct a list initialized with the specified reader containing JSON5.
	 *
	 * @param json5 The reader containing JSON5 text to parse.
	 * @return A new list (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static Json5List ofString(Reader json5) throws ParseException {
		return json5 == null ? new Json5List() : new Json5List(json5);
	}

	/**
	 * Parses a string that can consist of either a JSON5 array or comma-delimited list.
	 *
	 * <p>
	 * The type of string is auto-detected.
	 *
	 * @param s The string to parse.
	 * @return A new list (empty if the input was <jk>null</jk> or empty), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static Json5List ofJson5OrCdl(String s) throws ParseException {
		if (Shorts.ie(s))  // NOAI
			return new Json5List();
		if (! isProbablyJsonArray(s, true))
			return new Json5List((Object[])splita(s.trim(), ','));
		return new Json5List(s);
	}

	/**
	 * Construct a list initialized with the specified string.
	 *
	 * @param in The input being parsed.
	 * @param p The parser to use. If <jk>null</jk>, uses {@link Json5Parser}.
	 * @return A new list (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static Json5List ofString(CharSequence in, Parser p) throws ParseException {
		return in == null ? new Json5List() : new Json5List(in, p);
	}

	/**
	 * Construct a list initialized with the specified reader.
	 *
	 * @param in The reader containing the input being parsed.
	 * @param p The parser to use. If <jk>null</jk>, uses {@link Json5Parser}.
	 * @return A new list (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	@SuppressWarnings({
		"java:S1172" // Parameter reserved for future parser context support
	})
	public static Json5List ofString(Reader in, Parser p) throws ParseException {
		return in == null ? new Json5List() : new Json5List(in);
	}

	/**
	 * Construct an empty list.
	 */
	public Json5List() {}

	/**
	 * Construct an empty list with the specified bean context.
	 *
	 * @param session The bean session to use for creating beans.
	 */
	public Json5List(MarshallingSession session) {
		super(session);
	}

	/**
	 * Construct a list initialized with the specified JSON5 text.
	 *
	 * @param json5 The JSON5 text to parse.
	 * @throws ParseException Malformed input encountered.
	 */
	public Json5List(CharSequence json5) throws ParseException {
		this(json5, Json5Parser.DEFAULT);
	}

	/**
	 * Construct a list initialized with the specified string.
	 *
	 * @param in The input being parsed. Can be <jk>null</jk>.
	 * @param p The parser to use. If <jk>null</jk>, uses {@link Json5Parser}.
	 * @throws ParseException Malformed input encountered.
	 */
	public Json5List(CharSequence in, Parser p) throws ParseException {
		super(in, p == null ? Json5Parser.DEFAULT : p);
	}

	/**
	 * Construct a list initialized with the specified list.
	 *
	 * @param copyFrom The list to copy. Can be <jk>null</jk>.
	 */
	public Json5List(Collection<?> copyFrom) {
		super(copyFrom);
	}

	/**
	 * Construct a list initialized with the contents.
	 *
	 * @param entries The entries to add to this list.
	 */
	public Json5List(Object...entries) {
		super(entries);
	}

	/**
	 * Construct a list initialized with the specified reader containing JSON5.
	 *
	 * @param json5 The reader containing JSON5 text to parse.
	 * @throws ParseException Malformed input encountered.
	 */
	public Json5List(Reader json5) throws ParseException {
		this(json5, Json5Parser.DEFAULT);
	}

	/**
	 * Construct a list initialized with the specified reader and parser.
	 *
	 * @param in The reader containing the input being parsed.
	 * @param p The parser to use. If <jk>null</jk>, uses {@link Json5Parser}.
	 * @throws ParseException Malformed input encountered.
	 */
	public Json5List(Reader in, Parser p) throws ParseException {
		super(in, p == null ? Json5Parser.DEFAULT : p);
	}

	@Override /* Overridden from MarshalledList */
	public Json5List append(Collection<?> values) {
		super.append(values);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public Json5List append(Object value) {
		super.append(value);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public Json5List append(Object...values) {
		super.append(values);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public Json5List appendIf(boolean flag, Object value) {
		super.appendIf(flag, value);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public <T> Json5List appendIf(Predicate<T> test, T value) {
		super.appendIf(test, value);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public Json5List appendReverse(List<?> values) {
		super.appendReverse(values);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public Json5List appendReverse(Object...values) {
		super.appendReverse(values);
		return this;
	}

	/**
	 * Converts this object into the specified class type.
	 *
	 * <p>
	 * This method performs a round-trip serialization and deserialization to convert the list into the target type.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='warn'>The current implementation uses a serialization round-trip which may be inefficient for
	 * 		frequent conversions of large objects. Consider caching results or using direct object conversion where possible.
	 * </ul>
	 *
	 * @param cm The class type to convert this object to.
	 * @return A converted object.
	 */
	public Object cast(ClassMeta<?> cm) {
		try {
			return Json5Parser.DEFAULT.parse(Json5Serializer.DEFAULT.serialize(this), cm);
		} catch (ParseException | SerializeException e) {
			throw toRex(e);
		}
	}

	@Override /* Overridden from MarshalledList */
	public Json5List getList(int index) {
		return get(index, Json5List.class);
	}

	@Override /* Overridden from MarshalledList */
	public Json5Map getMap(int index) {
		return get(index, Json5Map.class);
	}

	@Override /* Overridden from MarshalledList */
	public Json5List modifiable() {
		if (isUnmodifiable())
			return new Json5List(this);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public Json5List session(MarshallingSession session) {
		super.session(session);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public Json5List setBeanSession(MarshallingSession value) {
		super.setBeanSession(value);
		return this;
	}

	/**
	 * Serializes this list to a JSON5 string.
	 *
	 * <p>
	 * A synonym for {@link #toString()}.
	 *
	 * @return This object as a JSON5 string.
	 */
	public String toJson5() {
		return Json5.DEFAULT.of(this);
	}

	@Override /* Overridden from Object */
	public String toString() {
		return Json5.DEFAULT.of(this);
	}

	@Override /* Overridden from MarshalledList */
	public Json5List unmodifiable() {
		if (this instanceof UnmodifiableJson5List this2)
			return this2;
		return new UnmodifiableJson5List(this);
	}

	/**
	 * Convenience method for serializing this list to the specified Writer using the {@link Json5Serializer#DEFAULT}
	 * serializer.
	 *
	 * @param w The writer to send the serialized contents of this object.
	 * @return This object.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public Json5List writeTo(Writer w) throws IOException, SerializeException {
		Json5Serializer.DEFAULT.serialize(this, w);
		return this;
	}
}
