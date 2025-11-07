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
package org.apache.juneau.common.reflect;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.PredicateUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.beans.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.utils.*;

/**
 * Lightweight utility class for introspecting information about a method.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class MethodInfo extends ExecutableInfo implements Comparable<MethodInfo>, Annotatable {
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
	 * @param m The method being wrapped.
	 * @return A new {@link MethodInfo} object, or <jk>null</jk> if the method was null;
	 */
	public static MethodInfo of(Method m) {
		if (m == null)
			return null;
		return ClassInfo.of(m.getDeclaringClass()).getMethodInfo(m);
	}

	private final Method inner;
	private final Supplier<ClassInfo> returnType;
	private final Supplier<List<MethodInfo>> matchingMethods;
	private final Supplier<List<AnnotationInfo<Annotation>>> annotationInfos;
	private final Supplier<List<AnnotationInfo<Annotation>>> allAnnotationInfos;

	/**
	 * Constructor.
	 *
	 * @param declaringClass The class that declares this method.
	 * @param inner The method being wrapped.
	 */
	protected MethodInfo(ClassInfo declaringClass, Method inner) {
		super(declaringClass, inner);
		this.inner = inner;
		this.returnType = memoize(() -> ClassInfo.of(inner.getReturnType(), inner.getGenericReturnType()));
		this.matchingMethods = memoize(this::findMatchingMethods);
		this.annotationInfos = memoize(() -> getMatchingMethods().stream().flatMap(m -> m.getDeclaredAnnotationInfos().stream()).toList());
		this.allAnnotationInfos = memoize(this::findAllAnnotationInfos);
	}

	/**
	 * Returns this method and all matching methods up the hierarchy chain.
	 *
	 * <p>
	 * Searches parent classes and interfaces for methods with matching name and parameter types.
	 * Results are returned in the following order:
	 * <ol>
	 * 	<li>This method
	 * 	<li>Any matching methods on declared interfaces of this class
	 * 	<li>Matching method on the parent class
	 * 	<li>Any matching methods on the declared interfaces of the parent class
	 * 	<li>Continue up the hierarchy
	 * </ol>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Interface and class hierarchy:</jc>
	 * 	<jk>interface</jk> I1 {
	 * 		<jk>void</jk> foo(String <jv>s</jv>);
	 * 	}
	 * 	<jk>class</jk> A {
	 * 		<jk>void</jk> foo(String <jv>s</jv>) {}
	 * 	}
	 * 	<jk>interface</jk> I2 {
	 * 		<jk>void</jk> foo(String <jv>s</jv>);
	 * 	}
	 * 	<jk>class</jk> B <jk>extends</jk> A <jk>implements</jk> I2 {
	 * 		&#64;Override
	 * 		<jk>void</jk> foo(String <jv>s</jv>) {}
	 * 	}
	 * 	<jc>// For B.foo(), returns: [B.foo, I2.foo, A.foo, I1.foo]</jc>
	 * 	MethodInfo <jv>mi</jv> = ...;
	 * 	List&lt;MethodInfo&gt; <jv>matching</jv> = <jv>mi</jv>.getMatchingMethods();
	 * </p>
	 *
	 * @return A list of matching methods including this one, in child-to-parent order.
	 */
	public List<MethodInfo> getMatchingMethods() {
		return matchingMethods.get();
	}

	/**
	 * Returns all annotations on this method and parent overridden methods in child-to-parent order.
	 *
	 * <p>
	 * 	Results include annotations from:
	 * <ul>
	 * 	<li>This method
	 * 	<li>Matching methods in parent classes
	 * 	<li>Matching methods in interfaces
	 * </ul>
	 *
	 * <p>
	 * 	List is unmodifiable.
	 *
	 * @return A list of all annotations on this method and overridden methods.
	 */
	public List<AnnotationInfo<Annotation>> getAnnotationInfos() {
		return annotationInfos.get();
	}

	/**
	 * Returns all annotations of the specified type on this method and parent overridden methods.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to filter by.
	 * @return A stream of matching annotation infos.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> getAnnotationInfos(Class<A> type) {
		assertArgNotNull("type", type);
		return getAnnotationInfos().stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	/**
	 * Returns all annotations on the declaring class, this method and parent overridden methods, return type, and package in child-to-parent order.
	 *
	 * <p>
	 * 	Annotations are ordered as follows:
	 * <ol>
	 * 	<li>Current method
	 * 	<li>Parent methods (child-to-parent order)
	 * 	<li>Return type on current method
	 * 	<li>Return type on parent methods (child-to-parent order)
	 * 	<li>Current class
	 * 	<li>Parent classes/interfaces (child-to-parent order)
	 * 	<li>Package annotations on the declaring class's package
	 * </ol>
	 *
	 * <p>
	 * 	List is unmodifiable.
	 *
	 * @return A list of all annotations.
	 */
	public List<AnnotationInfo<Annotation>> getAllAnnotationInfos() {
		return allAnnotationInfos.get();
	}

	/**
	 * Returns all annotations of the specified type on the declaring class, this method and parent overridden methods, return type, and declaring class's package.
	 *
	 * <p>
	 * 	See {@link #getAllAnnotationInfos()} for ordering details.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to filter by.
	 * @return A stream of matching annotation infos.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> getAllAnnotationInfos(Class<A> type) {
		assertArgNotNull("type", type);
		return getAllAnnotationInfos().stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	private List<MethodInfo> findMatchingMethods() {
		var result = new ArrayList<MethodInfo>();
		result.add(this); // 1. This method

		var cc = getDeclaringClass();

		while (nn(cc)) {
			// 2. Add matching methods from declared interfaces of current class
			cc.getDeclaredInterfaces().stream()
				.forEach(di -> addMatchingMethodsFromInterface(result, di));

			// 3. Move to parent class
			cc = cc.getSuperclass();
			if (nn(cc)) {
				// Add matching method from parent class
				cc.getDeclaredMethods().stream()
					.filter(this::matches)
					.findFirst()
					.ifPresent(result::add);
			}
		}

		return result;
	}

	private void addMatchingMethodsFromInterface(List<MethodInfo> result, ClassInfo iface) {
		// Add matching methods from this interface
		iface.getDeclaredMethods().stream()
			.filter(this::matches)
			.forEach(result::add);

		// Recursively search parent interfaces
		iface.getDeclaredInterfaces().stream()
			.forEach(pi -> addMatchingMethodsFromInterface(result, pi));
	}

	@SuppressWarnings("unchecked")
	private List<AnnotationInfo<Annotation>> findAllAnnotationInfos() {
		var list = new ArrayList<AnnotationInfo<Annotation>>();
		var returnType = getReturnType().unwrap(Value.class, Optional.class);

		// 1. Current method
		list.addAll(getDeclaredAnnotationInfos());

		// 2. Parent methods in child-to-parent order
		getMatchingMethods().stream().skip(1).forEach(m -> list.addAll(m.getDeclaredAnnotationInfos()));

		// 3. Return type on current
		returnType.getDeclaredAnnotationInfos().forEach(x -> list.add(x));

		// 4. Return type on parent methods in child-to-parent order
		getMatchingMethods().stream().skip(1).forEach(m -> {
			m.getReturnType().unwrap(Value.class, Optional.class).getDeclaredAnnotationInfos().forEach(x -> list.add(x));
		});

		// 5. Current class
		declaringClass.getDeclaredAnnotationInfos().forEach(x -> list.add(x));

		// 6. Parent classes/interfaces in child-to-parent order
		declaringClass.getParentsAndInterfaces().stream().skip(1).forEach(c -> c.getDeclaredAnnotationInfos().forEach(x -> list.add(x)));

		// 7. Package annotations
		var pkg = declaringClass.getPackage();
		if (nn(pkg))
			pkg.getAnnotations().forEach(x -> list.add(x));

		return u(list);
	}

	@Override /* Overridden from ExecutableInfo */
	public MethodInfo accessible() {
		super.accessible();
		return this;
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
	 * 	<jv>fooMethod</jv>.hasOnlyParameterTypes(String.<jk>class</jk>, Integer.<jk>class</jk>);  <jc>// True.</jc>
	 * 	<jv>fooMethod</jv>.hasOnlyParameterTypes(String.<jk>class</jk>, Integer.<jk>class</jk>, Map.<jk>class</jk>);  <jc>// True.</jc>
	 * 	<jv>fooMethod</jv>.hasOnlyParameterTypes(String.<jk>class</jk>);  <jc>// False.</jc>
	 * </p>
	 *
	 * @param args The valid class types (exact) for the arguments.
	 * @return <jk>true</jk> if the method parameters only consist of the types specified in the list.
	 */
	public boolean hasOnlyParameterTypes(Class<?>...args) {
		for (var param : getParameters()) {
			var c1 = param.getParameterType().inner();
			boolean foundMatch = false;
			for (var c2 : args)
				if (c1 == c2)
					foundMatch = true;
			if (! foundMatch)
				return false;
		}
		return true;
	}

	@Override
	public int compareTo(MethodInfo o) {
		int i = getSimpleName().compareTo(o.getSimpleName());
		if (i == 0) {
			var params = getParameters();
			var oParams = o.getParameters();
			i = params.size() - oParams.size();
			if (i == 0) {
				for (int j = 0; j < params.size() && i == 0; j++) {
					i = params.get(j).getParameterType().getName().compareTo(oParams.get(j).getParameterType().getName());
				}
			}
		}
		return i;
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
	 */
	public <A extends Annotation> void forEachAnnotation(AnnotationProvider annotationProvider, Class<A> type, Predicate<A> filter, Consumer<A> action) {
		declaringClass.forEachAnnotation(annotationProvider, type, filter, action);
		rstream(getMatchingMethods())
			.flatMap(m -> m.getDeclaredAnnotationInfos().stream())
			.map(AnnotationInfo::inner)
			.filter(type::isInstance)
			.map(type::cast)
			.forEach(a -> consumeIf(filter, action, a));
		getReturnType().unwrap(Value.class, Optional.class).forEachAnnotation(annotationProvider, type, filter, action);
	}


	/**
	 * Returns the name of this method.
	 *
	 * @return The name of this method
	 */
	public String getName() { return inner.getName(); }

	/**
	 * Returns the bean property name if this is a getter or setter.
	 *
	 * @return The bean property name, or <jk>null</jk> if this isn't a getter or setter.
	 */
	public String getPropertyName() {
		String n = inner.getName();
		if ((n.startsWith("get") || n.startsWith("set")) && n.length() > 3)
			return Introspector.decapitalize(n.substring(3));
		if (n.startsWith("is") && n.length() > 2)
			return Introspector.decapitalize(n.substring(2));
		return n;
	}

	/**
	 * Returns the generic return type of this method as a {@link ClassInfo} object.
	 *
	 * @return The generic return type of this method.
	 */
	public ClassInfo getReturnType() {
		return returnType.get();
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
		var sb = new StringBuilder(128);
		sb.append(inner.getName());
		var params = getParameters();
		if (params.size() > 0) {
			sb.append('(');
			for (int i = 0; i < params.size(); i++) {
				if (i > 0)
					sb.append(',');
				params.get(i).getParameterType().appendNameFormatted(sb, ClassNameFormat.FULL, true, '$', ClassArrayFormat.BRACKETS);
			}
			sb.append(')');
		}
		return sb.toString();
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
	public boolean hasAllParameters(Class<?>...requiredParams) {
		var paramTypes = getParameters().stream()
			.map(p -> p.getParameterType().inner())
			.toList();

		for (var c : requiredParams)
			if (! paramTypes.contains(c))
				return false;

		return true;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present on this method.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is present on this method.
	 */
	public <A extends Annotation> boolean hasAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		// Inline Context.firstAnnotation() call
		for (var m2 : getMatchingMethods())
			if (nn(annotationProvider.find(type, m2.inner()).map(x -> x.inner()).filter(x -> true).findFirst().orElse(null)))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present on this method or any matching methods in parent classes/interfaces.
	 *
	 * <p>
	 * This method searches through all matching methods in the hierarchy.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is present on this method.
	 */
	@Override
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		// Inline implementation using reflection directly instead of delegating to AnnotationProvider.DEFAULT
		if (!nn(type))
			return false;
		for (var m2 : getMatchingMethods())
			if (m2.inner().getAnnotation(type) != null)
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if at least one of the specified annotation is present on this method.
	 *
	 * @param types The annotation to look for.
	 * @return <jk>true</jk> if at least one of the specified annotation is present on this method.
	 */
	@SafeVarargs
	public final boolean hasAnyAnnotations(Class<? extends Annotation>...types) {
		return getAnnotationInfos().stream().anyMatch(ai -> stream(types).anyMatch(t -> t.isInstance(ai.inner())));
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
	public boolean hasParameter(Class<?> requiredParam) {
		return hasAllParameters(requiredParam);
	}

	/**
	 * Returns <jk>true</jk> if this method has this return type.
	 *
	 * @param c The return type to test for.
	 * @return <jk>true</jk> if this method has this return type.
	 */
	public boolean hasReturnType(Class<?> c) {
		return inner.getReturnType() == c;
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
		return ClassInfo.of(c).isParentOf(inner.getReturnType());
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

	/**
	 * Returns the wrapped method.
	 *
	 * @return The wrapped method.
	 */
	public Method inner() {
		return inner;
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
			return (T)inner.invoke(obj, args);
		} catch (IllegalAccessException e) {
			throw new ExecutableException(e);
		} catch (InvocationTargetException e) {
			throw new ExecutableException(e.getTargetException());
		}
	}

	/**
	 * Invokes the specified method using lenient argument matching.
	 *
	 * <p>
	 * Lenient matching allows arguments to be matched to parameters based on parameter types.
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
	public Object invokeLenient(Object pojo, Object...args) throws ExecutableException {
		try {
			return inner.invoke(pojo, ClassUtils.getMatchingArgs(inner.getParameterTypes(), args));
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new ExecutableException(e);
		}
	}

	@Override
	public boolean is(ElementFlag flag) {
		return switch (flag) {
			case BRIDGE -> isBridge();
			case NOT_BRIDGE -> !isBridge();
			case DEFAULT -> isDefault();
			case NOT_DEFAULT -> !isDefault();
			default -> super.is(flag);
		};
	}

	@Override
	public boolean isAll(ElementFlag...flags) {
		return stream(flags).allMatch(this::is);
	}

	@Override
	public boolean isAny(ElementFlag...flags) {
		return stream(flags).anyMatch(this::is);
	}

	/**
	 * Returns <jk>true</jk> if this method is a bridge method.
	 *
	 * @return <jk>true</jk> if this method is a bridge method.
	 */
	public boolean isBridge() { return inner.isBridge(); }

	/**
	 * Returns <jk>true</jk> if this method matches the specified method by name and parameter types.
	 *
	 * @param m The method to compare against.
	 * @return <jk>true</jk> if this method has the same name and parameter types as the specified method.
	 */
	public boolean matches(MethodInfo m) {
		return hasName(m.getName()) && hasMatchingParameters(m.getParameters());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// High Priority Methods (direct Method API compatibility)
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns a {@link Type} object that represents the formal return type of the method.
	 *
	 * <p>
	 * Same as calling {@link Method#getGenericReturnType()}.
	 *
	 * <p>
	 * If the return type is a parameterized type, the {@link Type} object returned reflects the actual type parameters used in the source code.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For method: public List&lt;String&gt; getValues()</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"getValues"</js>);
	 * 	Type <jv>returnType</jv> = <jv>mi</jv>.getGenericReturnType();
	 * 	<jk>if</jk> (<jv>returnType</jv> <jk>instanceof</jk> ParameterizedType) {
	 * 		ParameterizedType <jv>pType</jv> = (ParameterizedType)<jv>returnType</jv>;
	 * 		<jc>// pType.getActualTypeArguments()[0] is String.class</jc>
	 * 	}
	 * </p>
	 *
	 * @return A {@link Type} object representing the formal return type.
	 * @see Method#getGenericReturnType()
	 */
	public Type getGenericReturnType() {
		return inner.getGenericReturnType();
	}

	/**
	 * Returns an {@link AnnotatedType} object that represents the use of a type to specify the return type of the method.
	 *
	 * <p>
	 * Same as calling {@link Method#getAnnotatedReturnType()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For method: public @NotNull String getName()</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"getName"</js>);
	 * 	AnnotatedType <jv>aType</jv> = <jv>mi</jv>.getAnnotatedReturnType();
	 * 	<jc>// Check for @NotNull on the return type</jc>
	 * </p>
	 *
	 * @return An {@link AnnotatedType} object representing the return type.
	 * @see Method#getAnnotatedReturnType()
	 */
	public AnnotatedType getAnnotatedReturnType() {
		return inner.getAnnotatedReturnType();
	}

	/**
	 * Returns the default value for the annotation member represented by this method.
	 *
	 * <p>
	 * Same as calling {@link Method#getDefaultValue()}.
	 *
	 * <p>
	 * Returns <jk>null</jk> if this method is not an annotation member, or if the annotation member has no default value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @interface MyAnnotation { String value() default "default"; }</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyAnnotation.<jk>class</jk>).getMethod(<js>"value"</js>);
	 * 	Object <jv>defaultValue</jv> = <jv>mi</jv>.getDefaultValue();
	 * 	<jc>// defaultValue is "default"</jc>
	 * </p>
	 *
	 * @return The default value, or <jk>null</jk> if none.
	 * @see Method#getDefaultValue()
	 */
	public Object getDefaultValue() {
		return inner.getDefaultValue();
	}

	/**
	 * Returns <jk>true</jk> if this method is a default method (Java 8+ interface default method).
	 *
	 * <p>
	 * Same as calling {@link Method#isDefault()}.
	 *
	 * <p>
	 * A default method is a public non-abstract instance method (i.e., non-static method with a body) declared in an interface.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For interface: interface MyInterface { default String getName() { return "default"; } }</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyInterface.<jk>class</jk>).getMethod(<js>"getName"</js>);
	 * 	<jk>if</jk> (<jv>mi</jv>.isDefault()) {
	 * 		<jc>// This is a default interface method</jc>
	 * 	}
	 * </p>
	 *
	 * @return <jk>true</jk> if this method is a default method.
	 * @see Method#isDefault()
	 */
	public boolean isDefault() {
		return inner.isDefault();
	}

	MethodInfo findMatchingOnClass(ClassInfo c) {
		for (var m2 : c.getDeclaredMethods())
		if (hasName(m2.getName()) && hasParameterTypes(m2.getParameters().stream().map(ParameterInfo::getParameterType).toArray(ClassInfo[]::new)))
			return m2;
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotatable interface methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() {
		return AnnotatableType.METHOD_TYPE;
	}

	@Override /* Annotatable */
	public ClassInfo getClassInfo() {
		return getDeclaringClass();
	}

	@Override /* Annotatable */
	public String getAnnotatableName() {
		return getShortName();
	}
}