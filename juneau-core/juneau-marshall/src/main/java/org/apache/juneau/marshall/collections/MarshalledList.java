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
import static org.apache.juneau.commons.utils.PredicateUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.objecttools.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Neutral, marshaller-agnostic list of objects.
 *
 * <p>
 * This class is the base for {@link JsonList} and the per-marshaller {@code XList} variants. It carries
 * all marshaller-neutral functionality such as typed accessors, fluent setters, bean integration,
 * and {@link ObjectRest}-based path navigation. The default {@link #toString()} comes from
 * {@link LinkedList}; subclasses should override it to produce the appropriate marshalled form.
 *
 * <p>
 * Note that the use of this class is optional. The serializers will accept any objects that implement the
 * {@link Collection} interface. But this class provides useful additional functionality when working with
 * data models constructed from Java Collections Framework objects.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S110",  // Class has many fields, acceptable for collection implementation
	"java:S1206", // Inherits equals/hashCode from LinkedList; List equality is element-based
	"java:S2160"  // equals() inherited from LinkedList; element-based equality is correct
})
public class MarshalledList extends LinkedList<Object> {

	@SuppressWarnings({
		"java:S110" // Inner class has many fields, acceptable for collection implementation
	})
	private static class UnmodifiableMarshalledList extends MarshalledList {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings({
			"synthetic-access" // Access to outer class members is intentional
		})
		UnmodifiableMarshalledList(MarshalledList contents) {
			if (nn(contents))
				contents.forEach(super::add);
		}

		@Override /* Overridden from List */
		public boolean add(Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public void add(int location, Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public boolean addAll(Collection<?> c) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public boolean addAll(int location, Collection<?> c) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public void addFirst(Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public void addLast(Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public void clear() {
			throw unsupportedOpReadOnly();
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
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public boolean offerFirst(Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public boolean offerLast(Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Queue */
		public Object poll() {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public Object pollFirst() {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public Object pollLast() {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public Object pop() {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public void push(Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Queue */
		public Object remove() {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public Object remove(int location) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public boolean remove(Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public boolean removeAll(Collection<?> c) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public Object removeFirst() {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public boolean removeFirstOccurrence(Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Collection */
		public boolean removeIf(Predicate<? super Object> filter) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public Object removeLast() {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from Deque */
		public boolean removeLastOccurrence(Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public void replaceAll(UnaryOperator<Object> operator) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public boolean retainAll(Collection<?> c) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public Object set(int location, Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public void sort(Comparator<? super Object> c) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public List<Object> subList(int start, int end) {
			return Collections.unmodifiableList(new ArrayList<>(this).subList(start, end));
		}

		private static Iterator<Object> readOnlyIterator(Iterator<Object> it) {
			return new Iterator<>() {
				@Override public boolean hasNext() { return it.hasNext(); }
				@Override public Object next() { return it.next(); }
				@Override public void remove() { throw unsupportedOpReadOnly(); }
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
				@Override public void remove() { throw unsupportedOpReadOnly(); }
				@Override public void set(Object e) { throw unsupportedOpReadOnly(); }
				@Override public void add(Object e) { throw unsupportedOpReadOnly(); }
				@Override public void forEachRemaining(Consumer<? super Object> action) { it.forEachRemaining(action); }
			};
		}
	}

	private static final long serialVersionUID = 1L;

	/**
	 * An empty read-only MarshalledList.
	 *
	 * @serial exclude
	 */
	@SuppressWarnings({
		"java:S110", // Anonymous class has many fields, acceptable for collection implementation
		"java:S2386" // Public static final field accessed externally, cannot be protected
	})
	public static final MarshalledList EMPTY_LIST = new MarshalledList() {
		private static final long serialVersionUID = 1L;

		@Override /* Overridden from List */
		public void add(int location, Object object) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public ListIterator<Object> listIterator(int location) {
			return Collections.emptyList().listIterator(location);
		}

		@Override /* Overridden from List */
		public Object remove(int location) {
			throw unsupportedOpReadOnly();
		}

		@Override /* Overridden from List */
		public Object set(int location, Object object) {
			throw unsupportedOpReadOnly();
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
	public static MarshalledList create() {
		return new MarshalledList();
	}

	/**
	 * Construct a list initialized with the specified collection.
	 *
	 * @param values
	 * 	The collection to copy.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new list or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static MarshalledList of(Collection<?> values) {
		return values == null ? null : new MarshalledList(values);
	}

	/**
	 * Construct a list initialized with the specified values.
	 *
	 * @param values The values to add to this list.
	 * @return A new list, never <jk>null</jk>.
	 */
	public static MarshalledList of(Object...values) {
		return new MarshalledList(values);
	}

	/**
	 * Convenience method for creating a list of array objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static MarshalledList ofArrays(Object[]...values) {
		var l = new MarshalledList();
		Collections.addAll(l, values);
		return l;
	}

	/**
	 * Convenience method for creating a list of collection objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static MarshalledList ofCollections(Collection<?>...values) {
		var l = new MarshalledList();
		Collections.addAll(l, values);
		return l;
	}

	/**
	 * Construct a list initialized by parsing the specified string with the specified parser.
	 *
	 * @param in The input being parsed.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return A new list (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static MarshalledList ofString(CharSequence in, Parser p) throws ParseException {
		return in == null ? new MarshalledList() : new MarshalledList(in, p);
	}

	/**
	 * Construct a list initialized by parsing the specified reader with the specified parser.
	 *
	 * @param in The reader containing the input being parsed.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return A new list (empty if the input was <jk>null</jk>), never <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static MarshalledList ofString(java.io.Reader in, Parser p) throws ParseException {
		return in == null ? new MarshalledList() : new MarshalledList(in, p);
	}

	transient MarshallingSession session = null;

	private transient ObjectRest objectRest;

	/**
	 * Construct an empty list.
	 */
	public MarshalledList() {}

	/**
	 * Construct an empty list with the specified bean context.
	 *
	 * @param session The bean session to use for creating beans.
	 */
	public MarshalledList(MarshallingSession session) {
		this.session = session;
	}

	/**
	 * Construct a list initialized by parsing the specified string with the specified parser.
	 *
	 * @param in
	 * 	The input being parsed.
	 * 	<br>Can be <jk>null</jk>.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>Must not be <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public MarshalledList(CharSequence in, Parser p) throws ParseException {
		this(assertArgNotNull("p", p).getMarshallingContext().getSession());
		if (nn(in))
			p.parseIntoCollection(in, this, bs().object());
	}

	/**
	 * Construct a list initialized with the specified collection.
	 *
	 * @param copyFrom
	 * 	The collection to copy.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public MarshalledList(Collection<?> copyFrom) {
		super(copyFrom);
	}

	/**
	 * Construct a list initialized with the contents.
	 *
	 * @param entries The entries to add to this list.
	 */
	public MarshalledList(Object...entries) {
		Collections.addAll(this, entries);
	}

	/**
	 * Construct a list initialized by parsing the specified reader with the specified parser.
	 *
	 * @param in
	 * 	The reader containing the input being parsed.
	 * @param p
	 * 	The parser to use to parse the input.
	 * 	<br>Must not be <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public MarshalledList(java.io.Reader in, Parser p) throws ParseException {
		this(assertArgNotNull("p", p).getMarshallingContext().getSession());
		p.parseIntoCollection(in, this, bs().object());
	}

	/**
	 * Adds all the values in the specified collection to this list.
	 *
	 * @param values The values to add to this list.
	 * @return This object.
	 */
	public MarshalledList append(Collection<?> values) {
		if (nn(values))
			addAll(values);
		return this;
	}

	/**
	 * Adds the value to this list.
	 *
	 * @param value The value to add to this list.
	 * @return This object.
	 */
	public MarshalledList append(Object value) {
		add(value);
		return this;
	}

	/**
	 * Adds all the values in the specified array to this list.
	 *
	 * @param values The values to add to this list.
	 * @return This object.
	 */
	public MarshalledList append(Object...values) {
		Collections.addAll(this, values);
		return this;
	}

	/**
	 * Adds an entry to this list if the boolean flag is <jk>true</jk>.
	 *
	 * @param flag The boolean flag.
	 * @param value The value to add.
	 * @return This object.
	 */
	public MarshalledList appendIf(boolean flag, Object value) {
		if (flag)
			append(value);
		return this;
	}

	/**
	 * Add if predicate matches.
	 *
	 * @param <T> The type being tested.
	 * @param test The predicate to match against.
	 * @param value The value to add if the predicate matches.
	 * @return This object.
	 */
	public <T> MarshalledList appendIf(Predicate<T> test, T value) {
		return appendIf(test(test, value), value);
	}

	/**
	 * Adds all the entries in the specified collection to this list in reverse order.
	 *
	 * @param values The collection to add to this list.
	 * @return This object.
	 */
	public MarshalledList appendReverse(List<?> values) {
		if (nn(values))
			for (ListIterator<?> i = values.listIterator(values.size()); i.hasPrevious();)
				add(i.previous());
		return this;
	}

	/**
	 * Adds the contents of the array to the list in reverse order.
	 *
	 * <p>
	 * i.e. add values from the array from end-to-start order to the end of the list.
	 *
	 * @param values The collection to add to this list.
	 * @return This object.
	 */
	public MarshalledList appendReverse(Object...values) {
		if (values != null)
			for (var i = values.length - 1; i >= 0; i--)
				add(values[i]);
		return this;
	}

	/**
	 * Serializes this list to a string using the specified serializer.
	 *
	 * @param serializer The serializer to use to convert this object to a string.
	 * @return This object as a serialized string.
	 */
	public String toString(WriterSerializer serializer) {
		return serializer.toString(this);
	}

	/**
	 * Similar to {@link #remove(int) remove(int)},but the key is a slash-delimited path used to traverse entries in
	 * this POJO.
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

	/**
	 * Creates an {@link Iterable} with elements of the specified child type.
	 *
	 * <p>
	 * Attempts to convert the child objects to the correct type if they aren't already the correct type.
	 *
	 * <p>
	 * The <c>next()</c> method on the returned iterator may throw a {@link InvalidDataConversionException} if
	 * the next element cannot be converted to the specified type.
	 *
	 * <p>
	 * See {@link MarshallingSession#convertToType(Object, ClassMeta)} for a description of valid conversions.
	 *
	 * @param <E> The child object type.
	 * @param childType The child object type.
	 * @return A new <c>Iterable</c> object over this list.
	 */
	public <E> Iterable<E> elements(Class<E> childType) {
		final Iterator<?> iterator = iterator();
		return () -> new Iterator<>() {

			@Override /* Overridden from Iterator */
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override /* Overridden from Iterator */
			public E next() {
				return bs().convertToType(iterator.next(), childType);
			}

			@Override /* Overridden from Iterator */
			public void remove() {
				iterator.remove();
			}

		};
	}

	/**
	 * Get the entry at the specified index, converted to the specified type.
	 *
	 * <p>
	 * See {@link MarshallingSession#convertToType(Object, ClassMeta)} for the list of valid data conversions.
	 *
	 * @param index The index into this list.
	 * @param type The type of object to convert the entry to.
	 * @param <T> The type of object to convert the entry to.
	 * @return The converted entry.
	 */
	public <T> T get(int index, Class<T> type) {
		return bs().convertToType(get(index), type);
	}

	/**
	 * Get the entry at the specified index, converted to the specified type.
	 *
	 * <p>
	 * See {@link MarshallingSession#convertToType(Object, ClassMeta)} for the list of valid data conversions.
	 *
	 * @param index The index into this list.
	 * @param type The type of object to convert the entry to.
	 * @param args The type arguments of the type to convert the entry to.
	 * @param <T> The type of object to convert the entry to.
	 * @return The converted entry.
	 */
	public <T> T get(int index, Type type, Type...args) {
		return bs().convertToType(get(index), type, args);
	}

	/**
	 * Same as {@link #get(int,Class) get(int,Class)}, but the key is a slash-delimited path used to traverse entries in
	 * this POJO.
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
	 * Shortcut for calling <code>get(index, Boolean.<jk>class</jk>)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Boolean getBoolean(int index) {
		return get(index, Boolean.class);
	}

	/**
	 * Returns the {@link ClassMeta} of the class of the object at the specified index.
	 *
	 * @param index An index into this list, zero-based.
	 * @return The data type of the object at the specified index, or <jk>null</jk> if the value is null.
	 */
	@SuppressWarnings({
		"java:S1452"  // Wildcard required - ClassMeta<?> for element type metadata
	})
	public ClassMeta<?> getClassMeta(int index) {
		return bs().getClassMetaForObject(get(index));
	}

	/**
	 * Shortcut for calling <code>get(index, Integer.<jk>class</jk>)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Integer getInt(int index) {
		return get(index, Integer.class);
	}

	/**
	 * Shortcut for calling <code>get(index, MarshalledList.<jk>class</jk>)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public MarshalledList getList(int index) {
		return get(index, MarshalledList.class);
	}

	/**
	 * Same as {@link #getList(int)} except converts the elements to the specified types.
	 *
	 * @param <E> The element type.
	 * @param index The index.
	 * @param elementType The element type class.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public <E> List<E> getList(int index, Class<E> elementType) {
		return bs().convertToType(get(index), List.class, elementType);
	}

	/**
	 * Shortcut for calling <code>get(index, Long.<jk>class</jk>)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Long getLong(int index) {
		return get(index, Long.class);
	}

	/**
	 * Shortcut for calling <code>get(index, MarshalledMap.<jk>class</jk>)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public MarshalledMap getMap(int index) {
		return get(index, MarshalledMap.class);
	}

	/**
	 * Same as {@link #getMap(int)} except converts the keys and values to the specified types.
	 *
	 * @param <K> The key type class.
	 * @param <V> The value type class.
	 * @param index The index.
	 * @param keyType The key type class.
	 * @param valType The value type class.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public <K,V> Map<K,V> getMap(int index, Class<K> keyType, Class<V> valType) {
		return bs().convertToType(get(index), Map.class, keyType, valType);
	}

	/**
	 * Returns the {@link MarshallingSession} currently associated with this list.
	 *
	 * @return The {@link MarshallingSession} currently associated with this list.
	 */
	public MarshallingSession getMarshallingSession() { return session; }

	/**
	 * Shortcut for calling <code>get(index, String.<jk>class</jk>)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 */
	public String getString(int index) {
		return get(index, String.class);
	}

	/**
	 * Returns <jk>true</jk> if this list is unmodifiable.
	 *
	 * @return <jk>true</jk> if this list is unmodifiable.
	 */
	public boolean isUnmodifiable() { return false; }

	/**
	 * Returns a modifiable copy of this list if it's unmodifiable.
	 *
	 * @return A modifiable copy of this list if it's unmodifiable, or this list if it is already modifiable.
	 */
	public MarshalledList modifiable() {
		if (isUnmodifiable())
			return new MarshalledList(this);
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

	/**
	 * Same as {@link #set(int,Object) set(int,Object)}, but the key is a slash-delimited path used to traverse entries
	 * in this POJO.
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
	public MarshalledList session(MarshallingSession session) {
		this.session = session;
		return this;
	}

	/**
	 * Sets the {@link MarshallingSession} currently associated with this list.
	 *
	 * @param value The {@link MarshallingSession} currently associated with this list.
	 * @return This object.
	 */
	public MarshalledList setBeanSession(MarshallingSession value) {
		session = value;
		return this;
	}

	/**
	 * Returns an unmodifiable copy of this list if it's modifiable.
	 *
	 * @return An unmodifiable copy of this list if it's modifiable, or this list if it is already unmodifiable.
	 */
	public MarshalledList unmodifiable() {
		if (this instanceof UnmodifiableMarshalledList this2)
			return this2;
		return new UnmodifiableMarshalledList(this);
	}

	private ObjectRest getObjectRest() {
		if (objectRest == null)
			objectRest = new ObjectRest(this);
		return objectRest;
	}

	/**
	 * Returns the {@link MarshallingSession} associated with this list (lazily defaults to {@link MarshallingContext#DEFAULT_SESSION}).
	 *
	 * @return The {@link MarshallingSession} for this list.
	 */
	protected MarshallingSession bs() {
		if (session == null)
			session = MarshallingContext.DEFAULT_SESSION;
		return session;
	}
}
