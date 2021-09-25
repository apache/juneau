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
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.text.*;
import java.time.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import javax.xml.bind.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Session object that lives for the duration of a single use of {@link Serializer} or {@link Parser}.
 *
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BeanSession extends Session {

	private static Logger LOG = Logger.getLogger(BeanSession.class.getName());

	private final BeanContext ctx;
	private final HttpPartSchema schema;
	private final Stack<StringBuilder> sbStack;
	private final Locale locale;
	private final TimeZone timeZone;
	private final MediaType mediaType;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected BeanSession(BeanContext ctx, BeanSessionArgs args) {
		super(ctx, args);
		this.ctx = ctx;
		schema = args.schema;
		sbStack = args.unmodifiable ? null : new Stack<>();
		SessionProperties sp = args.properties;
		locale = sp.get(BEAN_locale, Locale.class).orElse(ctx.getDefaultLocale());
		timeZone = sp.get(BEAN_timeZone, TimeZone.class).orElse(ctx.getDefaultTimeZone());
		mediaType = sp.get(BEAN_mediaType, MediaType.class).orElse(ctx.getDefaultMediaType());
	}

	/**
	 * Converts the specified value to the specified class type.
	 *
	 * <p>
	 * See {@link #convertToType(Object, ClassMeta)} for the list of valid conversions.
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
		return convertToMemberType(null, value, getClassMeta(type));
	}

	/**
	 * Same as {@link #convertToType(Object, Class)}, except used for instantiating inner member classes that must
	 * be instantiated within another class instance.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param outer
	 * 	If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public final <T> T convertToMemberType(Object outer, Object value, Class<T> type) throws InvalidDataConversionException {
		return convertToMemberType(outer, value, getClassMeta(type));
	}

	/**
	 * Casts the specified value into the specified type.
	 *
	 * <p>
	 * If the value isn't an instance of the specified type, then converts the value if possible.
	 *
	 * <p>
	 * The following conversions are valid:
	 * <table class='styled'>
	 * 	<tr><th>Convert to type</th><th>Valid input value types</th><th>Notes</th></tr>
	 * 	<tr>
	 * 		<td>
	 * 			A class that is the normal type of a registered {@link PojoSwap}.
	 * 		</td>
	 * 		<td>
	 * 			A value whose class matches the transformed type of that registered {@link PojoSwap}.
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			A class that is the transformed type of a registered {@link PojoSwap}.
	 * 		</td>
	 * 		<td>
	 * 			A value whose class matches the normal type of that registered {@link PojoSwap}.
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code Number} (e.g. {@code Integer}, {@code Short}, {@code Float},...)
	 * 			<br><code>Number.<jsf>TYPE</jsf></code> (e.g. <code>Integer.<jsf>TYPE</jsf></code>,
	 * 			<code>Short.<jsf>TYPE</jsf></code>, <code>Float.<jsf>TYPE</jsf></code>,...)
	 * 		</td>
	 * 		<td>
	 * 			{@code Number}, {@code String}, <jk>null</jk>
	 * 		</td>
	 * 		<td>
	 * 			For primitive {@code TYPES}, <jk>null</jk> returns the JVM default value for that type.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code Map} (e.g. {@code Map}, {@code HashMap}, {@code TreeMap}, {@code OMap})
	 * 		</td>
	 * 		<td>
	 * 			{@code Map}
	 * 		</td>
	 * 		<td>
	 * 			If {@code Map} is not constructible, an {@code OMap} is created.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 		{@code Collection} (e.g. {@code List}, {@code LinkedList}, {@code HashSet}, {@code OList})
	 * 		</td>
	 * 		<td>
	 * 			{@code Collection<Object>}
	 * 			<br>{@code Object[]}
	 * 		</td>
	 * 		<td>
	 * 			If {@code Collection} is not constructible, an {@code OList} is created.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code X[]} (array of any type X)
	 * 		</td>
	 * 		<td>
	 * 			{@code List<X>}
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code X[][]} (multi-dimensional arrays)
	 * 		</td>
	 * 		<td>
	 * 			{@code List<List<X>>}
	 * 			<br>{@code List<X[]>}
	 * 			<br>{@code List[]<X>}
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code Enum}
	 * 		</td>
	 * 		<td>
	 * 			{@code String}
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			Bean
	 * 		</td>
	 * 		<td>
	 * 			{@code Map}
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code String}
	 * 		</td>
	 * 		<td>
	 * 			Anything
	 * 		</td>
	 * 		<td>
	 * 			Arrays are converted to JSON arrays
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			Anything with one of the following methods:
	 * 			<br><code><jk>public static</jk> T fromString(String)</code>
	 * 			<br><code><jk>public static</jk> T valueOf(String)</code>
	 * 			<br><code><jk>public</jk> T(String)</code>
	 * 		</td>
	 * 		<td>
	 * 			<c>String</c>
	 * 		</td>
	 * 		<td>
	 * 			<br>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to be converted.
	 * @param type The target object type.
	 * @return The converted type.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 */
	public final <T> T convertToType(Object value, ClassMeta<T> type) throws InvalidDataConversionException {
		return convertToMemberType(null, value, type);
	}

	/**
	 * Same as {@link #convertToType(Object, Class)}, but allows for complex data types consisting of collections or maps.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to be converted.
	 * @param type The target object type.
	 * @param args The target object parameter types.
	 * @return The converted type.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 */
	public final <T> T convertToType(Object value, Type type, Type...args) throws InvalidDataConversionException {
		return (T)convertToMemberType(null, value, getClassMeta(type, args));
	}

	/**
	 * Same as {@link #convertToType(Object, ClassMeta)}, except used for instantiating inner member classes that must
	 * be instantiated within another class instance.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param outer
	 * 	If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @param value The value to convert.
	 * @param to The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	protected final <T> T convertToMemberType(Object outer, Object value, ClassMeta<T> to) throws InvalidDataConversionException {
		if (to == null)
			to = (ClassMeta<T>)object();

		try {
			// Handle the case of a null value.
			if (value == null) {

				// If it's a primitive, then use the converters to get the default value for the primitive type.
				if (to.isPrimitive())
					return to.getPrimitiveDefault();

				// Otherwise, just return null.
				return to.isOptional() ? (T)to.getOptionalDefault() : null;
			}

			if (to.isOptional() && (! (value instanceof Optional)))
				return (T) Optional.ofNullable(convertToMemberType(outer, value, to.getElementType()));

			Class<T> tc = to.getInnerClass();

			// If no conversion needed, then just return the value.
			// Don't include maps or collections, because child elements may need conversion.
			if (tc.isInstance(value))
				if (! ((to.isMap() && to.getValueType().isNotObject()) || ((to.isCollection() || to.isOptional()) && to.getElementType().isNotObject())))
					return (T)value;

			PojoSwap swap = to.getSwap(this);
			if (swap != null) {
				ClassInfo nc = swap.getNormalClass(), fc = swap.getSwapClass();
				if (nc.isParentOf(tc) && fc.isParentOf(value.getClass()))
					return (T)swap.unswap(this, value, to);
				ClassMeta fcm = getClassMeta(fc.inner());
				if (fcm.isNumber() && value instanceof Number) {
					value = convertToMemberType(null, value, fc.inner());
					return (T)swap.unswap(this, value, to);
				}
			}

			ClassMeta<?> from = getClassMetaForObject(value);
			swap = from.getSwap(this);
			if (swap != null) {
				ClassInfo nc = swap.getNormalClass(), fc = swap.getSwapClass();
				if (nc.isParentOf(from.getInnerClass()) && fc.isParentOf(tc))
					return (T)swap.swap(this, value);
			}

			if (to.isPrimitive()) {
				if (to.isNumber()) {
					if (from.isNumber()) {
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
					} else if (from.isBoolean()) {
						Boolean b = (Boolean)value;
						if (tc == Integer.TYPE)
							return (T)(Integer.valueOf(b ? 1 : 0));
						if (tc == Short.TYPE)
							return (T)(Short.valueOf(b ? (short)1 : 0));
						if (tc == Long.TYPE)
							return (T)(Long.valueOf(b ? 1l : 0));
						if (tc == Float.TYPE)
							return (T)(Float.valueOf(b ? 1f : 0));
						if (tc == Double.TYPE)
							return (T)(Double.valueOf(b ? 1d : 0));
						if (tc == Byte.TYPE)
							return (T)(Byte.valueOf(b ? (byte)1 : 0));
					} else if (isNullOrEmpty(value)) {
						return (T)to.info.getPrimitiveDefault();
					} else {
						String s = value.toString();
						int multiplier = (tc == Integer.TYPE || tc == Short.TYPE || tc == Long.TYPE) ? getMultiplier(s) : 1;
						if (multiplier != 1) {
							s = s.substring(0, s.length()-1).trim();
							Long l = Long.valueOf(s) * multiplier;
							if (tc == Integer.TYPE)
								return (T)Integer.valueOf(l.intValue());
							if (tc == Short.TYPE)
								return (T)Short.valueOf(l.shortValue());
							if (tc == Long.TYPE)
								return (T)Long.valueOf(l.longValue());
						} else {
							if (tc == Integer.TYPE)
								return (T)Integer.valueOf(s);
							if (tc == Short.TYPE)
								return (T)Short.valueOf(s);
							if (tc == Long.TYPE)
								return (T)Long.valueOf(s);
							if (tc == Float.TYPE)
								return (T)new Float(s);
							if (tc == Double.TYPE)
								return (T)new Double(s);
							if (tc == Byte.TYPE)
								return (T)Byte.valueOf(s);
						}
					}
				} else if (to.isChar()) {
					if (isNullOrEmpty(value))
						return (T)to.info.getPrimitiveDefault();
					return (T)parseCharacter(value);
				} else if (to.isBoolean()) {
					if (from.isNumber()) {
						int i = ((Number)value).intValue();
						return (T)(i == 0 ? Boolean.FALSE : Boolean.TRUE);
					} else if (isNullOrEmpty(value)) {
						return (T)to.info.getPrimitiveDefault();
					} else {
						return (T)Boolean.valueOf(value.toString());
					}
				}
			}

			if (to.isNumber()) {
				if (from.isNumber()) {
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
					if (tc == AtomicInteger.class)
						return (T)new AtomicInteger(n.intValue());
					if (tc == AtomicLong.class)
						return (T)new AtomicLong(n.intValue());
				} else if (from.isBoolean()) {
					Boolean b = (Boolean)value;
					if (tc == Integer.class)
						return (T)Integer.valueOf(b ? 1 : 0);
					if (tc == Short.class)
						return (T)Short.valueOf(b ? (short)1 : 0);
					if (tc == Long.class)
						return (T)Long.valueOf(b ? 1 : 0);
					if (tc == Float.class)
						return (T)Float.valueOf(b ? 1 : 0);
					if (tc == Double.class)
						return (T)Double.valueOf(b ? 1 : 0);
					if (tc == Byte.class)
						return (T)Byte.valueOf(b ? (byte)1 : 0);
					if (tc == AtomicInteger.class)
						return (T)new AtomicInteger(b ? 1 : 0);
					if (tc == AtomicLong.class)
						return (T)new AtomicLong(b ? 1 : 0);
				} else if (isNullOrEmpty(value)) {
					return null;
				} else if (! hasMutater(from, to)) {
					String s = value.toString();

					int multiplier = (tc == Integer.class || tc == Short.class || tc == Long.class) ? getMultiplier(s) : 1;
					if (multiplier != 1) {
						s = s.substring(0, s.length()-1).trim();
						Long l = Long.valueOf(s) * multiplier;
						if (tc == Integer.TYPE)
							return (T)Integer.valueOf(l.intValue());
						if (tc == Short.TYPE)
							return (T)Short.valueOf(l.shortValue());
						if (tc == Long.TYPE)
							return (T)Long.valueOf(l.longValue());
					} else {
						if (tc == Integer.class)
							return (T)Integer.valueOf(s);
						if (tc == Short.class)
							return (T)Short.valueOf(s);
						if (tc == Long.class)
							return (T)Long.valueOf(s);
						if (tc == Float.class)
							return (T)new Float(s);
						if (tc == Double.class)
							return (T)new Double(s);
						if (tc == Byte.class)
							return (T)Byte.valueOf(s);
						if (tc == AtomicInteger.class)
							return (T)new AtomicInteger(Integer.valueOf(s));
						if (tc == AtomicLong.class)
							return (T)new AtomicLong(Long.valueOf(s));
						if (tc == Number.class)
							return (T)StringUtils.parseNumber(s, Number.class);
					}
				}
			}

			if (to.isChar()) {
				if (isNullOrEmpty(value))
					return null;
				String s = value.toString();
				if (s.length() == 1)
					return (T)Character.valueOf(s.charAt(0));
			}

			if (to.isByteArray()) {
				if (from.isInputStream())
					return (T)readBytes((InputStream)value);
				if (from.isReader())
					return (T)read((Reader)value).getBytes();
				if (to.hasMutaterFrom(from))
					return to.mutateFrom(value);
				if (from.hasMutaterTo(to))
					return from.mutateTo(value, to);
				return (T) value.toString().getBytes(Charset.forName("UTF-8"));
			}

			// Handle setting of array properties
			if (to.isArray()) {
				if (from.isCollection())
					return (T)toArray(to, (Collection)value);
				else if (from.isArray())
					return (T)toArray(to, Arrays.asList((Object[])value));
				else if (startsWith(value.toString(), '['))
					return (T)toArray(to, OList.ofJson(value.toString()).setBeanSession(this));
				else if (to.hasMutaterFrom(from))
					return to.mutateFrom(value);
				else if (from.hasMutaterTo(to))
					return from.mutateTo(value, to);
				else
					return (T)toArray(to, new OList((Object[])StringUtils.split(value.toString())).setBeanSession(this));
			}

			// Target type is some sort of Map that needs to be converted.
			if (to.isMap()) {
				try {
					if (from.isMap()) {
						Map m = to.canCreateNewInstance(outer) ? (Map)to.newInstance(outer) : newGenericMap(to);
						ClassMeta keyType = to.getKeyType(), valueType = to.getValueType();
						for (Map.Entry e : (Set<Map.Entry>)((Map)value).entrySet()) {
							Object k = e.getKey();
							if (keyType.isNotObject()) {
								if (keyType.isString() && k.getClass() != Class.class)
									k = k.toString();
								else
									k = convertToMemberType(m, k, keyType);
							}
							Object v = e.getValue();
							if (valueType.isNotObject())
								v = convertToMemberType(m, v, valueType);
							m.put(k, v);
						}
						return (T)m;
					} else if (!to.canCreateNewInstanceFromString(outer)) {
						OMap m = OMap.ofJson(value.toString());
						m.setBeanSession(this);
						return convertToMemberType(outer, m, to);
					}
				} catch (Exception e) {
					throw new InvalidDataConversionException(value.getClass(), to, e);
				}
			}

			// Target type is some sort of Collection
			if (to.isCollection()) {
				try {
					Collection l = to.canCreateNewInstance(outer) ? (Collection)to.newInstance(outer) : to.isSet() ? new LinkedHashSet<>() : new OList(this);
					ClassMeta elementType = to.getElementType();

					if (from.isArray())
						for (Object o : (Object[])value)
							l.add(elementType.isObject() ? o : convertToMemberType(l, o, elementType));
					else if (from.isCollection())
						for (Object o : (Collection)value)
							l.add(elementType.isObject() ? o : convertToMemberType(l, o, elementType));
					else if (from.isMap())
						l.add(elementType.isObject() ? value : convertToMemberType(l, value, elementType));
					else if (isNullOrEmpty(value))
						return null;
					else if (from.isString()) {
						String s = value.toString();
						if (isJsonArray(s, false)) {
							OList l2 = OList.ofJson(s);
							l2.setBeanSession(this);
							for (Object o : l2)
								l.add(elementType.isObject() ? o : convertToMemberType(l, o, elementType));
						} else {
							throw new InvalidDataConversionException(value.getClass(), to, null);
						}
					}
					else
						throw new InvalidDataConversionException(value.getClass(), to, null);
					return (T)l;
				} catch (InvalidDataConversionException e) {
					throw e;
				} catch (Exception e) {
					throw new InvalidDataConversionException(value.getClass(), to, e);
				}
			}

			if (to.isEnum()) {
				if (to.canCreateNewInstanceFromString(outer))
					return to.newInstanceFromString(outer, value.toString());
				if (isNullOrEmpty(value))
					return null;
				return (T)Enum.valueOf((Class<? extends Enum>)tc, value.toString());
			}

			if (to.isString()) {
				if (from.isByteArray()) {
					return (T) new String((byte[])value);
				} else if (from.isMapOrBean() || from.isCollectionOrArrayOrOptional()) {
					WriterSerializer ws = ctx.getBeanToStringSerializer();
					if (ws != null)
						return (T)ws.serialize(value);
				} else if (from.isClass()) {
					return (T)((Class<?>)value).getName();
				}
				return (T)value.toString();
			}

			if (to.isCharSequence()) {
				Class<?> c = value.getClass();
				if (c.isArray()) {
					if (c.getComponentType().isPrimitive()) {
						OList l = new OList(this);
						int size = Array.getLength(value);
						for (int i = 0; i < size; i++)
							l.add(Array.get(value, i));
						value = l;
					}
					value = new OList((Object[])value).setBeanSession(this);
				}

				return to.newInstanceFromString(outer, value.toString());
			}

			if (to.isBoolean()) {
				if (from.isNumber())
					return (T)(Boolean.valueOf(((Number)value).intValue() != 0));
				if (isNullOrEmpty(value))
					return null;
				if (! hasMutater(from, to))
					return (T)Boolean.valueOf(value.toString());
			}

			// It's a bean being initialized with a Map
			if (to.isBean() && value instanceof Map) {
				BuilderSwap<T,Object> builder = (BuilderSwap<T,Object>)to.getBuilderSwap(this);

				if (value instanceof OMap && builder == null) {
					OMap m2 = (OMap)value;
					String typeName = m2.getString(getBeanTypePropertyName(to));
					if (typeName != null) {
						ClassMeta cm = to.getBeanRegistry().getClassMeta(typeName);
						if (cm != null && to.info.isParentOf(cm.innerClass))
							return (T)m2.cast(cm);
					}
				}
				if (builder != null) {
					BeanMap m = toBeanMap(builder.create(this, to));
					m.load((Map<?,?>) value);
					return builder.build(this, m.getBean(), to);
				}
				return newBeanMap(tc).load((Map<?,?>) value).getBean();
			}

			if (to.isInputStream()) {
				if (from.isByteArray()) {
					byte[] b = (byte[])value;
					return (T) new ByteArrayInputStream(b, 0, b.length);
				}
				byte[] b = value.toString().getBytes();
				return (T)new ByteArrayInputStream(b, 0, b.length);
			}

			if (to.isReader()) {
				if (from.isByteArray()) {
					byte[] b = (byte[])value;
					return (T) new StringReader(new String(b));
				}
				return (T)new StringReader(value.toString());
			}

			if (to.isCalendar()) {
				if (from.isCalendar()) {
					Calendar c = (Calendar)value;
					if (value instanceof GregorianCalendar) {
						GregorianCalendar c2 = new GregorianCalendar(c.getTimeZone());
						c2.setTime(c.getTime());
						return (T)c2;
					}
				}
				if (from.isDate()) {
					Date d = (Date)value;
					if (value instanceof GregorianCalendar) {
						GregorianCalendar c2 = new GregorianCalendar(TimeZone.getDefault());
						c2.setTime(d);
						return (T)c2;
					}
				}
				return (T)DatatypeConverter.parseDateTime(DateUtils.toValidISO8601DT(value.toString()));
			}

			if (to.isDate() && to.getInnerClass() == Date.class) {
				if (from.isCalendar())
					return (T)((Calendar)value).getTime();
				return (T)DatatypeConverter.parseDateTime(DateUtils.toValidISO8601DT(value.toString())).getTime();
			}

			if (to.hasMutaterFrom(from))
				return to.mutateFrom(value);

			if (from.hasMutaterTo(to))
				return from.mutateTo(value, to);

			if (to.isBean())
				return newBeanMap(to.getInnerClass()).load(value.toString()).getBean();

			if (to.canCreateNewInstanceFromString(outer))
				return to.newInstanceFromString(outer, value.toString());

		} catch (Exception e) {
			throw new InvalidDataConversionException(value, to, e);
		}

		throw new InvalidDataConversionException(value, to, null);
	}

	private static boolean hasMutater(ClassMeta<?> from, ClassMeta<?> to) {
		return to.hasMutaterFrom(from) || from.hasMutaterTo(to);
	}

	private static final boolean isNullOrEmpty(Object o) {
		return o == null || o.toString().equals("") || o.toString().equals("null");
	}

	private static int getMultiplier(String s) {
		if (s.endsWith("G"))
			return 1024*1024*1024;
		if (s.endsWith("M"))
			return 1024*1024;
		if (s.endsWith("K"))
			return 1024;
		return 1;
	}

	/**
	 * Converts the contents of the specified list into an array.
	 *
	 * <p>
	 * Works on both object and primitive arrays.
	 *
	 * <p>
	 * In the case of multi-dimensional arrays, the incoming list must contain elements of type n-1 dimension.
	 * i.e. if {@code type} is <code><jk>int</jk>[][]</code> then {@code list} must have entries of type
	 * <code><jk>int</jk>[]</code>.
	 *
	 * @param type The type to convert to.  Must be an array type.
	 * @param list The contents to populate the array with.
	 * @return A new object or primitive array.
	 */
	protected final Object toArray(ClassMeta<?> type, Collection<?> list) {
		if (list == null)
			return null;
		ClassMeta<?> componentType = type.isArgs() ? object() : type.getElementType();
		Object array = Array.newInstance(componentType.getInnerClass(), list.size());
		int i = 0;
		for (Object o : list) {
			if (! type.getInnerClass().isInstance(o)) {
				if (componentType.isArray() && o instanceof Collection)
					o = toArray(componentType, (Collection<?>)o);
				else if (o == null && componentType.isPrimitive())
					o = componentType.getPrimitiveDefault();
				else
					o = convertToType(o, componentType);
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
	 * Wraps an object inside a {@link BeanMap} object (a modifiable {@link Map}).
	 *
	 * <p>
	 * If object is not a true bean, then throws a {@link BeanRuntimeException} with an explanation of why it's not a
	 * bean.
	 *
	 * <p>
	 * If object is already a {@link BeanMap}, simply returns the same object.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a bean map around a bean instance</jc>
	 * 	BeanMap&lt;Person&gt; bm = BeanContext.<jsf>DEFAULT</jsf>.forBean(<jk>new</jk> Person());
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param o The object to wrap in a map interface.  Must not be null.
	 * @return The wrapped object.
	 */
	public final <T> BeanMap<T> toBeanMap(T o) {
		if (o instanceof BeanMap)
			return (BeanMap<T>)o;
		return this.toBeanMap(o, (Class<T>)o.getClass());
	}

	/**
	 * Wraps an object inside a {@link BeanMap} object (a modifiable {@link Map}).
	 *
	 * <p>
	 * Same as {@link #toBeanMap(Object)} but allows you to specify a property namer instance.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Construct a bean map around a bean instance</jc>
	 * 	BeanMap&lt;Person&gt; bm = BeanContext.<jsf>DEFAULT</jsf>.toBeanMap(<jk>new</jk> Person(), PropertyNamerDLC.<jsf>INSTANCE</jsf>);
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param o The object to wrap in a map interface.  Must not be null.
	 * @param propertyNamer The property namer to use.
	 * @return The wrapped object.
	 */
	public final <T> BeanMap<T> toBeanMap(T o, PropertyNamer propertyNamer) {
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
	 * Wraps an object inside a {@link BeanMap} object (i.e.: a modifiable {@link Map}) defined as a bean for one of its
	 * class, a super class, or an implemented interface.
	 *
	 * <p>
	 * If object is not a true bean, throws a {@link BeanRuntimeException} with an explanation of why it's not a bean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * @throws
	 * 	BeanRuntimeException If specified object is not a bean according to the bean rules specified in this context
	 * class.
	 */
	public final <T> BeanMap<T> toBeanMap(T o, Class<? super T> c) throws BeanRuntimeException {
		assertArgNotNull("o", o);
		assertArgNotNull("c", c);
		assertArg(c.isInstance(o), "The specified object is not an instance of the specified class.  class=''{0}'', objectClass=''{1}'', object=''{2}''", className(c), className(o), 0);

		ClassMeta cm = getClassMeta(c);

		BeanMeta m = cm.getBeanMeta();
		if (m == null)
			throw new BeanRuntimeException(c, "Class is not a bean.  Reason=''{0}''", cm.getNotABeanReason());
		return new BeanMap<>(this, o, m);
	}

	/**
	 * Creates a new {@link BeanMap} object (a modifiable {@link Map}) of the given class with uninitialized
	 * property values.
	 *
	 * <p>
	 * If object is not a true bean, then throws a {@link BeanRuntimeException} with an explanation of why it's not a
	 * bean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * Same as {@link #newBeanMap(Class)}, except used for instantiating inner member classes that must be instantiated
	 * within another class instance.
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param c The name of the class to create a new instance of.
	 * @param outer
	 * 	If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @return A new instance of the class.
	 */
	public final <T> BeanMap<T> newBeanMap(Object outer, Class<T> c) {
		BeanMeta m = getBeanMeta(c);
		if (m == null)
			return null;
		T bean = null;
		if (m.constructorArgs.length == 0)
			bean = newBean(outer, c);
		return new BeanMap<>(this, bean, m);
	}

	/**
	 * Creates a new empty bean of the specified type, except used for instantiating inner member classes that must
	 * be instantiated within another class instance.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
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
	 * Same as {@link #newBean(Class)}, except used for instantiating inner member classes that must be instantiated
	 * within another class instance.
	 *
	 * @param <T> The class type of the bean being created.
	 * @param c The class type of the bean being created.
	 * @param outer
	 * 	If class is a member class, this is the instance of the containing class.
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
			if (o == null)
				throw new BeanRuntimeException(c, "Class does not have a no-arg constructor.");
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
	 * @return
	 * 	The {@link BeanMeta} for the specified class, or <jk>null</jk> if the class
	 * 	is not a bean per the settings on this context.
	 */
	public final <T> BeanMeta<T> getBeanMeta(Class<T> c) {
		if (c == null)
			return null;
		return getClassMeta(c).getBeanMeta();
	}

	/**
	 * Returns a {@code ClassMeta} wrapper around a {@link Class} object.
	 *
	 * @param <T> The class type being wrapped.
	 * @param c The class being wrapped.
	 * @return The class meta object containing information about the class.
	 */
	public final <T> ClassMeta<T> getClassMeta(Class<T> c) {
		return ctx.getClassMeta(c);
	}

	/**
	 * Used to resolve <c>ClassMetas</c> of type <c>Collection</c> and <c>Map</c> that have
	 * <c>ClassMeta</c> values that themselves could be collections or maps.
	 *
	 * <p>
	 * <c>Collection</c> meta objects are assumed to be followed by zero or one meta objects indicating the
	 * element type.
	 *
	 * <p>
	 * <c>Map</c> meta objects are assumed to be followed by zero or two meta objects indicating the key and value
	 * types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><code>getClassMeta(String.<jk>class</jk>);</code> - A normal type.
	 * 	<li><code>getClassMeta(List.<jk>class</jk>);</code> - A list containing objects.
	 * 	<li><code>getClassMeta(List.<jk>class</jk>, String.<jk>class</jk>);</code> - A list containing strings.
	 * 	<li><code>getClassMeta(LinkedList.<jk>class</jk>, String.<jk>class</jk>);</code> - A linked-list containing
	 * 		strings.
	 * 	<li><code>getClassMeta(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);</code> -
	 * 		A linked-list containing linked-lists of strings.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>);</code> - A map containing object keys/values.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);</code> - A map
	 * 		containing string keys/values.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);</code> -
	 * 		A map containing string keys and values of lists containing beans.
	 * </ul>
	 *
	 * @param type
	 * 	The class to resolve.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The class meta.
	 */
	public final <T> ClassMeta<T> getClassMeta(Type type, Type...args) {
		return ctx.getClassMeta(type, args);
	}

	/**
	 * Given an array of {@link Type} objects, returns a {@link ClassMeta} representing those arguments.
	 *
	 * <p>
	 * Constructs a new meta on each call.
	 *
	 * @param classes The array of classes to get class metas for.
	 * @return The args {@link ClassMeta} object corresponding to the classes.  Never <jk>null</jk>.
	 */
	protected final ClassMeta<Object[]> getArgsClassMeta(Type[] classes) {
		assertArgNotNull("classes", classes);
		ClassMeta[] cm = new ClassMeta<?>[classes.length];
		for (int i = 0; i < classes.length; i++)
			cm[i] = getClassMeta(classes[i]);
		return new ClassMeta(cm);
	}

	/**
	 * Shortcut for calling {@code getClassMeta(o.getClass())}.
	 *
	 * @param <T> The class of the object being passed in.
	 * @param o The class to find the class type for.
	 * @return The ClassMeta object, or <jk>null</jk> if {@code o} is <jk>null</jk>.
	 */
	public final <T> ClassMeta<T> getClassMetaForObject(T o) {
		return (ClassMeta<T>)getClassMetaForObject(o, null);
	}

	/**
	 * Shortcut for calling {@code getClassMeta(o.getClass())} but returns a default value if object is <jk>null</jk>.
	 *
	 * @param o The class to find the class type for.
	 * @param def The default {@link ClassMeta} if the object is null.
	 * @return The ClassMeta object, or the default value if {@code o} is <jk>null</jk>.
	 */
	protected final ClassMeta<?> getClassMetaForObject(Object o, ClassMeta<?> def) {
		if (o == null)
			return def;
		return getClassMeta(o.getClass());
	}

	/**
	 * Configuration property:  Locale.
	 *
	 * <p>
	 * The locale is determined in the following order:
	 * <ol>
	 * 	<li><c>locale</c> parameter passed in through constructor.
	 * 	<li>{@link BeanContext#BEAN_locale} entry in parameter passed in through constructor.
	 * 	<li>{@link BeanContext#BEAN_locale} setting on bean context.
	 * 	<li>Locale returned by {@link Locale#getDefault()}.
	 * </ol>
	 *
	 * @see BeanContext#BEAN_locale
	 * @return The session locale.
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Configuration property:  Media type.
	 *
	 * <p>
	 * For example, <js>"application/json"</js>.
	 *
	 * @see BeanContext#BEAN_mediaType
	 * @return The media type for this session, or <jk>null</jk> if not specified.
	 */
	public final MediaType getMediaType() {
		return mediaType;
	}

	/**
	 * Returns the type property name as defined by {@link BeanContext#BEAN_typePropertyName}.
	 *
	 * @param cm
	 * 	The class meta of the type we're trying to resolve the type name for.
	 * 	Can be <jk>null</jk>.
	 * @return The type property name.  Never <jk>null</jk>.
	 */
	public final String getBeanTypePropertyName(ClassMeta cm) {
		String s = cm == null ? null : cm.getBeanTypePropertyName();
		return s == null ? getBeanTypePropertyName() : s;
	}

	/**
	 * Returns the name property name.
	 *
	 * <p>
	 * Currently this always returns <js>"_name"</js>.
	 *
	 * @return The name property name.  Never <jk>null</jk>.
	 */
	public final String getNamePropertyName() {
		return "_name";
	}

	/**
	 * Returns the bean registry defined in this bean context defined by {@link BeanContext#BEAN_beanDictionary}.
	 *
	 * @return The bean registry defined in this bean context.  Never <jk>null</jk>.
	 */
	public final BeanRegistry getBeanRegistry() {
		return ctx.getBeanRegistry();
	}

	/**
	 * Creates a reusable {@link StringBuilder} object from an internal pool.
	 *
	 * <p>
	 * String builders are returned to the pool by calling {@link #returnStringBuilder(StringBuilder)}.
	 *
	 * @return A new or previously returned string builder.
	 */
	protected final StringBuilder getStringBuilder() {
		if (sbStack == null || sbStack.isEmpty())
			return new StringBuilder();
		return sbStack.pop();
	}

	/**
	 * Returns a {@link StringBuilder} object back into the internal reuse pool.
	 *
	 * @param sb The string builder to return to the pool.  No-op if <jk>null</jk>.
	 */
	protected final void returnStringBuilder(StringBuilder sb) {
		if (sb == null)
			return;
		sb.setLength(0);
		if (sbStack != null)
			sbStack.push(sb);
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <c>Object</c>.
	 *
	 * <p>
	 * This <c>ClassMeta</c> is often used to represent "any object type" when an object type is not known.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Object.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <c>Object</c> class.
	 */
	public final ClassMeta<Object> object() {
		return ctx.object();
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <c>String</c>.
	 *
	 * <p>
	 * This <c>ClassMeta</c> is often used to represent key types in maps.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(String.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <c>String</c> class.
	 */
	public final ClassMeta<String> string() {
		return ctx.string();
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <c>Class</c>.
	 *
	 * <p>
	 * This <c>ClassMeta</c> is often used to represent key types in maps.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Class.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <c>String</c> class.
	 */
	public final ClassMeta<Class> _class() {
		return ctx._class();
	}

	/**
	 * Creates either an {@link OMap} or {@link AMap} depending on whether the key type is
	 * String or something else.
	 *
	 * @param mapMeta The metadata of the map to create.
	 * @return A new map.
	 */
	protected Map newGenericMap(ClassMeta mapMeta) {
		ClassMeta<?> k = mapMeta.getKeyType();
		return (k == null || k.isString()) ? new OMap(this) : new AMap();
	}

	/**
	 * Logs a warning message.
	 *
	 * @param msg The warning message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	@Override
	public void addWarning(String msg, Object... args) {
		if (isDebug())
			LOG.log(Level.WARNING, ()->args.length == 0 ? msg : MessageFormat.format(msg, args));
		super.addWarning(msg, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Minimum bean class visibility.
	 *
	 * @see BeanContextBuilder#beanClassVisibility(Visibility)
	 * @return
	 * 	Classes are not considered beans unless they meet the minimum visibility requirements.
	 */
	public final Visibility getBeanClassVisibility() {
		return ctx.getBeanClassVisibility();
	}

	/**
	 * Configuration property:  Minimum bean constructor visibility.
	 *
	 * @see BeanContextBuilder#beanConstructorVisibility(Visibility)
	 * @return
	 * 	Only look for constructors with this specified minimum visibility.
	 */
	public final Visibility getBeanConstructorVisibility() {
		return ctx.getBeanConstructorVisibility();
	}

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 * @see BeanContext#BEAN_beanDictionary
	 * @return
	 * 	The list of classes that make up the bean dictionary in this bean context.
	 */
	// TODO - Rename to getBeanDictionary()
	public final List<Class<?>> getBeanDictionaryClasses() {
		return ctx.getBeanDictionaryClasses();
	}

	/**
	 * Configuration property:  Minimum bean field visibility.
	 *
	 *
	 * @see BeanContextBuilder#beanFieldVisibility(Visibility)
	 * @return
	 * 	Only look for bean fields with this specified minimum visibility.
	 */
	public final Visibility getBeanFieldVisibility() {
		return ctx.getBeanFieldVisibility();
	}

	/**
	 * Configuration property:  BeanMap.put() returns old property value.
	 *
	 * @see BeanContext#BEAN_beanMapPutReturnsOldValue
	 * @return
	 * 	<jk>true</jk> if the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property values.
	 * 	<br>Otherwise, it returns <jk>null</jk>.
	 */
	public final boolean isBeanMapPutReturnsOldValue() {
		return ctx.isBeanMapPutReturnsOldValue();
	}

	/**
	 * Configuration property:  Minimum bean method visibility.
	 *
	 * @see BeanContextBuilder#beanMethodVisibility(Visibility)
	 * @return
	 * 	Only look for bean methods with this specified minimum visibility.
	 */
	public final Visibility getBeanMethodVisibility() {
		return ctx.getBeanMethodVisibility();
	}

	/**
	 * Configuration property:  Beans require no-arg constructors.
	 *
	 * @see BeanContext#BEAN_beansRequireDefaultConstructor
	 * @return
	 * 	<jk>true</jk> if a Java class must implement a default no-arg constructor to be considered a bean.
	 * 	<br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireDefaultConstructor() {
		return ctx.isBeansRequireDefaultConstructor();
	}

	/**
	 * Configuration property:  Beans require Serializable interface.
	 *
	 * @see BeanContext#BEAN_beansRequireSerializable
	 * @return
	 * 	<jk>true</jk> if a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * 	<br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireSerializable() {
		return ctx.isBeansRequireSerializable();
	}

	/**
	 * Configuration property:  Beans require setters for getters.
	 *
	 * @see BeanContext#BEAN_beansRequireSettersForGetters
	 * @return
	 * 	<jk>true</jk> if only getters that have equivalent setters will be considered as properties on a bean.
	 * 	<br>Otherwise, they are ignored.
	 */
	public final boolean isBeansRequireSettersForGetters() {
		return ctx.isBeansRequireSettersForGetters();
	}

	/**
	 * Configuration property:  Beans require at least one property.
	 *
	 * @see BeanContext#BEAN_disableBeansRequireSomeProperties
	 * @return
	 * 	<jk>true</jk> if a Java class doesn't need to contain at least 1 property to be considered a bean.
	 * 	<br>Otherwise, the bean is serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireSomeProperties() {
		return ctx.isBeansRequireSomeProperties();
	}

	/**
	 * Configuration property:  Bean type property name.
	 *
	 * @see BeanContext#BEAN_typePropertyName
	 * @return
	 * 	The name of the bean property used to store the dictionary name of a bean type so that the parser knows the data type to reconstruct.
	 */
	public final String getBeanTypePropertyName() {
		return ctx.getBeanTypePropertyName();
	}

	/**
	 * Configuration property:  Find fluent setters.
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 *
	 * @see BeanContext#BEAN_findFluentSetters
	 * @return
	 * 	<jk>true</jk> if fluent setters are detected on beans.
	 */
	public final boolean isFindFluentSetters() {
		return ctx.isFindFluentSetters();
	}

	/**
	 * Configuration property:  Ignore invocation errors on getters.
	 *
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnGetters
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean getter methods are silently ignored.
	 */
	public final boolean isIgnoreInvocationExceptionsOnGetters() {
		return ctx.isIgnoreInvocationExceptionsOnGetters();
	}

	/**
	 * Configuration property:  Ignore invocation errors on setters.
	 *
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnSetters
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean setter methods are silently ignored.
	 */
	public final boolean isIgnoreInvocationExceptionsOnSetters() {
		return ctx.isIgnoreInvocationExceptionsOnSetters();
	}

	/**
	 * Configuration property:  Silently ignore missing setters.
	 *
	 * @see BeanContext#BEAN_disableIgnoreMissingSetters
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a bean property without a setter should throw a {@link BeanRuntimeException}.
	 */
	public final boolean isIgnoreMissingSetters() {
		return ctx.isIgnoreMissingSetters();
	}

	/**
	 * Configuration property:  Ignore unknown properties.
	 *
	 * @see BeanContext#BEAN_ignoreUnknownBeanProperties
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a non-existent bean property is silently ignored.
	 * 	<br>Otherwise, a {@code RuntimeException} is thrown.
	 */
	public final boolean isIgnoreUnknownBeanProperties() {
		return ctx.isIgnoreUnknownBeanProperties();
	}

	/**
	 * Configuration property:  Ignore unknown properties with null values.
	 *
	 * @see BeanContext#BEAN_disableIgnoreUnknownNullBeanProperties
	 * @return
	 * 	<jk>true</jk> if trying to set a <jk>null</jk> value on a non-existent bean property should not throw a {@link BeanRuntimeException}.
	 */
	public final boolean isIgnoreUnknownNullBeanProperties() {
		return ctx.isIgnoreUnknownNullBeanProperties();
	}

	/**
	 * Configuration property:  Bean class exclusions.
	 *
	 * @see BeanContext#BEAN_notBeanClasses
	 * @return
	 * 	The list of classes that are explicitly not beans.
	 */
	public final Class<?>[] getNotBeanClasses() {
		return ctx.getNotBeanClasses();
	}

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * @see BeanContext#BEAN_notBeanPackages
	 * @return
	 * 	The list of fully-qualified package names to exclude from being classified as beans.
	 */
	public final String[] getNotBeanPackagesNames() {
		return ctx.getNotBeanPackagesNames();
	}

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * @see BeanContext#BEAN_notBeanPackages
	 * @return
	 * 	The list of package name prefixes to exclude from being classified as beans.
	 */
	protected final String[] getNotBeanPackagesPrefixes() {
		return ctx.getNotBeanPackagesPrefixes();
	}

	/**
	 * Configuration property:  Bean property namer.
	 *
	 * @see BeanContext#BEAN_propertyNamer
	 * @return
	 * 	The interface used to calculate bean property names.
	 */
	public final PropertyNamer getPropertyNamer() {
		return ctx.getPropertyNamer();
	}

	/**
	 * Configuration property:  Sort bean properties.
	 *
	 * @see BeanContext#BEAN_sortProperties
	 * @return
	 * 	<jk>true</jk> if all bean properties will be serialized and access in alphabetical order.
	 */
	public final boolean isSortProperties() {
		return ctx.isSortProperties();
	}

	/**
	 * Configuration property:  Java object swaps.
	 *
	 * @see BeanContext#BEAN_swaps
	 * @return
	 * 	The list POJO swaps defined.
	 */
	public final PojoSwap<?,?>[] getSwaps() {
		return ctx.getSwaps();
	}

	/**
	 * Configuration property:  Time zone.
	 *
	 * <p>
	 * The timezone is determined in the following order:
	 * <ol>
	 * 	<li><c>timeZone</c> parameter passed in through constructor.
	 * 	<li>{@link BeanContext#BEAN_timeZone} entry in parameter passed in through constructor.
	 * 	<li>{@link BeanContext#BEAN_timeZone} setting on bean context.
	 * </ol>
	 *
	 * @see BeanContext#BEAN_timeZone
	 * @return The session timezone, or <jk>null</jk> if timezone not specified.
	 */
	public final TimeZone getTimeZone() {
		return timeZone;
	}

	/**
	 * Configuration property:  Time zone.
	 *
	 * <p>
	 * The timezone is determined in the following order:
	 * <ol>
	 * 	<li><c>timeZone</c> parameter passed in through constructor.
	 * 	<li>{@link BeanContext#BEAN_timeZone} entry in parameter passed in through constructor.
	 * 	<li>{@link BeanContext#BEAN_timeZone} setting on bean context.
	 * </ol>
	 *
	 * @see BeanContext#BEAN_timeZone
	 * @return The session timezone, or the system timezone if not specified.  Never <jk>null</jk>.
	 */
	public final ZoneId getTimeZoneId() {
		return timeZone == null ? ZoneId.systemDefault() : timeZone.toZoneId();
	}

	/**
	 * Configuration property:  Use enum names.
	 *
	 * @see BeanContext#BEAN_useEnumNames
	 * @return
	 * 	<jk>true</jk> if enums are always serialized by name, not using {@link Object#toString()}.
	 */
	public final boolean isUseEnumNames() {
		return ctx.isUseEnumNames();
	}

	/**
	 * Configuration property:  Use interface proxies.
	 *
	 * @see BeanContext#BEAN_disableInterfaceProxies
	 * @return
	 * 	<jk>true</jk> if interfaces will be instantiated as proxy classes through the use of an
	 * 	{@link InvocationHandler} if there is no other way of instantiating them.
	 */
	public final boolean isUseInterfaceProxies() {
		return ctx.isUseInterfaceProxies();
	}

	/**
	 * Configuration property:  Use Java Introspector.
	 *
	 * @see BeanContext#BEAN_useJavaBeanIntrospector
	 * @return
	 * 	<jk>true</jk> if the built-in Java bean introspector should be used for bean introspection.
	 */
	public final boolean isUseJavaBeanIntrospector() {
		return ctx.isUseJavaBeanIntrospector();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * HTTP part schema of object being serialized or parsed.
	 *
	 * @return HTTP part schema of object being serialized or parsed, or <jk>null</jk> if not specified.
	 */
	public final HttpPartSchema getSchema() {
		return schema;
	}

	@Override /* Session */
	public void checkForWarnings() {
		if (isDebug())
			super.checkForWarnings();
	}

	@Override /* Session */
	public OMap toMap() {
		return super.toMap()
			.a("Context", ctx.toMap())
			.a(
				"BeanSession",
				OMap
					.create()
					.filtered()
					.a("schema", schema)
			);
	}
}
