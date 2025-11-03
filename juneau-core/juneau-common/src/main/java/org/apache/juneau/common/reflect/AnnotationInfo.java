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

import static org.apache.juneau.common.utils.ClassUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.PredicateUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
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
	 * Convenience constructor when annotation is found on a class.
	 *
	 * @param <A> The annotation class.
	 * @param onClass The class where the annotation was found.
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(ClassInfo onClass, A value) {
		return new AnnotationInfo<>(onClass, value);
	}

	/**
	 * Convenience constructor when annotation is found on a method.
	 *
	 * @param <A> The annotation class.
	 * @param onMethod The method where the annotation was found.
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(MethodInfo onMethod, A value) {
		return new AnnotationInfo<>(onMethod, value);
	}

	/**
	 * Convenience constructor when annotation is found on a package.
	 *
	 * @param <A> The annotation class.
	 * @param onPackage The package where the annotation was found.
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(Package onPackage, A value) {
		return new AnnotationInfo<>(PackageInfo.of(onPackage), value);
	}

	/**
	 * Convenience constructor when annotation is found on a package.
	 *
	 * @param <A> The annotation class.
	 * @param onPackage The package where the annotation was found.
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(PackageInfo onPackage, A value) {
		return new AnnotationInfo<>(onPackage, value);
	}

	/**
	 * Convenience constructor when annotation is found on a field.
	 *
	 * @param <A> The annotation class.
	 * @param onField The field where the annotation was found.
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(FieldInfo onField, A value) {
		return new AnnotationInfo<>(onField, value);
	}

	/**
	 * Convenience constructor when annotation is found on a constructor.
	 *
	 * @param <A> The annotation class.
	 * @param onConstructor The constructor where the annotation was found.
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(ConstructorInfo onConstructor, A value) {
		return new AnnotationInfo<>(onConstructor, value);
	}

	/**
	 * Convenience constructor when annotation is found on a parameter.
	 *
	 * @param <A> The annotation class.
	 * @param onParam The parameter where the annotation was found.
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(ParameterInfo onParam, A value) {
		return new AnnotationInfo<>(onParam, value);
	}

	private static int getRank(Object a) {
		var ci = ClassInfo.of(a);
		var mi = ci.getPublicMethod(x -> x.hasName("rank") && x.hasNoParams() && x.hasReturnType(int.class));
		if (nn(mi)) {
			return safe(() -> (int)mi.invoke(a));
		}
		return 0;
	}

	private final ClassInfo c;
	private final MethodInfo m;
	private final FieldInfo f;
	private final ConstructorInfo ctor;
	private final ParameterInfo param;
	private final PackageInfo p;

	private T a;  // Effectively final

	private final Supplier<List<Method>> methods = memoize(() -> u(l(a.annotationType().getMethods())));

	final int rank;

	/**
	 * Constructor for class annotations.
	 *
	 * @param c The class where the annotation was found.
	 * @param a The annotation found.
	 */
	AnnotationInfo(ClassInfo c, T a) {
		this.c = c;
		this.m = null;
		this.f = null;
		this.ctor = null;
		this.param = null;
		this.p = null;
		this.a = a;
		this.rank = getRank(a);
	}

	/**
	 * Constructor for method annotations.
	 *
	 * @param m The method where the annotation was found.
	 * @param a The annotation found.
	 */
	AnnotationInfo(MethodInfo m, T a) {
		this.c = null;
		this.m = m;
		this.f = null;
		this.ctor = null;
		this.param = null;
		this.p = null;
		this.a = a;
		this.rank = getRank(a);
	}

	/**
	 * Constructor for field annotations.
	 *
	 * @param f The field where the annotation was found.
	 * @param a The annotation found.
	 */
	AnnotationInfo(FieldInfo f, T a) {
		this.c = null;
		this.m = null;
		this.f = f;
		this.ctor = null;
		this.param = null;
		this.p = null;
		this.a = a;
		this.rank = getRank(a);
	}

	/**
	 * Constructor for constructor annotations.
	 *
	 * @param ctor The constructor where the annotation was found.
	 * @param a The annotation found.
	 */
	AnnotationInfo(ConstructorInfo ctor, T a) {
		this.c = null;
		this.m = null;
		this.f = null;
		this.ctor = ctor;
		this.param = null;
		this.p = null;
		this.a = a;
		this.rank = getRank(a);
	}

	/**
	 * Constructor for parameter annotations.
	 *
	 * @param param The parameter where the annotation was found.
	 * @param a The annotation found.
	 */
	AnnotationInfo(ParameterInfo param, T a) {
		this.c = null;
		this.m = null;
		this.f = null;
		this.ctor = null;
		this.param = param;
		this.p = null;
		this.a = a;
		this.rank = getRank(a);
	}

	/**
	 * Constructor for package annotations.
	 *
	 * @param p The package where the annotation was found.
	 * @param a The annotation found.
	 */
	AnnotationInfo(PackageInfo p, T a) {
		this.c = null;
		this.m = null;
		this.f = null;
		this.ctor = null;
		this.param = null;
		this.p = p;
		this.a = a;
		this.rank = getRank(a);
	}

	/**
	 * Performs an action on this object if the specified predicate test passes.
	 *
	 * @param test A test to apply to determine if action should be executed.  Can be <jk>null</jk>.
	 * @param action An action to perform on this object.
	 * @return This object.
	 */
	public AnnotationInfo<?> accept(Predicate<AnnotationInfo<?>> test, Consumer<AnnotationInfo<?>> action) {
		if (matches(test))
			action.accept(this);
		return this;
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
			.filter(m -> eq(m.getName(), name) && eq(m.getReturnType(), type))
			.forEach(m -> safe(() -> consumeIf(test, action, (V)m.invoke(a))));
		return this;
	}

	/**
	 * Returns the class that this annotation was found on.
	 *
	 * <p>
	 * Returns the declaring class from whichever context this annotation belongs to.
	 *
	 * @return The class that this annotation was found on, or <jk>null</jk> if it was found on a package.
	 */
	public ClassInfo getClassInfo() {
		if (nn(this.c))
			return this.c;
		if (nn(this.m))
			return this.m.getDeclaringClass();
		if (nn(this.f))
			return this.f.getDeclaringClass();
		if (nn(this.ctor))
			return this.ctor.getDeclaringClass();
		if (nn(this.param))
			return this.param.getDeclaringExecutable().getDeclaringClass();
		return null;
	}

	/**
	 * Returns the class where the annotation was found.
	 *
	 * @return the class where the annotation was found, or <jk>null</jk> if it wasn't found on a method.
	 */
	public ClassInfo getClassOn() { return c; }

	/**
	 * Returns the method where the annotation was found.
	 *
	 * @return the method where the annotation was found, or <jk>null</jk> if it wasn't found on a method.
	 */
	public MethodInfo getMethodOn() { return m; }

	/**
	 * Returns the field where the annotation was found.
	 *
	 * @return the field where the annotation was found, or <jk>null</jk> if it wasn't found on a field.
	 */
	public FieldInfo getFieldOn() { return f; }

	/**
	 * Returns the constructor where the annotation was found.
	 *
	 * @return the constructor where the annotation was found, or <jk>null</jk> if it wasn't found on a constructor.
	 */
	public ConstructorInfo getConstructorOn() { return ctor; }

	/**
	 * Returns the parameter where the annotation was found.
	 *
	 * @return the parameter where the annotation was found, or <jk>null</jk> if it wasn't found on a parameter.
	 */
	public ParameterInfo getParamOn() { return param; }

	/**
	 * Returns the class name of the annotation.
	 *
	 * @return The simple class name of the annotation.
	 */
	public String getName() { return scn(a.annotationType()); }

	/**
	 * Returns the package where the annotation was found.
	 *
	 * @return the package where the annotation was found, or <jk>null</jk> if it wasn't found on a package.
	 */
	public PackageInfo getPackageOn() { return p; }

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
			.filter(m -> eq(m.getName(), name) && eq(m.getReturnType(), type))
			.map(m -> safe(() -> (V)m.invoke(a)))
			.filter(v -> test(test, v))
			.findFirst();
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
	 * Returns <jk>true</jk> if this object passes the specified predicate test.
	 *
	 * @param test The test to perform.
	 * @return <jk>true</jk> if this object passes the specified predicate test.
	 */
	public boolean matches(Predicate<AnnotationInfo<?>> test) {
		return test(test, this);
	}

	/**
	 * Converts this object to a readable map for debugging purposes.
	 *
	 * @return A new map showing the attributes of this object.
	 */
	public LinkedHashMap<String, Object> toMap() {
		var jm = new LinkedHashMap<String, Object>();
		if (nn(c))
			jm.put("class", c.getNameSimple());
		if (nn(m))
			jm.put("method", m.getShortName());
		if (nn(f))
			jm.put("field", f.getName());
		if (nn(ctor))
			jm.put("constructor", ctor.getShortName());
		if (nn(param))
			jm.put("parameter", param.getName());
		if (nn(p))
			jm.put("package", p.getName());
		var ja = new LinkedHashMap<String, Object>();
		var ca = ClassInfo.of(a.annotationType());
		ca.forEachDeclaredMethod(null, x -> {
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
				ai.accept(filter, action);
		var interfaces = classInfo.getInterfaces();
		for (int i = interfaces.size() - 1; i >= 0; i--)
			for (var a : interfaces.get(i).inner().getDeclaredAnnotations())
				for (var a2 : splitRepeated(a))
					AnnotationInfo.of(interfaces.get(i), a2).accept(filter, action);
		var parents = classInfo.getParents();
		for (int i = parents.size() - 1; i >= 0; i--)
			for (var a : parents.get(i).inner().getDeclaredAnnotations())
				for (var a2 : splitRepeated(a))
					AnnotationInfo.of(parents.get(i), a2).accept(filter, action);
	}

	/**
	 * Constructs an {@link AnnotationList} of all annotations found on the specified class.
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param classInfo The class to process.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public static AnnotationList getAnnotationList(ClassInfo classInfo) {
		return getAnnotationList(classInfo, x -> true);
	}

	/**
	 * Constructs an {@link AnnotationList} of all matching annotations on the specified class.
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param classInfo The class to process.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public static AnnotationList getAnnotationList(ClassInfo classInfo, Predicate<AnnotationInfo<?>> filter) {
		var l = new AnnotationList();
		forEachAnnotationInfo(classInfo, filter, x -> l.add(x));
		return l;
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
	 * Constructs an {@link AnnotationList} of all annotations found on the specified method.
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
	 * @param methodInfo The method to process.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public static AnnotationList getAnnotationList(MethodInfo methodInfo) {
		return getAnnotationList(methodInfo, x -> true);
	}

	/**
	 * Constructs an {@link AnnotationList} of all matching annotations found on the specified method.
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
	 * @param methodInfo The method to process.
	 * @param filter A predicate to apply to the entries to determine if value should be added.  Can be <jk>null</jk>.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public static AnnotationList getAnnotationList(MethodInfo methodInfo, Predicate<AnnotationInfo<?>> filter) {
		var al = new AnnotationList();
		forEachAnnotationInfo(methodInfo, filter, x -> al.add(x));
		return al;
	}

	/**
	 * Same as {@link #getAnnotationList(MethodInfo, Predicate)} except only returns annotations defined on methods.
	 *
	 * @param methodInfo The method to process.
	 * @param filter A predicate to apply to the entries to determine if value should be added.  Can be <jk>null</jk>.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public static AnnotationList getAnnotationListMethodOnly(MethodInfo methodInfo, Predicate<AnnotationInfo<?>> filter) {
		var al = new AnnotationList();
		forEachAnnotationInfoMethodOnly(methodInfo, filter, x -> al.add(x));
		return al;
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

	private static void forEachDeclaredAnnotationInfo(ClassInfo ci, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		if (nn(ci))
			for (var ai : ci.getDeclaredAnnotationInfos())
				ai.accept(filter, action);
	}

	private static void forEachDeclaredAnnotationInfo(PackageInfo pi, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		if (nn(pi))
			for (var ai : pi.getAnnotations())
				ai.accept(filter, action);
	}

	private static void forEachDeclaredMethodAnnotationInfo(MethodInfo methodInfo, ClassInfo ci, Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		MethodInfo mi = methodInfo.findMatchingOnClass(ci);
		if (nn(mi))
			for (var a : mi._getDeclaredAnnotations())
				AnnotationInfo.of(mi, a).accept(filter, action);
	}
}