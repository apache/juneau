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
package org.apache.juneau;

import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Session object that lives for the duration of a single use of {@link Serializer} or {@link Parser}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BeanSession extends Session {

	private final BeanContext ctx;
	private final Locale locale;
	private final TimeZone timeZone;
	private final MediaType mediaType;
	private final boolean debug;
	private Stack<StringBuilder> sbStack = new Stack<StringBuilder>();

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param locale The session locale.
	 * 	If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * 	If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 */
	protected BeanSession(BeanContext ctx, ObjectMap op, Locale locale, TimeZone timeZone, MediaType mediaType) {
		super(ctx, op);
		this.ctx = ctx;
		if (op == null || op.isEmpty()) {
			this.locale = (locale != null ? locale : ctx.locale);
			this.timeZone = (timeZone != null ? timeZone : ctx.timeZone);
			this.debug = ctx.debug;
			this.mediaType = mediaType != null ? mediaType : ctx.mediaType;
		} else {
			this.locale = (locale == null ? op.get(Locale.class, BEAN_locale, ctx.locale) : locale);
			this.timeZone = (timeZone == null ? op.get(TimeZone.class, BEAN_timeZone, ctx.timeZone) : timeZone);
			this.debug = op.getBoolean(BEAN_debug, false);
			this.mediaType = (mediaType == null ? op.get(MediaType.class, BEAN_mediaType, ctx.mediaType) : mediaType);
		}
	}

	/**
	 * Returns the locale defined on this session.
	 * <p>
	 * The locale is determined in the following order:
	 * <ol>
	 * 	<li><code>locale</code> parameter passed in through constructor.
	 * 	<li>{@link BeanContext#BEAN_locale} entry in <code>properties</code> parameter passed in through constructor.
	 * 	<li>{@link BeanContext#BEAN_locale} setting on bean context.
	 * 	<li>Locale returned by {@link Locale#getDefault()}.
	 * </ol>
	 *
	 * @return The session locale.
	 */
	public final Locale getLocale() {
		return locale;
	}

	/**
	 * Returns the timezone defined on this session.
	 * The timezone is determined in the following order:
	 * <ol>
	 * 	<li><code>timeZone</code> parameter passed in through constructor.
	 * 	<li>{@link BeanContext#BEAN_timeZone} entry in <code>properties</code> parameter passed in through constructor.
	 * 	<li>{@link BeanContext#BEAN_timeZone} setting on bean context.
	 * </ol>
	 *
	 * @return The session timezone, or <jk>null</jk> if timezone not specified.
	 */
	public final TimeZone getTimeZone() {
		return timeZone;
	}

	/**
	 * Returns the {@link SerializerContext#BEAN_debug} setting value for this session.
	 *
	 * @return The {@link SerializerContext#BEAN_debug} setting value for this session.
	 */
	public final boolean isDebug() {
		return debug;
	}

	/**
	 * Bean property getter:  <property>ignoreUnknownBeanProperties</property>.
	 * See {@link BeanContext#BEAN_ignoreUnknownBeanProperties}.
	 *
	 * @return The value of the <property>ignoreUnknownBeanProperties</property> property on this bean.
	 */
	public final boolean isIgnoreUnknownBeanProperties() {
		return ctx.ignoreUnknownBeanProperties;
	}

	/**
	 * Converts the specified value to the specified class type.
	 * <p>
	 * 	See {@link #convertToType(Object, ClassMeta)} for the list of valid conversions.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public final <T> T convertToType(Object value, Class<T> type) throws InvalidDataConversionException {
		// Shortcut for most common case.
		if (value != null && value.getClass() == type)
			return (T)value;
		return convertToType(null, value, ctx.getClassMeta(type));
	}

	/**
	 * Same as {@link #convertToType(Object, Class)}, except used for instantiating inner member classes that must
	 * 	be instantiated within another class instance.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param outer If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public final <T> T convertToType(Object outer, Object value, Class<T> type) throws InvalidDataConversionException {
		return convertToType(outer, value, ctx.getClassMeta(type));
	}

	/**
	 * Casts the specified value into the specified type.
	 * <p>
	 * 	If the value isn't an instance of the specified type, then converts
	 * 	the value if possible.<br>
	 * <p>
	 * 	The following conversions are valid:
	 * 	<table class='styled'>
	 * 		<tr><th>Convert to type</th><th>Valid input value types</th><th>Notes</th></tr>
	 * 		<tr>
	 * 			<td>
	 * 				A class that is the normal type of a registered {@link PojoSwap}.
	 * 			</td>
	 * 			<td>
	 * 				A value whose class matches the transformed type of that registered {@link PojoSwap}.
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				A class that is the transformed type of a registered {@link PojoSwap}.
	 * 			</td>
	 * 			<td>
	 * 				A value whose class matches the normal type of that registered {@link PojoSwap}.
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code Number} (e.g. {@code Integer}, {@code Short}, {@code Float},...)<br>
	 * 				<code>Number.<jsf>TYPE</jsf></code> (e.g. <code>Integer.<jsf>TYPE</jsf></code>, <code>Short.<jsf>TYPE</jsf></code>, <code>Float.<jsf>TYPE</jsf></code>,...)
	 * 			</td>
	 * 			<td>
	 * 				{@code Number}, {@code String}, <jk>null</jk>
	 * 			</td>
	 * 			<td>
	 * 				For primitive {@code TYPES}, <jk>null</jk> returns the JVM default value for that type.
	 * 			</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code Map} (e.g. {@code Map}, {@code HashMap}, {@code TreeMap}, {@code ObjectMap})
	 * 			</td>
	 * 			<td>
	 * 				{@code Map}
	 * 			</td>
	 * 			<td>
	 * 				If {@code Map} is not constructible, a {@code ObjectMap} is created.
	 * 			</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 			{@code Collection} (e.g. {@code List}, {@code LinkedList}, {@code HashSet}, {@code ObjectList})
	 * 			</td>
	 * 			<td>
	 * 				{@code Collection<Object>}<br>
	 * 				{@code Object[]}
	 * 			</td>
	 * 			<td>
	 * 				If {@code Collection} is not constructible, a {@code ObjectList} is created.
	 * 			</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code X[]} (array of any type X)<br>
	 * 			</td>
	 * 			<td>
	 * 				{@code List<X>}<br>
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code X[][]} (multi-dimensional arrays)<br>
	 * 			</td>
	 * 			<td>
	 * 				{@code List<List<X>>}<br>
	 * 				{@code List<X[]>}<br>
	 * 				{@code List[]<X>}<br>
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code Enum}<br>
	 * 			</td>
	 * 			<td>
	 * 				{@code String}<br>
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				Bean<br>
	 * 			</td>
	 * 			<td>
	 * 				{@code Map}<br>
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code String}<br>
	 * 			</td>
	 * 			<td>
	 * 				Anything<br>
	 * 			</td>
	 * 			<td>
	 * 				Arrays are converted to JSON arrays<br>
	 * 			</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				Anything with one of the following methods:<br>
	 * 				<code><jk>public static</jk> T fromString(String)</code><br>
	 * 				<code><jk>public static</jk> T valueOf(String)</code><br>
	 * 				<code><jk>public</jk> T(String)</code><br>
	 * 			</td>
	 * 			<td>
	 * 				<code>String</code><br>
	 * 			</td>
	 * 			<td>
	 * 				<br>
	 * 			</td>
	 * 		</tr>
	 * 	</table>
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to be converted.
	 * @param type The target object type.
	 * @return The converted type.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 */
	public final <T> T convertToType(Object value, ClassMeta<T> type) throws InvalidDataConversionException {
		return convertToType(null, value, type);
	}

	/**
	 * Same as {@link #convertToType(Object, ClassMeta)}, except used for instantiating inner member classes that must
	 * 	be instantiated within another class instance.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param outer If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public final <T> T convertToType(Object outer, Object value, ClassMeta<T> type) throws InvalidDataConversionException {
		if (type == null)
			type = (ClassMeta<T>)ctx.object();

		try {
			// Handle the case of a null value.
			if (value == null) {

				// If it's a primitive, then use the converters to get the default value for the primitive type.
				if (type.isPrimitive())
					return type.getPrimitiveDefault();

				// Otherwise, just return null.
				return null;
			}

			Class<T> tc = type.getInnerClass();

			// If no conversion needed, then just return the value.
			// Don't include maps or collections, because child elements may need conversion.
			if (tc.isInstance(value))
				if (! ((type.isMap() && type.getValueType().isNotObject()) || (type.isCollection() && type.getElementType().isNotObject())))
					return (T)value;

			if (tc == Class.class)
				return (T)(ctx.classLoader.loadClass(value.toString()));

			if (type.getPojoSwap() != null) {
				PojoSwap f = type.getPojoSwap();
				Class<?> nc = f.getNormalClass(), fc = f.getSwapClass();
				if (isParentClass(nc, tc) && isParentClass(fc, value.getClass()))
					return (T)f.unswap(this, value, type);
			}

			ClassMeta<?> vt = ctx.getClassMetaForObject(value);
			if (vt.getPojoSwap() != null) {
				PojoSwap f = vt.getPojoSwap();
				Class<?> nc = f.getNormalClass(), fc = f.getSwapClass();
				if (isParentClass(nc, vt.getInnerClass()) && isParentClass(fc, tc))
					return (T)f.swap(this, value);
			}

			if (type.isPrimitive()) {
				if (value.toString().isEmpty())
					return type.getPrimitiveDefault();

				if (type.isNumber()) {
					if (value instanceof Number) {
						Number n = (Number)value;
						if (tc == Integer.TYPE)
							return (T)Integer.valueOf(n.intValue());
						if (tc == Short.TYPE)
							return (T)Short.valueOf(n.shortValue());
						if (tc == Long.TYPE)
							return (T)Long.valueOf(n.longValue());
						if (tc == Float.TYPE)
							return (T)Float.valueOf(n.floatValue());
						if (tc == Double.TYPE)
							return (T)Double.valueOf(n.doubleValue());
						if (tc == Byte.TYPE)
							return (T)Byte.valueOf(n.byteValue());
					} else {
						String n = null;
						if (value instanceof Boolean)
							n = ((Boolean)value).booleanValue() ? "1" : "0";
						else
							n = value.toString();
						if (tc == Integer.TYPE)
							return (T)Integer.valueOf(n);
						if (tc == Short.TYPE)
							return (T)Short.valueOf(n);
						if (tc == Long.TYPE)
							return (T)Long.valueOf(n);
						if (tc == Float.TYPE)
							return (T)new Float(n);
						if (tc == Double.TYPE)
							return (T)new Double(n);
						if (tc == Byte.TYPE)
							return (T)Byte.valueOf(n);
					}
				} else if (type.isChar()) {
					String s = value.toString();
					return (T)Character.valueOf(s.length() == 0 ? 0 : s.charAt(0));
				} else if (type.isBoolean()) {
					if (value instanceof Number) {
						int i = ((Number)value).intValue();
						return (T)(i == 0 ? Boolean.FALSE : Boolean.TRUE);
					}
					return (T)Boolean.valueOf(value.toString());
				}
			}

			if (type.isNumber()) {
				if (value instanceof Number) {
					Number n = (Number)value;
					if (tc == Integer.class)
						return (T)Integer.valueOf(n.intValue());
					if (tc == Short.class)
						return (T)Short.valueOf(n.shortValue());
					if (tc == Long.class)
						return (T)Long.valueOf(n.longValue());
					if (tc == Float.class)
						return (T)Float.valueOf(n.floatValue());
					if (tc == Double.class)
						return (T)Double.valueOf(n.doubleValue());
					if (tc == Byte.class)
						return (T)Byte.valueOf(n.byteValue());
					if (tc == Byte.class)
						return (T)Byte.valueOf(n.byteValue());
					if (tc == AtomicInteger.class)
						return (T)new AtomicInteger(n.intValue());
					if (tc == AtomicLong.class)
						return (T)new AtomicLong(n.intValue());
				} else {
					if (value.toString().isEmpty())
						return null;
					String n = null;
					if (value instanceof Boolean)
						n = ((Boolean)value).booleanValue() ? "1" : "0";
					else
						n = value.toString();
					if (tc == Integer.class)
						return (T)Integer.valueOf(n);
					if (tc == Short.class)
						return (T)Short.valueOf(n);
					if (tc == Long.class)
						return (T)Long.valueOf(n);
					if (tc == Float.class)
						return (T)new Float(n);
					if (tc == Double.class)
						return (T)new Double(n);
					if (tc == Byte.class)
						return (T)Byte.valueOf(n);
					if (tc == AtomicInteger.class)
						return (T)new AtomicInteger(Integer.valueOf(n));
					if (tc == AtomicLong.class)
						return (T)new AtomicLong(Long.valueOf(n));
				}
			}

			if (type.isChar()) {
				String s = value.toString();
				return (T)Character.valueOf(s.length() == 0 ? 0 : s.charAt(0));
			}

			// Handle setting of array properties
			if (type.isArray()) {
				if (vt.isCollection())
					return (T)toArray(type, (Collection)value);
				else if (vt.isArray())
					return (T)toArray(type, Arrays.asList((Object[])value));
				else if (StringUtils.startsWith(value.toString(), '['))
					return (T)toArray(type, new ObjectList(value.toString()).setBeanSession(this));
			}

			// Target type is some sort of Map that needs to be converted.
			if (type.isMap()) {
				try {
					if (value instanceof Map) {
						Map m = type.canCreateNewInstance(outer) ? (Map)type.newInstance(outer) : new ObjectMap(this);
						ClassMeta keyType = type.getKeyType(), valueType = type.getValueType();
						for (Map.Entry e : (Set<Map.Entry>)((Map)value).entrySet()) {
							Object k = e.getKey();
							if (keyType.isNotObject()) {
								if (keyType.isString() && k.getClass() != Class.class)
									k = k.toString();
								else
									k = convertToType(m, k, keyType);
							}
							Object v = e.getValue();
							if (valueType.isNotObject())
								v = convertToType(m, v, valueType);
							m.put(k, v);
						}
						return (T)m;
					} else if (!type.canCreateNewInstanceFromString(outer)) {
						ObjectMap m = new ObjectMap(value.toString(), ctx.defaultParser);
						return convertToType(outer, m, type);
					}
				} catch (Exception e) {
					throw new InvalidDataConversionException(value.getClass(), type, e);
				}
			}

			// Target type is some sort of Collection
			if (type.isCollection()) {
				try {
					Collection l = type.canCreateNewInstance(outer) ? (Collection)type.newInstance(outer) : new ObjectList(this);
					ClassMeta elementType = type.getElementType();

					if (value.getClass().isArray())
						for (Object o : (Object[])value)
							l.add(elementType.isObject() ? o : convertToType(l, o, elementType));
					else if (value instanceof Collection)
						for (Object o : (Collection)value)
							l.add(elementType.isObject() ? o : convertToType(l, o, elementType));
					else if (value instanceof Map)
						l.add(elementType.isObject() ? value : convertToType(l, value, elementType));
					else if (! value.toString().isEmpty())
						throw new InvalidDataConversionException(value.getClass(), type, null);
					return (T)l;
				} catch (InvalidDataConversionException e) {
					throw e;
				} catch (Exception e) {
					throw new InvalidDataConversionException(value.getClass(), type, e);
				}
			}

			if (type.isEnum()) {
				if (type.canCreateNewInstanceFromString(outer))
					return type.newInstanceFromString(outer, value.toString());
				return (T)Enum.valueOf((Class<? extends Enum>)tc, value.toString());
			}

			if (type.isString()) {
				if (vt.isMapOrBean() || vt.isCollectionOrArray()) {
					if (JsonSerializer.DEFAULT_LAX != null)
						return (T)JsonSerializer.DEFAULT_LAX.serialize(value);
				} else if (vt.isClass()) {
					return (T)ClassUtils.getReadableClassName((Class<?>)value);
				}
				return (T)value.toString();
			}

			if (type.isCharSequence()) {
				Class<?> c = value.getClass();
				if (c.isArray()) {
					if (c.getComponentType().isPrimitive()) {
						ObjectList l = new ObjectList(this);
						int size = Array.getLength(value);
						for (int i = 0; i < size; i++)
							l.add(Array.get(value, i));
						value = l;
					}
					value = new ObjectList((Object[])value).setBeanSession(this);
				}

				return type.newInstanceFromString(outer, value.toString());
			}

			if (type.isBoolean()) {
				if (value instanceof Number)
					return (T)(Boolean.valueOf(((Number)value).intValue() != 0));
				return (T)Boolean.valueOf(value.toString());
			}

			// It's a bean being initialized with a Map
			if (type.isBean() && value instanceof Map)
				return newBeanMap(tc).load((Map<?,?>) value).getBean();

			if (type.canCreateNewInstanceFromObjectMap(outer) && value instanceof ObjectMap)
				return type.newInstanceFromObjectMap(outer, (ObjectMap)value);

			if (type.canCreateNewInstanceFromNumber(outer) && value instanceof Number)
				return type.newInstanceFromNumber(this, outer, (Number)value);

			if (type.canCreateNewInstanceFromString(outer))
				return type.newInstanceFromString(outer, value.toString());

			if (type.isBean())
				return newBeanMap(type.getInnerClass()).load(value.toString()).getBean();

		} catch (Exception e) {
			throw new InvalidDataConversionException(value, type, e);
		}

		throw new InvalidDataConversionException(value, type, null);
	}

	/**
	 * Converts the contents of the specified list into an array.
	 * <p>
	 * 	Works on both object and primitive arrays.
	 * <p>
	 * 	In the case of multi-dimensional arrays, the incoming list must
	 * 	contain elements of type n-1 dimension.  i.e. if {@code type} is <code><jk>int</jk>[][]</code>
	 * 	then {@code list} must have entries of type <code><jk>int</jk>[]</code>.
	 *
	 * @param type The type to convert to.  Must be an array type.
	 * @param list The contents to populate the array with.
	 * @return A new object or primitive array.
	 */
	public final Object toArray(ClassMeta<?> type, Collection<?> list) {
		if (list == null)
			return null;
		ClassMeta<?> componentType = type.getElementType();
		Object array = Array.newInstance(componentType.getInnerClass(), list.size());
		int i = 0;
		for (Object o : list) {
			if (! type.getInnerClass().isInstance(o)) {
				if (componentType.isArray() && o instanceof Collection)
					o = toArray(componentType, (Collection<?>)o);
				else if (o == null && componentType.isPrimitive())
					o = componentType.getPrimitiveDefault();
				else
					o = convertToType(null, o, componentType);
			}
			try {
				Array.set(array, i++, o);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				throw e;
			}
		}
		return array;
	}

	/**
	 * Wraps an object inside a {@link BeanMap} object (i.e. a modifiable {@link Map}).
	 * <p>
	 * 	If object is not a true bean, then throws a {@link BeanRuntimeException} with an explanation of why it's not a bean.
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p class='bcode'>
	 * 	<jc>// Construct a bean map around a bean instance</jc>
	 * 	BeanMap&lt;Person&gt; bm = BeanContext.<jsf>DEFAULT</jsf>.forBean(<jk>new</jk> Person());
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param o The object to wrap in a map interface.  Must not be null.
	 * @return The wrapped object.
	 */
	public final <T> BeanMap<T> toBeanMap(T o) {
		return this.toBeanMap(o, (Class<T>)o.getClass());
	}

	/**
	 * Determines whether the specified object matches the requirements on this context of being a bean.
	 *
	 * @param o The object being tested.
	 * @return <jk>true</jk> if the specified object is considered a bean.
	 */
	public final boolean isBean(Object o) {
		if (o == null)
			return false;
		return isBean(o.getClass());
	}

	/**
	 * Determines whether the specified class matches the requirements on this context of being a bean.
	 *
	 * @param c The class being tested.
	 * @return <jk>true</jk> if the specified class is considered a bean.
	 */
	public final boolean isBean(Class<?> c) {
		return getBeanMeta(c) != null;
	}

	/**
	 * Wraps an object inside a {@link BeanMap} object (i.e.: a modifiable {@link Map})
	 * defined as a bean for one of its class, a super class, or an implemented interface.
	 * <p>
	 * 	If object is not a true bean, throws a {@link BeanRuntimeException} with an explanation of why it's not a bean.
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p class='bcode'>
	 * 	<jc>// Construct a bean map for new bean using only properties defined in a superclass</jc>
	 * 	BeanMap&lt;MySubBean&gt; bm = BeanContext.<jsf>DEFAULT</jsf>.forBean(<jk>new</jk> MySubBean(), MySuperBean.<jk>class</jk>);
	 *
	 * 	<jc>// Construct a bean map for new bean using only properties defined in an interface</jc>
	 * 	BeanMap&lt;MySubBean&gt; bm = BeanContext.<jsf>DEFAULT</jsf>.forBean(<jk>new</jk> MySubBean(), MySuperInterface.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param o The object to wrap in a bean interface.  Must not be null.
	 * @param c The superclass to narrow the bean properties to.  Must not be null.
	 * @return The bean representation, or <jk>null</jk> if the object is not a true bean.
	 * @throws NullPointerException If either parameter is null.
	 * @throws IllegalArgumentException If the specified object is not an an instance of the specified class.
	 * @throws BeanRuntimeException If specified object is not a bean according to the bean rules
	 * 		specified in this context class.
	 */
	public final <T> BeanMap<T> toBeanMap(T o, Class<? super T> c) throws BeanRuntimeException {
		assertFieldNotNull(o, "o");
		assertFieldNotNull(c, "c");

		if (! c.isInstance(o))
			illegalArg("The specified object is not an instance of the specified class.  class=''{0}'', objectClass=''{1}'', object=''{2}''", c.getName(), o.getClass().getName(), 0);

		ClassMeta cm = getClassMeta(c);

		BeanMeta m = cm.getBeanMeta();
		if (m == null)
			throw new BeanRuntimeException(c, "Class is not a bean.  Reason=''{0}''", cm.getNotABeanReason());
		return new BeanMap<T>(this, o, m);
	}

	/**
	 * Creates a new {@link BeanMap} object (i.e. a modifiable {@link Map}) of the given class with uninitialized property values.
	 * <p>
	 * 	If object is not a true bean, then throws a {@link BeanRuntimeException} with an explanation of why it's not a bean.
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p class='bcode'>
	 * 	<jc>// Construct a new bean map wrapped around a new Person object</jc>
	 * 	BeanMap&lt;Person&gt; bm = BeanContext.<jsf>DEFAULT</jsf>.newBeanMap(Person.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param c The name of the class to create a new instance of.
	 * @return A new instance of the class.
	 */
	public final <T> BeanMap<T> newBeanMap(Class<T> c) {
		return newBeanMap(null, c);
	}

	/**
	 * Same as {@link #newBeanMap(Class)}, except used for instantiating inner member classes that must
	 * 	be instantiated within another class instance.
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param c The name of the class to create a new instance of.
	 * @param outer If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @return A new instance of the class.
	 */
	public final <T> BeanMap<T> newBeanMap(Object outer, Class<T> c) {
		BeanMeta m = getBeanMeta(c);
		if (m == null)
			return null;
		T bean = null;
		if (m.constructorArgs.length == 0) {
			bean = newBean(outer, c);
			// Beans with subtypes won't be instantiated until the sub type property is specified.
			if (bean == null && ! m.getClassMeta().hasSubTypes())
				return null;
		}
		return new BeanMap<T>(this, bean, m);
	}

	/**
	 * Creates a new empty bean of the specified type, except used for instantiating inner member classes that must
	 * 	be instantiated within another class instance.
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <p class='bcode'>
	 * 	<jc>// Construct a new instance of the specified bean class</jc>
	 * 	Person p = BeanContext.<jsf>DEFAULT</jsf>.newBean(Person.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class type of the bean being created.
	 * @param c The class type of the bean being created.
	 * @return A new bean object.
	 * @throws BeanRuntimeException If the specified class is not a valid bean.
	 */
	public final <T> T newBean(Class<T> c) throws BeanRuntimeException {
		return newBean(null, c);
	}

	/**
	 * Same as {@link #newBean(Class)}, except used for instantiating inner member classes that must
	 * 	be instantiated within another class instance.
	 *
	 * @param <T> The class type of the bean being created.
	 * @param c The class type of the bean being created.
	 * @param outer If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @return A new bean object.
	 * @throws BeanRuntimeException If the specified class is not a valid bean.
	 */
	public final <T> T newBean(Object outer, Class<T> c) throws BeanRuntimeException {
		ClassMeta<T> cm = getClassMeta(c);
		BeanMeta m = cm.getBeanMeta();
		if (m == null)
			return null;
		try {
			T o = (T)m.newBean(outer);
			if (o == null) {
				// Beans with subtypes won't be instantiated until the sub type property is specified.
				if (cm.beanFilter != null && cm.beanFilter.getSubTypeProperty() != null)
					return null;
				throw new BeanRuntimeException(c, "Class does not have a no-arg constructor.");
			}
			return o;
		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
		}
	}

	/**
	 * Returns the {@link BeanMeta} class for the specified class.
	 *
	 * @param <T> The class type to get the meta-data on.
	 * @param c The class to get the meta-data on.
	 * @return The {@link BeanMeta} for the specified class, or <jk>null</jk> if the class
	 * 	is not a bean per the settings on this context.
	 */
	public final <T> BeanMeta<T> getBeanMeta(Class<T> c) {
		if (c == null)
			return null;
		return getClassMeta(c).getBeanMeta();
	}

	/**
	 * Returns the class type bound to this bean context if the specified class type
	 * 	is from another bean context.
	 * <p>
	 * For example, this method allows you to pass in an object from <code>BeanContext.<jsf>DEFAULT</jsf>.getMapClassMeta(...)</code>
	 * 	to any of the <code>ReaderParser.parse(Reader, ClassMeta, ParserContext)</code> methods, and the parsers
	 * 	will use this method to replace the class type with the one registered with the parser.
	 * This ensures that registered transforms are applied correctly.
	 *
	 * @param <T> The class type.
	 * @param cm The class type.
	 * @return The class type bound by this bean context.
	 */
	public final <T> ClassMeta<T> normalizeClassMeta(ClassMeta<T> cm) {
		return ctx.normalizeClassMeta(cm);
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Class} object.
	 *
	 * @param <T> The class type being wrapped.
	 * @param c The class being wrapped.
	 * 	of type {@link Class} or {@link ClassMeta}.
	 * @return If the class is not an array, returns a cached {@link ClassMeta} object.
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.<br>
	 */
	public final <T> ClassMeta<T> getClassMeta(Class<T> c) {
		return ctx.getClassMeta(c);
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Map} object.
	 *
	 * @param <K> The map key class type.
	 * @param <V> The map value class type.
	 * @param <T> The map class type.
	 * @param c The map class type.
	 * @param keyType The map key class type.
	 * @param valueType The map value class type.
	 * @return If the key and value types are OBJECT, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public final <K,V,T extends Map<K,V>> ClassMeta<T> getMapClassMeta(Class<T> c, ClassMeta<K> keyType, ClassMeta<V> valueType) {
		return ctx.getMapClassMeta(c, keyType, valueType);
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Map} object.
	 *
	 * @param <K> The map key class type.
	 * @param <V> The map value class type.
	 * @param <T> The map class type.
	 * @param c The map class type.
	 * @param keyType The map key class type.
	 * @param valueType The map value class type.
	 * @return If the key and value types are Object, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public final <K,V,T extends Map<K,V>> ClassMeta<T> getMapClassMeta(Class<T> c, Class<K> keyType, Class<V> valueType) {
		return getMapClassMeta(c, getClassMeta(keyType), getClassMeta(valueType));
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Map} object.
	 *
	 * @param <T> The map class type.
	 * @param c The map class type.
	 * @param keyType The map key class type.
	 * @param valueType The map value class type.
	 * @return If the key and value types are Object, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public final <T extends Map> ClassMeta<T> getMapClassMeta(Class<T> c, Type keyType, Type valueType) {
		return getMapClassMeta(c, getClassMeta(keyType), getClassMeta(valueType));
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Collection} object.
	 *
	 * @param <E> The collection element class type.
	 * @param <T> The collection class type.
	 * @param c The collection class type.
	 * @param elementType The collection element class type.
	 * @return If the element type is <code>OBJECT</code>, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public final <E,T extends Collection<E>> ClassMeta<T> getCollectionClassMeta(Class<T> c, ClassMeta<E> elementType) {
		return ctx.getCollectionClassMeta(c, elementType);
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Collection} object.
	 *
	 * @param <E> The collection element class type.
	 * @param <T> The collection class type.
	 * @param c The collection class type.
	 * @param elementType The collection element class type.
	 * @return If the element type is <code>OBJECT</code>, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public final <E,T extends Collection<E>> ClassMeta<T> getCollectionClassMeta(Class<T> c, Class<E> elementType) {
		return getCollectionClassMeta(c, getClassMeta(elementType));
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Collection} object.
	 *
	 * @param <T> The collection class type.
	 * @param c The collection class type.
	 * @param elementType The collection element class type.
	 * @return If the element type is <code>OBJECT</code>, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public final <T extends Collection> ClassMeta<T> getCollectionClassMeta(Class<T> c, Type elementType) {
		return getCollectionClassMeta(c, getClassMeta(elementType));
	}

	/**
	 * Constructs a ClassMeta object given the specified object and parameters.
	 *
	 * @param o The parent class type.
	 * 	Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>{@link ClassMeta} object, which just returns the same object.
	 * 		<li>{@link Class} object (e.g. <code>String.<jk>class</jk></code>).
	 * 		<li>{@link Type} object (e.g. {@link ParameterizedType} or {@link GenericArrayType}.
	 * 		<li>Anything else is interpreted as {@code getClassMeta(o.getClass(), parameters);}
	 * 	</ul>
	 * @return A ClassMeta object, or <jk>null</jk> if the object is null.
	 */
	public final ClassMeta getClassMeta(Type o) {
		return ctx.getClassMeta(o, null);
	}

	/**
	 * Given an array of {@link Class} objects, returns an array of corresponding {@link ClassMeta} objects.
	 * Constructs a new array on each call.
	 *
	 * @param classes The array of classes to get class metas for.
	 * @return An array of {@link ClassMeta} objects corresponding to the classes.  Never <jk>null</jk>.
	 */
	public final ClassMeta<?>[] getClassMetas(Class<?>[] classes) {
		assertFieldNotNull(classes, "classes");
		ClassMeta<?>[] cm = new ClassMeta<?>[classes.length];
		for (int i = 0; i < classes.length; i++)
			cm[i] = getClassMeta(classes[i]);
		return cm;
	}

	/**
	 * Shortcut for calling {@code getClassMeta(o.getClass())}.
	 *
	 * @param <T> The class of the object being passed in.
	 * @param o The class to find the class type for.
	 * @return The ClassMeta object, or <jk>null</jk> if {@code o} is <jk>null</jk>.
	 */
	public final <T> ClassMeta<T> getClassMetaForObject(T o) {
		if (o == null)
			return null;
		return (ClassMeta<T>)getClassMeta(o.getClass());
	}

	/**
	 * Converts class name strings to ClassMeta objects.
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <ul>
	 * 	<li><js>"java.lang.String"</js>
	 * 	<li><js>"com.foo.sample.MyBean[]"</js>
	 * 	<li><js>"java.util.HashMap<java.lang.String,java.lang.Integer>"</js>
	 * 	<li><js>"[Ljava.lang.String;"</js> (i.e. the value of <code>String[].<jk>class</jk>.getName()</code>)
	 * </ul>
	 *
	 * @param s The class name.
	 * @return The ClassMeta corresponding to the class name string.
	 */
	public final ClassMeta<?> getClassMetaFromString(String s) {
		return ctx.getClassMetaFromString(s);
	}

	/**
	 * Returns the type property name as defined by {@link BeanContext#BEAN_beanTypePropertyName}.
	 *
	 * @return The type property name.  Never <jk>null</jk>.
	 */
	public final String getBeanTypePropertyName() {
		return ctx.beanTypePropertyName;
	}

	/**
	 * Returns the bean registry defined in this bean context defined by {@link BeanContext#BEAN_beanDictionary}.
	 *
	 * @return The bean registry defined in this bean context.  Never <jk>null</jk>.
	 */
	public final BeanRegistry getBeanRegistry() {
		return ctx.beanRegistry;
	}

	/**
	 * Creates a reusable {@link StringBuilder} object from an internal pool.
	 * <p>
	 * String builders are returned to the pool by calling {@link #returnStringBuilder(StringBuilder)}.
	 *
	 * @return A new or previously returned string builder.
	 */
	public final StringBuilder getStringBuilder() {
		if (sbStack.isEmpty())
			return new StringBuilder();
		return sbStack.pop();
	}

	/**
	 * Returns a {@link StringBuilder} object back into the internal reuse pool.
	 *
	 * @param sb The string builder to return to the pool.  No-op if <jk>null</jk>.
	 */
	public final void returnStringBuilder(StringBuilder sb) {
		if (sb == null)
			return;
		sb.setLength(0);
		sbStack.push(sb);
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>Object</code>.
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent "any object type" when an object type
	 * 	is not known.
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Object.<jk>class</jk>)</code> but uses
	 * 	a cached copy to avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>Object</code> class.
	 */
	public final ClassMeta<Object> object() {
		return ctx.cmObject;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>String</code>.
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent key types in maps.
	 * <p>
	 * This method is identical to calling <code>getClassMeta(String.<jk>class</jk>)</code> but uses
	 * 	a cached copy to avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>String</code> class.
	 */
	public final ClassMeta<String> string() {
		return ctx.cmString;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>Class</code>.
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent key types in maps.
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Class.<jk>class</jk>)</code> but uses
	 * 	a cached copy to avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>String</code> class.
	 */
	public final ClassMeta<Class> _class() {
		return ctx.cmClass;
	}

	/**
	 * Returns the classloader associated with this bean context.
	 *
	 * @return The classloader associated with this bean context.
	 */
	public final ClassLoader getClassLoader() {
		return ctx.classLoader;
	}

	/**
	 * Returns the media type specified for this session.
	 * <p>
	 * For example, <js>"application/json"</js>.
	 *
	 * @return The media type for this session, or <jk>null</jk> if not specified.
	 */
	public final MediaType getMediaType() {
		return mediaType;
	}

	@Override /* Session */
	public final ObjectMap asMap() {
		return super.asMap()
			.appendAll(ctx.asMap())
			.append("BeanSession", new ObjectMap()
				.append("locale", locale)
				.append("timeZone", timeZone)
			);
	}

	@Override /* Session */
	public boolean close() throws BeanRuntimeException {
		if (super.close()) {
			if (debug && hasWarnings())
				throw new BeanRuntimeException("Warnings occurred in session: \n" + StringUtils.join(getWarnings(), "\n"));
			return true;
		}
		return false;
	}
}
