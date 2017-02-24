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
 * 	<li>Maps (e.g. {@link HashMap HashMaps}, {@link TreeMap TreeMaps}) are converted to JSON objects.
 * 	<li>Collections (e.g. {@link HashSet HashSets}, {@link LinkedList LinkedLists}) and Java arrays are converted to JSON arrays.
 * 	<li>{@link String Strings} are converted to JSON strings.
 * 	<li>{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to JSON numbers.
 * 	<li>{@link Boolean Booleans} are converted to JSON booleans.
 * 	<li>{@code nulls} are converted to JSON nulls.
 * 	<li>{@code arrays} are converted to JSON arrays.
 * 	<li>{@code beans} are converted to JSON objects.
 * </ul>
 * <p>
 * The types above are considered "JSON-primitive" object types.  Any non-JSON-primitive object types are transformed
 * 	into JSON-primitive object types through {@link org.apache.juneau.transform.PojoSwap PojoSwaps} associated through the {@link CoreApi#addPojoSwaps(Class...)}
 * 	method.  Several default transforms are provided for transforming Dates, Enums, Iterators, etc...
 * <p>
 * This serializer provides several serialization options.  Typically, one of the predefined DEFAULT serializers will be sufficient.
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
 * 	<li>{@link Simple} - Default serializer, single quotes, simple mode.
 * 	<li>{@link SimpleReadable} - Default serializer, single quotes, simple mode, with whitespace.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(someObject);
 *
 * 	<jc>// Create a custom serializer for lax syntax using single quote characters</jc>
 * 	JsonSerializer serializer = <jk>new</jk> JsonSerializer()
 * 		.setSimpleMode(<jk>true</jk>)
 * 		.setQuoteChar(<js>'\''</js>);
 *
 * 	<jc>// Clone an existing serializer and modify it to use single-quotes</jc>
 * 	JsonSerializer serializer = JsonSerializer.<jsf>DEFAULT</jsf>.clone()
 * 		.setQuoteChar(<js>'\''</js>);
 *
 * 	<jc>// Serialize a POJO to JSON</jc>
 * 	String json = serializer.serialize(someObject);
 * </p>
 */
@Produces("application/json,text/json")
public class JsonSerializer extends WriterSerializer {

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT = new JsonSerializer().lock();

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT_READABLE = new Readable().lock();

	/** Default serializer, single quotes, simple mode. */
	public static final JsonSerializer DEFAULT_LAX = new Simple().lock();

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final JsonSerializer DEFAULT_LAX_READABLE = new SimpleReadable().lock();

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static final JsonSerializer DEFAULT_LAX_READABLE_SAFE = new SimpleReadableSafe().lock();

	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSerializer {
		/** Constructor */
		public Readable() {
			setUseWhitespace(true);
		}
	}

	/** Default serializer, single quotes, simple mode. */
	@Produces(value="application/json+simple,text/json+simple",contentType="application/json")
	public static class Simple extends JsonSerializer {
		/** Constructor */
		public Simple() {
			setSimpleMode(true);
			setQuoteChar('\'');
		}
	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class SimpleReadable extends Simple {
		/** Constructor */
		public SimpleReadable() {
			setUseWhitespace(true);
		}
	}

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static class SimpleReadableSafe extends SimpleReadable {
		/** Constructor */
		public SimpleReadableSafe() {
			setDetectRecursions(true);
		}
	}

	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	SerializerWriter serializeAnything(JsonSerializerSession session, JsonWriter out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws Exception {

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
			out.append('{').cr(session.indent).attr(wrapperAttr).append(':').s();
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
			out.q().appendUri(o).q();
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
			out.cr(session.indent-1).append('}');
		}

		if (! isRecursion)
			session.pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(JsonSerializerSession session, JsonWriter out, Map m, ClassMeta<?> type) throws Exception {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		m = session.sort(m);

		int depth = session.getIndent();
		out.append('{');

		Iterator mapEntries = m.entrySet().iterator();

		while (mapEntries.hasNext()) {
			Map.Entry e = (Map.Entry) mapEntries.next();
			Object value = e.getValue();

			Object key = session.generalize(e.getKey(), keyType);

			out.cr(depth).attr(session.toString(key)).append(':').s();

			serializeAnything(session, out, value, valueType, (key == null ? null : session.toString(key)), null);

			if (mapEntries.hasNext())
				out.append(',');
		}

		out.cr(depth-1).append('}');

		return out;
	}

	private SerializerWriter serializeBeanMap(JsonSerializerSession session, JsonWriter out, BeanMap<?> m, String typeName) throws Exception {
		int depth = session.getIndent();
		out.append('{');

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

			out.cr(depth).attr(key).append(':').s();

			serializeAnything(session, out, value, cMeta, key, pMeta);

			addComma = true;
		}
		out.cr(depth-1).append('}');
		return out;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private SerializerWriter serializeCollection(JsonSerializerSession session, JsonWriter out, Collection c, ClassMeta<?> type) throws Exception {

		ClassMeta<?> elementType = type.getElementType();

		c = session.sort(c);

		out.append('[');
		int depth = session.getIndent();

		for (Iterator i = c.iterator(); i.hasNext();) {

			Object value = i.next();

			out.cr(depth);

			serializeAnything(session, out, value, elementType, "<iterator>", null);

			if (i.hasNext())
				out.append(',');
		}
		out.cr(depth-1).append(']');
		return out;
	}

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 * @return The schema serializer.
	 */
	public JsonSchemaSerializer getSchemaSerializer() {
		JsonSchemaSerializer s = new JsonSchemaSerializer(getContextFactory());
		return s;
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public JsonSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new JsonSerializerSession(getContext(JsonSerializerContext.class), op, output, javaMethod, locale, timeZone, mediaType);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		JsonSerializerSession s = (JsonSerializerSession)session;
		serializeAnything(s, s.getWriter(), o, null, "root", null);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Simple JSON mode.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"JsonSerializer.simpleMode"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, JSON attribute names will only be quoted when necessary.
	 * Otherwise, they are always quoted.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>JSON_simpleMode</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see JsonSerializerContext#JSON_simpleMode
	 */
	public JsonSerializer setSimpleMode(boolean value) throws LockedException {
		return setProperty(JSON_simpleMode, value);
	}

	/**
	 * <b>Configuration property:</b>  Prefix solidus <js>'/'</js> characters with escapes.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"JsonSerializer.escapeSolidus"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, solidus (e.g. slash) characters should be escaped.
	 * The JSON specification allows for either format.
	 * However, if you're embedding JSON in an HTML script tag, this setting prevents
	 * 	confusion when trying to serialize <xt>&lt;\/script&gt;</xt>.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>JSON_escapeSolidus</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see JsonSerializerContext#JSON_escapeSolidus
	 */
	public JsonSerializer setEscapeSolidus(boolean value) throws LockedException {
		return setProperty(JSON_escapeSolidus, value);
	}

	@Override /* Serializer */
	public JsonSerializer setMaxDepth(int value) throws LockedException {
		super.setMaxDepth(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setInitialDepth(int value) throws LockedException {
		super.setInitialDepth(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setDetectRecursions(boolean value) throws LockedException {
		super.setDetectRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setIgnoreRecursions(boolean value) throws LockedException {
		super.setIgnoreRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setUseWhitespace(boolean value) throws LockedException {
		super.setUseWhitespace(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setAddBeanTypeProperties(boolean value) throws LockedException {
		super.setAddBeanTypeProperties(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setQuoteChar(char value) throws LockedException {
		super.setQuoteChar(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setTrimNullProperties(boolean value) throws LockedException {
		super.setTrimNullProperties(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setTrimEmptyCollections(boolean value) throws LockedException {
		super.setTrimEmptyCollections(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setTrimEmptyMaps(boolean value) throws LockedException {
		super.setTrimEmptyMaps(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setTrimStrings(boolean value) throws LockedException {
		super.setTrimStrings(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setRelativeUriBase(String value) throws LockedException {
		super.setRelativeUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setAbsolutePathUriBase(String value) throws LockedException {
		super.setAbsolutePathUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setSortCollections(boolean value) throws LockedException {
		super.setSortCollections(value);
		return this;
	}

	@Override /* Serializer */
	public JsonSerializer setSortMaps(boolean value) throws LockedException {
		super.setSortMaps(value);
		return this;
	}
	@Override /* CoreApi */
	public JsonSerializer setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		super.setBeansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeansRequireSerializable(boolean value) throws LockedException {
		super.setBeansRequireSerializable(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeansRequireSettersForGetters(boolean value) throws LockedException {
		super.setBeansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeansRequireSomeProperties(boolean value) throws LockedException {
		super.setBeansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		super.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeanConstructorVisibility(Visibility value) throws LockedException {
		super.setBeanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeanClassVisibility(Visibility value) throws LockedException {
		super.setBeanClassVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeanFieldVisibility(Visibility value) throws LockedException {
		super.setBeanFieldVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setMethodVisibility(Visibility value) throws LockedException {
		super.setMethodVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setUseJavaBeanIntrospector(boolean value) throws LockedException {
		super.setUseJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setUseInterfaceProxies(boolean value) throws LockedException {
		super.setUseInterfaceProxies(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		super.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setSortProperties(boolean value) throws LockedException {
		super.setSortProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setNotBeanPackages(String...values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setNotBeanPackages(Collection<String> values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addNotBeanPackages(String...values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addNotBeanPackages(Collection<String> values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removeNotBeanPackages(String...values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removeNotBeanPackages(Collection<String> values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setNotBeanClasses(Class<?>...values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addNotBeanClasses(Class<?>...values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removeNotBeanClasses(Class<?>...values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeanFilters(Class<?>...values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addBeanFilters(Class<?>...values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removeBeanFilters(Class<?>...values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setPojoSwaps(Class<?>...values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addPojoSwaps(Class<?>...values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removePojoSwaps(Class<?>...values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		super.setImplClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeanDictionary(Class<?>...values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addToBeanDictionary(Class<?>...values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removeFromBeanDictionary(Class<?>...values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setBeanTypePropertyName(String value) throws LockedException {
		super.setBeanTypePropertyName(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setDefaultParser(Class<?> value) throws LockedException {
		super.setDefaultParser(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setLocale(Locale value) throws LockedException {
		super.setLocale(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setTimeZone(TimeZone value) throws LockedException {
		super.setTimeZone(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setMediaType(MediaType value) throws LockedException {
		super.setMediaType(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setDebug(boolean value) throws LockedException {
		super.setDebug(value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setProperty(String name, Object value) throws LockedException {
		super.setProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addToProperty(String name, Object value) throws LockedException {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer putToProperty(String name, Object key, Object value) throws LockedException {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer putToProperty(String name, Object value) throws LockedException {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer removeFromProperty(String name, Object value) throws LockedException {
		super.removeFromProperty(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public JsonSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public JsonSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public JsonSerializer clone() {
		try {
			JsonSerializer c = (JsonSerializer)super.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
