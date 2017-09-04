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

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.uon.*;

/**
 * Session object that lives for the duration of a single use of {@link UrlEncodingSerializer}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class UrlEncodingSerializerSession extends UonSerializerSession {

	private final boolean expandedParams;

	/**
	 * Constructor.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param encode Override the {@link UonSerializerContext#UON_encodeChars} setting.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected UrlEncodingSerializerSession(UrlEncodingSerializerContext ctx, Boolean encode, SerializerSessionArgs args) {
		super(ctx, encode, args);
		ObjectMap p = getProperties();
		if (p.isEmpty()) {
			expandedParams = ctx.expandedParams;
		} else {
			expandedParams = p.getBoolean(UrlEncodingContext.URLENC_expandedParams, false);
		}
	}

	/*
	 * Returns <jk>true</jk> if the specified bean property should be expanded as multiple key-value pairs.
	 */
	private boolean shouldUseExpandedParams(BeanPropertyMeta pMeta) {
		ClassMeta<?> cm = pMeta.getClassMeta().getSerializedClassMeta(this);
		if (cm.isCollectionOrArray()) {
			if (expandedParams)
				return true;
			if (pMeta.getBeanMeta().getClassMeta().getExtendedMeta(UrlEncodingClassMeta.class).isExpandedParams())
				return true;
		}
		return false;
	}

	/*
	 * Returns <jk>true</jk> if the specified value should be represented as an expanded parameter list.
	 */
	private boolean shouldUseExpandedParams(Object value) {
		if (value == null || ! expandedParams)
			return false;
		ClassMeta<?> cm = getClassMetaForObject(value).getSerializedClassMeta(this);
		if (cm.isCollectionOrArray()) {
			if (expandedParams)
				return true;
		}
		return false;
	}

	@Override /* SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {
		serializeAnything(getUonWriter(out), o);
	}

	/*
	 * Workhorse method. Determines the type of object, and then calls the appropriate type-specific serialization method.
	 */
	private SerializerWriter serializeAnything(UonWriter out, Object o) throws Exception {

		ClassMeta<?> aType;			// The actual type
		ClassMeta<?> sType;			// The serialized type

		aType = push("root", o, object());
		indent--;
		if (aType == null)
			aType = object();

		sType = aType;
		String typeName = getBeanTypeName(object(), aType, null);

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

		if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(out, (BeanMap)o, typeName);
			else
				serializeMap(out, (Map)o, sType);
		} else if (sType.isBean()) {
			serializeBeanMap(out, toBeanMap(o), typeName);
		} else if (sType.isCollection() || sType.isArray()) {
			Map m = sType.isCollection() ? getCollectionMap((Collection)o) : getCollectionMap(o);
			serializeCollectionMap(out, m, getClassMeta(Map.class, Integer.class, Object.class));
		} else if (sType.isReader() || sType.isInputStream()) {
			IOUtils.pipe(o, out);
		} else {
			// All other types can't be serialized as key/value pairs, so we create a
			// mock key/value pair with a "_value" key.
			out.append("_value=");
			super.serializeAnything(out, o, null, null, null);
		}

		pop();
		return out;
	}

	/*
	 * Converts a Collection into an integer-indexed map.
	 */
	private static Map<Integer,Object> getCollectionMap(Collection<?> c) {
		Map<Integer,Object> m = new TreeMap<Integer,Object>();
		int i = 0;
		for (Object o : c)
			m.put(i++, o);
		return m;
	}

	/*
	 * Converts an array into an integer-indexed map.
	 */
	private static Map<Integer,Object> getCollectionMap(Object array) {
		Map<Integer,Object> m = new TreeMap<Integer,Object>();
		for (int i = 0; i < Array.getLength(array); i++)
			m.put(i, Array.get(array, i));
		return m;
	}

	private SerializerWriter serializeMap(UonWriter out, Map m, ClassMeta<?> type) throws Exception {

		m = sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		boolean addAmp = false;

		for (Map.Entry e : (Set<Map.Entry>)m.entrySet()) {
			Object key = generalize(e.getKey(), keyType);
			Object value = e.getValue();

			if (shouldUseExpandedParams(value)) {
				Iterator i = value instanceof Collection ? ((Collection)value).iterator() : iterator(value);
				while (i.hasNext()) {
					if (addAmp)
						out.cr(indent).append('&');
					out.appendObject(key, true).append('=');
					super.serializeAnything(out, i.next(), null, (key == null ? null : key.toString()), null);
					addAmp = true;
				}
			} else {
				if (addAmp)
					out.cr(indent).append('&');
				out.appendObject(key, true).append('=');
				super.serializeAnything(out, value, valueType, (key == null ? null : key.toString()), null);
				addAmp = true;
			}
		}

		return out;
	}

	private SerializerWriter serializeCollectionMap(UonWriter out, Map m, ClassMeta<?> type) throws Exception {

		ClassMeta<?> valueType = type.getValueType();

		boolean addAmp = false;

		for (Map.Entry e : (Set<Map.Entry>)m.entrySet()) {
			if (addAmp)
				out.cr(indent).append('&');
			out.append(e.getKey()).append('=');
			super.serializeAnything(out, e.getValue(), valueType, null, null);
			addAmp = true;
		}

		return out;
	}

	private SerializerWriter serializeBeanMap(UonWriter out, BeanMap<?> m, String typeName) throws Exception {
		boolean addAmp = false;

		for (BeanPropertyValue p : m.getValues(isTrimNulls(), typeName != null ? createBeanTypeNameProperty(m, typeName) : null)) {
			BeanPropertyMeta pMeta = p.getMeta();
			ClassMeta<?> cMeta = p.getClassMeta();
			ClassMeta<?> sMeta = cMeta.getSerializedClassMeta(this);

			String key = p.getName();
			Object value = p.getValue();
			Throwable t = p.getThrown();
			if (t != null)
				onBeanGetterException(pMeta, t);

			if (canIgnoreValue(sMeta, key, value))
				continue;

			if (value != null && shouldUseExpandedParams(pMeta)) {
				// Transformed object array bean properties may be transformed resulting in ArrayLists,
				// so we need to check type if we think it's an array.
				Iterator i = (sMeta.isCollection() || value instanceof Collection) ? ((Collection)value).iterator() : iterator(value);
				while (i.hasNext()) {
					if (addAmp)
						out.cr(indent).append('&');

					out.appendObject(key, true).append('=');

					super.serializeAnything(out, i.next(), cMeta.getElementType(), key, pMeta);

					addAmp = true;
				}
			} else {
				if (addAmp)
					out.cr(indent).append('&');

				out.appendObject(key, true).append('=');

				super.serializeAnything(out, value, cMeta, key, pMeta);

				addAmp = true;
			}

		}
		return out;
	}
}
