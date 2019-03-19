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
package org.apache.juneau.internal;

import java.util.concurrent.*;

import org.apache.juneau.reflection.*;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ClassFlags.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Cache of object that convert POJOs to and from common types such as strings, readers, and input streams.
 */
public class TransformCache {
	private static final ConcurrentHashMap<Class<?>,Map<Class<?>,Transform<?,?>>> CACHE = new ConcurrentHashMap<>();

	/**
	 * Represents a non-existent transform.
	 */
	public static final Transform<Object,Object> NULL = new Transform<Object,Object>() {
		@Override
		public Object transform(Object outer, Object in) {
			return null;
		}
	};

	// Special cases.
	static {

		// TimeZone doesn't follow any standard conventions.
		add(String.class, TimeZone.class,
			new Transform<String,TimeZone>() {
				@Override public TimeZone transform(Object outer, String in) {
					return TimeZone.getTimeZone(in);
				}
			}
		);
		add(TimeZone.class, String.class,
			new Transform<TimeZone,String>() {
				@Override public String transform(Object outer, TimeZone in) {
					return in.getID();
				}
			}
		);

		// Locale(String) doesn't work on strings like "ja_JP".
		add(String.class, Locale.class,
			new Transform<String,Locale>() {
				@Override
				public Locale transform(Object outer, String in) {
					return Locale.forLanguageTag(in.replace('_', '-'));
				}
			}
		);

		// String-to-Boolean transform should allow for "null" keyword.
		add(String.class, Boolean.class,
			new Transform<String,Boolean>() {
				@Override
				public Boolean transform(Object outer, String in) {
					if (in == null || "null".equals(in) || in.isEmpty())
						return null;
					return Boolean.valueOf(in);
				}
			}
		);
	}

	/**
	 * Adds a transform for the specified input/output types.
	 *
	 * @param ic The input type.
	 * @param oc The output type.
	 * @param t The transform for converting the input to the output.
	 */
	public static synchronized void add(Class<?> ic, Class<?> oc, Transform<?,?> t) {
		Map<Class<?>,Transform<?,?>> m = CACHE.get(oc);
		if (m == null) {
			m = new ConcurrentHashMap<>();
			CACHE.put(oc, m);
		}
		m.put(ic, t);
	}

	/**
	 * Returns the transform for converting the specified input type to the specified output type.
	 *
	 * @param ic The input type.
	 * @param oc The output type.
	 * @return The transform for performing the conversion, or <jk>null</jk> if the conversion cannot be made.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <I,O> Transform<I,O> get(final Class<I> ic, final Class<O> oc) {

		if (ic == null || oc == null)
			return null;

		Map<Class<?>,Transform<?,?>> m = CACHE.get(oc);
		if (m == null) {
			m = new ConcurrentHashMap<>();
			CACHE.putIfAbsent(oc, m);
			m = CACHE.get(oc);
		}

		Transform t = m.get(ic);
		if (t != null)
			return t == NULL ? null : t;

		ClassInfo ici = ClassInfo.lookup(ic), oci = ClassInfo.lookup(oc);

		for (ClassInfo pic : ici.getParentClassesAndInterfaces()) {
			t = m.get(pic.getInnerClass());
			if (t != null) {
				m.put(pic.getInnerClass(), t);
				return t == NULL ? null : t;
			}
		}

		if (ic == oc) {
			t = new Transform<I,O>() {
				@Override public O transform(Object outer, I in) {
					return (O)in;
				}
			};
		} else if (ic == String.class) {
			final Class<?> oc2 = hasPrimitiveWrapper(oc) ? getPrimitiveWrapper(oc) : oc;
			ClassInfo oc2i = ClassInfo.lookup(oc2);
			if (oc2.isEnum()) {
				t = new Transform<String,O>() {
					@Override
					public O transform(Object outer, String in) {
						return (O)Enum.valueOf((Class<? extends Enum>)oc2, in);
					}
				};
			} else {
				final MethodInfo fromStringMethod = oc2i.findPublicFromStringMethod();
				if (fromStringMethod != null) {
					t = new Transform<String,O>() {
						@Override
						public O transform(Object outer, String in) {
							try {
								return (O)fromStringMethod.invoke(null, in);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					};
				}
			}
		}

		if (t == null) {
			MethodInfo createMethod = oci.findPublicStaticCreateMethod(ic, "create");
			if (createMethod == null)
				createMethod = oci.findPublicStaticCreateMethod(ic, "from" + ic.getSimpleName());
			if (createMethod != null) {
				final Method cm = createMethod.getInner();
				t = new Transform<I,O>() {
					@Override
					public O transform(Object context, I in) {
						try {
							return (O)cm.invoke(null, in);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
			} else {
				final Constructor<?> c = findPublicConstructor(oc, ic);
				final boolean isMemberClass = oc.isMemberClass() && ! isStatic(oc);
				if (c != null && ! c.isAnnotationPresent(Deprecated.class)) {
					t = new Transform<I,O>() {
						@Override
						public O transform(Object outer, I in) {
							try {
								if (isMemberClass)
									return (O)c.newInstance(outer, in);
								return (O)c.newInstance(in);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					};
				}

			}
		}

		if (t == null) {
			for (MethodInfo m2 : ici.getAllMethods()) {
				if (m2.isAll(PUBLIC, NOT_STATIC, HAS_NO_ARGS, NOT_DEPRECATED) && m2.getName().startsWith("to") && m2.hasReturnType(oc)) {
					final Method m3 = m2.getInner();
					t = new Transform<I,O>() {
						@Override
						public O transform(Object outer, I in) {
							try {
								return (O)m3.invoke(in);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					};
					break;
				}
			}
		}
		if (t == null)
			t = NULL;

		m.put(ic, t);

		return t == NULL ? null : t;
	}
}
