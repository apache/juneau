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
import static org.apache.juneau.common.utils.ClassUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.PredicateUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.annotation.*;

/**
 * Represents an annotation instance on a class and the class it was found on.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> The annotation type.
 */
public class AnnotationInfo<T extends Annotation> {

	/**
	 * Convenience constructor for creating an annotation info object.
	 *
	 * @param <A> The annotation class.
	 * @param on The annotatable object where the annotation was found (class, method, field, constructor, parameter, or package).
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(Annotatable on, A value) {
		return new AnnotationInfo<>(on, value);
	}


	private final Annotatable annotatable;
	final int rank;
	private T a;  // Effectively final

	private final Supplier<List<MethodInfo>> methods = memoize(() -> stream(a.annotationType().getMethods()).map(m -> MethodInfo.of(ClassInfo.of(a.annotationType()), m)).toList());

	/**
	 * Constructor for class annotations.
	 *
	 * @param c The class where the annotation was found.
	 * @param a The annotation found.
	 */
	AnnotationInfo(Annotatable on, T a) {
		this.annotatable = on;  // TODO - Shouldn't allow null.
		this.a = assertArgNotNull("a", a);
		this.rank = findRank(a);
	}

	/**
	 * Returns the rank of this annotation.
	 * 
	 * <p>
	 * The rank is used for sorting annotations in order of precedence.
	 * 
	 * @return The rank of this annotation.
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * Performs an action on all matching values on this annotation.
	 *
	 * @param <V> The annotation field type.
	 * @param type The annotation field type.
	 * @param name The annotation field name.
	 * @param test A predicate to apply to the value to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the value.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public <V> AnnotationInfo<?> forEachValue(Class<V> type, String name, Predicate<V> test, Consumer<V> action) {
		methods.get().stream()
			.filter(m -> eq(m.getName(), name) && eq(m.getReturnType().inner(), type))
			.forEach(m -> safe(() -> consumeIf(test, action, (V)m.invoke(a))));
		return this;
	}

	/**
	 * Returns the class name of the annotation.
	 *
	 * @return The simple class name of the annotation.
	 */
	public String getName() { return scn(a.annotationType()); }

	/**
	 * Returns a matching value on this annotation.
	 *
	 * @param <V> The annotation field type.
	 * @param type The annotation field type.
	 * @param name The annotation field name.
	 * @param test A predicate to apply to the value to determine if value should be used.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public <V> Optional<V> getValue(Class<V> type, String name, Predicate<V> test) {
		return methods.get().stream()
			.filter(m -> eq(m.getName(), name) && eq(m.getReturnType().inner(), type))
			.map(m -> safe(() -> (V)m.invoke(a)))
			.filter(v -> test(test, v))
			.findFirst();
	}

	/**
	 * Casts this annotation info to a specific annotation type.
	 *
	 * @param <A> The annotation type to cast to.
	 * @param type The annotation type to cast to.
	 * @return This annotation info cast to the specified type, or <jk>null</jk> if the cast is not valid.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> AnnotationInfo<A> cast(Class<A> type) {
		return type.isInstance(a) ? (AnnotationInfo<A>)this : null;
	}

	/**
	 * Returns <jk>true</jk> if this annotation has the specified annotation defined on it.
	 *
	 * @param <A> The annotation class.
	 * @param type The annotation to test for.
	 * @return <jk>true</jk> if this annotation has the specified annotation defined on it.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return nn(this.a.annotationType().getAnnotation(type));
	}

	/**
	 * Returns the annotation found.
	 *
	 * @return The annotation found.
	 */
	public T inner() {
		return a;
	}

	/**
	 * Returns <jk>true</jk> if this annotation is in the specified {@link AnnotationGroup group}.
	 *
	 * @param <A> The annotation class.
	 * @param group The group annotation.
	 * @return <jk>true</jk> if this annotation is in the specified {@link AnnotationGroup group}.
	 */
	public <A extends Annotation> boolean isInGroup(Class<A> group) {
		var x = a.annotationType().getAnnotation(AnnotationGroup.class);
		return (nn(x) && x.value().equals(group));
	}

	/**
	 * Returns <jk>true</jk> if this annotation is the specified type.
	 *
	 * @param <A> The annotation class.
	 * @param type The type to test against.
	 * @return <jk>true</jk> if this annotation is the specified type.
	 */
	public <A extends Annotation> boolean isType(Class<A> type) {
		return this.a.annotationType() == type;
	}

	/**
	 * Converts this object to a readable map for debugging purposes.
	 *
	 * @return A new map showing the attributes of this object.
	 */
	public LinkedHashMap<String, Object> toMap() {
		var jm = new LinkedHashMap<String, Object>();
		jm.put(s(annotatable.getAnnotatableType()), annotatable.getAnnotatableName());
		var ja = new LinkedHashMap<String, Object>();
		var ca = ClassInfo.of(a.annotationType());
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
		var location = new StringBuilder();
		var ci = annotatable.getClassInfo();

		if (nn(ci)) {
			location.append(ci.getNameSimple());
			var type = annotatable.getAnnotatableType();
			if (type == AnnotatableType.METHOD_TYPE || type == AnnotatableType.FIELD_TYPE ||
				type == AnnotatableType.CONSTRUCTOR_TYPE || type == AnnotatableType.PARAMETER_TYPE) {
				location.append('.').append(annotatable.getAnnotatableName());
			}
		} else {
			// Package
			location.append(annotatable.getAnnotatableName());
		}

		return "@" + scn(a.annotationType()) + "(on=" + location + ")";
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
	// Static methods for ClassInfo
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Performs an action on all matching annotations on the specified class/parents/package.
	 *
	 * <p>
	 * Annotations are consumed in the following order:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param classInfo The class to process.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 */
	// TODO: Once ClassInfo arrays are converted to Lists, convert reverse iterations to rstream() and nested loops to flatMap()
	public static void forEachAnnotationInfo(ClassInfo classInfo, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		var pi = classInfo.getPackage();
		if (nn(pi))
			for (var ai : pi.getAnnotations())
				if (filter == null || filter.test(ai))
					action.accept(ai);
		var interfaces = classInfo.getInterfaces();
		for (int i = interfaces.size() - 1; i >= 0; i--)
			for (var a : interfaces.get(i).inner().getDeclaredAnnotations())
				for (var a2 : splitRepeated(a)) {
					var ai = AnnotationInfo.of(interfaces.get(i), a2);
					if (filter == null || filter.test(ai))
						action.accept(ai);
				}
		var parents = classInfo.getParents();
		for (int i = parents.size() - 1; i >= 0; i--)
			for (var a : parents.get(i).inner().getDeclaredAnnotations())
				for (var a2 : splitRepeated(a)) {
					var ai = AnnotationInfo.of(parents.get(i), a2);
					if (filter == null || filter.test(ai))
						action.accept(ai);
				}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Static methods for MethodInfo
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Performs an action on all matching annotations on the specified method.
	 *
	 * @param methodInfo The method to process.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 */
	// TODO: Once ClassInfo arrays are converted to Lists, convert reverse iterations to rstream()
	public static void forEachAnnotationInfo(MethodInfo methodInfo, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		var c = methodInfo.getDeclaringClass();
		forEachDeclaredAnnotationInfo(c.getPackage(), filter, action);
		var interfaces = c.getInterfaces();
		for (int i = interfaces.size() - 1; i >= 0; i--) {
			forEachDeclaredAnnotationInfo(interfaces.get(i), filter, action);
			forEachDeclaredMethodAnnotationInfo(methodInfo, interfaces.get(i), filter, action);
		}
		var parents = c.getParents();
		for (int i = parents.size() - 1; i >= 0; i--) {
			forEachDeclaredAnnotationInfo(parents.get(i), filter, action);
			forEachDeclaredMethodAnnotationInfo(methodInfo, parents.get(i), filter, action);
		}
	}

	/**
	 * Performs an action on all matching annotations on methods only.
	 *
	 * @param methodInfo The method to process.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 */
	// TODO: Once ClassInfo arrays are converted to Lists, convert reverse iterations to rstream()
	public static void forEachAnnotationInfoMethodOnly(MethodInfo methodInfo, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		var c = methodInfo.getDeclaringClass();
		var interfaces = c.getInterfaces();
		for (int i = interfaces.size() - 1; i >= 0; i--)
			forEachDeclaredMethodAnnotationInfo(methodInfo, interfaces.get(i), filter, action);
		var parents = c.getParents();
		for (int i = parents.size() - 1; i >= 0; i--)
			forEachDeclaredMethodAnnotationInfo(methodInfo, parents.get(i), filter, action);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Private helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private static int findRank(Object a) {
		return ClassInfo.of(a).getMethods().stream()
			.filter(m -> m.hasName("rank") && m.hasNoParameters() && m.hasReturnType(int.class))
			.findFirst()
			.map(m -> safe(() -> (int)m.invoke(a)))
			.orElse(0);
	}

	@SuppressWarnings("unchecked")
	private static void forEachDeclaredAnnotationInfo(ClassInfo ci, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		if (nn(ci))
			for (var ai : ci.getDeclaredAnnotationInfos())
				if (filter == null || filter.test(ai))
					action.accept(ai);
	}

	private static void forEachDeclaredAnnotationInfo(PackageInfo pi, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		if (nn(pi))
			for (var ai : pi.getAnnotations())
				if (filter == null || filter.test(ai))
					action.accept(ai);
	}

	private static void forEachDeclaredMethodAnnotationInfo(MethodInfo methodInfo, ClassInfo ci, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		MethodInfo mi = methodInfo.findMatchingOnClass(ci);
		if (nn(mi))
			mi.getDeclaredAnnotationInfos().forEach(ai -> {
				if (filter == null || filter.test(ai))
					action.accept(ai);
			});
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
	 * @param methodName The method name.
	 * @return An optional containing the method, or empty if method not found.
	 */
	public Optional<MethodInfo> getMethod(String methodName) {
		return methods.get().stream().filter(x -> eq(methodName, x.getSimpleName())).findFirst();
	}

	/**
	 * Returns the value of the <c>value()</c> method on this annotation as a string.
	 *
	 * @return An optional containing the value of the <c>value()</c> method, or empty if not found or not a string.
	 */
	public Optional<String> getValue() {
		return getString("value");
	}

	/**
	 * Returns the value of the specified method on this annotation as a string.
	 *
	 * @param methodName The method name.
	 * @return An optional containing the value of the specified method, or empty if not found or not a string.
	 */
	public Optional<String> getString(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(String.class)).map(x -> s(x.invoke(a)));
	}

	/**
	 * Returns the value of the specified method on this annotation as an integer.
	 *
	 * @param methodName The method name.
	 * @return An optional containing the value of the specified method, or empty if not found or not an integer.
	 */
	public Optional<Integer> getInt(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(int.class)).map(x -> (Integer)x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a boolean.
	 *
	 * @param methodName The method name.
	 * @return An optional containing the value of the specified method, or empty if not found or not a boolean.
	 */
	public Optional<Boolean> getBoolean(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(boolean.class)).map(x -> (Boolean)x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a long.
	 *
	 * @param methodName The method name.
	 * @return An optional containing the value of the specified method, or empty if not found or not a long.
	 */
	public Optional<Long> getLong(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(long.class)).map(x -> (Long)x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a double.
	 *
	 * @param methodName The method name.
	 * @return An optional containing the value of the specified method, or empty if not found or not a double.
	 */
	public Optional<Double> getDouble(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(double.class)).map(x -> (Double)x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a float.
	 *
	 * @param methodName The method name.
	 * @return An optional containing the value of the specified method, or empty if not found or not a float.
	 */
	public Optional<Float> getFloat(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(float.class)).map(x -> (Float)x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a class.
	 *
	 * @param methodName The method name.
	 * @return An optional containing the value of the specified method, or empty if not found or not a class.
	 */
	@SuppressWarnings("unchecked")
	public Optional<Class<?>> getClassValue(String methodName) {
		return (Optional<Class<?>>)(Optional<?>)getMethod(methodName).filter(x -> x.hasReturnType(Class.class)).map(x -> x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a string array.
	 *
	 * @param methodName The method name.
	 * @return An optional containing the value of the specified method, or empty if not found or not a string array.
	 */
	public Optional<String[]> getStringArray(String methodName) {
		return getMethod(methodName).filter(x -> x.hasReturnType(String[].class)).map(x -> (String[])x.invoke(a));
	}

	/**
	 * Returns the value of the specified method on this annotation as a class array.
	 *
	 * @param methodName The method name.
	 * @return An optional containing the value of the specified method, or empty if not found or not a class array.
	 */
	@SuppressWarnings("unchecked")
	public Optional<Class<?>[]> getClassArray(String methodName) {
		return (Optional<Class<?>[]>)(Optional<?>)getMethod(methodName).filter(x -> x.hasReturnType(Class[].class)).map(x -> x.invoke(a));
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