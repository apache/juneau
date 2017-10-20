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

import static org.apache.juneau.json.JsonSerializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonSerializer}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public class JsonSerializerSession extends WriterSerializerSession {

	private final boolean
		simpleMode,
		escapeSolidus,
		addBeanTypeProperties;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected JsonSerializerSession(JsonSerializerContext ctx, SerializerSessionArgs args) {
		super(ctx, args);
		ObjectMap p = getProperties();
		simpleMode = p.getBoolean(JSON_simpleMode, ctx.simpleMode);
		escapeSolidus = p.getBoolean(JSON_escapeSolidus, ctx.escapeSolidus);
		addBeanTypeProperties = p.getBoolean(JSON_addBeanTypeProperties, ctx.addBeanTypeProperties);
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("JsonSerializerSession", new ObjectMap()
				.append("addBeanTypeProperties", addBeanTypeProperties)
				.append("escapeSolidus", escapeSolidus)
				.append("simpleMode", simpleMode)
			);
	}

	@Override /* SerializerSesssion */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {
		serializeAnything(getJsonWriter(out), o, getExpectedRootType(o), "root", null);
	}

	/*
	 * Workhorse method.
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	SerializerWriter serializeAnything(JsonWriter out, Object o, ClassMeta<?> eType,	String attrName, BeanPropertyMeta pMeta) throws Exception {

		if (o == null) {
			out.append("null");
			return out;
		}

		if (eType == null)
			eType = object();

		ClassMeta<?> aType;			// The actual type
		ClassMeta<?> sType;			// The serialized type

		aType = push(attrName, o, eType);
		boolean isRecursion = aType == null;

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		sType = aType;
		String typeName = getBeanTypeName(eType, aType, pMeta);

		// Swap if necessary
		PojoSwap swap = aType.getPojoSwap(this);
		if (swap != null) {
			o = swap.swap(this, o);
			sType = swap.getSwapClassMeta(this);

			// If the getSwapClass() method returns Object, we need to figure out
			// the actual type now.
			if (sType.isObject())
				sType = getClassMetaForObject(o);
		}

		String wrapperAttr = sType.getExtendedMeta(JsonClassMeta.class).getWrapperAttr();
		if (wrapperAttr != null) {
			out.append('{').cr(indent).attr(wrapperAttr).append(':').s(indent);
			indent++;
		}

		// '\0' characters are considered null.
		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0))
			out.append("null");
		else if (sType.isNumber() || sType.isBoolean())
			out.append(o);
		else if (sType.isBean())
			serializeBeanMap(out, toBeanMap(o), typeName);
		else if (sType.isUri() || (pMeta != null && pMeta.isUri()))
			out.uriValue(o);
		else if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(out, (BeanMap)o, typeName);
			else
				serializeMap(out, (Map)o, eType);
		}
		else if (sType.isCollection()) {
			serializeCollection(out, (Collection) o, eType);
		}
		else if (sType.isArray()) {
			serializeCollection(out, toList(sType.getInnerClass(), o), eType);
		}
		else if (sType.isReader() || sType.isInputStream()) {
			IOUtils.pipe(o, out);
		}
		else
			out.stringValue(toString(o));

		if (wrapperAttr != null) {
			indent--;
			out.cre(indent-1).append('}');
		}

		if (! isRecursion)
			pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(JsonWriter out, Map m, ClassMeta<?> type) throws Exception {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		m = sort(m);

		int i = indent;
		out.append('{');

		Iterator mapEntries = m.entrySet().iterator();

		while (mapEntries.hasNext()) {
			Map.Entry e = (Map.Entry) mapEntries.next();
			Object value = e.getValue();

			Object key = generalize(e.getKey(), keyType);

			out.cr(i).attr(toString(key)).append(':').s(i);

			serializeAnything(out, value, valueType, (key == null ? null : toString(key)), null);

			if (mapEntries.hasNext())
				out.append(',').smi(i);
		}

		out.cre(i-1).append('}');

		return out;
	}

	private SerializerWriter serializeBeanMap(JsonWriter out, BeanMap<?> m, String typeName) throws Exception {
		int i = indent;
		out.append('{');

		boolean addComma = false;
		for (BeanPropertyValue p : m.getValues(isTrimNulls(), typeName != null ? createBeanTypeNameProperty(m, typeName) : null)) {
			BeanPropertyMeta pMeta = p.getMeta();
			ClassMeta<?> cMeta = p.getClassMeta();
			String key = p.getName();
			Object value = p.getValue();
			Throwable t = p.getThrown();
			if (t != null)
				onBeanGetterException(pMeta, t);

			if (canIgnoreValue(cMeta, key, value))
				continue;

			if (addComma)
				out.append(',').smi(i);

			out.cr(i).attr(key).append(':').s(i);

			serializeAnything(out, value, cMeta, key, pMeta);

			addComma = true;
		}
		out.cre(i-1).append('}');
		return out;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private SerializerWriter serializeCollection(JsonWriter out, Collection c, ClassMeta<?> type) throws Exception {

		ClassMeta<?> elementType = type.getElementType();

		c = sort(c);

		out.append('[');

		for (Iterator i = c.iterator(); i.hasNext();) {
			Object value = i.next();
			out.cr(indent);
			serializeAnything(out, value, elementType, "<iterator>", null);
			if (i.hasNext())
				out.append(',').smi(indent);
		}
		out.cre(indent-1).append(']');
		return out;
	}


	/**
	 * Returns the {@link JsonSerializer#JSON_addBeanTypeProperties} setting value for this session.
	 *
	 * @return The {@link JsonSerializer#JSON_addBeanTypeProperties} setting value for this session.
	 */
	@Override /* SerializerSession */
	protected final boolean isAddBeanTypeProperties() {
		return addBeanTypeProperties;
	}

	/**
	 * Converts the specified output target object to an {@link JsonWriter}.
	 *
	 * @param out The output target object.
	 * @return The output target object wrapped in an {@link JsonWriter}.
	 * @throws Exception
	 */
	protected final JsonWriter getJsonWriter(SerializerPipe out) throws Exception {
		Object output = out.getRawOutput();
		if (output instanceof JsonWriter)
			return (JsonWriter)output;
		JsonWriter w = new JsonWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), escapeSolidus, getQuoteChar(),
			simpleMode, isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}
}
