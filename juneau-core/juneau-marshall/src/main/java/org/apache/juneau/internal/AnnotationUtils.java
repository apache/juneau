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

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.reflect.*;

/**
 * Annotation utilities.
 */
public class AnnotationUtils {

	/**
	 * Checks if two annotations are equal using the criteria for equality presented in the {@link Annotation#equals(Object)} API docs.
	 *
	 * @param a1 the first Annotation to compare, {@code null} returns {@code false} unless both are {@code null}
	 * @param a2 the second Annotation to compare, {@code null} returns {@code false} unless both are {@code null}
	 * @return {@code true} if the two annotations are {@code equal} or both {@code null}
	 */
	public static boolean equals(Annotation a1, Annotation a2) {
		if (a1 == a2)
			return true;
		if (a1 == null || a2 == null)
			return false;

		Class<? extends Annotation> t1 = a1.annotationType();
		Class<? extends Annotation> t2 = a2.annotationType();

		if (! t1.equals(t2))
			return false;

		try {
			for (Method m : getAnnotationMethods(t1)) {
				Object v1 = m.invoke(a1);
				Object v2 = m.invoke(a2);
				if (! memberEquals(m.getReturnType(), v1, v2))
					return false;
			}
		} catch (IllegalAccessException ex) {
			return false;
		} catch (InvocationTargetException ex) {
			return false;
		}
		return true;
	}

	/**
	 * Generate a hash code for the given annotation using the algorithm presented in the {@link Annotation#hashCode()} API docs.
	 *
	 * @param a the Annotation for a hash code calculation is desired, not {@code null}
	 * @return the calculated hash code
	 * @throws RuntimeException if an {@code Exception} is encountered during annotation member access
	 * @throws IllegalStateException if an annotation method invocation returns {@code null}
	 */
	public static int hashCode(Annotation a) {
		int result = 0;
		Class<? extends Annotation> t = a.annotationType();

		for (Method m : getAnnotationMethods(t)) {
			try {
				Object value = m.invoke(a);
				if (value == null)
					throw new IllegalStateException(String.format("Annotation method %s returned null", m));
				result += hashMember(m.getName(), value);
			} catch (RuntimeException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		return result;
	}

	/**
	 * Returns the methods on the specified annotation type.
	 *
	 * @param type The annotation type.
	 * @return The methods on the specified annotation type.
	 */
	public static List<Method> getAnnotationMethods(Class<? extends Annotation> type) {
		return Arrays.asList(type.getDeclaredMethods())
			.stream()
			.filter(x -> x.getParameterCount() == 0 && x.getDeclaringClass().isAnnotation())
			.collect(Collectors.toList())
		;
	}

	/**
	 * Returns the methods on the specified annotation type ordered by method name.
	 *
	 * @param type The annotation type.
	 * @return The methods on the specified annotation typev.
	 */
	public static List<Method> getSortedAnnotationMethods(Class<? extends Annotation> type) {
		return Arrays.asList(type.getDeclaredMethods())
			.stream()
			.filter(x->x.getParameterCount() == 0 && x.getDeclaringClass().isAnnotation())
			.sorted(Comparator.comparing(Method::getName))
			.collect(Collectors.toList());
	}

	private static int hashMember(String name, Object value) {
		int part1 = name.hashCode() * 127;
		if (value.getClass().isArray())
			return part1 ^ arrayMemberHash(value.getClass().getComponentType(), value);
		if (value instanceof Annotation)
			return part1 ^ hashCode((Annotation) value);
		return part1 ^ value.hashCode();
	}

	private static boolean memberEquals(Class<?> type, Object o1, Object o2) {
		if (o1 == o2)
			return true;
		if (o1 == null || o2 == null)
			return false;
		if (type.isArray())
			return arrayMemberEquals(type.getComponentType(), o1, o2);
		if (type.isAnnotation())
			return equals((Annotation) o1, (Annotation) o2);
		return o1.equals(o2);
	}

	private static boolean arrayMemberEquals(Class<?> componentType, Object o1, Object o2) {
		if (componentType.isAnnotation())
			return annotationArrayMemberEquals((Annotation[]) o1, (Annotation[]) o2);
		if (componentType.equals(Byte.TYPE))
			return Arrays.equals((byte[]) o1, (byte[]) o2);
		if (componentType.equals(Short.TYPE))
			return Arrays.equals((short[]) o1, (short[]) o2);
		if (componentType.equals(Integer.TYPE))
			return Arrays.equals((int[]) o1, (int[]) o2);
		if (componentType.equals(Character.TYPE))
			return Arrays.equals((char[]) o1, (char[]) o2);
		if (componentType.equals(Long.TYPE))
			return Arrays.equals((long[]) o1, (long[]) o2);
		if (componentType.equals(Float.TYPE))
			return Arrays.equals((float[]) o1, (float[]) o2);
		if (componentType.equals(Double.TYPE))
			return Arrays.equals((double[]) o1, (double[]) o2);
		if (componentType.equals(Boolean.TYPE))
			return Arrays.equals((boolean[]) o1, (boolean[]) o2);
		return Arrays.equals((Object[]) o1, (Object[]) o2);
	}

	private static boolean annotationArrayMemberEquals(Annotation[] a1, Annotation[] a2) {
		if (a1.length != a2.length)
			return false;
		for (int i = 0; i < a1.length; i++)
			if (! equals(a1[i], a2[i]))
				return false;
		return true;
	}

	private static int arrayMemberHash(Class<?> componentType, Object o) {
		if (componentType.equals(Byte.TYPE))
			return Arrays.hashCode((byte[]) o);
		if (componentType.equals(Short.TYPE))
			return Arrays.hashCode((short[]) o);
		if (componentType.equals(Integer.TYPE))
			return Arrays.hashCode((int[]) o);
		if (componentType.equals(Character.TYPE))
			return Arrays.hashCode((char[]) o);
		if (componentType.equals(Long.TYPE))
			return Arrays.hashCode((long[]) o);
		if (componentType.equals(Float.TYPE))
			return Arrays.hashCode((float[]) o);
		if (componentType.equals(Double.TYPE))
			return Arrays.hashCode((double[]) o);
		if (componentType.equals(Boolean.TYPE))
			return Arrays.hashCode((boolean[]) o);
		return Arrays.hashCode((Object[]) o);
	}

	/**
	 * If the annotation is an array of other annotations, returns the inner annotations.
	 * 
	 * @param a The annotation to split if repeated.
	 * @return The nested annotations, or a singleton array of the same annotation if it's not repeated.
	 */
	public static Annotation[] splitRepeated(Annotation a) {
		try {
			ClassInfo ci = ClassInfo.ofc(a.annotationType());
			MethodInfo mi = ci.getRepeatedAnnotationMethod();
			if (mi != null)
				return mi.invoke(a);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Annotation[]{a};
	}
}
