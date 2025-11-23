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

import static org.apache.juneau.common.reflect.ReflectionUtils.*;
import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.annotation.*;

/**
 * Encapsulates information about an annotation instance and the element it's declared on.
 *
 * <p>
 * This class provides a convenient wrapper around Java annotations that allows you to:
 * <ul>
 * 	<li>Access annotation values in a type-safe manner
 * 	<li>Query annotation properties without reflection boilerplate
 * 	<li>Track where the annotation was found (class, method, field, etc.)
 * 	<li>Sort annotations by precedence using ranks
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Get annotation info from a class</jc>
 * 	ClassInfo <jv>ci</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
 * 	Optional&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>ai</jv> =
 * 		<jv>ci</jv>.getAnnotations(MyAnnotation.<jk>class</jk>).findFirst();
 *
 * 	<jc>// Access annotation values</jc>
 * 	<jv>ai</jv>.ifPresent(<jv>x</jv> -&gt; {
 * 		String <jv>value</jv> = <jv>x</jv>.getValue(String.<jk>class</jk>, <js>"value"</js>).orElse(<js>"default"</js>);
 * 		<jk>int</jk> <jv>priority</jv> = <jv>x</jv>.getInt(<js>"priority"</js>).orElse(0);
 * 	});
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ClassInfo}
 * 	<li class='jc'>{@link MethodInfo}
 * 	<li class='jc'>{@link FieldInfo}
 * 	<li class='jc'>{@link ConstructorInfo}
 * 	<li class='jc'>{@link ParameterInfo}
 * 	<li class='jc'>{@link PackageInfo}
 * </ul>
 *
 * @param <T> The annotation type.
 */
public class AnnotationInfo<T extends Annotation> {

	/**
	 * Creates a new annotation info object.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create annotation info for a class annotation</jc>
	 * 	ClassInfo <jv>ci</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
	 * 	MyAnnotation <jv>annotation</jv> = <jv>ci</jv>.inner().getAnnotation(MyAnnotation.<jk>class</jk>);
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = AnnotationInfo.<jsm>of</jsm>(<jv>ci</jv>, <jv>annotation</jv>);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param on The annotatable object where the annotation was found (class, method, field, constructor, parameter, or package).
	 * @param value The annotation instance. Must not be <jk>null</jk>.
	 * @return A new {@link AnnotationInfo} object wrapping the annotation.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(Annotatable on, A value) {
		return new AnnotationInfo<>(on, value);
	}

	private final Annotatable annotatable;
	final int rank;
	private T a;  // Effectively final

	private final Supplier<List<MethodInfo>> methods = memoize(() -> stream(a.annotationType().getMethods()).map(m -> MethodInfo.of(info(a.annotationType()), m)).toList());

	/**
	 * Constructor.
	 *
	 * @param on The annotatable object where the annotation was found.
	 * @param a The annotation instance.
	 */
	AnnotationInfo(Annotatable on, T a) {
		this.annotatable = on;  // TODO - Shouldn't allow null.
		this.a = assertArgNotNull("a", a);
		this.rank = findRank(a);
	}

	/**
	 * Returns the rank of this annotation for sorting by precedence.
	 *
	 * <p>
	 * The rank is determined by checking if the annotation has a {@code rank()} method that returns an {@code int}.
	 * If found, that value is used; otherwise the rank defaults to {@code 0}.
	 *
	 * <p>
	 * Higher rank values indicate higher precedence when multiple annotations of the same type are present.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Annotation with rank method</jc>
	 * 	<ja>@interface</ja> MyAnnotation {
	 * 		<jk>int</jk> rank() <jk>default</jk> 0;
	 * 	}
	 *
	 * 	<jc>// Get rank from annotation info</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	<jk>int</jk> <jv>rank</jv> = <jv>ai</jv>.getRank();  <jc>// Returns value from rank() method</jc>
	 * </p>
	 *
	 * @return The rank of this annotation, or {@code 0} if no rank method exists.
	 */
	public int getRank() { return rank; }

	/**
	 * Returns the simple class name of this annotation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	String <jv>name</jv> = <jv>ai</jv>.getName();  <jc>// Returns "MyAnnotation"</jc>
	 * </p>
	 *
	 * @return The simple class name of the annotation (e.g., {@code "Override"} for {@code @Override}).
	 */
	public String getName() { return scn(a.annotationType()); }

	/**
	 * Returns the value of a specific annotation method.
	 *
	 * <p>
	 * This method provides type-safe access to annotation field values without requiring
	 * explicit reflection calls or casting.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @interface MyAnnotation { String value(); int priority(); }</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 *
	 * 	<jc>// Get string value</jc>
	 * 	Optional&lt;String&gt; <jv>value</jv> = <jv>ai</jv>.getValue(String.<jk>class</jk>, <js>"value"</js>);
	 *
	 * 	<jc>// Get int value</jc>
	 * 	Optional&lt;Integer&gt; <jv>priority</jv> = <jv>ai</jv>.getValue(Integer.<jk>class</jk>, <js>"priority"</js>);
	 * </p>
	 *
	 * @param <V> The expected type of the annotation field value.
	 * @param type The expected class of the annotation field value.
	 * @param name The name of the annotation method (field).
	 * @return An {@link Optional} containing the value if found and type matches, empty otherwise.
	 */
	@SuppressWarnings("unchecked")
	public <V> Optional<V> getValue(Class<V> type, String name) {
		// @formatter:off
		return methods.get().stream()
			.filter(m -> eq(m.getName(), name) && eq(m.getReturnType().inner(), type))
			.map(m -> safe(() -> (V)m.invoke(a)))
			.findFirst();
		// @formatter:on
	}

	/**
	 * Casts this annotation info to a specific annotation type.
	 *
	 * <p>
	 * This is useful when you have an {@code AnnotationInfo<?>} and need to narrow it to a specific type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	AnnotationInfo&lt;?&gt; <jv>ai</jv> = ...;
	 *
	 * 	<jc>// Safe cast</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>myAi</jv> = <jv>ai</jv>.cast(MyAnnotation.<jk>class</jk>);
	 * 	<jk>if</jk> (<jv>myAi</jv> != <jk>null</jk>) {
	 * 		<jc>// Use strongly-typed annotation info</jc>
	 * 	}
	 * </p>
	 *
	 * @param <A> The annotation type to cast to.
	 * @param type The annotation type to cast to.
	 * @return This annotation info cast to the specified type, or <jk>null</jk> if the annotation is not of the specified type.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> AnnotationInfo<A> cast(Class<A> type) {
		return type.isInstance(a) ? (AnnotationInfo<A>)this : null;
	}

	/**
	 * Returns <jk>true</jk> if this annotation is itself annotated with the specified annotation.
	 *
	 * <p>
	 * This checks for meta-annotations on the annotation type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if @MyAnnotation is annotated with @Documented</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	<jk>boolean</jk> <jv>isDocumented</jv> = <jv>ai</jv>.hasAnnotation(Documented.<jk>class</jk>);
	 * </p>
	 *
	 * @param <A> The meta-annotation type.
	 * @param type The meta-annotation to test for.
	 * @return <jk>true</jk> if this annotation is annotated with the specified annotation.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return nn(this.a.annotationType().getAnnotation(type));
	}

	/**
	 * Returns the wrapped annotation instance.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	MyAnnotation <jv>annotation</jv> = <jv>ai</jv>.inner();
	 *
	 * 	<jc>// Access annotation methods directly</jc>
	 * 	String <jv>value</jv> = <jv>annotation</jv>.value();
	 * </p>
	 *
	 * @return The wrapped annotation instance.
	 */
	public T inner() {
		return a;
	}

	/**
	 * Returns <jk>true</jk> if this annotation is in the specified {@link AnnotationGroup}.
	 *
	 * <p>
	 * Annotation groups are used to logically group related annotations together.
	 * This checks if the annotation is annotated with {@link AnnotationGroup} and if
	 * the group value matches the specified type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Define an annotation group</jc>
	 * 	<ja>@interface</ja> MyGroup {}
	 *
	 * 	<jc>// Annotation in the group</jc>
	 * 	<ja>@AnnotationGroup</ja>(MyGroup.<jk>class</jk>)
	 * 	<ja>@interface</ja> MyAnnotation {}
	 *
	 * 	<jc>// Check if annotation is in group</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	<jk>boolean</jk> <jv>inGroup</jv> = <jv>ai</jv>.isInGroup(MyGroup.<jk>class</jk>);  <jc>// Returns true</jc>
	 * </p>
	 *
	 * @param <A> The group annotation type.
	 * @param group The group annotation class to test for.
	 * @return <jk>true</jk> if this annotation is in the specified group.
	 * @see AnnotationGroup
	 */
	public <A extends Annotation> boolean isInGroup(Class<A> group) {
		var x = a.annotationType().getAnnotation(AnnotationGroup.class);
		return (nn(x) && x.value().equals(group));
	}

	/**
	 * Returns <jk>true</jk> if this annotation is of the specified type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	AnnotationInfo&lt;?&gt; <jv>ai</jv> = ...;
	 *
	 * 	<jk>if</jk> (<jv>ai</jv>.isType(MyAnnotation.<jk>class</jk>)) {
	 * 		<jc>// Handle MyAnnotation specifically</jc>
	 * 	}
	 * </p>
	 *
	 * @param <A> The annotation type to test for.
	 * @param type The annotation type to test against.
	 * @return <jk>true</jk> if this annotation's type is exactly the specified type.
	 */
	public <A extends Annotation> boolean isType(Class<A> type) {
		return this.a.annotationType() == type;
	}

	/**
	 * Converts this annotation info to a map representation for debugging purposes.
	 *
	 * <p>
	 * The returned map contains:
	 * <ul>
	 * 	<li>The annotatable element's type and label (e.g., {@code "CLASS_TYPE" -> "com.example.MyClass"})
	 * 	<li>A nested map with the annotation's simple name as key and its non-default values
	 * </ul>
	 *
	 * <p>
	 * Only annotation values that differ from their default values are included.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	LinkedHashMap&lt;String,Object&gt; <jv>map</jv> = <jv>ai</jv>.toMap();
	 * 	<jc>// Returns: {"CLASS_TYPE": "MyClass", "@MyAnnotation": {"value": "foo", "priority": 5}}</jc>
	 * </p>
	 *
	 * @return A new map showing the attributes of this annotation info.
	 */
	public LinkedHashMap<String,Object> toMap() {
		var jm = new LinkedHashMap<String,Object>();
		jm.put(s(annotatable.getAnnotatableType()), annotatable.getLabel());
		var ja = new LinkedHashMap<String,Object>();
		var ca = info(a.annotationType());
		ca.getDeclaredMethods().stream().forEach(x -> {
			try {
				var v = x.invoke(a);
				var d = x.inner().getDefaultValue();
				if (ne(v, d)) {
					if (! (isArray(v) && length(v) == 0 && length(d) == 0))
						ja.put(x.getName(), v);
				}
			} catch (Exception e) {
				ja.put(x.getName(), lm(e));
			}
		});
		jm.put("@" + ca.getNameSimple(), ja);
		return jm;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation interface methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the annotation type of this annotation.
	 *
	 * <p>
	 * Same as calling {@link Annotation#annotationType()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	AnnotationInfo&lt;Deprecated&gt; <jv>ai</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getAnnotation(Deprecated.<jk>class</jk>);
	 * 	Class&lt;? <jk>extends</jk> Annotation&gt; <jv>type</jv> = <jv>ai</jv>.annotationType();  <jc>// Returns Deprecated.class</jc>
	 * </p>
	 *
	 * @return The annotation type of this annotation.
	 * @see Annotation#annotationType()
	 */
	public Class<? extends Annotation> annotationType() {
		return a.annotationType();
	}

	/**
	 * Returns the hash code of this annotation.
	 *
	 * <p>
	 * Same as calling {@link Annotation#hashCode()} on the wrapped annotation.
	 *
	 * <p>
	 * The hash code of an annotation is the sum of the hash codes of its members (including those with default values).
	 *
	 * @return The hash code of this annotation.
	 * @see Annotation#hashCode()
	 */
	@Override /* Overridden from Object */
	public int hashCode() {
		return a.hashCode();
	}

	/**
	 * Returns true if the specified object represents an annotation that is logically equivalent to this one.
	 *
	 * <p>
	 * Same as calling {@link Annotation#equals(Object)} on the wrapped annotation.
	 *
	 * <p>
	 * Two annotations are considered equal if:
	 * <ul>
	 * 	<li>They are of the same annotation type
	 * 	<li>All their corresponding member values are equal
	 * </ul>
	 *
	 * @param o The reference object with which to compare.
	 * @return <jk>true</jk> if the specified object is equal to this annotation.
	 * @see Annotation#equals(Object)
	 */
	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		if (o instanceof AnnotationInfo o2)
			return a.equals(o2.a);
		return a.equals(o);
	}

	/**
	 * Returns a simple string representation of this annotation showing the annotation type and location.
	 *
	 * <p>
	 * Format: {@code @AnnotationName(on=location)}
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li>{@code @Rest(on=MyClass)} - Annotation on a class
	 * 	<li>{@code @RestGet(on=MyClass.myMethod)} - Annotation on a method
	 * 	<li>{@code @Inject(on=MyClass.myField)} - Annotation on a field
	 * 	<li>{@code @PackageAnnotation(on=my.package)} - Annotation on a package
	 * </ul>
	 *
	 * @return A simple string representation of this annotation.
	 */
	public String toSimpleString() {
		return "@" + scn(a.annotationType()) + "(on=" + annotatable.getLabel() + ")";
	}

	/**
	 * Returns a string representation of this annotation.
	 *
	 * <p>
	 * Returns the map representation created by {@link #toMap()}.
	 *
	 * @return A string representation of this annotation.
	 */
	@Override /* Overridden from Object */
	public String toString() {
		return toMap().toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Private helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private static int findRank(Object a) {
		// @formatter:off
		return ClassInfo.of(a).getAllMethods().stream()
			.filter(m -> m.hasName("rank") && m.getParameterCount() == 0 && m.hasReturnType(int.class))
			.findFirst()
			.map(m -> safe(() -> (int)m.invoke(a)))
			.orElse(0);
		// @formatter:on
	}

	/**
	 * Returns <jk>true</jk> if this annotation has the specified simple name.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>boolean</jk> <jv>isName</jv> = <jv>annotationInfo</jv>.hasSimpleName(<js>"Name"</js>);
	 * </p>
	 *
	 * @param value The simple name to check.
	 * @return <jk>true</jk> if this annotation has the specified simple name.
	 */
	public boolean hasSimpleName(String value) {
		return eq(value, a.annotationType().getSimpleName());
	}

	/**
	 * Returns <jk>true</jk> if this annotation has the specified fully-qualified name.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>boolean</jk> <jv>isName</jv> = <jv>annotationInfo</jv>.hasName(<js>"org.apache.juneau.annotation.Name"</js>);
	 * </p>
	 *
	 * @param value The fully-qualified name to check.
	 * @return <jk>true</jk> if this annotation has the specified fully-qualified name.
	 */
	public boolean hasName(String value) {
		return eq(value, a.annotationType().getName());
	}

	/**
	 * Returns the method with the specified name on this annotation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	Optional&lt;MethodInfo&gt; <jv>method</jv> = <jv>ai</jv>.getMethod(<js>"value"</js>);
	 * 	<jv>method</jv>.ifPresent(<jv>m</jv> -&gt; System.<jsf>out</jsf>.println(<jv>m</jv>.getReturnType()));
	 * </p>
	 *
	 * @param methodName The method name to look for.
	 * @return An {@link Optional} containing the method info, or empty if method not found.
	 */
	public Optional<MethodInfo> getMethod(String methodName) {
		return methods.get().stream().filter(x -> eq(methodName, x.getSimpleName())).findFirst();
	}

	/**
	 * Returns the value of the {@code value()} method on this annotation as a string.
	 *
	 * <p>
	 * This is a convenience method equivalent to calling {@link #getString(String) getString("value")}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @MyAnnotation("foo")</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	String <jv>value</jv> = <jv>ai</jv>.getValue().orElse(<js>"default"</js>);  <jc>// Returns "foo"</jc>
	 * </p>
	 *
	 * @return An {@link Optional} containing the value of the {@code value()} method, or empty if not found or not a string.
	 */
	public Optional<String> getValue() { return getString("value"); }

	/**
	 * Returns the value of the specified method on this annotation as a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @MyAnnotation(name="John", age=30)</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	String <jv>name</jv> = <jv>ai</jv>.getString(<js>"name"</js>).orElse(<js>"unknown"</js>);  <jc>// Returns "John"</jc>
	 * </p>
	 *
	 * @param methodName The method name.
	 * @return An {@link Optional} containing the value as a string, or empty if not found or not a string type.
	 */
	public Optional<String> getString(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(String.class)).map(x -> s(x.invoke(a)));
	}

	/**
	 * Returns the value of the specified method on this annotation as an integer.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @MyAnnotation(priority=5)</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	<jk>int</jk> <jv>priority</jv> = <jv>ai</jv>.getInt(<js>"priority"</js>).orElse(0);  <jc>// Returns 5</jc>
	 * </p>
	 *
	 * @param methodName The method name.
	 * @return An {@link Optional} containing the value as an integer, or empty if not found or not an {@code int} type.
	 */
	public Optional<Integer> getInt(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(int.class)).map(x -> (Integer)x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a boolean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @MyAnnotation(enabled=true)</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	<jk>boolean</jk> <jv>enabled</jv> = <jv>ai</jv>.getBoolean(<js>"enabled"</js>).orElse(<jk>false</jk>);  <jc>// Returns true</jc>
	 * </p>
	 *
	 * @param methodName The method name.
	 * @return An {@link Optional} containing the value as a boolean, or empty if not found or not a {@code boolean} type.
	 */
	public Optional<Boolean> getBoolean(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(boolean.class)).map(x -> (Boolean)x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a long.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @MyAnnotation(timestamp=1234567890L)</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	<jk>long</jk> <jv>timestamp</jv> = <jv>ai</jv>.getLong(<js>"timestamp"</js>).orElse(0L);  <jc>// Returns 1234567890L</jc>
	 * </p>
	 *
	 * @param methodName The method name.
	 * @return An {@link Optional} containing the value as a long, or empty if not found or not a {@code long} type.
	 */
	public Optional<Long> getLong(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(long.class)).map(x -> (Long)x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a double.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @MyAnnotation(threshold=0.95)</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	<jk>double</jk> <jv>threshold</jv> = <jv>ai</jv>.getDouble(<js>"threshold"</js>).orElse(0.0);  <jc>// Returns 0.95</jc>
	 * </p>
	 *
	 * @param methodName The method name.
	 * @return An {@link Optional} containing the value as a double, or empty if not found or not a {@code double} type.
	 */
	public Optional<Double> getDouble(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(double.class)).map(x -> (Double)x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a float.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @MyAnnotation(weight=0.5f)</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	<jk>float</jk> <jv>weight</jv> = <jv>ai</jv>.getFloat(<js>"weight"</js>).orElse(0.0f);  <jc>// Returns 0.5f</jc>
	 * </p>
	 *
	 * @param methodName The method name.
	 * @return An {@link Optional} containing the value as a float, or empty if not found or not a {@code float} type.
	 */
	public Optional<Float> getFloat(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(float.class)).map(x -> (Float)x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a class.
	 *
	 * <p>
	 * For type-safe access to a class of a specific supertype, use {@link #getClassValue(String, Class)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @MyAnnotation(type=String.class)</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	Class&lt;?&gt; <jv>type</jv> = <jv>ai</jv>.getClassValue(<js>"type"</js>).orElse(<jk>null</jk>);  <jc>// Returns String.class</jc>
	 * </p>
	 *
	 * @param methodName The method name.
	 * @return An {@link Optional} containing the class value, or empty if not found or not a {@link Class} type.
	 */
	@SuppressWarnings("unchecked")
	public Optional<Class<?>> getClassValue(String methodName) {
		return (Optional<Class<?>>)(Optional<?>)getMethod(methodName).filter(x -> x.hasReturnType(Class.class)).map(x -> x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a class of a specific type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get a serializer class from an annotation</jc>
	 * 	Optional&lt;Class&lt;? <jk>extends</jk> Serializer&gt;&gt; <jv>serializerClass</jv> =
	 * 		<jv>annotationInfo</jv>.getClassValue(<js>"serializer"</js>, Serializer.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The expected supertype of the class.
	 * @param methodName The method name.
	 * @param type The expected supertype of the class value.
	 * @return An optional containing the value of the specified method cast to the expected type,
	 *         or empty if not found, not a class, or not assignable to the expected type.
	 */
	@SuppressWarnings({ "unchecked", "hiding" })
	public <T> Optional<Class<? extends T>> getClassValue(String methodName, Class<T> type) {
		// @formatter:off
		return getMethod(methodName)
			.filter(x -> x.hasReturnType(Class.class))
			.map(x -> (Class<?>)x.invoke(a))
			.filter(type::isAssignableFrom)
			.map(x -> (Class<? extends T>)x);
		// @formatter:on
	}

	/**
	 * Returns the value of the specified method on this annotation as a string array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @MyAnnotation(tags={"foo", "bar"})</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	String[] <jv>tags</jv> = <jv>ai</jv>.getStringArray(<js>"tags"</js>).orElse(<jk>new</jk> String[0]);  <jc>// Returns ["foo", "bar"]</jc>
	 * </p>
	 *
	 * @param methodName The method name.
	 * @return An {@link Optional} containing the string array value, or empty if not found or not a {@code String[]} type.
	 */
	public Optional<String[]> getStringArray(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(String[].class)).map(x -> (String[])x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a class array.
	 *
	 * <p>
	 * For type-safe access to an array of classes of a specific supertype, use {@link #getClassArray(String, Class)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// For annotation: @MyAnnotation(types={String.class, Integer.class})</jc>
	 * 	AnnotationInfo&lt;MyAnnotation&gt; <jv>ai</jv> = ...;
	 * 	Class&lt;?&gt;[] <jv>types</jv> = <jv>ai</jv>.getClassArray(<js>"types"</js>).orElse(<jk>new</jk> Class[0]);  <jc>// Returns [String.class, Integer.class]</jc>
	 * </p>
	 *
	 * @param methodName The method name.
	 * @return An {@link Optional} containing the class array value, or empty if not found or not a {@code Class[]} type.
	 */
	@SuppressWarnings("unchecked")
	public Optional<Class<?>[]> getClassArray(String methodName) {
		return (Optional<Class<?>[]>)(Optional<?>)getMethod(methodName).filter(x -> x.hasReturnType(Class[].class)).map(x -> x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a class array of a specific type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get an array of serializer classes from an annotation</jc>
	 * 	Optional&lt;Class&lt;? <jk>extends</jk> Serializer&gt;[]&gt; <jv>serializerClasses</jv> =
	 * 		<jv>annotationInfo</jv>.getClassArray(<js>"serializers"</js>, Serializer.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The expected supertype of the classes.
	 * @param methodName The method name.
	 * @param type The expected supertype of the class values.
	 * @return An optional containing the value of the specified method cast to the expected type,
	 *         or empty if not found, not a class array, or any element is not assignable to the expected type.
	 */
	@SuppressWarnings({ "unchecked", "hiding" })
	public <T> Optional<Class<? extends T>[]> getClassArray(String methodName, Class<T> type) {
		// @formatter:off
		return getMethod(methodName)
			.filter(x -> x.hasReturnType(Class[].class))
			.map(x -> (Class<?>[])x.invoke(a))
			.filter(arr -> {
				for (var c : arr) {
					if (!type.isAssignableFrom(c))
						return false;
				}
				return true;
			})
			.map(x -> (Class<? extends T>[])x);
		// @formatter:on
	}

	/**
	 * Returns the return type of the specified method on this annotation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Optional&lt;ClassInfo&gt; <jv>returnType</jv> = <jv>annotationInfo</jv>.getReturnType(<js>"value"</js>);
	 * </p>
	 *
	 * @param methodName The method name.
	 * @return An optional containing the return type of the specified method, or empty if method not found.
	 */
	public Optional<ClassInfo> getReturnType(String methodName) {
		return getMethod(methodName).map(x -> x.getReturnType());
	}
}