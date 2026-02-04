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

import static org.apache.juneau.commons.reflect.ClassArrayFormat.*;
import static org.apache.juneau.commons.reflect.ClassNameFormat.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.inject.*;
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
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsReflection">Reflection Package</a>
	 * </ul>
	 */
@SuppressWarnings("java:S115")
public class ParameterInfo extends ElementInfo implements Annotatable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_inner = "inner";

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
	static final ResettableSupplier<Boolean> DISABLE_PARAM_NAME_DETECTION = memr(() -> Boolean.getBoolean("juneau.disableParamNameDetection"));

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
		assertArgNotNull(ARG_inner, inner);
		var exec = inner.getDeclaringExecutable();
		ExecutableInfo execInfo;
		if (exec instanceof Constructor<?> c)
			execInfo = ConstructorInfo.of(c);
		else
			execInfo = MethodInfo.of((Method)exec);
		return execInfo.getParameters().stream()
			.filter(x -> eq(x.inner(), inner))
			.findFirst()
			.orElse(null);
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

	private final ResettableSupplier<String> resolvedName = memr(this::findNameInternal);  // Resolved name from @Name annotation or bytecode.

	private final ResettableSupplier<String> resolvedQualifier = memr(this::findQualifierInternal);  // Resolved qualifier from @Named or @Qualifier annotation.

	private final Supplier<String> toString;  // String representation with modifiers, type, name, and varargs flag.

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
		this.annotations = mem(() -> stream(inner.getAnnotations()).flatMap(AnnotationUtils::streamRepeated).map(a -> ai(this, a)).toList());
		this.matchingParameters = mem(this::findMatchingParameters);
		this.toString = mem(this::findToString);
	}

	/**
	 * Returns <jk>true</jk> if this parameter can accept the specified value.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if this parameter can accept the specified value.
	 */
	public boolean canAccept(Object value) {
		return getParameterType().canAcceptArg(value);
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
		var label = exec.getDeclaringClass().getNameSimple() + "." + exec.getNameShort();
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
	 * Searches for the {@link org.apache.juneau.annotation.Named @Named} or {@code javax.inject.Qualifier @Qualifier}
	 * annotation value to determine which named bean should be injected.
	 *
	 * <p>
	 * This method is used by the {@link org.apache.juneau.cp.BeanStore} for bean injection.
	 *
	 * <p>
	 * <b>Note:</b> This is different from {@link #getResolvedName()} which looks for {@link org.apache.juneau.annotation.Name @Name}
	 * annotations for bean property mapping.
	 *
	 * @return The bean qualifier name if {@code @Named} or {@code @Qualifier} annotation is found, or <jk>null</jk> if not annotated.
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

	/**
	 * Compares this ParameterInfo with the specified object for equality.
	 *
	 * <p>
	 * Two ParameterInfo objects are considered equal if they wrap the same underlying {@link Parameter} object.
	 * This delegates to the underlying {@link Parameter#equals(Object)} method.
	 *
	 * <p>
	 * This method makes ParameterInfo suitable for use as keys in hash-based collections such as {@link HashMap}
	 * and {@link HashSet}.
	 *
	 * @param obj The object to compare with.
	 * @return <jk>true</jk> if the objects are equal, <jk>false</jk> otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ParameterInfo other && eq(this, other, (x, y) -> eq(x.inner, y.inner));
	}

	/**
	 * Returns a hash code value for this ParameterInfo.
	 *
	 * <p>
	 * This delegates to the underlying {@link Parameter#hashCode()} method.
	 *
	 * <p>
	 * This method makes ParameterInfo suitable for use as keys in hash-based collections such as {@link HashMap}
	 * and {@link HashSet}.
	 *
	 * @return A hash code value for this ParameterInfo.
	 */
	@Override
	public int hashCode() {
		return inner.hashCode();
	}

	@Override
	public boolean is(ElementFlag flag) {
		return switch (flag) {
			case SYNTHETIC -> isSynthetic();
			case NOT_SYNTHETIC -> ! isSynthetic();  // HTT
			case VARARGS -> isVarArgs();
			case NOT_VARARGS -> ! isVarArgs();
			default -> super.is(flag);
		};
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

	/**
	 * Returns a detailed string representation of this parameter.
	 *
	 * <p>
	 * The returned string includes:
	 * <ul>
	 * 	<li>Modifiers (final)
	 * 	<li>Parameter type with generics (e.g., "List&lt;String&gt;")
	 * 	<li>Parameter name (if available)
	 * 	<li>Varargs indicator (if applicable)
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Simple parameter</jc>
	 * 	ParameterInfo <jv>pi</jv> = ...;
	 * 	<jv>pi</jv>.toString();
	 * 	<jc>// Returns: "java.lang.String name"</jc>
	 *
	 * 	<jc>// Final parameter</jc>
	 * 	<jc>// Returns: "final java.lang.String name"</jc>
	 *
	 * 	<jc>// Generic parameter</jc>
	 * 	<jc>// Returns: "java.util.List&lt;java.lang.String&gt; items"</jc>
	 *
	 * 	<jc>// Varargs parameter</jc>
	 * 	<jc>// Returns: "java.lang.String... values"</jc>
	 *
	 * 	<jc>// Parameter without name (fallback)</jc>
	 * 	<jc>// Returns: "int arg0"</jc>
	 * </p>
	 *
	 * @return A detailed string representation including modifiers, type, name, and varargs flag.
	 */
	@Override
	public String toString() {
		return toString.get();
	}

	private String findToString() {
		var sb = new StringBuilder(128);

		// Modifiers (final is common for parameters)
		var mods = Modifier.toString(getModifiers());
		if (nn(mods) && ! mods.isEmpty()) {
			sb.append(mods).append(" ");
		}

		// Parameter type (use generic type if available to show generics)
		var paramType = getParameterizedType();

		// For varargs, we need to get the component type and display it with "..." instead of "[]"
		if (isVarArgs()) {
			// Get the component type of the array
			var typeInfo = ClassInfo.of(paramType);
			var componentType = typeInfo.getComponentType();
			// Display the component type without array brackets, then add "..."
			componentType.appendNameFormatted(sb, FULL, true, '$', BRACKETS);
			sb.append("...");
		} else {
			ClassInfo.of(paramType).appendNameFormatted(sb, FULL, true, '$', BRACKETS);
		}

		// Parameter name (if available)
		var name = getResolvedName();
		if (nn(name)) {
			sb.append(" ").append(name);
		} else {
			// Fallback to index-based name if no name available
			sb.append(" arg").append(index);
		}

		return sb.toString();
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
				if (ai.hasNameSimple("Name")) {
					var value = ai.getValue().orElse(null);
					if (value != null)  // HTT
						return value;
				}
			}
		}

		return opt(inner).filter(x -> x.isNamePresent()).filter(x -> ! DISABLE_PARAM_NAME_DETECTION.get()).map(x -> x.getName()).orElse(null);
	}

	private String findQualifierInternal() {
		// Search through matching parameters in hierarchy for @Named or javax.inject.Qualifier annotations
		// @formatter:off
		return getMatchingParameters().stream()
			.flatMap(mp -> mp.getAnnotations().stream())
			.filter(ai -> ai.hasNameSimple("Named") || ai.hasNameSimple("Qualifier"))
			.map(ai -> ai.getValue().orElse(null))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
		// @formatter:on
	}

	/**
	 * Checks if this parameter can be resolved from the bean store.
	 *
	 * <p>
	 * The following parameter types are considered optional and always return <jk>true</jk>:
	 * <ul>
	 * 	<li>Parameters of type <c>Optional&lt;T&gt;</c>
	 * 	<li>Parameters of type <c>T[]</c>, <c>List&lt;T&gt;</c>, <c>Set&lt;T&gt;</c>, or <c>Map&lt;String,T&gt;</c>
	 * </ul>
	 *
	 * <p>
	 * If a parameter has a {@link org.apache.juneau.annotation.Named @Named} or {@code @Qualifier} annotation,
	 * the method checks for a bean with that specific name in the bean store.  Otherwise, it checks for an unnamed bean
	 * of the parameter type in the bean store, and if not found, checks the <c>otherBeans</c> parameter.
	 *
	 * @param beanStore The bean store to check for beans.
	 * @param otherBeans Optional additional bean instances to check if not found in the bean store.
	 * 	These are checked after the bean store but before returning <jk>false</jk>.
	 * @return <jk>true</jk> if this parameter can be resolved, <jk>false</jk> otherwise.
	 */
	public boolean canResolve(BeanStore beanStore, Object... otherBeans) {
		var pt = getParameterType();

		if (pt.is(Optional.class) || pt.isInjectCollectionType()) // Optional/Collection/array types are always satisfied (even if empty).
			return true;

		var bq = getResolvedQualifier();

		if (nn(bq))
			return beanStore.hasBean(pt.inner(), bq);

		if (beanStore.hasBean(pt.inner()))
			return true;

		for (var o : otherBeans)
			if (canAccept(o))
				return true;
		return false;
	}

	/**
	 * Returns the missing type name if this parameter cannot be resolved, <jk>null</jk> otherwise.
	 *
	 * <p>
	 * The following parameter types are considered optional and always return <jk>null</jk>:
	 * <ul>
	 * 	<li>Parameters of type <c>Optional&lt;T&gt;</c>
	 * 	<li>Parameters of type <c>T[]</c>, <c>List&lt;T&gt;</c>, <c>Set&lt;T&gt;</c>, or <c>Map&lt;String,T&gt;</c>
	 * </ul>
	 *
	 * <p>
	 * If a parameter has a {@link org.apache.juneau.annotation.Named @Named} or {@code @Qualifier} annotation,
	 * the method checks for a bean with that specific name in the bean store.  Otherwise, it checks for an unnamed bean
	 * of the parameter type in the bean store, and if not found, checks the <c>otherBeans</c> parameter.
	 *
	 * @param beanStore The bean store to check for beans.
	 * @param otherBeans Optional additional bean instances to check if not found in the bean store.
	 * 	These are checked after the bean store but before marking the parameter as missing.
	 * @return Missing type name (e.g., <js>"String"</js> or <js>"String@name"</js> for qualified beans) or <jk>null</jk> if resolvable.
	 */
	public String getMissingType(BeanStore beanStore, Object... otherBeans) {
		var pt = getParameterType();
		if (pt.is(Optional.class) || pt.isInjectCollectionType()) // Optional/Collection/array types are always satisfied (even if empty).
			return null;
		var bq = getResolvedQualifier();  // Use @Named/@Qualified for bean injection
		if (nn(bq)) {
			if (! beanStore.hasBean(pt.inner(), bq))
				return pt.getNameSimple() + '@' + bq;
			return null;
		}
		if (beanStore.hasBean(pt.inner()))
			return null;
		for (var o : otherBeans)
			if (canAccept(o))
				return null;
		return pt.getNameSimple();
	}

	/**
	 * Resolves the parameter value from the bean store.
	 *
	 * <p>
	 * For each parameter, this method:
	 * <ul>
	 * 	<li>If the parameter is a collection/array/map type, collects all beans of the element type.
	 * 	<li>Otherwise, looks up a single bean by type and optional qualifier name in the bean store.
	 * 	<li>If not found in the bean store, checks the <c>otherBeans</c> parameter.
	 * </ul>
	 *
	 * <h5 class='section'>Parameter Resolution:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>Single beans</b> - Resolved using {@link BeanStore#getBean(Class)} or {@link BeanStore#getBean(Class, String)}.
	 * 		If not found, checks <c>otherBeans</c> for a compatible instance.
	 * 		Throws {@link ExecutableException} if not found in either location.
	 * 	<li><b>Optional beans</b> - Wrapped in <c>Optional</c>, or <c>Optional.empty()</c> if not found.
	 * 		Never throws an exception.
	 * 	<li><b>Arrays</b> - All beans of the element type are collected into an array (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Lists</b> - All beans of the element type are collected into a <c>List</c> (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Sets</b> - All beans of the element type are collected into a <c>LinkedHashSet</c> (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Maps</b> - All beans of the value type are collected into a <c>LinkedHashMap</c> keyed by bean name (may be empty).
	 * 		Unnamed beans use an empty string as the key.  Never throws an exception.
	 * </ul>
	 *
	 * @param beanStore The bean store to resolve beans from.
	 * @param otherBeans Optional additional bean instances to use if not found in the bean store.
	 * 	These are checked after the bean store but before throwing an exception.
	 * @return The resolved parameter value.
	 * @throws ExecutableException If a required parameter (non-Optional, non-collection) cannot be resolved
	 * 	from the bean store or <c>otherBeans</c>.
	 */
	@SuppressWarnings({ "java:S3776", "java:S6541" })
	public Object resolveValue(BeanStore beanStore, Object... otherBeans) {
		var pt = getParameterType();
		var bq = getResolvedQualifier();
		var ptu = pt.unwrap(Optional.class);

		// Handle collections and arrays
		Object collectionValue = null;
		if (ptu.isInjectCollectionType()) {
			// Extract element type from collection/array/map parameter type
			Class<?> elementType = null;

			// Handle arrays
			if (ptu.isArray()) {
				elementType = ptu.getComponentType().inner();
			} else {
				// Get the parameterized type
				Type parameterizedType = getParameterizedType();
				if (parameterizedType instanceof ParameterizedType pt2) {
					var rawType = pt2.getRawType();
					// If wrapped in Optional, unwrap it to get the nested type
					if (rawType instanceof Class<?> rawClass && rawClass == Optional.class) {
						var typeArgs = pt2.getActualTypeArguments();
						if (typeArgs.length > 0 && typeArgs[0] instanceof ParameterizedType typeArgs2) {
							// Optional<List<T>> -> List<T>
							parameterizedType = typeArgs2;
						} else {
							// Optional<SomeClass> - not a collection, elementType remains null
							parameterizedType = null;
						}
					}
				} else if (pt.innerType() instanceof ParameterizedType) {
					// If pt is already a parameterized type (e.g., List<TestService> after unwrapping Optional),
					// use pt.innerType() as the parameterizedType
					parameterizedType = pt.innerType();
				} else {
					parameterizedType = null;
				}

				// Handle List<T> or Set<T>
				var inner = opt(ptu.inner()).orElse(Object.class);
				if (eq(inner, List.class) || eq(inner, Set.class)) {
					if (parameterizedType instanceof ParameterizedType pt2) {
						var typeArgs = pt2.getActualTypeArguments();
						if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> elementClass) {
							elementType = elementClass;
						}
					}
				} else if (eq(inner, Map.class)) {
					// Handle Map<String,T> - extract value type (second type argument)
					if (parameterizedType instanceof ParameterizedType pt2) {
						var typeArgs = pt2.getActualTypeArguments();
						// Verify key type is String and get value type
						if (typeArgs.length >= 2 && typeArgs[0] == String.class && typeArgs[1] instanceof Class<?> valueClass) {
							elementType = valueClass;
						}
					}
				}
			}

			collectionValue = ReflectionUtils.resolveCollectionValue(elementType, beanStore, ptu);
		}
		if (nn(collectionValue))
			return pt.is(Optional.class) ? Optional.of(collectionValue) : collectionValue;

		// Handle single bean
		var ptc = ptu.inner();

		Optional<Object> r;

		if (nn(bq)) {
			r = beanStore.getBean(ptc, bq);
		} else {
			r = beanStore.getBean(ptc);
			if (r.isEmpty()) {
				for (var r2 : otherBeans)
					if (canAccept(r2)) {
						r = opt(r2);
						break;
					}
			}
		}

		if (pt.is(Optional.class))
			return r;
		if (r.isPresent())
			return r.get();

		throw exex("Could not resolve value for parameter {0}", this);
	}

	/**
	 * Resolves the parameter value from the bean store (constructor version with enclosingInstance).
	 *
	 * <p>
	 * This method is used for constructor parameters.  If the first parameter type matches the <c>enclosingInstance</c>
	 * object type, it uses the <c>enclosingInstance</c> object (for non-static inner class constructors).
	 * Otherwise, it delegates to {@link #resolveValue(BeanStore, Object...)}.
	 *
	 * @param beanStore The bean store to resolve beans from.
	 * @param enclosingInstance The outer class instance for non-static inner class constructors.
	 * 	If the first parameter type matches this object's type, it is used as the first parameter value.
	 * 	Can be <jk>null</jk> for regular classes or static inner classes.
	 * @param otherBeans Optional additional bean instances to use if not found in the bean store.
	 * 	These are checked after the bean store but before throwing an exception.
	 * @return The resolved parameter value.
	 * @throws ExecutableException If a required parameter (non-Optional, non-collection) cannot be resolved
	 * 	from the bean store or <c>otherBeans</c>.
	 */
	public Object resolveValue(BeanStore beanStore, Object enclosingInstance, Object... otherBeans) {
		var pt = getParameterType();
		if (getIndex() == 0 && nn(enclosingInstance) && pt.isInstance(enclosingInstance))
			return enclosingInstance;
		return resolveValue(beanStore, otherBeans);
	}
}