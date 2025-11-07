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
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.collections.*;

/**
 * Enhanced annotation provider that returns {@link AnnotationInfo} objects and supports runtime annotations.
 *
 * <p>
 * This class provides a modern API for retrieving annotations with the following benefits:
 * <ul>
 * 	<li>Returns {@link AnnotationInfo} wrappers that provide additional methods and type safety
 * 	<li>Supports filtering by annotation type using streams
 * 	<li>Properly handles repeatable annotations
 * 	<li>Searches up the class/method hierarchy with well-defined order of precedence
 * 	<li>Supports runtime annotations (annotations added programmatically at runtime)
 * 	<li>Caches results for performance
 * </ul>
 *
 * <h5 class='section'>Annotation Order of Precedence:</h5>
 *
 * <h6 class='topic'>For Classes ({@link #find(Class)}):</h6>
 * <p>
 * Annotations are returned in <b>child-to-parent</b> order with the following precedence:
 * <ol>
 * 	<li><b>Runtime annotations</b> on the class (highest priority)
 * 	<li><b>Declared annotations</b> on the class
 * 	<li><b>Runtime annotations</b> on parent classes (child-to-parent order)
 * 	<li><b>Declared annotations</b> on parent classes (child-to-parent order)
 * 	<li><b>Runtime annotations</b> on interfaces (child-to-parent order)
 * 	<li><b>Declared annotations</b> on interfaces (child-to-parent order)
 * 	<li><b>Declared annotations</b> on the package (lowest priority)
 * </ol>
 *
 * <p class='bcode'>
 * 	<jc>// Example: Given class Child extends Parent</jc>
 * 	<jc>// Annotation order will be:</jc>
 * 	<jc>// 1. Runtime annotations on Child</jc>
 * 	<jc>// 2. @Annotation on Child</jc>
 * 	<jc>// 3. Runtime annotations on Parent</jc>
 * 	<jc>// 4. @Annotation on Parent</jc>
 * 	<jc>// 5. Runtime annotations on IChild (if Child implements IChild)</jc>
 * 	<jc>// 6. @Annotation on IChild</jc>
 * 	<jc>// 7. @Annotation on package-info.java</jc>
 * </p>
 *
 * <h6 class='topic'>For Methods ({@link #find(Method)}):</h6>
 * <p>
 * Annotations are returned in <b>child-to-parent</b> order with the following precedence:
 * <ol>
 * 	<li><b>Runtime annotations</b> on the method (highest priority)
 * 	<li><b>Declared annotations</b> on the method
 * 	<li><b>Runtime annotations</b> on overridden parent methods (child-to-parent order)
 * 	<li><b>Declared annotations</b> on overridden parent methods (child-to-parent order)
 * </ol>
 *
 * <h6 class='topic'>For Fields ({@link #find(Field)}):</h6>
 * <p>
 * Annotations are returned with the following precedence:
 * <ol>
 * 	<li><b>Runtime annotations</b> on the field (highest priority)
 * 	<li><b>Declared annotations</b> on the field
 * </ol>
 *
 * <h6 class='topic'>For Constructors ({@link #find(Constructor)}):</h6>
 * <p>
 * Annotations are returned with the following precedence:
 * <ol>
 * 	<li><b>Runtime annotations</b> on the constructor (highest priority)
 * 	<li><b>Declared annotations</b> on the constructor
 * </ol>
 *
 * <h5 class='section'>Runtime Annotations:</h5>
 * <p>
 * Runtime annotations are concrete objects that implement annotation interfaces, added programmatically via the
 * builder's {@link Builder#addRuntimeAnnotations(List)} method. They allow you to dynamically apply annotations
 * to classes, methods, fields, and constructors at runtime without modifying source code.
 *
 * <p>
 * <b>How Runtime Annotations Work:</b>
 * <ul>
 * 	<li>Runtime annotations are Java objects that implement annotation interfaces (e.g., {@code @Bean})
 * 	<li>They use special methods like {@code on()} or {@code onClass()} to specify their targets
 * 	<li>They always take precedence over declared annotations at the same level
 * 	<li>They are particularly useful for applying annotations to classes you don't control
 * </ul>
 *
 * <p class='bjava'>
 * 	<jc>// Example: Creating a runtime annotation</jc>
 * 	Bean <jv>runtimeAnnotation</jv> = BeanAnnotation
 * 		.<jsm>create</jsm>()
 * 		.onClass(MyClass.<jk>class</jk>)  <jc>// Target class</jc>
 * 		.typeName(<js>"MyType"</js>)         <jc>// Annotation property</jc>
 * 		.build();
 *
 * 	<jc>// Add to provider</jc>
 * 	AnnotationProvider <jv>provider</jv> = AnnotationProvider
 * 		.<jsm>create</jsm>()
 * 		.addRuntimeAnnotations(<jv>runtimeAnnotation</jv>)
 * 		.build();
 *
 * 	<jc>// Now MyClass will be found with @Bean annotation</jc>
 * 	Stream&lt;AnnotationInfo&lt;Bean&gt;&gt; <jv>annotations</jv> = <jv>provider</jv>.find(Bean.<jk>class</jk>, MyClass.<jk>class</jk>);
 * </p>
 *
 * <p>
 * <b>Targeting Methods:</b>
 * <ul>
 * 	<li>{@code on()} - String array of fully-qualified names (e.g., {@code "com.example.MyClass.myMethod"})
 * 	<li>{@code onClass()} - Class array for type-safe targeting
 * </ul>
 *
 * <p>
 * Runtime annotations are evaluated before declared annotations at each level, giving them higher priority.
 * For example, a runtime {@code @Bean} annotation on a class will be found before any {@code @Bean} annotation
 * declared directly on that class.
 *
 * <h5 class='section'>Comparison with ElementInfo Methods:</h5>
 * <p>
 * The methods in this class differ from {@link ClassInfo#getAnnotationInfos()}, {@link MethodInfo#getAnnotationInfos()}, etc.:
 * <ul>
 * 	<li><b>Runtime Annotations</b>: ElementInfo methods return ONLY declared annotations. This class includes runtime annotations.
 * 	<li><b>Hierarchy</b>: ElementInfo methods may have different traversal logic. This class uses a consistent approach.
 * 	<li><b>Precedence</b>: Runtime annotations are inserted at each level with higher priority than declared annotations.
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create with default settings</jc>
 * 	AnnotationProvider <jv>provider</jv> = AnnotationProvider.<jsm>create</jsm>().build();
 *
 * 	<jc>// Create with runtime annotations</jc>
 * 	AnnotationProvider <jv>provider</jv> = AnnotationProvider
 * 		.<jsm>create</jsm>()
 * 		.annotations(<jk>new</jk> MyAnnotationImpl())
 * 		.build();
 *
 * 	<jc>// Find all annotations on a class</jc>
 * 	List&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>annotations</jv> = <jv>provider</jv>.find(MyClass.<jk>class</jk>);
 *
 * 	<jc>// Find specific annotation type on a class</jc>
 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>myAnnotations</jv> = <jv>provider</jv>.find(MyAnnotation.<jk>class</jk>, MyClass.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link AnnotationProvider}
 * 	<li class='jc'>{@link AnnotationInfo}
 * 	<li class='jc'>{@link ClassInfo}
 * 	<li class='jc'>{@link MethodInfo}
 * </ul>
 */
public class AnnotationProvider {

	/**
	 * Disable annotation caching.
	 */
	private static final boolean DISABLE_ANNOTATION_CACHING = Boolean.getBoolean("juneau.disableAnnotationCaching");

	/**
	 * Default instance.
	 */
	public static final AnnotationProvider INSTANCE = new AnnotationProvider(create());

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder for creating configured {@link AnnotationProvider} instances.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	AnnotationProvider <jv>provider</jv> = AnnotationProvider
	 * 		.<jsm>create</jsm>()
	 * 		.disableCaching()
	 * 		.build();
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link AnnotationProvider#create()}
	 * </ul>
	 */
	public static class Builder {
		boolean disableCaching;
		ReflectionMap2.Builder<Annotation> runtimeAnnotations = ReflectionMap2.create(Annotation.class);

		Builder() {
			disableCaching = DISABLE_ANNOTATION_CACHING;
		}

		/**
		 * Builds a new {@link AnnotationProvider} instance with the configured settings.
		 *
		 * @return A new immutable {@link AnnotationProvider} instance.
		 */
		public AnnotationProvider build() {
			return new AnnotationProvider(this);
		}

		/**
		 * Disables annotation caching entirely.
		 *
		 * <p>
		 * When disabled, annotation lookups will always perform fresh searches without caching results.
		 *
		 * @return This object for method chaining.
		 */
		public Builder disableCaching() {
			disableCaching = true;
			return this;
		}

		/**
		 * Conditionally disables or enables annotation caching.
		 *
		 * @param value Whether to disable caching.
		 * @return This object for method chaining.
		 */
		public Builder disableCaching(boolean value) {
			disableCaching = value;
			return this;
		}

	/**
	 * Adds runtime annotations to be applied to classes, methods, fields, and constructors.
	 *
	 * <p>
	 * Runtime annotations are concrete Java objects that implement annotation interfaces (e.g., {@code @Bean}).
	 * They allow you to dynamically apply annotations to code elements at runtime without modifying source code.
	 *
	 * <p>
	 * <b>How It Works:</b>
	 * <ol>
	 * 	<li>Create annotation objects using builder classes (e.g., {@code BeanAnnotation.create()})
	 * 	<li>Specify targets using {@code on()} or {@code onClass()} methods
	 * 	<li>Set annotation properties (e.g., {@code typeName()}, {@code properties()})
	 * 	<li>Build the annotation object
	 * 	<li>Add to the provider via this method
	 * </ol>
	 *
	 * <p>
	 * <b>Targeting Requirements:</b>
	 * <ul>
	 * 	<li>Annotations MUST define an {@code onClass()} method returning {@code Class[]} for type-safe targeting
	 * 	<li>OR an {@code on()} method returning {@code String[]} for string-based targeting
	 * 	<li>The {@code on()} method accepts fully-qualified names:
	 * 		<ul>
	 * 			<li>{@code "com.example.MyClass"} - targets a class
	 * 			<li>{@code "com.example.MyClass.myMethod"} - targets a method
	 * 			<li>{@code "com.example.MyClass.myField"} - targets a field
	 * 		</ul>
	 * </ul>
	 *
	 * <p class='bjava'>
	 * 	<jc>// Example 1: Target a specific class using type-safe targeting</jc>
	 * 	Bean <jv>beanAnnotation</jv> = BeanAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.onClass(MyClass.<jk>class</jk>)  <jc>// Targets MyClass</jc>
	 * 		.typeName(<js>"MyType"</js>)
	 * 		.properties(<js>"id,name"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Example 2: Target multiple classes</jc>
	 * 	Bean <jv>multiAnnotation</jv> = BeanAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.onClass(MyClass.<jk>class</jk>, OtherClass.<jk>class</jk>)
	 * 		.sort(<jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Example 3: Target using string names (useful for dynamic/reflection scenarios)</jc>
	 * 	Bean <jv>stringAnnotation</jv> = BeanAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.on(<js>"com.example.MyClass"</js>)
	 * 		.findFluentSetters(<jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Example 4: Target a specific method</jc>
	 * 	Swap <jv>swapAnnotation</jv> = SwapAnnotation
	 * 		.<jsm>create</jsm>()
	 * 		.on(<js>"com.example.MyClass.getValue"</js>)
	 * 		.value(MySwap.<jk>class</jk>)
	 * 		.build();
	 *
 * 	<jc>// Add all runtime annotations to the provider</jc>
 * 	AnnotationProvider <jv>provider</jv> = AnnotationProvider
 * 		.<jsm>create</jsm>()
 * 		.addRuntimeAnnotations(<jv>beanAnnotation</jv>, <jv>multiAnnotation</jv>, <jv>stringAnnotation</jv>, <jv>swapAnnotation</jv>)
 * 		.build();
	 * </p>
	 *
	 * <p>
	 * <b>Priority:</b> Runtime annotations always take precedence over declared annotations at the same level.
	 * They are evaluated first when searching for annotations.
	 *
	 * @param annotations The list of runtime annotation objects to add.
	 * @return This object for method chaining.
	 * @throws BeanRuntimeException If any annotation is invalid (missing {@code on()} or {@code onClass()} methods,
	 * 	or if the methods return incorrect types).
	 */
	public Builder addRuntimeAnnotations(List<Annotation> annotations) {

			for (var a : annotations) {
				try {
					var ci = ClassInfo.of(a.getClass());

					MethodInfo mi = ci.getPublicMethod(x -> x.hasName("onClass"));
					if (nn(mi)) {
						if (! mi.getReturnType().is(Class[].class))
							throw new BeanRuntimeException("Invalid annotation @{0} used in runtime annotations.  Annotation must define an onClass() method that returns a Class array.", scn(a));
						for (var c : (Class<?>[])mi.accessible().invoke(a))
							runtimeAnnotations.append(c.getName(), a);
					}

					mi = ci.getPublicMethod(x -> x.hasName("on"));
					if (nn(mi)) {
						if (! mi.getReturnType().is(String[].class))
							throw new BeanRuntimeException("Invalid annotation @{0} used in runtime annotations.  Annotation must define an on() method that returns a String array.", scn(a));
						for (var s : (String[])mi.accessible().invoke(a))
							runtimeAnnotations.append(s, a);
					}

				} catch (BeanRuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new BeanRuntimeException(e, null, "Invalid annotation @{0} used in runtime annotations.", cn(a));
				}
			}
			return this;
		}

		/**
		 * Adds runtime annotations to be applied to classes, methods, fields, and constructors.
		 *
		 * <p>
		 * This is a convenience method that delegates to {@link #addRuntimeAnnotations(List)}.
		 * See that method for detailed documentation on how runtime annotations work.
		 *
		 * <p class='bjava'>
		 * 	<jc>// Example: Add multiple runtime annotations using varargs</jc>
		 * 	Bean <jv>beanAnnotation</jv> = BeanAnnotation
		 * 		.<jsm>create</jsm>()
		 * 		.onClass(MyClass.<jk>class</jk>)
		 * 		.typeName(<js>"MyType"</js>)
		 * 		.build();
		 *
		 * 	Swap <jv>swapAnnotation</jv> = SwapAnnotation
		 * 		.<jsm>create</jsm>()
		 * 		.on(<js>"com.example.MyClass.getValue"</js>)
		 * 		.value(MySwap.<jk>class</jk>)
		 * 		.build();
		 *
		 * 	AnnotationProvider <jv>provider</jv> = AnnotationProvider
		 * 		.<jsm>create</jsm>()
		 * 		.addRuntimeAnnotations(<jv>beanAnnotation</jv>, <jv>swapAnnotation</jv>)  <jc>// Varargs</jc>
		 * 		.build();
		 * </p>
		 *
		 * @param annotations The runtime annotation objects to add (varargs).
		 * @return This object for method chaining.
		 * @throws BeanRuntimeException If any annotation is invalid.
		 * @see #addRuntimeAnnotations(List)
		 */
		public Builder addRuntimeAnnotations(Annotation...annotations) {
			return addRuntimeAnnotations(l(annotations));
		}
	}

	/**
	 * Creates a new {@link Builder} for constructing an annotation provider.
	 *
	 * @return A new builder for configuring the annotation provider.
	 */
	public static Builder create() {
		return new Builder();
	}

	// @formatter:off
	private final Cache<Class<?>,List<AnnotationInfo<Annotation>>> classAnnotations;
	private final Cache<Class<?>,List<AnnotationInfo<Annotation>>> classDeclaredAnnotations;
	private final Cache<Method,List<AnnotationInfo<Annotation>>> methodAnnotations;
	private final Cache<Field,List<AnnotationInfo<Annotation>>> fieldAnnotations;
	private final Cache<Constructor<?>,List<AnnotationInfo<Annotation>>> constructorAnnotations;
	private final ReflectionMap2<Annotation> runtimeAnnotations;
	// @formatter:on

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing configuration settings.
	 */
	protected AnnotationProvider(Builder builder) {
		this.classAnnotations = Cache.<Class<?>,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findClassAnnotations)
			.disableCaching(builder.disableCaching)
			.build();
		this.classDeclaredAnnotations = Cache.<Class<?>,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findClassDeclaredAnnotations)
			.disableCaching(builder.disableCaching)
			.build();
		this.methodAnnotations = Cache.<Method,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findMethodAnnotations)
			.disableCaching(builder.disableCaching)
			.build();
		this.fieldAnnotations = Cache.<Field,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findFieldAnnotations)
			.disableCaching(builder.disableCaching)
			.build();
		this.constructorAnnotations = Cache.<Constructor<?>,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findConstructorAnnotations)
			.disableCaching(builder.disableCaching)
			.build();
		this.runtimeAnnotations = builder.runtimeAnnotations.build();
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Public API
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds all annotations on the specified class, including runtime annotations.
	 *
	 * <p>
	 * Searches the class, its parent classes, interfaces, and package for annotations.
	 * Returns annotations in <b>child-to-parent</b> order with runtime annotations
	 * taking precedence at each level.
	 *
	 * <p>
	 * <b>Order of precedence</b> (see class javadocs for details):
	 * <ol>
	 * 	<li>Runtime + declared annotations on this class
	 * 	<li>Runtime + declared annotations on parent classes (child-to-parent)
	 * 	<li>Runtime + declared annotations on interfaces (child-to-parent)
	 * 	<li>Declared annotations on the package
	 * </ol>
	 *
	 * <p>
	 * <b>Comparison with {@link ClassInfo#getAnnotationInfos(Class)}:</b>
	 * <ul>
	 * 	<li>This method includes <b>runtime annotations</b>; ClassInfo does not
	 * 	<li>Same traversal order (child-to-parent with interfaces and package)
	 * 	<li>Runtime annotations are inserted with higher priority at each level
	 * </ul>
	 *
	 * @param onClass The class to search on.
	 * @return A list of {@link AnnotationInfo} objects representing all annotations on the specified class,
	 * 	its parents, interfaces, and package. Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<Annotation>> find(Class<?> onClass) {
		assertArgNotNull("onClass", onClass);
		return classAnnotations.get(onClass);
	}

	/**
	 * Finds all annotations of the specified type on the specified class, including runtime annotations.
	 *
	 * <p>
	 * Searches the class, its parent classes, interfaces, and package for annotations of the specified type.
	 * Returns annotations in <b>child-to-parent</b> order with runtime annotations
	 * taking precedence at each level.
	 *
	 * <p>
	 * This is a filtered version of {@link #find(Class)} that only returns annotations matching the specified type.
	 *
	 * <p>
	 * <b>Comparison with {@link ClassInfo#getAnnotationInfos(Class)}:</b>
	 * <ul>
	 * 	<li>This method includes <b>runtime annotations</b>; ClassInfo does not
	 * 	<li>Same traversal order (child-to-parent with interfaces and package)
	 * 	<li>Runtime annotations are inserted with higher priority at each level
	 * </ul>
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @return A stream of {@link AnnotationInfo} objects representing annotations of the specified type on the
	 * 	specified class, its parents, interfaces, and package. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, Class<?> onClass) {
		assertArgNotNull("type", type);
		assertArgNotNull("onClass", onClass);
		return find(onClass).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	/**
	 * Finds annotations declared directly on the specified class, including runtime annotations.
	 *
	 * <p>
	 * Unlike {@link #find(Class)}, this method only returns annotations declared directly on the specified class,
	 * not on its parents, interfaces, or package.
	 *
	 * <p>
	 * <b>Order of precedence</b>:
	 * <ol>
	 * 	<li>Runtime annotations on this class (highest priority)
	 * 	<li>Declared annotations on this class
	 * </ol>
	 *
	 * <p>
	 * <b>Comparison with {@link ClassInfo#getDeclaredAnnotationInfos()}:</b>
	 * <ul>
	 * 	<li>This method includes <b>runtime annotations</b>; ClassInfo does not
	 * 	<li>Runtime annotations are returned first (higher priority)
	 * </ul>
	 *
	 * @param onClass The class to search on.
	 * @return A list of {@link AnnotationInfo} objects representing annotations declared directly on the class.
	 * 	Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<Annotation>> findDeclared(Class<?> onClass) {
		assertArgNotNull("onClass", onClass);
		return classDeclaredAnnotations.get(onClass);
	}

	/**
	 * Finds annotations of the specified type declared directly on the specified class, including runtime annotations.
	 *
	 * <p>
	 * This is a filtered version of {@link #findDeclared(Class)} that only returns annotations matching the specified type.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @return A stream of {@link AnnotationInfo} objects representing annotations of the specified type declared
	 * 	directly on the class. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> findDeclared(Class<A> type, Class<?> onClass) {
		assertArgNotNull("type", type);
		assertArgNotNull("onClass", onClass);
		return findDeclared(onClass).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	/**
	 * Finds all declared annotations on the specified class in parent-to-child order (reversed).
	 *
	 * <p>
	 * This method returns annotations in the opposite order from {@link #findDeclared(Class)}.
	 * Returns annotations in <b>parent-to-child</b> order with declared annotations coming before
	 * runtime annotations at each level.
	 *
	 * <p>
	 * <b>Order of precedence</b>:
	 * <ol>
	 * 	<li>Declared annotations on this class (lowest priority)
	 * 	<li>Runtime annotations on this class (highest priority)
	 * </ol>
	 *
	 * <p>
	 * This is useful when you want to process multiple annotation values where runtime annotations
	 * can override values from declared annotations.
	 *
	 * @param onClass The class to search on.
	 * @return A stream of {@link AnnotationInfo} objects in parent-to-child order.
	 */
	public Stream<AnnotationInfo<Annotation>> findDeclaredParentFirst(Class<?> onClass) {
		assertArgNotNull("onClass", onClass);
		var list = classDeclaredAnnotations.get(onClass);
		// Iterate backwards to get parent-to-child order
		return rstream(list);
	}

	/**
	 * Finds all declared annotations of the specified type on the specified class in parent-to-child order (reversed).
	 *
	 * <p>
	 * This method returns annotations in the opposite order from {@link #findDeclared(Class, Class)}.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @return A stream of {@link AnnotationInfo} objects in parent-to-child order.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> findDeclaredParentFirst(Class<A> type, Class<?> onClass) {
		assertArgNotNull("type", type);
		assertArgNotNull("onClass", onClass);
		return findDeclaredParentFirst(onClass)
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	/**
	 * Finds all annotations on the specified method, including runtime annotations.
	 *
	 * <p>
	 * Searches the method and all overridden parent methods for annotations.
	 * Returns annotations in <b>child-to-parent</b> order with runtime annotations
	 * taking precedence at each level.
	 *
	 * <p>
	 * <b>Order of precedence</b> (see class javadocs for details):
	 * <ol>
	 * 	<li>Runtime annotations on this method (highest priority)
	 * 	<li>Declared annotations on this method
	 * 	<li>Runtime annotations on overridden parent methods (child-to-parent)
	 * 	<li>Declared annotations on overridden parent methods (child-to-parent)
	 * </ol>
	 *
	 * <p>
	 * <b>Comparison with {@link MethodInfo#getAnnotationInfos()}:</b>
	 * <ul>
	 * 	<li>This method includes <b>runtime annotations</b>; MethodInfo does not
	 * 	<li>Runtime annotations are inserted with higher priority at each level
	 * </ul>
	 *
	 * @param onMethod The method to search on.
	 * @return A list of {@link AnnotationInfo} objects representing all annotations on the method and
	 * 	overridden parent methods. Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<Annotation>> find(Method onMethod) {
		assertArgNotNull("onMethod", onMethod);
		return methodAnnotations.get(onMethod);
	}

	/**
	 * Finds all annotations of the specified type on the specified method, including runtime annotations.
	 *
	 * <p>
	 * This is a filtered version of {@link #find(Method)} that only returns annotations matching the specified type.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onMethod The method to search on.
	 * @return A stream of {@link AnnotationInfo} objects representing annotations of the specified type on the
	 * 	method and overridden parent methods. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, Method onMethod) {
		assertArgNotNull("type", type);
		assertArgNotNull("onMethod", onMethod);
		return find(onMethod).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	/**
	 * Finds all annotations on the specified field, including runtime annotations.
	 *
	 * <p>
	 * <b>Order of precedence</b>:
	 * <ol>
	 * 	<li>Runtime annotations on this field (highest priority)
	 * 	<li>Declared annotations on this field
	 * </ol>
	 *
	 * <p>
	 * <b>Comparison with {@link FieldInfo#getAnnotationInfos()}:</b>
	 * <ul>
	 * 	<li>This method includes <b>runtime annotations</b>; FieldInfo does not
	 * 	<li>Runtime annotations are returned first (higher priority)
	 * </ul>
	 *
	 * @param onField The field to search on.
	 * @return A list of {@link AnnotationInfo} objects representing all annotations on the field.
	 * 	Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<Annotation>> find(Field onField) {
		assertArgNotNull("onField", onField);
		return fieldAnnotations.get(onField);
	}

	/**
	 * Finds all annotations of the specified type on the specified field, including runtime annotations.
	 *
	 * <p>
	 * This is a filtered version of {@link #find(Field)} that only returns annotations matching the specified type.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onField The field to search on.
	 * @return A stream of {@link AnnotationInfo} objects representing annotations of the specified type on the field.
	 * 	Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, Field onField) {
		assertArgNotNull("type", type);
		assertArgNotNull("onField", onField);
		return find(onField).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	/**
	 * Finds all annotations on the specified constructor, including runtime annotations.
	 *
	 * <p>
	 * <b>Order of precedence</b>:
	 * <ol>
	 * 	<li>Runtime annotations on this constructor (highest priority)
	 * 	<li>Declared annotations on this constructor
	 * </ol>
	 *
	 * <p>
	 * <b>Comparison with {@link ConstructorInfo#getDeclaredAnnotationInfos()}:</b>
	 * <ul>
	 * 	<li>This method includes <b>runtime annotations</b>; ConstructorInfo does not
	 * 	<li>Runtime annotations are returned first (higher priority)
	 * </ul>
	 *
	 * @param onConstructor The constructor to search on.
	 * @return A list of {@link AnnotationInfo} objects representing all annotations on the constructor.
	 * 	Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<Annotation>> find(Constructor<?> onConstructor) {
		assertArgNotNull("onConstructor", onConstructor);
		return constructorAnnotations.get(onConstructor);
	}

	/**
	 * Finds all annotations of the specified type on the specified constructor, including runtime annotations.
	 *
	 * <p>
	 * This is a filtered version of {@link #find(Constructor)} that only returns annotations matching the specified type.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onConstructor The constructor to search on.
	 * @return A stream of {@link AnnotationInfo} objects representing annotations of the specified type on the constructor.
	 * 	Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, Constructor<?> onConstructor) {
		assertArgNotNull("type", type);
		assertArgNotNull("onConstructor", onConstructor);
		return find(onConstructor).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Private implementation
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds all annotations on the specified class in child-to-parent order.
	 *
	 * <p>
	 * Annotations are appended in the following order:
	 * <ol>
	 * 	<li>On this class.
	 * 	<li>On parent classes ordered child-to-parent.
	 * 	<li>On interfaces ordered child-to-parent.
	 * 	<li>On the package of this class.
	 * </ol>
	 *
	 * @param forClass The class to find annotations on.
	 * @return A list of {@link AnnotationInfo} objects in child-to-parent order.
	 */
	private List<AnnotationInfo<Annotation>> findClassAnnotations(Class<?> forClass) {
		var ci = ClassInfo.of(forClass);
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		// On all parent classes and interfaces (properly traversed to avoid duplicates)
		var parentsAndInterfaces = ci.getParentsAndInterfaces();
		for (int i = 0; i < parentsAndInterfaces.size(); i++)
			findDeclaredAnnotations(list, parentsAndInterfaces.get(i).inner());

		// On the package of this class
		var pkg = ci.getPackage();
		if (nn(pkg)) {
			var pi = PackageInfo.of(pkg.inner());
			for (var a : pkg.inner().getAnnotations())
				for (var a2 : splitRepeated(a))
					list.add(AnnotationInfo.of(pi, a2));
		}

		return u(list);
	}

	private List<AnnotationInfo<Annotation>> findClassDeclaredAnnotations(Class<?> forClass) {
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		// On this class
		findDeclaredAnnotations(list, forClass);

		return u(list);
	}

	private List<AnnotationInfo<Annotation>> findMethodAnnotations(Method forMethod) {
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		MethodInfo.of(forMethod).getMatchingMethods().forEach(m -> {
			runtimeAnnotations.findMatching(m.inner()).forEach(a -> list.add(AnnotationInfo.of(m, a)));
			list.addAll(m.getDeclaredAnnotationInfos());
		});

		return u(list);
	}

	private List<AnnotationInfo<Annotation>> findFieldAnnotations(Field forField) {
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		FieldInfo fi = FieldInfo.of(forField);
		runtimeAnnotations.findMatching(forField).forEach(a -> list.add(AnnotationInfo.of(fi, a)));
		list.addAll(fi.getDeclaredAnnotationInfos());

		return u(list);
	}

	private List<AnnotationInfo<Annotation>> findConstructorAnnotations(Constructor<?> forConstructor) {
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		ConstructorInfo ci = ConstructorInfo.of(forConstructor);
		runtimeAnnotations.findMatching(forConstructor).forEach(a -> list.add(AnnotationInfo.of(ci, a)));
		list.addAll(ci.getDeclaredAnnotationInfos());

		return u(list);
	}

	/**
	 * Finds all declared annotations on the specified class and appends them to the list.
	 *
	 * @param appendTo The list to append to.
	 * @param forClass The class to find declared annotations on.
	 */
	private void findDeclaredAnnotations(List<AnnotationInfo<Annotation>> appendTo, Class<?> forClass) {
		var ci = ClassInfo.of(forClass);
		runtimeAnnotations.findMatching(forClass).forEach(x -> appendTo.add(AnnotationInfo.of(ClassInfo.of(forClass), x)));
		for (var a : forClass.getDeclaredAnnotations())
			for (var a2 : splitRepeated(a))
				appendTo.add(AnnotationInfo.of(ci, a2));
	}

	/**
	 * Iterates through annotations on a method, its declaring class hierarchy, and return type hierarchy.
	 *
	 * <p>
	 * This traverses annotations in parent-first order from:
	 * <ol>
	 * 	<li>Declaring class hierarchy (via this AnnotationProvider)
	 * 	<li>Method hierarchy (parent-first, declared annotations only)
	 * 	<li>Return type hierarchy (via this AnnotationProvider)
	 * </ol>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param mi The method info to traverse.
	 * @param filter Optional filter to apply to annotations. Can be <jk>null</jk>.
	 * @param action The action to perform on each matching annotation.
	 */
	public <A extends Annotation> void forEachMethodAnnotation(Class<A> type, MethodInfo mi, Predicate<A> filter, Consumer<A> action) {
		forEachClassAnnotation(type, mi.getDeclaringClass(), filter, action);
		rstream(mi.getMatchingMethods())
			.flatMap(m -> m.getDeclaredAnnotationInfos().stream())
			.map(AnnotationInfo::inner)
			.filter(type::isInstance)
			.map(type::cast)
			.forEach(a -> consumeIf(filter, action, a));
		forEachClassAnnotation(type, mi.getReturnType().unwrap(Value.class, Optional.class), filter, action);
	}

	/**
	 * Iterates through annotations on a class hierarchy.
	 *
	 * <p>
	 * This traverses annotations in parent-first order from:
	 * <ol>
	 * 	<li>Package annotations
	 * 	<li>Interface hierarchy (parent-first)
	 * 	<li>Class hierarchy (parent-first)
	 * </ol>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param ci The class info to traverse.
	 * @param filter Optional filter to apply to annotations. Can be <jk>null</jk>.
	 * @param action The action to perform on each matching annotation.
	 */
	public <A extends Annotation> void forEachClassAnnotation(Class<A> type, ClassInfo ci, Predicate<A> filter, Consumer<A> action) {
		A t2 = ci.getPackageAnnotation(type);
		if (nn(t2))
			consumeIf(filter, action, t2);
		var interfaces2 = ci.getInterfaces();
		for (int i = interfaces2.size() - 1; i >= 0; i--)
			findDeclaredParentFirst(type, interfaces2.get(i).inner()).map(x -> x.inner()).filter(x -> filter == null || filter.test(x)).forEach(x -> action.accept(x));
		var parents2 = ci.getParents();
		for (int i = parents2.size() - 1; i >= 0; i--)
			findDeclaredParentFirst(type, parents2.get(i).inner()).map(x -> x.inner()).filter(x -> filter == null || filter.test(x)).forEach(x -> action.accept(x));
	}
}