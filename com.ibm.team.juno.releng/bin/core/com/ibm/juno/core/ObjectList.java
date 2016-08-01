/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Java implementation of a JSON array.
 * <p>
 * 	An extension of {@link LinkedList}, so all methods available to in that class are also available
 * 	to this class.
 * <p>
 * 	Note that the use of this class is optional.  The serializers will accept any objects that implement
 * 	the {@link Collection} interface.  But this class provides some useful additional functionality
 * 	when working with JSON models constructed from Java Collections Framework objects.  For example, a
 * 	constructor is provided for converting a JSON array string directly into a {@link List}.  It also contains
 * 	accessor methods for to avoid common typecasting when accessing elements in a list.
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Construct an empty List</jc>
 * 	List l = <jk>new</jk> ObjectList();
 *
 * 	<jc>// Construct a list of objects using various methods</jc>
 * 	l = <jk>new</jk> ObjectList().append(<js>"foo"</js>).append(123).append(<jk>true</jk>);
 * 	l = <jk>new</jk> ObjectList().append(<js>"foo"</js>, 123, <jk>true</jk>);  <jc>// Equivalent</jc>
 * 	l = <jk>new</jk> ObjectList(<js>"foo"</js>, 123, <jk>true</jk>);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Construct a list of integers from JSON</jc>
 * 	l = <jk>new</jk> ObjectList(<js>"[1,2,3]"</js>);
 *
 * 	<jc>// Construct a list of generic ObjectMap objects from JSON</jc>
 * 	l = <jk>new</jk> ObjectList(<js>"[{foo:'bar'},{baz:'bing'}]"</js>);
 *
 * 	<jc>// Construct a list of integers from XML</jc>
 * 	String xml = <js>"&lt;array&gt;&lt;number&gt;1&lt;/number&gt;&lt;number&gt;2&lt;/number&gt;&lt;number&gt;3&lt;/number&gt;&lt;/array&gt;"</js>;
 * 	l = <jk>new</jk> ObjectList(xml, DataFormat.<jsf>XML</jsf>);
 * 	l = (List)XmlParser.<jsf>DEFAULT</jsf>.parse(xml);  <jc>// Equivalent</jc>
 * 	l = (List)XmlParser.<jsf>DEFAULT</jsf>.parse(Object.<jk>class</jk>, xml);  <jc>// Equivalent</jc>
 * 	l = XmlParser.<jsf>DEFAULT</jsf>.parse(List.<jk>class</jk>, xml);  <jc>// Equivalent</jc>
 * 	l = XmlParser.<jsf>DEFAULT</jsf>.parse(ObjectList.<jk>class</jk>, xml);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Construct JSON from ObjectList</jc>
 * 	l = <jk>new</jk> ObjectList(<js>"[{foo:'bar'},{baz:'bing'}]"</js>);
 * 	String json = l.toString();  <jc>// Produces "[{foo:'bar'},{baz:'bing'}]"</jc>
 * 	json = l.toString(JsonSerializer.<jsf>DEFAULT_CONDENSED</jsf>);  <jc>// Equivalent</jc>
 * 	json = JsonSerializer.<jsf>DEFAULT_CONDENSED</jsf>.serialize(l);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Get one of the entries in the list as an Integer</jc>
 * 	l = <jk>new</jk> ObjectList(<js>"[1,2,3]"</js>);
 * 	Integer i = l.getInt(1);
 * 	i = l.get(Integer.<jk>class</jk>, 1);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Get one of the entries in the list as an Float</jc>
 * 	l = <jk>new</jk> ObjectList(<js>"[1,2,3]"</js>);
 * 	Float f = l.getFloat(1); <jc>// Returns 2f </jc>
 * 	f = l.get(Float.<jk>class</jk>, 1);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Same as above, except converted to a String</jc>
 * 	l = <jk>new</jk> ObjectList(<js>"[1,2,3]"</js>);
 * 	String s = l.getString(1); <jc>// Returns "2" </jc>
 * 	s = l.get(String.<jk>class</jk>, 1);  <jc>// Equivalent</jc>
 *
 * 	<jc>// Get one of the entries in the list as a bean (converted to a bean if it isn't already one)</jc>
 * 	l = <jk>new</jk> ObjectList(<js>"[{name:'John Smith',age:45}]"</js>);
 * 	Person p = l.get(Person.<jk>class</jk>, 0);
 *
 * 	<jc>// Iterate over a list of beans using the elements() method</jc>
 * 	ObjectList ObjectList = <jk>new</jk> ObjectList(<js>"[{name:'John Smith',age:45}]"</js>);
 * 	<jk>for</jk> (Person p : ObjectList.elements(Person.<jk>class</jk>) {
 * 		<jc>// Do something with p</jc>
 * 	}
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ObjectList extends LinkedList<Object> {
	private static final long serialVersionUID = 1L;

	private transient BeanContext beanContext = BeanContext.DEFAULT;

	/**
	 * An empty read-only ObjectList.
	 */
	public static final ObjectList EMPTY_LIST = new ObjectList() {
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

	/**
	 * Construct a JSON array directly from text using the specified parser.
	 *
	 * @param s The string being parsed.
	 * @param p The parser to use to parse the input.
	 * @throws ParseException If the input contains a syntax error or is malformed.
	 */
	public ObjectList(CharSequence s, ReaderParser p) throws ParseException {
		this(p == null ? BeanContext.DEFAULT : p.getBeanContext());
		try {
			if (p == null)
				p = JsonParser.DEFAULT;
			if (s != null)
				p.parseIntoCollection(new CharSequenceReader(s), s.length(), this, beanContext.object());
		} catch (IOException e) {
			throw new ParseException(e);
		}
	}

	/**
	 * Shortcut for <code><jk>new</jk> ObjectList(String,JsonParser.<jsf>DEFAULT</jsf>);</code>
	 *
	 * @param s The string being parsed.
	 * @throws ParseException If the input contains a syntax error or is malformed.
	 */
	public ObjectList(CharSequence s) throws ParseException {
		this(s, null);
	}

	/**
	 * Construct a JSON array directly from a reader using the specified parser.
	 *
	 * @param r The reader to read from.  Will automatically be wrapped in a {@link BufferedReader} if it isn't already a BufferedReader.
	 * @param p The parser to use to parse the input.
	 * @throws ParseException If the input contains a syntax error or is malformed.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public ObjectList(Reader r, ReaderParser p) throws ParseException, IOException {
		parseReader(r, p);
	}

	private void parseReader(Reader r, ReaderParser p) throws IOException, ParseException {
		if (p == null)
			p = JsonParser.DEFAULT;
		p.parseIntoCollection(r, -1, this, beanContext.object());
	}

	/**
	 * Construct an empty JSON array. (i.e. an empty {@link LinkedList}).
	 */
	public ObjectList() {
		this(BeanContext.DEFAULT);
	}

	/**
	 * Construct an empty JSON array with the specified bean context. (i.e. an empty {@link LinkedList}).
	 *
	 * @param beanContext The bean context to associate with this object list for creating beans.
	 */
	public ObjectList(BeanContext beanContext) {
		super();
		this.beanContext = beanContext;
	}

	/**
	 * Construct a JSON array and fill it with the specified objects.
	 *
	 * @param o A list of objects to add to this list.
	 */
	public ObjectList(Object... o) {
		super(Arrays.asList(o));
	}

	/**
	 * Construct a JSON array and fill it with the specified collection of objects.
	 *
	 * @param c A list of objects to add to this list.
	 */
	public ObjectList(Collection<?> c) {
		super(c);
	}

	/**
	 * Override the default bean context used for converting POJOs.
	 * <p>
	 * Default is {@link BeanContext#DEFAULT}, which is sufficient in most cases.
	 * <p>
	 * Useful if you're serializing/parsing beans with filters defined.
	 *
	 * @param beanContext The new bean context.
	 * @return This object (for method chaining).
	 */
	public ObjectList setBeanContext(BeanContext beanContext) {
		this.beanContext = beanContext;
		return this;
	}

	/**
	 * Convenience method for adding multiple objects to this list.
	 * @param o The objects to add to the list.
	 * @return This object (for method chaining).
	 */
	public ObjectList append(Object...o) {
		for (Object o2 : o)
			add(o2);
		return this;
	}

	/**
	 * Get the entry at the specified index, converted to the specified type (if possible).
	 * <p>
	 * 	See {@link BeanContext#convertToType(Object, ClassMeta)} for the list of valid data conversions.
	 *
	 * @param type The type of object to convert the entry to.
	 * @param index The index into this list.
	 * @param <T> The type of object to convert the entry to.
	 * @return The converted entry.
	 */
	public <T> T get(Class<T> type, int index) {
		return beanContext.convertToType(get(index), type);
	}

	/**
	 * Shortcut for calling <code>get(String.<jk>class</jk>, index)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 */
	public String getString(int index) {
		return get(String.class, index);
	}

	/**
	 * Shortcut for calling <code>get(Integer.<jk>class</jk>, index)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Integer getInt(int index) {
		return get(Integer.class, index);
	}

	/**
	 * Shortcut for calling <code>get(Boolean.<jk>class</jk>, index)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Boolean getBoolean(int index) {
		return get(Boolean.class, index);
	}

	/**
	 * Shortcut for calling <code>get(Long.<jk>class</jk>, index)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Long getLong(int index) {
		return get(Long.class, index);
	}

	/**
	 * Shortcut for calling <code>get(Map.<jk>class</jk>, index)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public Map<?,?> getMap(int index) {
		return get(Map.class, index);
	}

	/**
	 * Shortcut for calling <code>get(List.<jk>class</jk>, index)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public List<?> getList(int index) {
		return get(List.class, index);
	}

	/**
	 * Shortcut for calling <code>get(ObjectMap.<jk>class</jk>, index)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public ObjectMap getObjectMap(int index) {
		return get(ObjectMap.class, index);
	}

	/**
	 * Shortcut for calling <code>get(ObjectList.<jk>class</jk>, index)</code>.
	 *
	 * @param index The index.
	 * @return The converted value.
	 * @throws InvalidDataConversionException If value cannot be converted.
	 */
	public ObjectList getObjectList(int index) {
		return get(ObjectList.class, index);
	}

	/**
	 * Creates an {@link Iterable} with elements of the specified child type.
	 * <p>
	 * Attempts to convert the child objects to the correct type if they aren't already the correct type.
	 * <p>
	 * The <code>next()</code> method on the returned iterator may throw a {@link InvalidDataConversionException} if
	 * 	the next element cannot be converted to the specified type.
	 * <p>
	 * See {@link BeanContext#convertToType(Object, ClassMeta)} for a description of valid conversions.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jc>// Iterate over a list of ObjectMaps.</jc>
	 * 	ObjectList l = <jk>new</jk> ObjectList(<js>"[{foo:'bar'},{baz:123}]"</js>);
	 * 	for (ObjectMap m : l.elements(ObjectMap.<jk>class</jk>)) {
	 * 		<jc>// Do something with m.</jc>
	 * 	}
	 *
	 * 	<jc>// Iterate over a list of ints.</jc>
	 * 	ObjectList l = <jk>new</jk> ObjectList(<js>"[1,2,3]"</js>);
	 * 	for (Integer i : l.elements(Integer.<jk>class</jk>)) {
	 * 		<jc>// Do something with i.</jc>
	 * 	}
	 *
	 * 	<jc>// Iterate over a list of beans.</jc>
	 * 	<jc>// Automatically converts to beans.</jc>
	 * 	ObjectList l = <jk>new</jk> ObjectList(<js>"[{name:'John Smith',age:45}]"</js>);
	 * 	for (Person p : l.elements(Person.<jk>class</jk>)) {
	 * 		<jc>// Do something with p.</jc>
	 * 	}
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <E> The child object type.
	 * @param childType The child object type.
	 * @return A new <code>Iterable</code> object over this list.
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
						return beanContext.convertToType(i.next(), childType);
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
		return beanContext.getClassMetaForObject(get(index));
	}

	/**
	 * Serialize this array to a string using the specified serializer.
	 *
	 * @param serializer The serializer to use to convert this object to a string.
	 * @return This object as a serialized string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public String toString(WriterSerializer serializer) throws SerializeException {
		return serializer.serialize(this);
	}

	/**
	 * Serialize this array to JSON using the {@link JsonSerializer#DEFAULT} serializer.
	 */
	@Override /* Object */
	public String toString() {
		try {
			return this.toString(JsonSerializer.DEFAULT_LAX);
		} catch (SerializeException e) {
			return e.getLocalizedMessage();
		}
	}

	/**
	 * Convenience method for serializing this ObjectList to the specified Writer using
	 * the JsonSerializer.DEFAULT serializer.
	 *
	 * @param w The writer to send the serialized contents of this object.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public void serializeTo(Writer w) throws IOException, SerializeException {
		JsonSerializer.DEFAULT.serialize(this);
	}
}
