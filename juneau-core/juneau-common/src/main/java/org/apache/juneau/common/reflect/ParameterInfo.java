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


import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.PredicateUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.collections.*;

/**
 * Lightweight utility class for introspecting information about a method parameter.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ParameterInfo extends ElementInfo implements Annotatable {

	private final ExecutableInfo executable;
	private final Parameter parameter;
	private final int index;
	private final ClassInfo type;

	@SuppressWarnings({"rawtypes"})
	private final Cache foundAnnotations = Cache.of(Class.class, List.class).supplier(this::findAnnotationInfosInternal).build();

	private final Supplier<List<AnnotationInfo<Annotation>>> annotations = memoize(this::_findAnnotations);
	private final Supplier<List<ParameterInfo>> matchingParameters = memoize(this::_findMatchingParameters);
	private final Supplier<String> foundName = memoize(this::findNameInternal);

	/**
	 * Constructor.
	 *
	 * @param eInfo The constructor or method wrapper.
	 * @param p The parameter being wrapped.
	 * @param index The parameter index.
	 * @param type The parameter type.
	 */
	// TODO - Investigate if we can construct ClassInfo directly from parameter.
	protected ParameterInfo(ExecutableInfo eInfo, Parameter p, int index, ClassInfo type) {
		super(p.getModifiers());
		this.executable = eInfo;
		this.parameter = p;
		this.index = index;
		this.type = type;
	}

	/**
	 * Returns the wrapped {@link Parameter} object.
	 *
	 * @return The wrapped {@link Parameter} object.
	 */
	public Parameter inner() {
		return parameter;
	}

	private List<AnnotationInfo<Annotation>> _findAnnotations() {
		return stream(parameter.getAnnotations()).map(a -> AnnotationInfo.of(this, a)).toList();
	}

	/**
	 * Returns <jk>true</jk> if this parameter can accept the specified value.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if this parameter can accept the specified value.
	 */
	public boolean canAccept(Object value) {
		return getParameterType().isInstance(value);
	}

	/**
	 * Performs an action on all matching annotations on this parameter.
	 *
	 * <p>
	 * Searches all methods with the same signature on the parent classes or interfaces
	 * and the return type on the method.
	 * <p>
	 * Results are in parent-to-child order.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation type to look for.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public <A extends Annotation> ParameterInfo forEachAnnotation(Class<A> type, Predicate<A> filter, Consumer<A> action) {
		return forEachAnnotation(AnnotationProvider.DEFAULT, type, filter, action);
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> getAnnotations(Class<A> type) {
		return getAnnotationInfos().stream().filter(x -> x.isType(type)).map(x -> (AnnotationInfo<A>)x);
	}

	/**
	 * Returns this parameter and all matching parameters in parent classes.
	 *
	 * <p>
	 * For constructors, searches parent class constructors for parameters with matching name and type,
	 * regardless of parameter count or position. This allows finding annotated parameters that may be
	 * inherited by child classes even when constructor signatures differ.
	 *
	 * <p>
	 * For methods, searches matching methods (same signature) in parent classes/interfaces
	 * for parameters at the same index with matching name and type.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Constructor with different parameter counts:</jc>
	 * 	<jk>class</jk> A {
	 * 		A(String <jv>foo</jv>, <jk>int</jk> <jv>bar</jv>) {}
	 * 	}
	 * 	<jk>class</jk> B <jk>extends</jk> A {
	 * 		B(String <jv>foo</jv>) {}
	 * 	}
	 * 	<jc>// For B's foo parameter, returns: [B.foo, A.foo]</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	List&lt;ParameterInfo&gt; <jv>matching</jv> = <jv>pi</jv>.getMatchingParameters();
	 * </p>
	 *
	 * @return A list of matching parameters including this one, in child-to-parent order.
	 */
	public List<ParameterInfo> getMatchingParameters() {
		return matchingParameters.get();
	}

	private List<ParameterInfo> _findMatchingParameters() {
		if (executable.isConstructor()) {
			// For constructors: search parent class constructors for parameters with matching index and type
			// Note: We match by index and type only, not by name, to avoid circular dependency
			// (getName() needs getMatchingParameters() which needs getName())
			var ci = (ConstructorInfo)executable;
			var list = new ArrayList<ParameterInfo>();

			// Add this parameter first
			list.add(this);

			// Search parent classes for matching parameters
			var cc = ci.getDeclaringClass().getSuperclass();
			while (nn(cc)) {
				// Check all constructors in parent class
				for (var pc : cc.getDeclaredConstructors()) {
					// Check if constructor has parameter at this index with matching type
					var params = pc.getParameters();
					if (index < params.size() && getParameterType().is(params.get(index).getParameterType())) {
						list.add(params.get(index));
					}
				}
				cc = cc.getSuperclass();
			}

			return list;
		}
		// For methods: use matching methods from parent classes
		return ((MethodInfo)executable).getMatchingMethods().stream().map(m -> m.getParameter(index)).toList();
	}

	/**
	 * Finds all annotation infos of the specified type defined on this method parameter.
	 *
	 * <p>
	 * Searches through matching parameters in the hierarchy and the parameter type.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation type to look for.
	 * @return A list of annotation infos found, or an empty list if none found.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> List<AnnotationInfo<A>> findAnnotationInfos(Class<A> type) {
		return (List<AnnotationInfo<A>>)foundAnnotations.get(type);
	}

	/**
	 * Finds the first annotation info of the specified type defined on this method parameter.
	 *
	 * <p>
	 * Searches through matching parameters in the hierarchy and the parameter type.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation type to look for.
	 * @return The annotation info if found, or <jk>null</jk> if not.
	 */
	public <A extends Annotation> AnnotationInfo<A> findAnnotationInfo(Class<A> type) {
		var list = findAnnotationInfos(type);
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * Returns the constructor that this parameter belongs to.
	 *
	 * @return The constructor that this parameter belongs to, or <jk>null</jk> if it belongs to a method.
	 */
	public ConstructorInfo getConstructor() { return executable.isConstructor() ? (ConstructorInfo)executable : null; }

	/**
	 * Returns the specified parameter annotation declared on this parameter.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation type to look for.
	 * @return The specified parameter annotation declared on this parameter, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getDeclaredAnnotation(Class<A> type) {
		if (nn(type))
			for (var a : getAnnotations())
				if (type.isInstance(a))
					return type.cast(a);
		return null;
	}

	/**
	 * Returns the index position of this parameter.
	 *
	 * @return The index position of this parameter.
	 */
	public int getIndex() { return index; }

	/**
	 * Returns the method that this parameter belongs to.
	 *
	 * @return The method that this parameter belongs to, or <jk>null</jk> if it belongs to a constructor.
	 */
	public MethodInfo getMethod() { return executable.isConstructor() ? null : (MethodInfo)executable; }

	/**
	 * Helper method to extract the name from any annotation with the simple name "Name".
	 *
	 * <p>
	 * This method uses reflection to find any annotation with the simple name "Name"
	 * and dynamically invokes its <c>value()</c> method to retrieve the parameter name.
	 * This allows it to work with any <c>@Name</c> annotation from any package without
	 * creating a compile-time dependency.
	 *
	 * @return The name from the annotation, or <jk>null</jk> if no compatible annotation is found.
	 */
	private String findNameFromAnnotation() {
		for (var annotation : parameter.getAnnotations()) {
			var annotationType = annotation.annotationType();
			if ("Name".equals(annotationType.getSimpleName())) {
				try {
					var valueMethod = annotationType.getMethod("value");
					if (valueMethod.getReturnType() == String.class) {
						var value = valueMethod.invoke(annotation);
						if (value instanceof String)
							return (String)value;
					}
				} catch (Exception e) {
					// Ignore - annotation doesn't have a compatible value() method
				}
			}
		}
		return null;
	}

	/**
	 * Finds the name of this parameter by searching the hierarchy.
	 *
	 * <p>
	 * Searches for the parameter name in the following order:
	 * <ol>
	 * 	<li>@Name annotation value (takes precedence over bytecode parameter names)
	 * 	<li>Bytecode parameter name (if compiled with -parameters flag)
	 * 	<li>Matching parameters in parent classes/interfaces (for methods)
	 * </ol>
	 *
	 * @return The parameter name if found, or <jk>null</jk> if not available.
	 */
	public String findName() {
		return foundName.get();
	}

	private String findNameInternal() {
		// Search through matching parameters in hierarchy
		for (var mp : getMatchingParameters()) {
			// Check for @Name annotation first (overrides bytecode names)
			String annotationName = mp.findNameFromAnnotation();
			if (annotationName != null)
				return annotationName;
			
			// Then check if bytecode name is present
			if (mp.parameter.isNamePresent())
				return mp.parameter.getName();
		}
		return null;
	}

	/**
	 * Returns the name of the parameter.
	 *
	 * <p>
	 * Searches for the name in the following order:
	 * <ol>
	 * 	<li>@Name annotation value (takes precedence over bytecode parameter names)
	 * 	<li>Bytecode parameter name (if compiled with -parameters flag)
	 * 	<li>Matching parameters in parent classes/interfaces (for methods)
	 * 	<li>Synthetic name like "arg0", "arg1", etc. (fallback)
	 * </ol>
	 *
	 * <p>
	 * This method works with any annotation named "Name" (from any package) that has a <c>String value()</c> method.
	 *
	 * @return The name of the parameter, never <jk>null</jk>.
	 * @see Parameter#getName()
	 */
	public String getName() {
		String name = findName();
		return name != null ? name : parameter.getName();
	}

	/**
	 * Returns all annotations declared on this parameter.
	 *
	 * <p>
	 * Returns annotations directly declared on this parameter, wrapped as {@link AnnotationInfo} objects.
	 *
	 * @return An unmodifiable list of annotations on this parameter, never <jk>null</jk>.
	 */
	public List<AnnotationInfo<Annotation>> getAnnotationInfos() {
		return annotations.get();
	}

	/**
	 * Returns the class type of this parameter.
	 *
	 * @return The class type of this parameter.
	 */
	public ClassInfo getParameterType() { return type; }

	/**
	 * Returns <jk>true</jk> if this parameter has the specified annotation.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation type to look for.
	 * @return
	 * 	The <jk>true</jk> if annotation if found.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return nn(findAnnotationInfo(type));
	}

	/**
	 * Returns <jk>true</jk> if the parameter has a name.
	 *
	 * <p>
	 * This returns <jk>true</jk> if the parameter has an annotation with the simple name "Name",
	 * or if the parameter's name is present in the class file.
	 *
	 * @return <jk>true</jk> if the parameter has a name.
	 */
	public boolean hasName() {
		return findName() != null;
	}

	/**
	 * Returns <jk>true</jk> if this parameter doesn't have the specified annotation.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation type to look for.
	 * @return
	 * 	The <jk>true</jk> if annotation if not found.
	 */
	public <A extends Annotation> boolean hasNoAnnotation(Class<A> type) {
		return ! hasAnnotation(type);
	}

	/**
	 * Returns <jk>true</jk> if the parameter type is an exact match for the specified class.
	 *
	 * @param c The type to check.
	 * @return <jk>true</jk> if the parameter type is an exact match for the specified class.
	 */
	public boolean isType(Class<?> c) {
		return getParameterType().is(c);
	}

	/**
	 * Returns <jk>true</jk> if this object passes the specified predicate test.
	 *
	 * @param test The test to perform.
	 * @return <jk>true</jk> if this object passes the specified predicate test.
	 */
	public boolean matches(Predicate<ParameterInfo> test) {
		return test(test, this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// High Priority Methods (direct Parameter API compatibility)
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the {@link ExecutableInfo} which declares this parameter.
	 *
	 * <p>
	 * Same as calling {@link Parameter#getDeclaringExecutable()} but returns {@link ExecutableInfo} instead.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get the method or constructor that declares this parameter</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	ExecutableInfo <jv>executable</jv> = <jv>pi</jv>.getDeclaringExecutable();
	 * 	<jk>if</jk> (<jv>executable</jv>.isConstructor()) {
	 * 		ConstructorInfo <jv>ci</jv> = (ConstructorInfo)<jv>executable</jv>;
	 * 	}
	 * </p>
	 *
	 * @return The {@link ExecutableInfo} declaring this parameter.
	 * @see Parameter#getDeclaringExecutable()
	 */
	public ExecutableInfo getDeclaringExecutable() {
		return executable;
	}

	/**
	 * Returns the Java language modifiers for the parameter represented by this object, as an integer.
	 *
	 * <p>
	 * The {@link java.lang.reflect.Modifier} class should be used to decode the modifiers.
	 *
	 * <p>
	 * Same as calling {@link Parameter#getModifiers()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if parameter is final</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	<jk>int</jk> <jv>modifiers</jv> = <jv>pi</jv>.getModifiers();
	 * 	<jk>boolean</jk> <jv>isFinal</jv> = Modifier.<jsm>isFinal</jsm>(<jv>modifiers</jv>);
	 * </p>
	 *
	 * @return The Java language modifiers for this parameter.
	 * @see Parameter#getModifiers()
	 * @see java.lang.reflect.Modifier
	 */
	@Override
	public int getModifiers() {
		return parameter.getModifiers();
	}

	/**
	 * Returns <jk>true</jk> if the parameter has a name according to the <c>.class</c> file.
	 *
	 * <p>
	 * Same as calling {@link Parameter#isNamePresent()}.
	 *
	 * <p>
	 * <b>Note:</b> This method is different from {@link #hasName()} which also checks for the presence
	 * of a <c>@Name</c> annotation. This method only checks if the name is present in the bytecode.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if parameter name is in bytecode</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	<jk>if</jk> (<jv>pi</jv>.isNamePresent()) {
	 * 		String <jv>name</jv> = <jv>pi</jv>.getName();
	 * 	}
	 * </p>
	 *
	 * @return <jk>true</jk> if the parameter has a name in the bytecode.
	 * @see Parameter#isNamePresent()
	 * @see #hasName()
	 */
	public boolean isNamePresent() {
		return parameter.isNamePresent();
	}

	/**
	 * Returns <jk>true</jk> if this parameter is implicitly declared in source code.
	 *
	 * <p>
	 * Returns <jk>true</jk> if this parameter is neither explicitly nor implicitly declared in source code.
	 *
	 * <p>
	 * Same as calling {@link Parameter#isImplicit()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Filter out implicit parameters</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	<jk>if</jk> (! <jv>pi</jv>.isImplicit()) {
	 * 		<jc>// Process explicit parameter</jc>
	 * 	}
	 * </p>
	 *
	 * @return <jk>true</jk> if this parameter is implicitly declared.
	 * @see Parameter#isImplicit()
	 */
	public boolean isImplicit() {
		return parameter.isImplicit();
	}

	/**
	 * Returns <jk>true</jk> if this parameter is a synthetic construct as defined by the Java Language Specification.
	 *
	 * <p>
	 * Same as calling {@link Parameter#isSynthetic()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Filter out compiler-generated parameters</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	<jk>if</jk> (! <jv>pi</jv>.isSynthetic()) {
	 * 		<jc>// Process real parameter</jc>
	 * 	}
	 * </p>
	 *
	 * @return <jk>true</jk> if this parameter is a synthetic construct.
	 * @see Parameter#isSynthetic()
	 */
	public boolean isSynthetic() {
		return parameter.isSynthetic();
	}

	@Override
	public boolean is(ElementFlag flag) {
		return switch (flag) {
			case SYNTHETIC -> isSynthetic();
			case NOT_SYNTHETIC -> !isSynthetic();
			case VARARGS -> isVarArgs();
			case NOT_VARARGS -> !isVarArgs();
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
	 * Returns <jk>true</jk> if this parameter represents a variable argument list.
	 *
	 * <p>
	 * Same as calling {@link Parameter#isVarArgs()}.
	 *
	 * <p>
	 * Only returns <jk>true</jk> for the last parameter of a variable arity method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if this is a varargs parameter</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	<jk>if</jk> (<jv>pi</jv>.isVarArgs()) {
	 * 		<jc>// Handle variable arguments</jc>
	 * 	}
	 * </p>
	 *
	 * @return <jk>true</jk> if this parameter represents a variable argument list.
	 * @see Parameter#isVarArgs()
	 */
	public boolean isVarArgs() {
		return parameter.isVarArgs();
	}

	/**
	 * Returns a {@link Type} object that identifies the parameterized type for this parameter.
	 *
	 * <p>
	 * Same as calling {@link Parameter#getParameterizedType()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get generic type information for parameter: List&lt;String&gt; values</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	Type <jv>type</jv> = <jv>pi</jv>.getParameterizedType();
	 * 	<jk>if</jk> (<jv>type</jv> <jk>instanceof</jk> ParameterizedType) {
	 * 		ParameterizedType <jv>pType</jv> = (ParameterizedType)<jv>type</jv>;
	 * 		<jc>// pType.getActualTypeArguments()[0] is String.class</jc>
	 * 	}
	 * </p>
	 *
	 * @return A {@link Type} object identifying the parameterized type.
	 * @see Parameter#getParameterizedType()
	 */
	public Type getParameterizedType() {
		return parameter.getParameterizedType();
	}

	/**
	 * Returns an {@link AnnotatedType} object that represents the use of a type to specify the type of this parameter.
	 *
	 * <p>
	 * Same as calling {@link Parameter#getAnnotatedType()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get annotated type: void method(@NotNull String value)</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	AnnotatedType <jv>aType</jv> = <jv>pi</jv>.getAnnotatedType();
	 * 	<jc>// Check for @NotNull on the type</jc>
	 * </p>
	 *
	 * @return An {@link AnnotatedType} object representing the type of this parameter.
	 * @see Parameter#getAnnotatedType()
	 */
	public AnnotatedType getAnnotatedType() {
		return parameter.getAnnotatedType();
	}

	/**
	 * Returns annotations that are <em>present</em> on this parameter.
	 *
	 * <p>
	 * Same as calling {@link Parameter#getAnnotations()}.
	 *
	 * <p>
	 * <b>Note:</b> This returns the simple array of annotations directly present on the parameter.
	 * For Juneau's enhanced annotation searching (through class hierarchies), use {@link #findAnnotationInfo(Class)} instead.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all annotations on parameter</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	Annotation[] <jv>annotations</jv> = <jv>pi</jv>.getAnnotations();
	 * </p>
	 *
	 * @return Annotations present on this parameter, or an empty array if there are none.
	 * @see Parameter#getAnnotations()
	 */
	public Annotation[] getAnnotations() {
		return parameter.getAnnotations();
	}

	/**
	 * Returns annotations that are <em>directly present</em> on this parameter.
	 *
	 * <p>
	 * Same as calling {@link Parameter#getDeclaredAnnotations()}.
	 *
	 * <p>
	 * <b>Note:</b> This returns the simple array of declared annotations.
	 * For Juneau's enhanced annotation searching, use {@link #getDeclaredAnnotation(Class)} instead.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get declared annotations on parameter</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	Annotation[] <jv>annotations</jv> = <jv>pi</jv>.getDeclaredAnnotations();
	 * </p>
	 *
	 * @return Annotations directly present on this parameter, or an empty array if there are none.
	 * @see Parameter#getDeclaredAnnotations()
	 */
	public Annotation[] getDeclaredAnnotations() {
		return parameter.getDeclaredAnnotations();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Medium Priority Methods (repeatable annotations)
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns this element's annotations of the specified type (including repeated annotations).
	 *
	 * <p>
	 * Same as calling {@link Parameter#getAnnotationsByType(Class)}.
	 *
	 * <p>
	 * This method handles repeatable annotations by "looking through" container annotations.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all @Author annotations (including repeated): @Authors({@Author(...), @Author(...)})</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	Author[] <jv>authors</jv> = <jv>pi</jv>.getAnnotationsByType(Author.<jk>class</jk>);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return All this element's annotations of the specified type, or an empty array if there are none.
	 * @see Parameter#getAnnotationsByType(Class)
	 */
	public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
		return parameter.getAnnotationsByType(annotationClass);
	}

	/**
	 * Returns this element's declared annotations of the specified type (including repeated annotations).
	 *
	 * <p>
	 * Same as calling {@link Parameter#getDeclaredAnnotationsByType(Class)}.
	 *
	 * <p>
	 * This method handles repeatable annotations by "looking through" container annotations,
	 * but only examines annotations directly declared on this parameter (not inherited).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get declared @Author annotations (including repeated)</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	Author[] <jv>authors</jv> = <jv>pi</jv>.getDeclaredAnnotationsByType(Author.<jk>class</jk>);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return All this element's declared annotations of the specified type, or an empty array if there are none.
	 * @see Parameter#getDeclaredAnnotationsByType(Class)
	 */
	public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
		return parameter.getDeclaredAnnotationsByType(annotationClass);
	}

	@Override
	public String toString() {
		return (executable.getSimpleName()) + "[" + index + "]";
	}

	@SuppressWarnings("unchecked")
	private <A extends Annotation> List<AnnotationInfo<A>> findAnnotationInfosInternal(Class<A> type) {
		var list = new ArrayList<AnnotationInfo<A>>();

		// Search through matching parameters in hierarchy
		for (var mp : getMatchingParameters()) {
			mp.getAnnotationInfos().stream()
				.filter(x -> x.isType(type))
				.map(x -> (AnnotationInfo<A>)x)
				.forEach(list::add);
		}

		// Search on parameter type
		var paramType = executable.getParameter(index).getParameterType().unwrap(Value.class, Optional.class);
		paramType.getDeclaredAnnotationInfos().stream()
			.filter(x -> x.isType(type))
			.map(x -> (AnnotationInfo<A>)x)
			.forEach(list::add);

		return list;
	}

	private <A extends Annotation> ParameterInfo forEachAnnotation(AnnotationProvider ap, Class<A> a, Predicate<A> filter, Consumer<A> action) {
		if (executable.isConstructor) {
			var ci = executable.getParameter(index).getParameterType().unwrap(Value.class, Optional.class);
			var annotations = getAnnotations();
			ci.forEachAnnotation(ap, a, filter, action);
			for (var a2 : annotations)
				if (a.isInstance(a2))
					consumeIf(filter, action, a.cast(a2));
		} else {
			var mi = (MethodInfo)executable;
			var ci = executable.getParameter(index).getParameterType().unwrap(Value.class, Optional.class);
			ci.forEachAnnotation(ap, a, filter, action);
			mi.forEachMatchingParentFirst(x -> true, x -> x.forEachParameterAnnotation(index, a, filter, action));
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotatable interface methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() {
		return AnnotatableType.PARAMETER_TYPE;
	}

	@Override /* Annotatable */
	public ClassInfo getClassInfo() {
		return getDeclaringExecutable().getDeclaringClass();
	}

	@Override /* Annotatable */
	public String getAnnotatableName() {
		return getName();
	}
}