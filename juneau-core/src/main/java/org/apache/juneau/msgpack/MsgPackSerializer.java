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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJO models to MessagePack.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>octal/msgpack</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>octal/msgpack</code>
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link MsgPackSerializerContext}
 * 	<li>{@link SerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Produces({"octal/msgpack"})
public class MsgPackSerializer extends OutputStreamSerializer {

	/** Default serializer, all default settings.*/
	public static final MsgPackSerializer DEFAULT = new MsgPackSerializer().lock();

	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	MsgPackOutputStream serializeAnything(MsgPackSerializerSession session, MsgPackOutputStream out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws Exception {
		BeanContext bc = session.getBeanContext();

		if (o == null)
			return out.appendNull();

		if (eType == null)
			eType = object();

		boolean addClassAttr;		// Add "_type" attribute to element?
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
		addClassAttr = (session.isAddClassAttrs() && ! eType.equals(aType));

		// Swap if necessary
		PojoSwap swap = aType.getPojoSwap();
		if (swap != null) {
			o = swap.swap(o, bc);

			// If the getSwapClass() method returns Object, we need to figure out
			// the actual type now.
			if (sType.isObject())
				sType = bc.getClassMetaForObject(o);
		}

		// '\0' characters are considered null.
		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0))
			out.appendNull();
		else if (sType.isBoolean())
			out.appendBoolean((Boolean)o);
		else if (sType.isNumber())
			out.appendNumber((Number)o);
		else if (sType.hasToObjectMapMethod())
			serializeMap(session, out, sType.toObjectMap(o), sType);
		else if (sType.isBean())
			serializeBeanMap(session, out, bc.forBean(o), addClassAttr);
		else if (sType.isUri() || (pMeta != null && pMeta.isUri()))
			out.appendString(session.resolveUri(o.toString()));
		else if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(session, out, (BeanMap)o, addClassAttr);
			else
				serializeMap(session, out, (Map)o, eType);
		}
		else if (sType.isCollection()) {
			if (addClassAttr)
				serializeCollectionMap(session, out, (Collection)o, sType);
			else
				serializeCollection(session, out, (Collection) o, eType);
		}
		else if (sType.isArray()) {
			if (addClassAttr)
				serializeCollectionMap(session, out, toList(sType.getInnerClass(), o), sType);
			else
				serializeCollection(session, out, toList(sType.getInnerClass(), o), eType);
		} else
			out.appendString(session.toString(o));

		if (! isRecursion)
			session.pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeMap(MsgPackSerializerSession session, MsgPackOutputStream out, Map m, ClassMeta<?> type) throws Exception {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		m = session.sort(m);

		// The map size may change as we're iterating over it, so
		// grab a snapshot of the entries in a separate list.
		List<SimpleMapEntry> entries = new ArrayList<SimpleMapEntry>(m.size());
		for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
			entries.add(new SimpleMapEntry(e.getKey(), e.getValue()));

		out.startMap(entries.size());

		for (SimpleMapEntry e : entries) {
			Object value = e.value;
			Object key = session.generalize(e.key, keyType);

			serializeAnything(session, out, key, keyType, null, null);
			serializeAnything(session, out, value, valueType, null, null);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	private void serializeCollectionMap(MsgPackSerializerSession session, MsgPackOutputStream out, Collection o, ClassMeta<?> type) throws Exception {
		BeanContext bc = session.getBeanContext();
		out.startMap(2);
		serializeAnything(session, out, bc.getTypePropertyName(), null, null, null);
		serializeAnything(session, out, type.getInnerClass().getName(), null, null, null);
		serializeAnything(session, out, "items", null, null, null);
		serializeCollection(session, out, o, type);
	}

	private void serializeBeanMap(MsgPackSerializerSession session, MsgPackOutputStream out, final BeanMap<?> m, boolean addClassAttr) throws Exception {

		List<BeanPropertyValue> values = m.getValues(session.isTrimNulls(), addClassAttr ? session.createBeanClassProperty(m, null) : null);

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
				session.addBeanGetterWarning(pMeta, t);
			else {
				serializeAnything(session, out, key, null, null, null);
				serializeAnything(session, out, value, cMeta, key, pMeta);
			}
		}
	}

	private static class SimpleMapEntry {
		final Object key;
		final Object value;

		private SimpleMapEntry(Object key, Object value) {
			this.key = key;
			this.value = value;
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void serializeCollection(MsgPackSerializerSession session, MsgPackOutputStream out, Collection c, ClassMeta<?> type) throws Exception {

		ClassMeta<?> elementType = type.getElementType();
		List<Object> l = new ArrayList<Object>(c.size());

		c = session.sort(c);
		l.addAll(c);

		out.startArray(l.size());

		for (Object o : l)
			serializeAnything(session, out, o, elementType, "<iterator>", null);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public MsgPackSerializerSession createSession(Object output, ObjectMap properties, Method javaMethod) {
		return new MsgPackSerializerSession(getContext(MsgPackSerializerContext.class), getBeanContext(), output, properties, javaMethod);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		MsgPackSerializerSession s = (MsgPackSerializerSession)session;
		serializeAnything(s, s.getOutputStream(), o, null, "root", null);
	}

	@Override /* CoreApi */
	public MsgPackSerializer setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addBeanFilters(Class<?>...classes) throws LockedException {
		super.addBeanFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addPojoSwaps(Class<?>...classes) throws LockedException {
		super.addPojoSwaps(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> MsgPackSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public MsgPackSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public MsgPackSerializer clone() {
		try {
			MsgPackSerializer c = (MsgPackSerializer)super.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
