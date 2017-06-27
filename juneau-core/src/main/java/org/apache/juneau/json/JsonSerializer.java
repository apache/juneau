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
package org.apache.juneau.json;

import static org.apache.juneau.json.JsonSerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJO models to JSON.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Accept</code> types: <code>application/json, text/json</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>application/json</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * The conversion is as follows...
 * <ul class='spaced-list'>
 * 	<li>
 * 		Maps (e.g. {@link HashMap HashMaps}, {@link TreeMap TreeMaps}) are converted to JSON objects.
 * 	<li>
 * 		Collections (e.g. {@link HashSet HashSets}, {@link LinkedList LinkedLists}) and Java arrays are converted to
 * 		JSON arrays.
 * 	<li>
 * 		{@link String Strings} are converted to JSON strings.
 * 	<li>
 * 		{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to JSON numbers.
 * 	<li>
 * 		{@link Boolean Booleans} are converted to JSON booleans.
 * 	<li>
 * 		{@code nulls} are converted to JSON nulls.
 * 	<li>
 * 		{@code arrays} are converted to JSON arrays.
 * 	<li>
 * 		{@code beans} are converted to JSON objects.
 * </ul>
 * <p>
 * The types above are considered "JSON-primitive" object types.  Any non-JSON-primitive object types are transformed
 * into JSON-primitive object types through {@link org.apache.juneau.transform.PojoSwap PojoSwaps} associated
 * through the {@link CoreObjectBuilder#pojoSwaps(Class...)} method.
 * Several default transforms are provided for transforming Dates, Enums, Iterators, etc...
 * <p>
 * This serializer provides several serialization options.  Typically, one of the predefined DEFAULT serializers will
 * be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link JsonSerializerContext}
 * 	<li>{@link SerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link Simple} - Default serializer, single quotes, simple mode.
 * 	<li>
 * 		{@link SimpleReadable} - Default serializer, single quotes, simple mode, with whitespace.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(someObject);
 *
 * 	<jc>// Create a custom serializer for lax syntax using single quote characters</jc>
 * 	JsonSerializer serializer = <jk>new</jk> JsonSerializerBuilder().simple().sq().build();
 *
 * 	<jc>// Clone an existing serializer and modify it to use single-quotes</jc>
 * 	JsonSerializer serializer = JsonSerializer.<jsf>DEFAULT</jsf>.builder().sq().build();
 *
 * 	<jc>// Serialize a POJO to JSON</jc>
 * 	String json = serializer.serialize(someObject);
 * </p>
 */
@Produces("application/json,text/json")
public class JsonSerializer extends WriterSerializer {

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT = new JsonSerializer(PropertyStore.create());

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT_READABLE = new Readable(PropertyStore.create());

	/** Default serializer, single quotes, simple mode. */
	public static final JsonSerializer DEFAULT_LAX = new Simple(PropertyStore.create());

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final JsonSerializer DEFAULT_LAX_READABLE = new SimpleReadable(PropertyStore.create());

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static final JsonSerializer DEFAULT_LAX_READABLE_SAFE = new SimpleReadableSafe(PropertyStore.create());


	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSerializer {

		/**
		 * Constructor.
		 *
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

	/** Default serializer, single quotes, simple mode. */
	@Produces(value="application/json+simple,text/json+simple",contentType="application/json")
	public static class Simple extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Simple(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(JSON_simpleMode, true).append(SERIALIZER_quoteChar, '\'');
		}
	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class SimpleReadable extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public SimpleReadable(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(JSON_simpleMode, true).append(SERIALIZER_quoteChar, '\'')
				.append(SERIALIZER_useWhitespace, true);
		}
	}

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static class SimpleReadableSafe extends JsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public SimpleReadableSafe(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(JSON_simpleMode, true).append(SERIALIZER_quoteChar, '\'')
				.append(SERIALIZER_useWhitespace, true).append(SERIALIZER_detectRecursions, true);
		}
	}


	private final JsonSerializerContext ctx;
	private volatile JsonSchemaSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public JsonSerializer(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(JsonSerializerContext.class);
	}

	@Override /* CoreObject */
	public JsonSerializerBuilder builder() {
		return new JsonSerializerBuilder(propertyStore);
	}

	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	SerializerWriter serializeAnything(JsonSerializerSession session, JsonWriter out, Object o, ClassMeta<?> eType,
			String attrName, BeanPropertyMeta pMeta) throws Exception {

		if (o == null) {
			out.append("null");
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

		String wrapperAttr = sType.getExtendedMeta(JsonClassMeta.class).getWrapperAttr();
		if (wrapperAttr != null) {
			out.append('{').cr(session.indent).attr(wrapperAttr).append(':').s(session.indent);
			session.indent++;
		}

		// '\0' characters are considered null.
		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0))
			out.append("null");
		else if (sType.isNumber() || sType.isBoolean())
			out.append(o);
		else if (sType.isBean())
			serializeBeanMap(session, out, session.toBeanMap(o), typeName);
		else if (sType.isUri() || (pMeta != null && pMeta.isUri()))
			out.uriValue(o);
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
		else
			out.stringValue(session.toString(o));

		if (wrapperAttr != null) {
			session.indent--;
			out.cre(session.indent-1).append('}');
		}

		if (! isRecursion)
			session.pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(JsonSerializerSession session, JsonWriter out, Map m, ClassMeta<?> type)
			throws Exception {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		m = session.sort(m);

		int i = session.getIndent();
		out.append('{');

		Iterator mapEntries = m.entrySet().iterator();

		while (mapEntries.hasNext()) {
			Map.Entry e = (Map.Entry) mapEntries.next();
			Object value = e.getValue();

			Object key = session.generalize(e.getKey(), keyType);

			out.cr(i).attr(session.toString(key)).append(':').s(i);

			serializeAnything(session, out, value, valueType, (key == null ? null : session.toString(key)), null);

			if (mapEntries.hasNext())
				out.append(',').smi(i);
		}

		out.cre(i-1).append('}');

		return out;
	}

	private SerializerWriter serializeBeanMap(JsonSerializerSession session, JsonWriter out, BeanMap<?> m,
			String typeName) throws Exception {
		int i = session.getIndent();
		out.append('{');

		boolean addComma = false;
		for (BeanPropertyValue p : m.getValues(session.isTrimNulls(), typeName != null ? session.createBeanTypeNameProperty(m, typeName) : null)) {
			BeanPropertyMeta pMeta = p.getMeta();
			ClassMeta<?> cMeta = p.getClassMeta();
			String key = p.getName();
			Object value = p.getValue();
			Throwable t = p.getThrown();
			if (t != null)
				session.onBeanGetterException(pMeta, t);

			if (session.canIgnoreValue(cMeta, key, value))
				continue;

			if (addComma)
				out.append(',').smi(i);

			out.cr(i).attr(key).append(':').s(i);

			serializeAnything(session, out, value, cMeta, key, pMeta);

			addComma = true;
		}
		out.cre(i-1).append('}');
		return out;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private SerializerWriter serializeCollection(JsonSerializerSession session, JsonWriter out, Collection c,
			ClassMeta<?> type) throws Exception {

		ClassMeta<?> elementType = type.getElementType();

		c = session.sort(c);

		out.append('[');
		int depth = session.getIndent();

		for (Iterator i = c.iterator(); i.hasNext();) {

			Object value = i.next();

			out.cr(depth);

			serializeAnything(session, out, value, elementType, "<iterator>", null);

			if (i.hasNext())
				out.append(',').smi(depth);
		}
		out.cre(depth-1).append(']');
		return out;
	}

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 *
	 * @return The schema serializer.
	 */
	public JsonSchemaSerializer getSchemaSerializer() {
		if (schemaSerializer == null)
			schemaSerializer = new JsonSchemaSerializer(propertyStore, getOverrideProperties());
		return schemaSerializer;
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public JsonSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale,
			TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		return new JsonSerializerSession(ctx, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		JsonSerializerSession s = (JsonSerializerSession)session;
		serializeAnything(s, s.getWriter(), o, s.getExpectedRootType(o), "root", null);
	}
}
