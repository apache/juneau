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

import static org.apache.juneau.serializer.SerializerContext.*;
import static org.apache.juneau.uon.UonSerializerContext.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.uon.*;

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
public class UrlEncodingSerializer extends UonSerializer implements PartSerializer {

	/** Reusable instance of {@link UrlEncodingSerializer}, all default settings. */
	public static final UrlEncodingSerializer DEFAULT = new UrlEncodingSerializer(PropertyStore.create());

	/** Reusable instance of {@link UrlEncodingSerializer.Expanded}. */
	public static final UrlEncodingSerializer DEFAULT_EXPANDED = new Expanded(PropertyStore.create());

	/** Reusable instance of {@link UrlEncodingSerializer.Readable}. */
	public static final UrlEncodingSerializer DEFAULT_READABLE = new Readable(PropertyStore.create());

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializerBuilder().expandedParams(<jk>true</jk>).build();</code>.
	 */
	@Produces(value="application/x-www-form-urlencoded",contentType="application/x-www-form-urlencoded")
	public static class Expanded extends UrlEncodingSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Expanded(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(UrlEncodingContext.URLENC_expandedParams, true);
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializerBuilder().useWhitespace(<jk>true</jk>).build();</code>.
	 */
	public static class Readable extends UrlEncodingSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Readable(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(SERIALIZER_useWhitespace, true);
		}
	}


	private final UrlEncodingSerializerContext ctx;

	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public UrlEncodingSerializer(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(UrlEncodingSerializerContext.class);
	}

	@Override /* CoreObject */
	public UrlEncodingSerializerBuilder builder() {
		return new UrlEncodingSerializerBuilder(propertyStore);
	}

	@Override /* CoreObject */
	protected ObjectMap getOverrideProperties() {
		return super.getOverrideProperties().append(UON_encodeChars, true);
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
		} else if (sType.isCollection() || sType.isArray()) {
			Map m = sType.isCollection() ? getCollectionMap((Collection)o) : getCollectionMap(o);
			serializeCollectionMap(session, out, m, session.getClassMeta(Map.class, Integer.class, Object.class));
		} else {
			// All other types can't be serialized as key/value pairs, so we create a
			// mock key/value pair with a "_value" key.
			out.append("_value=");
			super.serializeAnything(session, out, o, null, null, null, session.plainTextParams());
		}

		session.pop();
		return out;
	}

	/**
	 * Converts a Collection into an integer-indexed map.
	 */
	private static Map<Integer,Object> getCollectionMap(Collection<?> c) {
		Map<Integer,Object> m = new TreeMap<Integer,Object>();
		int i = 0;
		for (Object o : c)
			m.put(i++, o);
		return m;
	}

	/**
	 * Converts an array into an integer-indexed map.
	 */
	private static Map<Integer,Object> getCollectionMap(Object array) {
		Map<Integer,Object> m = new TreeMap<Integer,Object>();
		for (int i = 0; i < Array.getLength(array); i++)
			m.put(i, Array.get(array, i));
		return m;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(UrlEncodingSerializerSession session, UonWriter out, Map m, ClassMeta<?> type) throws Exception {

		boolean plainTextParams = session.plainTextParams();
		m = session.sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		int depth = session.getIndent();
		boolean addAmp = false;

		for (Map.Entry e : (Set<Map.Entry>)m.entrySet()) {
			Object key = session.generalize(e.getKey(), keyType);
			Object value = e.getValue();

			if (session.shouldUseExpandedParams(value)) {
				Iterator i = value instanceof Collection ? ((Collection)value).iterator() : ArrayUtils.iterator(value);
				while (i.hasNext()) {
					if (addAmp)
						out.cr(depth).append('&');
					out.appendObject(key, true, plainTextParams).append('=');
					super.serializeAnything(session, out, i.next(), null, (key == null ? null : key.toString()), null, plainTextParams);
					addAmp = true;
				}
			} else {
				if (addAmp)
					out.cr(depth).append('&');
				out.appendObject(key, true, plainTextParams).append('=');
				super.serializeAnything(session, out, value, valueType, (key == null ? null : key.toString()), null, plainTextParams);
				addAmp = true;
			}
		}

		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeCollectionMap(UrlEncodingSerializerSession session, UonWriter out, Map m, ClassMeta<?> type) throws Exception {

		ClassMeta<?> valueType = type.getValueType();

		int depth = session.getIndent();
		boolean addAmp = false;

		for (Map.Entry e : (Set<Map.Entry>)m.entrySet()) {
			if (addAmp)
				out.cr(depth).append('&');
			out.append(e.getKey()).append('=');
			super.serializeAnything(session, out, e.getValue(), valueType, null, null, session.plainTextParams());
			addAmp = true;
		}

		return out;
	}

	@SuppressWarnings({ "rawtypes" })
	private SerializerWriter serializeBeanMap(UrlEncodingSerializerSession session, UonWriter out, BeanMap<?> m, String typeName) throws Exception {
		int depth = session.getIndent();
		boolean plainTextParams = session.plainTextParams();

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

					out.appendObject(key, true, plainTextParams).append('=');

					super.serializeAnything(session, out, i.next(), cMeta.getElementType(), key, pMeta, plainTextParams);

					addAmp = true;
				}
			} else {
				if (addAmp)
					out.cr(depth).append('&');

				out.appendObject(key, true, plainTextParams).append('=');

				super.serializeAnything(session, out, value, cMeta, key, pMeta, plainTextParams);

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
	 * @param urlEncode URL-encode the string if necessary.
	 * If <jk>null</jk>, then uses the value of the {@link UonSerializerContext#UON_encodeChars} setting.
	 * @param plainTextParams Whether we're using plain-text params.
	 * If <jk>null</jk>, then uses the value from the {@link UrlEncodingSerializerContext#URLENC_paramFormat} setting.
	 * @return The serialized object.
	 */
	private String serializePart(Object o, Boolean urlEncode, Boolean plainTextParams) {
		try {
			// Shortcut for simple types.
			ClassMeta<?> cm = getBeanContext().getClassMetaForObject(o);
			if (cm != null) {
				if (cm.isNumber() || cm.isBoolean())
					return o.toString();
				if (cm.isCharSequence()) {
					String s = o.toString();
					boolean ptt = (plainTextParams != null ? plainTextParams : ctx.plainTextParams);
					if (ptt || ! UonUtils.needsQuotes(s))
						return (urlEncode ? StringUtils.urlEncode(s) : s);
				}
			}

			StringWriter w = new StringWriter();
			UonSerializerSession s = new UrlEncodingSerializerSession(ctx, urlEncode, null, w, null, null, null, MediaType.UON, null);
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
	public UrlEncodingSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		return new UrlEncodingSerializerSession(ctx, null, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		UrlEncodingSerializerSession s = (UrlEncodingSerializerSession)session;
		serializeAnything(s, s.getWriter(), o);
	}

	@Override /* PartSerializer */
	public String serialize(PartType type, Object value) {
		switch(type) {
			case HEADER: return serializePart(value, false, true);
			case FORM_DATA: return serializePart(value, false, null);
			case PATH: return serializePart(value, false, null);
			case QUERY: return serializePart(value, false, null);
			default: return StringUtils.toString(value);
		}
	}
}
