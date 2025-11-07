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
import org.apache.juneau.common.function.ResettableSupplier;

/**
 * Lightweight utility class for introspecting information about a method parameter.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ParameterInfo extends ElementInfo implements Annotatable {

	/**
	 * Resettable supplier for the system property to disable bytecode parameter name detection.
	 *
	 * <p>
	 * When the value is <jk>true</jk>, parameter names will only come from {@link org.apache.juneau.annotation.Name @Name}
	 * annotations and not from bytecode parameter names (even if compiled with <c>-parameters</c> flag).
	 *
	 * <p>
	 * This can be set via system property: <c>juneau.disableParamNameDetection=true</c>
	 *
	 * <p>
	 * The supplier can be reset for testing purposes using {@link #resetDisableParamNameDetection()}.
	 */
	static final ResettableSupplier<Boolean> DISABLE_PARAM_NAME_DETECTION = memoizeResettable(() -> Boolean.getBoolean("juneau.disableParamNameDetection"));

	private final ExecutableInfo executable;
	private final Parameter inner;
	private final int index;
	private final ClassInfo type;

	@SuppressWarnings({"rawtypes","unchecked"})
	private final Cache<Class,List<AnnotationInfo<Annotation>>> allAnnotations = Cache.<Class,List<AnnotationInfo<Annotation>>>create().supplier((k) -> findAllAnnotationInfos(k)).build();

	private final Supplier<List<AnnotationInfo<Annotation>>> declaredAnnotations;  // All annotations declared directly on this parameter.
	private final Supplier<List<ParameterInfo>> matchingParameters;  // Matching parameters in parent methods.
	private final ResettableSupplier<String> resolvedName = memoizeResettable(this::findNameInternal);  // Resolved name from @Name annotation or bytecode.
	private final ResettableSupplier<String> resolvedQualifier = memoizeResettable(this::findQualifierInternal);  // Resolved qualifier from @Named annotation.

	/**
	 * Constructor.
	 *
	 * @param executable The constructor or method wrapper.
	 * @param inner The parameter being wrapped.
	 * @param index The parameter index.
	 * @param type The parameter type.
	 */
	// TODO - Investigate if we can construct ClassInfo directly from parameter.
	protected ParameterInfo(ExecutableInfo executable, Parameter inner, int index, ClassInfo type) {
		super(inner.getModifiers());
		this.executable = executable;
		this.inner = inner;
		this.index = index;
		this.type = type;
		this.declaredAnnotations = memoize(() -> stream(inner.getAnnotations()).map(a -> AnnotationInfo.of(this, a)).toList());
		this.matchingParameters = memoize(this::findMatchingParameters);
	}

	/**
	 * Returns the wrapped {@link Parameter} object.
	 *
	 * @return The wrapped {@link Parameter} object.
	 */
	public Parameter inner() {
		return inner;
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
		return declaredAnnotations.get();
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
		// Inline implementation using reflection directly instead of delegating to AnnotationProvider.DEFAULT
		if (!nn(type))
			return this;

		if (executable.isConstructor()) {
			// For constructors: search parameter type hierarchy and parameter annotations
			var ci = executable.getParameter(index).getParameterType().unwrap(Value.class, Optional.class);
			// Search class hierarchy using reflection (package -> interfaces -> parents -> class)
			var packageAnn = ci.getPackageAnnotation(type);
			if (nn(packageAnn))
				consumeIf(filter, action, packageAnn);
			// Get annotations from interfaces (reverse order)
			var interfaces2 = ci.getInterfaces();
			for (int i = interfaces2.size() - 1; i >= 0; i--)
				for (var ann : interfaces2.get(i).inner().getDeclaredAnnotationsByType(type))
					consumeIf(filter, action, ann);
			// Get annotations from parent classes (reverse order)
			var parents2 = ci.getParents();
			for (int i = parents2.size() - 1; i >= 0; i--)
				for (var ann : parents2.get(i).inner().getDeclaredAnnotationsByType(type))
					consumeIf(filter, action, ann);
			// Get annotations directly from parameter
			var annotationInfos = getAnnotationInfos();
			for (var ai : annotationInfos)
				if (type.isInstance(ai.inner()))
					consumeIf(filter, action, type.cast(ai.inner()));
		} else {
			// For methods: search parameter type hierarchy and matching parent methods
			var mi = (MethodInfo)executable;
			var ci = executable.getParameter(index).getParameterType().unwrap(Value.class, Optional.class);
			// Search class hierarchy using reflection (package -> interfaces -> parents -> class)
			var packageAnn = ci.getPackageAnnotation(type);
			if (nn(packageAnn))
				consumeIf(filter, action, packageAnn);
			// Get annotations from interfaces (reverse order)
			var interfaces2 = ci.getInterfaces();
			for (int i = interfaces2.size() - 1; i >= 0; i--)
				for (var ann : interfaces2.get(i).inner().getDeclaredAnnotationsByType(type))
					consumeIf(filter, action, ann);
			// Get annotations from parent classes (reverse order)
			var parents2 = ci.getParents();
			for (int i = parents2.size() - 1; i >= 0; i--)
				for (var ann : parents2.get(i).inner().getDeclaredAnnotationsByType(type))
					consumeIf(filter, action, ann);
				// Get annotations from matching parent methods' parameters
				rstream(mi.getMatchingMethods()).forEach(x -> {
					x.getParameter(index).getAnnotationInfos().stream()
						.filter(ai -> type.isInstance(ai.inner()))
						.map(ai -> type.cast(ai.inner()))
						.forEach(ann -> consumeIf(filter, action, ann));
				});
		}
		return this;
	}

	/**
	 * Returns a stream of annotation infos of the specified type declared on this parameter.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type.
	 * @return A stream of annotation infos, never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> getAnnotationInfos(Class<A> type) {
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

	private List<ParameterInfo> findMatchingParameters() {
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
	 * Returns all annotation infos of the specified type defined on this parameter.
	 *
	 * <p>
	 * Performs a comprehensive search through the parameter hierarchy and parameter type hierarchy.
	 *
	 * <h5 class='section'>Search Order (child-to-parent):</h5>
	 * <ol>
	 * 	<li><b>Matching parameters in hierarchy</b>
	 * 		<ul>
	 * 			<li>For methods: This parameter → parent method parameters (via {@link #getMatchingParameters()})
	 * 			<li>For constructors: This parameter → parent constructor parameters (via {@link #getMatchingParameters()})
	 * 		</ul>
	 * 	<li><b>Parameter type hierarchy</b> (via {@link ClassInfo#getParentsAndInterfaces()})
	 * 		<ul>
	 * 			<li>Parameter's class and its interfaces (interleaved)
	 * 			<li>Parameter's parent classes and their interfaces (interleaved)
	 * 			<li>Continues up to Object class
	 * 		</ul>
	 * 	<li><b>Package annotation</b> - Package of the parameter type
	 * </ol>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Given:</jc>
	 * 	<jk>class</jk> Parent {
	 * 		<jk>void</jk> method(@MyAnnotation String <jv>param</jv>) {}
	 * 	}
	 * 	<jk>class</jk> Child <jk>extends</jk> Parent {
	 * 		<ja>@Override</ja>
	 * 		<jk>void</jk> method(@MyAnnotation String <jv>param</jv>) {}
	 * 	}
	 *
	 * 	<jc>// Search order for Child.method parameter:</jc>
	 * 	ParameterInfo <jv>pi</jv> = ClassInfo.<jsm>of</jsm>(Child.<jk>class</jk>).getMethod(<js>"method"</js>, String.<jk>class</jk>).getParameter(0);
	 * 	List&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>annotations</jv> = <jv>pi</jv>.getAllAnnotationInfos(MyAnnotation.<jk>class</jk>);
	 * 	<jc>// Returns (in order):</jc>
	 * 	<jc>//   1. @MyAnnotation on Child.method parameter</jc>
	 * 	<jc>//   2. @MyAnnotation on Parent.method parameter</jc>
	 * 	<jc>//   3. Any @MyAnnotation on String class hierarchy</jc>
	 * 	<jc>//   4. Any @MyAnnotation on java.lang package</jc>
	 * </p>
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation type to look for.
	 * @return An unmodifiable list of annotation infos in child-to-parent order, or an empty list if none found.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <A extends Annotation> List<AnnotationInfo<A>> getAllAnnotationInfos(Class<A> type) {
		return (List)allAnnotations.get(type);
	}

	/**
	 * Returns the first annotation info of the specified type defined on this parameter.
	 *
	 * <p>
	 * This is a convenience method that returns the first result from {@link #getAllAnnotationInfos(Class)}.
	 *
	 * <p>
	 * Performs a comprehensive search through the parameter hierarchy and parameter type hierarchy
	 * in child-to-parent order, returning the first annotation found.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation type to look for.
	 * @return The first annotation info if found (closest to this parameter), or <jk>null</jk> if not found.
	 * @see #getAllAnnotationInfos(Class)
	 */
	public <A extends Annotation> AnnotationInfo<A> getAllAnnotationInfo(Class<A> type) {
		var list = getAllAnnotationInfos(type);
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
			for (var ai : getAnnotationInfos())
				if (type.isInstance(ai.inner()))
					return type.cast(ai.inner());
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
	 * Finds the name of this parameter for bean property mapping.
	 *
	 * <p>
	 * Searches for the parameter name in the following order:
	 * <ol>
	 * 	<li>{@link org.apache.juneau.annotation.Name @Name} annotation value
	 * 	<li>Bytecode parameter name (if available and not disabled via system property)
	 * 	<li>Matching parameters in parent classes/interfaces
	 * </ol>
	 *
	 * <p>
	 * This method is used for mapping constructor parameters to bean properties.
	 *
	 * <p>
	 * <b>Note:</b> This is different from {@link #getResolvedQualifier()} which looks for {@link org.apache.juneau.annotation.Named @Named}
	 * annotations for bean injection purposes.
	 *
	 * @return The parameter name if found, or <jk>null</jk> if not available.
	 * @see #getResolvedQualifier()
	 * @see #getName()
	 */
	public String getResolvedName() {
		return resolvedName.get();
	}

	static void reset() {
		DISABLE_PARAM_NAME_DETECTION.reset();
	}

	private String findNameInternal() {
		// Search through matching parameters in hierarchy for @Name annotations only.
		// Note: We intentionally prioritize @Name annotations over bytecode parameter names
		// because bytecode names are unreliable - users may or may not compile with -parameters flag.
		for (var mp : getMatchingParameters()) {
			for (var ai : mp.getAnnotationInfos()) {
				if (ai.hasSimpleName("Name")) {
					String value = ai.getValue().orElse(null);
					if (value != null)
						return value;
				}
			}
		}

		// Fall back to bytecode parameter name if available and not disabled
		if (!DISABLE_PARAM_NAME_DETECTION.get() && inner.isNamePresent()) {
			return inner.getName();
		}

		return null;
	}

	/**
	 * Finds the bean injection qualifier for this parameter.
	 *
	 * <p>
	 * Searches for the {@link org.apache.juneau.annotation.Named @Named} annotation value to determine
	 * which named bean should be injected.
	 *
	 * <p>
	 * This method is used by the {@link org.apache.juneau.cp.BeanStore} for bean injection.
	 *
	 * <p>
	 * <b>Note:</b> This is different from {@link #getResolvedName()} which looks for {@link org.apache.juneau.annotation.Name @Name}
	 * annotations for bean property mapping.
	 *
	 * @return The bean qualifier name if {@code @Named} annotation is found, or <jk>null</jk> if not annotated.
	 * @see #getResolvedName()
	 */
	public String getResolvedQualifier() {
		return resolvedQualifier.get();
	}

	private String findQualifierInternal() {
		// Search through matching parameters in hierarchy for @Named or javax.inject.Qualifier annotations
		return getMatchingParameters().stream()
			.flatMap(mp -> mp.getAnnotationInfos().stream())
			.filter(ai -> ai.hasSimpleName("Named") || ai.hasSimpleName("Qualifier"))
			.map(ai -> ai.getValue().orElse(null))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
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
		String name = getResolvedName();
		return name != null ? name : inner.getName();
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
		return nn(getAllAnnotationInfo(type));
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
		return getResolvedName() != null;
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
		return inner.getModifiers();
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
		return inner.isNamePresent();
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
		return inner.isImplicit();
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
		return inner.isSynthetic();
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
		return inner.isVarArgs();
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
		return inner.getParameterizedType();
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
		return inner.getAnnotatedType();
	}

	/**
	 * Returns annotations that are <em>directly present</em> on this parameter.
	 *
	 * <p>
	 * Same as calling {@link Parameter#getDeclaredAnnotations()}.
	 *
	 * <p>
	 * <b>Note:</b> This returns the simple array of declared annotations.
	 * For Juneau's enhanced annotation searching, use {@link #findAnnotationInfo(Class)} instead.
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
		return inner.getDeclaredAnnotations();
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
		return inner.getDeclaredAnnotationsByType(annotationClass);
	}

	@Override
	public String toString() {
		return (executable.getSimpleName()) + "[" + index + "]";
	}

	@SuppressWarnings("unchecked")
	private <A extends Annotation> List<AnnotationInfo<A>> findAllAnnotationInfos(Class<A> type) {
		var list = new ArrayList<AnnotationInfo<A>>();

		// Search through matching parameters in hierarchy (child-to-parent order)
		for (var mp : getMatchingParameters()) {
			mp.getAnnotationInfos().stream()
				.filter(x -> x.isType(type))
				.map(x -> (AnnotationInfo<A>)x)
				.forEach(list::add);
		}

		// Search parameter type hierarchy in child-to-parent order (interleaved classes and interfaces)
		var paramType = executable.getParameter(index).getParameterType().unwrap(Value.class, Optional.class);
		
		// Traverse parent classes and interfaces (child-to-parent, interleaved)
		var parentsAndInterfaces = paramType.getParentsAndInterfaces();
		for (int i = 0; i < parentsAndInterfaces.size(); i++) {
			parentsAndInterfaces.get(i).getDeclaredAnnotationInfos().stream()
				.filter(x -> x.isType(type))
				.map(x -> (AnnotationInfo<A>)x)
				.forEach(list::add);
		}
		
		// Package annotation (last)
		var packageAnn = paramType.getPackageAnnotation(type);
		if (nn(packageAnn))
			list.add(AnnotationInfo.of(paramType, packageAnn));

		return list;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotatable interface methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() {
		return AnnotatableType.PARAMETER_TYPE;
	}

	@Override /* Annotatable */
	public String getLabel() {
		var exec = getDeclaringExecutable();
		var label = exec.getDeclaringClass().getNameSimple() + "." + exec.getShortName();
		return label + "[" + index + "]";
	}
}