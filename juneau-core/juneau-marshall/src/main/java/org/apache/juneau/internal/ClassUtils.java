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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.reflection.*;
import org.apache.juneau.utils.*;

/**
 * Class-related utility methods.
 */
public final class ClassUtils {

	/**
	 * Shortcut for calling {@link ClassInfo#of(Type)}.
	 *
	 * @param t The class being wrapped.
	 * @return The wrapped class.
	 */
	public static ClassInfo getClassInfo(Type t) {
		return ClassInfo.of(t);
	}

	/**
	 * Shortcut for calling {@link ClassInfo#of(Object)}.
	 *
	 * @param o The object whose class being wrapped.
	 * @return The wrapped class.
	 */
	public static ClassInfo getClassInfo(Object o) {
		return ClassInfo.of(o);
	}

	/**
	 * Shortcut for calling {@link MethodInfo#of(Method)}.
	 *
	 * @param m The method being wrapped.
	 * @return The wrapped method.
	 */
	public static MethodInfo getMethodInfo(Method m) {
		return MethodInfo.of(m);
	}

	/**
	 * Shortcut for calling {@link FieldInfo#of(Field)}.
	 *
	 * @param f The field being wrapped.
	 * @return The wrapped field.
	 */
	public static FieldInfo getFieldInfo(Field f) {
		return FieldInfo.of(f);
	}

	/**
	 * Shortcut for calling {@link ConstructorInfo#of(Constructor)}.
	 *
	 * @param c The constructor being wrapped.
	 * @return The wrapped constructor.
	 */
	public static ConstructorInfo getConstructorInfo(Constructor<?> c) {
		return ConstructorInfo.of(c);
	}

	/**
	 * Given the specified list of objects, return readable names for the class types of the objects.
	 *
	 * @param o The objects.
	 * @return An array of readable class type strings.
	 */
	public static ObjectList getReadableClassNames(Object[] o) {
		ObjectList l = new ObjectList();
		for (int i = 0; i < o.length; i++)
			l.add(o[i] == null ? "null" : getReadableClassName(o[i].getClass()));
		return l;
	}

	/**
	 * Shortcut for calling <code><jsm>getReadableClassName</jsm>(c.getName())</code>
	 *
	 * @param c The class.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassName(Class<?> c) {
		if (c == null)
			return null;
		return getReadableClassName(c.getName());
	}

	/**
	 * Shortcut for calling <code><jsm>getReadableClassName</jsm>(c.getClass().getName())</code>
	 *
	 * @param o The object whose class we want to render.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassNameForObject(Object o) {
		if (o == null)
			return null;
		return getReadableClassName(o.getClass().getName());
	}

	/**
	 * Converts the specified class name to a readable form when class name is a special construct like <js>"[[Z"</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jsm>getReadableClassName</jsm>(<js>"java.lang.Object"</js>);  <jc>// Returns "java.lang.Object"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"boolean"</js>);  <jc>// Returns "boolean"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[Z"</js>);  <jc>// Returns "boolean[]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[[Z"</js>);  <jc>// Returns "boolean[][]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[Ljava.lang.Object;"</js>);  <jc>// Returns "java.lang.Object[]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<jk>null</jk>);  <jc>// Returns null</jc>
	 * </p>
	 *
	 * @param className The class name.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassName(String className) {
		if (className == null)
			return null;
		if (! StringUtils.startsWith(className, '['))
			return className;
		int depth = 0;
		for (int i = 0; i < className.length(); i++) {
			if (className.charAt(i) == '[')
				depth++;
			else
				break;
		}
		char type = className.charAt(depth);
		String c;
		switch (type) {
			case 'Z': c = "boolean"; break;
			case 'B': c = "byte"; break;
			case 'C': c = "char"; break;
			case 'D': c = "double"; break;
			case 'F': c = "float"; break;
			case 'I': c = "int"; break;
			case 'J': c = "long"; break;
			case 'S': c = "short"; break;
			default: c = className.substring(depth+1, className.length()-1);
		}
		StringBuilder sb = new StringBuilder(c.length() + 2*depth).append(c);
		for (int i = 0; i < depth; i++)
			sb.append("[]");
		return sb.toString();
	}

	/**
	 * Returns <jk>true</jk> if the specified argument types are valid for the specified parameter types.
	 *
	 * @param paramTypes The parameters types specified on a method.
	 * @param argTypes The class types of the arguments being passed to the method.
	 * @return <jk>true</jk> if the arguments match the parameters.
	 */
	public static boolean argsMatch(Class<?>[] paramTypes, Class<?>[] argTypes) {
		if (paramTypes.length == argTypes.length) {
			for (int i = 0; i < paramTypes.length; i++)
				if (! getClassInfo(paramTypes[i]).isParentOf(argTypes[i]))
					return false;
			return true;
		}
		return false;
	}

	/**
	 * Returns a number representing the number of arguments that match the specified parameters.
	 *
	 * @param paramTypes The parameters types specified on a method.
	 * @param argTypes The class types of the arguments being passed to the method.
	 * @return The number of matching arguments, or <code>-1</code> a parameter was found that isn't in the list of args.
	 */
	public static int fuzzyArgsMatch(Class<?>[] paramTypes, Class<?>... argTypes) {
		int matches = 0;
		outer: for (Class<?> p : paramTypes) {
			p = getClassInfo(p).getWrapperIfPrimitive();
			for (Class<?> a : argTypes) {
				if (getClassInfo(p).isParentOf(a)) {
					matches++;
					continue outer;
				}
			}
			return -1;
		}
		return matches;
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
	 * Creates an instance of the specified class.
	 *
	 * @param c
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @return
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws
	 * 	RuntimeException if constructor could not be found or called.
	 */
	public static <T> T castOrCreate(Class<T> c, Object c2) {
		return castOrCreateFromOuter(null, c, c2, false);
	}

	/**
	 * Creates an instance of the specified class.
	 *
	 * @param c
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @param fuzzyArgs
	 * 	Use fuzzy constructor arg matching.
	 * 	<br>When <jk>true</jk>, constructor args can be in any order and extra args are ignored.
	 * 	<br>No-arg constructors are also used if no other constructors are found.
	 * @param args
	 * 	The arguments to pass to the constructor.
	 * @return
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws
	 * 	RuntimeException if constructor could not be found or called.
	 */
	public static <T> T castOrCreate(Class<T> c, Object c2, boolean fuzzyArgs, Object...args) {
		return castOrCreateFromOuter(null, c, c2, fuzzyArgs, args);
	}

	/**
	 * Creates an instance of the specified class from within the context of another object.
	 *
	 * @param outer
	 * 	The outer object.
	 * 	Can be <jk>null</jk>.
	 * @param c
	 * 	The class to cast to.
	 * @param c2
	 * 	The class to instantiate.
	 * 	Can also be an instance of the class.
	 * @param fuzzyArgs
	 * 	Use fuzzy constructor arg matching.
	 * 	<br>When <jk>true</jk>, constructor args can be in any order and extra args are ignored.
	 * 	<br>No-arg constructors are also used if no other constructors are found.
	 * @param args
	 * 	The arguments to pass to the constructor.
	 * @return
	 * 	The new class instance, or <jk>null</jk> if the class was <jk>null</jk> or is abstract or an interface.
	 * @throws
	 * 	RuntimeException if constructor could not be found or called.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T castOrCreateFromOuter(Object outer, Class<T> c, Object c2, boolean fuzzyArgs, Object...args) {
		if (c2 == null)
			return null;
		if (c2 instanceof Class) {
			try {
				ClassInfo c3 = getClassInfo((Class<?>)c2);
				if (c3.isInterface() || c3.isAbstract())
					return null;

				// First look for an exact match.
				Constructor<?> con = c3.findPublicConstructor(false, args);
				if (con != null)
					return (T)con.newInstance(args);

				// Next look for an exact match including the outer.
				if (outer != null) {
					args = new AList<>().append(outer).appendAll(args).toArray();
					con = c3.findPublicConstructor(false, args);
					if (con != null)
						return (T)con.newInstance(args);
				}

				// Finally use fuzzy matching.
				if (fuzzyArgs) {
					con = c3.findPublicConstructor(true, args);
					if (con != null)
						return (T)con.newInstance(getMatchingArgs(con.getParameterTypes(), args));
				}

				throw new FormattedRuntimeException("Could not instantiate class {0}/{1}.  Constructor not found.", c.getName(), c2);
			} catch (Exception e) {
				throw new FormattedRuntimeException(e, "Could not instantiate class {0}", c.getName());
			}
		} else if (getClassInfo(c).isParentOf(c2.getClass())) {
			return (T)c2;
		} else {
			throw new FormattedRuntimeException("Object of type {0} found but was expecting {1}.", c2.getClass(), c.getClass());
		}
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
		Object[] params = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			ClassInfo pt = getClassInfo(paramTypes[i]).getWrapperInfoIfPrimitive();
			for (int j = 0; j < args.length; j++) {
				if (pt.isParentOf(args[j].getClass())) {
					params[i] = args[j];
					break;
				}
			}
		}
		return params;
	}

	/**
	 * Returns a list of all the parent classes of the specified class including the class itself.
	 *
	 * @param c The class to retrieve the parent classes.
	 * @param parentFirst In parent-to-child order, otherwise child-to-parent.
	 * @param includeInterfaces Include interfaces.
	 * @return An iterator of parent classes in the class hierarchy.
	 */
	public static Iterator<Class<?>> getParentClasses(final Class<?> c, boolean parentFirst, boolean includeInterfaces) {
		List<Class<?>> l = getParentClasses(new ArrayList<Class<?>>(), c, parentFirst, includeInterfaces);
		return l.iterator();
	}

	private static List<Class<?>> getParentClasses(List<Class<?>> l, Class<?> c, boolean parentFirst, boolean includeInterfaces) {
		if (parentFirst) {
			if (includeInterfaces)
				for (Class<?> i : c.getInterfaces())
					l.add(i);
			if (c.getSuperclass() != Object.class && c.getSuperclass() != null)
				getParentClasses(l, c.getSuperclass(), parentFirst, includeInterfaces);
			l.add(c);
		} else {
			l.add(c);
			if (c.getSuperclass() != Object.class && c.getSuperclass() != null)
				getParentClasses(l, c.getSuperclass(), parentFirst, includeInterfaces);
			if (includeInterfaces)
				for (Class<?> i : c.getInterfaces())
					l.add(i);
		}
		return l;
	}

	/**
	 * Returns a readable representation of the specified method.
	 *
	 * <p>
	 * The format of the string is <js>"full-qualified-class.method-name(parameter-simple-class-names)"</js>.
	 *
	 * @param m The method to stringify.
	 * @return The stringified method.
	 */
	public static String toString(Method m) {
		StringBuilder sb = new StringBuilder(m.getDeclaringClass().getName() + "." + m.getName() + "(");
		for (int i = 0; i < m.getParameterTypes().length; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(m.getParameterTypes()[i].getSimpleName());
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Returns a readable representation of the specified field.
	 *
	 * <p>
	 * The format of the string is <js>"full-qualified-class.field-name"</js>.
	 *
	 * @param f The field to stringify.
	 * @return The stringified field.
	 */
	public static String toString(Field f) {
		return f.getDeclaringClass().getName() + "." + f.getName();
	}

	/**
	 * Constructs a new instance of the specified class from the specified string.
	 *
	 * <p>
	 * Class must be one of the following:
	 * <ul>
	 * 	<li>Have a public constructor that takes in a single <code>String</code> argument.
	 * 	<li>Have a static <code>fromString(String)</code> (or related) method.
	 * 		<br>See {@link ClassInfo#findPublicFromStringMethod()} for the list of possible static method names.
	 * 	<li>Be an <code>enum</code>.
	 * </ul>
	 *
	 * @param c The class.
	 * @param s The string to create the instance from.
	 * @return A new object instance, or <jk>null</jk> if a method for converting the string to an object could not be found.
	 */
	public static <T> T fromString(Class<T> c, String s) {
		Transform<String,T> t = TransformCache.get(String.class, c);
		return t == null ? null : t.transform(s);
	}

	/**
	 * Converts an object to a string.
	 *
	 * <p>
	 * Normally, this is just going to call <code>toString()</code> on the object.
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
		Transform<Object,String> t = (Transform<Object,String>)TransformCache.get(o.getClass(), String.class);
		return t == null ? o.toString() : t.transform(o);
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The constructor.
	 * @param ignoreExceptions Ignore {@link SecurityException SecurityExceptions} and just return <jk>false</jk> if thrown.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Constructor<?> x, boolean ignoreExceptions) {
		try {
			if (! (x == null || x.isAccessible()))
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			if (ignoreExceptions)
				return false;
			throw new ClassMetaRuntimeException("Could not set accessibility to true on constructor ''{0}''", x);
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The method.
	 * @param ignoreExceptions Ignore {@link SecurityException SecurityExceptions} and just return <jk>false</jk> if thrown.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Method x, boolean ignoreExceptions) {
		try {
			if (! (x == null || x.isAccessible()))
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			if (ignoreExceptions)
				return false;
			throw new ClassMetaRuntimeException("Could not set accessibility to true on method ''{0}''", x);
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The field.
	 * @param ignoreExceptions Ignore {@link SecurityException SecurityExceptions} and just return <jk>false</jk> if thrown.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Field x, boolean ignoreExceptions) {
		try {
			if (! (x == null || x.isAccessible()))
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			if (ignoreExceptions)
				return false;
			throw new ClassMetaRuntimeException("Could not set accessibility to true on field ''{0}''", x);
		}
	}

	/**
	 * Returns the simple name of a class.
	 *
	 * <p>
	 * Similar to {@link Class#getSimpleName()}, but includes the simple name of an enclosing or declaring class.
	 *
	 * @param c The class to get the simple name on.
	 * @return The simple name of a class.
	 */
	public static String getSimpleName(Class<?> c) {
		if (c.isLocalClass())
			return getSimpleName(c.getEnclosingClass()) + '.' + c.getSimpleName();
		if (c.isMemberClass())
			return getSimpleName(c.getDeclaringClass()) + '.' + c.getSimpleName();
		return c.getSimpleName();
	}

	/**
	 * Returns the simple name of a class.
	 *
	 * <p>
	 * Similar to {@link Class#getSimpleName()}, but includes the simple name of an enclosing or declaring class.
	 *
	 * @param t The class to get the simple name on.
	 * @return The simple name of a class.
	 */
	public static String getSimpleName(Type t) {
		if (t instanceof Class)
			return getSimpleName((Class<?>)t);
		if (t instanceof ParameterizedType) {
			StringBuilder sb = new StringBuilder();
			ParameterizedType pt = (ParameterizedType)t;
			sb.append(getSimpleName(pt.getRawType()));
			sb.append("<");
			boolean first = true;
			for (Type t2 : pt.getActualTypeArguments()) {
				if (! first)
					sb.append(',');
				first = false;
				sb.append(getSimpleName(t2));
			}
			sb.append(">");
			return sb.toString();
		}
		return null;
	}

	/**
	 * Returns the specified annotation only if it's been declared on the specified class.
	 *
	 * <p>
	 * More efficient than calling {@link Class#getAnnotation(Class)} since it doesn't recursively look for the class
	 * up the parent chain.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class.
	 * @param t The annotated class.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T getDeclaredAnnotation(Class<T> a, Type t) {
		Class<?> c = toClass(t);
		if (c != null)
			for (Annotation a2 : c.getDeclaredAnnotations())
				if (a2.annotationType() == a)
					return (T)a2;
		return null;
	}

	/**
	 * Same as getAnnotations(Class, Type) except returns the annotations as a map with the keys being the
	 * class on which the annotation was found.
	 *
	 * <p>
	 * Results are ordered child-to-parent.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @param t The class being searched.
	 * @return The found matches, or an empty map if annotation was not found.
	 */
	public static <T extends Annotation> LinkedHashMap<Class<?>,T> getAnnotationsMap(Class<T> a, Type t) {
		LinkedHashMap<Class<?>,T> m = new LinkedHashMap<>();
		findAnnotationsMap(a, t, m);
		return m;
	}

	/**
	 * Same as {@link #getAnnotationsMap(Class, Type)} except returns results in parent-to-child order.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @param t The class being searched.
	 * @return The found matches, or an empty map if annotation was not found.
	 */
	public static <T extends Annotation> LinkedHashMap<Class<?>,T> getAnnotationsMapParentFirst(Class<T> a, Type t) {
		return CollectionUtils.reverse(getAnnotationsMap(a, t));
	}

	private static <T extends Annotation> void findAnnotationsMap(Class<T> a, Type t, Map<Class<?>,T> m) {
		Class<?> c = toClass(t);
		if (c != null) {

			T t2 = getDeclaredAnnotation(a, c);
			if (t2 != null)
				m.put(c, t2);

			findAnnotationsMap(a, c.getSuperclass(), m);

			for (Class<?> c2 : c.getInterfaces())
				findAnnotationsMap(a, c2, m);
		}
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 *
	 * @param a The annotation.
	 * @param t The class.
	 * @param l The list of annotations.
	 */
	public static <T extends Annotation> void appendAnnotations(Class<T> a, Type t, List<T> l) {
		Class<?> c = toClass(t);
		if (c != null) {
			addIfNotNull(l, getDeclaredAnnotation(a, c));

			if (c.getPackage() != null)
				addIfNotNull(l, c.getPackage().getAnnotation(a));

			appendAnnotations(a, c.getSuperclass(), l);

			for (Class<?> c2 : c.getInterfaces())
				appendAnnotations(a, c2, l);
		}
	}

	/**
	 * Returns the specified type as a <code>Class</code>.
	 *
	 * <p>
	 * If it's already a <code>Class</code>, it just does a cast.
	 * <br>If it's a <code>ParameterizedType</code>, it returns the raw type.
	 *
	 * @param t The type to convert.
	 * @return The type converted to a <code>Class</code>, or <jk>null</jk> if it could not be converted.
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
	 * Similar to {@link Class#getResourceAsStream(String)} except looks up the parent hierarchy for the existence of
	 * the specified resource.
	 *
	 * @param c The class to return the resource on.
	 * @param name The resource name.
	 * @return An input stream on the specified resource, or <jk>null</jk> if the resource could not be found.
	 */
	public static InputStream getResource(Class<?> c, String name) {
		if (name == null)
			return null;
		while (c != null) {
			InputStream is = c.getResourceAsStream(name);
			if (is != null)
				return is;
			c = c.getSuperclass();
		}
		return null;
	}
}
