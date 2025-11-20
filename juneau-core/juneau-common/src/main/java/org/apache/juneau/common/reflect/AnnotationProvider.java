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
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.common.reflect.AnnotationTraversal.*;
import static java.util.stream.Stream.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
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
	// Performance instrumentation
	//-----------------------------------------------------------------------------------------------------------------

	private static final Map<String, Long> methodCallCounts = new java.util.concurrent.ConcurrentHashMap<>();
	private static final boolean ENABLE_INSTRUMENTATION = Boolean.getBoolean("juneau.instrumentAnnotationProvider");
	private static final boolean ENABLE_NEW_CODE = Boolean.getBoolean("juneau.annotationProvider.enableNewCode");

	static {
		if (ENABLE_INSTRUMENTATION) {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("/tmp/annotation-provider-stats.txt"));
					pw.println("\n=== AnnotationProvider Method Call Statistics ===");
					pw.println(String.format("%-65s %12s", "Method", "Calls"));
					pw.println("=".repeat(80));
					
					methodCallCounts.entrySet().stream()
						.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
						.forEach(e -> pw.println(String.format("%-65s %,12d", e.getKey(), e.getValue())));
					
					long totalCalls = methodCallCounts.values().stream().mapToLong(Long::longValue).sum();
					pw.println("=".repeat(80));
					pw.println(String.format("%-65s %,12d", "TOTAL", totalCalls));
					pw.println("=== Total methods instrumented: " + methodCallCounts.size() + " ===\n");
					pw.close();
					System.err.println("\n=== AnnotationProvider statistics written to /tmp/annotation-provider-stats.txt ===\n");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}));
		}
	}

	private static void trackCall(String methodSignature) {
		if (ENABLE_INSTRUMENTATION) {
			methodCallCounts.merge(methodSignature, 1L, Long::sum);
		}
	}

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
		ReflectionMap.Builder<Annotation> runtimeAnnotations = ReflectionMap.create(Annotation.class);

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

					ci.getPublicMethod(x -> x.hasName("onClass")).ifPresent(mi -> {
						if (! mi.getReturnType().is(Class[].class))
							throw new BeanRuntimeException("Invalid annotation @{0} used in runtime annotations.  Annotation must define an onClass() method that returns a Class array.", scn(a));
						for (var c : (Class<?>[])mi.accessible().invoke(a))
							runtimeAnnotations.append(c.getName(), a);
					});

					ci.getPublicMethod(x -> x.hasName("on")).ifPresent(mi -> {
						if (! mi.getReturnType().is(String[].class))
							throw new BeanRuntimeException("Invalid annotation @{0} used in runtime annotations.  Annotation must define an on() method that returns a String array.", scn(a));
						for (var s : (String[])mi.accessible().invoke(a))
							runtimeAnnotations.append(s, a);
					});

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

	private final Cache<Class<?>,List<AnnotationInfo<Annotation>>> classAnnnotations;
	private final Cache<Method,List<AnnotationInfo<Annotation>>> methodAnnotations;
	private final Cache<Field,List<AnnotationInfo<Annotation>>> fieldAnnotations;
	private final Cache<Constructor<?>,List<AnnotationInfo<Annotation>>> constructorAnnotations;
	private final ReflectionMap<Annotation> annotationMap;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing configuration settings.
	 */
	protected AnnotationProvider(Builder builder) {
		this.classAnnnotations = Cache.<Class<?>,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findClassAnnotations)
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
		this.annotationMap = builder.runtimeAnnotations.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Private implementation
	//-----------------------------------------------------------------------------------------------------------------

	private List<AnnotationInfo<Annotation>> findClassAnnotations(Class<?> forClass) {
		var ci = ClassInfo.of(forClass);
		return annotationMap.find(forClass).map(a -> ai(ci, a)).toList();
	}

	private List<AnnotationInfo<Annotation>> findMethodAnnotations(Method forMethod) {
		var mi = MethodInfo.of(forMethod);
		return annotationMap.find(forMethod).map(a -> ai(mi, a)).toList();
	}

	private List<AnnotationInfo<Annotation>> findFieldAnnotations(Field forField) {
		var fi = FieldInfo.of(forField);
		return annotationMap.find(forField).map(a -> ai(fi, a)).toList();
	}

	private List<AnnotationInfo<Annotation>> findConstructorAnnotations(Constructor<?> forConstructor) {
		var ci = ConstructorInfo.of(forConstructor);
		return annotationMap.find(forConstructor).map(a -> ai(ci, a)).toList();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Stream-based traversal methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Streams annotations from a class using configurable traversal options.
	 *
	 * <p>
	 * This method provides a flexible, stream-based API for traversing annotations without creating intermediate lists.
	 * It uses {@link AnnotationTraversal} enums to configure what elements to search and in what order.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Search class only</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s1</jv> =
	 * 		search(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, SELF);
	 *
	 * 	<jc>// Search class and parents</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s2</jv> =
	 * 		search(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, SELF, PARENTS);
	 *
	 * 	<jc>// Search class, parents, and package (parent-first order using rstream)</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s3</jv> =
	 * 		rstream(search(MyAnnotation.<jk>class</jk>, <jv>ci</jv>, SELF, PARENTS, PACKAGE).toList());
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param clazz The class to search.
	 * @param traversals The traversal options (what to search and order).
	 * @return A stream of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, ClassInfo clazz, AnnotationTraversal... traversals) {
		trackCall("find(Class, ClassInfo, AnnotationTraversal...)");
		if (ENABLE_NEW_CODE)
			return findNew(type, clazz, traversals).stream();
		
		assertArgNotNull("type", type);
		assertArgNotNull("clazz", clazz);
		if (traversals.length == 0)
			traversals = a(PARENTS, PACKAGE);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == SELF) {
					return concat(
						classAnnnotations.get(clazz.inner()).stream(),
						clazz.getDeclaredAnnotations().stream()
					)
					.filter(a -> a.isType(type)).map(a -> (AnnotationInfo<A>)a);
				} else if (traversal == PARENTS) {
					return clazz.getParentsAndInterfaces().stream().flatMap(x ->
						concat(
							classAnnnotations.get(x.inner()).stream(),
							x.getDeclaredAnnotations().stream()
						).filter(a -> a.isType(type)).map(a -> (AnnotationInfo<A>)a)
					);
				} else if (traversal == PACKAGE) {
					return opt(clazz.getPackage()).map(x -> x.getAnnotations().stream().filter(a -> a.isType(type)).map(a -> (AnnotationInfo<A>)a)).orElse(Stream.empty());
				}
				throw illegalArg("Invalid traversal type for class annotations: {0}", traversal);
			});
	}

	/**
	 * New optimized implementation of find(Class, ClassInfo, AnnotationTraversal...).
	 * Returns a List for better performance.
	 * Enable with -Djuneau.annotationProvider.enableNewCode=true
	 */
	@SuppressWarnings("unchecked")
	private <A extends Annotation> List<AnnotationInfo<A>> findNew(Class<A> type, ClassInfo clazz, AnnotationTraversal... traversals) {
		// TODO: Implement optimized version
		throw new UnsupportedOperationException("New implementation not yet available");
	}

	/**
	 * Streams all annotations from a class using configurable traversal options, without filtering by annotation type.
	 *
	 * <p>
	 * This method provides a flexible, stream-based API for traversing all class annotations without creating intermediate lists.
	 * Unlike {@link #find(Class, ClassInfo, AnnotationTraversal...)}, this method does not filter by annotation type.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all annotations from class only</jc>
	 * 	Stream&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>s1</jv> =
	 * 		find(<jv>ci</jv>, SELF);
	 *
	 * 	<jc>// Get all annotations from class and parents</jc>
	 * 	Stream&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>s2</jv> =
	 * 		find(<jv>ci</jv>, SELF, PARENTS);
	 * </p>
	 *
	 * @param clazz The class to search.
	 * @param traversals The traversal options (what to search and order).
	 * @return A stream of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public Stream<AnnotationInfo<? extends Annotation>> find(ClassInfo clazz, AnnotationTraversal... traversals) {
		trackCall("find(ClassInfo, AnnotationTraversal...)");
		assertArgNotNull("clazz", clazz);
		if (traversals.length == 0)
			traversals = a(PARENTS, PACKAGE);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == SELF) {
					return concat(
						classAnnnotations.get(clazz.inner()).stream(),
						clazz.getDeclaredAnnotations().stream().map(a -> (AnnotationInfo<Annotation>)a)
					);
				} else if (traversal == PARENTS) {
					return clazz.getParentsAndInterfaces().stream().flatMap(x -> {
						return concat(
							classAnnnotations.get(x.inner()).stream(),
							x.getDeclaredAnnotations().stream().map(a -> (AnnotationInfo<Annotation>)a)
						);
					});
				} else if (traversal == PACKAGE) {
					return opt(clazz.getPackage()).map(x -> x.getAnnotations().stream()).orElse(Stream.empty());
				}
				throw illegalArg("Invalid traversal type for class annotations: {0}", traversal);
			});
	}

	/**
	 * Streams annotations from a class using configurable traversal options in parent-first order.
	 *
	 * <p>
	 * This is equivalent to calling {@link #find(Class, ClassInfo, AnnotationTraversal...)}
	 * and reversing the result.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param clazz The class to search.
	 * @param traversals The traversal options (what to search and order).
	 * @return A stream of {@link AnnotationInfo} objects in parent-first order. Never <jk>null</jk>.
	 */
	public <A extends Annotation> Stream<AnnotationInfo<A>> findTopDown(Class<A> type, ClassInfo clazz, AnnotationTraversal... traversals) {
		trackCall("findTopDown(Class, ClassInfo, AnnotationTraversal...)");
		return rstream(find(type, clazz, traversals).toList());
	}

	/**
	 * Streams all annotations from a class using configurable traversal options in parent-first order, without filtering by annotation type.
	 *
	 * <p>
	 * This is equivalent to calling {@link #find(ClassInfo, AnnotationTraversal...)}
	 * and reversing the result.
	 *
	 * @param clazz The class to search.
	 * @param traversals The traversal options (what to search and order).
	 * @return A stream of {@link AnnotationInfo} objects in parent-first order. Never <jk>null</jk>.
	 */
	public Stream<AnnotationInfo<? extends Annotation>> findTopDown(ClassInfo clazz, AnnotationTraversal... traversals) {
		trackCall("findTopDown(ClassInfo, AnnotationTraversal...)");
		return rstream(find(clazz, traversals).toList());
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
	 * @param clazz The class to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, PARENTS, PACKAGE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#PARENTS PARENTS}, {@link AnnotationTraversal#PACKAGE PACKAGE}
	 * @return <jk>true</jk> if the annotation is found, <jk>false</jk> otherwise.
	 */
	public <A extends Annotation> boolean has(Class<A> type, ClassInfo clazz, AnnotationTraversal... traversals) {
		trackCall("has(Class, ClassInfo, AnnotationTraversal...)");
		return find(type, clazz, traversals).findFirst().isPresent();
	}

	/**
	 * Streams annotations from a method using configurable traversal options.
	 *
	 * <p>
	 * This method provides a flexible, stream-based API for traversing method annotations without creating intermediate lists.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Search method and matching parent methods</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s1</jv> =
	 * 		findAnnotations(MyAnnotation.<jk>class</jk>, <jv>mi</jv>, SELF, MATCHING_METHODS);
	 *
	 * 	<jc>// Search method, matching methods, and return type (parent-first using findAnnotationsParentFirst)</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s2</jv> =
	 * 		findAnnotationsParentFirst(MyAnnotation.<jk>class</jk>, <jv>mi</jv>, SELF, MATCHING_METHODS, RETURN_TYPE);
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param method The method to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, MethodInfo method, AnnotationTraversal... traversals) {
		trackCall("find(Class, MethodInfo, AnnotationTraversal...)");
		assertArgNotNull("type", type);
		assertArgNotNull("method", method);
		if (traversals.length == 0)
			traversals = a(SELF, MATCHING_METHODS, DECLARING_CLASS, RETURN_TYPE, PACKAGE);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == SELF) {
					return concat(
						methodAnnotations.get(method.inner()).stream(),
						method.getDeclaredAnnotations().stream()
					).filter(a -> a.isType(type)).map(a -> (AnnotationInfo<A>)a);
				} else if (traversal == MATCHING_METHODS) {
					return method.getMatchingMethods().stream().skip(1).flatMap(m ->
						concat(
							methodAnnotations.get(m.inner()).stream(),
							m.getDeclaredAnnotations().stream()
						).filter(a -> a.isType(type)).map(a -> (AnnotationInfo<A>)a)
					);
				} else if (traversal == DECLARING_CLASS) {
					return find(type, method.getDeclaringClass(), PARENTS);
				} else if (traversal == RETURN_TYPE) {
					return find(type, method.getReturnType().unwrap(Value.class, Optional.class), PARENTS);
				} else if (traversal == PACKAGE) {
					return opt(method.getDeclaringClass().getPackage()).map(x -> x.getAnnotations().stream().filter(a -> a.isType(type)).map(a -> (AnnotationInfo<A>)a)).orElse(Stream.empty());
				}
				throw illegalArg("Invalid traversal type for method annotations: {0}", traversal);
			});
	}

	/**
	 * Streams all annotations from a method using configurable traversal options, without filtering by annotation type.
	 *
	 * <p>
	 * This method provides a flexible, stream-based API for traversing all method annotations without creating intermediate lists.
	 * Unlike {@link #find(Class, MethodInfo, AnnotationTraversal...)}, this method does not filter by annotation type.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get all annotations from method and matching parent methods</jc>
	 * 	Stream&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>s1</jv> =
	 * 		find(<jv>mi</jv>, SELF, MATCHING_METHODS);
	 *
	 * 	<jc>// Get all annotations from method, matching methods, and return type</jc>
	 * 	Stream&lt;AnnotationInfo&lt;Annotation&gt;&gt; <jv>s2</jv> =
	 * 		find(<jv>mi</jv>, SELF, MATCHING_METHODS, RETURN_TYPE);
	 * </p>
	 *
	 * @param method The method to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public Stream<AnnotationInfo<? extends Annotation>> find(MethodInfo method, AnnotationTraversal... traversals) {
		trackCall("find(MethodInfo, AnnotationTraversal...)");
		assertArgNotNull("method", method);
		if (traversals.length == 0)
			traversals = a(SELF, MATCHING_METHODS, DECLARING_CLASS, RETURN_TYPE, PACKAGE);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == SELF) {
					return concat(
						methodAnnotations.get(method.inner()).stream(),
						method.getDeclaredAnnotations().stream()
					);
				} else if (traversal == MATCHING_METHODS) {
					return method.getMatchingMethods().stream().skip(1).flatMap(m -> {
						return concat(
							methodAnnotations.get(m.inner()).stream(),
							m.getDeclaredAnnotations().stream()
						);
					});
				} else if (traversal == DECLARING_CLASS) {
					return find(method.getDeclaringClass(), PARENTS);
				} else if (traversal == RETURN_TYPE) {
					return find(method.getReturnType().unwrap(Value.class, Optional.class), PARENTS);
				} else if (traversal == PACKAGE) {
					return opt(method.getDeclaringClass().getPackage()).map(x -> x.getAnnotations().stream()).orElse(Stream.empty());
				}
				throw illegalArg("Invalid traversal type for method annotations: {0}", traversal);
			});
	}

	/**
	 * Streams annotations from a method using configurable traversal options in parent-first order.
	 *
	 * <p>
	 * This is equivalent to calling {@link #find(Class, MethodInfo, AnnotationTraversal...)}
	 * and reversing the result.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param method The method to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects in parent-first order. Never <jk>null</jk>.
	 */
	public <A extends Annotation> Stream<AnnotationInfo<A>> findTopDown(Class<A> type, MethodInfo method, AnnotationTraversal... traversals) {
		trackCall("findTopDown(Class, MethodInfo, AnnotationTraversal...)");
		return rstream(find(type, method, traversals).toList());
	}

	/**
	 * Streams all annotations from a method using configurable traversal options in parent-first order, without filtering by annotation type.
	 *
	 * <p>
	 * This is equivalent to calling {@link #find(MethodInfo, AnnotationTraversal...)}
	 * and reversing the result.
	 *
	 * @param method The method to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects in parent-first order. Never <jk>null</jk>.
	 */
	public Stream<AnnotationInfo<? extends Annotation>> findTopDown(MethodInfo method, AnnotationTraversal... traversals) {
		trackCall("findTopDown(MethodInfo, AnnotationTraversal...)");
		return rstream(find(method, traversals).toList());
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
	 * @param method The method to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, MATCHING_METHODS, RETURN_TYPE, PACKAGE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#MATCHING_METHODS MATCHING_METHODS}, {@link AnnotationTraversal#RETURN_TYPE RETURN_TYPE}, {@link AnnotationTraversal#PACKAGE PACKAGE}
	 * @return <jk>true</jk> if the annotation is found, <jk>false</jk> otherwise.
	 */
	public <A extends Annotation> boolean has(Class<A> type, MethodInfo method, AnnotationTraversal... traversals) {
		trackCall("has(Class, MethodInfo, AnnotationTraversal...)");
		return find(type, method, traversals).findFirst().isPresent();
	}

	/**
	 * Streams annotations from a parameter using configurable traversal options in child-to-parent order.
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
	 * @param parameter The parameter to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS}, {@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE}
	 * @return A stream of {@link AnnotationInfo} objects in child-to-parent order. Never <jk>null</jk>.
	 */
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, ParameterInfo parameter, AnnotationTraversal... traversals) {
		trackCall("find(Class, ParameterInfo, AnnotationTraversal...)");
		assertArgNotNull("type", type);
		assertArgNotNull("parameter", parameter);
		if (traversals.length == 0)
			traversals = a(SELF, MATCHING_PARAMETERS, PARAMETER_TYPE);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == SELF) {
					return parameter.getAnnotations(type);
				} else if (traversal == MATCHING_PARAMETERS) {
					return parameter.getMatchingParameters().stream().skip(1).flatMap(x -> x.getAnnotations(type));
				} else if (traversal == PARAMETER_TYPE) {
					return find(type, parameter.getParameterType().unwrap(Value.class, Optional.class), PARENTS, PACKAGE);
				}
				throw illegalArg("Invalid traversal type for parameter annotations: {0}", traversal);
			});
	}

	/**
	 * Streams annotations from a parameter using configurable traversal options in parent-to-child order.
	 *
	 * <p>
	 * This is equivalent to calling {@link #find(Class, ParameterInfo, AnnotationTraversal...)} and reversing the result.
	 * Use this when you need parent annotations to take precedence over child annotations.
	 *
	 * <h5 class='section'>Supported Traversal Types:</h5>
	 * <ul>
	 * 	<li>{@link AnnotationTraversal#SELF SELF} - Annotations declared directly on this parameter
	 * 	<li>{@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS} - Matching parameters in parent methods/constructors (parent-to-child)
	 * 	<li>{@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE} - The parameter's type hierarchy (includes class parents and package)
	 * </ul>
	 *
	 * <p>
	 * <b>Default:</b> If no traversals are specified, defaults to: {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Search in parent-to-child order</jc>
	 * 	Stream&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>s</jv> =
	 * 		findTopDown(MyAnnotation.<jk>class</jk>, <jv>pi</jv>, SELF, MATCHING_PARAMETERS, PARAMETER_TYPE);
	 *
	 * 	<jc>// Get first annotation (from parent)</jc>
	 * 	Optional&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>first</jv> = <jv>s</jv>.findFirst();
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param parameter The parameter to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS}, {@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE}
	 * @return A stream of {@link AnnotationInfo} objects in parent-to-child order. Never <jk>null</jk>.
	 */
	public <A extends Annotation> Stream<AnnotationInfo<A>> findTopDown(Class<A> type, ParameterInfo parameter, AnnotationTraversal... traversals) {
		trackCall("findTopDown(Class, ParameterInfo, AnnotationTraversal...)");
		return rstream(find(type, parameter, traversals).toList());
	}

	/**
	 * Streams all annotations from a parameter using configurable traversal options, without filtering by annotation type.
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
	 * @param parameter The parameter to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS}, {@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE}
	 * @return A stream of {@link AnnotationInfo} objects in child-to-parent order. Never <jk>null</jk>.
	 */
	public Stream<AnnotationInfo<? extends Annotation>> find(ParameterInfo parameter, AnnotationTraversal... traversals) {
		trackCall("find(ParameterInfo, AnnotationTraversal...)");
		assertArgNotNull("parameter", parameter);
		if (traversals.length == 0)
			traversals = a(SELF, MATCHING_PARAMETERS, PARAMETER_TYPE);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == SELF) {
					return parameter.getAnnotations().stream();
				} else if (traversal == MATCHING_PARAMETERS) {
					return parameter.getMatchingParameters().stream().skip(1).flatMap(x -> x.getAnnotations().stream());
				} else if (traversal == PARAMETER_TYPE) {
					return find(parameter.getParameterType().unwrap(Value.class, Optional.class), PARENTS, PACKAGE);
				}
				throw illegalArg("Invalid traversal type for parameter annotations: {0}", traversal);
			});
	}

	/**
	 * Streams all annotations from a parameter using configurable traversal options in parent-first order, without filtering by annotation type.
	 *
	 * <p>
	 * This is equivalent to calling {@link #find(ParameterInfo, AnnotationTraversal...)}
	 * and reversing the result.
	 *
	 * @param parameter The parameter to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS}, {@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE}
	 * @return A stream of {@link AnnotationInfo} objects in parent-to-child order. Never <jk>null</jk>.
	 */
	public Stream<AnnotationInfo<? extends Annotation>> findTopDown(ParameterInfo parameter, AnnotationTraversal... traversals) {
		trackCall("findTopDown(ParameterInfo, AnnotationTraversal...)");
		return rstream(find(parameter, traversals).toList());
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
	 * @param parameter The parameter to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF, MATCHING_PARAMETERS, PARAMETER_TYPE}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}, {@link AnnotationTraversal#MATCHING_PARAMETERS MATCHING_PARAMETERS}, {@link AnnotationTraversal#PARAMETER_TYPE PARAMETER_TYPE}
	 * @return <jk>true</jk> if the annotation is found, <jk>false</jk> otherwise.
	 */
	public <A extends Annotation> boolean has(Class<A> type, ParameterInfo parameter, AnnotationTraversal... traversals) {
		trackCall("has(Class, ParameterInfo, AnnotationTraversal...)");
		return find(type, parameter, traversals).findFirst().isPresent();
	}

	/**
	 * Streams annotations from a field using configurable traversal options.
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
	 * @param field The field to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, FieldInfo field, AnnotationTraversal... traversals) {
		trackCall("find(Class, FieldInfo, AnnotationTraversal...)");
		assertArgNotNull("type", type);
		assertArgNotNull("field", field);
		if (traversals.length == 0)
			traversals = a(SELF);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == SELF) {
					return concat(
						fieldAnnotations.get(field.inner()).stream(),
						field.getAnnotations().stream()
					).filter(a -> a.isType(type)).map(a -> (AnnotationInfo<A>)a);
				}
				throw illegalArg("Invalid traversal type for field annotations: {0}", traversal);
			});
	}

	/**
	 * Streams all annotations from a field using configurable traversal options, without filtering by annotation type.
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
	 * @param field The field to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public Stream<AnnotationInfo<? extends Annotation>> find(FieldInfo field, AnnotationTraversal... traversals) {
		trackCall("find(FieldInfo, AnnotationTraversal...)");
		assertArgNotNull("field", field);
		if (traversals.length == 0)
			traversals = a(SELF);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == SELF) {
					return concat(
						fieldAnnotations.get(field.inner()).stream(),
						field.getAnnotations().stream()
					);
				}
				throw illegalArg("Invalid traversal type for field annotations: {0}", traversal);
			});
	}

	/**
	 * Streams annotations from a field using configurable traversal options in parent-first order.
	 *
	 * <p>
	 * This is equivalent to calling {@link #find(Class, FieldInfo, AnnotationTraversal...)}
	 * and reversing the result.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param field The field to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects in parent-first order. Never <jk>null</jk>.
	 */
	public <A extends Annotation> Stream<AnnotationInfo<A>> findTopDown(Class<A> type, FieldInfo field, AnnotationTraversal... traversals) {
		trackCall("findTopDown(Class, FieldInfo, AnnotationTraversal...)");
		return rstream(find(type, field, traversals).toList());
	}

	/**
	 * Streams all annotations from a field using configurable traversal options in parent-first order, without filtering by annotation type.
	 *
	 * <p>
	 * This is equivalent to calling {@link #find(FieldInfo, AnnotationTraversal...)}
	 * and reversing the result.
	 *
	 * @param field The field to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects in parent-first order. Never <jk>null</jk>.
	 */
	public Stream<AnnotationInfo<? extends Annotation>> findTopDown(FieldInfo field, AnnotationTraversal... traversals) {
		trackCall("findTopDown(FieldInfo, AnnotationTraversal...)");
		return rstream(find(field, traversals).toList());
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
	 * @param field The field to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}
	 * @return <jk>true</jk> if the annotation is found, <jk>false</jk> otherwise.
	 */
	public <A extends Annotation> boolean has(Class<A> type, FieldInfo field, AnnotationTraversal... traversals) {
		trackCall("has(Class, FieldInfo, AnnotationTraversal...)");
		return find(type, field, traversals).findFirst().isPresent();
	}

	/**
	 * Streams annotations from a constructor using configurable traversal options.
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
	 * @param constructor The constructor to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, ConstructorInfo constructor, AnnotationTraversal... traversals) {
		trackCall("find(Class, ConstructorInfo, AnnotationTraversal...)");
		assertArgNotNull("type", type);
		assertArgNotNull("constructor", constructor);
		if (traversals.length == 0)
			traversals = a(SELF);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == SELF) {
					return concat(
						constructorAnnotations.get(constructor.inner()).stream(),
						constructor.getDeclaredAnnotations().stream()
					).filter(a -> a.isType(type)).map(a -> (AnnotationInfo<A>)a);
				}
				throw illegalArg("Invalid traversal type for constructor annotations: {0}", traversal);
			});
	}

	/**
	 * Streams all annotations from a constructor using configurable traversal options, without filtering by annotation type.
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
	 * @param constructor The constructor to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects. Never <jk>null</jk>.
	 */
	public Stream<AnnotationInfo<? extends Annotation>> find(ConstructorInfo constructor, AnnotationTraversal... traversals) {
		trackCall("find(ConstructorInfo, AnnotationTraversal...)");
		assertArgNotNull("constructor", constructor);
		if (traversals.length == 0)
			traversals = a(SELF);

		return Arrays.stream(traversals)
			.sorted(Comparator.comparingInt(AnnotationTraversal::getOrder))
			.flatMap(traversal -> {
				if (traversal == SELF) {
					return concat(
						constructorAnnotations.get(constructor.inner()).stream(),
						constructor.getDeclaredAnnotations().stream()
					);
				}
				throw illegalArg("Invalid traversal type for constructor annotations: {0}", traversal);
			});
	}

	/**
	 * Streams annotations from a constructor using configurable traversal options in parent-first order.
	 *
	 * <p>
	 * This is equivalent to calling {@link #find(Class, ConstructorInfo, AnnotationTraversal...)}
	 * and reversing the result.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to search for.
	 * @param constructor The constructor to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects in parent-first order. Never <jk>null</jk>.
	 */
	public <A extends Annotation> Stream<AnnotationInfo<A>> findTopDown(Class<A> type, ConstructorInfo constructor, AnnotationTraversal... traversals) {
		trackCall("findTopDown(Class, ConstructorInfo, AnnotationTraversal...)");
		return rstream(find(type, constructor, traversals).toList());
	}

	/**
	 * Streams all annotations from a constructor using configurable traversal options in parent-first order, without filtering by annotation type.
	 *
	 * <p>
	 * This is equivalent to calling {@link #find(ConstructorInfo, AnnotationTraversal...)}
	 * and reversing the result.
	 *
	 * @param constructor The constructor to search.
	 * @param traversals The traversal options.
	 * @return A stream of {@link AnnotationInfo} objects in parent-first order. Never <jk>null</jk>.
	 */
	public Stream<AnnotationInfo<? extends Annotation>> findTopDown(ConstructorInfo constructor, AnnotationTraversal... traversals) {
		trackCall("findTopDown(ConstructorInfo, AnnotationTraversal...)");
		return rstream(find(constructor, traversals).toList());
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
	 * @param constructor The constructor to search.
	 * @param traversals
	 * 	The traversal options. If not specified, defaults to {@code SELF}.
	 * 	<br>Valid values: {@link AnnotationTraversal#SELF SELF}
	 * @return <jk>true</jk> if the annotation is found, <jk>false</jk> otherwise.
	 */
	public <A extends Annotation> boolean has(Class<A> type, ConstructorInfo constructor, AnnotationTraversal... traversals) {
		trackCall("has(Class, ConstructorInfo, AnnotationTraversal...)");
		return find(type, constructor, traversals).findFirst().isPresent();
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private <A extends Annotation> AnnotationInfo<A> ai(Annotatable on, A value) {
		return AnnotationInfo.of(on, value);
	}
}