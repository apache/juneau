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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.collections.*;

/**
 * Utility methods for working with classes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ClassUtils {
	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The constructor.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Constructor<?> x) {
		try {
			if (nn(x))
				x.setAccessible(true);
			return true;
		} catch (@SuppressWarnings("unused") SecurityException e) {
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
			if (nn(x))
				x.setAccessible(true);
			return true;
		} catch (@SuppressWarnings("unused") SecurityException e) {
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
			if (nn(x))
				x.setAccessible(true);
			return true;
		} catch (@SuppressWarnings("unused") SecurityException e) {
			return false;
		}
	}

	/**
	 * Returns the generic parameter type of the Value type.
	 *
	 * @param t The type to find the parameter type of.
	 * @return The parameter type of the value, or <jk>null</jk> if the type is not a subclass of <c>Value</c>.
	 */
	public static Type getParameterType(Type t) {
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			if (pt.getRawType() == Value.class) {
				Type[] ta = pt.getActualTypeArguments();
				if (ta.length > 0)
					return ta[0];
			}
		} else if (t instanceof Class) {
			Class<?> c = (Class<?>)t;
			if (Value.class.isAssignableFrom(c)) {
				return ClassUtils.getParameterType(c, 0, Value.class);
			}
		}

		return null;
	}

	/**
	 * Returns the generic parameter type at the specified index for a class that extends a parameterized type.
	 *
	 * @param c The class to examine.
	 * @param index The zero-based index of the parameter to retrieve.
	 * @param pt The parameterized superclass or interface.
	 * @return The parameter type at the specified index.
	 * @throws IllegalArgumentException If the class is not a subclass of the parameterized type or if the index is invalid.
	 */
	public static Class<?> getParameterType(Class<?> c, int index, Class<?> pt) {
		AssertionUtils.assertArgNotNull("pt", pt);
		AssertionUtils.assertArgNotNull("c", c);

		// We need to make up a mapping of type names.
		Map<Type,Type> typeMap = new HashMap<>();
		Class<?> cc = c;
		while (pt != cc.getSuperclass()) {
			extractTypes(typeMap, cc);
			cc = cc.getSuperclass();
			AssertionUtils.assertArg(nn(cc), "Class ''{0}'' is not a subclass of parameterized type ''{1}''", c.getSimpleName(), pt.getSimpleName());
		}

		Type gsc = cc.getGenericSuperclass();

		AssertionUtils.assertArg(gsc instanceof ParameterizedType, "Class ''{0}'' is not a parameterized type", pt.getSimpleName());

		ParameterizedType cpt = (ParameterizedType)gsc;
		Type[] atArgs = cpt.getActualTypeArguments();
		AssertionUtils.assertArg(index < atArgs.length, "Invalid type index. index={0}, argsLength={1}", index, atArgs.length);
		Type actualType = cpt.getActualTypeArguments()[index];

		if (typeMap.containsKey(actualType))
			actualType = typeMap.get(actualType);

		if (actualType instanceof Class) {
			return (Class<?>)actualType;

		} else if (actualType instanceof GenericArrayType) {
			Type gct = ((GenericArrayType)actualType).getGenericComponentType();
			if (gct instanceof ParameterizedType)
				return Array.newInstance((Class<?>)((ParameterizedType)gct).getRawType(), 0).getClass();
		} else if (actualType instanceof TypeVariable) {
			TypeVariable<?> typeVariable = (TypeVariable<?>)actualType;
			List<Class<?>> nestedOuterTypes = new LinkedList<>();
			for (Class<?> ec = cc.getEnclosingClass(); nn(ec); ec = ec.getEnclosingClass()) {
				Class<?> outerClass = cc.getClass();
				nestedOuterTypes.add(outerClass);
				Map<Type,Type> outerTypeMap = new HashMap<>();
				extractTypes(outerTypeMap, outerClass);
				for (Map.Entry<Type,Type> entry : outerTypeMap.entrySet()) {
					Type key = entry.getKey(), value = entry.getValue();
					if (key instanceof TypeVariable) {
						TypeVariable<?> keyType = (TypeVariable<?>)key;
						if (keyType.getName().equals(typeVariable.getName()) && isInnerClass(keyType.getGenericDeclaration(), typeVariable.getGenericDeclaration())) {
							if (value instanceof Class)
								return (Class<?>)value;
							typeVariable = (TypeVariable<?>)entry.getValue();
						}
					}
				}
			}
		} else if (actualType instanceof ParameterizedType) {
			return (Class<?>)((ParameterizedType)actualType).getRawType();
		}
		throw new IllegalArgumentException("Could not resolve variable '" + actualType.getTypeName() + "' to a type.");
	}

	private static void extractTypes(Map<Type,Type> typeMap, Class<?> c) {
		Type gs = c.getGenericSuperclass();
		if (gs instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)gs;
			Type[] typeParameters = ((Class<?>)pt.getRawType()).getTypeParameters();
			Type[] actualTypeArguments = pt.getActualTypeArguments();
			for (int i = 0; i < typeParameters.length; i++) {
				if (typeMap.containsKey(actualTypeArguments[i]))
					actualTypeArguments[i] = typeMap.get(actualTypeArguments[i]);
				typeMap.put(typeParameters[i], actualTypeArguments[i]);
			}
		}
	}

	private static boolean isInnerClass(GenericDeclaration od, GenericDeclaration id) {
		if (od instanceof Class && id instanceof Class) {
			Class<?> oc = (Class<?>)od;
			Class<?> ic = (Class<?>)id;
			while (nn(ic = ic.getEnclosingClass()))
				if (ic == oc)
					return true;
		}
		return false;
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
	 * Predicate check to filter out void classes.
	 */
	public static final Predicate<Class<?>> NOT_VOID = ClassUtils::isNotVoid;

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
	 * Shortcut for calling {@link #className(Object)}.
	 *
	 * @param value The object to get the class name for.
	 * @return The name of the class or <jk>null</jk> if the value was null.
	 */
	public static String cn(Object value) {
		return className(value);
	}

	/**
	 * Returns the simple (non-qualified) class name for the specified object.
	 *
	 * @param value The object to get the simple class name for.
	 * @return The simple name of the class or <jk>null</jk> if the value was null.
	 */
	public static String simpleClassName(Object value) {
		return value == null ? null : value instanceof Class<?> ? ((Class<?>)value).getSimpleName() : value.getClass().getSimpleName();
	}

	/**
	 * Shortcut for calling {@link #simpleClassName(Object)}.
	 *
	 * @param value The object to get the simple class name for.
	 * @return The simple name of the class or <jk>null</jk> if the value was null.
	 */
	public static String scn(Object value) {
		return simpleClassName(value);
	}

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
	 * Returns <jk>false</jk> if the specific class is <jk>null</jk> or <c><jk>void</jk>.<jk>class</jk></c> or {@link Void} or has the simple name <js>"Void</js>.
	 *
	 * @param c The class to check.
	 * @return <jk>false</jk> if the specific class is <jk>null</jk> or <c><jk>void</jk>.<jk>class</jk></c> or {@link Void} or has the simple name <js>"Void</js>.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isNotVoid(Class c) {
		return ! ClassUtils.isVoid(c);
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
}
