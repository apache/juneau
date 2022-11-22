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
import static org.apache.juneau.internal.ConsumerUtils.*;

import java.beans.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Lightweight utility class for introspecting information about a method.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@FluentSetters
public final class MethodInfo extends ExecutableInfo implements Comparable<MethodInfo> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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
		return declaringClass.getMethodInfo(m);
	}

	/**
	 * Convenience method for instantiating a {@link MethodInfo};
	 *
	 * @param declaringClass The class that declares this method.
	 * @param m The method being wrapped.
	 * @return A new {@link MethodInfo} object, or <jk>null</jk> if the method was null;
	 */
	public static MethodInfo of(Class<?> declaringClass, Method m) {
		if (m == null)
			return null;
		return ClassInfo.of(declaringClass).getMethodInfo(m);
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
		return ClassInfo.of(m.getDeclaringClass()).getMethodInfo(m);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Method m;
	private volatile ClassInfo returnType;
	private volatile MethodInfo[] matching;

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
	 * Returns <jk>true</jk> if this constructor can accept the specified arguments in the specified order.
	 *
	 * @param args The arguments to check.
	 * @return <jk>true</jk> if this constructor can accept the specified arguments in the specified order.
	 */
	public boolean canAccept(Object...args) {
		Class<?>[] pt = m.getParameterTypes();
		if (pt.length != args.length)
			return false;
		for (int i = 0; i < pt.length; i++)
			if (! pt[i].isInstance(args[i]))
				return false;
		return true;
	}

	/**
	 * Returns the number of matching arguments for this method.
	 *
	 * @param args The arguments to check.
	 * @return the number of matching arguments for this method.
	 */
	public int canAcceptFuzzy(Object...args) {
		int matches = 0;
		outer: for (ClassInfo pi : _getParameterTypes()) {
			for (Object a : args) {
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
	 * Performs an action on all matching declared methods with the same name and arguments on all superclasses and interfaces.
	 *
	 * <p>
	 * Methods are accessed from child-to-parent order.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public MethodInfo forEachMatching(Predicate<MethodInfo> filter, Consumer<MethodInfo> action) {
		for (MethodInfo m : _getMatching())
			consume(filter, action, m);
		return this;
	}

	/**
	 * Performs an action on all matching declared methods with the same name and arguments on all superclasses and interfaces.
	 *
	 * <p>
	 * Methods are accessed from parent-to-child order.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public MethodInfo forEachMatchingParentFirst(Predicate<MethodInfo> filter, Consumer<MethodInfo> action) {
		MethodInfo[] m = _getMatching();
		for (int i = m.length-1; i >= 0; i--)
			consume(filter, action, m[i]);
		return this;
	}

	private static List<MethodInfo> findMatching(List<MethodInfo> l, MethodInfo m, ClassInfo c) {
		for (MethodInfo m2 : c._getDeclaredMethods())
			if (m.hasName(m2.getName()) && Arrays.equals(m._getParameterTypes(), m2._getParameterTypes()))
				l.add(m2);
		ClassInfo pc = c.getSuperclass();
		if (pc != null)
			findMatching(l, m, pc);
		for (ClassInfo ic : c._getDeclaredInterfaces())
			findMatching(l, m, ic);
		return l;
	}

	private MethodInfo findMatchingOnClass(ClassInfo c) {
		for (MethodInfo m2 : c._getDeclaredMethods())
			if (hasName(m2.getName()) && Arrays.equals(_getParameterTypes(), m2._getParameterTypes()))
				return m2;
		return null;
	}

	MethodInfo[] _getMatching() {
		if (matching == null) {
			synchronized(this) {
				List<MethodInfo> l = findMatching(list(), this, getDeclaringClass());
				matching = l.toArray(new MethodInfo[l.size()]);
			}
		}
		return matching;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds the annotation of the specified type defined on this method.
	 *
	 * <p>
	 * If this is a method and the annotation cannot be found on the immediate method, searches methods with the same
	 * signature on the parent classes or interfaces.
	 * <br>The search is performed in child-to-parent order.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return The annotation if found, or <jk>null</jk> if not.
	 */
	public final <A extends Annotation> A getAnnotation(Class<A> type) {
		return getAnnotation(AnnotationProvider.DEFAULT, type);
	}

	/**
	 * Finds the annotation of the specified type defined on this method.
	 *
	 * <p>
	 * Searches all methods with the same signature on the parent classes or interfaces
	 * and the return type on the method.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return The first annotation found, or <jk>null</jk> if it doesn't exist.
	 */
	public final <A extends Annotation> A getAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		if (type == null)
			return null;
		Value<A> t = Value.empty();
		for (MethodInfo m2 : _getMatching()) {
			annotationProvider.forEachAnnotation(type, m2.inner(), x -> true, x -> t.set(x));
			if (t.isPresent())
				return t.get();
		}
		return null;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present on this method.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is present on this method.
	 */
	public final <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return hasAnnotation(AnnotationProvider.DEFAULT, type);
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present on this method.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is present on this method.
	 */
	public final <A extends Annotation> boolean hasAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		for (MethodInfo m2 : _getMatching())
			if (annotationProvider.firstAnnotation(type, m2.inner(), x -> true) != null)
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is not present on this method.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is not present on this method.
	 */
	public final <A extends Annotation> boolean hasNoAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		return ! hasAnnotation(annotationProvider, type);
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is not present on this method.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is not present on this method.
	 */
	public final <A extends Annotation> boolean hasNoAnnotation(Class<A> type) {
		return getAnnotation(type) == null;
	}

	/**
	 * Returns <jk>true</jk> if at least one of the specified annotation is present on this method.
	 *
	 * @param types The annotation to look for.
	 * @return <jk>true</jk> if at least one of the specified annotation is present on this method.
	 */
	@SafeVarargs
	public final boolean hasAnyAnnotations(Class<? extends Annotation>...types) {
		for (Class<? extends Annotation> a : types)
			if (hasAnnotation(a))
				return true;
		return false;
	}

	/**
	 * Performs an action on all matching annotations defined on this method.
	 *
	 * <p>
	 * Searches all methods with the same signature on the parent classes or interfaces
	 * and the return type on the method.
	 * <br>Results are parent-to-child ordered.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public <A extends Annotation> MethodInfo forEachAnnotation(Class<A> type, Predicate<A> filter, Consumer<A> action) {
		return forEachAnnotation(AnnotationProvider.DEFAULT, type, filter, action);
	}

	/**
	 * Performs an action on all matching annotations defined on this method.
	 *
	 * <p>
	 * Searches all methods with the same signature on the parent classes or interfaces
	 * and the return type on the method.
	 * <br>Results are parent-to-child ordered.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation type.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public <A extends Annotation> MethodInfo forEachAnnotation(AnnotationProvider annotationProvider, Class<A> type, Predicate<A> filter, Consumer<A> action) {
		declaringClass.forEachAnnotation(annotationProvider, type, filter, action);
		MethodInfo[] m = _getMatching();
		for (int i = m.length-1; i >= 0; i--)
			for (Annotation a2 : m[i]._getDeclaredAnnotations())
				consume(type, filter, action, a2);
		getReturnType().unwrap(Value.class,Optional.class).forEachAnnotation(annotationProvider, type, filter, action);
		return this;
	}

	/**
	 * Returns the first annotation in the specified list on this method.
	 *
	 * @param types The annotations to look for.
	 * @return The first matching annotation.
	 */
	@SafeVarargs
	public final Annotation getAnyAnnotation(Class<? extends Annotation>...types) {
		for (Class<? extends Annotation> cc : types) {
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
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * 	<li>On this method and matching methods ordered parent-to-child.
	 * </ol>
	 *
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public AnnotationList getAnnotationList() {
		return getAnnotationList(x -> true);
	}

	/**
	 * Constructs an {@link AnnotationList} of all matching annotations found on this method.
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
	 * @param filter A predicate to apply to the entries to determine if value should be added.  Can be <jk>null</jk>.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public AnnotationList getAnnotationList(Predicate<AnnotationInfo<?>> filter) {
		AnnotationList al = new AnnotationList();
		forEachAnnotationInfo(filter, x -> al.add(x));
		return al;
	}

	/**
	 * Same as {@link #getAnnotationList(Predicate)} except only returns annotations defined on methods.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be added.  Can be <jk>null</jk>.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public AnnotationList getAnnotationListMethodOnly(Predicate<AnnotationInfo<?>> filter) {
		AnnotationList al = new AnnotationList();
		forEachAnnotationInfoMethodOnly(filter, x -> al.add(x));
		return al;
	}

	/**
	 * Perform an action on all matching annotations on this method.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public MethodInfo forEachAnnotationInfo(Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		ClassInfo c = this.declaringClass;
		forEachDeclaredAnnotationInfo(c.getPackage(), filter, action);
		ClassInfo[] interfaces = c._getInterfaces();
		for (int i = interfaces.length-1; i >= 0; i--) {
			forEachDeclaredAnnotationInfo(interfaces[i], filter, action);
			forEachDeclaredMethodAnnotationInfo(interfaces[i], filter, action);
		}
		ClassInfo[] parents = c._getParents();
		for (int i = parents.length-1; i >= 0; i--) {
			forEachDeclaredAnnotationInfo(parents[i], filter, action);
			forEachDeclaredMethodAnnotationInfo(parents[i], filter, action);
		}
		return this;
	}

	private void forEachAnnotationInfoMethodOnly(Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		ClassInfo c = this.declaringClass;
		ClassInfo[] interfaces = c._getInterfaces();
		for (int i = interfaces.length-1; i >= 0; i--)
			forEachDeclaredMethodAnnotationInfo(interfaces[i], filter, action);
		ClassInfo[] parents = c._getParents();
		for (int i = parents.length-1; i >= 0; i--)
			forEachDeclaredMethodAnnotationInfo(parents[i], filter, action);
	}

	private void forEachDeclaredAnnotationInfo(Package p, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		if (p != null)
			for (Annotation a : p.getDeclaredAnnotations())
				AnnotationInfo.of(p, a).accept(filter, action);
	}

	private void forEachDeclaredAnnotationInfo(ClassInfo ci, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		if (ci != null)
			for (Annotation a : ci._getDeclaredAnnotations())
				AnnotationInfo.of(ci, a).accept(filter, action);
	}

	private void forEachDeclaredMethodAnnotationInfo(ClassInfo ci, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		MethodInfo m = findMatchingOnClass(ci);
		if (m != null)
			for (Annotation a : m._getDeclaredAnnotations())
				AnnotationInfo.of(m, a).accept(filter, action);
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
		if (returnType == null) {
			synchronized(this) {
				returnType = ClassInfo.of(m.getReturnType(), m.getGenericReturnType());
			}
		}
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
	 * Returns <jk>true</jk> if this object passes the specified predicate test.
	 *
	 * @param test The test to perform.
	 * @return <jk>true</jk> if this object passes the specified predicate test.
	 */
	public boolean matches(Predicate<MethodInfo> test) {
		return test(test, this);
	}

	/**
	 * Performs an action on this object if the specified predicate test passes.
	 *
	 * @param test A test to apply to determine if action should be executed.  Can be <jk>null</jk>.
	 * @param action An action to perform on this object.
	 * @return This object.
	 */
	public MethodInfo accept(Predicate<MethodInfo> test, Consumer<MethodInfo> action) {
		if (matches(test))
			action.accept(this);
		return this;
	}

	/**
	 * Shortcut for calling the invoke method on the underlying method.
	 *
	 * @param <T> The method return type.
	 * @param obj the object the underlying method is invoked from.
	 * @param args the arguments used for the method call
	 * @return The object returned from the method.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings("unchecked")
	public <T> T invoke(Object obj, Object...args) throws ExecutableException {
		try {
			return (T)m.invoke(obj, args);
		} catch (IllegalAccessException e) {
			throw new ExecutableException(e);
		} catch (InvocationTargetException e) {
			throw new ExecutableException(e.getTargetException());
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
		Class<?>[] pt = _getRawParamTypes();
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
	 * <p class='bjava'>
	 *
	 * 	<jc>// Example method:</jc>
	 * 	<jk>public void</jk> foo(String <jv>bar</jv>, Integer <jv>baz</jv>);
	 *
	 * 	argsOnlyOfType(<jv>fooMethod</jv>, String.<jk>class</jk>, Integer.<jk>class</jk>);  <jc>// True.</jc>
	 * 	argsOnlyOfType(<jv>fooMethod</jv>, String.<jk>class</jk>, Integer.<jk>class</jk>, Map.<jk>class</jk>);  <jc>// True.</jc>
	 * 	argsOnlyOfType(<jv>fooMethod</jv>, String.<jk>class</jk>);  <jc>// False.</jc>
	 * </p>
	 *
	 * @param args The valid class types (exact) for the arguments.
	 * @return <jk>true</jk> if the method parameters only consist of the types specified in the list.
	 */
	public boolean argsOnlyOfType(Class<?>...args) {
		for (Class<?> c1 : _getRawParamTypes()) {
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
	 * Returns <jk>true</jk> if this method has at least the specified parameters.
	 *
	 * <p>
	 * Method may or may not have additional parameters besides those specified.
	 *
	 * @param requiredParams The parameter types to check for.
	 * @return <jk>true</jk> if this method has at least the specified parameters.
	 */
	public boolean hasAllArgs(Class<?>...requiredParams) {
		List<Class<?>> rawParamTypes = getRawParamTypes();

		for (Class<?> c : requiredParams)
			if (! rawParamTypes.contains(c))
				return false;

		return true;
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified parameter.
	 *
	 * <p>
	 * Method may or may not have additional parameters besides the one specified.
	 *
	 * @param requiredParam The parameter type to check for.
	 * @return <jk>true</jk> if this method has at least the specified parameter.
	 */
	public boolean hasArg(Class<?> requiredParam) {
		return hasAllArgs(requiredParam);
	}

	/**
	 * Returns <jk>true</jk> if this method is a bridge method.
	 *
	 * @return <jk>true</jk> if this method is a bridge method.
	 */
	public boolean isBridge() {
		return m.isBridge();
	}

	/**
	 * Returns the name of this method.
	 *
	 * @return The name of this method
	 */
	public String getName() {
		return m.getName();
	}

	@Override
	public int compareTo(MethodInfo o) {
		int i = getSimpleName().compareTo(o.getSimpleName());
		if (i == 0) {
			i = _getRawParamTypes().length - o._getRawParamTypes().length;
			if (i == 0) {
				for (int j = 0; j < _getRawParamTypes().length && i == 0; j++) {
					i = _getRawParamTypes()[j].getName().compareTo(o._getRawParamTypes()[j].getName());
				}
			}
		}
		return i;
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.reflect.ExecutableInfo */
	public MethodInfo accessible() {
		super.accessible();
		return this;
	}

	// </FluentSetters>
}
