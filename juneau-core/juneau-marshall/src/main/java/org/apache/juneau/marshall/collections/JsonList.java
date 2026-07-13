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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Java implementation of a JSON array.
 *
 * <p>
 * This class is a subclass of {@link MarshalledList} that carries strict-JSON-specific surface, including
 * strict-JSON {@link #toString()} (via {@link Json#of(Object)}), {@link JsonParser}-defaulting
 * {@link CharSequence} / {@link Reader} constructors, and convenience methods for serializing to the
 * various JSON dialects (JSON5, JSON Lines, canonical JSON, HJSON).
 *
 * <p>
 * <b>v10.0 behavioral break:</b> {@code toString()} now produces strict (RFC 8259) JSON (was JSON5),
 * and the {@link CharSequence} / {@link Reader} constructors now default to {@link JsonParser#DEFAULT}
 * (was {@link Json5Parser#DEFAULT}). For the previous JSON5 behavior use {@link Json5List}.
 *
 * <p>
 * Note that the use of this class is optional for generating JSON.  The serializers will accept any objects that implement the
 * {@link Collection} interface.  But this class provides some useful additional functionality when working with JSON
 * models constructed from Java Collections Framework objects.  For example, a constructor is provided for converting a
 * JSON array string directly into a {@link List}.  It also contains accessor methods for to avoid common typecasting
 * when accessing elements in a list.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct an empty List</jc>
 * 	JsonList <jv>list</jv> = JsonList.<jsm>of</jsm>();
 *
 * 	<jc>// Construct a list of objects using various methods</jc>
 * 	<jv>list</jv> = JsonList.<jsm>of</jsm>().a(<js>"foo"</js>).a(123).a(<jk>true</jk>);
 * 	<jv>list</jv> = JsonList.<jsm>of</jsm>().a(<js>"foo"</js>, 123, <jk>true</jk>);  <jc>// Equivalent</jc>
 * 	<jv>list</jv> = JsonList.<jsm>of</jsm>(<js>"foo"</js>, 123, <jk>true</jk>);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Construct a list of integers from JSON</jc>
 * 	<jv>list</jv> = JsonList.<jsm>ofString</jsm>(<js>"[1,2,3]"</js>);
 *
 * 	<jc>// Construct a list of generic JsonMap objects from JSON</jc>
 * 	<jv>list</jv> = JsonList.<jsm>ofString</jsm>(<js>"[{foo:'bar'},{baz:'bing'}]"</js>);
 *
 * 	<jc>// Construct JSON from JsonList</jc>
 * 	<jv>list</jv> = JsonList.<jsm>ofString</jsm>(<js>"[{foo:'bar'},{baz:'bing'}]"</js>);
 * 	String <jv>json</jv> = <jv>list</jv>.toString();  <jc>// Produces "[{foo:'bar'},{baz:'bing'}]"</jc>
 * 	<jv>json</jv> = <jv>list</jv>.toString(JsonSerializer.<jsf>DEFAULT</jsf>);  <jc>// Equivalent</jc>
 * 	<jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>list</jv>);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Get one of the entries in the list as an Integer</jc>
 * 	<jv>list</jv> = JsonList.<jsm>ofString</jsm>(<js>"[1,2,3]"</js>);
 * 	Integer <jv>integer</jv> = <jv>list</jv>.getInt(1);
 * 	<jv>integer</jv> = <jv>list</jv>.get(Integer.<jk>class</jk>, 1);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Iterate over a list of beans using the elements() method</jc>
 * 	<jv>list</jv> = JsonList.<jsm>ofString</jsm>(<js>"[{name:'John Smith',age:45}]"</js>);
 * 	<jk>for</jk> (Person <jv>person</jv> : <jv>list</jv>.elements(Person.<jk>class</jk>) {
 * 		<jc>// Do something with p</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S110",  // Class has many fields, acceptable for collection implementation
	"java:S1206", // Inherits equals/hashCode from LinkedList; List equality is element-based
	"java:S2160"  // equals() inherited from LinkedList; element-based equality is correct for a JSON list
})
public class JsonList extends MarshalledList {
	@SuppressWarnings({
		"java:S110" // Inner class has many fields, acceptable for collection implementation
	})
	private static class UnmodifiableJsonList extends JsonList {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings({
			"synthetic-access" // Access to outer class members is intentional
		})
		UnmodifiableJsonList(JsonList contents) {
			if (nn(contents))
				contents.forEach(super::add);
		}

		@Override /* Overridden from List */
		public boolean add(Object object) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public void add(int location, Object object) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public boolean addAll(Collection<?> c) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public boolean addAll(int location, Collection<?> c) {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public void addFirst(Object object) {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public void addLast(Object object) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public void clear() {
			throw uoroex();
		}

		@Override
		public boolean isUnmodifiable() { return true; }

		@Override /* Overridden from List */
		public Iterator<Object> iterator() {
			return readOnlyIterator(super.iterator());
		}

		@Override /* Overridden from List */
		public ListIterator<Object> listIterator() {
			return readOnlyListIterator(super.listIterator());
		}

		@Override /* Overridden from List */
		public ListIterator<Object> listIterator(int location) {
			return readOnlyListIterator(super.listIterator(location));
		}

		@Override /* Overridden from Queue */
		public boolean offer(Object object) {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public boolean offerFirst(Object object) {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public boolean offerLast(Object object) {
			throw uoroex();
		}

		@Override /* Overridden from Queue */
		public Object poll() {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public Object pollFirst() {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public Object pollLast() {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public Object pop() {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public void push(Object object) {
			throw uoroex();
		}

		@Override /* Overridden from Queue */
		public Object remove() {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public Object remove(int location) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public boolean remove(Object object) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public boolean removeAll(Collection<?> c) {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public Object removeFirst() {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public boolean removeFirstOccurrence(Object object) {
			throw uoroex();
		}

		@Override /* Overridden from Collection */
		public boolean removeIf(Predicate<? super Object> filter) {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public Object removeLast() {
			throw uoroex();
		}

		@Override /* Overridden from Deque */
		public boolean removeLastOccurrence(Object object) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public void replaceAll(UnaryOperator<Object> operator) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public boolean retainAll(Collection<?> c) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public Object set(int location, Object object) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public void sort(Comparator<? super Object> c) {
			throw uoroex();
		}

		@Override /* Overridden from List */
		public List<Object> subList(int start, int end) {
			return Collections.unmodifiableList(new ArrayList<>(this).subList(start, end));
		}

		private static Iterator<Object> readOnlyIterator(Iterator<Object> it) {
			return new Iterator<>() {
				@Override public boolean hasNext() { return it.hasNext(); }
				@Override public Object next() { return it.next(); }
				@Override public void remove() { throw uoroex(); }
				@Override public void forEachRemaining(Consumer<? super Object> action) { it.forEachRemaining(action); }
			};
		}

		private static ListIterator<Object> readOnlyListIterator(ListIterator<Object> it) {
			return new ListIterator<>() {
				@Override public boolean hasNext() { return it.hasNext(); }
				@Override public Object next() { return it.next(); }
				@Override public boolean hasPrevious() { return it.hasPrevious(); }
				@Override public Object previous() { return it.previous(); }
				@Override public int nextIndex() { return it.nextIndex(); }
				@Override public int previousIndex() { return it.previousIndex(); }
				@Override public void remove() { throw uoroex(); }
				@Override public void set(Object e) { throw uoroex(); }
				@Override public void add(Object e) { throw uoroex(); }
				@Override public void forEachRemaining(Consumer<? super Object> action) { it.forEachRemaining(action); }
			};
		}
	}

	private static final long serialVersionUID = 1L;
	/**
	 * An empty read-only JsonList.
	 *
	 * @serial exclude
	 */
	@SuppressWarnings({
		"java:S110", // Anonymous class has many fields, acceptable for collection implementation
		"java:S2386" // Public static final field accessed externally, cannot be protected
	})
	public static final JsonList EMPTY_LIST = new JsonList() {
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
	public static JsonList create() {
		return new JsonList();
	}

	/**
	 * Construct a list initialized with the specified list.
	 *
	 * @param values
	 * 	The list to copy.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new list or <jk>null</jk> if the list was <jk>null</jk>.
	 */
	public static JsonList of(Collection<?> values) {
		return values == null ? null : new JsonList(values);
	}

	/**
	 * Construct a list initialized with the specified values.
	 *
	 * @param values The values to add to this list.
	 * @return A new list, never <jk>null</jk>.
	 */
	public static JsonList of(Object...values) {
		return new JsonList(values);
	}

	/**
	 * Convenience method for creating a list of array objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static JsonList ofArrays(Object[]...values) {
		var l = new JsonList();
		Collections.addAll(l, values);
		return l;
	}

	/**
	 * Convenience method for creating a list of collection objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static JsonList ofCollections(Collection<?>...values) {
		var l = new JsonList();
		Collections.addAll(l, values);
		return l;
	}

	/**
	 * Construct a list initialized with the specified JSON string.
	 *
	 * @param json
	 * 	The JSON text to parse.
	 * 	<br>Can be normal or simplified JSON.
	 * @return A new list (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static JsonList ofString(CharSequence json) throws ParseException {
		return json == null ? new JsonList() : new JsonList(json);
	}

	/**
	 * Construct a list initialized with the specified reader containing JSON.
	 *
	 * @param json
	 * 	The reader containing JSON text to parse.
	 * 	<br>Can contain normal or simplified JSON.
	 * @return A new list (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static JsonList ofString(Reader json) throws ParseException {
		return json == null ? new JsonList() : new JsonList(json);
	}

	/**
	 * Parses a string that can consist of either a JSON array or comma-delimited list.
	 *
	 * <p>
	 * The type of string is auto-detected.
	 *
	 * @param s The string to parse.
	 * @return A new list (empty if the input was <jk>null</jk> or empty), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static JsonList ofJsonOrCdl(String s) throws ParseException {
		if (Shorts.ie(s))  // NOAI
			return new JsonList();
		if (! isProbablyJsonArray(s, true))
			return new JsonList((Object[])splita(s.trim(), ','));
		return new JsonList(s);
	}

	/**
	 * Construct a list initialized with the specified string.
	 *
	 * @param in The input being parsed.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>If <jk>null</jk>, uses {@link JsonParser}.
	 * @return A new list (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static JsonList ofString(CharSequence in, Parser p) throws ParseException {
		return in == null ? new JsonList() : new JsonList(in, p);
	}

	/**
	 * Construct a list initialized with the specified string.
	 *
	 * @param in
	 * 	The reader containing the input being parsed.
	 * 	<br>Can contain normal or simplified JSON.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>If <jk>null</jk>, uses {@link JsonParser}.
	 * @return A new list (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	@SuppressWarnings({
		"java:S1172" // Parameter reserved for future parser context support
	})
	public static JsonList ofString(Reader in, Parser p) throws ParseException {
		return in == null ? new JsonList() : new JsonList(in);
	}

	/**
	 * Construct an empty list.
	 */
	public JsonList() {}

	/**
	 * Construct an empty list with the specified bean context.
	 *
	 * @param session The bean session to use for creating beans.
	 */
	public JsonList(MarshallingSession session) {
		super(session);
	}

	/**
	 * Construct a list initialized with the specified JSON.
	 *
	 * @param json
	 * 	The JSON text to parse.
	 * 	<br>Must be strict (RFC 8259) JSON; for JSON5 input use {@link Json5List} or pass
	 * 	{@link Json5Parser#DEFAULT} explicitly.
	 * @throws ParseException Malformed input encountered.
	 */
	public JsonList(CharSequence json) throws ParseException {
		this(json, JsonParser.DEFAULT);
	}

	/**
	 * Construct a list initialized with the specified string.
	 *
	 * @param in
	 * 	The input being parsed.
	 * 	<br>Can be <jk>null</jk>.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>If <jk>null</jk>, uses {@link JsonParser}.
	 * @throws ParseException Malformed input encountered.
	 */
	public JsonList(CharSequence in, Parser p) throws ParseException {
		super(in, p == null ? JsonParser.DEFAULT : p);
	}

	/**
	 * Construct a list initialized with the specified list.
	 *
	 * @param copyFrom
	 * 	The list to copy.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public JsonList(Collection<?> copyFrom) {
		super(copyFrom);
	}

	/**
	 * Construct a list initialized with the contents.
	 *
	 * @param entries The entries to add to this list.
	 */
	public JsonList(Object...entries) {
		super(entries);
	}

	/**
	 * Construct a list initialized with the specified reader containing JSON.
	 *
	 * @param json
	 * 	The reader containing JSON text to parse.
	 * 	<br>Must be strict (RFC 8259) JSON; for JSON5 input use {@link Json5List} or pass
	 * 	{@link Json5Parser#DEFAULT} explicitly.
	 * @throws ParseException Malformed input encountered.
	 */
	public JsonList(Reader json) throws ParseException {
		this(json, JsonParser.DEFAULT);
	}

	/**
	 * Construct a list initialized with the specified string.
	 *
	 * @param in
	 * 	The reader containing the input being parsed.
	 * 	<br>Must be strict (RFC 8259) JSON.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>If <jk>null</jk>, uses {@link JsonParser}.
	 * @throws ParseException Malformed input encountered.
	 */
	public JsonList(Reader in, Parser p) throws ParseException {
		super(in, p == null ? JsonParser.DEFAULT : p);
	}

	@Override /* Overridden from MarshalledList */
	public JsonList append(Collection<?> values) {
		super.append(values);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public JsonList append(Object value) {
		super.append(value);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public JsonList append(Object...values) {
		super.append(values);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public JsonList appendIf(boolean flag, Object value) {
		super.appendIf(flag, value);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public <T> JsonList appendIf(Predicate<T> test, T value) {
		super.appendIf(test, value);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public JsonList appendReverse(List<?> values) {
		super.appendReverse(values);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public JsonList appendReverse(Object...values) {
		super.appendReverse(values);
		return this;
	}

	/**
	 * Serializes this list to a standard JSON string.
	 *
	 * @return This object as a standard JSON string.
	 */
	public String toJson() {
		return Json.of(this);
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
		return Json5.of(this);
	}

	/**
	 * Serializes this list to a JSON Lines string.
	 *
	 * @return This object as a JSON Lines string.
	 */
	public String toJsonl() {
		return Jsonl.of(this);
	}

	/**
	 * Serializes this list to a canonical JSON string (RFC 8785).
	 *
	 * @return This object as a canonical JSON string.
	 */
	public String toJcs() {
		return Jcs.of(this);
	}

	/**
	 * Serializes this list to an HJSON string.
	 *
	 * @return This object as an HJSON string.
	 */
	public String toHjson() {
		return Hjson.of(this);
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
	public JsonList getList(int index) {
		return get(index, JsonList.class);
	}

	@Override /* Overridden from MarshalledList */
	public JsonMap getMap(int index) {
		return get(index, JsonMap.class);
	}

	@Override /* Overridden from MarshalledList */
	public JsonList modifiable() {
		if (isUnmodifiable())
			return new JsonList(this);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public JsonList session(MarshallingSession session) {
		super.session(session);
		return this;
	}

	@Override /* Overridden from MarshalledList */
	public JsonList setBeanSession(MarshallingSession value) {
		super.setBeanSession(value);
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return Json.of(this);
	}

	@Override /* Overridden from MarshalledList */
	public JsonList unmodifiable() {
		if (this instanceof UnmodifiableJsonList this2)
			return this2;
		return new UnmodifiableJsonList(this);
	}

	/**
	 * Convenience method for serializing this JsonList to the specified Writer using the JsonSerializer.DEFAULT
	 * serializer.
	 *
	 * @param w The writer to send the serialized contents of this object.
	 * @return This object.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public JsonList writeTo(Writer w) throws IOException, SerializeException {
		JsonSerializer.DEFAULT.serialize(this, w);
		return this;
	}
}
