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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
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

	private final JsonSerializer ctx;

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
	protected JsonSerializerSession(JsonSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		this.ctx = ctx;
	}

	@Override /* SerializerSesssion */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		serializeAnything(getJsonWriter(out).i(getInitialDepth()), o, getExpectedRootType(o), "root", null);
	}

	/**
	 * Method that can be called from subclasses to serialize an object to JSON.
	 *
	 * <p>
	 * Used by {@link JsonSchemaSerializerSession} for serializing examples to JSON.
	 *
	 * @param o The object to serialize.
	 * @return The serialized object.
	 * @throws Exception Error occurred.
	 */
	protected String serializeJson(Object o) throws Exception {
		StringWriter sw = new StringWriter();
		serializeAnything(getJsonWriter(createPipe(sw)).i(getInitialDepth()), o, getExpectedRootType(o), "root", null);
		return sw.toString();
	}

	/**
	 * Workhorse method.
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 *
	 * @param out The output writer.
	 * @param o The object to serialize.
	 * @param eType The expected type.
	 * @param attrName The attribute name.
	 * @param pMeta The bean property currently being parsed.
	 * @return The same writer passed in.
	 * @throws IOException Thrown by underlying stream.
	 * @throws SerializeException General serialization error occurred.
	 */
	@SuppressWarnings({ "rawtypes" })
	protected JsonWriter serializeAnything(JsonWriter out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws IOException, SerializeException {

		if (o == null) {
			out.append("null");
			return out;
		}

		if (eType == null)
			eType = object();

		ClassMeta<?> aType;			// The actual type
		ClassMeta<?> sType;			// The serialized type

		aType = push2(attrName, o, eType);
		boolean isRecursion = aType == null;

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		// Handle Optional<X>
		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
		}

		sType = aType;
		String typeName = getBeanTypeName(this, eType, aType, pMeta);

		// Swap if necessary
		PojoSwap swap = aType.getSwap(this);
		if (swap != null) {
			o = swap(swap, o);
			sType = swap.getSwapClassMeta(this);

			// If the getSwapClass() method returns Object, we need to figure out
			// the actual type now.
			if (sType.isObject())
				sType = getClassMetaForObject(o);
		}

		String wrapperAttr = getJsonClassMeta(sType).getWrapperAttr();
		if (wrapperAttr != null) {
			out.append('{').cr(indent).attr(wrapperAttr).append(':').s(indent);
			indent++;
		}

		// '\0' characters are considered null.
		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0)) {
			out.append("null");
		} else if (sType.isNumber() || sType.isBoolean()) {
			out.append(o);
		} else if (sType.isBean()) {
			serializeBeanMap(out, toBeanMap(o), typeName);
		} else if (sType.isUri() || (pMeta != null && pMeta.isUri())) {
			out.uriValue(o);
		} else if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(out, (BeanMap)o, typeName);
			else
				serializeMap(out, (Map)o, eType);
		} else if (sType.isCollection()) {
			serializeCollection(out, (Collection) o, eType);
		} else if (sType.isArray()) {
			serializeCollection(out, toList(sType.getInnerClass(), o), eType);
		} else if (sType.isReader() || sType.isInputStream()) {
			IOUtils.pipe(o, out);
		} else {
			out.stringValue(toString(o));
		}

		if (wrapperAttr != null) {
			indent--;
			out.cre(indent-1).append('}');
		}

		if (! isRecursion)
			pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(JsonWriter out, Map m, ClassMeta<?> type) throws IOException, SerializeException {

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

	private SerializerWriter serializeBeanMap(JsonWriter out, BeanMap<?> m, String typeName) throws IOException, SerializeException {
		int i = indent;
		out.append('{');

		boolean addComma = false;
		for (BeanPropertyValue p : m.getValues(isKeepNullProperties(), typeName != null ? createBeanTypeNameProperty(m, typeName) : null)) {
			BeanPropertyMeta pMeta = p.getMeta();
			if (pMeta.canRead()) {
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
		}
		out.cre(i-1).append('}');
		return out;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private SerializerWriter serializeCollection(JsonWriter out, Collection c, ClassMeta<?> type) throws IOException, SerializeException {

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
	 * Converts the specified output target object to an {@link JsonWriter}.
	 *
	 * @param out The output target object.
	 * @return The output target object wrapped in an {@link JsonWriter}.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected final JsonWriter getJsonWriter(SerializerPipe out) throws IOException {
		Object output = out.getRawOutput();
		if (output instanceof JsonWriter)
			return (JsonWriter)output;
		JsonWriter w = new JsonWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isEscapeSolidus(), getQuoteChar(),
			isSimpleMode(), isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * @see JsonSerializer#JSON_addBeanTypes
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}

	/**
	 * Configuration property:  Prefix solidus <js>'/'</js> characters with escapes.
	 *
	 * @see JsonSerializer#JSON_escapeSolidus
	 * @return
	 * 	<jk>true</jk> if solidus (e.g. slash) characters should be escaped.
	 */
	protected final boolean isEscapeSolidus() {
		return ctx.isEscapeSolidus();
	}

	/**
	 * Configuration property:  Simple JSON mode.
	 *
	 * @see JsonSerializer#JSON_simpleMode
	 * @return
	 * 	<jk>true</jk> if JSON attribute names will only be quoted when necessary.
	 * 	<br>Otherwise, they are always quoted.
	 */
	protected final boolean isSimpleMode() {
		return ctx.isSimpleMode();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	protected JsonClassMeta getJsonClassMeta(ClassMeta<?> cm) {
		return ctx.getJsonClassMeta(cm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Session */
	public OMap toMap() {
		return super.toMap()
			.a("JsonSerializerSession", new DefaultFilteringOMap()
		);
	}
}
