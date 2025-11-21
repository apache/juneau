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
package org.apache.juneau.reflect;

import static org.apache.juneau.common.reflect.ReflectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.common.reflect.*;

/**
 * Cache of object that convert POJOs to and from common types such as strings, readers, and input streams.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class Mutaters {
	private static final ConcurrentHashMap<Class<?>,Map<Class<?>,Mutater<?,?>>> CACHE = new ConcurrentHashMap<>();

	/**
	 * Represents a non-existent transform.
	 */
	public static final Mutater<Object,Object> NULL = new Mutater<>() {
		@Override
		public Object mutate(Object outer, Object in) {
			return null;
		}
	};

	// Special cases.
	static {
		// @formatter:off

		// TimeZone doesn't follow any standard conventions.
		add(String.class, TimeZone.class,
			new Mutater<String,TimeZone>() {
				@Override public TimeZone mutate(Object outer, String in) {
					return TimeZone.getTimeZone(in);
				}
			}
		);
		add(TimeZone.class, String.class,
			new Mutater<TimeZone,String>() {
				@Override public String mutate(Object outer, TimeZone in) {
					return in.getID();
				}
			}
		);

		// Locale(String) doesn't work on strings like "ja_JP".
		add(String.class, Locale.class,
			new Mutater<String,Locale>() {
				@Override
				public Locale mutate(Object outer, String in) {
					return Locale.forLanguageTag(in.replace('_', '-'));
				}
			}
		);

		// String-to-Boolean transform should allow for "null" keyword.
		add(String.class, Boolean.class,
			new Mutater<String,Boolean>() {
				@Override
				public Boolean mutate(Object outer, String in) {
					if (in == null || "null".equals(in) || in.isEmpty())
						return null;
					return Boolean.valueOf(in);
				}
			}
		);
		// @formatter:on
	}

	/**
	 * Adds a transform for the specified input/output types.
	 *
	 * @param ic The input type.
	 * @param oc The output type.
	 * @param t The transform for converting the input to the output.
	 */
	public static synchronized void add(Class<?> ic, Class<?> oc, Mutater<?,?> t) {
		Map<Class<?>,Mutater<?,?>> m = CACHE.get(oc);
		if (m == null) {
			m = new ConcurrentHashMap<>();
			CACHE.put(oc, m);
		}
		m.put(ic, t);
	}

	/**
	 * Constructs a new instance of the specified class from the specified string.
	 *
	 * <p>
	 * Class must be one of the following:
	 * <ul>
	 * 	<li>Have a public constructor that takes in a single <c>String</c> argument.
	 * 	<li>Have a static <c>fromString(String)</c> (or related) method.
	 * 	<li>Be an <c>enum</c>.
	 * </ul>
	 *
	 * @param <T> The class type.
	 * @param c The class type.
	 * @param s The string to create the instance from.
	 * @return A new object instance, or <jk>null</jk> if a method for converting the string to an object could not be found.
	 */
	public static <T> T fromString(Class<T> c, String s) {
		Mutater<String,T> t = get(String.class, c);
		return t == null ? null : t.mutate(s);
	}

	/**
	 * Returns the transform for converting the specified input type to the specified output type.
	 *
	 * @param <I> The input type.
	 * @param <O> The output type.
	 * @param ic The input type.
	 * @param oc The output type.
	 * @return The transform for performing the conversion, or <jk>null</jk> if the conversion cannot be made.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <I,O> Mutater<I,O> get(Class<I> ic, Class<O> oc) {

		if (ic == null || oc == null)
			return null;

		Map<Class<?>,Mutater<?,?>> m = CACHE.get(oc);
		if (m == null) {
			m = new ConcurrentHashMap<>();
			CACHE.putIfAbsent(oc, m);
			m = CACHE.get(oc);
		}

		Mutater t = m.get(ic);

		if (t == null) {
			t = find(ic, oc, m);
			m.put(ic, t);
		}

		return t == NULL ? null : t;
	}

	/**
	 * Returns the transform for converting the specified input type to the specified output type.
	 *
	 * @param <I> The input type.
	 * @param <O> The output type.
	 * @param ic The input type.
	 * @param oc The output type.
	 * @return The transform for performing the conversion, or <jk>null</jk> if the conversion cannot be made.
	 */
	public static <I,O> boolean hasMutate(Class<I> ic, Class<O> oc) {
		return get(ic, oc) != NULL;
	}

	/**
	 * Converts an object to a string.
	 *
	 * <p>
	 * Normally, this is just going to call <c>toString()</c> on the object.
	 * However, the {@link Locale} and {@link TimeZone} objects are treated special so that the returned value
	 * works with the {@link #fromString(Class, String)} method.
	 *
	 * @param o The object to convert to a string.
	 * @return The stringified object, or <jk>null</jk> if the object was <jk>null</jk>.
	 */
	@SuppressWarnings({ "unchecked" })
	public static String toString(Object o) {
		if (o == null)
			return null;
		var t = (Mutater<Object,String>)get(o.getClass(), String.class);
		return t == null ? o.toString() : t.mutate(o);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Mutater find(Class<?> ic, Class<?> oc, Map<Class<?>,Mutater<?,?>> m) {

		if (ic == oc) {
			return new Mutater() {
				@Override
				public Object mutate(Object outer, Object in) {
					return in;
				}
			};
		}

	var ici = info(ic);
	var oci = info(oc);

		ClassInfo pic = ici.getAllParents().stream().filter(x -> nn(m.get(x.inner()))).findFirst().orElse(null);
		if (nn(pic))
			return m.get(pic.inner());

		if (ic == String.class) {
		Class<?> oc2 = oci.hasPrimitiveWrapper() ? oci.getPrimitiveWrapper() : oc;
		var oc2i = info(oc2);

			// @formatter:off
			final MethodInfo createMethod = oc2i.getPublicMethod(
				x -> x.isStatic()
				&& x.isNotDeprecated()
				&& x.hasReturnType(oc2)
				&& x.hasParameterTypes(ic)
				&& (x.hasName("forName") || isStaticCreateMethodName(x, ic))
			).orElse(null);
			// @formatter:on

			if (oc2.isEnum() && createMethod == null) {
				return new Mutater<String,Object>() {
					@Override
					public Object mutate(Object outer, String in) {
						return Enum.valueOf((Class<? extends Enum>)oc2, in);
					}
				};
			}

			if (nn(createMethod)) {
				return new Mutater<String,Object>() {
					@Override
					public Object mutate(Object outer, String in) {
						try {
							return createMethod.invoke(null, in);
						} catch (Exception e) {
							throw toRex(e);
						}
					}
				};
			}
		} else {
			// @formatter:off
			MethodInfo createMethod = oci.getPublicMethod(
				x -> x.isStatic()
				&& x.isNotDeprecated()
				&& x.hasReturnType(oc)
				&& x.hasParameterTypes(ic)
				&& isStaticCreateMethodName(x, ic)
			).orElse(null);
			// @formatter:on

			if (nn(createMethod)) {
				Method cm = createMethod.inner();
				return new Mutater() {
					@Override
					public Object mutate(Object context, Object in) {
						try {
							return cm.invoke(null, in);
						} catch (Exception e) {
							throw toRex(e);
						}
					}
				};
			}
		}

		ConstructorInfo c = oci.getPublicConstructor(x -> x.hasParameterTypes(ic)).orElse(null);
		if (nn(c) && c.isNotDeprecated()) {
			boolean isMemberClass = oci.isNonStaticMemberClass();
			return new Mutater() {
				@Override
				public Object mutate(Object outer, Object in) {
					try {
						if (isMemberClass)
							return c.newInstance(outer, in);
						return c.newInstance(in);
					} catch (Exception e) {
						throw toRex(e);
					}
				}
			};
		}

		MethodInfo toXMethod = findToXMethod(ici, oci);
		if (nn(toXMethod)) {
			return new Mutater() {
				@Override
				public Object mutate(Object outer, Object in) {
					try {
						return toXMethod.invoke(in);
					} catch (Exception e) {
						throw toRex(e);
					}
				}
			};
		}

		return NULL;
	}

	private static MethodInfo findToXMethod(ClassInfo ic, ClassInfo oc) {
		String tn = oc.getNameReadable();
		// @formatter:off
		return ic.getPublicMethod(
			x -> x.isNotStatic()
			&& x.getParameterCount() == 0
			&& x.getSimpleName().startsWith("to")
			&& x.getSimpleName().substring(2).equalsIgnoreCase(tn)
		).orElse(null);
		// @formatter:on
	}

	private static boolean isStaticCreateMethodName(MethodInfo mi, Class<?> ic) {
		var n = mi.getSimpleName();
		var cn = ic.getSimpleName();
		// @formatter:off
		return isOneOf(n, "create","from","fromValue","parse","valueOf","builder")
			|| (n.startsWith("from") && n.substring(4).equals(cn))
			|| (n.startsWith("for") && n.substring(3).equals(cn))
			|| (n.startsWith("parse") && n.substring(5).equals(cn));
		// @formatter:on
	}
}