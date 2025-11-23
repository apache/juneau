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

import static org.apache.juneau.common.reflect.AnnotationTraversal.*;
import static org.apache.juneau.common.reflect.ReflectionUtils.*;
import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

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
 * <h6 class='topic'>For Classes ({@link #xfind(Class)}):</h6>
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
 * <h6 class='topic'>For Methods ({@link #xfind(Method)}):</h6>
 * <p>
 * Annotations are returned in <b>child-to-parent</b> order with the following precedence:
 * <ol>
 * 	<li><b>Runtime annotations</b> on the method (highest priority)
 * 	<li><b>Declared annotations</b> on the method
 * 	<li><b>Runtime annotations</b> on overridden parent methods (child-to-parent order)
 * 	<li><b>Declared annotations</b> on overridden parent methods (child-to-parent order)
 * </ol>
 *
 * <h6 class='topic'>For Fields ({@link #xfind(Field)}):</h6>
 * <p>
 * Annotations are returned with the following precedence:
 * <ol>
 * 	<li><b>Runtime annotations</b> on the field (highest priority)
 * 	<li><b>Declared annotations</b> on the field
 * </ol>
 *
 * <h6 class='topic'>For Constructors ({@link #xfind(Constructor)}):</h6>
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
 * The methods in this class differ from {@link ClassInfo#getAnnotations()}, {@link MethodInfo#getAnnotations()}, etc.:
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
@SuppressWarnings({ "unchecked", "rawtypes" })
public class AnnotationProvider {

	//-----------------------------------------------------------------------------------------------------------------
	// System properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Caching mode for annotation lookups.
	 *
	 * <p>
	 * System property: <c>juneau.annotationProvider.caching</c>
	 * <br>Valid values: <c>NONE</c>, <c>WEAK</c>, <c>FULL</c> (case-insensitive)
	 * <br>Default: <c>FULL</c>
	 *
	 * <ul>
	 * 	<li><c>NONE</c> - Disables all caching (always recompute)
	 * 	<li><c>WEAK</c> - Uses WeakHashMap (allows GC of cached entries)
	 * 	<li><c>FULL</c> - Uses ConcurrentHashMap (best performance)
	 * </ul>
	 */
	private static final CacheMode CACHING_MODE = CacheMode.parse(System.getProperty("juneau.annotationProvider.caching", "FULL"));

	/**
	 * Enable logging of cache statistics on JVM shutdown.
	 *
	 * <p>
	 * System property: <c>juneau.annotationProvider.caching.logOnExit</c>
	 * <br>Valid values: <c>TRUE</c>, <c>FALSE</c> (case-insensitive)
	 * <br>Default: <c>FALSE</c>
	 */
	private static final boolean LOG_ON_EXIT = b(System.getProperty("juneau.annotationProvider.caching.logOnExit"));

	/**
	 * Default instance.
	 */
	public static final AnnotationProvider INSTANCE = new AnnotationProvider(create().logOnExit());

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
		CacheMode cacheMode;
		boolean logOnExit;
		ReflectionMap.Builder<Annotation> runtimeAnnotations;

		Builder() {
			cacheMode = CACHING_MODE;
			logOnExit = LOG_ON_EXIT;
		}

		/**
		 * Builds a new {@link AnnotationProvider} instance with the configured settings.
		 *
		 * @return A new immutable {@link AnnotationProvider} instance.
		 */
		public AnnotationProvider build() {
			if ((runtimeAnnotations == null || runtimeAnnotations.isEmpty()) && INSTANCE != null)
				return INSTANCE;
			return new AnnotationProvider(this);
		}

		/**
		 * Sets the caching mode for annotation lookups.
		 *
		 * <p>
		 * Available modes:
		 * <ul>
		 * 	<li><c>NONE</c> - Disables all caching (always recompute)
		 * 	<li><c>WEAK</c> - Uses WeakHashMap (allows GC of cached entries)
		 * 	<li><c>FULL</c> - Uses ConcurrentHashMap (best performance)
		 * </ul>
		 *
		 * @param value The caching mode.
		 * @return This object for method chaining.
		 */
		public Builder cacheMode(CacheMode value) {
			cacheMode = value;
			return this;
		}

		/**
		 * Enables logging of cache statistics on JVM shutdown.
		 *
		 * @return This object for method chaining.
		 */
		public Builder logOnExit() {
			logOnExit = true;
			return this;
		}

		/**
		 * Conditionally enables logging of cache statistics on JVM shutdown.
		 *
		 * @param value Whether to log on exit.
		 * @return This object for method chaining.
		 */
		public Builder logOnExit(boolean value) {
			logOnExit = value;
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
			if (runtimeAnnotations == null)
				runtimeAnnotations = ReflectionMap.create(Annotation.class);

			for (var a : annotations) {
				try {
					var ci = ClassInfo.of(a.getClass());

					ci.getPublicMethod(x -> x.hasName("onClass")).ifPresent(mi -> {
						if (! mi.getReturnType().is(Class[].class))
							throw bex("Invalid annotation @{0} used in runtime annotations.  Annotation must define an onClass() method that returns a Class array.", scn(a));
						for (var c : (Class<?>[])mi.accessible().invoke(a))
							runtimeAnnotations.append(c.getName(), a);
					});

					ci.getPublicMethod(x -> x.hasName("on")).ifPresent(mi -> {
						if (! mi.getReturnType().is(String[].class))
							throw bex("Invalid annotation @{0} used in runtime annotations.  Annotation must define an on() method that returns a String array.", scn(a));
						for (var s : (String[])mi.accessible().invoke(a))
							runtimeAnnotations.append(s, a);
					});

				} catch (BeanRuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw bex(e, (Class<?>)null, "Invalid annotation @{0} used in runtime annotations.", cn(a));
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

	private final Cache<Object,List<AnnotationInfo<Annotation>>> runtimeCache;
	private final Cache3<Class<?>,ElementInfo,AnnotationTraversal[],List> cache;
	private final ReflectionMap<Annotation> annotationMap;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing configuration settings.
	 */
	protected AnnotationProvider(Builder builder) {
		// @formatter:off
		this.runtimeCache = Cache.<Object,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::load)
			.cacheMode(builder.cacheMode)
			.logOnExit(builder.logOnExit, "AnnotationProvider.runtimeAnnotations")
			.build();

		this.cache = Cache3.<Class<?>,ElementInfo,AnnotationTraversal[],List>create()
			.supplier(this::load)
			.cacheMode(builder.cacheMode)
			.logOnExit(builder.logOnExit, "AnnotationProvider.cache")
			.build();

		this.annotationMap = opt(builder.runtimeAnnotations).map(x -> x.build()).orElse(null);
		// @formatter:on
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Stream-based traversal methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds annotations from a class using configurable traversal options.
	 *
	 * <p>
	 * This method provides a flexible API for traversing annotations.
	 * It uses {@link AnnotationTraversal} enums to configure what elements to search and in what order.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Search class only</jc>
	 * 	List&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>l1</jv> =
	 * 		find(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, SELF);
	 *
	 * 	<jc>// Search class and parents</jc>
	 * 	List&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>l2</jv> =
	 * 		find(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, SELF, PARENTS);
	 *
	 * 	<jc>// Search class, parents, and package (parent-first order using rstream)</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s3</jv> =
	 * 		rstream(find(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, SELF, PARENTS, PACKAGE));
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param c The class to search.
	 * @param traversals The traversal options (what to search and order).
	 * @return A list of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public <A extends Annotation> List<AnnotationInfo<A>> find(Class<A> type, ClassInfo c, AnnotationTraversal...traversals) {
		assertArgNotNull("type", type);
		assertArgNotNull("c", c);
		return cache.get(type, c, traversals);
	}

	/**
	 * Finds all annotations from a class using configurable traversal options, without filtering by annotation type.
	 *
	 * <p>
	 * This method provides a flexible API for traversing all class annotations.
	 * Unlike {@link #find(Class, ClassInfo, AnnotationTraversal...)}, this method does not filter by annotation type.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all annotations from class only</jc>
	 * 	List&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>l1</jv> =
	 * 		find(<jv>ci</jv>, SELF);
	 *
	 * 	<jc>// Get all annotations from class and parents</jc>
	 * 	List&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>l2</jv> =
	 * 		find(<jv>ci</jv>, SELF, PARENTS);
	 * </p>
	 *
	 * @param c The class to search.
	 * @param traversals The traversal options (what to search and order).
	 * @return A list of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<? extends Annotation>> find(ClassInfo c, AnnotationTraversal...traversals) {
		assertArgNotNull("c", c);
		return cache.get(null, c, traversals);
	}

	/**
	 * Checks if a class has the specified annotation.
	 *
	 * <p>
	 * This is a convenience method equivalent to:
	 * <p class='bjava'>
	 * 	find(<jv>type</jv>, <jv>clazz</jv>, <jv>traversals</jv>).findFirst().isPresent()
	 * </p>
	 *
	 * <h5 class='section'>Supported Traversal Types:</h5>
	 * <ul>
	 * 	<li>{@link AnnotationTraversal#SELF SELF} - Annotations declared directly on this class
	 * 	<li>{@link AnnotationTraversal#PARENTS PARENTS} - Parent classes and interfaces (child-to-parent order)
	 * 	<li>{@link AnnotationTraversal#PACKAGE PACKAGE} - The package annotations
	 * </ul>
	 *
	 * <p>
	 * <b>Default:</b> If no traversals are specified, defaults to: {@code PARENTS, PACKAGE}
	 * <br>Note: {@code PARENTS} includes the class itself plus all parent classes and interfaces.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if class has @MyAnnotation anywhere in hierarchy</jc>
	 * 	<jk>boolean</jk> <jv>hasIt</jv> = has(MyAnnotation.<jk>class</jk>, <jv>ci</jv>);
	 *
	 * 	<jc>// Check only on the class itself</jc>
	 * 	<jk>boolean</jk> <jv>hasIt2</jv> = has(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, SELF);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param c The class to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, PARENTS, PACKAGE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#PARENTS PARENTS}, {@link AnnotationTraversal#PACKAGE PACKAGE}
	 * @return <jk>true</jk> if the annotation is found, <jk>false</jk> otherwise.
	 */
	public <A extends Annotation> boolean has(Class<A> type, ClassInfo c, AnnotationTraversal...traversals) {
		return ! find(type, c, traversals).isEmpty();
	}

	/**
	 * Finds annotations from a method using configurable traversal options.
	 *
	 * <p>
	 * This method provides a flexible API for traversing method annotations.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Search method and matching parent methods</jc>
	 * 	List&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>l1</jv> =
	 * 		find(MyAnnotation.<jk>class</jk>, <jv>mi</jv>, SELF, MATCHING_METHODS);
	 *
	 * 	<jc>// Search method, matching methods, and return type (parent-first using rstream)</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s2</jv> =
	 * 		rstream(find(MyAnnotation.<jk>class</jk>, <jv>mi</jv>, SELF, MATCHING_METHODS, RETURN_TYPE));
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param m The method to search.
	 * @param traversals The traversal options.
	 * @return A list of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public <A extends Annotation> List<AnnotationInfo<A>> find(Class<A> type, MethodInfo m, AnnotationTraversal...traversals) {
		assertArgNotNull("type", type);
		assertArgNotNull("m", m);
		return cache.get(type, m, traversals);
	}

	/**
	 * Finds all annotations from a method using configurable traversal options, without filtering by annotation type.
	 *
	 * <p>
	 * This method provides a flexible API for traversing all method annotations.
	 * Unlike {@link #find(Class, MethodInfo, AnnotationTraversal...)}, this method does not filter by annotation type.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all annotations from method and matching parent methods</jc>
	 * 	List&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>l1</jv> =
	 * 		find(<jv>mi</jv>, SELF, MATCHING_METHODS);
	 *
	 * 	<jc>// Get all annotations from method, matching methods, and return type</jc>
	 * 	List&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>l2</jv> =
	 * 		find(<jv>mi</jv>, SELF, MATCHING_METHODS, RETURN_TYPE);
	 * </p>
	 *
	 * @param m The method to search.
	 * @param traversals The traversal options.
	 * @return A list of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<? extends Annotation>> find(MethodInfo m, AnnotationTraversal...traversals) {
		assertArgNotNull("m", m);
		return cache.get(null, m, traversals);
	}

	/**
	 * Checks if a method has the specified annotation.
	 *
	 * <p>
	 * This is a convenience method equivalent to:
	 * <p class='bjava'>
	 * 	find(<jv>type</jv>, <jv>method</jv>, <jv>traversals</jv>).findFirst().isPresent()
	 * </p>
	 *
	 * <h5 class='section'>Supported Traversal Types:</h5>
	 * <ul>
	 * 	<li>{@link AnnotationTraversal#SELF SELF} - Annotations declared directly on this method
	 * 	<li>{@link AnnotationTraversal#MATCHING_METHODS MATCHING_METHODS} - Matching methods in parent classes (child-to-parent)
	 * 	<li>{@link AnnotationTraversal#RETURN_TYPE RETURN_TYPE} - The return type hierarchy (includes class parents and package)
	 * 	<li>{@link AnnotationTraversal#PACKAGE PACKAGE} - The declaring class's package annotations
	 * </ul>
	 *
	 * <p>
	 * <b>Default:</b> If no traversals are specified, defaults to: {@code SELF, MATCHING_METHODS, RETURN_TYPE, PACKAGE}
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if method has @MyAnnotation anywhere in hierarchy</jc>
	 * 	<jk>boolean</jk> <jv>hasIt</jv> = has(MyAnnotation.<jk>class</jk>, <jv>mi</jv>);
	 *
	 * 	<jc>// Check only on the method itself</jc>
	 * 	<jk>boolean</jk> <jv>hasIt2</jv> = has(MyAnnotation.<jk>class</jk>, <jv>mi</jv>, SELF);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param m The method to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, MATCHING_METHODS, RETURN_TYPE, PACKAGE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#MATCHING_METHODS MATCHING_METHODS}, {@link AnnotationTraversal#RETURN_TYPE RETURN_TYPE}, {@link AnnotationTraversal#PACKAGE PACKAGE}
	 * @return <jk>true</jk> if the annotation is found, <jk>false</jk> otherwise.
	 */
	public <A extends Annotation> boolean has(Class<A> type, MethodInfo m, AnnotationTraversal...traversals) {
		return ! find(type, m, traversals).isEmpty();
	}

	/**
	 * Finds annotations from a parameter using configurable traversal options in child-to-parent order.
	 *
	 * <p>
	 * This method provides a flexible, stream-based API for traversing parameter annotations without creating intermediate lists.
	 *
	 * <h5 class='section'>Supported Traversal Types:</h5>
	 * <ul>
	 * 	<li>{@link AnnotationTraversal#SELF SELF} - Annotations declared directly on this parameter
	 * 	<li>{@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS} - Matching parameters in parent methods/constructors (child-to-parent)
	 * 	<li>{@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE} - The parameter's type hierarchy (includes class parents and package)
	 * </ul>
	 *
	 * <p>
	 * <b>Default:</b> If no traversals are specified, defaults to: {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Search parameter, matching parameters, and parameter type (child-to-parent)</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s1</jv> =
	 * 		find(MyAnnotation.<jk>class</jk>, <jv>pi</jv>, SELF, MATCHING_PARAMETERS, PARAMETER_TYPE);
	 *
	 * 	<jc>// Just search this parameter</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s2</jv> =
	 * 		find(MyAnnotation.<jk>class</jk>, <jv>pi</jv>, SELF);
	 *
	 * 	<jc>// Search in parent-to-child order using findTopDown</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s3</jv> =
	 * 		findTopDown(MyAnnotation.<jk>class</jk>, <jv>pi</jv>, SELF, MATCHING_PARAMETERS, PARAMETER_TYPE);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param p The parameter to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS}, {@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE}
	 * @return A list of {@link AnnotationInfo} objects in child-to-parent order. Never <jk>null</jk>.
	 */
	public <A extends Annotation> List<AnnotationInfo<A>> find(Class<A> type, ParameterInfo p, AnnotationTraversal...traversals) {
		assertArgNotNull("type", type);
		assertArgNotNull("p", p);
		return cache.get(type, p, traversals);
	}

	/**
	 * Finds all annotations from a parameter using configurable traversal options, without filtering by annotation type.
	 *
	 * <p>
	 * This method provides a flexible, stream-based API for traversing all parameter annotations without creating intermediate lists.
	 * Unlike {@link #find(Class, ParameterInfo, AnnotationTraversal...)}, this method does not filter by annotation type.
	 *
	 * <h5 class='section'>Supported Traversal Types:</h5>
	 * <ul>
	 * 	<li>{@link AnnotationTraversal#SELF SELF} - Annotations declared directly on this parameter
	 * 	<li>{@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS} - Matching parameters in parent methods/constructors (child-to-parent)
	 * 	<li>{@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE} - The parameter's type hierarchy (includes class parents and package)
	 * </ul>
	 *
	 * <p>
	 * <b>Default:</b> If no traversals are specified, defaults to: {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all annotations from parameter, matching parameters, and parameter type</jc>
	 * 	Stream&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>s1</jv> =
	 * 		find(<jv>pi</jv>, SELF, MATCHING_PARAMETERS, PARAMETER_TYPE);
	 *
	 * 	<jc>// Just get annotations from this parameter</jc>
	 * 	Stream&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>s2</jv> =
	 * 		find(<jv>pi</jv>, SELF);
	 * </p>
	 *
	 * @param p The parameter to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS}, {@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE}
	 * @return A list of {@link AnnotationInfo} objects in child-to-parent order. Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<? extends Annotation>> find(ParameterInfo p, AnnotationTraversal...traversals) {
		assertArgNotNull("p", p);
		return cache.get(null, p, traversals);
	}

	/**
	 * Checks if a parameter has the specified annotation.
	 *
	 * <p>
	 * This is a convenience method equivalent to:
	 * <p class='bjava'>
	 * 	find(<jv>type</jv>, <jv>parameter</jv>, <jv>traversals</jv>).findFirst().isPresent()
	 * </p>
	 *
	 * <h5 class='section'>Supported Traversal Types:</h5>
	 * <ul>
	 * 	<li>{@link AnnotationTraversal#SELF SELF} - Annotations declared directly on this parameter
	 * 	<li>{@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS} - Matching parameters in parent methods/constructors (child-to-parent)
	 * 	<li>{@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE} - The parameter's type hierarchy (includes class parents and package)
	 * </ul>
	 *
	 * <p>
	 * <b>Default:</b> If no traversals are specified, defaults to: {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if parameter has @MyAnnotation anywhere in hierarchy</jc>
	 * 	<jk>boolean</jk> <jv>hasIt</jv> = has(MyAnnotation.<jk>class</jk>, <jv>pi</jv>);
	 *
	 * 	<jc>// Check only on the parameter itself</jc>
	 * 	<jk>boolean</jk> <jv>hasIt2</jv> = has(MyAnnotation.<jk>class</jk>, <jv>pi</jv>, SELF);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param p The parameter to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS}, {@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE}
	 * @return <jk>true</jk> if the annotation is found, <jk>false</jk> otherwise.
	 */
	public <A extends Annotation> boolean has(Class<A> type, ParameterInfo p, AnnotationTraversal...traversals) {
		return ! find(type, p, traversals).isEmpty();
	}

	/**
	 * Finds annotations from a field using configurable traversal options.
	 *
	 * <p>
	 * This method provides a flexible, stream-based API for traversing field annotations without creating intermediate lists.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Search field annotations</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s1</jv> =
	 * 		findAnnotations(MyAnnotation.<jk>class</jk>, <jv>fi</jv>, SELF);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param f The field to search.
	 * @param traversals The traversal options.
	 * @return A list of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public <A extends Annotation> List<AnnotationInfo<A>> find(Class<A> type, FieldInfo f, AnnotationTraversal...traversals) {
		assertArgNotNull("type", type);
		assertArgNotNull("f", f);
		return cache.get(type, f, traversals);
	}

	/**
	 * Finds all annotations from a field using configurable traversal options, without filtering by annotation type.
	 *
	 * <p>
	 * This method provides a flexible, stream-based API for traversing all field annotations without creating intermediate lists.
	 * Unlike {@link #find(Class, FieldInfo, AnnotationTraversal...)}, this method does not filter by annotation type.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all annotations from field</jc>
	 * 	Stream&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>s1</jv> =
	 * 		find(<jv>fi</jv>, SELF);
	 * </p>
	 *
	 * @param f The field to search.
	 * @param traversals The traversal options.
	 * @return A list of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<? extends Annotation>> find(FieldInfo f, AnnotationTraversal...traversals) {
		assertArgNotNull("f", f);
		return cache.get(null, f, traversals);
	}

	/**
	 * Checks if a field has the specified annotation.
	 *
	 * <p>
	 * This is a convenience method equivalent to:
	 * <p class='bjava'>
	 * 	find(<jv>type</jv>, <jv>field</jv>, <jv>traversals</jv>).findFirst().isPresent()
	 * </p>
	 *
	 * <h5 class='section'>Supported Traversal Types:</h5>
	 * <ul>
	 * 	<li>{@link AnnotationTraversal#SELF SELF} - Annotations declared directly on this field
	 * </ul>
	 *
	 * <p>
	 * <b>Default:</b> If no traversals are specified, defaults to: {@code SELF}
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if field has @MyAnnotation</jc>
	 * 	<jk>boolean</jk> <jv>hasIt</jv> = has(MyAnnotation.<jk>class</jk>, <jv>fi</jv>);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param f The field to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}
	 * @return <jk>true</jk> if the annotation is found, <jk>false</jk> otherwise.
	 */
	public <A extends Annotation> boolean has(Class<A> type, FieldInfo f, AnnotationTraversal...traversals) {
		return ! find(type, f, traversals).isEmpty();
	}

	/**
	 * Finds annotations from a constructor using configurable traversal options.
	 *
	 * <p>
	 * This method provides a flexible, stream-based API for traversing constructor annotations without creating intermediate lists.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Search constructor annotations</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s1</jv> =
	 * 		findAnnotations(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, SELF);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param c The constructor to search.
	 * @param traversals The traversal options.
	 * @return A list of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public <A extends Annotation> List<AnnotationInfo<A>> find(Class<A> type, ConstructorInfo c, AnnotationTraversal...traversals) {
		assertArgNotNull("type", type);
		assertArgNotNull("c", c);
		return cache.get(type, c, traversals);
	}

	/**
	 * Finds all annotations from a constructor using configurable traversal options, without filtering by annotation type.
	 *
	 * <p>
	 * This method provides a flexible, stream-based API for traversing all constructor annotations without creating intermediate lists.
	 * Unlike {@link #find(Class, ConstructorInfo, AnnotationTraversal...)}, this method does not filter by annotation type.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all annotations from constructor</jc>
	 * 	Stream&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>s1</jv> =
	 * 		find(<jv>ci</jv>, SELF);
	 * </p>
	 *
	 * @param c The constructor to search.
	 * @param traversals The traversal options.
	 * @return A list of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<? extends Annotation>> find(ConstructorInfo c, AnnotationTraversal...traversals) {
		assertArgNotNull("c", c);
		return cache.get(null, c, traversals);
	}

	/**
	 * Checks if a constructor has the specified annotation.
	 *
	 * <p>
	 * This is a convenience method equivalent to:
	 * <p class='bjava'>
	 * 	find(<jv>type</jv>, <jv>constructor</jv>, <jv>traversals</jv>).findFirst().isPresent()
	 * </p>
	 *
	 * <h5 class='section'>Supported Traversal Types:</h5>
	 * <ul>
	 * 	<li>{@link AnnotationTraversal#SELF SELF} - Annotations declared directly on this constructor
	 * </ul>
	 *
	 * <p>
	 * <b>Default:</b> If no traversals are specified, defaults to: {@code SELF}
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if constructor has @MyAnnotation</jc>
	 * 	<jk>boolean</jk> <jv>hasIt</jv> = has(MyAnnotation.<jk>class</jk>, <jv>ci</jv>);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param c The constructor to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}
	 * @return <jk>true</jk> if the annotation is found, <jk>false</jk> otherwise.
	 */
	public <A extends Annotation> boolean has(Class<A> type, ConstructorInfo c, AnnotationTraversal...traversals) {
		return ! find(type, c, traversals).isEmpty();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private List load(Object o) {
		if (o instanceof Class o2) {
			var ci = ClassInfo.of(o2);
			return annotationMap == null ? liste() : annotationMap.find(ci.inner()).map(a -> ai(ci, a)).toList();
		}
		if (o instanceof Method o2) {
			var mi = info(o2);
			return annotationMap == null ? liste() : annotationMap.find(mi.inner()).map(a -> ai(mi, a)).toList();
		}
		if (o instanceof Field o2) {
			var fi = info(o2);
			return annotationMap == null ? liste() : annotationMap.find(fi.inner()).map(a -> ai(fi, a)).toList();
		}
		if (o instanceof Constructor o2) {
			var ci = info(o2);
			return annotationMap == null ? liste() : annotationMap.find(ci.inner()).map(a -> ai(ci, a)).toList();
		}
		throw unsupportedOp();
	}

	/**
	 * Computes and caches the complete list of annotations for a given type, class, and traversal combination.
	 * This is the supplier function for the findCache.
	 */
	private List load(Class<?> type, ElementInfo element, AnnotationTraversal[] traversals) {

		if (type != null) {
			return cache.get(null, element, traversals).stream().filter(x -> ((AnnotationInfo)x).isType(type)).toList();
		}

		var l = new ArrayList();

		List<AnnotationTraversal> t;
		if (traversals.length > 0)
			t = l(traversals);
		else if (element instanceof ClassInfo)
			t = l(a(PARENTS, PACKAGE));
		else if (element instanceof MethodInfo)
			t = l(a(SELF, MATCHING_METHODS, DECLARING_CLASS, RETURN_TYPE, PACKAGE));
		else if (element instanceof FieldInfo || element instanceof ConstructorInfo)
			t = l(a(SELF));
		else if (element instanceof ParameterInfo)
			t = l(a(SELF, MATCHING_PARAMETERS, PARAMETER_TYPE));
		else
			t = l();  // Never happens.

		if (element instanceof ClassInfo element2) {
			if (t.contains(SELF)) {
				l.addAll(runtimeCache.get(element2.inner()));
				l.addAll(element2.getDeclaredAnnotations());
			}
			if (t.contains(PARENTS)) {
				for (var p : element2.getParentsAndInterfaces()) {
					l.addAll(runtimeCache.get(p.inner()));
					l.addAll(p.getDeclaredAnnotations());
				}
			}
			if (t.contains(PACKAGE)) {
				if (element2.getPackage() != null)
					l.addAll(element2.getPackage().getAnnotations());
			}
		} else if (element instanceof MethodInfo element3) {
			if (t.contains(SELF)) {
				l.addAll(runtimeCache.get(element3.inner()));
				l.addAll(element3.getDeclaredAnnotations());
			}
			if (t.contains(MATCHING_METHODS)) {
				for (var m : element3.getMatchingMethods().stream().skip(1).toList()) {
					l.addAll(runtimeCache.get(m.inner()));
					l.addAll(m.getDeclaredAnnotations());
				}
			}
			if (t.contains(DECLARING_CLASS)) {
				l.addAll(find(element3.getDeclaringClass(), a(PARENTS)));
			}
			if (t.contains(RETURN_TYPE)) {
				l.addAll(find(element3.getReturnType().unwrap(Value.class, Optional.class), a(PARENTS)));
			}
			if (t.contains(PACKAGE)) {
				if (element3.getDeclaringClass().getPackage() != null)
					l.addAll(element3.getDeclaringClass().getPackage().getAnnotations());
			}
		} else if (element instanceof FieldInfo element4) {
			if (t.contains(SELF)) {
				l.addAll(runtimeCache.get(element4.inner()));
				l.addAll(element4.getAnnotations());
			}
		} else if (element instanceof ConstructorInfo element5) {
			if (t.contains(SELF)) {
				l.addAll(runtimeCache.get(element5.inner()));
				l.addAll(element5.getDeclaredAnnotations());
			}
		} else if (element instanceof ParameterInfo element6) {
			if (t.contains(SELF)) {
				l.addAll(element6.getAnnotations());
			}
			if (t.contains(MATCHING_PARAMETERS)) {
				for (var p : element6.getMatchingParameters().stream().skip(1).toList()) {
					l.addAll(p.getAnnotations());
				}
			}
			if (t.contains(PARAMETER_TYPE)) {
				l.addAll(find(element6.getParameterType().unwrap(Value.class, Optional.class), a(PARENTS, PACKAGE)));
			}
		}

		return l;
	}

	private static <A extends Annotation> AnnotationInfo<A> ai(Annotatable on, A value) {
		return AnnotationInfo.of(on, value);
	}
}