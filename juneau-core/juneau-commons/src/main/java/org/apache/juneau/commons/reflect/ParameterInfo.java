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
package org.apache.juneau.commons.reflect;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.utils.*;

/**
 * Lightweight utility class for introspecting information about a method or constructor parameter.
 *
 * <p>
 * This class provides a convenient wrapper around {@link Parameter} that extends the standard Java reflection
 * API with additional functionality for parameter introspection, annotation handling, and name resolution.
 * It supports resolving parameter names from bytecode (when compiled with <c>-parameters</c>) or from
 * {@link org.apache.juneau.annotation.Name @Name} annotations.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Parameter introspection - access parameter metadata, type, annotations
 * 	<li>Name resolution - resolve parameter names from bytecode or annotations
 * 	<li>Qualifier support - extract qualifier names from annotations
 * 	<li>Hierarchy traversal - find matching parameters in parent methods/constructors
 * 	<li>Annotation support - get annotations declared on the parameter
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Introspecting parameter metadata for code generation or analysis
 * 	<li>Resolving parameter names for dependency injection frameworks
 * 	<li>Finding annotations on parameters
 * 	<li>Working with parameter types and qualifiers
 * 	<li>Building frameworks that need to analyze method/constructor signatures
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Get ParameterInfo from a method</jc>
 * 	MethodInfo <jv>mi</jv> = ...;
 * 	ParameterInfo <jv>param</jv> = <jv>mi</jv>.getParameters().get(0);
 *
	 * 	<jc>// Get parameter type</jc>
	 * 	ClassInfo <jv>type</jv> = <jv>param</jv>.getParameterType();
	 *
	 * 	<jc>// Get resolved name (from bytecode or @Name annotation)</jc>
	 * 	String <jv>name</jv> = <jv>param</jv>.getResolvedName();
	 *
	 * 	<jc>// Get annotations</jc>
	 * 	List&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>annotations</jv> =
	 * 		<jv>param</jv>.getAnnotations(MyAnnotation.<jk>class</jk>).toList();
	 * </p>
	 *
	 * <h5 class='section'>Parameter Name Resolution:</h5>
	 * <p>
	 * Parameter names are resolved in the following order:
	 * <ol class='spaced-list'>
	 * 	<li>{@link org.apache.juneau.annotation.Name @Name} annotation value (if present)
	 * 	<li>Bytecode parameter names (if compiled with <c>-parameters</c> flag)
	 * 	<li><c>arg0</c>, <c>arg1</c>, etc. (fallback if names unavailable)
	 * </ol>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link MethodInfo} - Method introspection
	 * 	<li class='jc'>{@link ConstructorInfo} - Constructor introspection
	 * 	<li class='jc'>{@link ExecutableInfo} - Common executable functionality
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonReflect">juneau-common-reflect</a>
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

	/**
	 * Creates a ParameterInfo wrapper for the specified parameter.
	 *
	 * <p>
	 * This convenience method automatically determines the declaring executable from the parameter
	 * and finds the matching ParameterInfo.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Parameter <jv>p</jv> = ...;
	 * 	ParameterInfo <jv>pi</jv> = ParameterInfo.<jsm>of</jsm>(<jv>p</jv>);
	 * </p>
	 *
	 * @param inner The parameter being wrapped. Must not be <jk>null</jk>.
	 * @return A ParameterInfo object wrapping the parameter.
	 * @throws IllegalArgumentException If the parameter is <jk>null</jk> or cannot be found in its declaring executable.
	 */
	public static ParameterInfo of(Parameter inner) {
		assertArgNotNull("inner", inner);
		var exec = inner.getDeclaringExecutable();
		ExecutableInfo execInfo = exec instanceof Constructor ? ConstructorInfo.of((Constructor<?>)exec) : MethodInfo.of((Method)exec);
		var params = execInfo.getParameters();
		for (var param : params) {
			if (param.inner() == inner) {
				return param;
			}
		}
		throw new IllegalArgumentException("Parameter not found in declaring executable: " + inner);
	}

	static void reset() {
		DISABLE_PARAM_NAME_DETECTION.reset();
	}

	private final ExecutableInfo executable;
	private final Parameter inner;

	private final int index;
	private final ClassInfo type;
	private final Supplier<List<AnnotationInfo<Annotation>>> annotations;  // All annotations declared directly on this parameter.
	private final Supplier<List<ParameterInfo>> matchingParameters;  // Matching parameters in parent methods.

	private final ResettableSupplier<String> resolvedName = memoizeResettable(this::findNameInternal);  // Resolved name from @Name annotation or bytecode.

	private final ResettableSupplier<String> resolvedQualifier = memoizeResettable(this::findQualifierInternal);  // Resolved qualifier from @Named annotation.

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new ParameterInfo wrapper for the specified parameter. This constructor is protected
	 * and should not be called directly. ParameterInfo instances are typically obtained from
	 * {@link ExecutableInfo#getParameters()} or {@link #of(Parameter)}.
	 *
	 * @param executable The ExecutableInfo (MethodInfo or ConstructorInfo) that contains this parameter.
	 * @param inner The parameter being wrapped.
	 * @param index The zero-based index of this parameter in the method/constructor signature.
	 * @param type The ClassInfo representing the parameter type.
	 */
	// TODO - Investigate if we can construct ClassInfo directly from parameter.
	protected ParameterInfo(ExecutableInfo executable, Parameter inner, int index, ClassInfo type) {
		super(inner.getModifiers());
		this.executable = executable;
		this.inner = inner;
		this.index = index;
		this.type = type;
		this.annotations = memoize(() -> stream(inner.getAnnotations()).flatMap(a -> AnnotationUtils.streamRepeated(a)).map(a -> ai(this, a)).toList());
		this.matchingParameters = memoize(this::findMatchingParameters);
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

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() { return AnnotatableType.PARAMETER_TYPE; }

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
	public AnnotatedType getAnnotatedType() { return inner.getAnnotatedType(); }

	/**
	 * Returns all annotations declared on this parameter.
	 *
	 * <p>
	 * Returns annotations directly declared on this parameter, wrapped as {@link AnnotationInfo} objects.
	 *
	 * <p>
	 * <b>Note on Repeatable Annotations:</b>
	 * Repeatable annotations (those marked with {@link java.lang.annotation.Repeatable @Repeatable}) are automatically
	 * expanded into their individual annotation instances. For example, if a parameter has multiple {@code @Bean} annotations,
	 * this method returns each {@code @Bean} annotation separately, rather than the container annotation.
	 *
	 * @return
	 * 	An unmodifiable list of annotations on this parameter, never <jk>null</jk>.
	 * 	<br>Repeatable annotations are expanded into individual instances.
	 */
	public List<AnnotationInfo<Annotation>> getAnnotations() { return annotations.get(); }

	/**
	 * Returns a stream of annotation infos of the specified type declared on this parameter.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type.
	 * @return A stream of annotation infos, never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> getAnnotations(Class<A> type) {
		return getAnnotations().stream().filter(x -> x.isType(type)).map(x -> (AnnotationInfo<A>)x);
	}

	/**
	 * Returns the constructor that this parameter belongs to.
	 *
	 * @return The constructor that this parameter belongs to, or <jk>null</jk> if it belongs to a method.
	 */
	public ConstructorInfo getConstructor() { return executable.isConstructor() ? (ConstructorInfo)executable : null; }

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
	public ExecutableInfo getDeclaringExecutable() { return executable; }

	/**
	 * Returns the index position of this parameter.
	 *
	 * @return The index position of this parameter.
	 */
	public int getIndex() { return index; }

	@Override /* Annotatable */
	public String getLabel() {
		var exec = getDeclaringExecutable();
		var label = exec.getDeclaringClass().getNameSimple() + "." + exec.getShortName();
		return label + "[" + index + "]";
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
	public List<ParameterInfo> getMatchingParameters() { return matchingParameters.get(); }

	/**
	 * Returns the method that this parameter belongs to.
	 *
	 * @return The method that this parameter belongs to, or <jk>null</jk> if it belongs to a constructor.
	 */
	public MethodInfo getMethod() { return executable.isConstructor() ? null : (MethodInfo)executable; }

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
	public int getModifiers() { return inner.getModifiers(); }

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
		var name = getResolvedName();
		return name != null ? name : inner.getName();
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
	public Type getParameterizedType() { return inner.getParameterizedType(); }

	/**
	 * Returns the class type of this parameter.
	 *
	 * @return The class type of this parameter.
	 */
	public ClassInfo getParameterType() { return type; }

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
	public String getResolvedName() { return resolvedName.get(); }

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
	public String getResolvedQualifier() { return resolvedQualifier.get(); }

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
	 * Returns the wrapped {@link Parameter} object.
	 *
	 * @return The wrapped {@link Parameter} object.
	 */
	public Parameter inner() {
		return inner;
	}

	@Override
	public boolean is(ElementFlag flag) {
		return switch (flag) {
			case SYNTHETIC -> isSynthetic();
			case NOT_SYNTHETIC -> ! isSynthetic();
			case VARARGS -> isVarArgs();
			case NOT_VARARGS -> ! isVarArgs();
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
	public boolean isImplicit() { return inner.isImplicit(); }

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
	public boolean isNamePresent() { return inner.isNamePresent(); }

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
	public boolean isSynthetic() { return inner.isSynthetic(); }

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
	public boolean isVarArgs() { return inner.isVarArgs(); }

	@Override
	public String toString() {
		return (executable.getSimpleName()) + "[" + index + "]";
	}

	private List<ParameterInfo> findMatchingParameters() {
		if (executable instanceof ConstructorInfo executable2) {
			// For constructors: search parent class constructors for parameters with matching index and type
			// Note: We match by index and type only, not by name, to avoid circular dependency
			// (getName() needs getMatchingParameters() which needs getName())
			var list = new ArrayList<ParameterInfo>();

			// Add this parameter first
			list.add(this);

			// Search parent classes for matching parameters
			var cc = executable2.getDeclaringClass().getSuperclass();
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

	//-----------------------------------------------------------------------------------------------------------------
	// Annotatable interface methods
	//-----------------------------------------------------------------------------------------------------------------

	private String findNameInternal() {
		// Search through matching parameters in hierarchy for @Name annotations only.
		// Note: We intentionally prioritize @Name annotations over bytecode parameter names
		// because bytecode names are unreliable - users may or may not compile with -parameters flag.
		for (var mp : getMatchingParameters()) {
			for (var ai : mp.getAnnotations()) {
				if (ai.hasSimpleName("Name")) {
					var value = ai.getValue().orElse(null);
					if (value != null)
						return value;
				}
			}
		}

		// Fall back to bytecode parameter name if available and not disabled
		if (! DISABLE_PARAM_NAME_DETECTION.get() && inner.isNamePresent()) {
			return inner.getName();
		}

		return null;
	}

	private String findQualifierInternal() {
		// Search through matching parameters in hierarchy for @Named or javax.inject.Qualifier annotations
		// @formatter:off
		return getMatchingParameters().stream()
			.flatMap(mp -> mp.getAnnotations().stream())
			.filter(ai -> ai.hasSimpleName("Named") || ai.hasSimpleName("Qualifier"))
			.map(ai -> ai.getValue().orElse(null))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
		// @formatter:on
	}
}