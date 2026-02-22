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

import static java.util.stream.Collectors.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.utils.*;

/**
 * Lightweight utility class for introspecting information about a Java constructor.
 *
 * <p>
 * This class provides a convenient wrapper around {@link Constructor} that extends the standard Java reflection
 * API with additional functionality for constructor introspection, annotation handling, and instance creation.
 * It extends {@link ExecutableInfo} to provide common functionality shared with methods.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Constructor introspection - access constructor metadata, parameters, exceptions
 * 	<li>Annotation support - get annotations declared on the constructor
 * 	<li>Instance creation - create new instances with type safety
 * 	<li>Accessibility control - make private constructors accessible
 * 	<li>Thread-safe - instances are immutable and safe for concurrent access
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Introspecting constructor metadata for code generation or analysis
 * 	<li>Creating instances of classes dynamically
 * 	<li>Finding annotations on constructors
 * 	<li>Working with constructor parameters and exceptions
 * 	<li>Building frameworks that need to instantiate objects
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Get ConstructorInfo from a class</jc>
 * 	ClassInfo <jv>ci</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
 * 	ConstructorInfo <jv>ctor</jv> = <jv>ci</jv>.getConstructor(String.<jk>class</jk>);
 *
 * 	<jc>// Get annotations</jc>
 * 	List&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>annotations</jv> =
 * 		<jv>ctor</jv>.getAnnotations(MyAnnotation.<jk>class</jk>).toList();
 *
 * 	<jc>// Create instance</jc>
 * 	<jv>ctor</jv>.accessible();  <jc>// Make accessible if private</jc>
 * 	MyClass <jv>obj</jv> = <jv>ctor</jv>.invoke(<js>"arg"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ClassInfo} - Class introspection
 * 	<li class='jc'>{@link MethodInfo} - Method introspection
 * 	<li class='jc'>{@link FieldInfo} - Field introspection
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsReflection">Reflection Package</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class ConstructorInfo extends ExecutableInfo implements Comparable<ConstructorInfo>, Annotatable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_declaringClass = "declaringClass";
	private static final String ARG_inner = "inner";

	/**
	 * Creates a ConstructorInfo wrapper for the specified constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ClassInfo <jv>ci</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
	 * 	Constructor&lt;?&gt; <jv>c</jv> = MyClass.<jk>class</jk>.getConstructor(String.<jk>class</jk>);
	 * 	ConstructorInfo <jv>ci2</jv> = ConstructorInfo.<jsm>of</jsm>(<jv>ci</jv>, <jv>c</jv>);
	 * </p>
	 *
	 * @param declaringClass The ClassInfo for the class that declares this constructor. Must not be <jk>null</jk>.
	 * @param inner The constructor being wrapped. Must not be <jk>null</jk>.
	 * @return A new ConstructorInfo object wrapping the constructor.
	 */
	public static ConstructorInfo of(ClassInfo declaringClass, Constructor<?> inner) {
		assertArgNotNull(ARG_declaringClass, declaringClass);
		return declaringClass.getConstructor(inner);
	}

	/**
	 * Creates a ConstructorInfo wrapper for the specified constructor.
	 *
	 * <p>
	 * This convenience method automatically determines the declaring class from the constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Constructor&lt;?&gt; <jv>c</jv> = MyClass.<jk>class</jk>.getConstructor(String.<jk>class</jk>);
	 * 	ConstructorInfo <jv>ci</jv> = ConstructorInfo.<jsm>of</jsm>(<jv>c</jv>);
	 * </p>
	 *
	 * @param inner The constructor being wrapped. Must not be <jk>null</jk>.
	 * @return A new ConstructorInfo object wrapping the constructor.
	 */
	public static ConstructorInfo of(Constructor<?> inner) {
		assertArgNotNull(ARG_inner, inner);
		return ClassInfo.of(inner.getDeclaringClass()).getConstructor(inner);
	}

	private final Constructor<?> inner;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new ConstructorInfo wrapper for the specified constructor. This constructor is protected
	 * and should not be called directly. Use the static factory methods {@link #of(Constructor)} or
	 * obtain ConstructorInfo instances from {@link ClassInfo#getConstructor(Constructor)}.
	 *
	 * @param declaringClass The ClassInfo for the class that declares this constructor.
	 * @param inner The constructor being wrapped.
	 */
	protected ConstructorInfo(ClassInfo declaringClass, Constructor<?> inner) {
		super(declaringClass, inner);
		this.inner = inner;
	}

	@Override /* Overridden from ExecutableInfo */
	public ConstructorInfo accessible() {
		super.accessible();
		return this;
	}

	@Override
	public int compareTo(ConstructorInfo o) {
		int i = cmp(getNameSimple(), o.getNameSimple());
		if (i == 0) {
			i = getParameterCount() - o.getParameterCount();
			if (i == 0) {
				var params = getParameters();
				var oParams = o.getParameters();
				for (var j = 0; j < params.size() && i == 0; j++) {
					i = cmp(params.get(j).getParameterType().getName(), oParams.get(j).getParameterType().getName());
				}
			}
		}
		return i;
	}

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() { return AnnotatableType.CONSTRUCTOR_TYPE; }

	@Override /* Annotatable */
	public String getLabel() { return getDeclaringClass().getNameSimple() + "." + getNameShort(); }

	/**
	 * Returns the wrapped constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ConstructorInfo <jv>ci</jv> = ...;
	 * 	Constructor&lt;MyClass&gt; <jv>ctor</jv> = <jv>ci</jv>.inner();
	 * </p>
	 *
	 * @param <T> The class type of the constructor.
	 * @return The wrapped constructor.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast to Constructor<T>
	})
	public <T> Constructor<T> inner() {
		return (Constructor<T>)inner;
	}

	/**
	 * Compares this ConstructorInfo with the specified object for equality.
	 *
	 * <p>
	 * Two ConstructorInfo objects are considered equal if they wrap the same underlying {@link Constructor} object.
	 * This delegates to the underlying {@link Constructor#equals(Object)} method.
	 *
	 * <p>
 * This method makes ConstructorInfo suitable for use as keys in hash-based collections such as {@link java.util.HashMap}
 * and {@link java.util.HashSet}.
	 *
	 * @param obj The object to compare with.
	 * @return <jk>true</jk> if the objects are equal, <jk>false</jk> otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ConstructorInfo other && eq(this, other, (x, y) -> eq(x.inner, y.inner));
	}

	/**
	 * Returns a hash code value for this ConstructorInfo.
	 *
	 * <p>
	 * This delegates to the underlying {@link Constructor#hashCode()} method.
	 *
	 * <p>
 * This method makes ConstructorInfo suitable for use as keys in hash-based collections such as {@link java.util.HashMap}
 * and {@link java.util.HashSet}.
	 *
	 * @return A hash code value for this ConstructorInfo.
	 */
	@Override
	public int hashCode() {
		return inner.hashCode();
	}

	/**
	 * Shortcut for calling the new-instance method on the underlying constructor.
	 *
	 * <p>
	 * If the number of formal parameters required by the underlying constructor is 0, the supplied <c>args</c> array may be of length 0 or <jk>null</jk>.
	 *
	 * @param <T> The constructor class type.
	 * @param args The arguments used for the constructor call.  Can be <jk>null</jk> or empty for constructors with no parameters.
	 * @return The object returned from the constructor.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast to T for instance creation
	})
	public <T> T newInstance(Object...args) throws ExecutableException {
		return safe(() -> {
			try {
				return (T)inner.newInstance(args);
			} catch (InvocationTargetException e) {
				throw exex(e.getTargetException());
			}
		}, e -> exex(e));  // HTT
	}

	/**
	 * Shortcut for calling the new-instance method on the underlying constructor using lenient argument matching.
	 *
	 * <p>
	 * Lenient matching allows arguments to be matched to parameters based on parameter types.
	 * <br>Arguments can be in any order.
	 * <br>Extra arguments are ignored.
	 * <br>Missing arguments are set to <jk>null</jk>.
	 *
	 * @param <T> The constructor class type.
	 * @param args The arguments used for the constructor call.
	 * @return The object returned from the constructor.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public <T> T newInstanceLenient(Object...args) throws ExecutableException {
		return newInstance(ClassUtils.getMatchingArgs(inner.getParameterTypes(), args));
	}

	/**
	 * Returns comma-delimited list of missing parameter types.
	 *
	 * <p>
	 * Analyzes the parameters of this constructor and checks if all required beans are available
	 * in the bean store or in the <c>otherBeans</c> parameter.
	 *
	 * <p>
	 * The following parameter types are considered optional and are not checked:
	 * <ul>
	 * 	<li>Parameters of type <c>Optional&lt;T&gt;</c>
	 * 	<li>Parameters of type <c>T[]</c>, <c>List&lt;T&gt;</c>, <c>Set&lt;T&gt;</c>, or <c>Map&lt;String,T&gt;</c>
	 * 	<li>The first parameter if it matches the <c>enclosingInstance</c> object type (for non-static inner classes)
	 * </ul>
	 *
	 * <p>
	 * If a parameter has a {@link org.apache.juneau.annotation.Named @Named} or {@code @Qualifier} annotation,
	 * the method checks for a bean with that specific name in the bean store.  Otherwise, it checks for an unnamed bean
	 * of the parameter type in the bean store, and if not found, checks the <c>otherBeans</c> parameter.
	 *
	 * @param beanStore The bean store to check for beans.
	 * @param enclosingInstance The outer class instance for non-static inner class constructors.
	 * 	If the first parameter type matches this object's type, it is used as the first parameter and not checked in the bean store.
	 * 	Can be <jk>null</jk> for regular classes or static inner classes.
	 * @param otherBeans Optional additional bean instances to check if not found in the bean store.
	 * 	These are checked after the bean store but before marking the parameter as missing.
	 * @return A comma-delimited, sorted list of missing parameter types (e.g., <js>"String,Integer"</js>),
	 * 	or <jk>null</jk> if all required parameters are available.
	 */
	public String getMissingParameterTypes(BeanStore beanStore, Object enclosingInstance, Object... otherBeans) {
		// @formatter:off
		return nullIfEmpty(
			getParameters()
				.stream()
				.map(x -> {
					var pt = x.getParameterType();
					if (x.getIndex() == 0 && nn(enclosingInstance) && pt.isInstance(enclosingInstance))
						return null;
					return x.getMissingType(beanStore, otherBeans);
				})
				.filter(Objects::nonNull)
				.sorted()
				.collect(joining(","))
		);
		// @formatter:on
	}

	/**
	 * Checks if all constructor parameters can be resolved.
	 *
	 * <p>
	 * This method performs the same checks as {@link #getMissingParameterTypes(BeanStore, Object, Object...)} but
	 * returns a boolean instead of a list of missing types.
	 *
	 * <p>
	 * The following parameter types are considered optional and are not checked:
	 * <ul>
	 * 	<li>Parameters of type <c>Optional&lt;T&gt;</c>
	 * 	<li>Parameters of type <c>T[]</c>, <c>List&lt;T&gt;</c>, <c>Set&lt;T&gt;</c>, or <c>Map&lt;String,T&gt;</c>
	 * 	<li>The first parameter if it matches the <c>enclosingInstance</c> object type (for non-static inner classes)
	 * </ul>
	 *
	 * <p>
	 * If a parameter has a {@link org.apache.juneau.annotation.Named @Named} or {@code @Qualifier} annotation,
	 * the method checks for a bean with that specific name in the bean store.  Otherwise, it checks for an unnamed bean
	 * of the parameter type in the bean store, and if not found, checks the <c>otherBeans</c> parameter.
	 *
	 * @param beanStore The bean store to check for beans.
	 * @param enclosingInstance The outer class instance for non-static inner class constructors.
	 * 	If the first parameter type matches this object's type, it is used as the first parameter and not checked in the bean store.
	 * 	Can be <jk>null</jk> for regular classes or static inner classes.
	 * @param otherBeans Optional additional bean instances to check if not found in the bean store.
	 * 	These are checked after the bean store but before marking the parameter as missing.
	 * @return <jk>true</jk> if all required parameters are available in the bean store or <c>otherBeans</c>, <jk>false</jk> otherwise.
	 */
	public boolean canResolveAllParameters(BeanStore beanStore, Object enclosingInstance, Object... otherBeans) {
		return getParameters().stream().map(x -> {
			var pt = x.getParameterType();
			if (x.getIndex() == 0 && nn(enclosingInstance) && pt.isInstance(enclosingInstance))
				return true;
			return x.canResolve(beanStore, otherBeans);
		}).allMatch(x -> x);
	}

	/**
	 * Resolves parameters from the bean store and invokes the constructor.
	 *
	 * <p>
	 * For each parameter in the constructor, this method:
	 * <ul>
	 * 	<li>If the first parameter type matches the <c>enclosingInstance</c> object type, uses the <c>enclosingInstance</c> object
	 * 		(for non-static inner class constructors).
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
	 * <p>
	 * The <c>enclosingInstance</c> parameter is used for non-static inner class constructors.  If the first parameter type
	 * matches this object's type, it is used as the first parameter value.  For regular constructors, this parameter
	 * is ignored if it doesn't match the first parameter type.
	 *
	 * @param <T> The return type of the constructor.
	 * @param beanStore The bean store to resolve parameters from.
	 * @param enclosingInstance The outer class instance for non-static inner classes (can be <jk>null</jk>).
	 * @param otherBeans Optional additional bean instances to use if not found in the bean store.
	 * @return The result of invoking the constructor.
	 * @throws ExecutableException If the constructor cannot be invoked or parameter resolution fails.
	 */
	public <T> T inject(BeanStore beanStore, Object enclosingInstance, Object... otherBeans) {
		var params = getParameters().stream().map(x -> x.resolveValue(beanStore, enclosingInstance, otherBeans)).toArray();
		return accessible().newInstance(params);
	}

	/**
	 * Resolves all constructor parameters from the bean store.
	 *
	 * <p>
	 * This is a package-private helper method used by tests.  For normal usage, use {@link #inject(BeanStore, Object, Object...)}.
	 *
	 * @param beanStore The bean store to resolve beans from.
	 * @param enclosingInstance The outer class instance for non-static inner class constructors.
	 * @param otherBeans Optional additional bean instances to use if not found in the bean store.
	 * @return An array of parameter values in the same order as the constructor parameters.
	 * @throws ExecutableException If a required parameter cannot be resolved.
	 */
	Object[] resolveParameters(BeanStore beanStore, Object enclosingInstance, Object... otherBeans) {
		return getParameters().stream().map(x -> x.resolveValue(beanStore, enclosingInstance, otherBeans)).toArray();
	}
}