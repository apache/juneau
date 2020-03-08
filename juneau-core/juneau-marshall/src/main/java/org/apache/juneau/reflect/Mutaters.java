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
package org.apache.juneau.reflect;

import static org.apache.juneau.reflect.ReflectFlags.*;

import java.util.concurrent.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Cache of object that convert POJOs to and from common types such as strings, readers, and input streams.
 */
public class Mutaters {
	private static final ConcurrentHashMap<Class<?>,Map<Class<?>,Mutater<?,?>>> CACHE = new ConcurrentHashMap<>();

	/**
	 * Represents a non-existent transform.
	 */
	public static final Mutater<Object,Object> NULL = new Mutater<Object,Object>() {
		@Override
		public Object mutate(Object outer, Object in) {
			return null;
		}
	};

	// Special cases.
	static {

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
	 * Returns the transform for converting the specified input type to the specified output type.
	 *
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
	 * @param ic The input type.
	 * @param oc The output type.
	 * @return The transform for performing the conversion, or <jk>null</jk> if the conversion cannot be made.
	 */
	public static <I,O> boolean hasMutate(Class<I> ic, Class<O> oc) {
		return get(ic, oc) != NULL;
	}

	@SuppressWarnings({"unchecked","rawtypes"})
	private static Mutater find(Class<?> ic, Class<?> oc, Map<Class<?>,Mutater<?,?>> m) {

		if (ic == oc) {
			return new Mutater() {
				@Override public Object mutate(Object outer, Object in) {
					return in;
				}
			};
		}

		ClassInfo ici = ClassInfo.of(ic), oci = ClassInfo.of(oc);

		for (ClassInfo pic : ici.getAllParentsChildFirst()) {
			Mutater t = m.get(pic.inner());
			if (t != null)
				return t;
		}

		if (ic == String.class) {
			Class<?> oc2 = oci.hasPrimitiveWrapper() ? oci.getPrimitiveWrapper() : oc;
			ClassInfo oc2i = ClassInfo.of(oc2);

			final MethodInfo createMethod = oc2i.getStaticCreateMethod(ic, "forName");

			if (oc2.isEnum() && createMethod == null) {
				return new Mutater<String,Object>() {
					@Override
					public Object mutate(Object outer, String in) {
						return Enum.valueOf((Class<? extends Enum>)oc2, in);
					}
				};
			}

			if (createMethod != null) {
				return new Mutater<String,Object>() {
					@Override
					public Object mutate(Object outer, String in) {
						try {
							return createMethod.invoke(null, in);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
			}
		} else {
			MethodInfo createMethod = oci.getStaticCreateMethod(ic);
			if (createMethod != null) {
				Method cm = createMethod.inner();
				return new Mutater() {
					@Override
					public Object mutate(Object context, Object in) {
						try {
							return cm.invoke(null, in);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
			}
		}

		ConstructorInfo c = oci.getPublicConstructor(ic);
		if (c != null && c.isNotDeprecated()) {
			boolean isMemberClass = oci.isNonStaticMemberClass();
			return new Mutater() {
				@Override
				public Object mutate(Object outer, Object in) {
					try {
						if (isMemberClass)
							return c.invoke(outer, in);
						return c.invoke(in);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
		}

		MethodInfo toXMethod = findToXMethod(ici, oci);
		if (toXMethod != null) {
			return new Mutater() {
				@Override
				public Object mutate(Object outer, Object in) {
					try {
						return toXMethod.invoke(in);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
		}

		return NULL;
	}

	private static MethodInfo findToXMethod(ClassInfo ic, ClassInfo oc) {
		String tn = oc.getReadableName();
		for (MethodInfo m : ic.getAllMethods()) {
			if (m.isAll(PUBLIC, NOT_STATIC, HAS_NO_PARAMS, NOT_DEPRECATED)
				&& m.getSimpleName().startsWith("to")
				&& m.getSimpleName().substring(2).equalsIgnoreCase(tn))
				return m;
		}
		return null;
	}

}
