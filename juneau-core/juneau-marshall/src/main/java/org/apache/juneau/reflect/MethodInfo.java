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
import java.util.function.*;

import org.apache.juneau.*;
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

	/**
	 * Convenience method for retrieving values in {@link #getMatching()} in parent-to-child order.
	 *
	 * @return
	 * 	All matching methods including this method itself.
	 * 	<br>Methods are ordered from parent-to-child order.
	 */
	public Iterable<Method> getMatchingParentFirst() {
		return iterable(getMatching(), true);
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

	private Method findMatchingOnClass(ClassInfo c) {
		for (Method m2 : c.inner().getDeclaredMethods())
			if (m.getName().equals(m2.getName()) && Arrays.equals(m.getParameterTypes(), m2.getParameterTypes()))
				return m2;
		return null;
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
		return appendAnnotations(new ArrayList<>(), a);
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
		return appendAnnotationsParentFirst(new ArrayList<>(), a);
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
	@SuppressWarnings("unchecked")
	public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a) {
		for (Method m2 : getMatching())
			for (Annotation a2 :  m2.getDeclaredAnnotations())
				if (a.isInstance(a2))
					l.add((T)a2);
		getReturnType().resolved().appendAnnotations(l, a);
		declaringClass.appendAnnotations(l, a);
		return l;
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 *
	 * @param l The list of annotations.
	 * @param a The annotation.
	 * @return The same list.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> List<T> appendAnnotationsParentFirst(List<T> l, Class<T> a) {
		declaringClass.appendAnnotationsParentFirst(l, a);
		for (Method m2 : getMatchingParentFirst())
			for (Annotation a2 :  m2.getDeclaredAnnotations())
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

	/**
	 * Constructs an {@link AnnotationList} of all annotations found on this method.
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On this method and matching methods ordered child-to-parent.
	 * 	<li>On this class.
	 * 	<li>On parent classes ordered child-to-parent.
	 * 	<li>On interfaces ordered child-to-parent.
	 * 	<li>On the package of this class.
	 * </ol>
	 *
	 * @param filter
	 * 	Optional filter to apply to limit which annotations are added to the list.
	 * 	<br>Can be <jk>null</jk> for no filtering.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public AnnotationList getAnnotationList(Predicate<AnnotationInfo<?>> filter) {
		return appendAnnotationList(new AnnotationList(filter));
	}

	/**
	 * Constructs an {@link AnnotationList} of all annotations found on this method.
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * 	<li>On this method and matching methods ordered parent-to-child.
	 * </ol>
	 *
	 * @param filter
	 * 	Optional filter to apply to limit which annotations are added to the list.
	 * 	<br>Can be <jk>null</jk> for no filtering.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public AnnotationList getAnnotationListParentFirst(Predicate<AnnotationInfo<?>> filter) {
		return appendAnnotationListParentFirst(new AnnotationList(filter));
	}

	/**
	 * Same as {@link #getAnnotationListParentFirst(Predicate)} except only returns annotations defined on methods.
	 *
	 * @param filter
	 * 	Optional filter to apply to limit which annotations are added to the list.
	 * 	<br>Can be <jk>null</jk> for no filtering.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public AnnotationList getAnnotationListMethodOnlyParentFirst(Predicate<AnnotationInfo<?>> filter) {
		return appendAnnotationListMethodOnlyParentFirst(new AnnotationList(filter));
	}

	/**
	 * Returns <jk>true</jk> if this method or parent methods have any annotations annotated with {@link PropertyStoreApply}.
	 *
	 * @return <jk>true</jk> if this method or parent methods have any annotations annotated with {@link PropertyStoreApply}.
	 */
	public boolean hasConfigAnnotations() {
		for (Method m2 : getMatching())
			for (Annotation a2 :  m2.getAnnotations())
				if (a2.annotationType().getAnnotation(PropertyStoreApply.class) != null)
					return true;
		return false;
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

	AnnotationList appendAnnotationList(AnnotationList al) {
		ClassInfo c = this.declaringClass;
		for (ClassInfo ci : c.getParents()) {
			appendMethodAnnotations(al, ci);
			appendAnnotations(al, ci);
		}
		for (ClassInfo ci : c.getInterfaces()) {
			appendMethodAnnotations(al, ci);
			appendAnnotations(al, ci);
		}
		appendAnnotations(al, c.getPackage());
		return al;
	}

	AnnotationList appendAnnotationListParentFirst(AnnotationList al) {
		ClassInfo c = this.declaringClass;
		appendAnnotations(al, c.getPackage());
		for (ClassInfo ci : c.getInterfacesParentFirst()) {
			appendAnnotations(al, ci);
			appendMethodAnnotations(al, ci);
		}
		for (ClassInfo ci : c.getParentsParentFirst()) {
			appendAnnotations(al, ci);
			appendMethodAnnotations(al, ci);
		}
		return al;
	}

	AnnotationList appendAnnotationListMethodOnlyParentFirst(AnnotationList al) {
		ClassInfo c = this.declaringClass;
		for (ClassInfo ci : c.getInterfacesParentFirst())
			appendMethodAnnotations(al, ci);
		for (ClassInfo ci : c.getParentsParentFirst())
			appendMethodAnnotations(al, ci);
		return al;
	}

	void appendAnnotations(AnnotationList al, Package p) {
		if (p != null)
			for (Annotation a : p.getDeclaredAnnotations())
				al.add(AnnotationInfo.of(p, a));
	}

	void appendAnnotations(AnnotationList al, ClassInfo ci) {
		if (ci != null)
			for (Annotation a : ci.c.getDeclaredAnnotations())
				al.add(AnnotationInfo.of(ci, a));
	}

	void appendMethodAnnotations(AnnotationList al, ClassInfo ci) {
		Method m = findMatchingOnClass(ci);
		if (m != null)
			for (Annotation a : m.getDeclaredAnnotations())
				al.add(AnnotationInfo.of(MethodInfo.of(m), a));
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
	 * Returns <jk>true</jk> if this method has this return type.
	 *
	 * @param ci The return type to test for.
	 * @return <jk>true</jk> if this method has this return type.
	 */
	public boolean hasReturnType(ClassInfo ci) {
		return hasReturnType(ci.inner());
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
	 * Returns <jk>true</jk> if this method has this parent return type.
	 *
	 * @param ci The return type to test for.
	 * @return <jk>true</jk> if this method has this parent return type.
	 */
	public boolean hasReturnTypeParent(ClassInfo ci) {
		return hasReturnTypeParent(ci.inner());
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
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings("unchecked")
	public <T> T invoke(Object obj, Object...args) throws ExecutableException {
		try {
			return (T)m.invoke(obj, args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new ExecutableException(e);
		}
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
	 * <br>It's not going to work on methods with generic argument types like <c>Object</c>
	 *
	 * @param pojo
	 * 	The POJO the method is being called on.
	 * 	<br>Can be <jk>null</jk> for static methods.
	 * @param args
	 * 	The arguments to pass to the method.
	 * @return
	 * 	The results of the method invocation.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public Object invokeFuzzy(Object pojo, Object...args) throws ExecutableException {
		try {
			return m.invoke(pojo, ClassUtils.getMatchingArgs(m.getParameterTypes(), args));
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new ExecutableException(e);
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
