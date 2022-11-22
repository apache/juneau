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

import java.lang.reflect.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;

/**
 * Class-related utility methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class ClassUtils {

	/**
	 * Predicate check to filter out void classes.
	 */
	public static final Predicate<Class<?>> NOT_VOID = x -> isNotVoid(x);

	/**
	 * Returns the class types for the specified arguments.
	 *
	 * @param args The objects we're getting the classes of.
	 * @return The classes of the arguments.
	 */
	public static Class<?>[] getClasses(Object...args) {
		Class<?>[] pt = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++)
			pt[i] = args[i] == null ? null : args[i].getClass();
		return pt;
	}

	/**
	 * Matches arguments to a list of parameter types.
	 *
	 * <p>
	 * Extra parameters are ignored.
	 * <br>Missing parameters are left null.
	 *
	 * @param paramTypes The parameter types.
	 * @param args The arguments to match to the parameter types.
	 * @return
	 * 	An array of parameters.
	 */
	public static Object[] getMatchingArgs(Class<?>[] paramTypes, Object... args) {
		boolean needsShuffle = paramTypes.length != args.length;
		if (! needsShuffle) {
			for (int i = 0; i < paramTypes.length; i++) {
				if (! paramTypes[i].isInstance(args[i]))
					needsShuffle = true;
			}
		}
		if (! needsShuffle)
			return args;
		Object[] params = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			ClassInfo pt = ClassInfo.of(paramTypes[i]).getWrapperInfoIfPrimitive();
			for (int j = 0; j < args.length; j++) {
				if (args[j] != null && pt.isParentOf(args[j].getClass())) {
					params[i] = args[j];
					break;
				}
			}
		}
		return params;
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The constructor.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Constructor<?> x) {
		try {
			if (x != null)
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The method.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Method x) {
		try {
			if (x != null)
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The field.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Field x) {
		try {
			if (x != null)
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Returns the specified type as a <c>Class</c>.
	 *
	 * <p>
	 * If it's already a <c>Class</c>, it just does a cast.
	 * <br>If it's a <c>ParameterizedType</c>, it returns the raw type.
	 *
	 * @param t The type to convert.
	 * @return The type converted to a <c>Class</c>, or <jk>null</jk> if it could not be converted.
	 */
	public static Class<?> toClass(Type t) {
		if (t instanceof Class)
			return (Class<?>)t;
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			// The raw type should always be a class (right?)
			return (Class<?>)pt.getRawType();
		}
		return null;
	}

	/**
	 * Returns the fully-qualified class name for the specified object.
	 *
	 * @param value The object to get the class name for.
	 * @return The name of the class or <jk>null</jk> if the value was null.
	 */
	public static String className(Object value) {
		return value == null ? null : value instanceof Class<?> ? ((Class<?>)value).getName() : value.getClass().getName();
	}

	/**
	 * Returns the simple class name for the specified object.
	 *
	 * @param value The object to get the class name for.
	 * @return The name of the class or <jk>null</jk> if the value was null.
	 */
	public static String simpleClassName(Object value) {
		if (value == null)
			return null;
		if (value instanceof ClassInfo)
			return ((ClassInfo)value).getSimpleName();
		if (value instanceof ClassMeta)
			return ((ClassMeta<?>)value).getSimpleName();
		if (value instanceof Class)
			return ((Class<?>)value).getSimpleName();
		return value.getClass().getSimpleName();
	}

	/**
	 * Returns <jk>true</jk> if the specific class is <jk>null</jk> or <c><jk>void</jk>.<jk>class</jk></c> or {@link Void} or has the simple name <js>"Void</js>.
	 *
	 * @param c The class to check.
	 * @return <jk>true</jk> if the specific class is <jk>null</jk> or <c><jk>void</jk>.<jk>class</jk></c> or {@link Void} or has the simple name <js>"Void</js>.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isVoid(Class c) {
		return (c == null || c == void.class || c == Void.class || c.getSimpleName().equalsIgnoreCase("void"));
	}

	/**
	 * Returns <jk>false</jk> if the specific class is <jk>null</jk> or <c><jk>void</jk>.<jk>class</jk></c> or {@link Void} or has the simple name <js>"Void</js>.
	 *
	 * @param c The class to check.
	 * @return <jk>false</jk> if the specific class is <jk>null</jk> or <c><jk>void</jk>.<jk>class</jk></c> or {@link Void} or has the simple name <js>"Void</js>.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isNotVoid(Class c) {
		return ! isVoid(c);
	}
}
