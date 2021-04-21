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
package org.apache.juneau.collections;

import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Java implementation of a JSON array.
 *
 * <p>
 * An extension of {@link LinkedList}, so all methods available to in that class are also available to this class.
 *
 * <p>
 * Note that the use of this class is optional for generating JSON.  The serializers will accept any objects that implement the
 * {@link Collection} interface.  But this class provides some useful additional functionality when working with JSON
 * models constructed from Java Collections Framework objects.  For example, a constructor is provided for converting a
 * JSON array string directly into a {@link List}.  It also contains accessor methods for to avoid common typecasting
 * when accessing elements in a list.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Construct an empty List</jc>
 * 	OList l = OList.<jsm>of</jsm>();
 *
 * 	<jc>// Construct a list of objects using various methods</jc>
 * 	l = OList.<jsm>of</jsm>().a(<js>"foo"</js>).a(123).a(<jk>true</jk>);
 * 	l = OList.<jsm>of</jsm>().a(<js>"foo"</js>, 123, <jk>true</jk>);  <jc>// Equivalent</jc>
 * 	l = OList.<jsm>of</jsm>(<js>"foo"</js>, 123, <jk>true</jk>);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Construct a list of integers from JSON</jc>
 * 	l = OList.<jsm>ofJson</jsm>(<js>"[1,2,3]"</js>);
 *
 * 	<jc>// Construct a list of generic OMap objects from JSON</jc>
 * 	l = OList.<jsm>ofJson</jsm>(<js>"[{foo:'bar'},{baz:'bing'}]"</js>);
 *
 * 	<jc>// Construct a list of integers from XML</jc>
 * 	String xml = <js>"&lt;array&gt;&lt;number&gt;1&lt;/number&gt;&lt;number&gt;2&lt;/number&gt;&lt;number&gt;3&lt;/number&gt;&lt;/array&gt;"</js>;
 * 	l = OList.<jsm>of</jsm>(xml, XmlParser.<jsf>DEFAULT</jsf>);
 * 	l = (List)XmlParser.<jsf>DEFAULT</jsf>.parse(xml);  <jc>// Equivalent</jc>
 * 	l = (List)XmlParser.<jsf>DEFAULT</jsf>.parse(Object.<jk>class</jk>, xml);  <jc>// Equivalent</jc>
 * 	l = XmlParser.<jsf>DEFAULT</jsf>.parse(List.<jk>class</jk>, xml);  <jc>// Equivalent</jc>
 * 	l = XmlParser.<jsf>DEFAULT</jsf>.parse(OList.<jk>class</jk>, xml);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Construct JSON from OList</jc>
 * 	l = OList.<jsm>ofJson</jsm>(<js>"[{foo:'bar'},{baz:'bing'}]"</js>);
 * 	String json = l.toString();  <jc>// Produces "[{foo:'bar'},{baz:'bing'}]"</jc>
 * 	json = l.toString(JsonSerializer.<jsf>DEFAULT</jsf>);  <jc>// Equivalent</jc>
 * 	json = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(l);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Get one of the entries in the list as an Integer</jc>
 * 	l = OList.<jsm>ofJson</jsm>(<js>"[1,2,3]"</js>);
 * 	Integer i = l.getInt(1);
 * 	i = l.get(Integer.<jk>class</jk>, 1);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Get one of the entries in the list as an Float</jc>
 * 	l = OList.<jsm>ofJson</jsm>(<js>"[1,2,3]"</js>);
 * 	Float f = l.getFloat(1); <jc>// Returns 2f </jc>
 * 	f = l.get(Float.<jk>class</jk>, 1);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Same as above, except converted to a String</jc>
 * 	l = OList.<jsm>ofJson</jsm>(<js>"[1,2,3]"</js>);
 * 	String s = l.getString(1); <jc>// Returns "2" </jc>
 * 	s = l.get(String.<jk>class</jk>, 1);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Get one of the entries in the list as a bean (converted to a bean if it isn't already one)</jc>
 * 	l = OList.<jsm>ofJson</jsm>(<js>"[{name:'John Smith',age:45}]"</js>);
 * 	Person p = l.get(Person.<jk>class</jk>, 0);
 *
 * 	<jc>// Iterate over a list of beans using the elements() method</jc>
 * 	OList l = OList.<jsm>ofJson</jsm>(<js>"[{name:'John Smith',age:45}]"</js>);
 * 	<jk>for</jk> (Person p : l.elements(Person.<jk>class</jk>) {
 * 		<jc>// Do something with p</jc>
 * 	}
 * </p>
 *
 * <p>
 * This class is not thread safe.
 */
public class OList extends LinkedList<Object> {
	private static final long serialVersionUID = 1L;

	transient BeanSession session = null;
	private transient PojoRest pojoRest;

	/**
	 * An empty read-only OList.
	 */
	public static final OList EMPTY_LIST = new OList() {
		private static final long serialVersionUID = 1L;

		@Override /* List */
		public void add(int location, Object object) {
			throw new UnsupportedOperationException();
		}

		@Override /* List */
		public ListIterator<Object> listIterator(final int location) {
			return Collections.emptyList().listIterator(location);
		}

		@Override /* List */
		public Object remove(int location) {
			throw new UnsupportedOperationException();
		}

		@Override /* List */
		public Object set(int location, Object object) {
			throw new UnsupportedOperationException();
		}

		@Override /* List */
		public List<Object> subList(int start, int end) {
			return Collections.emptyList().subList(start, end);
		}
	};

	//------------------------------------------------------------------------------------------------------------------
	// Constructors.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Construct an empty list.
	 */
	public OList() {}

	/**
	 * Construct an empty list with the specified bean context.
	 *
	 * @param session The bean session to use for creating beans.
	 */
	public OList(BeanSession session) {
		super();
		this.session = session;
	}

	/**
	 * Construct a list initialized with the specified list.
	 *
	 * @param copyFrom
	 * 	The list to copy.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public OList(Collection<?> copyFrom) {
		super(copyFrom);
	}

	/**
	 * Construct a list initialized with the specified JSON.
	 *
	 * @param json
	 * 	The JSON text to parse.
	 * 	<br>Can be normal or simplified JSON.
	 * @throws ParseException Malformed input encountered.
	 */
	public OList(CharSequence json) throws ParseException {
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
	public OList(CharSequence in, Parser p) throws ParseException {
		this(p == null ? null : p.createBeanSession());
		if (p == null)
			p = JsonParser.DEFAULT;
		if (in != null)
			p.parseIntoCollection(in, this, bs().object());
	}

	/**
	 * Construct a list initialized with the specified reader containing JSON.
	 *
	 * @param json
	 * 	The reader containing JSON text to parse.
	 * 	<br>Can contain normal or simplified JSON.
	 * @throws ParseException Malformed input encountered.
	 */
	public OList(Reader json) throws ParseException {
		parse(json, JsonParser.DEFAULT);
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
	 * @throws ParseException Malformed input encountered.
	 */
	public OList(Reader in, Parser p) throws ParseException {
		this(p == null ? null : p.createBeanSession());
		parse(in, p);
	}

	/**
	 * Construct a list initialized with the contents.
	 *
	 * @param entries The entries to add to this list.
	 */
	public OList(Object... entries) {
		super();
		Collections.addAll(this, entries);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Creators.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Construct an empty list.
	 *
	 * @return An empty list.
	 */
	public static OList create() {
		return new OList();
	}

	/**
	 * Construct a list initialized with the specified list.
	 *
	 * @param values
	 * 	The list to copy.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new list or <jk>null</jk> if the list was <jk>null</jk>.
	 */
	public static OList of(Collection<?> values) {
		return values == null ? null : new OList(values);
	}

	/**
	 * Convenience method for creating a list of collection objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static OList ofCollections(Collection<?>...values) {
		OList l = new OList();
		for (Collection<?> v : values)
			l.add(v);
		return l;
	}

	/**
	 * Convenience method for creating a list of array objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static OList ofArrays(Object[]...values) {
		OList l = new OList();
		for (Object[] v : values)
			l.add(v);
		return l;
	}

	/**
	 * Construct a list initialized with the specified JSON string.
	 *
	 * @param json
	 * 	The JSON text to parse.
	 * 	<br>Can be normal or simplified JSON.
	 * @return A new list or <jk>null</jk> if the string was null.
	 * @throws ParseException Malformed input encountered.
	 */
	public static OList ofJson(CharSequence json) throws ParseException {
		return json == null ? null : new OList(json);
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
	 * @return A new list or <jk>null</jk> if the input was <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static OList ofText(CharSequence in, Parser p) throws ParseException {
		return in == null ? null : new OList(in, p);
	}

	/**
	 * Construct a list initialized with the specified reader containing JSON.
	 *
	 * @param json
	 * 	The reader containing JSON text to parse.
	 * 	<br>Can contain normal or simplified JSON.
	 * @return A new list or <jk>null</jk> if the input was <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static OList ofJson(Reader json) throws ParseException {
		return json == null ? null : new OList(json);
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
	 * @return A new list or <jk>null</jk> if the input was <jk>null</jk>.
	 * @throws ParseException Malformed input encountered.
	 */
	public static OList ofText(Reader in, Parser p) throws ParseException {
		return in == null ? null : new OList(in);
	}

	/**
	 * Construct a list initialized with the specified values.
	 *
	 * @param values The values to add to this list.
	 * @return A new list, never <jk>null</jk>.
	 */
	public static OList of(Object... values) {
		return new OList(values);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Initializers.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Override the default bean session used for converting POJOs.
	 *
	 * <p>
	 * Default is {@link BeanContext#DEFAULT}, which is sufficient in most cases.
	 *
	 * <p>
	 * Useful if you're serializing/parsing beans with transforms defined.
	 *
	 * @param session The new bean session.
	 * @return This object (for method chaining).
	 */
	public OList session(BeanSession session) {
		this.session = session;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Adds the value to this list.
	 *
	 * @param value The value to add to this list.
	 * @return This object (for method chaining).
	 */
	public OList append(Object value) {
		add(value);
		return this;
	}

	/**
	 * Adds all the values in the specified array to this list.
	 *
	 * @param values The values to add to this list.
	 * @return This object (for method chaining).
	 */
	public OList append(Object...values) {
		Collections.addAll(this, values);
		return this;
	}

	/**
	 * Adds all the values in the specified collection to this list.
	 *
	 * @param values The values to add to this list.
	 * @return This object (for method chaining).
	 */
	public OList append(Collection<?> values) {
		if (values != null)
			addAll(values);
		return this;
	}

	/**
	 * Same as {@link #append(Object)}.
	 *
	 * @param value The entry to add to this list.
	 * @return This object (for method chaining).
	 */
	public OList a(Object value) {
		return append(value);
	}

	/**
	 * Same as {@link #append(Collection)}.
	 *
	 * @param values The collection to add to this list.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public OList a(Collection<?> values) {
		return append(values);
	}

	/**
	 * Same as {@link #append(Object...)}.
	 *
	 * @param values The array to add to this list.
	 * @return This object (for method chaining).
	 */
	public OList a(Object...values) {
		return append(values);
	}

	/**
	 * Adds an entry to this list if the boolean flag is <jk>true</jk>.
	 *
	 * @param flag The boolean flag.
	 * @param value The value to add.
	 * @return This object (for method chaining).
	 */
	public OList appendIf(boolean flag, Object value) {
		if (flag)
			a(value);
		return this;
	}

	/**
	 * Adds entries to this list skipping <jk>null</jk> values.
	 *
	 * @param values The objects to add to the list.
	 * @return This object (for method chaining).
	 */
	public OList appendIfNotNull(Object...values) {
		for (Object o2 : values)
			if (o2 != null)
				a(o2);
		return this;
	}

	/**
	 * Adds all the entries in the specified collection to this list in reverse order.
	 *
	 * @param values The collection to add to this list.
	 * @return This object (for method chaining).
	 */
	public OList appendReverse(List<?> values) {
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
	 * @return This object (for method chaining).
	 */
	public OList appendReverse(Object...values) {
		for (int i = values.length - 1; i >= 0; i--)
			add(values[i]);
		return this;
	}

	/**
	 * Add values but skip any strings that are <jk>null</jk> or empty.
	 *
	 * @param values The objects to add to the list.
	 * @return This object (for method chaining).
	 */
	public OList appendIfNotEmpty(String...values) {
		for (String s : values)
			if (isNotEmpty(s))
				add(s);
		return this;
	}

	/**
	 * Add if predicate matches.
	 *
	 * @param test The predicate to match against.
	 * @param value The value to add if the predicate matches.
	 * @return This object (for method chaining).
	 */
	public OList appendIf(Predicate<Object> test, Object value) {
		return appendIf(test.test(value), value);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Retrievers.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Get the entry at the specified index, converted to the specified type.
	 *
	 * <p>
	 * This is the preferred get method for simple types.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	OList l = OList.<jsm>ofJson</jsm>(<js>"..."</js>);
	 *
	 * 	<jc>// Value converted to a string.</jc>
	 * 	String s = l.get(1, String.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a bean.</jc>
	 * 	MyBean b = l.get(2, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a bean array.</jc>
	 * 	MyBean[] ba = l.get(3, MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a linked-list of objects.</jc>
	 * 	List l1 = l.get(4, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a map of object keys/values.</jc>
	 * 	Map m1 = l.get(5, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * See {@link BeanSession#convertToType(Object, ClassMeta)} for the list of valid data conversions.
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
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	OList l = OList.<jsm>ofJson</jsm>(<js>"..."</js>);
	 *
	 * 	<jc>// Value converted to a linked-list of strings.</jc>
	 * 	List&lt;String&gt; l1 = l.get(1, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a linked-list of beans.</jc>
	 * 	List&lt;MyBean&gt; l2 = l.get(2, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; l3 = l.get(3, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; m1 = l.get(4, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Value converted to a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; m2 = l.get(5, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
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
	 * <p>
	 * See {@link BeanSession#convertToType(Object, ClassMeta)} for the list of valid data conversions.
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
	 * Shortcut for calling <code>get(index, String.<jk>class</jk>)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 */
	public String getString(int index) {
		return get(index, String.class);
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
	 * Shortcut for calling <code>get(index, OMap.<jk>class</jk>)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public OMap getMap(int index) {
		return get(index, OMap.class);
	}

	/**
	 * Same as {@link #getMap(int)} except converts the keys and values to the specified types.
	 *
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
	 * Shortcut for calling <code>get(index, OList.<jk>class</jk>)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public OList getList(int index) {
		return get(index, OList.class);
	}

	/**
	 * Same as {@link #getList(int)} except converts the elements to the specified types.
	 *
	 * @param index The index.
	 * @param elementType The element type class.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public <E> List<E> getList(int index, Class<E> elementType) {
		return bs().convertToType(get(index), List.class, elementType);
	}

	//------------------------------------------------------------------------------------------------------------------
	// POJO REST methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Same as {@link #get(int,Class) get(int,Class)}, but the key is a slash-delimited path used to traverse entries in
	 * this POJO.
	 *
	 * <p>
	 * For example, the following code is equivalent:
	 * </p>
	 * <p class='bcode w800'>
	 * 	OList ol = OList.<jsm>ofJson</jsm>(<js>"..."</js>);
	 *
	 * 	<jc>// Long way</jc>
	 * 	<jk>long</jk> l = ol.getMap(<js>"0"</js>).getLong(<js>"baz"</js>);
	 *
	 * 	<jc>// Using this method</jc>
	 * 	<jk>long</jk> l = ol.getAt(<js>"0/baz"</js>, <jk>long</jk>.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * This method uses the {@link PojoRest} class to perform the lookup, so the map can contain any of the various
	 * class types that the {@link PojoRest} class supports (e.g. beans, collections, arrays).
	 *
	 * @param path The path to the entry.
	 * @param type The class type.
	 *
	 * @param <T> The class type.
	 * @return The value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public <T> T getAt(String path, Class<T> type) {
		return getPojoRest().get(path, type);
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
		return getPojoRest().get(path, type, args);
	}

	/**
	 * Same as {@link #set(int,Object) set(int,Object)}, but the key is a slash-delimited path used to traverse entries
	 * in this POJO.
	 *
	 * <p>
	 * For example, the following code is equivalent:
	 * </p>
	 * <p class='bcode w800'>
	 * 	OList ol = OList.<jsm>ofJson</jsm>(<js>"..."</js>);
	 *
	 * 	<jc>// Long way</jc>
	 * 	ol.getMap(<js>"0"</js>).put(<js>"baz"</js>, 123);
	 *
	 * 	<jc>// Using this method</jc>
	 * 	ol.putAt(<js>"0/baz"</js>, 123);
	 * </p>
	 *
	 * <p>
	 * This method uses the {@link PojoRest} class to perform the lookup, so the map can contain any of the various
	 * class types that the {@link PojoRest} class supports (e.g. beans, collections, arrays).
	 *
	 * @param path The path to the entry.
	 * @param o The new value.
	 * @return The previous value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public Object putAt(String path, Object o) {
		return getPojoRest().put(path, o);
	}

	/**
	 * Similar to {@link #putAt(String,Object) putAt(String,Object)}, but used to append to collections and arrays.
	 *
	 * <p>
	 * For example, the following code is equivalent:
	 * </p>
	 * <p class='bcode w800'>
	 * 	OList ol = OList.<jsm>ofJson</jsm>(<js>"..."</js>);
	 *
	 * 	<jc>// Long way</jc>
	 * 	ol.getMap(0).getList(<js>"bar"</js>).append(123);
	 *
	 * 	<jc>// Using this method</jc>
	 * 	ol.postAt(<js>"0/bar"</js>, 123);
	 * </p>
	 *
	 * <p>
	 * This method uses the {@link PojoRest} class to perform the lookup, so the map can contain any of the various
	 * class types that the {@link PojoRest} class supports (e.g. beans, collections, arrays).
	 *
	 * @param path The path to the entry.
	 * @param o The new value.
	 * @return The previous value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public Object postAt(String path, Object o) {
		return getPojoRest().post(path, o);
	}

	/**
	 * Similar to {@link #remove(int) remove(int)},but the key is a slash-delimited path used to traverse entries in
	 * this POJO.
	 *
	 * <p>
	 * For example, the following code is equivalent:
	 * </p>
	 * <p class='bcode w800'>
	 * 	OList ol = OList.<jsm>ofJson</jsm>(<js>"..."</js>);
	 *
	 * 	<jc>// Long way</jc>
	 * 	ol.getMap(0).getList(<js>"bar"</js>).delete(0);
	 *
	 * 	<jc>// Using this method</jc>
	 * 	m.deleteAt(<js>"0/bar/0"</js>);
	 * </p>
	 *
	 * <p>
	 * This method uses the {@link PojoRest} class to perform the lookup, so the map can contain any of the various
	 * class types that the {@link PojoRest} class supports (e.g. beans, collections, arrays).
	 *
	 * @param path The path to the entry.
	 * @return The previous value, or <jk>null</jk> if the entry doesn't exist.
	 */
	public Object deleteAt(String path) {
		return getPojoRest().delete(path);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the {@link BeanSession} currently associated with this list.
	 *
	 * @return The {@link BeanSession} currently associated with this list.
	 */
	public BeanSession getBeanSession() {
		return session;
	}

	/**
	 * Sets the {@link BeanSession} currently associated with this list.
	 *
	 * @param value The {@link BeanSession} currently associated with this list.
	 * @return This object (for method chaining).
	 */
	public OList setBeanSession(BeanSession value) {
		this.session = value;
		return this;
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
	 * See {@link BeanSession#convertToType(Object, ClassMeta)} for a description of valid conversions.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Iterate over a list of OMaps.</jc>
	 * 	OList l = OList.<jsm>ofJson</jsm>(<js>"[{foo:'bar'},{baz:123}]"</js>);
	 * 	for (OMap m : l.elements(OMap.<jk>class</jk>)) {
	 * 		<jc>// Do something with m.</jc>
	 * 	}
	 *
	 * 	<jc>// Iterate over a list of ints.</jc>
	 * 	OList l = OList.<jsm>ofJson</jsm>(<js>"[1,2,3]"</js>);
	 * 	for (Integer i : l.elements(Integer.<jk>class</jk>)) {
	 * 		<jc>// Do something with i.</jc>
	 * 	}
	 *
	 * 	<jc>// Iterate over a list of beans.</jc>
	 * 	<jc>// Automatically converts to beans.</jc>
	 * 	OList l = OList.<jsm>ofJson</jsm>(<js>"[{name:'John Smith',age:45}]"</js>);
	 * 	for (Person p : l.elements(Person.<jk>class</jk>)) {
	 * 		<jc>// Do something with p.</jc>
	 * 	}
	 * </p>
	 *
	 * @param <E> The child object type.
	 * @param childType The child object type.
	 * @return A new <c>Iterable</c> object over this list.
	 */
	public <E> Iterable<E> elements(final Class<E> childType) {
		final Iterator<?> i = iterator();
		return new Iterable<E>() {

			@Override /* Iterable */
			public Iterator<E> iterator() {
				return new Iterator<E>() {

					@Override /* Iterator */
					public boolean hasNext() {
						return i.hasNext();
					}

					@Override /* Iterator */
					public E next() {
						return bs().convertToType(i.next(), childType);
					}

					@Override /* Iterator */
					public void remove() {
						i.remove();
					}

				};
			}
		};
	}

	/**
	 * Returns the {@link ClassMeta} of the class of the object at the specified index.
	 *
	 * @param index An index into this list, zero-based.
	 * @return The data type of the object at the specified index, or <jk>null</jk> if the value is null.
	 */
	public ClassMeta<?> getClassMeta(int index) {
		return bs().getClassMetaForObject(get(index));
	}

	/**
	 * Serialize this array to a string using the specified serializer.
	 *
	 * @param serializer The serializer to use to convert this object to a string.
	 * @return This object as a serialized string.
	 */
	public String asString(WriterSerializer serializer) {
		return serializer.toString(this);
	}

	/**
	 * Serialize this array to Simplified JSON.
	 *
	 * @return This object as a serialized string.
	 */
	public String asString() {
		return SimpleJsonSerializer.DEFAULT.toString(this);
	}

	/**
	 * Returns <jk>true</jk> if this list is unmodifiable.
	 *
	 * @return <jk>true</jk> if this list is unmodifiable.
	 */
	public boolean isUnmodifiable() {
		return false;
	}

	/**
	 * Returns a modifiable copy of this list if it's unmodifiable.
	 *
	 * @return A modifiable copy of this list if it's unmodifiable, or this list if it is already modifiable.
	 */
	public OList modifiable() {
		if (isUnmodifiable())
			return new OList(this);
		return this;
	}

	/**
	 * Returns an unmodifiable copy of this list if it's modifiable.
	 *
	 * @return An unmodifiable copy of this list if it's modifiable, or this list if it is already unmodifiable.
	 */
	public OList unmodifiable() {
		if (this instanceof UnmodifiableOList)
			return this;
		return new UnmodifiableOList(this);
	}

	/**
	 * Convenience method for serializing this OList to the specified Writer using the JsonSerializer.DEFAULT
	 * serializer.
	 *
	 * @param w The writer to send the serialized contents of this object.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public OList writeTo(Writer w) throws IOException, SerializeException {
		JsonSerializer.DEFAULT.serialize(this, w);
		return this;
	}

	/**
	 * Converts this object into the specified class type.
	 *
	 * <p>
	 * TODO - The current implementation is very inefficient.
	 *
	 * @param cm The class type to convert this object to.
	 * @return A converted object.
	 */
	public Object cast(ClassMeta<?> cm) {
		try {
			return JsonParser.DEFAULT.parse(SimpleJsonSerializer.DEFAULT.serialize(this), cm);
		} catch (ParseException | SerializeException e) {
			throw runtimeException(e);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods.
	//------------------------------------------------------------------------------------------------------------------

	private void parse(Reader r, Parser p) throws ParseException {
		if (p == null)
			p = JsonParser.DEFAULT;
		p.parseIntoCollection(r, this, bs().object());
	}

	private PojoRest getPojoRest() {
		if (pojoRest == null)
			pojoRest = new PojoRest(this);
		return pojoRest;
	}

	BeanSession bs() {
		if (session == null)
			session = BeanContext.DEFAULT.createBeanSession();
		return session;
	}

	private static final class UnmodifiableOList extends OList {
		private static final long serialVersionUID = 1L;

		UnmodifiableOList(OList contents) {
			super();
			if (contents != null) {
				for (Object e : this) {
					super.add(e);
				}
			}
		}

		@Override /* List */
		public void add(int location, Object object) {
			throw new UnsupportedOperationException("OList is read-only.");
		}

		@Override /* List */
		public Object remove(int location) {
			throw new UnsupportedOperationException("OList is read-only.");
		}

		@Override /* List */
		public Object set(int location, Object object) {
			throw new UnsupportedOperationException("OList is read-only.");
		}

		@Override
		public final boolean isUnmodifiable() {
			return true;
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Overridden methods.
	//------------------------------------------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return SimpleJson.DEFAULT.toString(this);
	}
}
