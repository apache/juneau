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
package org.apache.juneau.urlencoding;

import static org.apache.juneau.urlencoding.UrlEncodingSerializerContext.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJO models to URL-encoded notation with UON-encoded values (a notation for URL-encoded query paramter values).
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Accept</code> types: <code>application/x-www-form-urlencoded</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>application/x-www-form-urlencoded</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * This serializer provides several serialization options.  Typically, one of the predefined DEFAULT serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link UonSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 * <p>
 * The following shows a sample object defined in Javascript:
 * </p>
 * <p class='bcode'>
 * 	{
 * 		id: 1,
 * 		name: <js>'John Smith'</js>,
 * 		uri: <js>'http://sample/addressBook/person/1'</js>,
 * 		addressBookUri: <js>'http://sample/addressBook'</js>,
 * 		birthDate: <js>'1946-08-12T00:00:00Z'</js>,
 * 		otherIds: <jk>null</jk>,
 * 		addresses: [
 * 			{
 * 				uri: <js>'http://sample/addressBook/address/1'</js>,
 * 				personUri: <js>'http://sample/addressBook/person/1'</js>,
 * 				id: 1,
 * 				street: <js>'100 Main Street'</js>,
 * 				city: <js>'Anywhereville'</js>,
 * 				state: <js>'NY'</js>,
 * 				zip: 12345,
 * 				isCurrent: <jk>true</jk>,
 * 			}
 * 		]
 * 	}
 * </p>
 * <p>
 * Using the "strict" syntax defined in this document, the equivalent
 * 	URL-encoded notation would be as follows:
 * </p>
 * <p class='bcode'>
 * 	<ua>id</ua>=<un>1</un>
 * 	&amp;<ua>name</ua>=<us>'John+Smith'</us>,
 * 	&amp;<ua>uri</ua>=<us>http://sample/addressBook/person/1</us>,
 * 	&amp;<ua>addressBookUri</ua>=<us>http://sample/addressBook</us>,
 * 	&amp;<ua>birthDate</ua>=<us>1946-08-12T00:00:00Z</us>,
 * 	&amp;<ua>otherIds</ua>=<uk>null</uk>,
 * 	&amp;<ua>addresses</ua>=@(
 * 		(
 * 			<ua>uri</ua>=<us>http://sample/addressBook/address/1</us>,
 * 			<ua>personUri</ua>=<us>http://sample/addressBook/person/1</us>,
 * 			<ua>id</ua>=<un>1</un>,
 * 			<ua>street</ua>=<us>'100+Main+Street'</us>,
 * 			<ua>city</ua>=<us>Anywhereville</us>,
 * 			<ua>state</ua>=<us>NY</us>,
 * 			<ua>zip</ua>=<un>12345</un>,
 * 			<ua>isCurrent</ua>=<uk>true</uk>
 * 		)
 * 	)
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Serialize a Map</jc>
 * 	Map m = <jk>new</jk> ObjectMap(<js>"{a:'b',c:1,d:false,e:['f',1,false],g:{h:'i'}}"</js>);
 *
 * 	<jc>// Serialize to value equivalent to JSON.</jc>
 * 	<jc>// Produces "a=b&amp;c=1&amp;d=false&amp;e=@(f,1,false)&amp;g=(h=i)"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 *
 * 	<jc>// Serialize a bean</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> Person(String s);
 * 		<jk>public</jk> String getName();
 * 		<jk>public int</jk> getAge();
 * 		<jk>public</jk> Address getAddress();
 * 		<jk>public boolean</jk> deceased;
 * 	}
 *
 * 	<jk>public class</jk> Address {
 * 		<jk>public</jk> String getStreet();
 * 		<jk>public</jk> String getCity();
 * 		<jk>public</jk> String getState();
 * 		<jk>public int</jk> getZip();
 * 	}
 *
 * 	Person p = <jk>new</jk> Person(<js>"John Doe"</js>, 23, <js>"123 Main St"</js>, <js>"Anywhere"</js>, <js>"NY"</js>, 12345, <jk>false</jk>);
 *
 * 	<jc>// Produces "name=John+Doe&amp;age=23&amp;address=(street='123+Main+St',city=Anywhere,state=NY,zip=12345)&amp;deceased=false"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 * </p>
 */
@Produces("application/x-www-form-urlencoded")
@SuppressWarnings("hiding")
public class UrlEncodingSerializer extends UonSerializer {

	/** Reusable instance of {@link UrlEncodingSerializer}, all default settings. */
	public static final UrlEncodingSerializer DEFAULT = new UrlEncodingSerializer().lock();

	/** Reusable instance of {@link UrlEncodingSerializer.Expanded}. */
	public static final UrlEncodingSerializer DEFAULT_EXPANDED = new Expanded().lock();

	/** Reusable instance of {@link UrlEncodingSerializer.Readable}. */
	public static final UrlEncodingSerializer DEFAULT_READABLE = new Readable().lock();

	/**
	 * Constructor.
	 */
	public UrlEncodingSerializer() {
		setEncodeChars(true);
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializer().setSimpleMode(<jk>true</jk>).setExpandedParams(<jk>true</jk>);</code>.
	 */
	@Produces(value="application/x-www-form-urlencoded",contentType="application/x-www-form-urlencoded")
	public static class Expanded extends UrlEncodingSerializer {
		/** Constructor */
		public Expanded() {
			setExpandedParams(true);
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializer().setUseWhitespace(<jk>true</jk>);</code>.
	 */
	public static class Readable extends UrlEncodingSerializer {
		/** Constructor */
		public Readable() {
			setUseWhitespace(true);
		}
	}

	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeAnything(UrlEncodingSerializerSession session, UonWriter out, Object o) throws Exception {

		ClassMeta<?> aType;			// The actual type
		ClassMeta<?> sType;			// The serialized type

		aType = session.push("root", o, object());
		session.indent--;
		if (aType == null)
			aType = object();

		sType = aType.getSerializedClassMeta();
		String typeName = session.getBeanTypeName(session.object(), aType, null);

		// Swap if necessary
		PojoSwap swap = aType.getPojoSwap();
		if (swap != null) {
			o = swap.swap(session, o);

			// If the getSwapClass() method returns Object, we need to figure out
			// the actual type now.
			if (sType.isObject())
				sType = session.getClassMetaForObject(o);
		}

		if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(session, out, (BeanMap)o, typeName);
			else
				serializeMap(session, out, (Map)o, sType);
		} else if (sType.isBean()) {
			serializeBeanMap(session, out, session.toBeanMap(o), typeName);
		} else if (sType.isCollection()) {
			serializeMap(session, out, getCollectionMap((Collection)o), session.getClassMeta(Map.class, Integer.class, sType.getElementType()));
		} else {
			// All other types can't be serialized as key/value pairs, so we create a
			// mock key/value pair with a "_value" key.
			out.append("_value=");
			super.serializeAnything(session, out, o, null, null, null);
		}

		session.pop();
		return out;
	}

	private Map<Integer,Object> getCollectionMap(Collection<?> c) {
		Map<Integer,Object> m = new TreeMap<Integer,Object>();
		int i = 0;
		for (Object o : c)
			m.put(i++, o);
		return m;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(UrlEncodingSerializerSession session, UonWriter out, Map m, ClassMeta<?> type) throws Exception {

		m = session.sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		int depth = session.getIndent();
		boolean addAmp = false;

		Iterator mapEntries = m.entrySet().iterator();

		while (mapEntries.hasNext()) {
			Map.Entry e = (Map.Entry) mapEntries.next();
			Object value = e.getValue();
			Object key = session.generalize(e.getKey(), keyType);


			if (session.shouldUseExpandedParams(value)) {
				Iterator i = value instanceof Collection ? ((Collection)value).iterator() : ArrayUtils.iterator(value);
				while (i.hasNext()) {
					if (addAmp)
						out.cr(depth).append('&');
					out.appendObject(key, true).append('=');
					super.serializeAnything(session, out, i.next(), null, (key == null ? null : key.toString()), null);
					addAmp = true;
				}
			} else {
				if (addAmp)
					out.cr(depth).append('&');
				out.appendObject(key, true).append('=');
				super.serializeAnything(session, out, value, valueType, (key == null ? null : key.toString()), null);
				addAmp = true;
			}
		}

		return out;
	}

	@SuppressWarnings({ "rawtypes" })
	private SerializerWriter serializeBeanMap(UrlEncodingSerializerSession session, UonWriter out, BeanMap<?> m, String typeName) throws Exception {
		int depth = session.getIndent();

		boolean addAmp = false;

		for (BeanPropertyValue p : m.getValues(session.isTrimNulls(), typeName != null ? session.createBeanTypeNameProperty(m, typeName) : null)) {
			BeanPropertyMeta pMeta = p.getMeta();
			ClassMeta<?> cMeta = p.getClassMeta();

			String key = p.getName();
			Object value = p.getValue();
			Throwable t = p.getThrown();
			if (t != null)
				session.addBeanGetterWarning(pMeta, t);

			if (session.canIgnoreValue(cMeta, key, value))
				continue;

			if (value != null && session.shouldUseExpandedParams(pMeta)) {
				// Transformed object array bean properties may be transformed resulting in ArrayLists,
				// so we need to check type if we think it's an array.
				Iterator i = (cMeta.isCollection() || value instanceof Collection) ? ((Collection)value).iterator() : ArrayUtils.iterator(value);
				while (i.hasNext()) {
					if (addAmp)
						out.cr(depth).append('&');

					out.appendObject(key, true).append('=');

					super.serializeAnything(session, out, i.next(), cMeta.getElementType(), key, pMeta);

					addAmp = true;
				}
			} else {
				if (addAmp)
					out.cr(depth).append('&');

				out.appendObject(key, true).append('=');

				super.serializeAnything(session, out, value, cMeta, key, pMeta);

				addAmp = true;
			}

		}
		return out;
	}


	//--------------------------------------------------------------------------------
	// Methods for constructing individual parameter values.
	//--------------------------------------------------------------------------------

	/**
	 * Converts the specified object to a string using this serializers {@link BeanSession#convertToType(Object, Class)} method
	 * 	and runs {@link URLEncoder#encode(String,String)} against the results.
	 * Useful for constructing URL parts.
	 *
	 * @param o The object to serialize.
	 * @return The serialized object.
	 */
	public String serializeUrlPart(Object o) {
		try {
			// Shortcut for simple types.
			ClassMeta<?> cm = getBeanContext().getClassMetaForObject(o);
			if (cm != null)
				if (cm.isCharSequence() || cm.isNumber() || cm.isBoolean())
					return o.toString();

			StringWriter w = new StringWriter();
			UonSerializerSession s = createSession(w, null, null, null, null, MediaType.UON);
			super.doSerialize(s, o);
			return w.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public UrlEncodingSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new UrlEncodingSerializerSession(getContext(UrlEncodingSerializerContext.class), op, output, javaMethod, locale, timeZone, mediaType);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		UrlEncodingSerializerSession s = (UrlEncodingSerializerSession)session;
		serializeAnything(s, s.getWriter(), o);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Serialize bean property collections/arrays as separate key/value pairs.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UrlEncoding.expandedParams"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>false</jk>, serializing the array <code>[1,2,3]</code> results in <code>?key=$a(1,2,3)</code>.
	 * If <jk>true</jk>, serializing the same array results in <code>?key=1&amp;key=2&amp;key=3</code>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> String[] f1 = {<js>"a"</js>,<js>"b"</js>};
	 * 		<jk>public</jk> List&lt;String&gt; f2 = <jk>new</jk> LinkedList&lt;String&gt;(Arrays.<jsm>asList</jsm>(<jk>new</jk> String[]{<js>"c"</js>,<js>"d"</js>}));
	 * 	}
	 *
	 * 	UrlEncodingSerializer s1 = <jk>new</jk> UrlEncodingParser();
	 * 	UrlEncodingSerializer s2 = <jk>new</jk> UrlEncodingParser().setExpandedParams(<jk>true</jk>);
	 *
	 * 	String s1 = p1.serialize(<jk>new</jk> A()); <jc>// Produces "f1=(a,b)&amp;f2=(c,d)"</jc>
	 * 	String s2 = p2.serialize(<jk>new</jk> A()); <jc>// Produces "f1=a&amp;f1=b&amp;f2=c&amp;f2=d"</jc>
	 * </p>
	 * <p>
	 * This option only applies to beans.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>If parsing multi-part parameters, it's highly recommended to use Collections or Lists
	 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * 		is added to it.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>URLENC_expandedParams</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see UrlEncodingSerializerContext#URLENC_expandedParams
	 */
	public UrlEncodingSerializer setExpandedParams(boolean value) throws LockedException {
		return setProperty(URLENC_expandedParams, value);
	}

	@Override /* UonSerializer */
	public UrlEncodingSerializer setEncodeChars(boolean value) throws LockedException {
		super.setEncodeChars(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setMaxDepth(int value) throws LockedException {
		super.setMaxDepth(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setInitialDepth(int value) throws LockedException {
		super.setInitialDepth(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setDetectRecursions(boolean value) throws LockedException {
		super.setDetectRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setIgnoreRecursions(boolean value) throws LockedException {
		super.setIgnoreRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setUseWhitespace(boolean value) throws LockedException {
		super.setUseWhitespace(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setAddBeanTypeProperties(boolean value) throws LockedException {
		super.setAddBeanTypeProperties(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setQuoteChar(char value) throws LockedException {
		super.setQuoteChar(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setTrimNullProperties(boolean value) throws LockedException {
		super.setTrimNullProperties(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setTrimEmptyCollections(boolean value) throws LockedException {
		super.setTrimEmptyCollections(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setTrimEmptyMaps(boolean value) throws LockedException {
		super.setTrimEmptyMaps(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setTrimStrings(boolean value) throws LockedException {
		super.setTrimStrings(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setRelativeUriBase(String value) throws LockedException {
		super.setRelativeUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setAbsolutePathUriBase(String value) throws LockedException {
		super.setAbsolutePathUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setSortCollections(boolean value) throws LockedException {
		super.setSortCollections(value);
		return this;
	}

	@Override /* Serializer */
	public UrlEncodingSerializer setSortMaps(boolean value) throws LockedException {
		super.setSortMaps(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		super.setBeansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeansRequireSerializable(boolean value) throws LockedException {
		super.setBeansRequireSerializable(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeansRequireSettersForGetters(boolean value) throws LockedException {
		super.setBeansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeansRequireSomeProperties(boolean value) throws LockedException {
		super.setBeansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		super.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeanConstructorVisibility(Visibility value) throws LockedException {
		super.setBeanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeanClassVisibility(Visibility value) throws LockedException {
		super.setBeanClassVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeanFieldVisibility(Visibility value) throws LockedException {
		super.setBeanFieldVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setMethodVisibility(Visibility value) throws LockedException {
		super.setMethodVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setUseJavaBeanIntrospector(boolean value) throws LockedException {
		super.setUseJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setUseInterfaceProxies(boolean value) throws LockedException {
		super.setUseInterfaceProxies(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		super.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setSortProperties(boolean value) throws LockedException {
		super.setSortProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setNotBeanPackages(String...values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setNotBeanPackages(Collection<String> values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addNotBeanPackages(String...values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addNotBeanPackages(Collection<String> values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removeNotBeanPackages(String...values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removeNotBeanPackages(Collection<String> values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setNotBeanClasses(Class<?>...values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addNotBeanClasses(Class<?>...values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removeNotBeanClasses(Class<?>...values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeanFilters(Class<?>...values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addBeanFilters(Class<?>...values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removeBeanFilters(Class<?>...values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setPojoSwaps(Class<?>...values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addPojoSwaps(Class<?>...values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removePojoSwaps(Class<?>...values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		super.setImplClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeanDictionary(Class<?>...values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addToBeanDictionary(Class<?>...values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removeFromBeanDictionary(Class<?>...values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setBeanTypePropertyName(String value) throws LockedException {
		super.setBeanTypePropertyName(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setDefaultParser(Class<?> value) throws LockedException {
		super.setDefaultParser(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setLocale(Locale value) throws LockedException {
		super.setLocale(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setTimeZone(TimeZone value) throws LockedException {
		super.setTimeZone(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setMediaType(MediaType value) throws LockedException {
		super.setMediaType(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setDebug(boolean value) throws LockedException {
		super.setDebug(value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setProperty(String name, Object value) throws LockedException {
		super.setProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addToProperty(String name, Object value) throws LockedException {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer putToProperty(String name, Object key, Object value) throws LockedException {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer putToProperty(String name, Object value) throws LockedException {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer removeFromProperty(String name, Object value) throws LockedException {
		super.removeFromProperty(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public UrlEncodingSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public UrlEncodingSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public UrlEncodingSerializer clone() {
		UrlEncodingSerializer c = (UrlEncodingSerializer)super.clone();
		return c;
	}
}
