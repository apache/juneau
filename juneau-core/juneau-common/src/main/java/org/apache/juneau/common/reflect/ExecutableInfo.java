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

import static org.apache.juneau.common.reflect.ClassArrayFormat.*;
import static org.apache.juneau.common.reflect.ClassNameFormat.*;
import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static java.util.stream.Collectors.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.utils.*;

/**
 * Abstract base class containing common functionality for {@link ConstructorInfo} and {@link MethodInfo}.
 *
 * <p>
 * This class provides shared functionality for both constructors and methods, which are both types of
 * {@link Executable} in Java. It extends {@link AccessibleInfo} to provide {@link AccessibleObject}
 * functionality for accessing private methods and constructors.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Parameter introspection - access method/constructor parameters
 * 	<li>Exception handling - get declared exceptions
 * 	<li>Annotation support - get annotations declared on the executable
 * 	<li>Name formatting - get short and fully qualified names
 * 	<li>Parameter matching - match parameters by type (strict and lenient)
 * 	<li>Accessibility control - make private executables accessible
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Working with both methods and constructors in a unified way
 * 	<li>Finding methods/constructors that match specific parameter types
 * 	<li>Introspecting parameter and exception information
 * 	<li>Building frameworks that need to analyze executable signatures
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Get ExecutableInfo (could be MethodInfo or ConstructorInfo)</jc>
 * 	MethodInfo <jv>mi</jv> = ...;
 * 	ExecutableInfo <jv>ei</jv> = <jv>mi</jv>;  <jc>// MethodInfo extends ExecutableInfo</jc>
 *
 * 	<jc>// Get parameters</jc>
 * 	List&lt;ParameterInfo&gt; <jv>params</jv> = <jv>ei</jv>.getParameters();
 *
 * 	<jc>// Get exceptions</jc>
 * 	List&lt;ClassInfo&gt; <jv>exceptions</jv> = <jv>ei</jv>.getExceptions();
 *
 * 	<jc>// Check parameter matching</jc>
 * 	<jk>boolean</jk> <jv>matches</jv> = <jv>ei</jv>.parameterMatches(String.<jk>class</jk>, Integer.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MethodInfo} - Method introspection
 * 	<li class='jc'>{@link ConstructorInfo} - Constructor introspection
 * 	<li class='jc'>{@link ParameterInfo} - Parameter introspection
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonReflect">juneau-common-reflect</a>
 * </ul>
 */
public abstract class ExecutableInfo extends AccessibleInfo {

	protected final ClassInfo declaringClass;
	private final Executable inner;
	private final boolean isConstructor;

	private final Supplier<List<ParameterInfo>> parameters;  // All parameters of this executable.
	private final Supplier<List<ClassInfo>> exceptions;  // All exceptions declared by this executable.
	private final Supplier<List<AnnotationInfo<Annotation>>> declaredAnnotations;  // All annotations declared directly on this executable.
	private final Supplier<String> shortName;  // Short name (method/constructor name with parameters).
	private final Supplier<String> fullName;  // Fully qualified name (declaring-class.method-name with parameters).

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new ExecutableInfo wrapper for the specified executable (method or constructor).
	 * This constructor is protected and should not be called directly. Use the constructors
	 * of {@link MethodInfo} or {@link ConstructorInfo} instead.
	 *
	 * @param declaringClass The ClassInfo for the class that declares this method or constructor.
	 * @param inner The constructor or method that this info represents. Must not be <jk>null</jk>.
	 */
	protected ExecutableInfo(ClassInfo declaringClass, Executable inner) {
		super(inner, assertArgNotNull("inner", inner).getModifiers());
		this.declaringClass = declaringClass;
		this.inner = inner;
		this.isConstructor = inner instanceof Constructor;
		this.parameters = memoize(this::findParameters);
		this.exceptions = memoize(() -> stream(inner.getExceptionTypes()).map(ClassInfo::of).toList());
		this.declaredAnnotations = memoize(() -> stream(inner.getDeclaredAnnotations()).flatMap(a -> AnnotationUtils.streamRepeated(a)).map(a -> ai((Annotatable)this, a)).toList());
		this.shortName = memoize(() -> f("{0}({1})", getSimpleName(), getParameters().stream().map(p -> p.getParameterType().getNameSimple()).collect(joining(","))));
		this.fullName = memoize(this::findFullName);
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return This object.
	 */
	public ExecutableInfo accessible() {
		setAccessible();
		return this;
	}

	/**
	 * Returns how well this method matches the specified arg types using lenient matching.
	 *
	 * <p>
	 * Lenient matching allows arguments to be matched to parameters based on type compatibility,
	 * where arguments can be in any order.
	 *
	 * <p>
	 * The number returned is the number of method arguments that match the passed in arg types.
	 * <br>Returns <c>-1</c> if the method cannot take in one or more of the specified arguments.
	 *
	 * @param argTypes The arg types to check against.
	 * @return How many parameters match or <c>-1</c> if method cannot handle one or more of the arguments.
	 */
	public final int parameterMatchesLenientCount(Class<?>...argTypes) {
		int matches = 0;
		outer: for (var param : getParameters()) {
			for (var a : argTypes) {
				if (param.getParameterType().isParentOfLenient(a)) {
					matches++;
					continue outer;
				}
			}
			return -1;
		}
		return matches;
	}

	/**
	 * Returns how well this method matches the specified arg types using lenient matching.
	 *
	 * <p>
	 * Lenient matching allows arguments to be matched to parameters based on type compatibility,
	 * where arguments can be in any order.
	 *
	 * <p>
	 * The number returned is the number of method arguments that match the passed in arg types.
	 * <br>Returns <c>-1</c> if the method cannot take in one or more of the specified arguments.
	 *
	 * @param argTypes The arg types to check against.
	 * @return How many parameters match or <c>-1</c> if method cannot handle one or more of the arguments.
	 */
	public final int parameterMatchesLenientCount(ClassInfo...argTypes) {
		int matches = 0;
		outer: for (var param : getParameters()) {
			for (var a : argTypes) {
				if (param.getParameterType().isParentOfLenient(a)) {
					matches++;
					continue outer;
				}
			}
			return -1;
		}
		return matches;
	}

	/**
	 * Returns how well this method matches the specified arg types using lenient matching.
	 *
	 * <p>
	 * Lenient matching allows arguments to be matched to parameters based on type compatibility,
	 * where arguments can be in any order.
	 *
	 * <p>
	 * The number returned is the number of method arguments that match the passed in arg types.
	 * <br>Returns <c>-1</c> if the method cannot take in one or more of the specified arguments.
	 *
	 * @param argTypes The arg types to check against.
	 * @return How many parameters match or <c>-1</c> if method cannot handle one or more of the arguments.
	 */
	public final int parameterMatchesLenientCount(Object...argTypes) {
		int matches = 0;
		outer: for (var param : getParameters()) {
			for (var a : argTypes) {
				if (param.getParameterType().canAcceptArg(a)) {
					matches++;
					continue outer;
				}
			}
			return -1;
		}
		return matches;
	}

	/**
	 * Returns metadata about the class that declared this method or constructor.
	 *
	 * @return Metadata about the class that declared this method or constructor.
	 */
	public final ClassInfo getDeclaringClass() { return declaringClass; }

	/**
	 * Returns the exception types on this executable.
	 *
	 * @return The exception types on this executable.
	 */
	public final List<ClassInfo> getExceptionTypes() { return exceptions.get(); }

	/**
	 * Returns the declared annotations on this executable.
	 *
	 * <p>
	 * <b>Note on Repeatable Annotations:</b>
	 * Repeatable annotations (those marked with {@link java.lang.annotation.Repeatable @Repeatable}) are automatically
	 * expanded into their individual annotation instances. For example, if a method has multiple {@code @Bean} annotations,
	 * this method returns each {@code @Bean} annotation separately, rather than the container annotation.
	 *
	 * @return
	 * 	The declared annotations on this executable as {@link AnnotationInfo} objects.
	 * 	<br>Repeatable annotations are expanded into individual instances.
	 */
	public final List<AnnotationInfo<Annotation>> getDeclaredAnnotations() { return declaredAnnotations.get(); }

	/**
	 * Returns the declared annotations of the specified type on this executable.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type.
	 * @return A stream of matching annotations.
	 */
	@SuppressWarnings("unchecked")
	public final <A extends Annotation> Stream<AnnotationInfo<A>> getDeclaredAnnotations(Class<A> type) {
		assertArgNotNull("type", type);
		// @formatter:off
		return declaredAnnotations.get().stream()
			.filter(x -> type.isInstance(x.inner()))
			.map(x -> (AnnotationInfo<A>)x);
		// @formatter:on
	}

	/**
	 * Returns <jk>true</jk> if this executable has the specified annotation.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type.
	 * @return <jk>true</jk> if this executable has the specified annotation.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return getDeclaredAnnotations(type).findFirst().isPresent();
	}

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
	public final String getFullName() { return fullName.get(); }

	/**
	 * Returns parameter information at the specified index.
	 *
	 * @param index The parameter index.
	 * @return The parameter information, never <jk>null</jk>.
	 */
	public final ParameterInfo getParameter(int index) {
		checkIndex(index);
		return getParameters().get(index);
	}

	/**
	 * Returns the number of parameters in this executable.
	 *
	 * <p>
	 * Same as calling {@link Executable#getParameterCount()}.
	 *
	 * @return The number of parameters in this executable.
	 */
	public final int getParameterCount() { return inner.getParameterCount(); }

	/**
	 * Returns the parameters defined on this executable.
	 *
	 * <p>
	 * Same as calling {@link Executable#getParameters()} but wraps the results
	 *
	 * @return An array of parameter information, never <jk>null</jk>.
	 */
	public final List<ParameterInfo> getParameters() { return parameters.get(); }

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
	public final String getShortName() { return shortName.get(); }

	/**
	 * Returns the simple name of the underlying method.
	 *
	 * @return The simple name of the underlying method;
	 */
	public final String getSimpleName() { return isConstructor ? cns(inner.getDeclaringClass()) : inner.getName(); }

	/**
	 * Returns <jk>true</jk> if this executable can accept the specified arguments in the specified order.
	 *
	 * <p>
	 * This method checks if the provided arguments are compatible with the executable's parameter types
	 * in exact order, using {@link Class#isInstance(Object)} for type checking.
	 *
	 * <p>
	 * <strong>Important:</strong> For non-static inner class constructors, the first parameter is the
	 * implicit outer class instance (e.g., {@code Outer.this}). This method checks against the
	 * <em>actual</em> parameters including this implicit parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Regular method</jc>
	 * 	<jk>public void</jk> foo(String <jv>s</jv>, Integer <jv>i</jv>);
	 * 	<jv>methodInfo</jv>.canAccept(<js>"hello"</js>, 42);  <jc>// true</jc>
	 *
	 * 	<jc>// Non-static inner class constructor</jc>
	 * 	<jk>class</jk> Outer {
	 * 		<jk>class</jk> Inner {
	 * 			Inner(String <jv>s</jv>) {}
	 * 		}
	 * 	}
	 * 	<jc>// Constructor actually has signature: Inner(Outer this$0, String s)</jc>
	 * 	Outer <jv>outer</jv> = <jk>new</jk> Outer();
	 * 	<jv>constructorInfo</jv>.canAccept(<js>"hello"</js>);  <jc>// false - missing outer instance</jc>
	 * 	<jv>constructorInfo</jv>.canAccept(<jv>outer</jv>, <js>"hello"</js>);  <jc>// true</jc>
	 * </p>
	 *
	 * @param args The arguments to check.
	 * @return <jk>true</jk> if this executable can accept the specified arguments in the specified order.
	 */
	public final boolean canAccept(Object...args) {
		Class<?>[] pt = inner.getParameterTypes();
		if (pt.length != args.length)
			return false;
		for (var i = 0; i < pt.length; i++)
			if (! pt[i].isInstance(args[i]))
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this method has at most only these arguments using lenient matching.
	 *
	 * <p>
	 * Lenient matching allows arguments to be matched to parameters based on type compatibility,
	 * where arguments can be in any order.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has at most only these arguments in any order.
	 */
	public final boolean hasParameterTypesLenient(Class<?>...args) {
		return parameterMatchesLenientCount(args) != -1;
	}

	/**
	 * Returns <jk>true</jk> if this method has at most only these arguments using lenient matching.
	 *
	 * <p>
	 * Lenient matching allows arguments to be matched to parameters based on type compatibility,
	 * where arguments can be in any order.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has at most only these arguments in any order.
	 */
	public final boolean hasParameterTypesLenient(ClassInfo...args) {
		return parameterMatchesLenientCount(args) != -1;
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified argument parent classes.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has this arguments in the exact order.
	 */
	public final boolean hasParameterTypeParents(Class<?>...args) {
		var params = getParameters();
		return params.size() == args.length && params.stream().allMatch(p -> stream(args).anyMatch(a -> p.getParameterType().isParentOfLenient(a)));
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified argument parent classes.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has this arguments in the exact order.
	 */
	public final boolean hasParameterTypeParents(ClassInfo...args) {
		var params = getParameters();
		return params.size() == args.length && params.stream().allMatch(p -> stream(args).anyMatch(a -> p.getParameterType().isParentOfLenient(a)));
	}

	/**
	 * Returns <jk>true</jk> if this method has a name in the specified set.
	 *
	 * @param names The names to test for.
	 * @return <jk>true</jk> if this method has one of the names.
	 */
	public final boolean hasAnyName(Collection<String> names) {
		return names.contains(getSimpleName());
	}

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
	public final boolean hasAnyName(String...names) {
		return stream(names).anyMatch(n -> eq(n, getSimpleName()));
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
	public final boolean hasNumParameters(int number) {
		return getParameterCount() == number;
	}

	/**
	 * Returns <jk>true</jk> if this executable has at least one parameter.
	 *
	 * <p>
	 * Same as calling {@link Executable#getParameterCount()} and comparing with zero.
	 *
	 * @return <jk>true</jk> if this executable has at least one parameter.
	 */
	public final boolean hasParameters() {
		return getParameterCount() != 0;
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified arguments.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has this arguments in the exact order.
	 */
	public final boolean hasParameterTypes(Class<?>...args) {
		var params = getParameters();
		return params.size() == args.length && IntStream.range(0, args.length).allMatch(i -> params.get(i).getParameterType().is(args[i]));
	}

	/**
	 * Returns <jk>true</jk> if this method has the specified arguments.
	 *
	 * @param args The arguments to test for.
	 * @return <jk>true</jk> if this method has this arguments in the exact order.
	 */
	public final boolean hasParameterTypes(ClassInfo...args) {
		var params = getParameters();
		return params.size() == args.length && IntStream.range(0, args.length).allMatch(i -> params.get(i).getParameterType().is(args[i]));
	}

	/**
	 * Returns <jk>true</jk> if this executable has matching parameter types with the provided parameter list.
	 *
	 * @param params The parameters to match against.
	 * @return <jk>true</jk> if this executable has matching parameter types.
	 */
	public final boolean hasMatchingParameters(List<ParameterInfo> params) {
		var myParams = getParameters();
		return myParams.size() == params.size() && IntStream.range(0, params.size()).allMatch(i -> myParams.get(i).getParameterType().is(params.get(i).getParameterType()));
	}

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this method.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this method.
	 */
	@Override
	public boolean is(ElementFlag flag) {
		return switch (flag) {
			case CONSTRUCTOR -> isConstructor();
			case NOT_CONSTRUCTOR -> ! isConstructor();
			case DEPRECATED -> isDeprecated();
			case NOT_DEPRECATED -> isNotDeprecated();
			case HAS_PARAMS -> hasParameters();
			case HAS_NO_PARAMS -> getParameterCount() == 0;
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
	 * Returns <jk>true</jk> if this executable represents a {@link Constructor}.
	 *
	 * @return
	 * 	<jk>true</jk> if this executable represents a {@link Constructor} and can be cast to {@link ConstructorInfo}.
	 * 	<jk>false</jk> if this executable represents a {@link Method} and can be cast to {@link MethodInfo}.
	 */
	public final boolean isConstructor() { return isConstructor; }

	/**
	 * Returns <jk>true</jk> if this method has the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this method has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public final boolean isDeprecated() {
		return inner.isAnnotationPresent(Deprecated.class);

	}

	/**
	 * Returns <jk>true</jk> if this method doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this method doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 */
	public final boolean isNotDeprecated() {
		return ! inner.isAnnotationPresent(Deprecated.class);

	}

	/**
	 * Identifies if the specified visibility matches this method.
	 *
	 * @param v The visibility to validate against.
	 * @return <jk>true</jk> if this visibility matches the modifier attribute of this method.
	 */
	public final boolean isVisible(Visibility v) {
		return v.isVisible(inner);
	}

	/**
	 * Returns <jk>true</jk> if this executable is a synthetic construct as defined by the Java Language Specification.
	 *
	 * <p>
	 * Same as calling {@link Executable#isSynthetic()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if method is compiler-generated</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"access$000"</js>);
	 * 	<jk>boolean</jk> <jv>isSynthetic</jv> = <jv>mi</jv>.isSynthetic();
	 * </p>
	 *
	 * @return <jk>true</jk> if this executable is a synthetic construct.
	 * @see Executable#isSynthetic()
	 */
	public final boolean isSynthetic() { return inner.isSynthetic(); }

	/**
	 * Returns <jk>true</jk> if this executable was declared to take a variable number of arguments.
	 *
	 * <p>
	 * Same as calling {@link Executable#isVarArgs()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if method accepts varargs</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"myMethod"</js>, String[].<jk>class</jk>);
	 * 	<jk>boolean</jk> <jv>isVarArgs</jv> = <jv>mi</jv>.isVarArgs();
	 * </p>
	 *
	 * @return <jk>true</jk> if this executable was declared to take a variable number of arguments.
	 * @see Executable#isVarArgs()
	 */
	public final boolean isVarArgs() { return inner.isVarArgs(); }

	/**
	 * Returns an array of {@link TypeVariable} objects that represent the type variables declared by the generic declaration.
	 *
	 * <p>
	 * Returns an empty array if the generic declaration declares no type variables.
	 *
	 * <p>
	 * Same as calling {@link Executable#getTypeParameters()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get type parameters from method: &lt;T extends Number&gt; void myMethod(T value)</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"myMethod"</js>, Number.<jk>class</jk>);
	 * 	TypeVariable&lt;?&gt;[] <jv>typeParams</jv> = <jv>mi</jv>.getTypeParameters();
	 * 	<jc>// typeParams[0].getName() returns "T"</jc>
	 * </p>
	 *
	 * @return An array of {@link TypeVariable} objects, or an empty array if none.
	 * @see Executable#getTypeParameters()
	 */
	public final TypeVariable<?>[] getTypeParameters() { return inner.getTypeParameters(); }

	/**
	 * Returns a string describing this executable, including type parameters.
	 *
	 * <p>
	 * The string includes the method/constructor name, parameter types (with generic information), and return type (for methods).
	 *
	 * <p>
	 * Same as calling {@link Executable#toGenericString()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get generic string for: public &lt;T&gt; List&lt;T&gt; myMethod(T value) throws IOException</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"myMethod"</js>, Object.<jk>class</jk>);
	 * 	String <jv>str</jv> = <jv>mi</jv>.toGenericString();
	 * 	<jc>// Returns: "public &lt;T&gt; java.util.List&lt;T&gt; com.example.MyClass.myMethod(T) throws java.io.IOException"</jc>
	 * </p>
	 *
	 * @return A string describing this executable.
	 * @see Executable#toGenericString()
	 */
	public final String toGenericString() {
		return inner.toGenericString();
	}

	/**
	 * Returns an {@link AnnotatedType} object that represents the use of a type to specify the receiver type of the method/constructor.
	 *
	 * <p>
	 * Returns <jk>null</jk> if this executable object represents a top-level type or static member.
	 *
	 * <p>
	 * Same as calling {@link Executable#getAnnotatedReceiverType()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get annotated receiver type from method: void myMethod(@MyAnnotation MyClass this)</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"myMethod"</js>);
	 * 	AnnotatedType <jv>receiverType</jv> = <jv>mi</jv>.getAnnotatedReceiverType();
	 * </p>
	 *
	 * @return An {@link AnnotatedType} object representing the receiver type, or <jk>null</jk> if not applicable.
	 * @see Executable#getAnnotatedReceiverType()
	 */
	public final AnnotatedType getAnnotatedReceiverType() { return inner.getAnnotatedReceiverType(); }

	/**
	 * Returns an array of {@link AnnotatedType} objects that represent the use of types to specify formal parameter types.
	 *
	 * <p>
	 * The order of the objects corresponds to the order of the formal parameter types in the executable declaration.
	 *
	 * <p>
	 * Same as calling {@link Executable#getAnnotatedParameterTypes()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get annotated parameter types from method: void myMethod(@NotNull String s, @Range(min=0) int i)</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"myMethod"</js>, String.<jk>class</jk>, <jk>int</jk>.<jk>class</jk>);
	 * 	AnnotatedType[] <jv>paramTypes</jv> = <jv>mi</jv>.getAnnotatedParameterTypes();
	 * </p>
	 *
	 * @return An array of {@link AnnotatedType} objects, or an empty array if the executable has no parameters.
	 * @see Executable#getAnnotatedParameterTypes()
	 */
	public final AnnotatedType[] getAnnotatedParameterTypes() { return inner.getAnnotatedParameterTypes(); }

	/**
	 * Returns an array of {@link AnnotatedType} objects that represent the use of types to specify the declared exceptions.
	 *
	 * <p>
	 * The order of the objects corresponds to the order of the exception types in the executable declaration.
	 *
	 * <p>
	 * Same as calling {@link Executable#getAnnotatedExceptionTypes()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get annotated exception types from method: void myMethod() throws @NotNull IOException</jc>
	 * 	MethodInfo <jv>mi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getMethod(<js>"myMethod"</js>);
	 * 	AnnotatedType[] <jv>exTypes</jv> = <jv>mi</jv>.getAnnotatedExceptionTypes();
	 * </p>
	 *
	 * @return An array of {@link AnnotatedType} objects, or an empty array if the executable declares no exceptions.
	 * @see Executable#getAnnotatedExceptionTypes()
	 */
	public final AnnotatedType[] getAnnotatedExceptionTypes() { return inner.getAnnotatedExceptionTypes(); }

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return <jk>true</jk> if call was successful.
	 */
	@Override
	public final boolean setAccessible() {
		try {
			if (nn(inner))
				inner.setAccessible(true);
			return true;
		} catch (@SuppressWarnings("unused") SecurityException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return getShortName();
	}

	private void checkIndex(int index) {
		int pc = getParameterCount();
		if (pc == 0)
			throw new IndexOutOfBoundsException(mformat("Invalid index ''{0}''.  No parameters.", index));
		if (index < 0 || index >= pc)
			throw new IndexOutOfBoundsException(mformat("Invalid index ''{0}''.  Parameter count: {1}", index, pc));
	}

	private List<ParameterInfo> findParameters() {
		var rp = inner.getParameters();
		var ptc = inner.getParameterTypes();
		// Note that due to a bug involving Enum constructors, getGenericParameterTypes() may
		// always return an empty array.  This appears to be fixed in Java 8 b75.
		var ptt = inner.getGenericParameterTypes();
		Type[] genericTypes;
		if (ptt.length != ptc.length) {
			// Bug in javac: generic type array excludes enclosing instance parameter
			// for inner classes with at least one generic constructor parameter.
			if (ptt.length + 1 == ptc.length) {
				var ptt2 = new Type[ptc.length];
				ptt2[0] = ptc[0];
				for (var i = 0; i < ptt.length; i++)
					ptt2[i + 1] = ptt[i];
				genericTypes = ptt2;
			} else {
				genericTypes = ptc;
			}
		} else {
			genericTypes = ptt;
		}
		return IntStream.range(0, rp.length).mapToObj(i -> new ParameterInfo(this, rp[i], i, ClassInfo.of(ptc[i], genericTypes[i]))).toList();
	}

	private String findFullName() {
		var sb = new StringBuilder(128);
		var dc = declaringClass;
		var pi = dc.getPackage();
		if (nn(pi))
			sb.append(pi.getName()).append('.');
		dc.appendNameFormatted(sb, SHORT, true, '$', BRACKETS);
		if (! isConstructor)
			sb.append('.').append(getSimpleName());
		sb.append('(');
		sb.append(getParameters().stream().map(p -> p.getParameterType().getNameFormatted(FULL, true, '$', BRACKETS)).collect(joining(",")));
		sb.append(')');
		return sb.toString();
	}
}