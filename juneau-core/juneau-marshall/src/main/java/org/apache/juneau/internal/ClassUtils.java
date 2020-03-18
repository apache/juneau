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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;

/**
 * Class-related utility methods.
 */
public final class ClassUtils {

	/**
	 * Given the specified list of objects, return readable names for the class types of the objects.
	 *
	 * @param o The objects.
	 * @return An array of readable class type strings.
	 */
	public static ObjectList getFullClassNames(Object[] o) {
		ObjectList l = new ObjectList();
		for (int i = 0; i < o.length; i++)
			l.add(o[i] == null ? "null" : ClassInfo.of((o[i].getClass())).getFullName());
		return l;
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
				ClassInfo c3 = ClassInfo.of((Class<?>)c2);

				MethodInfo mi = c3.getStaticCreator(args);
				if (mi != null)
					return mi.invoke(null, args);

				if (c3.isInterface() || c3.isAbstract())
					return null;

				// First look for an exact match.
				ConstructorInfo con = c3.getPublicConstructor(args);
				if (con != null)
					return con.<T>invoke(args);

				// Next look for an exact match including the outer.
				if (outer != null) {
					args = new AList<>().append(outer).appendAll(args).toArray();
					con = c3.getPublicConstructor(args);
					if (con != null)
						return con.<T>invoke(args);
				}

				// Finally use fuzzy matching.
				if (fuzzyArgs) {
					mi = c3.getStaticCreatorFuzzy(args);
					if (mi != null)
						return mi.invoke(null, getMatchingArgs(mi.getParamTypes(), args));

					con = c3.getPublicConstructorFuzzy(args);
					if (con != null)
						return con.<T>invoke(getMatchingArgs(con.getParamTypes(), args));
				}

				throw new FormattedRuntimeException("Could not instantiate class {0}/{1}.  Constructor not found.", c.getName(), c2);
			} catch (Exception e) {
				throw new FormattedRuntimeException(e, "Could not instantiate class {0}", c.getName());
			}
		} else if (ClassInfo.of(c).isParentOf(c2.getClass())) {
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
	public static Object[] getMatchingArgs(List<ClassInfo> paramTypes, Object... args) {
		Object[] params = new Object[paramTypes.size()];
		for (int i = 0; i < paramTypes.size(); i++) {
			ClassInfo pt = paramTypes.get(i).getWrapperInfoIfPrimitive();
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
	 * @param c The class.
	 * @param s The string to create the instance from.
	 * @return A new object instance, or <jk>null</jk> if a method for converting the string to an object could not be found.
	 */
	public static <T> T fromString(Class<T> c, String s) {
		Mutater<String,T> t = Mutaters.get(String.class, c);
		return t == null ? null : t.mutate(s);
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
		Mutater<Object,String> t = (Mutater<Object,String>)Mutaters.get(o.getClass(), String.class);
		return t == null ? o.toString() : t.mutate(o);
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The constructor.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Constructor<?> x) {
		try {
			if (! (x == null || x.isAccessible()))
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
			if (! (x == null || x.isAccessible()))
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
			if (! (x == null || x.isAccessible()))
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
}
