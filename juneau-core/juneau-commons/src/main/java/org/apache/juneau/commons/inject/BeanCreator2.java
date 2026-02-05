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
package org.apache.juneau.commons.inject;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static java.util.Comparator.*;
import static org.apache.juneau.commons.reflect.ElementFlag.*;
import static java.util.Collections.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.concurrent.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.commons.reflect.*;

/**
 * Utility class for creating beans through constructors, creator methods, and builders.
 *
 * <p>
 * Uses a {@link BeanStore} to find available ways to construct beans via injection of beans from the store.
 *
 * <p>
 * This class is instantiated through the following method:
 * <ul class='javatree'>
 * 	<li class='jm'>{@link BeanCreator2#of(Class)}
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Bean with builder pattern</jc>
 * 	<jk>public class</jk> MyBean {
 * 		<jk>private final</jk> String <jf>value</jf>;
 *
 * 		<jk>private</jk> MyBean(String <jv>value</jv>) {
 * 			<jk>this</jk>.<jf>value</jf> = <jv>value</jv>;
 * 		}
 *
 * 		<jk>public static</jk> Builder <jsm>create</jsm>() {
 * 			<jk>return new</jk> Builder();
 * 		}
 *
 * 		<jk>public static class</jk> Builder {
 * 			<jk>private</jk> String <jf>value</jf>;
 *
 * 			<jk>public</jk> Builder value(String <jv>value</jv>) {
 * 				<jk>this</jk>.<jf>value</jf> = <jv>value</jv>;
 * 				<jk>return this</jk>;
 * 			}
 *
 * 			<jk>public</jk> MyBean build() {
 * 				<jk>return new</jk> MyBean(<jf>value</jf>);
 * 			}
 * 		}
 *
 * 	<jc>// Builder with @Inject on build() method</jc>
 * 	<jk>public class</jk> MyBean2 {
 * 		<jk>private final</jk> String <jf>value</jf>;
 * 		<jk>private final</jk> MyDependency <jf>dependency</jf>;
 *
 * 		<jk>private</jk> MyBean2(String <jv>value</jv>, MyDependency <jv>dependency</jv>) {
 * 			<jk>this</jk>.<jf>value</jf> = <jv>value</jv>;
 * 			<jk>this</jk>.<jf>dependency</jf> = <jv>dependency</jv>;
 * 		}
 *
 * 		<jk>public static class</jk> Builder {
 * 			<jk>private</jk> String <jf>value</jf>;
 *
 * 			<jk>public</jk> Builder value(String <jv>value</jv>) {
 * 				<jk>this</jk>.<jf>value</jf> = <jv>value</jv>;
 * 				<jk>return this</jk>;
 * 			}
 *
 * 			<ja>@Inject</ja>
 * 			<jk>public</jk> MyBean2 build(MyDependency <jv>dependency</jv>) {
 * 				<jk>return new</jk> MyBean2(<jv>value</jv>, <jv>dependency</jv>);
 * 			}
 * 		}
 * 	}
 * 	}
 *
 * 	<jc>// Create bean using BeanCreator2</jc>
 * 	MyBean <jv>bean</jv> = BeanCreator2
 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>myBeanStore</jv>)
 * 		.run();
 * </p>
 *
 * <h5 class='section'>Bean Creation Order:</h5>
 * <p>
 * The creator attempts to instantiate beans in the following order:
 * <ol class='spaced-list'>
 * 	<li><b>Using a Builder</b> (if a builder is found or specified):
 * 		<ol>
 * 			<li>Calls a <c>build()</c>/<c>create()</c>/<c>get()</c> method on the builder that returns the bean type.
 * 				<ul>
 * 					<li>The method is <b>required</b> - if no such method exists, bean creation fails.
 * 					<li>If the method has <c>@Inject</c> or <c>@Autowired</c> annotation, its parameters are resolved from the bean store.
 * 					<li>If the method does not have <c>@Inject</c> annotation, it must have no parameters.
 * 				</ul>
 * 		</ol>
 * 	<li><b>Without a Builder</b>:
 * 		<ol>
 * 			<li>Calls a static no-arg <c>getInstance()</c> method on the bean class.
 * 			<li>Calls a public constructor with parameters that can be resolved from the bean store.
 * 		</ol>
 * </ol>
 *
 * <h5 class='section'>Builder Detection:</h5>
 * <p>
 * Builders are detected in the following priority order:
 * <ol class='spaced-list'>
 * 	<li>Explicitly set via {@link #builder(Object)} or {@link #builder(Class)}.
 * 	<li>{@link Builder @Builder} annotation on the bean subtype.
 * 	<li>{@link Builder @Builder} annotation on the bean type.
 * 	<li>Auto-detection:
 * 		<ul>
 * 			<li>Static <c>create()</c> or <c>builder()</c> method that returns a builder type
 * 				(method names can be customized via {@link #builderMethodNames(String...)}).
 * 			<li>Inner class named <c>Builder</c>.
 * 		</ul>
 * </ol>
 *
 * <h5 class='section'>Dependency Injection:</h5>
 * <p>
 * Both builder instances and the final bean have automatic dependency injection performed via
 * {@link ClassInfo#inject(Object, BeanStore)}, which processes {@code @Inject} and {@code @Autowired} annotations
 * on fields and methods.
 *
 * <p>
 * This means that after a bean is created, all of its fields and methods annotated with {@code @Inject} or {@code @Autowired}
 * will automatically be populated with beans from the bean store.
 *
 * <h5 class='section'>Caching:</h5>
 * <p>
 * By default, each call to {@link #run()} creates a new bean instance. To enable caching, call {@link #cached()}.
 * When caching is enabled, bean instances are cached using {@link ResettableSupplier}, so multiple calls to {@link #run()}
 * will return the same instance unless {@link #reset()} is called or configuration changes. Explicit implementations set via
 * {@link #implementation(Object)} and explicit builder instances set via {@link #builder(Object)} are preserved
 * across resets.
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe. All configuration methods and bean creation use {@link SimpleReadWriteLock} to ensure
 * safe concurrent access. Multiple threads can safely call {@link #run()} simultaneously, and configuration changes
 * are properly synchronized.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Builders must have a <c>build()</c>, <c>create()</c>, or <c>get()</c> method that returns the bean type.
 * 		This method is required - constructors accepting builders are no longer supported.
 * 	<li class='note'>The builder's <c>build()</c>/<c>create()</c>/<c>get()</c> method may have <c>@Inject</c> or <c>@Autowired</c>
 * 		annotation to allow dependency injection of parameters from the bean store.
 * 	<li class='note'>If the builder's <c>build()</c>/<c>create()</c>/<c>get()</c> method does not have <c>@Inject</c> annotation,
 * 		it must have no parameters.
 * 	<li class='note'>All constructors and methods except {@link Optional} and {@link List} parameters
 * 		must have beans available in the store.
 * 	<li class='note'>If multiple constructors/methods are found, the one with the most matching parameters is used.
 * 	<li class='note'>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods/constructors are ignored.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicBeanStore2}
 * </ul>
 *
 * @param <T> The bean type being created.
 */
@SuppressWarnings("java:S115")
public class BeanCreator2<T> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_beanType = "beanType";
	private static final String ARG_fallback = "fallback";
	private static final String ARG_hook = "hook";
	private static final String ARG_value = "value";

	/** Default factory method names used for bean instantiation. */
	protected static final Set<String> DEFAULT_FACTORY_METHOD_NAMES = u(set("getInstance"));

	/** Default builder method names used for builder factory methods (static methods that create/return a builder). */
	protected static final Set<String> DEFAULT_BUILDER_METHOD_NAMES = u(set("create", "builder"));

	/** Default build method names used for builder build methods (instance methods on builder that build/return the bean). */
	protected static final Set<String> DEFAULT_BUILD_METHOD_NAMES = u(set("build", "create", "get"));

	/** Default builder class names for auto-detection. */
	protected static final Set<String> DEFAULT_BUILDER_CLASS_NAMES = u(set("Builder"));

	/**
	 * Creates a new bean creator.
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @return A new bean creator.
	 */
	public static <T> BeanCreator2<T> of(Class<T> beanType) {
		return new BeanCreator2<>(beanType, null, null, null);
	}

	/**
	 * Creates a new bean creator with a parent bean store.
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @param parentStore The parent bean store to use for resolving dependencies. Can be <jk>null</jk>.
	 * @return A new bean creator.
	 */
	public static <T> BeanCreator2<T> of(Class<T> beanType, BeanStore parentStore) {
		return new BeanCreator2<>(beanType, parentStore, null, null);
	}

	/**
	 * Creates a new bean creator with a parent bean store, name, and enclosing instance.
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @param parentStore The parent bean store to use for resolving dependencies. Can be <jk>null</jk>.
	 * @param name The bean name. Can be <jk>null</jk>.
	 * @param enclosingInstance The enclosing instance object. Can be <jk>null</jk>.
	 * @return A new bean creator.
	 */
	public static <T> BeanCreator2<T> of(Class<T> beanType, BeanStore parentStore, String name, Object enclosingInstance) {
		return new BeanCreator2<>(beanType, parentStore, name, enclosingInstance);
	}

	private final BeanStore parentStore;
	private final BasicBeanStore2 store;
	private final ClassInfoTyped<T> beanType;
	private final SimpleReadWriteLock lock = new SimpleReadWriteLock();
	private final OptionalReference<List<String>> debug = OptionalReference.empty();
	private static final Logger logger = Logger.getLogger(BeanCreator2.class);

	private ClassInfoTyped<? extends T> beanSubType;
	private ClassInfo explicitBuilderType = null;
	private Object explicitBuilder = null;
	private final Object enclosingInstance;
	private T explicitImplementation = null;
	private List<Consumer<T>> postCreateHooks = new ArrayList<>();
	private Set<String> factoryMethodNames = DEFAULT_FACTORY_METHOD_NAMES;
	private Set<String> builderMethodNames = DEFAULT_BUILDER_METHOD_NAMES;
	private Set<String> buildMethodNames = DEFAULT_BUILD_METHOD_NAMES;
	private Set<String> builderClassNames = DEFAULT_BUILDER_CLASS_NAMES;
	private Supplier<? extends T> fallbackSupplier = null;
	private final String name;
	private boolean cached = false;

	private ResettableSupplier<ClassInfo> builderType = memr(() -> findBuilderType());
	private ResettableSupplier<List<ClassInfo>> builderTypes = memr(() -> findBuilderTypes());
	private ResettableSupplier<List<ClassInfo>> beanSubTypes = memr(() -> findBeanSubTypes());
	private ResettableSupplier<Object> builder = memr(() -> findBuilder());
	private ResettableSupplier<T> beanImpl = memr(() -> findBeanImpl());

	/**
	 * Constructor.
	 *
	 * @param beanType The bean type being created.
	 * @param parentStore The parent bean store to use for resolving dependencies. Can be <jk>null</jk>.
	 * @param name The bean name. Can be <jk>null</jk>.
	 * @param enclosingInstance The enclosing instance object. Can be <jk>null</jk>.
	 */
	protected BeanCreator2(Class<T> beanType, BeanStore parentStore, String name, Object enclosingInstance) {
		this.beanType = info(assertArgNotNull(ARG_beanType, beanType));
		this.beanSubType = this.beanType;
		this.parentStore = parentStore;
		this.store = new BasicBeanStore2(this.parentStore);
		this.name = name;
		this.enclosingInstance = enclosingInstance;
	}

	/**
	 * Adds a bean to the internal bean store and returns it.
	 *
	 * @param <T2> The bean type.
	 * @param type The bean type.
	 * @param bean The bean instance.
	 * @return The bean that was added.
	 */
	public <T2> T2 add(Class<T2> type, T2 bean) {
		try (var writeLock = lock.write()) {
			store.add(type, bean);
			reset();
		}
		return bean;
	}

	/**
	 * Adds a bean to the internal bean store.
	 *
	 * @param <T2> The bean type.
	 * @param type The bean type.
	 * @param bean The bean instance.
	 * @return This object.
	 */
	public <T2> BeanCreator2<T> addBean(Class<T2> type, T2 bean) {
		try (var writeLock = lock.write()) {
			store.add(type, bean);
			reset();
		}
		return this;
	}

	/**
	 * Adds a named bean to the internal bean store.
	 *
	 * @param <T2> The bean type.
	 * @param type The bean type.
	 * @param bean The bean instance.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return This object.
	 */
	public <T2> BeanCreator2<T> addBean(Class<T2> type, T2 bean, String name) {
		try (var writeLock = lock.write()) {
			store.add(type, bean, name);
			reset();
		}
		return this;
	}

	/**
	 * Attempts to create the bean, returning an {@link Optional} instead of throwing an exception on failure.
	 *
	 * <p>
	 * This is a non-throwing variant of {@link #run()} that wraps the result in an {@link Optional}.
	 * If bean creation fails for any reason (missing dependencies, abstract class, interface, no matching constructor, etc.),
	 * this method returns {@link Optional#empty()} instead of throwing {@link ExecutableException}.
	 *
	 * <p>
	 * This method is useful when bean creation failure is expected and should not be treated as exceptional,
	 * such as when implementing optional features or trying multiple creation strategies.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Unlike {@link #run()}, this method catches {@link ExecutableException} and returns empty.
	 * 	<li class='note'>If a fallback is registered via {@link #fallback(Supplier)},
	 * 		it will be used before returning empty, so this method will only return empty if both
	 * 		normal creation AND fallback fail.
	 * 	<li class='note'>The created bean is cached via {@link ResettableSupplier}, so subsequent calls return the same instance
	 * 		unless {@link #reset()} is called or configuration changes.
	 * 	<li class='note'>Post-creation hooks are still executed if creation succeeds.
	 * 	<li class='note'>Other exceptions (e.g., from post-creation hooks or fallback suppliers) are NOT caught
	 * 		and will propagate to the caller.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Try to create, use default if it fails</jc>
	 * 	MyBean <jv>bean</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.asOptional()
	 * 		.orElse(<jk>new</jk> DefaultMyBean());
	 *
	 * 	<jc>// Chain multiple creation attempts</jc>
	 * 	MyService <jv>service</jv> = BeanCreator2.<jsm>of</jsm>(AdvancedService.<jk>class</jk>, <jv>store</jv>)
	 * 		.asOptional()
	 * 		.or(() -&gt; BeanCreator2.<jsm>of</jsm>(BasicService.<jk>class</jk>, <jv>store</jv>)
	 * 			.asOptional())
	 * 		.orElseGet(() -&gt; <jk>new</jk> FallbackService());
	 *
	 * 	<jc>// Check if optional feature is available</jc>
	 * 	Optional&lt;OptionalFeature&gt; <jv>feature</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(OptionalFeature.<jk>class</jk>, <jv>store</jv>)
	 * 		.asOptional();
	 * 	<jk>if</jk> (<jv>feature</jv>.isPresent()) {
	 * 		<jv>feature</jv>.get().enable();
	 * 	}
	 * </p>
	 *
	 * @return An {@link Optional} containing the created bean, or {@link Optional#empty()} if creation failed.
	 */
	public Optional<T> asOptional() {
		try {
			return Optional.of(run());
		} catch (@SuppressWarnings("unused") ExecutableException e) {
			return Optional.empty();
		}
	}

	/**
	 * Converts this creator into a resettable supplier.
	 *
	 * <p>
	 * A {@link ResettableSupplier} caches the result of the first {@link ResettableSupplier#get()} call
	 * and returns that cached value for subsequent calls. The {@link ResettableSupplier#reset()} method
	 * can be used to clear the cache, forcing the bean to be recreated on the next get() call.
	 *
	 * <p>
	 * This is particularly useful when the bean store contents change and you want to recreate the bean
	 * with updated dependencies.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The returned {@link ResettableSupplier} is thread-safe.
	 * 	<li class='note'>Each call to {@link ResettableSupplier#get()} after a reset() will invoke
	 * 		{@link #run()}, which may trigger dependency injection and post-creation hooks.
	 * 	<li class='note'>The supplier inherits all Optional-like methods from {@link OptionalSupplier}.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a resettable supplier for a bean with dependencies</jc>
	 * 	BeanStore <jv>store</jv> = <jk>new</jk> BasicBeanStore2(<jk>null</jk>);
	 * 	<jv>store</jv>.addBean(String.<jk>class</jk>, <js>"initial"</js>);
	 *
	 * 	ResettableSupplier&lt;MyBean&gt; <jv>supplier</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>store</jv>)
	 * 		.asResettableSupplier();
	 *
	 * 	<jc>// First call creates and caches the bean</jc>
	 * 	MyBean <jv>bean1</jv> = <jv>supplier</jv>.get();
	 *
	 * 	<jc>// Subsequent calls return the cached bean</jc>
	 * 	MyBean <jv>bean2</jv> = <jv>supplier</jv>.get();
	 * 	<jsm>assertSame</jsm>(<jv>bean1</jv>, <jv>bean2</jv>);
	 *
	 * 	<jc>// Update the bean store</jc>
	 * 	<jv>store</jv>.addBean(String.<jk>class</jk>, <js>"updated"</js>);
	 *
	 * 	<jc>// Reset forces recreation with updated dependencies</jc>
	 * 	<jv>supplier</jv>.reset();
	 * 	MyBean <jv>bean3</jv> = <jv>supplier</jv>.get();
	 * 	<jsm>assertNotSame</jsm>(<jv>bean1</jv>, <jv>bean3</jv>);  <jc>// New instance with updated deps</jc>
	 * </p>
	 *
	 * @return A resettable supplier that caches the created bean until reset.
	 */
	public ResettableSupplier<T> asResettableSupplier() {
		return new ResettableSupplier<>(this::run);
	}

	/**
	 * Converts this creator into a supplier.
	 *
	 * @return A supplier that returns the results of the {@link #run()} method.
	 */
	public Supplier<T> asSupplier() {
		return this::run;
	}

	/**
	 * Specifies a subclass of the bean type to create.
	 *
	 * <p>
	 * This allows you to create a specific implementation when the bean type is an interface, abstract class,
	 * or when you want to instantiate a customized subclass of a concrete class.
	 *
	 * @param value The subclass type to create. Cannot be <jk>null</jk>. Must be a subclass of {@code beanType}.
	 * @return This object.
	 * @throws IllegalArgumentException If value is not a subclass of {@code beanType}.
	 */
	public BeanCreator2<T> beanSubType(Class<? extends T> value) {
		assertArgNotNull(ARG_value, value);
		try (var writeLock = lock.write()) {
			beanSubType = info(value);
			assertArg(beanType.isParentOf(beanSubType), "beanSubType must be a subclass of beanType. beanType={0}, beanSubType={1}", cn(beanType), cn(beanSubType));
			reset();
		}
		return this;
	}

	/**
	 * Specifies the builder type to use for creating the bean.
	 *
	 * <p>
	 * This method triggers builder type detection and instantiation for the specified type.
	 *
	 * <p>
	 * The builder type must be valid, meaning it must have a <c>build()</c>, <c>create()</c>, or <c>get()</c> method
	 * that returns the bean type (or a parent of the bean type). The method may have <c>@Inject</c> or <c>@Autowired</c>
	 * annotation to allow injected parameters; otherwise, it must have no parameters.
	 *
	 * <p>
	 * If the builder type is invalid (does not have a valid build method), this method throws an
	 * {@link IllegalArgumentException}.
	 *
	 * <p>
	 * The builder will be instantiated by looking for:
	 * <ol>
	 * 	<li>A static <c>create()</c> or <c>builder()</c> method that returns the builder type.
	 * 	<li>A public constructor on the builder type with parameters that can be resolved from the bean store.
	 * 	<li>A protected constructor on the builder type with parameters that can be resolved from the bean store.
	 * </ol>
	 *
	 * @param value The builder type. Cannot be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If the builder type is invalid (does not have a valid build/create/get method).
	 */
	public BeanCreator2<T> builder(Class<?> value) {
		try (var writeLock = lock.write()) {
			explicitBuilderType = info(assertArgNotNull(ARG_value, value));
			builderType.set(explicitBuilderType);
			assertArg(isValidBuilderType(explicitBuilderType), "Invalid builder type {0} for bean type {1}. Builder must have a build(), create(), or get() method that returns {1} (or a parent of {1}). The method may have @Inject annotation to allow injected parameters; otherwise, it must have no parameters.", cn(explicitBuilderType), cn(beanType));
			builderTypes.get();  // Triggers validation on type hierarchy.
			reset();
		}
		return this;
	}

	/**
	 * Specifies an existing builder instance to use for creating the bean.
	 *
	 * <p>
	 * The builder instance's class must be a valid builder type, meaning it must have a <c>build()</c>, <c>create()</c>,
	 * or <c>get()</c> method that returns the bean type (or a parent of the bean type). The method may have
	 * <c>@Inject</c> or <c>@Autowired</c> annotation to allow injected parameters; otherwise, it must have no parameters.
	 *
	 * <p>
	 * If the builder instance's class is invalid (does not have a valid build method), this method throws an
	 * {@link IllegalArgumentException}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>When specified, the builder type detection is bypassed.
	 * 	<li class='note'>The builder will be used to create the bean via:
	 * 		<ul>
	 * 			<li>A <c>build()</c>/<c>create()</c>/<c>get()</c> method on the builder (required).
	 * 				The method may have <c>@Inject</c> annotation to allow injected parameters.
	 * 		</ul>
	 * 	<li class='note'>The explicit builder instance is preserved even when {@link #reset()} is called,
	 * 		unlike cached results from suppliers.
	 * </ul>
	 *
	 * @param value The builder instance. Cannot be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If the builder instance's class is invalid (does not have a valid build/create/get method).
	 */
	public BeanCreator2<T> builder(Object value) {
		try (var writeLock = lock.write()) {
			builder(value.getClass());
			explicitBuilder = assertArgNotNull(ARG_value, value);
			reset();
		}
		return this;
	}

	/**
	 * Specifies custom class names to look for when auto-detecting builder inner classes.
	 *
	 * <p>
	 * By default, {@code BeanCreator2} looks for inner classes named {@code "Builder"}.
	 * This method allows you to specify alternative class names.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Inner builder classes must be public, static, and pass builder validation
	 * 		(have a build/create/get method or be accepted by a bean constructor).
	 * 	<li class='note'>If multiple inner classes match, the first valid one found will be used.
	 * 	<li class='note'>This only affects inner class detection. It does not affect builders specified via
	 * 		{@link #builder(Class)}, {@link #builder(Object)}, or the {@link Builder @Builder} annotation.
	 * 	<li class='note'>Calling this method replaces the default builder class names.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Support alternative builder class naming conventions</jc>
	 * 	MyBean <jv>bean</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.builderClassNames(<js>"BuilderImpl"</js>, <js>"Factory"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param names The builder class names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public BeanCreator2<T> builderClassNames(String... names) {
		try (var writeLock = lock.write()) {
			builderClassNames = set(assertArgNoNulls("names", names));
			reset();
		}
		return this;
	}

	/**
	 * Specifies custom method names to look for when auto-detecting builder factory methods.
	 *
	 * <p>
	 * By default, {@code BeanCreator2} looks for static methods named {@code "create"} or {@code "builder"}
	 * that return a builder type. This method allows you to specify alternative method names.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Builder factory methods must be public, static, non-deprecated, and return a builder type
	 * 		(not the bean type itself).
	 * 	<li class='note'>If multiple builder factory methods match, the one with the most parameters that can be
	 * 		resolved from the bean store will be used.
	 * 	<li class='note'>This affects both builder type detection and builder instance creation.
	 * 	<li class='note'>Calling this method replaces the default builder method names.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Support alternative builder factory method naming conventions</jc>
	 * 	MyBean <jv>bean</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.builderMethodNames(<js>"newBuilder"</js>, <js>"instance"</js>)
		.run();
	 * </p>
	 *
	 * @param names The builder factory method names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public BeanCreator2<T> builderMethodNames(String... names) {
		try (var writeLock = lock.write()) {
			builderMethodNames = set(assertArgNoNulls("names", names));
			reset();
		}
		return this;
	}

	/**
	 * Specifies custom method names to look for when calling build methods on builder instances.
	 *
	 * <p>
	 * By default, {@code BeanCreator2} looks for instance methods named {@code "build"}, {@code "create"}, or {@code "get"}
	 * on the builder that return the bean type. This method allows you to specify alternative method names.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Build methods must be public, non-static, non-deprecated, and return the bean type
	 * 		(or a parent type of the bean type).
	 * 	<li class='note'>If the method has {@code @Inject} or {@code @Autowired} annotation, its parameters are resolved from the bean store.
	 * 	<li class='note'>If the method does not have {@code @Inject} annotation, it must have no parameters.
	 * 	<li class='note'>Calling this method replaces the default build method names.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Support alternative build method naming conventions</jc>
	 * 	MyBean <jv>bean</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.buildMethodNames(<js>"execute"</js>, <js>"make"</js>)
		.run();
	 * </p>
	 *
	 * @param names The build method names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public BeanCreator2<T> buildMethodNames(String... names) {
		try (var writeLock = lock.write()) {
			buildMethodNames = set(assertArgNoNulls("names", names));
			reset();
		}
		return this;
	}

	/**
	 * Enables caching mode.
	 *
	 * <p>
	 * When caching mode is enabled, bean instances are cached using {@link ResettableSupplier},
	 * so multiple calls to {@link #run()} will return the same instance unless {@link #reset()} is called
	 * or configuration changes.
	 *
	 * <p>
	 * When caching mode is disabled (the default), each call to {@link #run()} will create a new bean instance
	 * by calling {@link #findBeanImpl()} directly.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Enable caching mode - same instance returned on each run()</jc>
	 * 	<jk>var</jk> <jv>creator</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.<jsm>cached</jsm>();
	 * 	MyBean <jv>bean1</jv> = <jv>creator</jv>.<jsm>run</jsm>();
	 * 	MyBean <jv>bean2</jv> = <jv>creator</jv>.<jsm>run</jsm>();
	 * 	<jc>// bean1 and bean2 are the same instance</jc>
	 * </p>
	 *
	 * @return This object.
	 */
	public BeanCreator2<T> cached() {
		try (var writeLock = lock.write()) {
			cached = true;
		}
		return this;
	}

	/**
	 * Creates the bean.
	 *
	 * <p>
	 * After creating the bean, this method automatically injects dependencies into fields and methods
	 * annotated with {@code @Inject} or {@code @Autowired} using {@link ClassInfo#inject(Object, BeanStore)}.
	 *
	 * @return A new bean with all dependencies injected.
	 * @throws ExecutableException if bean could not be created.
	 */
	public T run() {
		debug.ifPresent(x -> x.clear());

		try (var readLock = lock.read()) {

			if (neq(beanSubType, beanType))
				log("Subtype specified: %s", beanSubType.getName());

			if (explicitImplementation != null) {
				log("Using pre-configured impl() instance");
				return runPostCreateHooks(inject(explicitImplementation));
			}

			if (cached) {
				log("Using cached instance");
				return beanImpl.get();
			}

			log("Using new instance");
			return findBeanImpl();
		}
	}

	/**
	 * Enables debug mode to track the bean creation process.
	 *
	 * <p>
	 * When debug mode is enabled, the creator logs each step of the bean creation process
	 * to an internal list. This log can be retrieved via {@link #getDebugLog()} to understand
	 * which creation path was taken, why creation might have failed, or to troubleshoot
	 * unexpected behavior.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Debug logs include: builder detection, factory method attempts, constructor attempts,
	 * 		dependency resolution, injection, hooks execution, validation, and fallback usage.
	 * 	<li class='note'>The debug log is reset each time {@link #run()} or {@link #asOptional()} is called.
	 * 	<li class='note'>Debug mode has minimal performance impact but should generally be disabled in production.
	 * 	<li class='note'>The debug log is stored in memory and can be retrieved after bean creation.
	 * 	<li class='note'>When caching is enabled via {@link #cached()}, the log reflects the first creation only
	 * 		(subsequent calls return the cached instance unless {@link #reset()} is called).
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Enable debug mode</jc>
	 * 	<jk>var</jk> <jv>creator</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>store</jv>)
	 * 		.debug();
	 *
	 * 	MyBean <jv>bean</jv> = <jv>creator</jv>.run();
	 *
	 * 	<jc>// Print debug log</jc>
	 * 	<jv>creator</jv>.getDebugLog().forEach(System.<jf>out</jf>::println);
	 * </p>
	 *
	 * @return This object.
	 */
	public BeanCreator2<T> debug() {
		try (var writeLock = lock.write()) {
			debug.set(synchronizedList(new ArrayList<>()));
		}
		return this;
	}


	/**
	 * Specifies custom static factory method names to look for when creating the bean.
	 *
	 * <p>
	 * By default, {@link BeanCreator2} looks for a static {@code getInstance()} method when attempting
	 * to create beans without a builder. This method allows you to specify alternative
	 * factory method names to support non-standard naming conventions.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Factory methods must be public, static, non-deprecated, and return the bean type.
	 * 	<li class='note'>If multiple factory methods match, the one with the most parameters that can be
	 * 		resolved from the bean store will be used.
	 * 	<li class='note'>Factory methods are attempted before constructors in the bean creation order.
	 * 	<li class='note'>This setting only affects factory method detection; it does not affect builder detection.
	 * 	<li class='note'>Calling this method replaces the default factory method names.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Support multiple factory method naming conventions</jc>
	 * 	MyBean <jv>bean</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.factoryMethodNames(<js>"of"</js>, <js>"from"</js>, <js>"create"</js>, <js>"newInstance"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param names The factory method names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public BeanCreator2<T> factoryMethodNames(String... names) {
		try (var writeLock = lock.write()) {
			factoryMethodNames = set(assertArgNoNulls("names", names));
			reset();
		}
		return this;
	}

	/**
	 * Specifies a fallback supplier to use if bean creation fails.
	 *
	 * <p>
	 * When a fallback supplier is registered, if bean creation fails (throws {@link ExecutableException}),
	 * instead of propagating the exception, the fallback supplier will be invoked to provide a default instance.
	 *
	 * <p>
	 * This enables graceful degradation when bean creation cannot succeed, such as when optional features
	 * or dependencies are not available.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The fallback supplier is only invoked if bean creation fails.
	 * 		If bean creation succeeds normally, the fallback is never called.
	 * 	<li class='note'>Fallbacks are checked after all normal creation attempts (builders, factory methods, constructors).
	 * 	<li class='note'>The fallback-provided instance will have post-creation hooks executed on it.
	 * 	<li class='note'>If the fallback is used, that instance is cached via {@link ResettableSupplier} and returned on subsequent calls
	 * 		unless {@link #reset()} is called or configuration changes.
	 * 	<li class='note'>Only one fallback can be registered. Subsequent calls to this method will replace
	 * 		the previous fallback.
	 * 	<li class='note'>If the fallback supplier itself throws an exception, that exception will propagate to the caller.
	 * 	<li class='note'>To use a pre-created instance as a fallback, wrap it in a supplier: <c>fallback(() -&gt; myInstance)</c>.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Provide a default instance if creation fails</jc>
	 * 	MyService <jv>service</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyService.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.fallback(() -&gt; <jk>new</jk> DefaultMyService())
	 * 		.run();
	 *
	 * 	<jc>// Use a pre-created instance as fallback</jc>
	 * 	MyService <jv>defaultService</jv> = <jk>new</jk> DefaultMyService();
	 * 	MyService <jv>service2</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyService.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.fallback(() -&gt; <jv>defaultService</jv>)
	 * 		.run();
	 * </p>
	 *
	 * @param fallback The fallback supplier. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> fallback(Supplier<? extends T> fallback) {
		assertArgNotNull(ARG_fallback, fallback);
		try (var writeLock = lock.write()) {
			this.fallbackSupplier = fallback;
		}
		return this;
	}

	/**
	 * Returns the bean subtype class info if specified, or the normal bean type if not.
	 *
	 * <p>
	 * If {@link #beanSubType(Class)} was called to specify a subtype, returns that subtype.
	 * Otherwise, returns the original bean type passed to {@link #of(Class)}.
	 *
	 * @return The bean subtype class info, or the bean type class info if no subtype was specified.
	 */
	protected ClassInfo getBeanSubType() {
		try (var readLock = lock.read()) {
			return beanSubType;
		}
	}

	/**
	 * Returns the name of the bean being created.
	 *
	 * <p>
	 * Returns the name that was set via {@link #name(String)}, or <jk>null</jk> if no name was specified.
	 *
	 * @return The bean name, or <jk>null</jk> if not set.
	 */
	public String getName() {
		try (var readLock = lock.read()) {
			return name;
		}
	}

	/**
	 * Returns the list of bean subtype classes, including the bean subtype, bean type, and all classes in between.
	 *
	 * <p>
	 * This method returns a list that starts with the bean subtype (if different from bean type) and includes
	 * all parent classes up to and including the bean type. If no subtype was specified, the list will contain
	 * only the bean type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// If beanType is A and beanSubType is C (extends B extends A)</jc>
	 * 	<jc>// getBeanSubTypes() returns: [C, B, A]</jc>
	 * </p>
	 *
	 * @return A list of bean subtype class info objects, never <jk>null</jk>.
	 */
	protected List<ClassInfo> getBeanSubTypes() {
		try (var readLock = lock.read()) {
			return beanSubTypes.get();
		}
	}

	/**
	 * Returns the builder instance if one has been created or specified.
	 *
	 * @param <B> The builder type.
	 * @return The builder instance wrapped in an {@link Optional}, or an empty optional if no builder exists.
	 */
	@SuppressWarnings("unchecked")
	public <B> Optional<B> getBuilder() {
		try (var writeLock = lock.write()) {
			beanImpl.reset();
			return (Optional<B>)builder.toOptional();
		}
	}

	/**
	 * Returns the builder type class info determined by {@link #findBuilderType()}.
	 *
	 * @return The builder type class info, or <jk>null</jk> if no builder type was determined.
	 */
	protected ClassInfo getBuilderType() {
		try (var readLock = lock.read()) {
			return builderType.get();
		}
	}

	/**
	 * Returns the list of builder types, including the primary builder type and any builder types found in the builder's parent hierarchy.
	 *
	 * <p>
	 * This method returns a list that includes the primary builder type and all builder types found by traversing
	 * the builder's parent hierarchy. This allows parent builders to be resolved as injectable types.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Class A with builder</jc>
	 * 	<jk>class</jk> A {
	 * 		<jk>static class</jk> Builder {
	 * 			A create() { <jk>return new</jk> A(); }
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Class B extends A with builder</jc>
	 * 	<jk>class</jk> B <jk>extends</jk> A {
	 * 		<jk>static class</jk> Builder <jk>extends</jk> A.Builder {
	 * 			B create() { <jk>return new</jk> B(); }
	 * 		}
	 * 	}
	 *
	 * 	<jc>// When creating B, getBuilderTypes() returns: [B.Builder, A.Builder]</jc>
	 * </p>
	 *
	 * @return A list of builder type class info objects, never <jk>null</jk>.
	 */
	protected List<ClassInfo> getBuilderTypes() {
		try (var readLock = lock.read()) {
			return builderTypes.get();
		}
	}

	/**
	 * Returns an unmodifiable list of debug log entries from the last bean creation attempt.
	 *
	 * <p>
	 * This method returns the detailed log of steps taken during the most recent call to
	 * {@link #run()} or {@link #asOptional()}. The log is only populated when {@link #debug()}
	 * mode is enabled.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Returns an empty list if {@link #debug()} was not called or if no creation has been attempted.
	 * 	<li class='note'>The log is reset at the beginning of each {@link #run()} call.
	 * 	<li class='note'>Since {@link #run()} uses a cached {@link ResettableSupplier}, the log reflects only the first creation
	 * 		(subsequent calls return the cached instance unless {@link #reset()} is called).
	 * 	<li class='note'>The returned list is unmodifiable.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>var</jk> <jv>creator</jv> = BeanCreator2.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>store</jv>)
	 * 		.debug();
	 *
	 * 	<jk>try</jk> {
		<jv>creator</jv>.run();
	 * 	} <jk>catch</jk> (ExecutableException <jv>e</jv>) {
	 * 		<jc>// Print debug log to understand why creation failed</jc>
	 * 		<jv>creator</jv>.getDebugLog().forEach(System.<jf>err</jf>::println);
	 * 	}
	 * </p>
	 *
	 * @return An unmodifiable list of debug log entries.
	 */
	public List<String> getDebugLog() {
		return debug.map(x -> u(copyOf(debug.get()))).orElse(emptyList());
	}

	/**
	 * Specifies an existing bean implementation to use instead of creating a new instance.
	 *
	 * <p>
	 * When specified, the bean creation logic is bypassed and this implementation is returned
	 * directly from {@link #run()}, after performing dependency injection on it.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The bean implementation will still have its fields and methods injected via {@link ClassInfo#inject(Object, BeanStore)}.
	 * 	<li class='note'>This is useful when you want to provide a pre-configured instance but still benefit from dependency injection.
	 * 	<li class='note'>The explicit implementation is preserved even when {@link #reset()} is called, unlike cached results from suppliers.
	 * </ul>
	 *
	 * @param value The bean implementation instance.
	 * @return This object.
	 */
	public BeanCreator2<T> implementation(T value) {
		try (var writeLock = lock.write()) {
			this.explicitImplementation = value;
		}
		return this;
	}

	/**
	 * Registers a post-creation hook to run after the bean is created.
	 *
	 * <p>
	 * Post-creation hooks are executed after the bean is instantiated and after dependency injection
	 * has been performed via {@link ClassInfo#inject(Object, BeanStore)}.
	 *
	 * <p>
	 * Multiple hooks can be registered and they will be executed in the order they were added.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Hooks are executed every time {@link #run()} is called. Since {@link #run()} uses a cached
	 * 		{@link ResettableSupplier}, hooks are only executed once unless {@link #reset()} is called.
	 * 	<li class='note'>Hooks are executed after dependency injection, so all {@code @Inject} fields and methods
	 * 		will have been populated before the hook runs.
	 * 	<li class='note'>Hooks can be used for custom initialization logic, logging, wrapping beans with proxies,
	 * 		or any other post-creation processing.
	 * 	<li class='note'>If a hook throws an exception, bean creation will fail and the exception will propagate to the caller.
	 * 	<li class='note'>The hook receives the fully initialized bean instance as its parameter.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Register multiple post-creation hooks</jc>
	 * 	MyService <jv>service</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyService.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.postCreateHook(<jv>s</jv> -&gt; <jv>s</jv>.initialize())
	 * 		.postCreateHook(<jv>s</jv> -&gt; <jv>s</jv>.loadConfiguration())
	 * 		.postCreateHook(<jv>s</jv> -&gt; <jv>logger</jv>.info(<js>"Created service: "</js> + <jv>s</jv>))
		.run();
	 * </p>
	 *
	 * @param hook The post-creation hook to run after bean creation. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> postCreateHook(Consumer<T> hook) {
		assertArgNotNull(ARG_hook, hook);
		try (var writeLock = lock.write()) {
			postCreateHooks.add(hook);
		}
		return this;
	}


	/**
	 * Resets the builder, builderType, and beanImpl suppliers.
	 *
	 * <p>
	 * This causes them to be re-evaluated on the next call to {@link #run()}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>If an explicit builder instance was set via {@link #builder(Object)},
	 * 		it is preserved and not cleared by this method.
	 * 	<li class='note'>If an explicit implementation was set via {@link #implementation(Object)},
	 * 		it is preserved and not cleared by this method.
	 * 	<li class='note'>Only the cached results from suppliers are reset, not the configuration itself.
	 * </ul>
	 *
	 * @return This object.
	 */
	public BeanCreator2<T> reset() {
		try (var writeLock = lock.write()) {
			// Only reset builder if no explicit builder instance was set
			// Explicit builder instances should be preserved across resets
			if (explicitBuilder == null) {
				builder.reset();
			}
			builderType.reset();
			// Only reset beanImpl if no explicit implementation was set
			// Explicit implementations should be preserved across resets
			if (explicitImplementation == null) {
				beanImpl.reset();
			}
		}
		return this;
	}

	/**
	 * Finds and creates the bean implementation.
	 *
	 * <p>
	 * This method contains all the logic for creating beans via builders, factory methods, constructors, or fallbacks.
	 *
	 * @return The created bean.
	 * @throws ExecutableException if bean could not be created.
	 */
	@SuppressWarnings({ "unchecked", "java:S3776", "java:S6541" })
	private T findBeanImpl() {
		var store2 = this.store;
		var builder2 = explicitBuilder != null ? explicitBuilder : this.builder.get();  // Use explicit builder if set, otherwise get from supplier
		T bean = null;
		var methodComparator = comparing(MethodInfo::getParameterCount).reversed();
		var constructorComparator = comparing(ConstructorInfo::getParameterCount).reversed();

		if (builder2 != null) {
			log("Builder detected: %s", builder2.getClass().getName());

			// Look for Builder.build()/create()/get() - REQUIRED
			// Uses buildMethodNames configuration (defaults to "build", "create", "get")
			// Check declared methods first to get the most specific override, then fall back to public methods
			var buildMethod = info(builder2.getClass()).getPublicMethods().stream()
				.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
				.filter(x -> buildMethodNames.contains(x.getNameSimple()))
				.filter(x -> opt(x).map(x2 -> x2.getReturnType()).filter(x2 -> x2.is(beanSubType.inner()) || x2.isParentOf(beanSubType)).isPresent()) // Accept methods that return beanSubType or a parent type of beanSubType
				.filter(x -> x.getAnnotations().stream().map(AnnotationInfo::getNameSimple).anyMatch(n -> eqAny(n, "Inject", "Autowired")) ? x.canResolveAllParameters(store2) : x.getParameterCount() == 0)
				.sorted(methodComparator)
				.findFirst();

			if (buildMethod.isPresent()) {
				var method = buildMethod.get();
				log("Found build method: %s", method.getNameFull());
				log("Method declaring class: %s", method.getDeclaringClass().getName());
				log("Builder class: %s", builder2.getClass().getName());
				var returnType = method.getReturnType();
				log("Method return type: %s", returnType.getName());
				log("Expected beanSubType: %s", beanSubType.getName());

				// Check if method has @Inject annotation
				boolean hasInject = method.getAnnotations().stream().map(AnnotationInfo::getNameSimple).anyMatch(n -> eqAny(n, "Inject", "Autowired"));
				if (hasInject) {
					log("Method has @Inject annotation, resolving parameters from bean store");
				} else {
					log("Method has no parameters, calling without injection");
				}

				// Call the builder method
				Object builtBean = method.inject(store2, builder2);

				// Builder build method must return the exact beanSubType
				if (!returnType.is(beanSubType.inner())) {
					log("Builder method %s returns %s, but must return %s", method.getNameFull(), returnType.getName(), beanSubType.getName());
					throw exex("Builder method {0} returns {1}, but must return {2}. Builder build methods must always return the exact bean subtype being created.", method.getNameFull(), returnType.getName(), beanSubType.getName());
				}
				bean = (T)beanType.cast(builtBean);
			}

			if (bean != null) {
				log("Bean created successfully via builder");
				bean = inject(bean);
				runPostCreateHooks(bean);
				return bean;
			}

			// Builder was found but no appropriate build method was found
			// Try fallback: look for ANY method on builder that returns the exact bean type
			log("Attempting Builder.anything()");
			var anyMethod = info(builder2.getClass()).getPublicMethods().stream()
				.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
				.filter(x -> !buildMethodNames.contains(x.getNameSimple())) // Skip standard build methods we already checked
				.filter(x -> x.getReturnType().is(beanSubType.inner())) // Must return exact beanSubType, not parent
				.filter(x -> x.getAnnotations().stream().map(AnnotationInfo::getNameSimple).anyMatch(n -> eqAny(n, "Inject", "Autowired")) ? x.canResolveAllParameters(store2) : x.getParameterCount() == 0)
				.sorted(methodComparator)
				.findFirst();

			if (anyMethod.isPresent()) {
				var method = anyMethod.get();
				log("Found builder method returning bean type: %s", method.getNameFull());
				var returnType = method.getReturnType();

				// Call the builder method
				Object builtBean = method.inject(store2, builder2);

				// Builder method must return the exact beanSubType
				if (!returnType.is(beanSubType.inner())) {
					log("Builder method %s returns %s, but must return %s", method.getNameFull(), returnType.getName(), beanSubType.getName());
					throw exex("Builder method {0} returns {1}, but must return {2}. Builder build methods must always return the exact bean subtype being created.", method.getNameFull(), returnType.getName(), beanSubType.getName());
				}
				bean = (T)beanType.cast(builtBean);

				if (bean != null) {
					log("Bean created successfully via builder fallback method");
					bean = inject(bean);
					runPostCreateHooks(bean);
					return bean;
				}
			}

			// Check if builder type has ANY method that returns the right type (even if not named build/create/get)
			// If so, log that builder was detected but no appropriate build method was found
			// Otherwise, log that builder type is invalid
			var builderTypeInfo = builderType.get();
			boolean hasAnyMethodWithRightReturnType = builderTypeInfo != null && builderTypeInfo.getPublicMethods().stream()
				.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
				.map(x -> x.getReturnType())
				.anyMatch(x -> x.isAssignableFrom(beanSubType));
			boolean hasBuildMethodWithRightReturnType = builderTypeInfo != null && builderTypeInfo.getPublicMethods().stream()
				.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
				.filter(x -> buildMethodNames.contains(x.getNameSimple()))
				.map(x -> x.getReturnType())
				.anyMatch(x -> x.isAssignableFrom(beanSubType));
			boolean isExplicitBuilder = explicitBuilderType != null || explicitBuilder != null;
			boolean isValidBuilder = builderTypeInfo != null && isValidBuilderType(builderTypeInfo);

			if (hasAnyMethodWithRightReturnType && !hasBuildMethodWithRightReturnType) {
				// Builder has a method returning the right type but not with the expected name
				log("Builder detected but no appropriate build method found. Builder type: %s. Expected method names: %s", builder2.getClass().getName(), buildMethodNames);
				// Fall through to factory methods/constructors
			} else if (hasBuildMethodWithRightReturnType || isExplicitBuilder || isValidBuilder) {
				// Builder has a build method with right return type (even if can't be called) or was explicitly set
				// Throw exception unless fallback exists
				log("Builder detected but no appropriate build method found. Builder type: %s. Expected method names: %s", builder2.getClass().getName(), buildMethodNames);

				if (fallbackSupplier != null) {
					log("Using fallback supplier");
					T fallbackBean = fallbackSupplier.get();
					runPostCreateHooks(fallbackBean);
					return fallbackBean;
				}

				log("Failed to create bean using builder");
				throw exex("Could not instantiate class {0} using builder type {1}. Builder must have a build(), create(), or get() method that returns {0}. The method may have @Inject annotation to allow injected parameters.", beanSubType.getName(), builderTypeInfo != null ? builderTypeInfo.getName() : builder2.getClass().getName());
			} else {
				// Builder type is invalid and was auto-detected - fall through to factory methods/constructors
				log("Builder detected but builder type is invalid. Builder type: %s. Falling back to factory methods/constructors.", builder2.getClass().getName());
			}
		}

		// Look for Bean.factoryMethod().
		log("Attempting Bean.factoryMethod()");
		// If builder was detected but has no build method, pass it as extra bean for factory methods
		Object[] factoryMethodExtraBeans = builder2 != null ? new Object[]{builder2} : new Object[0];
		bean = beanSubType.getPublicMethods().stream()
			.filter(x -> x.isAll(STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
			.filter(x -> factoryMethodNames.contains(x.getNameSimple()))
			.filter(x -> x.hasReturnType(beanSubType))
			.filter(x -> x.canResolveAllParameters(store2, factoryMethodExtraBeans))
			.sorted(methodComparator)
			.findFirst()
			.map(x -> {
				log("Found factory method: %s", x.getNameFull());
				return (T)beanType.cast(x.inject(store2, null, factoryMethodExtraBeans));
			})
			.orElse(null);

		// Look for Bean().
		if (bean == null) {
			log("Attempting Bean() constructor");
			// If builder was detected but has no build method, pass it as extra bean for constructors
			Object[] constructorExtraBeans = builder2 != null ? new Object[]{builder2} : new Object[0];
			bean = beanSubType.getPublicConstructors().stream()
				.filter(x -> x.isAll(NOT_DEPRECATED))
				.filter(x -> x.isDeclaringClass(beanSubType))
				.filter(x -> x.canResolveAllParameters(store2, enclosingInstance, constructorExtraBeans))
				.sorted(constructorComparator)
				.findFirst()
				.map(x -> {
					log("Found constructor: %s", x.getNameFull());
					return (T)beanType.cast(x.inject(store2, enclosingInstance, constructorExtraBeans));
				})
				.orElse(null);
		}

		if (bean != null) {
			log("Bean created successfully");
			bean = inject(bean);
			runPostCreateHooks(bean);
			return bean;
		}

		if (beanSubType.isInterface()) {
			log("Bean type is an interface");
			if (fallbackSupplier != null) {
				log("Using fallback supplier");
				T fallbackBean = fallbackSupplier.get();
				runPostCreateHooks(fallbackBean);
				return fallbackBean;
			}
			throw exex("Could not instantiate class {0}: {1}.", beanSubType.getName(), "Class is an interface");
		}

		if (beanSubType.isAbstract()) {
			log("Bean type is abstract");
			if (fallbackSupplier != null) {
				log("Using fallback supplier");
				T fallbackBean = fallbackSupplier.get();
				runPostCreateHooks(fallbackBean);
				return fallbackBean;
			}
			throw exex("Could not instantiate class {0}: {1}.", beanSubType.getName(), "Class is abstract");
		}

		if (fallbackSupplier != null) {
			log("Using fallback supplier");
			T fallbackBean = fallbackSupplier.get();
			runPostCreateHooks(fallbackBean);
			return fallbackBean;
		}

		log("Failed to create bean: no methods/constructors found with matching parameters");
		throw exex("Could not instantiate class {0}:  No methods/constructors found with matching parameters", beanSubType.getName());
	}

	/**
	 * Finds all bean subtypes, including beanSubType, beanType, and all classes in between.
	 *
	 * <p>
	 * This method traverses the hierarchy from {@code beanSubType} up to {@code beanType}, collecting all classes
	 * in the inheritance chain. This allows parent bean types to be resolved as injectable types.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Class hierarchy</jc>
	 * 	<jk>class</jk> A { }
	 * 	<jk>class</jk> B <jk>extends</jk> A { }
	 * 	<jk>class</jk> C <jk>extends</jk> B { }
	 *
	 * 	<jc>// When creating C with beanType=A, beanSubType=C</jc>
	 * 	<jc>// This method returns: [C, B, A]</jc>
	 * </p>
	 *
	 * @return A list of bean subtypes from beanSubType to beanType, never <jk>null</jk>.
	 */
	private List<ClassInfo> findBeanSubTypes() {
		var result = new ArrayList<ClassInfo>();

		// If beanSubType is the same as beanType, just return a single-element list
		if (beanSubType.is(beanType.inner())) {
			result.add(beanSubType);
			return result;
		}

		// Traverse from beanSubType up to beanType
		var parents = beanSubType.getParents();
		for (var parent : parents) {
			result.add(parent);
			// Stop when we reach beanType
			if (parent.is(beanType.inner())) {
				break;
			}
		}

		// This should never happen if beanSubType validation is correct, but check anyway
		if (result.isEmpty() || !result.get(result.size() - 1).is(beanType.inner())) {
			throw illegalArg("beanType {0} was not found in the parent hierarchy of beanSubType {1}. This indicates a validation error.", beanType.getName(), beanSubType.getName());
		}

		return result;
	}

	private Object findBuilder() {

		var bs = store;
		var builderType2 = this.builderType.get();

		// If no builder type was determined, return null (builder not needed)
		if (builderType2 == null)
			return null;

		Optional<Object> r;

		// Step 1: Look for a static create/builder method that returns the builder type
		r = beanSubType.getPublicMethods().stream()
			.filter(x -> x.isAll(STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
			.filter(x -> builderMethodNames.contains(x.getNameSimple()))
			.filter(x -> x.hasReturnType(builderType2))
			.filter(x -> x.canResolveAllParameters(bs, enclosingInstance))
			.sorted(comparing(MethodInfo::getParameterCount).reversed())
			.findFirst()
			.map(x -> inject(x.inject(bs, enclosingInstance)));
		if (r.isPresent())
			return r.get();

		// Step 2: Look for a public constructor on the builder type
		r = builderType2.getPublicConstructors().stream()
			.filter(x -> x.isAll(NOT_DEPRECATED))
			.filter(x -> x.canResolveAllParameters(bs, enclosingInstance))
			.sorted(comparing(ConstructorInfo::getParameterCount).reversed())
			.findFirst()
			.map(x -> inject(x.inject(bs, enclosingInstance)));

		if (r.isPresent())
			return r.get();

		// Step 3: Look for a protected constructor on the builder type
		r = builderType2.getDeclaredConstructors().stream()
			.filter(x -> x.isAll(ElementFlag.PROTECTED, NOT_DEPRECATED))
			.filter(x -> x.canResolveAllParameters(bs, enclosingInstance))
			.sorted(comparing(ConstructorInfo::getParameterCount).reversed())
			.findFirst()
			.map(x -> inject(x.inject(bs, enclosingInstance)));

		return r.orElse(null);
	}

	/**
	 * Finds the builder type based on priority:
	 * 1. Explicitly set builderType
	 * 2. @Builder annotation on beanSubType (includes inherited annotations)
	 * 3. Autodetect from static methods or inner classes
	 */
	@SuppressWarnings("unchecked")
	private ClassInfo findBuilderType() {
		log("Finding builder type...");

		// Priority 1: Explicitly set builderType
		if (explicitBuilderType != null) {
			log("Using explicitly set builder type: %s", explicitBuilderType.getName());
			return explicitBuilderType;
		}

		// Priority 2: @Builder annotation on beanSubType
		// Check declared annotations first to ensure child's annotation overrides parent's
		var r = beanSubType.getDeclaredAnnotations().stream()
			.filter(a -> a.isType(Builder.class))
			.map(a -> (AnnotationInfo<Builder>)a)
			.map(x -> ClassInfo.class.cast(info(x.inner().value())))
			.findFirst();
		if (r.isPresent()) {
			log("Found builder via @Builder annotation on beanSubType: %s", r.get().getName());
			return r.get();
		}
		// Fall back to inherited annotations if no declared annotation found
		r = beanSubType.getAnnotations(Builder.class)
			.map(x -> ClassInfo.class.cast(info(x.inner().value())))
			.findFirst();
		if (r.isPresent()) {
			log("Found builder via @Builder annotation (inherited): %s", r.get().getName());
			return r.get();
		}

		// Priority 3: Autodetect
		// 3a: Look for static create/builder method that returns a potential builder type
		r = beanSubType.getPublicMethods().stream()
			.filter(x -> x.isAll(STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
			.filter(x -> builderMethodNames.contains(x.getNameSimple()))
			.filter(x -> ! x.hasReturnType(beanSubType)) // Must not return the bean type itself
			.filter(x -> isValidBuilderType(x.getReturnType()))
			.findFirst()
			.map(MethodInfo::getReturnType);
		if (r.isPresent()) {
			log("Found builder via static method: %s", r.get().getName());
			return r.get();
		}

		// 3b: Look for inner Builder class
		log("Looking for inner builder class (names: %s)...", builderClassNames);
		r = beanSubType.getDeclaredMemberClasses().stream()
			.filter(x -> builderClassNames.contains(x.getNameSimple()))
			.findFirst();
		if (r.isPresent()) {
			var builderClass = r.get();
			if (isValidBuilderType(builderClass)) {
				log("Found builder via inner class: %s", builderClass.getName());
				return builderClass;
			}
			// Still return it so we can provide a better error message when trying to use it
			log("Found builder class via inner class but it is not valid: %s", builderClass.getName());
			return builderClass;
		}

		// 3c: Look for builder in parent classes (skip the first element which is beanSubType itself)
		var parentClasses = beanSubType.getParents();
		for (int i = 1; i < parentClasses.size(); i++) {
			var parentClass = parentClasses.get(i);
			r = parentClass.getDeclaredMemberClasses().stream()
				.filter(x -> builderClassNames.contains(x.getNameSimple()))
				.findFirst();
			if (r.isPresent()) {
				var builderClass = r.get();
				if (isValidBuilderType(builderClass)) {
					log("Found builder via parent inner class: %s", builderClass.getName());
					return builderClass;
				}
				// Still return it so we can provide a better error message when trying to use it
				log("Found builder class via parent inner class but it is not valid: %s", builderClass.getName());
				return builderClass;
			}
		}

		// No builder type found
		log("No builder type found");
		return null;
	}

	/**
	 * Finds all builder types, including the primary builder type and any builder types found in the builder's parent hierarchy.
	 *
	 * <p>
	 * This method traverses the parent hierarchy of the primary builder type, looking for builder classes whose build methods
	 * return types that are the same as or parents of {@code beanType}. This allows parent builders to be resolved as injectable types.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Class A with builder</jc>
	 * 	<jk>class</jk> A {
	 * 		<jk>static class</jk> Builder {
	 * 			A create() { <jk>return new</jk> A(); }
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Class B extends A with builder</jc>
	 * 	<jk>class</jk> B <jk>extends</jk> A {
	 * 		<jk>static class</jk> Builder <jk>extends</jk> A.Builder {
	 * 			B create() { <jk>return new</jk> B(); }
	 * 		}
	 * 	}
	 *
	 * 	<jc>// When creating B, this method returns: [B.Builder, A.Builder]</jc>
	 * 	<jc>// Traverses B.Builder → A.Builder hierarchy</jc>
	 * 	<jc>// B.Builder.create() returns B (matches beanType)</jc>
	 * 	<jc>// A.Builder.create() returns A (A is parent of B)</jc>
	 * </p>
	 *
	 * @return A list of builder types, never <jk>null</jk>.
	 */
	private List<ClassInfo> findBuilderTypes() {
		var result = new ArrayList<ClassInfo>();

		// Start with the primary builder type
		var primaryBuilderType = findBuilderType();
		if (primaryBuilderType == null) {
			return result;
		}

		// Add the primary builder type first
		result.add(primaryBuilderType);

		// Traverse the parent hierarchy of the builder type itself (skip the first element which is the primary builder type itself)
		var builderParents = primaryBuilderType.getParents();
		for (int i = 0; i < builderParents.size(); i++) {
			var builderParent = builderParents.get(i);
			builderParent.getPublicMethods().stream()
				.filter(m -> m.isAll(NOT_STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
				.filter(m -> buildMethodNames.contains(m.getNameSimple()))
				.filter(m -> m.getReturnType().isParentOf(beanType))
				.findFirst()
				.ifPresent(x -> result.add(builderParent));
		}

		return result;
	}

	/**
	 * Helper method to inject beans into an object instance.
	 *
	 * @param <T> The bean type.
	 * @param bean The object to inject beans into.
	 * @return The same bean instance (for method chaining).
	 */
	private <T2> T2 inject(T2 bean) {
		return ClassInfo.of(bean).inject(bean, store);
	}

	/**
	 * Checks if a type can be used as a builder.
	 * A valid builder type must have:
	 * - A build/create/get method that returns the bean type or a parent of the bean type
	 *   - The method may have @Inject annotation to allow injected parameters
	 *   - If no @Inject annotation, the method must have no parameters
	 */
	private boolean isValidBuilderType(ClassInfo builderCandidate) {
		log("Validating builder candidate: %s", builderCandidate.getName());

		// Check if it has a build/create/get method that returns beanSubType, beanType, or a parent of beanSubType
		var hasBuildMethod = builderCandidate.getPublicMethods().stream()
			.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
			.filter(x -> buildMethodNames.contains(x.getNameSimple()))
			.filter(x -> {
				var returnType = x.getReturnType();
				return returnType.is(beanSubType.inner()) || returnType.is(beanType.inner()) || returnType.isParentOf(beanSubType);
			})
			.anyMatch(x -> x.getAnnotations().stream().map(AnnotationInfo::getNameSimple).anyMatch(n -> eqAny(n, "Inject", "Autowired")) || x.getParameterCount() == 0);

		if (hasBuildMethod) {
			log("Builder is valid: has build/create/get method returning bean type");
			return true;
		}

		log("Builder is NOT valid: no build/create/get method returning bean type");
		return false;
	}

	private void log(String message, Object... args) {
		// Log to Logger at FINE level
		logger.fine(beanType.getName() + ": " + message, args);
		// Also add to debug log if enabled
		debug.ifPresent(x -> x.add(args.length == 0 ? message : String.format(message, args)));
	}

	private T runPostCreateHooks(T bean) {
		postCreateHooks.forEach(x -> x.accept(bean));
		return bean;
	}
}