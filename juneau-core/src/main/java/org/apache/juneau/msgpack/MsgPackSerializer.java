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
 * <h5 class='section'>Media types:</h5>
 * <p>
 * 	Handles <code>Accept</code> types: <code>octal/msgpack</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>octal/msgpack</code>
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link MsgPackSerializerContext}
 * 	<li>{@link SerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
@Produces("octal/msgpack")
public class MsgPackSerializer extends OutputStreamSerializer {

	/** Default serializer, all default settings.*/
	public static final MsgPackSerializer DEFAULT = new MsgPackSerializer().lock();

	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	MsgPackOutputStream serializeAnything(MsgPackSerializerSession session, MsgPackOutputStream out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws Exception {

		if (o == null)
			return out.appendNull();

		if (eType == null)
			eType = object();

		boolean addTypeProperty;		// Add "_type" attribute to element?
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
		addTypeProperty = (session.isAddBeanTypeProperties() && ! eType.equals(aType));

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
			out.appendNull();
		else if (sType.isBoolean())
			out.appendBoolean((Boolean)o);
		else if (sType.isNumber())
			out.appendNumber((Number)o);
		else if (sType.isBean())
			serializeBeanMap(session, out, session.toBeanMap(o), addTypeProperty);
		else if (sType.isUri() || (pMeta != null && pMeta.isUri()))
			out.appendString(session.resolveUri(o.toString()));
		else if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(session, out, (BeanMap)o, addTypeProperty);
			else
				serializeMap(session, out, (Map)o, eType);
		}
		else if (sType.isCollection()) {
			serializeCollection(session, out, (Collection) o, eType);
		}
		else if (sType.isArray()) {
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

	private void serializeBeanMap(MsgPackSerializerSession session, MsgPackOutputStream out, final BeanMap<?> m, boolean addTypeProperty) throws Exception {

		List<BeanPropertyValue> values = m.getValues(session.isTrimNulls(), addTypeProperty ? session.createBeanTypeNameProperty(m) : null);

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
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public MsgPackSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new MsgPackSerializerSession(getContext(MsgPackSerializerContext.class), op, output, javaMethod, locale, timeZone, mediaType);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		MsgPackSerializerSession s = (MsgPackSerializerSession)session;
		serializeAnything(s, s.getOutputStream(), o, null, "root", null);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public MsgPackSerializer setMaxDepth(int value) throws LockedException {
		super.setMaxDepth(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setInitialDepth(int value) throws LockedException {
		super.setInitialDepth(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setDetectRecursions(boolean value) throws LockedException {
		super.setDetectRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setIgnoreRecursions(boolean value) throws LockedException {
		super.setIgnoreRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setUseIndentation(boolean value) throws LockedException {
		super.setUseIndentation(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setAddBeanTypeProperties(boolean value) throws LockedException {
		super.setAddBeanTypeProperties(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setQuoteChar(char value) throws LockedException {
		super.setQuoteChar(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setTrimNullProperties(boolean value) throws LockedException {
		super.setTrimNullProperties(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setTrimEmptyCollections(boolean value) throws LockedException {
		super.setTrimEmptyCollections(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setTrimEmptyMaps(boolean value) throws LockedException {
		super.setTrimEmptyMaps(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setTrimStrings(boolean value) throws LockedException {
		super.setTrimStrings(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setRelativeUriBase(String value) throws LockedException {
		super.setRelativeUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setAbsolutePathUriBase(String value) throws LockedException {
		super.setAbsolutePathUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setSortCollections(boolean value) throws LockedException {
		super.setSortCollections(value);
		return this;
	}

	@Override /* Serializer */
	public MsgPackSerializer setSortMaps(boolean value) throws LockedException {
		super.setSortMaps(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		super.setBeansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeansRequireSerializable(boolean value) throws LockedException {
		super.setBeansRequireSerializable(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeansRequireSettersForGetters(boolean value) throws LockedException {
		super.setBeansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeansRequireSomeProperties(boolean value) throws LockedException {
		super.setBeansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		super.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeanConstructorVisibility(Visibility value) throws LockedException {
		super.setBeanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeanClassVisibility(Visibility value) throws LockedException {
		super.setBeanClassVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeanFieldVisibility(Visibility value) throws LockedException {
		super.setBeanFieldVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setMethodVisibility(Visibility value) throws LockedException {
		super.setMethodVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setUseJavaBeanIntrospector(boolean value) throws LockedException {
		super.setUseJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setUseInterfaceProxies(boolean value) throws LockedException {
		super.setUseInterfaceProxies(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		super.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setSortProperties(boolean value) throws LockedException {
		super.setSortProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setNotBeanPackages(String...values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setNotBeanPackages(Collection<String> values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addNotBeanPackages(String...values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addNotBeanPackages(Collection<String> values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removeNotBeanPackages(String...values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removeNotBeanPackages(Collection<String> values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setNotBeanClasses(Class<?>...values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addNotBeanClasses(Class<?>...values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removeNotBeanClasses(Class<?>...values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeanFilters(Class<?>...values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addBeanFilters(Class<?>...values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removeBeanFilters(Class<?>...values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setPojoSwaps(Class<?>...values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addPojoSwaps(Class<?>...values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removePojoSwaps(Class<?>...values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		super.setImplClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeanDictionary(Class<?>...values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addToBeanDictionary(Class<?>...values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removeFromBeanDictionary(Class<?>...values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setBeanTypePropertyName(String value) throws LockedException {
		super.setBeanTypePropertyName(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setDefaultParser(Class<?> value) throws LockedException {
		super.setDefaultParser(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setLocale(Locale value) throws LockedException {
		super.setLocale(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setTimeZone(TimeZone value) throws LockedException {
		super.setTimeZone(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setMediaType(MediaType value) throws LockedException {
		super.setMediaType(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setDebug(boolean value) throws LockedException {
		super.setDebug(value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setProperty(String name, Object value) throws LockedException {
		super.setProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer addToProperty(String name, Object value) throws LockedException {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer putToProperty(String name, Object key, Object value) throws LockedException {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer putToProperty(String name, Object value) throws LockedException {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackSerializer removeFromProperty(String name, Object value) throws LockedException {
		super.removeFromProperty(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

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
