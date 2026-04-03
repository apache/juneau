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

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.lang.reflect.*;
import java.math.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.reflect.*;

/**
 * A concrete {@link CachingConverter} that supports common type conversions without
 * requiring a {@code BeanContext} or {@code BeanSession}.
 *
 * <p>
 * The following conversions are supported:
 *
 * <table class='styled'>
 * 	<tr><th>Input type</th><th>Output type</th><th>Notes</th></tr>
 * 	<tr>
 * 		<td>Any type</td>
 * 		<td>Same or supertype</td>
 * 		<td>Identity / widening (no-op cast)</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link Number}</td>
 * 		<td>
 * 			{@link Integer}, {@link Long}, {@link Short}, {@link Float}, {@link Double}, {@link Byte},
 * 			{@link AtomicInteger}, {@link AtomicLong}, and primitive equivalents
 * 		</td>
 * 		<td>Narrowing/widening numeric conversion</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link Boolean}</td>
 * 		<td>{@link Number} types</td>
 * 		<td><c>true=1, false=0</c></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link CharSequence}</td>
 * 		<td>{@link Number} types</td>
 * 		<td>Parsed via {@link org.apache.juneau.commons.utils.StringUtils#parseNumber(String, Class)}</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link Number}</td>
 * 		<td>{@link Boolean}</td>
 * 		<td><c>intValue() != 0</c></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link CharSequence}</td>
 * 		<td>{@link Boolean}</td>
 * 		<td>{@link Boolean#valueOf(String)}</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link CharSequence} (length 1)</td>
 * 		<td>{@link Character}</td>
 * 		<td><c>charAt(0)</c></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link Number}</td>
 * 		<td>{@link Character}</td>
 * 		<td><c>(char) intValue()</c></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Any</td>
 * 		<td>{@link String}</td>
 * 		<td>{@link Object#toString()}, with array support via {@link Arrays#toString(Object[])}</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link CharSequence}</td>
 * 		<td>Any {@link Enum}</td>
 * 		<td>{@link Enum#valueOf(Class, String)}</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link Collection} or array</td>
 * 		<td>{@link Collection} subtype</td>
 * 		<td>Copies elements; element type from <c>args[0]</c></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link Map}</td>
 * 		<td>{@link Map} subtype</td>
 * 		<td>Copies entries; key type from <c>args[0]</c>, value type from <c>args[1]</c></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link Collection} or array</td>
 * 		<td>Array type</td>
 * 		<td>Element type from <c>outType.getComponentType()</c></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link String}</td>
 * 		<td>{@link TimeZone}</td>
 * 		<td>{@link TimeZone#getTimeZone(String)}</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link TimeZone}</td>
 * 		<td>{@link String}</td>
 * 		<td>{@link TimeZone#getID()}</td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link String}</td>
 * 		<td>{@link Locale}</td>
 * 		<td>{@link Locale#forLanguageTag(String)} (underscores converted to hyphens)</td>
 * 	</tr>
 * 	<tr>
 * 		<td>Any</td>
 * 		<td>Any with <c>static T fromString/valueOf/of/from/parse/create/forName/fromValue/builder(X)</c>
 * 			or dynamic <c>fromX/forX/parseX</c> where X is the input class name</td>
 * 		<td>Reflection-based static factory lookup</td>
 * 	</tr>
 * 	<tr>
 * 		<td>Any</td>
 * 		<td>Any with <c>public T(X)</c> constructor</td>
 * 		<td>Reflection-based constructor lookup</td>
 * 	</tr>
 * 	<tr>
 * 		<td>Any with <c>toX()</c> instance method</td>
 * 		<td>Any type X</td>
 * 		<td>Instance method where name matches <c>to</c> + output class name (e.g. <c>toInteger()</c>)</td>
 * 	</tr>
 * </table>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe. The singleton instance can be safely shared across multiple threads.
 */
@SuppressWarnings({
	"rawtypes", // Raw types necessary for generic conversion dispatch
	"unchecked", // Type erasure requires unchecked casts throughout conversion logic
	"java:S3776", // Cognitive complexity of conversion dispatch methods is inherent to the number of supported type pairs
	"java:S1067" // Complex boolean expressions in conversion checks reflect the natural type hierarchy
})
public class BasicConverter extends CachingConverter {

	/**
	 * Singleton instance.
	 */
	public static final BasicConverter INSTANCE = new BasicConverter();

	private static final Set<String> FACTORY_METHOD_NAMES = Set.of(
		"fromString", "valueOf", "of", "from", "parse", "create", "forName", "fromValue", "builder"
	);

	private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(
		boolean.class, Boolean.class,
		byte.class, Byte.class,
		char.class, Character.class,
		double.class, Double.class,
		float.class, Float.class,
		int.class, Integer.class,
		long.class, Long.class,
		short.class, Short.class
	);

	/**
	 * Constructor.
	 */
	protected BasicConverter() {}

	@Override
	protected <I, O> Conversion<I, O> findConversion(Class<I> inType, Class<O> outType) {
		var out = outType.isPrimitive() ? (Class<O>) PRIMITIVE_TO_WRAPPER.get(outType) : outType;

		Conversion<I, O> c;

		if ((c = findSpecialConversion(inType, out)) != null) return c;

		if (out.isAssignableFrom(inType) && !Collection.class.isAssignableFrom(out) && !Map.class.isAssignableFrom(out))
			return (in, memberOf, session, args) -> (O) in;

		if (Number.class.isAssignableFrom(out) && (c = findNumberConversion(inType, out)) != null) return c;

		if (out == Boolean.class && (c = findBooleanConversion(inType)) != null) return c;

		if (out == Character.class && (c = findCharacterConversion(inType)) != null) return c;

		if (out == String.class)
			return (Conversion<I, O>) findToStringConversion(inType);

		if (out.isEnum() && (c = findEnumConversion(inType, out)) != null) return c;

		if (out == Optional.class && (c = findOptionalConversion(inType)) != null) return c;

		if (Collection.class.isAssignableFrom(out) && (c = findCollectionConversion(inType, out)) != null) return c;

		if (Map.class.isAssignableFrom(out) && (c = findMapConversion(inType, out)) != null) return c;

		if (out.isArray() && (c = findArrayConversion(inType, out)) != null) return c;

		if ((c = findToXMethod(inType, out)) != null) return c;

		if ((c = findStaticFactory(inType, out)) != null) return c;

		if ((c = findConstructorConversion(inType, out)) != null) return c;

		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Number conversions
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findNumberConversion(Class<I> inType, Class<O> outType) {
		if (Number.class.isAssignableFrom(inType))
			return findNumberFromNumber(outType);
		if (inType == Boolean.class)
			return (Conversion<I, O>) findNumberFromBoolean(outType);
		if (CharSequence.class.isAssignableFrom(inType))
			return (Conversion<I, O>) findNumberFromString(outType);
		return null;
	}

	private <I, O> Conversion<I, O> findNumberFromNumber(Class<O> outType) {
		if (outType == Integer.class) return (in, memberOf, session, args) -> (O) Integer.valueOf(((Number) in).intValue());
		if (outType == Long.class) return (in, memberOf, session, args) -> (O) Long.valueOf(((Number) in).longValue());
		if (outType == Short.class) return (in, memberOf, session, args) -> (O) Short.valueOf(((Number) in).shortValue());
		if (outType == Float.class) return (in, memberOf, session, args) -> (O) Float.valueOf(((Number) in).floatValue());
		if (outType == Double.class) return (in, memberOf, session, args) -> (O) Double.valueOf(((Number) in).doubleValue());
		if (outType == Byte.class) return (in, memberOf, session, args) -> (O) Byte.valueOf(((Number) in).byteValue());
		if (outType == AtomicInteger.class) return (in, memberOf, session, args) -> (O) new AtomicInteger(((Number) in).intValue());
		if (outType == AtomicLong.class) return (in, memberOf, session, args) -> (O) new AtomicLong(((Number) in).longValue());
		if (outType == BigDecimal.class) return (in, memberOf, session, args) -> (O) new BigDecimal(((Number) in).toString());
		if (outType == BigInteger.class) return (in, memberOf, session, args) -> {
			var n = (Number) in;
			if (n instanceof BigDecimal bd) return (O) bd.toBigInteger();
			if (n instanceof BigInteger bi) return (O) bi;
			return (O) BigInteger.valueOf(n.longValue());
		};
		return null;
	}

	private <O> Conversion<Boolean, O> findNumberFromBoolean(Class<O> outType) {
		if (outType == Integer.class) return (in, memberOf, session, args) -> (O) Integer.valueOf(in.booleanValue() ? 1 : 0);
		if (outType == Long.class) return (in, memberOf, session, args) -> (O) Long.valueOf(in.booleanValue() ? 1L : 0L);
		if (outType == Short.class) return (in, memberOf, session, args) -> (O) Short.valueOf(in.booleanValue() ? (short) 1 : (short) 0);
		if (outType == Float.class) return (in, memberOf, session, args) -> (O) Float.valueOf(in.booleanValue() ? 1f : 0f);
		if (outType == Double.class) return (in, memberOf, session, args) -> (O) Double.valueOf(in.booleanValue() ? 1d : 0d);
		if (outType == Byte.class) return (in, memberOf, session, args) -> (O) Byte.valueOf(in.booleanValue() ? (byte) 1 : (byte) 0);
		if (outType == AtomicInteger.class) return (in, memberOf, session, args) -> (O) new AtomicInteger(in.booleanValue() ? 1 : 0);
		if (outType == AtomicLong.class) return (in, memberOf, session, args) -> (O) new AtomicLong(in.booleanValue() ? 1L : 0L);
		if (outType == BigDecimal.class) return (in, memberOf, session, args) -> (O) BigDecimal.valueOf(in.booleanValue() ? 1L : 0L);
		if (outType == BigInteger.class) return (in, memberOf, session, args) -> (O) BigInteger.valueOf(in.booleanValue() ? 1L : 0L);
		return null;
	}

	private <O> Conversion<CharSequence, O> findNumberFromString(Class<O> outType) {
		return (in, memberOf, session, args) -> (O) parseNumber(in.toString(), (Class<? extends Number>) outType);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Boolean conversions
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findBooleanConversion(Class<I> inType) {
		if (Number.class.isAssignableFrom(inType))
			return (in, memberOf, session, args) -> (O) Boolean.valueOf(((Number) in).intValue() != 0);
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Character conversions
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findCharacterConversion(Class<I> inType) {
		if (CharSequence.class.isAssignableFrom(inType))
			return (in, memberOf, session, args) -> {
				var s = in.toString();
				if (s.length() != 1)
					throw illegalArg("Cannot convert string of length {0} to char: ''{1}''", s.length(), s);
				return (O) Character.valueOf(s.charAt(0));
			};
		if (Number.class.isAssignableFrom(inType))
			return (in, memberOf, session, args) -> {
				var s = in.toString();
				return s.isEmpty() ? null : (O) Character.valueOf(s.charAt(0));
			};
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String conversions
	//-----------------------------------------------------------------------------------------------------------------

	private <I> Conversion<I, String> findToStringConversion(Class<I> inType) {
		if (inType.isArray()) {
			if (inType == int[].class) return (in, memberOf, session, args) -> Arrays.toString((int[]) in);
			if (inType == long[].class) return (in, memberOf, session, args) -> Arrays.toString((long[]) in);
			if (inType == double[].class) return (in, memberOf, session, args) -> Arrays.toString((double[]) in);
			if (inType == float[].class) return (in, memberOf, session, args) -> Arrays.toString((float[]) in);
			if (inType == boolean[].class) return (in, memberOf, session, args) -> Arrays.toString((boolean[]) in);
			if (inType == byte[].class) return (in, memberOf, session, args) -> Arrays.toString((byte[]) in);
			if (inType == short[].class) return (in, memberOf, session, args) -> Arrays.toString((short[]) in);
			if (inType == char[].class) return (in, memberOf, session, args) -> Arrays.toString((char[]) in);
			return (in, memberOf, session, args) -> Arrays.deepToString((Object[]) in);
		}
		return (in, memberOf, session, args) -> in.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Enum conversions
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findEnumConversion(Class<I> inType, Class<O> outType) {
		if (CharSequence.class.isAssignableFrom(inType))
			return (in, memberOf, session, args) -> (O) Enum.valueOf((Class<Enum>) outType, in.toString());
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Optional conversions
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private <I, O> Conversion<I, O> findOptionalConversion(Class<I> inType) {
		return (Conversion<I, O>) (Conversion<I, Optional<?>>) (in, memberOf, session, args) -> {
			if (in instanceof Optional<?> opt) {
				if (args.length == 0)
					return opt;
				return opt.map(x -> to(x, args[0]));
			}
			if (args.length == 0)
				return Optional.of(in);
			return Optional.of(to(in, args[0]));
		};
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Collection conversions
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findCollectionConversion(Class<I> inType, Class<O> outType) {
		if (Collection.class.isAssignableFrom(inType) || inType.isArray()) {
			return (in, memberOf, session, args) -> {
				var elemType = args.length > 0 ? args[0] : null;
				if (elemType == null && !inType.isArray() && outType.isAssignableFrom(inType))
					return (O) in;
				var result = newCollection(outType);
				if (Collection.class.isAssignableFrom(inType)) {
					for (var elem : (Collection<?>) in)
						result.add(elemType != null ? to(elem, elemType) : elem);
				} else {
					var len = Array.getLength(in);
					for (var i = 0; i < len; i++) {
						var elem = Array.get(in, i);
						result.add(elemType != null ? to(elem, elemType) : elem);
					}
				}
				return (O) result;
			};
		}
		return null;
	}

	private Collection<Object> newCollection(Class<?> outType) {
		if (outType == List.class || outType == Collection.class || outType == Iterable.class /* HTT: Iterable is a supertype of Collection so findCollectionConversion never passes Iterable as outType */ || outType == AbstractList.class)
			return new ArrayList<>();
		if (outType == Set.class || outType == LinkedHashSet.class || outType == AbstractSet.class)
			return new LinkedHashSet<>();
		if (outType == SortedSet.class || outType == NavigableSet.class || outType == TreeSet.class)
			return new TreeSet<>();
		if (outType == Queue.class || outType == Deque.class || outType == LinkedList.class)
			return new LinkedList<>();
		return newInstanceOrDefault(outType, ArrayList::new);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Map conversions
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findMapConversion(Class<I> inType, Class<O> outType) {
		if (Map.class.isAssignableFrom(inType)) {
			return (in, memberOf, session, args) -> {
				var keyType = args.length > 0 ? args[0] : null;
				var valType = args.length > 1 ? args[1] : null;
				if (keyType == null && outType.isAssignableFrom(inType))
					return (O) in;
				var result = newMap(outType);
				((Map<?, ?>) in).forEach((k, v) -> result.put(
					keyType != null ? to(k, keyType) : k,
					valType != null ? to(v, valType) : v
				));
				return (O) result;
			};
		}
		return null;
	}

	private Map<Object, Object> newMap(Class<?> outType) {
		if (outType == Map.class || outType == LinkedHashMap.class || outType == AbstractMap.class)
			return new LinkedHashMap<>();
		if (outType == SortedMap.class || outType == NavigableMap.class || outType == TreeMap.class)
			return new TreeMap<>();
		if (outType == HashMap.class)
			return new HashMap<>();
		return newInstanceOrDefault(outType, LinkedHashMap::new);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Array conversions
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findArrayConversion(Class<I> inType, Class<O> outType) {
		if (Collection.class.isAssignableFrom(inType) || inType.isArray()) {
			var componentType = outType.getComponentType();
			// For array→array, validate that element-level conversion is possible.
			// Skip the pre-check for Object[] — runtime elements may be of a more specific type.
			// Collections use runtime element types so we skip the pre-check there too.
			if (inType.isArray()) {
				var inComponentType = inType.getComponentType();
				if (inComponentType != componentType
						&& inComponentType != Object.class
						&& !canConvert(inComponentType, componentType))
					return null;
			}
			return (in, memberOf, session, args) -> {
				if (Collection.class.isAssignableFrom(inType)) {
					var list = (Collection<?>) in;
					var arr = Array.newInstance(componentType, list.size());
					var i = 0;
					for (var elem : list)
						Array.set(arr, i++, to(elem, componentType));
					return (O) arr;
				}
				var len = Array.getLength(in);
				var arr = Array.newInstance(componentType, len);
				for (var i = 0; i < len; i++)
					Array.set(arr, i, to(Array.get(in, i), componentType));
				return (O) arr;
			};
		}
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Special-case conversions
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findSpecialConversion(Class<I> inType, Class<O> outType) {
		if (inType == String.class && outType == TimeZone.class)
			return (Conversion<I, O>) (Conversion<String, TimeZone>) (in, memberOf, session, args) -> TimeZone.getTimeZone(in);
		if (TimeZone.class.isAssignableFrom(inType) && outType == String.class)
			return (Conversion<I, O>) (Conversion<TimeZone, String>) (in, memberOf, session, args) -> in.getID();
		if (inType == String.class && outType == Locale.class)
			return (Conversion<I, O>) (Conversion<String, Locale>) (in, memberOf, session, args) -> Locale.forLanguageTag(in.replace('_', '-'));
		if (CharSequence.class.isAssignableFrom(inType) && outType == Boolean.class)
			return (in, memberOf, session, args) -> {
				var s = in.toString();
				if (s.isEmpty() || "null".equals(s))
					return null;
				return (O) Boolean.valueOf(s);
			};
		if (CharSequence.class.isAssignableFrom(inType) && outType == byte[].class)
			return (in, memberOf, session, args) -> (O) in.toString().getBytes(StandardCharsets.UTF_8);
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Reflection: static factory methods
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findStaticFactory(Class<I> inType, Class<O> outType) {
		var ci = info(outType);

		for (var name : FACTORY_METHOD_NAMES) {
			var opt = findStaticMethod(ci, name, inType, outType);
			if (opt.isPresent())
				return (in, memberOf, session, args) -> opt.get().invoke(null, in);
		}

		// Walk the type hierarchy (superclasses then interfaces) so that e.g. InputStreamReader
		// can be passed to a method declared as fromReader(Reader r).
		for (Class<?> c = inType; c != null && c != Object.class; c = c.getSuperclass()) {
			var inName = c.getSimpleName();
			for (var prefix : new String[]{"from", "for", "parse"}) {
				var opt = findStaticMethod(ci, prefix + inName, inType, outType);
				if (opt.isPresent())
					return (in, memberOf, session, args) -> opt.get().invoke(null, in);
			}
		}
		for (var iface : allInterfaces(inType)) {
			var inName = iface.getSimpleName();
			for (var prefix : new String[]{"from", "for", "parse"}) {
				var opt = findStaticMethod(ci, prefix + inName, inType, outType);
				if (opt.isPresent())
					return (in, memberOf, session, args) -> opt.get().invoke(null, in);
			}
		}

		return null;
	}

	private static List<Class<?>> allInterfaces(Class<?> c) {
		var result = new ArrayList<Class<?>>();
		for (var x = c; x != null; x = x.getSuperclass())
			for (var iface : x.getInterfaces())
				collectInterfaces(iface, result);
		return result;
	}

	private static void collectInterfaces(Class<?> iface, List<Class<?>> result) {
		if (!result.contains(iface)) {
			result.add(iface);
			for (var parent : iface.getInterfaces())
				collectInterfaces(parent, result);
		}
	}

	private Optional<MethodInfo> findStaticMethod(ClassInfo ci, String name, Class<?> inType, Class<?> outType) {
		return ci.getPublicMethod(m ->
			m.isStatic()
			&& m.isNotDeprecated()
			&& m.hasName(name)
			&& m.getParameterCount() == 1
			&& m.getParameterTypes().get(0).isAssignableFrom(inType)
			&& m.hasReturnTypeParent(outType)
		);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Reflection: public constructors
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findConstructorConversion(Class<I> inType, Class<O> outType) {
		if (outType.getEnclosingClass() != null && !Modifier.isStatic(outType.getModifiers())) {
			var enclosing = outType.getEnclosingClass();
			var opt = info(outType).getPublicConstructor(c ->
				c.getParameterCount() == 2
				&& c.isNotDeprecated()
				&& c.getParameterTypes().get(0).inner() == enclosing
				&& c.getParameterTypes().get(1).isAssignableFrom(inType)
			);
			if (opt.isPresent()) {
				var ctor = opt.get();
				return (in, memberOf, session, args) -> ctor.newInstance(memberOf, in);
			}
		}

		var opt = info(outType).getPublicConstructor(c ->
			c.getParameterCount() == 1
			&& c.isNotDeprecated()
			&& c.getParameterTypes().get(0).isAssignableFrom(inType)
		);
		if (opt.isPresent()) {
			var ctor = opt.get();
			return (in, memberOf, session, args) -> ctor.newInstance(in);
		}
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Reflection: toX() instance methods
	//-----------------------------------------------------------------------------------------------------------------

	private <I, O> Conversion<I, O> findToXMethod(Class<I> inType, Class<O> outType) {
		// Use getNameReadable() so that array types use "Array" suffix (e.g. "toStringArray" for String[]).
		// Use equalsIgnoreCase to match Mutaters' behavior (e.g. "toByteArray" matches "tobyteArray" from byte[]).
		var methodName = "to" + info(outType).getNameReadable();
		var opt = info(inType).getPublicMethod(m ->
			m.isNotStatic()
			&& m.isNotDeprecated()
			&& m.getParameterCount() == 0
			&& m.getNameSimple().equalsIgnoreCase(methodName)
			&& m.hasReturnTypeParent(outType)
		);
		if (opt.isPresent()) {
			var method = opt.get();
			return (in, memberOf, session, args) -> method.invoke(in);
		}
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	private <T> T newInstanceOrDefault(Class<?> type, java.util.function.Supplier<T> defaultSupplier) {
		var opt = info(type).getPublicConstructor(c -> c.getParameterCount() == 0);
		if (opt.isPresent()) {
			var ctor = opt.get();
		try {
			return ctor.newInstance();
		} catch (@SuppressWarnings("unused") Exception e) { // HTT: constructor would have to throw to reach here
			return defaultSupplier.get(); // HTT
		}
		}
		return defaultSupplier.get();
	}
}
