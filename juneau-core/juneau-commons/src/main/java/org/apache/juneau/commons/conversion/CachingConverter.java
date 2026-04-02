/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.conversion;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * An abstract {@link Converter} implementation that caches previously determined type conversions.
 *
 * <p>
 * Conversion functions are discovered lazily on the first call for a given input/output type pair and
 * stored in a two-level {@link ConcurrentHashMap} for subsequent fast lookup.
 *
 * <p>
 * Subclasses implement {@link #findConversion(Class, Class)} to provide the actual conversion logic.
 * The result is cached so that reflection or other expensive discovery happens only once per type pair.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> MyConverter <jk>extends</jk> CachingConverter {
 *
 * 		<ja>@Override</ja>
 * 		<jk>protected</jk> &lt;I,O&gt; Conversion&lt;I,O&gt; findConversion(Class&lt;I&gt; inType, Class&lt;O&gt; outType) {
 * 			<jk>if</jk> (inType == String.<jk>class</jk> &amp;&amp; outType == Integer.<jk>class</jk>)
 * 				<jk>return</jk> (Conversion&lt;I,O&gt;) (Conversion&lt;String,Integer&gt;) (<jv>s</jv>, <jv>memberOf</jv>, <jv>args</jv>) -&gt; Integer.<jsm>valueOf</jsm>(<jv>s</jv>);
 * 			<jk>return null</jk>;
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe. The cache uses {@link ConcurrentHashMap} for safe concurrent access.
 * </p>
 */
public abstract class CachingConverter implements Converter {

	// Sentinel stored in the cache when findConversion() returns null.
	// ConcurrentHashMap does not permit null values, so we cannot store null directly.
	// Using this sentinel avoids re-invoking findConversion() for unconvertable type pairs.
	private static final Conversion<?,?> NO_CONVERSION = (in, memberOf, session, args) -> null; // HTT: sentinel body is never invoked; ConcurrentHashMap prohibits null values so we use this placeholder

	/**
	 * Returns the appropriate default value for a null input to the given target type.
	 *
	 * <ul>
	 * 	<li>For {@link Optional}: returns {@link Optional#empty()}, or {@code Optional.of(Optional.empty())} for
	 * 		nested optional types such as {@code Optional<Optional<Integer>>}.
	 * 	<li>For primitives: returns the JVM zero/false default (e.g. {@code 0} for {@code int}).
	 * 	<li>For all other types: returns {@code null}.
	 * </ul>
	 *
	 * @param type The target type.
	 * @param args The generic type arguments (e.g. element type for {@link Optional}).
	 * @param <T> The target type.
	 * @return The appropriate null default.
	 */
	@SuppressWarnings("unchecked")
	static <T> T nullDefault(Class<T> type, Class<?>... args) {
		if (type == Optional.class) {
			if (args.length > 0)
				return (T) Optional.ofNullable(nullDefault(args[0], Arrays.copyOfRange(args, 1, args.length)));
			return (T) Optional.empty();
		}
		return type.isPrimitive() ? primitiveDefault(type) : null;
	}

	/**
	 * Returns the JVM default value for a primitive type, or <jk>null</jk> if the type is not primitive.
	 *
	 * <p>
	 * This is used to satisfy {@link #to} when the input is <jk>null</jk> and the target type is a
	 * non-boxed primitive (e.g. {@code int.class}), where returning <jk>null</jk> would be invalid.
	 *
	 * @param type The target type.
	 * @param <T> The target type.
	 * @return The JVM zero/false default, or <jk>null</jk> if {@code type} is not a primitive.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T primitiveDefault(Class<T> type) {
		if (type == Integer.TYPE)   return (T) Integer.valueOf(0);
		if (type == Long.TYPE)      return (T) Long.valueOf(0L);
		if (type == Double.TYPE)    return (T) Double.valueOf(0.0d);
		if (type == Float.TYPE)     return (T) Float.valueOf(0.0f);
		if (type == Boolean.TYPE)   return (T) Boolean.FALSE;
		if (type == Short.TYPE)     return (T) Short.valueOf((short)0);
		if (type == Byte.TYPE)      return (T) Byte.valueOf((byte)0);
		if (type == Character.TYPE) return (T) Character.valueOf('\0');
		return null; // HTT: no other primitive types exist in Java
	}

	// Two-level cache: input type -> output type -> conversion function (or NO_CONVERSION sentinel).
	private final Map<Class<?>, Map<Class<?>, Conversion<?,?>>> conversions = new ConcurrentHashMap<>();

	/**
	 * Finds a conversion function from the specified input type to the specified output type.
	 *
	 * <p>
	 * This method is called lazily the first time a conversion between a given type pair is requested.
	 * The result is cached so subsequent calls for the same type pair bypass this method entirely.
	 * When this method returns <jk>null</jk>, a sentinel is stored so that unconvertable type pairs
	 * are also only evaluated once.
	 *
	 * <p>
	 * Implementations should return <jk>null</jk> if no conversion is possible for the given types,
	 * which will cause {@link #to(Object, Class)} and {@link #to(Object, Type, Type...)} to throw
	 * {@link InvalidConversionException} for that type pair.
	 *
	 * @param <I> The input type.
	 * @param <O> The output type.
	 * @param inType The runtime class of the input object.
	 * @param outType The target output class.
	 * @return A {@link Conversion} function, or <jk>null</jk> if no conversion is available.
	 */
	protected abstract <I, O> Conversion<I, O> findConversion(Class<I> inType, Class<O> outType);

	@SuppressWarnings("unchecked")
	private <I, O> Conversion<I, O> lookupConversion(Class<I> inType, Class<O> outType) {
		var fn = conversions
			.computeIfAbsent(inType, k -> new ConcurrentHashMap<>())
			.computeIfAbsent(outType, k -> {
				var found = findConversion(inType, outType);
				return found != null ? found : NO_CONVERSION;
			});
		return fn == NO_CONVERSION ? null : (Conversion<I, O>) fn;
	}

	@Override
	public boolean canConvert(Class<?> inType, Class<?> outType) {
		if (inType == outType)
			return true;
		return lookupConversion(inType, outType) != null;
	}

	/**
	 * Converts the specified object to the specified type.
	 *
	 * <p>
	 * On the first call for a given input/output type pair, {@link #findConversion(Class, Class)} is
	 * invoked and the result is cached. Subsequent calls use the cached function directly.
	 *
	 * @param o The object to convert. Can be <jk>null</jk>.
	 * @param type The target type.
	 * @param <T> The target type.
	 * @return The converted object, or the primitive zero-value if the input is <jk>null</jk> and the target
	 * 	is a primitive type, or <jk>null</jk> otherwise.
	 * @throws InvalidConversionException If no conversion path exists from the input type to the target type.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T to(Object o, Class<T> type) {
		if (o == null)
			return type.isPrimitive() ? primitiveDefault(type) : null;
		var inType = o.getClass();
		if (inType == type)
			return (T) o;
		var fn = (Conversion<Object, T>) lookupConversion(inType, type);
		if (fn == null)
			throw new InvalidConversionException(inType, type);
		return fn.to(o, null, (ConverterSession)null);
	}

	/**
	 * Converts the specified object to the specified parameterized type.
	 *
	 * <p>
	 * The raw class is extracted from {@code mainType} (supporting both {@link Class} and
	 * {@link ParameterizedType} values). Type arguments are extracted from {@code args} the same way
	 * and passed through to the cached {@link Conversion} function at call time.
	 *
	 * <p>
	 * The cache is keyed on the raw input and output classes only. The {@code args} are not part of
	 * the cache key — a single cached {@link Conversion} handles all parameterizations of a given
	 * output type.
	 *
	 * @param o The object to convert. Can be <jk>null</jk>.
	 * @param mainType The target type. May be a {@link Class} or {@link ParameterizedType}.
	 * @param args The type arguments of the target type (e.g. element type for collections).
	 * @param <T> The target type.
	 * @return The converted object, or <jk>null</jk> if the input is <jk>null</jk>.
	 * @throws InvalidConversionException If no conversion path exists from the input type to the target type.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T to(Object o, Type mainType, Type... args) {
		var rawType = (Class<T>) (mainType instanceof ParameterizedType pt ? pt.getRawType() : (Class<?>) mainType);
		var argClasses = Stream.of(args)
			.map(t -> (Class<?>) (t instanceof ParameterizedType pt2 ? pt2.getRawType() : t))
			.toArray(Class[]::new);
		if (o == null)
			return nullDefault(rawType, argClasses);
		var inType = o.getClass();
		var fn = (Conversion<Object, T>) lookupConversion(inType, rawType);
		if (fn == null)
			throw new InvalidConversionException(inType, rawType);
		return fn.to(o, null, (ConverterSession)null, argClasses);
	}

	/**
	 * Converts the specified object to the specified type, using the given outer instance and converter session.
	 *
	 * <p>
	 * The session is forwarded to the cached {@link Conversion} function so conversion lambdas can access
	 * contextual objects such as {@link java.util.TimeZone} or {@link java.util.Locale}.
	 *
	 * @param o The object to convert. Can be <jk>null</jk>.
	 * @param memberOf The outer instance for non-static inner class construction, or <jk>null</jk>.
	 * @param session The converter session providing contextual objects, or <jk>null</jk>.
	 * @param type The target type.
	 * @param <T> The target type.
	 * @return The converted object, or the primitive zero-value if the input is <jk>null</jk> and the target
	 * 	is a primitive type, or <jk>null</jk> otherwise.
	 * @throws InvalidConversionException If no conversion path exists from the input type to the target type.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T to(Object o, Object memberOf, ConverterSession session, Class<T> type) {
		if (o == null)
			return type.isPrimitive() ? primitiveDefault(type) : null;
		var inType = o.getClass();
		if (inType == type)
			return (T) o;
		var fn = (Conversion<Object, T>) lookupConversion(inType, type);
		if (fn == null)
			throw new InvalidConversionException(inType, type);
		return fn.to(o, memberOf, session);
	}

	/**
	 * Converts the specified object to the specified parameterized type, using the given outer instance and converter session.
	 *
	 * <p>
	 * The session is forwarded to the cached {@link Conversion} function so conversion lambdas can access
	 * contextual objects such as {@link java.util.TimeZone} or {@link java.util.Locale}.
	 *
	 * @param o The object to convert. Can be <jk>null</jk>.
	 * @param memberOf The outer instance for non-static inner class construction, or <jk>null</jk>.
	 * @param session The converter session providing contextual objects, or <jk>null</jk>.
	 * @param mainType The target type. May be a {@link Class} or {@link ParameterizedType}.
	 * @param args The type arguments of the target type (e.g. element type for collections).
	 * @param <T> The target type.
	 * @return The converted object, or <jk>null</jk> if the input is <jk>null</jk>.
	 * @throws InvalidConversionException If no conversion path exists from the input type to the target type.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T to(Object o, Object memberOf, ConverterSession session, Type mainType, Type... args) {
		var rawType = (Class<T>) (mainType instanceof ParameterizedType pt ? pt.getRawType() : (Class<?>) mainType);
		var argClasses = Stream.of(args)
			.map(t -> (Class<?>) (t instanceof ParameterizedType pt2 ? pt2.getRawType() : t))
			.toArray(Class[]::new);
		if (o == null)
			return nullDefault(rawType, argClasses);
		var inType = o.getClass();
		var fn = (Conversion<Object, T>) lookupConversion(inType, rawType);
		if (fn == null)
			throw new InvalidConversionException(inType, rawType);
		return fn.to(o, memberOf, session, argClasses);
	}
}
