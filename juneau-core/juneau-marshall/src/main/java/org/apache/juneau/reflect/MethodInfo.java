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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.beans.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Lightweight utility class for introspecting information about a method.
 */
@BeanIgnore
public final class MethodInfo extends ExecutableInfo implements Comparable<MethodInfo> {

	private ClassInfo returnType;
	private final Method m;
	private List<Method> matching;

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param declaringClass The class that declares this method.
	 * @param m The method being wrapped.
	 */
	protected MethodInfo(ClassInfo declaringClass, Method m) {
		super(declaringClass, m);
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

	//-----------------------------------------------------------------------------------------------------------------
	// Matching methods.
	//-----------------------------------------------------------------------------------------------------------------

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

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

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
		return appendAnnotations(new ArrayList<>(), a, false);
	}

	/**
	 * Identical to {@link #getAnnotations(Class)} but returns the list in reverse (parent-to-child) order.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	A list of all matching annotations found or an empty list if none found.
	 */
	public <T extends Annotation> List<T> getAnnotationsParentFirst(Class<T> a) {
		return appendAnnotations(new ArrayList<>(), a, true);
	}

	/**
	 * Finds and appends the specified annotation on the specified method and methods on superclasses/interfaces to the specified
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
	 * @return The same list.
	 */
	public <T extends Annotation> List<T> appendAnnotationsParentFirst(List<T> l, Class<T> a) {
		return appendAnnotations(l, a, true);
	}

	@SuppressWarnings("unchecked")
	private <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a, boolean parentFirst) {
		List<Method> methods = getMatching();
		for (Method m2 : iterable(methods, parentFirst))
			for (Annotation a2 :  m2.getAnnotations())
				if (a.isInstance(a2))
					l.add((T)a2);
		getReturnType().resolved().appendAnnotations(l, a);
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

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends Annotation> T findAnnotation(Class<T> a) {
		for (Method m2 : getMatching())
			for (Annotation a2 :  m2.getAnnotations())
				if (a.isInstance(a2))
					return (T)a2;
		return getReturnType().resolved().getAnnotation(a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Return type.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the generic return type of this method as a {@link ClassInfo} object.
	 *
	 * @return The generic return type of this method.
	 */
	public ClassInfo getReturnType() {
		if (returnType == null)
			returnType = ClassInfo.of(m.getReturnType(), m.getGenericReturnType());
		return returnType;
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
		StringBuilder sb = new StringBuilder(128);
		sb.append(m.getName());
		Class<?>[] pt = rawParamTypes();
		if (pt.length > 0) {
			sb.append('(');
			List<ParamInfo> mpi = getParams();
			for (int i = 0; i < pt.length; i++) {
				if (i > 0)
					sb.append(',');
				mpi.get(i).getParameterType().appendFullName(sb);
			}
			sb.append(')');
		}
		return sb.toString();
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
	 * 	argsOnlyOfType(fooMethod, String.<jk>class</jk>, Integer.<jk>class</jk>);  <jc>// True.</jc>
	 * 	argsOnlyOfType(fooMethod, String.<jk>class</jk>, Integer.<jk>class</jk>, Map.<jk>class</jk>);  <jc>// True.</jc>
	 * 	argsOnlyOfType(fooMethod, String.<jk>class</jk>);  <jc>// False.</jc>
	 * </p>
	 *
	 * @param args The valid class types (exact) for the arguments.
	 * @return <jk>true</jk> if the method parameters only consist of the types specified in the list.
	 */
	public boolean argsOnlyOfType(Class<?>...args) {
		for (Class<?> c1 : rawParamTypes()) {
			boolean foundMatch = false;
			for (Class<?> c2 : args)
				if (c1 == c2)
					foundMatch = true;
			if (! foundMatch)
				return false;
		}
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this method is a bridge method.
	 *
	 * @return <jk>true</jk> if this method is a bridge method.
	 */
	public boolean isBridge() {
		return m.isBridge();
	}

	@Override
	public int compareTo(MethodInfo o) {
		int i = getSimpleName().compareTo(o.getSimpleName());
		if (i == 0) {
			i = rawParamTypes().length - o.rawParamTypes().length;
			if (i == 0) {
				for (int j = 0; j < rawParamTypes().length && i == 0; j++) {
					i = rawParamTypes()[j].getName().compareTo(o.rawParamTypes()[j].getName());
				}
			}
		}
		return i;
	}
}
