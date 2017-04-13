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
package org.apache.juneau.uon;

import static org.apache.juneau.serializer.SerializerContext.*;
import static org.apache.juneau.uon.UonSerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJO models to UON (a notation for URL-encoded query parameter values).
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Accept</code> types: <code>text/uon</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/uon</code>
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
 * 	UON notation would be as follows:
 * </p>
 * <p class='bcode'>
 * 	(
 * 		<ua>id</ua>=<un>1</un>,
 * 		<ua>name</ua>=<us>'John+Smith'</us>,
 * 		<ua>uri</ua>=<us>http://sample/addressBook/person/1</us>,
 * 		<ua>addressBookUri</ua>=<us>http://sample/addressBook</us>,
 * 		<ua>birthDate</ua>=<us>1946-08-12T00:00:00Z</us>,
 * 		<ua>otherIds</ua>=<uk>null</uk>,
 * 		<ua>addresses</ua>=@(
 * 			(
 * 				<ua>uri</ua>=<us>http://sample/addressBook/address/1</us>,
 * 				<ua>personUri</ua>=<us>http://sample/addressBook/person/1</us>,
 * 				<ua>id</ua>=<un>1</un>,
 * 				<ua>street</ua>=<us>'100+Main+Street'</us>,
 * 				<ua>city</ua>=<us>Anywhereville</us>,
 * 				<ua>state</ua>=<us>NY</us>,
 * 				<ua>zip</ua>=<un>12345</un>,
 * 				<ua>isCurrent</ua>=<uk>true</uk>
 * 			)
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
 * 	<jc>// Produces "(a=b,c=1,d=false,e=@(f,1,false),g=(h=i))"</jc>
 * 	String s = UonSerializer.<jsf>DEFAULT</jsf>.serialize(s);
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
 * 	<jc>// Produces "(name='John Doe',age=23,address=(street='123 Main St',city=Anywhere,state=NY,zip=12345),deceased=false)"</jc>
 * 	String s = UonSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 * </p>
 */
@Produces("text/uon")
public class UonSerializer extends WriterSerializer {

	/** Reusable instance of {@link UonSerializer}, all default settings. */
	public static final UonSerializer DEFAULT = new UonSerializer(PropertyStore.create());

	/** Reusable instance of {@link UonSerializer.Readable}. */
	public static final UonSerializer DEFAULT_READABLE = new Readable(PropertyStore.create());

	/** Reusable instance of {@link UonSerializer.Encoding}. */
	public static final UonSerializer DEFAULT_ENCODING = new Encoding(PropertyStore.create());

	/**
	 * Equivalent to <code><jk>new</jk> UonSerializerBuilder().ws().build();</code>.
	 */
	public static class Readable extends UonSerializer {

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

	/**
	 * Equivalent to <code><jk>new</jk> UonSerializerBuilder().encoding().build();</code>.
	 */
	public static class Encoding extends UonSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Encoding(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(UON_encodeChars, true);
		}
	}


	private final UonSerializerContext ctx;

	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public UonSerializer(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(UonSerializerContext.class);
	}

	@Override /* CoreObject */
	public UonSerializerBuilder builder() {
		return new UonSerializerBuilder(propertyStore);
	}

	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 * @param session The context that exist for the duration of a serialize.
	 * @param out The writer to serialize to.
	 * @param o The object being serialized.
	 * @param eType The expected type of the object if this is a bean property.
	 * @param attrName The bean property name if this is a bean property.  <jk>null</jk> if this isn't a bean property being serialized.
	 * @param pMeta The bean property metadata.
	 * @param plainTextParams <jk>true</jk> if this is a top level parameter key or value and paramFormat is PLAINTEXT.
	 *
	 * @return The same writer passed in.
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected SerializerWriter serializeAnything(UonSerializerSession session, UonWriter out, Object o, ClassMeta<?> eType,
			String attrName, BeanPropertyMeta pMeta, boolean plainTextParams) throws Exception {

		if (o == null) {
			out.appendObject(null, false, plainTextParams);
			return out;
		}

		if (eType == null)
			eType = object();

		ClassMeta<?> aType;			// The actual type
		ClassMeta<?> sType;			// The serialized type

		aType = session.push(attrName, o, eType);
		boolean isRecursion = aType == null;

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		sType = aType.getSerializedClassMeta();
		String typeName = session.getBeanTypeName(eType, aType, pMeta);

		// Swap if necessary
		PojoSwap swap = aType.getPojoSwap();
		if (swap != null) {
			o = swap.swap(session, o);

			// If the getSwapClass() method returns Object, we need to figure out
			// the actual type now.
			if (sType.isObject())
				sType = session.getClassMetaForObject(o);
		}

		// '\0' characters are considered null.
		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0))
			out.appendObject(null, false, plainTextParams);
		else if (sType.isBoolean())
			out.appendBoolean(o);
		else if (sType.isNumber())
			out.appendNumber(o);
		else if (sType.isBean())
			serializeBeanMap(session, out, session.toBeanMap(o), typeName);
		else if (sType.isUri() || (pMeta != null && pMeta.isUri()))
			out.appendUri(o);
		else if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(session, out, (BeanMap)o, typeName);
			else
				serializeMap(session, out, (Map)o, eType);
		}
		else if (sType.isCollection()) {
			serializeCollection(session, out, (Collection) o, eType);
		}
		else if (sType.isArray()) {
			serializeCollection(session, out, toList(sType.getInnerClass(), o), eType);
		}
		else {
			out.appendObject(o, false, plainTextParams);
		}

		if (! isRecursion)
			session.pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(UonSerializerSession session, UonWriter out, Map m, ClassMeta<?> type) throws Exception {

		m = session.sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		int depth = session.getIndent();
		out.append('(');

		Iterator mapEntries = m.entrySet().iterator();

		while (mapEntries.hasNext()) {
			Map.Entry e = (Map.Entry) mapEntries.next();
			Object value = e.getValue();
			Object key = session.generalize(e.getKey(), keyType);
			out.cr(depth).appendObject(key, false, false).append('=');
			serializeAnything(session, out, value, valueType, (key == null ? null : session.toString(key)), null, false);
			if (mapEntries.hasNext())
				out.append(',');
		}

		if (m.size() > 0)
			out.cr(depth-1);
		out.append(')');

		return out;
	}

	private SerializerWriter serializeBeanMap(UonSerializerSession session, UonWriter out, BeanMap<?> m, String typeName) throws Exception {
		int depth = session.getIndent();

		out.append('(');

		boolean addComma = false;

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

			if (addComma)
				out.append(',');

			out.cr(depth).appendObject(key, false, false).append('=');

			serializeAnything(session, out, value, cMeta, key, pMeta, false);

			addComma = true;
		}

		if (m.size() > 0)
			out.cr(depth-1);
		out.append(')');

		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeCollection(UonSerializerSession session, UonWriter out, Collection c, ClassMeta<?> type) throws Exception {

		ClassMeta<?> elementType = type.getElementType();

		c = session.sort(c);

		out.append('@').append('(');

		int depth = session.getIndent();

		for (Iterator i = c.iterator(); i.hasNext();) {
			out.cr(depth);
			serializeAnything(session, out, i.next(), elementType, "<iterator>", null, false);
			if (i.hasNext())
				out.append(',');
		}

		if (c.size() > 0)
			out.cr(depth-1);
		out.append(')');

		return out;
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public UonSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new UonSerializerSession(ctx, null, op, output, javaMethod, locale, timeZone, mediaType);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		UonSerializerSession s = (UonSerializerSession)session;
		serializeAnything(s, s.getWriter(), o, s.getExpectedRootType(o), "root", null, false);
	}
}
