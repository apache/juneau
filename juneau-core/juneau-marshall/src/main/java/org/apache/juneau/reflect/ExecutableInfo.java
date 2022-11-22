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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConsumerUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Contains common methods between {@link ConstructorInfo} and {@link MethodInfo}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@FluentSetters
public abstract class ExecutableInfo {

	final ClassInfo declaringClass;
	final Executable e;
	final boolean isConstructor;

	private volatile ParamInfo[] params;
	private volatile ClassInfo[] paramTypes, exceptionInfos;
	private volatile Class<?>[] rawParamTypes;
	private volatile Type[] rawGenericParamTypes;
	private volatile Parameter[] rawParameters;
	private volatile Annotation[][] parameterAnnotations;
	private volatile Annotation[] declaredAnnotations;

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
		return ulist(_getParams());
	}

	/**
	 * Performs an action on every parameter that matches the specified filter.
	 *
	 * @param filter The filter, can be <jk>null</jk>.
	 * @param action The action to perform.
	 * @return This object.
	 */
	public ExecutableInfo forEachParam(Predicate<ParamInfo> filter, Consumer<ParamInfo> action) {
		for (ParamInfo pi : _getParams())
			if (test(filter, pi))
				action.accept(pi);
		return this;
	}

	/**
	 * Returns parameter information at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter information, never <jk>null</jk>.
	 */
	public final ParamInfo getParam(int index) {
		checkIndex(index);
		return _getParams()[index];
	}

	/**
	 * Returns the parameter types on this executable.
	 *
	 * @return The parameter types on this executable.
	 */
	public final List<ClassInfo> getParamTypes() {
		return ulist(_getParameterTypes());
	}

	/**
	 * Returns the parameter type of the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter type of the parameter at the specified index.
	 */
	public final ClassInfo getParamType(int index) {
		checkIndex(index);
		return _getParameterTypes()[index];
	}

	/**
	 * Returns the raw parameter types on this executable.
	 *
	 * @return The raw parameter types on this executable.
	 */
	public final List<Class<?>> getRawParamTypes() {
		return ulist(_getRawParamTypes());
	}

	/**
	 * Returns the raw parameter type of the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The raw parameter type of the parameter at the specified index.
	 */
	public final Class<?> getRawParamType(int index) {
		checkIndex(index);
		return _getRawParamTypes()[index];
	}

	/**
	 * Returns the raw generic parameter types on this executable.
	 *
	 * @return The raw generic parameter types on this executable.
	 */
	public final List<Type> getRawGenericParamTypes() {
		return ulist(_getRawGenericParamTypes());
	}

	/**
	 * Returns the raw generic parameter type of the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The raw generic parameter type of the parameter at the specified index.
	 */
	public final Type getRawGenericParamType(int index) {
		checkIndex(index);
		return _getRawGenericParamTypes()[index];
	}

	/**
	 * Returns an array of raw {@link Parameter} objects that represent all the parameters to the underlying executable represented by this object.
	 *
	 * @return An array of raw {@link Parameter} objects, or an empty array if executable has no parameters.
	 * @see Executable#getParameters()
	 */
	public final List<Parameter> getRawParameters() {
		return ulist(_getRawParameters());
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
		return _getRawParameters()[index];
	}

	final ParamInfo[] _getParams() {
		if (params == null) {
			synchronized(this) {
				Parameter[] rp = _getRawParameters();
				ParamInfo[] l = new ParamInfo[rp.length];
				for (int i = 0; i < rp.length; i++)
					l[i] = new ParamInfo(this, rp[i], i);
				params = l;
			}
		}
		return params;
	}

	final ClassInfo[] _getParameterTypes() {
		if (paramTypes == null) {
			synchronized(this) {
				Class<?>[] ptc = _getRawParamTypes();
				// Note that due to a bug involving Enum constructors, getGenericParameterTypes() may
				// always return an empty array.  This appears to be fixed in Java 8 b75.
				Type[] ptt = _getRawGenericParamTypes();
				if (ptt.length != ptc.length) {
					// Bug in javac: generic type array excludes enclosing instance parameter
					// for inner classes with at least one generic constructor parameter.
					if (ptt.length + 1 == ptc.length) {
						Type[] ptt2 = new Type[ptc.length];
						ptt2[0] = ptc[0];
						for (int i = 0; i < ptt.length; i++)
							ptt2[i+1] = ptt[i];
						ptt = ptt2;
					} else {
						ptt = ptc;
					}
				}
				ClassInfo[] l = new ClassInfo[ptc.length];
				for (int i = 0; i < ptc.length; i++)
					l[i] = ClassInfo.of(ptc[i], ptt[i]);
				paramTypes = l;
			}
		}
		return paramTypes;
	}

	Class<?>[] _getRawParamTypes() {
		if (rawParamTypes == null) {
			synchronized(this) {
				rawParamTypes = e.getParameterTypes();
			}
		}
		return rawParamTypes;
	}

	final Type[] _getRawGenericParamTypes() {
		if (rawGenericParamTypes == null) {
			synchronized(this) {
				rawGenericParamTypes = e.getGenericParameterTypes();
			}
		}
		return rawGenericParamTypes;
	}

	final Parameter[] _getRawParameters() {
		if (rawParameters == null) {
			synchronized(this) {
				rawParameters = e.getParameters();
			}
		}
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
	 * Performs an action on all matching parameter annotations at the specified parameter index.
	 *
	 * @param <A> The annotation type.
	 * @param index The parameter index.
	 * @param type The annotation type.
	 * @param predicate The predicate.
	 * @param consumer The consumer.
	 * @return This object.
	 */
	public final <A extends Annotation> ExecutableInfo forEachParameterAnnotation(int index, Class<A> type, Predicate<A> predicate, Consumer<A> consumer) {
		for (Annotation a : _getParameterAnnotations(index))
			if (type.isInstance(a))
				consume(predicate, consumer, type.cast(a));
		return this;
	}

	final Annotation[][] _getParameterAnnotations() {
		if (parameterAnnotations == null) {
			synchronized(this) {
				parameterAnnotations = e.getParameterAnnotations();
			}
		}
		return parameterAnnotations;
	}

	final Annotation[] _getParameterAnnotations(int index) {
		checkIndex(index);
		Annotation[][] x = _getParameterAnnotations();
		int c = e.getParameterCount();
		if (c != x.length) {
			// Seems to be a JVM bug where getParameterAnnotations() don't take mandated parameters into account.
			Annotation[][] x2 = new Annotation[c][];
			int diff = c - x.length;
			for (int i = 0; i < diff; i++)
				x2[i] = new Annotation[0];
			for (int i = diff; i < c; i++)
				x2[i] = x[i-diff];
			x = x2;
		}
		return x[index];
	}

	final Annotation[] _getDeclaredAnnotations() {
		if (declaredAnnotations == null) {
			synchronized(this) {
				declaredAnnotations = e.getDeclaredAnnotations();
			}
		}
		return declaredAnnotations;
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
		return ulist(_getExceptionTypes());
	}

	final ClassInfo[] _getExceptionTypes() {
		if (exceptionInfos == null) {
			synchronized(this) {
				Class<?>[] exceptionTypes = e.getExceptionTypes();
				ClassInfo[] l = new ClassInfo[exceptionTypes.length];
				for (int i = 0; i < exceptionTypes.length; i++)
					l[i] = ClassInfo.of(exceptionTypes[i]);
				exceptionInfos = l;
			}
		}
		return exceptionInfos;
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
				case PROTECTED:
					if (isNotProtected())
						return false;
					break;
				case NOT_PROTECTED:
					if (isProtected())
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
					throw new BasicRuntimeException("Invalid flag for executable: {0}", f);
			}
		}
		return true;
	}

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this field.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this field.
	 */
	public final boolean is(ReflectFlags...flags) {
		return isAll(flags);
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
				case PROTECTED:
					if (isProtected())
						return true;
					break;
				case NOT_PROTECTED:
					if (isNotProtected())
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
					throw new BasicRuntimeException("Invalid flag for executable: {0}", f);
			}
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified arguments.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has this arguments in the exact order.
	 */
	public final boolean hasParamTypes(Class<?>...args) {
		Class<?>[] pt = _getRawParamTypes();
		if (pt.length == args.length) {
			for (int i = 0; i < pt.length; i++)
				if (! pt[i].equals(args[i]))
					return false;
			return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified arguments.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has this arguments in the exact order.
	 */
	public final boolean hasParamTypes(ClassInfo...args) {
		Class<?>[] pt = _getRawParamTypes();
		if (pt.length == args.length) {
			for (int i = 0; i < pt.length; i++)
				if (! pt[i].equals(args[i].inner()))
					return false;
			return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified argument parent classes.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has this arguments in the exact order.
	 */
	public final boolean hasMatchingParamTypes(Class<?>...args) {
		ClassInfo[] pt = _getParameterTypes();
		if (pt.length != args.length)
			return false;
		for (int i = 0; i < pt.length; i++) {
			boolean matched = false;
			for (int j = 0; j < args.length; j++)
				if (pt[i].isParentOfFuzzyPrimitives(args[j]))
					matched = true;
			if (! matched)
				return false;
		}
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified argument parent classes.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has this arguments in the exact order.
	 */
	public final boolean hasMatchingParamTypes(ClassInfo...args) {
		ClassInfo[] pt = _getParameterTypes();
		if (pt.length != args.length)
			return false;
		for (int i = 0; i < pt.length; i++) {
			boolean matched = false;
			for (int j = 0; j < args.length; j++)
				if (pt[i].isParentOfFuzzyPrimitives(args[j].inner()))
					matched = true;
			if (! matched)
				return false;
		}
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this method has at most only this arguments in any order.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has at most only this arguments in any order.
	 */
	public final boolean hasFuzzyParamTypes(Class<?>...args) {
		return fuzzyArgsMatch(args) != -1;
	}

	/**
	 * Returns how well this method matches the specified arg types.
	 *
	 * <p>
	 * The number returned is the number of method arguments that match the passed in arg types.
	 * <br>Returns <c>-1</c> if the method cannot take in one or more of the specified arguments.
	 *
	 * @param argTypes The arg types to check against.
	 * @return How many parameters match or <c>-1</c> if method cannot handle one or more of the arguments.
	 */
	public final int fuzzyArgsMatch(Class<?>... argTypes) {
		int matches = 0;
		outer: for (ClassInfo pi : getParamTypes()) {
			for (Class<?> a : argTypes) {
				if (pi.isParentOfFuzzyPrimitives(a)) {
					matches++;
					continue outer;
				}
			}
			return -1;
		}
		return matches;
	}

	/**
	 * Returns how well this method matches the specified arg types.
	 *
	 * <p>
	 * The number returned is the number of method arguments that match the passed in arg types.
	 * <br>Returns <c>-1</c> if the method cannot take in one or more of the specified arguments.
	 *
	 * @param argTypes The arg types to check against.
	 * @return How many parameters match or <c>-1</c> if method cannot handle one or more of the arguments.
	 */
	public final int fuzzyArgsMatch(Object... argTypes) {
		int matches = 0;
		outer: for (ClassInfo pi : getParamTypes()) {
			for (Object a : argTypes) {
				if (pi.canAcceptArg(a)) {
					matches++;
					continue outer;
				}
			}
			return -1;
		}
		return matches;
	}

	/**
	 * Returns <jk>true</jk> if this method has at most only this arguments in any order.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has at most only this arguments in any order.
	 */
	public final boolean hasFuzzyParamTypes(ClassInfo...args) {
		return fuzzyArgsMatch(args) != -1;
	}

	/**
	 * Returns how well this method matches the specified arg types.
	 *
	 * <p>
	 * The number returned is the number of method arguments that match the passed in arg types.
	 * <br>Returns <c>-1</c> if the method cannot take in one or more of the specified arguments.
	 *
	 * @param argTypes The arg types to check against.
	 * @return How many parameters match or <c>-1</c> if method cannot handle one or more of the arguments.
	 */
	public final int fuzzyArgsMatch(ClassInfo... argTypes) {
		int matches = 0;
		outer: for (ClassInfo pi : getParamTypes()) {
			for (ClassInfo a : argTypes) {
				if (pi.isParentOfFuzzyPrimitives(a)) {
					matches++;
					continue outer;
				}
			}
			return -1;
		}
		return matches;
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
	 * Returns <jk>true</jk> if this method is protected.
	 *
	 * @return <jk>true</jk> if this method is protected.
	 */
	public final boolean isProtected() {
		return Modifier.isProtected(e.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is not protected.
	 *
	 * @return <jk>true</jk> if this method is not protected.
	 */
	public final boolean isNotProtected() {
		return ! Modifier.isProtected(e.getModifiers());
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
	 * @return This object.
	 */
	@FluentSetter
	public ExecutableInfo accessible() {
		setAccessible();
		return this;
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return <jk>true</jk> if call was successful.
	 */
	public final boolean setAccessible() {
		try {
			if (e != null)
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

	/**
	 * Returns <jk>true</jk> if this method has a name in the specified list.
	 *
	 * @param names The names to test for.
	 * @return <jk>true</jk> if this method has one of the names.
	 */
	public final boolean hasName(String...names) {
		for (String n : names)
			if (getSimpleName().equals(n))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this method has a name in the specified set.
	 *
	 * @param names The names to test for.
	 * @return <jk>true</jk> if this method has one of the names.
	 */
	public final boolean hasName(Set<String> names) {
		return names.contains(getSimpleName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the full name of this executable.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><js>"com.foo.MyClass.get(java.util.String)"</js> - Method.
	 * 	<li><js>"com.foo.MyClass(java.util.String)"</js> - Constructor.
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
	 * 	<li><js>"MyClass.get(String)"</js> - Method.
	 * 	<li><js>"MyClass(String)"</js> - Constructor.
	 * </ul>
	 *
	 * @return The underlying executable name.
	 */
	public final String getShortName() {
		StringBuilder sb = new StringBuilder(64);
		sb.append(getSimpleName()).append('(');
		Class<?>[] pt = _getRawParamTypes();
		for (int i = 0; i < pt.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(pt[i].getSimpleName());
		}
		sb.append(')');
		return sb.toString();
	}

	/**
	 * Returns the simple name of the underlying method.
	 *
	 * @return The simple name of the underlying method;
	 */
	public final String getSimpleName() {
		return isConstructor ? e.getDeclaringClass().getSimpleName() : e.getName();
	}

	@Override
	public String toString() {
		return getShortName();
	}

	// <FluentSetters>

	// </FluentSetters>
}
