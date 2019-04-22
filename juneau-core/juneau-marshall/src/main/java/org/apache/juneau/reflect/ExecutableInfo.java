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

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Contains common methods between {@link ConstructorInfo} and {@link MethodInfo}.
 */
public abstract class ExecutableInfo {

	final ClassInfo declaringClass;
	final Executable e;
	final boolean isConstructor;

	private List<ParamInfo> params;
	private List<ClassInfo> paramTypes, exceptionInfos;
	private Class<?>[] rawParamTypes, rawExceptionTypes;
	private Type[] rawGenericParamTypes;
	private Parameter[] rawParameters;
	private Map<Class<?>,Optional<Annotation>> annotationMap;

	/**
	 * Constructor.
	 *
	 * @param declaringClass The class that declares this method or constructor.
	 * @param e The constructor or method that this info represents.
	 */
	protected ExecutableInfo(ClassInfo declaringClass, Executable e) {
		this.declaringClass = declaringClass;
		this.e = e;
		this.isConstructor = e instanceof Constructor;
	}

	/**
	 * Returns metadata about the class that declared this method or constructor.
	 *
	 * @return Metadata about the class that declared this method or constructor.
	 */
	public final ClassInfo getDeclaringClass() {
		return declaringClass;
	}

	/**
	 * Returns <jk>true</jk> if this executable represents a {@link Constructor}.
	 *
	 * @return
	 * 	<jk>true</jk> if this executable represents a {@link Constructor} and can be cast to {@link ConstructorInfo}.
	 * 	<jk>false</jk> if this executable represents a {@link Method} and can be cast to {@link MethodInfo}.
	 */
	public final boolean isConstructor() {
		return isConstructor;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the number of parameters in this executable.
	 *
	 * <p>
	 * Same as calling {@link Executable#getParameterCount()}.
	 *
	 * @return The number of parameters in this executable.
	 */
	public final int getParamCount() {
		return e.getParameterCount();
	}

	/**
	 * Returns <jk>true</jk> if this executable has at least one parameter.
	 *
	 * <p>
	 * Same as calling {@link Executable#getParameterCount()} and comparing with zero.
	 *
	 * @return <jk>true</jk> if this executable has at least one parameter.
	 */
	public final boolean hasParams() {
		return getParamCount() != 0;
	}

	/**
	 * Returns <jk>true</jk> if this executable has no parameters.
	 *
	 * <p>
	 * Same as calling {@link Executable#getParameterCount()} and comparing with zero.
	 *
	 * @return <jk>true</jk> if this executable has no parameters.
	 */
	public final boolean hasNoParams() {
		return getParamCount() == 0;
	}

	/**
	 * Returns <jk>true</jk> if this executable has this number of arguments.
	 *
	 * <p>
	 * Same as calling {@link Executable#getParameterCount()} and comparing the count.
	 *
	 * @param number The number of expected arguments.
	 * @return <jk>true</jk> if this executable has this number of arguments.
	 */
	public final boolean hasNumParams(int number) {
		return getParamCount() == number;
	}

	/**
	 * Returns the parameters defined on this executable.
	 *
	 * <p>
	 * Same as calling {@link Executable#getParameters()} but wraps the results
	 *
	 * @return An array of parameter information, never <jk>null</jk>.
	 */
	public final List<ParamInfo> getParams() {
		if (params == null) {
			Parameter[] rp = rawParameters();
			List<ParamInfo> l = new ArrayList<>(rp.length);
			for (int i = 0; i < rp.length; i++)
				l.add(new ParamInfo(this, rp[i], i));
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
	public final ParamInfo getParam(int index) {
		checkIndex(index);
		if (params != null)
			return params.get(index);
		return new ParamInfo(this, rawParameters()[index], index);
	}

	/**
	 * Returns the parameter types on this executable.
	 *
	 * @return The parameter types on this executable.
	 */
	public final List<ClassInfo> getParamTypes() {
		if (paramTypes == null) {
			Class<?>[] ptc = rawParamTypes();
			// Note that due to a bug involving Enum constructors, getGenericParameterTypes() may
			// always return an empty array.  This appears to be fixed in Java 8 b75.
			Type[] ptt = rawGenericParamTypes();
			if (ptt.length != ptc.length)
				ptt = ptc;
			List<ClassInfo> l = new ArrayList<>(ptc.length);
			for (int i = 0; i < ptc.length; i++)
				l.add(ClassInfo.of(ptc[i], ptt[i]));
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
	public final ClassInfo getParamType(int index) {
		checkIndex(index);
		if (paramTypes != null)
			return getParamTypes().get(index);
		return ClassInfo.of(getRawParamType(index), getRawGenericParamType(index));
	}

	/**
	 * Returns the raw parameter types on this executable.
	 *
	 * @return The raw parameter types on this executable.
	 */
	public final Class<?>[] getRawParamTypes() {
		return rawParamTypes().clone();
	}

	/**
	 * Returns the raw parameter type of the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The raw parameter type of the parameter at the specified index.
	 */
	public final Class<?> getRawParamType(int index) {
		checkIndex(index);
		return rawParamTypes()[index];
	}

	/**
	 * Returns the raw generic parameter types on this executable.
	 *
	 * @return The raw generic parameter types on this executable.
	 */
	public final Type[] getRawGenericParamTypes() {
		return rawGenericParamTypes().clone();
	}

	/**
	 * Returns the raw generic parameter type of the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The raw generic parameter type of the parameter at the specified index.
	 */
	public final Type getRawGenericParamType(int index) {
		checkIndex(index);
		return rawGenericParamTypes()[index];
	}

	/**
	 * Returns an array of raw {@link Parameter} objects that represent all the parameters to the underlying executable represented by this object.
	 *
	 * @return An array of raw {@link Parameter} objects, or an empty array if executable has no parameters.
	 * @see Executable#getParameters()
	 */
	public final Parameter[] getRawParameters() {
		return rawParameters().clone();
	}

	/**
	 * Returns the raw {@link Parameter} object that represents the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The raw {@link Parameter} object that represents the parameter at the specified index.
	 * @see Executable#getParameters()
	 */
	public final Parameter getRawParameter(int index) {
		checkIndex(index);
		return rawParameters()[index];
	}

	Class<?>[] rawParamTypes() {
		if (rawParamTypes == null)
			rawParamTypes = e.getParameterTypes();
		return rawParamTypes;
	}

	Type[] rawGenericParamTypes() {
		if (rawGenericParamTypes == null)
			rawGenericParamTypes = e.getGenericParameterTypes();
		return rawGenericParamTypes;
	}

	Parameter[] rawParameters() {
		if (rawParameters == null)
			rawParameters = e.getParameters();
		return rawParameters;
	}

	private void checkIndex(int index) {
		int pc = getParamCount();
		if (pc == 0)
			throw new IndexOutOfBoundsException(format("Invalid index ''{0}''.  No parameters.", index));
		if (index < 0 || index >= pc)
			throw new IndexOutOfBoundsException(format("Invalid index ''{0}''.  Parameter count: {1}", index, pc));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the parameter annotations on this executable.
	 *
	 * @return The parameter annotations on this executable.
	 */
	public final Annotation[][] getParameterAnnotations() {
		return e.getParameterAnnotations();
	}

	/**
	 * Returns the parameter annotations on the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter annotations on the parameter at the specified index.
	 */
	public final Annotation[] getParameterAnnotations(int index) {
		checkIndex(index);
		return e.getParameterAnnotations()[index];
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present on this constructor.
	 *
	 * @param a The annotation to check for.
	 * @return <jk>true</jk> if the specified annotation is present on this constructor.
	 */
	public final boolean hasAnnotation(Class<? extends Annotation> a) {
		return getAnnotation(a) != null;
	}

	/**
	 * Finds the annotation of the specified type defined on this executable.
	 *
	 * <p>
	 * If this is a method and the annotation cannot be found on the immediate method, searches methods with the same
	 * signature on the parent classes or interfaces.
	 * <br>The search is performed in child-to-parent order.
	 *
	 * <p>
	 * If still not found, searches for the annotation on the return type of the method.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	The annotation if found, or <jk>null</jk> if not.
	 */
	@SuppressWarnings("unchecked")
	public final <T extends Annotation> T getAnnotation(Class<T> a) {
		if (a == null)
			return null;
		Optional<Annotation> o = annotationMap().get(a);
		if (o == null) {
			o = Optional.ofNullable(findAnnotation(a));
			annotationMap().put(a, o);
		}
		return o.isPresent() ? (T)o.get() : null;
	}

	/**
	 * Searched for the specified annotation.
	 *
	 * @param a The annotation to search for.
	 * @return The annotation if found.
	 */
	protected <T extends Annotation> T findAnnotation(Class<T> a) {
		return e.getAnnotation(a);
	}

	private synchronized Map<Class<?>,Optional<Annotation>> annotationMap() {
		if (annotationMap == null)
			annotationMap = new ConcurrentHashMap<>();
		return annotationMap;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Exceptions
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the exception types on this executable.
	 *
	 * @return The exception types on this executable.
	 */
	public final List<ClassInfo> getExceptionTypes() {
		if (exceptionInfos == null) {
			Class<?>[] exceptionTypes = rawExceptionTypes();
			List<ClassInfo> l = new ArrayList<>(exceptionTypes.length);
			for (Class<?> et : exceptionTypes)
				l.add(ClassInfo.of(et));
			exceptionInfos = Collections.unmodifiableList(l);
		}
		return exceptionInfos;
	}

	/**
	 * Returns the raw exception types on this executable.
	 *
	 * @return The raw exception types on this executable.
	 */
	public final Class<?>[] getRawExceptionTypes() {
		return rawExceptionTypes().clone();
	}

	private Class<?>[] rawExceptionTypes() {
		if (rawExceptionTypes == null)
			rawExceptionTypes = e.getExceptionTypes();
		return rawExceptionTypes;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Characteristics
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this method.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this method.
	 */
	public final boolean isAll(ReflectFlags...flags) {
		for (ReflectFlags f : flags) {
			switch (f) {
				case DEPRECATED:
					if (isNotDeprecated())
						return false;
					break;
				case NOT_DEPRECATED:
					if (isDeprecated())
						return false;
					break;
				case HAS_PARAMS:
					if (hasNoParams())
						return false;
					break;
				case HAS_NO_PARAMS:
					if (hasParams())
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
				case STATIC:
					if (isNotStatic())
						return false;
					break;
				case NOT_STATIC:
					if (isStatic())
						return false;
					break;
				case ABSTRACT:
					if (isNotAbstract())
						return false;
					break;
				case NOT_ABSTRACT:
					if (isAbstract())
						return false;
					break;
				default:
					throw new RuntimeException("Invalid flag for executable: " + f);
			}
		}
		return true;
	}

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this method.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this method.
	 */
	public final boolean isAny(ReflectFlags...flags) {
		for (ReflectFlags f : flags) {
			switch (f) {
				case DEPRECATED:
					if (isDeprecated())
						return true;
					break;
				case NOT_DEPRECATED:
					if (isNotDeprecated())
						return true;
					break;
				case HAS_PARAMS:
					if (hasParams())
						return true;
					break;
				case HAS_NO_PARAMS:
					if (hasNoParams())
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
				case STATIC:
					if (isStatic())
						return true;
					break;
				case NOT_STATIC:
					if (isNotStatic())
						return true;
					break;
				case ABSTRACT:
					if (isAbstract())
						return true;
					break;
				case NOT_ABSTRACT:
					if (isNotAbstract())
						return true;
					break;
				default:
					throw new RuntimeException("Invalid flag for executable: " + f);
			}
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this method has this arguments.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has this arguments in the exact order.
	 */
	public final boolean hasArgs(Class<?>...args) {
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
	 * Returns <jk>true</jk> if this method has at most only this arguments in any order.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has at most only this arguments in any order.
	 */
	public final boolean hasFuzzyArgs(Class<?>...args) {
		return ClassUtils.fuzzyArgsMatch(rawParamTypes(), args) != -1;
	}

	/**
	 * Returns <jk>true</jk> if this method has the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this method has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public final boolean isDeprecated() {
		return e.isAnnotationPresent(Deprecated.class);

	}

	/**
	 * Returns <jk>true</jk> if this method doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this method doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 */
	public final boolean isNotDeprecated() {
		return ! e.isAnnotationPresent(Deprecated.class);

	}

	/**
	 * Returns <jk>true</jk> if this method is abstract.
	 *
	 * @return <jk>true</jk> if this method is abstract.
	 */
	public final boolean isAbstract() {
		return Modifier.isAbstract(e.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is not abstract.
	 *
	 * @return <jk>true</jk> if this method is not abstract.
	 */
	public final boolean isNotAbstract() {
		return ! Modifier.isAbstract(e.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is public.
	 *
	 * @return <jk>true</jk> if this method is public.
	 */
	public final boolean isPublic() {
		return Modifier.isPublic(e.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is not public.
	 *
	 * @return <jk>true</jk> if this method is not public.
	 */
	public final boolean isNotPublic() {
		return ! Modifier.isPublic(e.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is static.
	 *
	 * @return <jk>true</jk> if this method is static.
	 */
	public final boolean isStatic() {
		return Modifier.isStatic(e.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is not static.
	 *
	 * @return <jk>true</jk> if this method is not static.
	 */
	public final boolean isNotStatic() {
		return ! Modifier.isStatic(e.getModifiers());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Visibility
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return <jk>true</jk> if call was successful.
	 */
	public final boolean setAccessible() {
		try {
			if (! (e.isAccessible()))
				e.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Identifies if the specified visibility matches this method.
	 *
	 * @param v The visibility to validate against.
	 * @return <jk>true</jk> if this visibility matches the modifier attribute of this method.
	 */
	public final boolean isVisible(Visibility v) {
		return v.isVisible(e);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this method has this name.
	 *
	 * @param name The name to test for.
	 * @return <jk>true</jk> if this method has this name.
	 */
	public final boolean hasName(String name) {
		return getSimpleName().equals(name);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the full name of this executable.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><js>"com.foo.MyClass.get(java.util.String)"<js> - Method.
	 * 	<li><js>"com.foo.MyClass(java.util.String)"<js> - Constructor.
	 * </ul>
	 *
	 * @return The underlying executable name.
	 */
	public final String getFullName() {
		StringBuilder sb = new StringBuilder(128);
		ClassInfo dc = declaringClass;
		Package p = dc.getPackage();
		if (p != null)
			sb.append(p.getName()).append('.');
		dc.appendShortName(sb);
		if (! isConstructor)
			sb.append('.').append(getSimpleName());
		sb.append('(');
		List<ClassInfo> pt = getParamTypes();
		for (int i = 0; i < pt.size(); i++) {
			if (i > 0)
				sb.append(',');
			pt.get(i).appendFullName(sb);
		}
		sb.append(')');
		return sb.toString();
	}

	/**
	 * Returns the short name of this executable.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><js>"MyClass.get(String)"<js> - Method.
	 * 	<li><js>"MyClass(String)"<js> - Constructor.
	 * </ul>
	 *
	 * @return The underlying executable name.
	 */
	public final String getShortName() {
		StringBuilder sb = new StringBuilder(64);
		sb.append(getSimpleName()).append('(');
		Class<?>[] pt = rawParamTypes();
		for (int i = 0; i < pt.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(pt[i].getSimpleName());
		}
		sb.append(')');
		return sb.toString();
	}

	/**
	 * Returns the simple name of the underlying class.
	 *
	 * <p>
	 * Returns either {@link Class#getSimpleName()} or {@link Type#getTypeName()} depending on whether
	 * this is a class or type.
	 *
	 * @return The simple name of the underlying class;
	 */
	public final String getSimpleName() {
		return isConstructor ? e.getDeclaringClass().getSimpleName() : e.getName();
	}

	@Override
	public String toString() {
		return getShortName();
	}
}
