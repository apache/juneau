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
package org.apache.juneau.msgpack;

import static org.apache.juneau.msgpack.MsgPackSerializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Session object that lives for the duration of a single use of {@link MsgPackSerializer}.
 * 
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public final class MsgPackSerializerSession extends OutputStreamSerializerSession {

	private final boolean
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
	protected MsgPackSerializerSession(MsgPackSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		addBeanTypeProperties = getProperty(MSGPACK_addBeanTypeProperties, boolean.class, ctx.addBeanTypeProperties);
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("MsgPackSerializerSession", new ObjectMap()
				.append("addBeanTypeProperties", addBeanTypeProperties)
			);
	}

	/**
	 * Returns the {@link MsgPackSerializer#MSGPACK_addBeanTypeProperties} setting value for this session.
	 * 
	 * @return The {@link MsgPackSerializer#MSGPACK_addBeanTypeProperties} setting value for this session.
	 */
	@Override /* SerializerSession */
	protected final boolean isAddBeanTypeProperties() {
		return addBeanTypeProperties;
	}

	@Override /* SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {
		serializeAnything(getMsgPackOutputStream(out), o, getExpectedRootType(o), "root", null);
	}

	/*
	 * Converts the specified output target object to an {@link MsgPackOutputStream}.
	 */
	private static final MsgPackOutputStream getMsgPackOutputStream(SerializerPipe out) throws Exception {
		Object output = out.getRawOutput();
		if (output instanceof MsgPackOutputStream)
			return (MsgPackOutputStream)output;
		MsgPackOutputStream os = new MsgPackOutputStream(out.getOutputStream());
		out.setOutputStream(os);
		return os;
	}

	/*
	 * Workhorse method.
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private MsgPackOutputStream serializeAnything(MsgPackOutputStream out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws Exception {

		if (o == null)
			return out.appendNull();

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

		// '\0' characters are considered null.
		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0))
			out.appendNull();
		else if (sType.isBoolean())
			out.appendBoolean((Boolean)o);
		else if (sType.isNumber())
			out.appendNumber((Number)o);
		else if (sType.isBean())
			serializeBeanMap(out, toBeanMap(o), typeName);
		else if (sType.isUri() || (pMeta != null && pMeta.isUri()))
			out.appendString(resolveUri(o.toString()));
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
			out.appendString(toString(o));

		if (! isRecursion)
			pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeMap(MsgPackOutputStream out, Map m, ClassMeta<?> type) throws Exception {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		m = sort(m);

		// The map size may change as we're iterating over it, so
		// grab a snapshot of the entries in a separate list.
		List<SimpleMapEntry> entries = new ArrayList<>(m.size());
		for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
			entries.add(new SimpleMapEntry(e.getKey(), e.getValue()));

		out.startMap(entries.size());

		for (SimpleMapEntry e : entries) {
			Object value = e.value;
			Object key = generalize(e.key, keyType);

			serializeAnything(out, key, keyType, null, null);
			serializeAnything(out, value, valueType, null, null);
		}
	}

	private void serializeBeanMap(MsgPackOutputStream out, final BeanMap<?> m, String typeName) throws Exception {

		List<BeanPropertyValue> values = m.getValues(isTrimNulls(), typeName != null ? createBeanTypeNameProperty(m, typeName) : null);

		int size = values.size();
		for (BeanPropertyValue p : values)
			if (p.getThrown() != null)
				size--;
		out.startMap(size);

		for (BeanPropertyValue p : values) {
			BeanPropertyMeta pMeta = p.getMeta();
			ClassMeta<?> cMeta = p.getClassMeta();
			String key = p.getName();
			Object value = p.getValue();
			Throwable t = p.getThrown();
			if (t != null)
				onBeanGetterException(pMeta, t);
			else {
				serializeAnything(out, key, null, null, null);
				serializeAnything(out, value, cMeta, key, pMeta);
			}
		}
	}

	private static final class SimpleMapEntry {
		final Object key;
		final Object value;

		SimpleMapEntry(Object key, Object value) {
			this.key = key;
			this.value = value;
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void serializeCollection(MsgPackOutputStream out, Collection c, ClassMeta<?> type) throws Exception {

		ClassMeta<?> elementType = type.getElementType();
		List<Object> l = new ArrayList<>(c.size());

		c = sort(c);
		l.addAll(c);

		out.startArray(l.size());

		for (Object o : l)
			serializeAnything(out, o, elementType, "<iterator>", null);
	}
}
