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
package org.apache.juneau.reflection;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Lightweight utility class for introspecting information about a constructor.
 */
@BeanIgnore
public final class ConstructorInfo implements Comparable<ConstructorInfo> {

	private final ClassInfo declaringClass;
	private final Constructor<?> c;
	private List<MethodParamInfo> params;
	private List<ClassInfo> paramTypes;
	private Class<?>[] rawParamTypes;
	private Type[] rawGenericParamTypes;

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param c The constructor being wrapped.
	 */
	protected ConstructorInfo(Constructor<?> c) {
		this(ClassInfo.of(c.getDeclaringClass()), c);
	}

	/**
	 * Constructor.
	 *
	 * @param declaringClass The class that declares this method.
	 * @param c The constructor being wrapped.
	 */
	protected ConstructorInfo(ClassInfo declaringClass, Constructor<?> c) {
		this.declaringClass = declaringClass;
		this.c = c;
	}

	/**
	 * Convenience method for instantiating a {@link ConstructorInfo};
	 *
	 * @param declaringClass The class that declares this method.
	 * @param c The constructor being wrapped.
	 * @return A new {@link ConstructorInfo} object, or <jk>null</jk> if the method was null;
	 */
	public static ConstructorInfo of(ClassInfo declaringClass, Constructor<?> c) {
		if (c == null)
			return null;
		return new ConstructorInfo(declaringClass, c);
	}

	/**
	 * Convenience method for instantiating a {@link ConstructorInfo};
	 *
	 * @param c The constructor being wrapped.
	 * @return A new {@link ConstructorInfo} object, or <jk>null</jk> if the method was null;
	 */
	public static ConstructorInfo of(Constructor<?> c) {
		if (c == null)
			return null;
		return new ConstructorInfo(ClassInfo.of(c.getDeclaringClass()), c);
	}

	/**
	 * Returns the wrapped method.
	 *
	 * @return The wrapped method.
	 */
	@SuppressWarnings("unchecked")
	public <T> Constructor<T> inner() {
		return (Constructor<T>)c;
	}

	/**
	 * Returns metadata about the declaring class.
	 *
	 * @return Metadata about the declaring class.
	 */
	public ClassInfo getDeclaringClass() {
		return declaringClass;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the parameters defined on this method.
	 *
	 * @return An array of parameter information, never <jk>null</jk>.
	 */
	public List<MethodParamInfo> getParams() {
		if (params == null) {
			List<MethodParamInfo> l = new ArrayList<>(c.getParameterCount());
			for (int i = 0; i < c.getParameterCount(); i++)
				l.add(new MethodParamInfo(this, i));
			params = Collections.unmodifiableList(l);
		}
		return params;
	}

	/**
	 * Returns parameter information at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter information, never <jk>null</jk>.
	 */
	public MethodParamInfo getParam(int index) {
		return getParams().get(index);
	}

	/**
	 * Returns the parameter types on this constructor.
	 *
	 * @return The parameter types on this constructor.
	 */
	public List<ClassInfo> getParamTypes() {
		if (paramTypes == null) {
			// Note that due to a bug involving Enum constructors, getGenericParameterTypes() may
			// always return an empty array.
			Class<?>[] ptc = rawParamTypes();
			Type[] ptt = rawGenericParamTypes();
			List<ClassInfo> l = new ArrayList<>(ptc.length);
			for (int i = 0; i < ptc.length; i++)
				l.add(ClassInfo.of(ptc[i], ptt.length > i ? ptt[i] : ptc[i]));
			paramTypes = Collections.unmodifiableList(l);
		}
		return paramTypes;
	}

	/**
	 * Returns the parameter type of the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter type of the parameter at the specified index.
	 */
	public ClassInfo getParamType(int index) {
		return getParamTypes().get(index);
	}

	/**
	 * Returns the parameter type of the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter type of the parameter at the specified index.
	 */
	public Class<?> getRawParamType(int index) {
		return rawParamTypes()[index];
	}

	/**
	 * Returns the parameter type of the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter type of the parameter at the specified index.
	 */
	public Type getRawGenericParamType(int index) {
		return rawGenericParamTypes()[index];
	}

	private Class<?>[] rawParamTypes() {
		if (rawParamTypes == null)
			rawParamTypes = c.getParameterTypes();
		return rawParamTypes;
	}

	private Type[] rawGenericParamTypes() {
		if (rawGenericParamTypes == null)
			rawGenericParamTypes = c.getGenericParameterTypes();
		return rawGenericParamTypes;
	}

	/**
	 * Returns the number of parameters in this method.
	 *
	 * @return The number of parameters in this method.
	 */
	public int getParamCount() {
		return c.getParameterCount();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the parameter annotations on this constructor.
	 *
	 * @return The parameter annotations on this constructor.
	 */
	public Annotation[][] getParameterAnnotations() {
		return c.getParameterAnnotations();
	}

	/**
	 * Returns the parameter annotations on the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter annotations on the parameter at the specified index.
	 */
	public Annotation[] getParameterAnnotations(int index) {
		return c.getParameterAnnotations()[index];
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present on this constructor.
	 *
	 * @param a The annotation to check for.
	 * @return <jk>true</jk> if the specified annotation is present on this constructor.
	 */
	public boolean isAnnotationPresent(Class<? extends Annotation> a) {
		return c.isAnnotationPresent(a);
	}

	/**
	 * Returns the specified annotation on this constructor.
	 *
	 * @param a The annotation to search for.
	 * @return The annotation, or <jk>null</jk> if not present.
	 */
	public <T extends Annotation> T getAnnotation(Class<T> a) {
		return c.getAnnotation(a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Characteristics
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this constructor.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this constructor.
	 */
	public boolean isAll(ClassFlags...flags) {
		for (ClassFlags f : flags) {
			switch (f) {
				case DEPRECATED:
					if (isNotDeprecated())
						return false;
					break;
				case NOT_DEPRECATED:
					if (isDeprecated())
						return false;
					break;
				case HAS_ARGS:
					if (hasNoArgs())
						return false;
					break;
				case HAS_NO_ARGS:
					if (hasArgs())
						return false;
					break;
				case PUBLIC:
					if (isNotPublic())
						return false;
					break;
				case NOT_PUBLIC:
					if (isPublic())
						return false;
					break;
				default:
					throw new RuntimeException("Invalid flag for constructor: " + f);
			}
		}
		return true;
	}

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this constructor.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this constructor.
	 */
	public boolean isAny(ClassFlags...flags) {
		for (ClassFlags f : flags) {
			switch (f) {
				case DEPRECATED:
					if (isDeprecated())
						return true;
					break;
				case NOT_DEPRECATED:
					if (isNotDeprecated())
						return true;
					break;
				case HAS_ARGS:
					if (hasArgs())
						return true;
					break;
				case HAS_NO_ARGS:
					if (hasNoArgs())
						return true;
					break;
				case PUBLIC:
					if (isPublic())
						return true;
					break;
				case NOT_PUBLIC:
					if (isNotPublic())
						return true;
					break;
				default:
					throw new RuntimeException("Invalid flag for constructor: " + f);
			}
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this constructor has this arguments.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this constructor has this arguments in the exact order.
	 */
	public boolean hasArgs(Class<?>...args) {
		Class<?>[] pt = rawParamTypes();
		if (pt.length == args.length) {
			for (int i = 0; i < pt.length; i++)
				if (! pt[i].equals(args[i]))
					return false;
			return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this constructor has one or more arguments.
	 *
	 * @return <jk>true</jk> if this constructor has one or more arguments.
	 */
	public boolean hasArgs() {
		return getParamCount() != 0;
	}

	/**
	 * Returns <jk>true</jk> if this constructor has zero arguments.
	 *
	 * @return <jk>true</jk> if this constructor has zero arguments.
	 */
	public boolean hasNoArgs() {
		return getParamCount() == 0;
	}

	/**
	 * Returns <jk>true</jk> if this constructor has this number of arguments.
	 *
	 * @param number The number of expected arguments.
	 * @return <jk>true</jk> if this constructor has this number of arguments.
	 */
	public boolean hasNumArgs(int number) {
		return getParamCount() == number;
	}

	/**
	 * Returns <jk>true</jk> if this constructor has at most only this arguments in any order.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this constructor has at most only this arguments in any order.
	 */
	public boolean hasFuzzyArgs(Class<?>...args) {
		return ClassUtils.fuzzyArgsMatch(rawParamTypes(), args) != -1;
	}

	/**
	 * Returns <jk>true</jk> if this constructor has the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this constructor has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isDeprecated() {
		return c.isAnnotationPresent(Deprecated.class);
	}

	/**
	 * Returns <jk>true</jk> if this constructor doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this constructor doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isNotDeprecated() {
		return ! c.isAnnotationPresent(Deprecated.class);
	}

	/**
	 * Returns <jk>true</jk> if this constructor is public.
	 *
	 * @return <jk>true</jk> if this constructor is public.
	 */
	public boolean isPublic() {
		return Modifier.isPublic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this constructor is not public.
	 *
	 * @return <jk>true</jk> if this constructor is not public.
	 */
	public boolean isNotPublic() {
		return ! Modifier.isPublic(c.getModifiers());
	}

	/**
	 * Identifies if the specified visibility matches this constructor.
	 *
	 * @param v The visibility to validate against.
	 * @return <jk>true</jk> if this visibility matches the modifier attribute of this constructor.
	 */
	public boolean isVisible(Visibility v) {
		return v.isVisible(c);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Shortcut for calling the new-instance method on the underlying constructor.
	 *
	 * @param args the arguments used for the method call
	 * @return The object returned from the constructor.
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings("unchecked")
	public <T> T invoke(Object...args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		return (T)c.newInstance(args);
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return <jk>true</jk> if call was successful.
	 */
	public boolean setAccessible() {
		try {
			if (! (c.isAccessible()))
				c.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Makes constructor accessible if it matches the visibility requirements, or returns <jk>null</jk> if it doesn't.
	 *
	 * <p>
	 * Security exceptions thrown on the call to {@link Constructor#setAccessible(boolean)} are quietly ignored.
	 *
	 * @param v The minimum visibility.
	 * @return
	 * 	The same constructor if visibility requirements met, or <jk>null</jk> if visibility requirement not
	 * 	met or call to {@link Constructor#setAccessible(boolean)} throws a security exception.
	 */
	public ConstructorInfo transform(Visibility v) {
		if (v.transform(c) == null)
			return null;
		return this;
	}

	/**
	 * Returns the name of the underlying constructor.
	 *
	 * @return The name of the underlying constructor.
	 */
	public String getName() {
		return c.getName();
	}

	@Override
	public int compareTo(ConstructorInfo o) {
		int i = getName().compareTo(o.getName());
		if (i == 0) {
			i = getParamCount() - o.getParamCount();
			if (i == 0) {
				for (int j = 0; j < getParamCount() && i == 0; j++) {
					Class<?>[] tpt = rawParamTypes(), opt = o.rawParamTypes();
					i = tpt[j].getName().compareTo(opt[j].getName());
				}
			}
		}
		return i;
	}

	/**
	 * Returns a string representation of this constructor that consists of its name and simple arguments.
	 *
	 * @return A string representation of this constructor that consists of its name and simple arguments.
	 */
	public String getLabel() {
		return ClassUtils.asString(c);
	}
}
