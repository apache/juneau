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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.beans.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.Visibility;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Utility class for introspecting information about a method.
 */
@BeanIgnore
public final class MethodInfo {

	private ClassInfo declaringClass;
	private final Method m;
	private MethodParamInfo[] params;
	private List<Method> matching;
	private Map<Class<?>,Optional<Annotation>> annotationMap;
	private ClassInfo[] exceptionInfos;
	private String signature;

	/**
	 * Constructor.
	 *
	 * @param m The method being wrapped.
	 */
	public MethodInfo(Method m) {
		this(null, m);
	}

	/**
	 * Constructor.
	 *
	 * @param declaringClass The class that declares this method.
	 * @param m The method being wrapped.
	 */
	public MethodInfo(ClassInfo declaringClass, Method m) {
		this.declaringClass = declaringClass;
		this.m = m;
	}

	/**
	 * Convenience method for instantiating a {@link MethodInfo};
	 *
	 * @param declaringClass The class that declares this method.
	 * @param m The method being wrapped.
	 * @return A new {@link MethodInfo} object, or <jk>null</jk> if the method was null;
	 */
	public static MethodInfo of(ClassInfo declaringClass, Method m) {
		if (m == null)
			return null;
		return new MethodInfo(declaringClass, m);
	}

	/**
	 * Convenience method for instantiating a {@link MethodInfo};
	 *
	 * @param m The method being wrapped.
	 * @return A new {@link MethodInfo} object, or <jk>null</jk> if the method was null;
	 */
	public static MethodInfo of(Method m) {
		if (m == null)
			return null;
		return new MethodInfo(ClassInfo.of(m.getDeclaringClass()), m);
	}

	/**
	 * Returns the wrapped method.
	 *
	 * @return The wrapped method.
	 */
	public Method inner() {
		return m;
	}

	/**
	 * Returns metadata about the declaring class.
	 *
	 * @return Metadata about the declaring class.
	 */
	public ClassInfo getDeclaringClass() {
		if (declaringClass == null)
			declaringClass = ClassInfo.of(m.getDeclaringClass());
		return declaringClass;
	}

	/**
	 * Returns the parameters defined on this method.
	 *
	 * @return An array of parameter information, never <jk>null</jk>.
	 */
	public MethodParamInfo[] getParams() {
		if (params == null) {
			params = new MethodParamInfo[m.getParameterCount()];
			for (int i = 0; i < m.getParameterCount(); i++)
				params[i] = new MethodParamInfo(this, i);
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
		return getParams()[index];
	}

	/**
	 * Finds all declared methods with the same name and arguments on all superclasses and interfaces.
	 *
	 * @return
	 * 	All matching methods including this method itself.
	 * 	<br>Methods are ordered from child-to-parent order.
	 */
	public List<Method> getMatching() {
		if (matching == null)
			matching = Collections.unmodifiableList(findMatching(new ArrayList<>(), m, m.getDeclaringClass()));
		return matching;
	}

	private static List<Method> findMatching(List<Method> l, Method m, Class<?> c) {
		for (Method m2 : c.getDeclaredMethods())
			if (m.getName().equals(m2.getName()) && Arrays.equals(m.getParameterTypes(), m2.getParameterTypes()))
				l.add(m2);
		Class<?> pc = c.getSuperclass();
		if (pc != null)
			findMatching(l, m, pc);
		for (Class<?> ic : c.getInterfaces())
			findMatching(l, m, ic);
		return l;
	}

	/**
	 * Returns the {@link ClassInfo} objects associated with the exception types on this method.
	 *
	 * @return The {@link ClassInfo} objects associated with the exception types on this method.
	 */
	public synchronized ClassInfo[] getExceptionInfos() {
		if (exceptionInfos == null) {
			Class<?>[] exceptionTypes = m.getExceptionTypes();
			exceptionInfos = new ClassInfo[exceptionTypes.length];
			for (int i = 0; i < exceptionTypes.length; i++)
				exceptionInfos[i] = ClassInfo.of(exceptionTypes[i]);
		}
		return exceptionInfos;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds the annotation of the specified type defined on this method.
	 *
	 * <p>
	 * If the annotation cannot be found on the immediate method, searches methods with the same
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
	public <T extends Annotation> T getAnnotation(Class<T> a) {
		Optional<Annotation> o = annotationMap().get(a);
		if (o == null) {
			o = Optional.ofNullable(findAnnotation(a));
			annotationMap().put(a, o);
		}
		return o.isPresent() ? (T)o.get() : null;
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified annotation.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	The <jk>true</jk> if annotation if found.
	 */
	public boolean hasAnnotation(Class<? extends Annotation> a) {
		return getAnnotation(a) != null;
	}

	/**
	 * Returns all annotations of the specified type defined on the specified method.
	 *
	 * <p>
	 * Searches all methods with the same signature on the parent classes or interfaces
	 * and the return type on the method.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	A list of all matching annotations found in child-to-parent order, or an empty list if none found.
	 */
	public <T extends Annotation> List<T> getAnnotations(Class<T> a) {
		return getAnnotations(a, false);
	}

	/**
	 * Identical to {@link #getAnnotations(Class)} but optionally returns the list in reverse (parent-to-child) order.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @param parentFirst If <jk>true</jk>, results are in parent-to-child order.
	 * @return
	 * 	A list of all matching annotations found or an empty list if none found.
	 */
	public <T extends Annotation> List<T> getAnnotations(Class<T> a, boolean parentFirst) {
		return appendAnnotations(new ArrayList<>(), a, parentFirst);
	}

	/**
	 * Finds and appends the specified annotation on the specified method and methods onsuperclasses/interfaces to the specified
	 * list.
	 *
	 * <p>
	 * Results are ordered in child-to-parent order.
	 *
	 * @param l The list of annotations.
	 * @param a The annotation.
	 * @return The same list.
	 */
	public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a) {
		return appendAnnotations(l, a, false);
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 *
	 * @param l The list of annotations.
	 * @param a The annotation.
	 * @param parentFirst If <jk>true</jk>, results are ordered in parent-to-child order.
	 * @return The same list.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a, boolean parentFirst) {
		List<Method> methods = getMatching();
		for (Method m2 : iterable(methods, parentFirst))
			for (Annotation a2 :  m2.getAnnotations())
				if (a.isInstance(a2))
					l.add((T)a2);
		getGenericReturnTypeInfo().resolved().appendAnnotations(l, a);
		return l;
	}

	/**
	 * Returns the first annotation in the specified list on this method.
	 *
	 * @param c The annotations that cannot be present on the method.
	 * @return <jk>true</jk> if this method does not have any of the specified annotations.
	 */
	@SafeVarargs
	public final Annotation getAnnotation(Class<? extends Annotation>...c) {
		for (Class<? extends Annotation> cc : c) {
			Annotation a = getAnnotation(cc);
			if (a != null)
				return a;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T extends Annotation> T findAnnotation(Class<T> a) {
		for (Method m2 : getMatching())
			for (Annotation a2 :  m2.getAnnotations())
				if (a.isInstance(a2))
					return (T)a2;
		return getGenericReturnTypeInfo().resolved().getAnnotation(a);
	}

	private synchronized Map<Class<?>,Optional<Annotation>> annotationMap() {
		if (annotationMap == null)
			annotationMap = new ConcurrentHashMap<>();
		return annotationMap;
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
				case TRANSIENT:
				case NOT_TRANSIENT:
				default:
					break;

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
				case TRANSIENT:
				case NOT_TRANSIENT:
				default:
					break;

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
	public boolean hasArgs(Class<?>...args) {
		Class<?>[] pt = m.getParameterTypes();
		if (pt.length == args.length) {
			for (int i = 0; i < pt.length; i++)
				if (! pt[i].equals(args[i]))
					return false;
			return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this method has this number of arguments.
	 *
	 * @param number The number of expected arguments.
	 * @return <jk>true</jk> if this method has this number of arguments.
	 */
	public boolean hasNumArgs(int number) {
		return m.getParameterTypes().length == number;
	}

	/**
	 * Returns <jk>true</jk> if this method has at most only this arguments in any order.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has at most only this arguments in any order.
	 */
	public boolean hasFuzzyArgs(Class<?>...args) {
		return ClassUtils.fuzzyArgsMatch(m.getParameterTypes(), args) != -1;
	}

	/**
	 * Returns <jk>true</jk> if this method has the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this method has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isDeprecated() {
		return m.isAnnotationPresent(Deprecated.class);

	}

	/**
	 * Returns <jk>true</jk> if this method doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this method doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isNotDeprecated() {
		return ! m.isAnnotationPresent(Deprecated.class);

	}

	/**
	 * Returns <jk>true</jk> if this method is abstract.
	 *
	 * @return <jk>true</jk> if this method is abstract.
	 */
	public boolean isAbstract() {
		return Modifier.isAbstract(m.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is not abstract.
	 *
	 * @return <jk>true</jk> if this method is not abstract.
	 */
	public boolean isNotAbstract() {
		return ! Modifier.isAbstract(m.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is public.
	 *
	 * @return <jk>true</jk> if this method is public.
	 */
	public boolean isPublic() {
		return Modifier.isPublic(m.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is not public.
	 *
	 * @return <jk>true</jk> if this method is not public.
	 */
	public boolean isNotPublic() {
		return ! Modifier.isPublic(m.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is static.
	 *
	 * @return <jk>true</jk> if this method is static.
	 */
	public boolean isStatic() {
		return Modifier.isStatic(m.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method is not static.
	 *
	 * @return <jk>true</jk> if this method is not static.
	 */
	public boolean isNotStatic() {
		return ! Modifier.isStatic(m.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this method has one or more arguments.
	 *
	 * @return <jk>true</jk> if this method has one or more arguments.
	 */
	public boolean hasArgs() {
		return m.getParameterTypes().length > 0;
	}

	/**
	 * Returns <jk>true</jk> if this method has zero arguments.
	 *
	 * @return <jk>true</jk> if this method has zero arguments.
	 */
	public boolean hasNoArgs() {
		return m.getParameterTypes().length == 0;
	}

	/**
	 * Returns <jk>true</jk> if this method has this name.
	 *
	 * @param name The name to test for.
	 * @return <jk>true</jk> if this method has this name.
	 */
	public boolean hasName(String name) {
		return m.getName().equals(name);
	}

	/**
	 * Returns <jk>true</jk> if this method has this return type.
	 *
	 * @param c The return type to test for.
	 * @return <jk>true</jk> if this method has this return type.
	 */
	public boolean hasReturnType(Class<?> c) {
		return m.getReturnType() == c;
	}

	/**
	 * Returns <jk>true</jk> if this method has this parent return type.
	 *
	 * @param c The return type to test for.
	 * @return <jk>true</jk> if this method has this parent return type.
	 */
	public boolean hasReturnTypeParent(Class<?> c) {
		return ClassInfo.of(c).isParentOf(m.getReturnType());
	}

	/**
	 * Returns the name of this method.
	 *
	 * @return The name of this method.
	 */
	public String getName() {
		return m.getName();
	}

	/**
	 * Returns the return type of this method.
	 *
	 * @return The return type of this method.
	 */
	public ClassInfo getReturnType() {
		return ClassInfo.of(m.getReturnType());
	}

	/**
	 * Identifies if the specified visibility matches this method.
	 *
	 * @param v The visibility to validate against.
	 * @return <jk>true</jk> if this visibility matches the modifier attribute of this method.
	 */
	public boolean isVisible(Visibility v) {
		return v.isVisible(m);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Shortcut for calling the invoke method on the underlying method.
	 *
	 * @param obj the object the underlying method is invoked from.
	 * @param args the arguments used for the method call
	 * @return The object returned from the method.
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public Object invoke(Object obj, Object...args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return m.invoke(obj, args);
	}

	/**
	 * Invokes the specified method using fuzzy-arg matching.
	 *
	 * <p>
	 * Arguments will be matched to the parameters based on the parameter types.
	 * <br>Arguments can be in any order.
	 * <br>Extra arguments will be ignored.
	 * <br>Missing arguments will be left <jk>null</jk>.
	 *
	 * <p>
	 * Note that this only works for methods that have distinguishable argument types.
	 * <br>It's not going to work on methods with generic argument types like <code>Object</code>
	 *
	 * @param pojo
	 * 	The POJO the method is being called on.
	 * 	<br>Can be <jk>null</jk> for static methods.
	 * @param args
	 * 	The arguments to pass to the method.
	 * @return
	 * 	The results of the method invocation.
	 * @throws Exception
	 */
	public Object invokeFuzzy(Object pojo, Object...args) throws Exception {
		return m.invoke(pojo, ClassUtils.getMatchingArgs(m.getParameterTypes(), args));
	}


	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return <jk>true</jk> if call was successful.
	 */
	public boolean setAccessible() {
		try {
			if (! (m.isAccessible()))
				m.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Returns the signature of this method.
	 *
	 * <p>
	 * For no-arg methods, the signature will be a simple string such as <js>"toString"</js>.
	 * For methods with one or more args, the arguments will be fully-qualified class names (e.g.
	 * <js>"append(java.util.StringBuilder,boolean)"</js>)
	 *
	 * @return The methods signature.
	 */
	public String getSignature() {
		if (signature == null) {
			StringBuilder sb = new StringBuilder(m.getName());
			Class<?>[] pt = m.getParameterTypes();
			if (pt.length > 0) {
				sb.append('(');
				for (int i = 0; i < pt.length; i++) {
					if (i > 0)
						sb.append(',');
					sb.append(ClassUtils.getReadableClassName(pt[i]));
				}
				sb.append(')');
			}
			signature = sb.toString();
		}
		return signature;
	}

	/**
	 * Returns the parameter types on this method.
	 *
	 * @return The parameter types on this method.
	 */
	public Class<?>[] getParameterTypes() {
		return m.getParameterTypes();
	}

	/**
	 * Returns the generic parameter types on this method.
	 *
	 * @return The generic parameter types on this method.
	 */
	public Type[] getGenericParameterTypes() {
		return m.getGenericParameterTypes();
	}

	/**
	 * Returns the parameter type of the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter type of the parameter at the specified index.
	 */
	public Class<?> getParameterType(int index) {
		return getParameterTypes()[index];
	}

	/**
	 * Returns the generic parameter type of the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The generic parameter type of the parameter at the specified index.
	 */
	public Type getGenericParameterType(int index) {
		return getGenericParameterTypes()[index];
	}

	/**
	 * Returns the generic parameter type of the parameter at the specified index as a {@link ClassInfo} object.
	 *
	 * @param index The parameter index.
	 * @return The generic parameter type of the parameter at the specified index.
	 */
	public ClassInfo getGenericParameterTypeInfo(int index) {
		return ClassInfo.of(getGenericParameterType(index));
	}

	/**
	 * Returns the parameter annotations on this method.
	 *
	 * @return The parameter annotations on this method.
	 */
	public Annotation[][] getParameterAnnotations() {
		return m.getParameterAnnotations();
	}

	/**
	 * Returns the parameter annotations on the parameter at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter annotations on the parameter at the specified index.
	 */
	public Annotation[] getParameterAnnotations(int index) {
		return m.getParameterAnnotations()[index];
	}

	/**
	 * Returns the bean property name if this is a getter or setter.
	 *
	 * @return The bean property name, or <jk>null</jk> if this isn't a getter or setter.
	 */
	public String getPropertyName() {
		String n = m.getName();
		if ((n.startsWith("get") || n.startsWith("set")) && n.length() > 3)
			return Introspector.decapitalize(n.substring(3));
		if (n.startsWith("is") && n.length() > 2)
			return Introspector.decapitalize(n.substring(2));
		return n;
	}

	/**
	 * Returns <jk>true</jk> if the parameters on the method only consist of the types specified in the list.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bpcode w800'>
	 *
	 *  <jc>// Example method:</jc>
	 * 	<jk>public void</jk> foo(String bar, Integer baz);
	 *
	 * 	isArgsOnlyOfType(fooMethod, String.<jk>class</jk>, Integer.<jk>class</jk>);  <jc>// True.</jc>
	 * 	isArgsOnlyOfType(fooMethod, String.<jk>class</jk>, Integer.<jk>class</jk>, Map.<jk>class</jk>);  <jc>// True.</jc>
	 * 	isArgsOnlyOfType(fooMethod, String.<jk>class</jk>);  <jc>// False.</jc>
	 * </p>
	 *
	 * @param args The valid class types (exact) for the arguments.
	 * @return <jk>true</jk> if the method parameters only consist of the types specified in the list.
	 */
	public boolean argsOnlyOfType(Class<?>...args) {
		for (Class<?> c1 : getParameterTypes()) {
			boolean foundMatch = false;
			for (Class<?> c2 : args)
				if (c1 == c2)
					foundMatch = true;
			if (! foundMatch)
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
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
	 * Returns the generic return type of this method.
	 *
	 * @return The generic return type of this method.
	 */
	public Type getGenericReturnType() {
		return m.getGenericReturnType();
	}

	/**
	 * Returns the generic return type of this method as a {@link ClassInfo} object.
	 *
	 * @return The generic return type of this method.
	 */
	public ClassInfo getGenericReturnTypeInfo() {
		return ClassInfo.of(getGenericReturnType());
	}

	/**
	 * Returns <jk>true</jk> if this method is a bridge method.
	 *
	 * @return <jk>true</jk> if this method is a bridge method.
	 */
	public boolean isBridge() {
		return m.isBridge();
	}
}
