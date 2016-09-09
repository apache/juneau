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

import static org.apache.juneau.urlencoding.UonSerializerContext.*;
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
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>application/x-www-form-urlencoded</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>application/x-www-form-urlencoded</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This serializer provides several serialization options.  Typically, one of the predefined DEFAULT serializers will be sufficient.
 * 	However, custom serializers can be constructed to fine-tune behavior.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link UonSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 * <p>
 * 	The following shows a sample object defined in Javascript:
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
 * 	Using the "strict" syntax defined in this document, the equivalent
 * 		URL-encoded notation would be as follows:
 * </p>
 * <p class='bcode'>
 * 	<xa>id</xa>=$n(<xs>1</xs>)
 * 	&amp;<xa>name</xa>=<xs>John+Smith</xs>,
 * 	&amp;<xa>uri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 	&amp;<xa>addressBookUri</xa>=<xs>http://sample/addressBook</xs>,
 * 	&amp;<xa>birthDate</xa>=<xs>1946-08-12T00:00:00Z</xs>,
 * 	&amp;<xa>otherIds</xa>=<xs>%00</xs>,
 * 	&amp;<xa>addresses</xa>=$a(
 * 		$o(
 * 			<xa>uri</xa>=<xs>http://sample/addressBook/address/1</xs>,
 * 			<xa>personUri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 			<xa>id</xa>=$n(<xs>1</xs>),
 * 			<xa>street</xa>=<xs>100+Main+Street</xs>,
 * 			<xa>city</xa>=<xs>Anywhereville</xs>,
 * 			<xa>state</xa>=<xs>NY</xs>,
 * 			<xa>zip</xa>=$n(<xs>12345</xs>),
 * 			<xa>isCurrent</xa>=$b(<xs>true</xs>)
 * 		)
 * 	)
 * </p>
 * <p>
 * 	A secondary "lax" syntax is available when the data type of the
 * 		values are already known on the receiving end of the transmission:
 * </p>
 * <p class='bcode'>
 * 	<xa>id</xa>=<xs>1</xs>,
 * 	&amp;<xa>name</xa>=<xs>John+Smith</xs>,
 * 	&amp;<xa>uri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 	&amp;<xa>addressBookUri</xa>=<xs>http://sample/addressBook</xs>,
 * 	&amp;<xa>birthDate</xa>=<xs>1946-08-12T00:00:00Z</xs>,
 * 	&amp;<xa>otherIds</xa>=<xs>%00</xs>,
 * 	&amp;<xa>addresses</xa>=(
 * 		(
 * 			<xa>uri</xa>=<xs>http://sample/addressBook/address/1</xs>,
 * 			<xa>personUri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 			<xa>id</xa>=<xs>1</xs>,
 * 			<xa>street</xa>=<xs>100+Main+Street</xs>,
 * 			<xa>city</xa>=<xs>Anywhereville</xs>,
 * 			<xa>state</xa>=<xs>NY</xs>,
 * 			<xa>zip</xa>=<xs>12345</xs>,
 * 			<xa>isCurrent</xa>=<xs>true</xs>
 * 		)
 * 	)
 * </p>
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Serialize a Map</jc>
 * 	Map m = <jk>new</jk> ObjectMap(<js>"{a:'b',c:1,d:false,e:['f',1,false],g:{h:'i'}}"</js>);
 *
 * 	<jc>// Serialize to value equivalent to JSON.</jc>
 * 	<jc>// Produces "a=b&amp;c=$n(1)&amp;d=$b(false)&amp;e=$a(f,$n(1),$b(false))&amp;g=$o(h=i)"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 *
 * 	<jc>// Serialize to simplified value (for when data type is already known by receiver).</jc>
 * 	<jc>// Produces "a=b&amp;c=1&amp;d=false&amp;e=(f,1,false)&amp;g=(h=i))"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT_SIMPLE</jsf>.serialize(s);
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
 * 	<jc>// Produces "name=John+Doe&amp;age=23&amp;address=$o(street=123+Main+St,city=Anywhere,state=NY,zip=$n(12345))&amp;deceased=$b(false)"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 *
 * 	<jc>// Produces "name=John+Doe&amp;age=23&amp;address=(street=123+Main+St,city=Anywhere,state=NY,zip=12345)&amp;deceased=false)"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT_SIMPLE</jsf>.serialize(s);
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Produces("application/x-www-form-urlencoded")
@SuppressWarnings("hiding")
public class UrlEncodingSerializer extends UonSerializer {

	/** Reusable instance of {@link UrlEncodingSerializer}, all default settings. */
	public static final UrlEncodingSerializer DEFAULT = new UrlEncodingSerializer().lock();

	/** Reusable instance of {@link UrlEncodingSerializer.Simple}. */
	public static final UrlEncodingSerializer DEFAULT_SIMPLE = new Simple().lock();

	/** Reusable instance of {@link UrlEncodingSerializer.SimpleExpanded}. */
	public static final UrlEncodingSerializer DEFAULT_SIMPLE_EXPANDED = new SimpleExpanded().lock();

	/** Reusable instance of {@link UrlEncodingSerializer.Readable}. */
	public static final UrlEncodingSerializer DEFAULT_READABLE = new Readable().lock();

	/**
	 * Constructor.
	 */
	public UrlEncodingSerializer() {
		setProperty(UON_encodeChars, true);
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializer().setProperty(UonSerializerContext.<jsf>UON_simpleMode</jsf>,<jk>true</jk>);</code>.
	 */
	@Produces(value={"application/x-www-form-urlencoded-simple"},contentType="application/x-www-form-urlencoded")
	public static class Simple extends UrlEncodingSerializer {
		/** Constructor */
		public Simple() {
			setProperty(UON_simpleMode, true);
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializer().setProperty(UonSerializerContext.<jsf>UON_simpleMode</jsf>,<jk>true</jk>).setProperty(UonSerializerContext.<jsf>URLENC_expandedParams</jsf>,<jk>true</jk>);</code>.
	 */
	@Produces(value={"application/x-www-form-urlencoded-simple"},contentType="application/x-www-form-urlencoded")
	public static class SimpleExpanded extends Simple {
		/** Constructor */
		public SimpleExpanded() {
			setProperty(URLENC_expandedParams, true);
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializer().setProperty(UonSerializerContext.<jsf>UON_useWhitespace</jsf>,<jk>true</jk>);</code>.
	 */
	public static class Readable extends UrlEncodingSerializer {
		/** Constructor */
		public Readable() {
			setProperty(UON_useWhitespace, true);
		}
	}

	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeAnything(UrlEncodingSerializerSession session, UonWriter out, Object o) throws Exception {
		BeanContext bc = session.getBeanContext();

		boolean addTypeProperty;		// Add "_type" attribute to element?
		ClassMeta<?> aType;			// The actual type
		ClassMeta<?> sType;			// The serialized type

		aType = session.push("root", o, object());
		session.indent--;
		if (aType == null)
			aType = object();

		sType = aType.getSerializedClassMeta();
		addTypeProperty = (session.isAddBeanTypeProperties());

		// Swap if necessary
		PojoSwap swap = aType.getPojoSwap();
		if (swap != null) {
			o = swap.swap(o, bc);

			// If the getSwapClass() method returns Object, we need to figure out
			// the actual type now.
			if (sType.isObject())
				sType = bc.getClassMetaForObject(o);
		}

		if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(session, out, (BeanMap)o, addTypeProperty);
			else
				serializeMap(session, out, (Map)o, sType);
		} else if (sType.hasToObjectMapMethod()) {
			serializeMap(session, out, sType.toObjectMap(o), sType);
		} else if (sType.isBean()) {
			serializeBeanMap(session, out, bc.forBean(o), addTypeProperty);
		} else if (sType.isCollection()) {
			serializeMap(session, out, getCollectionMap((Collection)o), bc.getMapClassMeta(Map.class, Integer.class, sType.getElementType()));
		} else {
			// All other types can't be serialized as key/value pairs, so we create a
			// mock key/value pair with a "_value" key.
			out.append("_value=");
			super.serializeAnything(session, out, o, null, null, null, false, true);
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
					out.appendObject(key, false, true, true).append('=');
					super.serializeAnything(session, out, i.next(), null, (key == null ? null : key.toString()), null, false, true);
					addAmp = true;
				}
			} else {
				if (addAmp)
					out.cr(depth).append('&');
				out.appendObject(key, false, true, true).append('=');
				super.serializeAnything(session, out, value, valueType, (key == null ? null : key.toString()), null, false, true);
				addAmp = true;
			}
		}

		return out;
	}

	@SuppressWarnings({ "rawtypes" })
	private SerializerWriter serializeBeanMap(UrlEncodingSerializerSession session, UonWriter out, BeanMap<?> m, boolean addClassAttr) throws Exception {
		int depth = session.getIndent();

		boolean addAmp = false;

		for (BeanPropertyValue p : m.getValues(session.isTrimNulls(), addClassAttr ? session.createBeanTypeNameProperty(m, null) : null)) {
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

					out.appendObject(key, false, true, true).append('=');

					super.serializeAnything(session, out, i.next(), cMeta.getElementType(), key, pMeta, false, true);

					addAmp = true;
				}
			} else {
				if (addAmp)
					out.cr(depth).append('&');

				out.appendObject(key, false, true, true).append('=');

				super.serializeAnything(session, out, value, cMeta, key, pMeta, false, true);

				addAmp = true;
			}

		}
		return out;
	}

	//--------------------------------------------------------------------------------
	// Methods for constructing individual parameter values.
	//--------------------------------------------------------------------------------

	/**
	 * Converts the specified object to a string using this serializers {@link BeanContext#convertToType(Object, Class)} method
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
			UonSerializerSession s = createSession(w, null, null);
			super.doSerialize(s, o);
			return w.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public UrlEncodingSerializerSession createSession(Object output, ObjectMap properties, Method javaMethod) {
		return new UrlEncodingSerializerSession(getContext(UrlEncodingSerializerContext.class), getBeanContext(), output, properties, javaMethod);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		UrlEncodingSerializerSession s = (UrlEncodingSerializerSession)session;
		serializeAnything(s, s.getWriter(), o);
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addBeanFilters(Class<?>...classes) throws LockedException {
		super.addBeanFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addPojoSwaps(Class<?>...classes) throws LockedException {
		super.addPojoSwaps(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> UrlEncodingSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

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
