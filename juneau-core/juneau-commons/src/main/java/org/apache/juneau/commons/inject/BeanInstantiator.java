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
 * 	<li class='jm'>{@link BeanInstantiator#of(Class)}
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
 * 	<jc>// Create bean using BeanInstantiator</jc>
 * 	MyBean <jv>bean</jv> = BeanInstantiator
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
 * 			<li>Falls back to a protected constructor with parameters that can be resolved from the bean store.
 * 			<li>Falls back to a package-private constructor with parameters that can be resolved from the bean store.
 * 				Private constructors are deliberately excluded.
 * 		</ol>
 * </ol>
 *
 * <h5 class='section'>Builder Detection:</h5>
 * <p>
 * Builders are detected in the following priority order:
 * <ol class='spaced-list'>
 * 	<li>Explicitly set via {@link Builder#builder(Object)} or {@link Builder#builder(Class)}.
 * 	<li>{@link org.apache.juneau.commons.annotation.Builder @Builder} annotation on the bean subtype.
 * 	<li>{@link org.apache.juneau.commons.annotation.Builder @Builder} annotation on the bean type.
 * 	<li>Auto-detection:
 * 		<ul>
 * 			<li>Static <c>create()</c> or <c>builder()</c> method that returns a builder type
 * 				(method names can be customized via {@link Builder#builderMethodNames(String...)}).
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
 * By default, each call to {@link #run()} creates a new bean instance. To enable caching, call {@link Builder#cached()}.
 * When caching is enabled, bean instances are cached using {@link Memoizer}, so multiple calls to {@link #run()}
 * will return the same instance unless {@link #reset()} is called or configuration changes. Explicit implementations set via
 * {@link Builder#impl(Object)} and explicit builder instances set via {@link Builder#builder(Object)} are preserved
 * across resets.
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * Configuration is performed on the inner {@link Builder} which is <b>not</b> thread-safe. Configure on a single
 * thread, then call {@link Builder#build()} to produce an immutable {@link BeanInstantiator} that is safe for
 * shared concurrent use. The {@link Builder#run()} shorthand is equivalent to {@code build().run()} and is
 * appropriate for one-shot bean creation from a single thread.
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
 * 	<li class='note'>Deprecated and <a class="doclink" href="https://juneau.apache.org/site/apidocs/org/apache/juneau/commons/bean/BeanIgnore.html">@BeanIgnore</a>-annotated methods/constructors are ignored.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicBeanStore}
 * </ul>
 *
 * @param <T> The bean type being created.
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class BeanInstantiator<T> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_beanType = "beanType";
	private static final String ARG_fallback = "fallback";
	private static final String ARG_hook = "hook";
	private static final String ARG_names = "names";
	private static final String ARG_value = "value";

	private static final String CONST_usingFallbackSupplier = "Using fallback supplier";

	/** Default factory method names used for bean instantiation. */
	protected static final Set<String> DEFAULT_FACTORY_METHOD_NAMES = u(set("getInstance"));

	/** Default builder method names used for builder factory methods (static methods that create/return a builder). */
	protected static final Set<String> DEFAULT_BUILDER_METHOD_NAMES = u(set("create", "builder"));

	/** Default build method names used for builder build methods (instance methods on builder that build/return the bean). */
	protected static final Set<String> DEFAULT_BUILD_METHOD_NAMES = u(set("build", "create", "get"));

	/** Default builder class names for auto-detection. */
	protected static final Set<String> DEFAULT_BUILDER_CLASS_NAMES = u(set("Builder"));

	private static final Logger logger = Logger.getLogger(BeanInstantiator.class);

	/**
	 * Lifecycle scope for the bean being created.
	 *
	 * <p>
	 * Maps to {@link Builder#cached()} so that {@link Builder#scope(Scope)} provides a more declarative alternative.
	 *
	 * @see Builder#scope(Scope)
	 */
	public enum Scope {
		/** A new bean instance is produced on every call to {@link Builder#run()}. */
		PROTOTYPE,
		/** A single bean instance is created and reused for the lifetime of the builder. */
		SINGLETON
	}

	/**
	 * Creates a new bean creator builder.
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @return A new bean creator builder.
	 */
	public static <T> Builder<T> of(Class<T> beanType) {
		return new Builder<>(beanType, null, null, null);
	}

	/**
	 * Creates a new bean creator builder with a parent bean store.
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @param parentStore The parent bean store to use for resolving dependencies. Can be <jk>null</jk>.
	 * @return A new bean creator builder.
	 */
	public static <T> Builder<T> of(Class<T> beanType, BeanStore parentStore) {
		return new Builder<>(beanType, parentStore, null, null);
	}

	/**
	 * Creates a new bean creator builder with a parent bean store, name, and enclosing instance.
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @param parentStore The parent bean store to use for resolving dependencies. Can be <jk>null</jk>.
	 * @param name The bean name. Can be <jk>null</jk>.
	 * @param enclosingInstance The enclosing instance object. Can be <jk>null</jk>.
	 * @return A new bean creator builder.
	 */
	public static <T> Builder<T> of(Class<T> beanType, BeanStore parentStore, String name, Object enclosingInstance) {
		return new Builder<>(beanType, parentStore, name, enclosingInstance);
	}

	/**
	 * Instantiates a bean of the given type, or returns <jk>null</jk> if the type is <jk>null</jk> or instantiation fails.
	 *
	 * <p>
	 * Shorthand for the common pattern:
	 * <p class='bjava'>
	 * 	<jv>cls</jv> == <jk>null</jk> ? <jk>null</jk> : BeanInstantiator.<jsm>of</jsm>(<jv>cls</jv>).fallback(() -> <jk>null</jk>).run()
	 * </p>
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.  Can be <jk>null</jk>.
	 * @return A new bean instance, or <jk>null</jk> if the type is <jk>null</jk> or instantiation fails.
	 */
	public static <T> T createOrNull(Class<T> beanType) {
		return beanType == null ? null : of(beanType).fallback(() -> null).run();
	}

	/**
	 * Instantiates a bean of the given type and wraps it in an {@link Optional}, or returns {@link Optional#empty()}
	 * if the type is <jk>null</jk> or instantiation fails.
	 *
	 * <p>
	 * Shorthand for the common pattern:
	 * <p class='bjava'>
	 * 	<jv>cls</jv> == <jk>null</jk> ? Optional.empty() : BeanInstantiator.<jsm>of</jsm>(<jv>cls</jv>).asOptional()
	 * </p>
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.  Can be <jk>null</jk>.
	 * @return An {@link Optional} containing the created bean, or {@link Optional#empty()} if the type is <jk>null</jk> or instantiation fails.
	 */
	public static <T> Optional<T> optionalOf(Class<T> beanType) {
		return beanType == null ? Optional.empty() : of(beanType).asOptional();
	}

	/**
	 * Instantiates a bean of the given type, or returns a default value if the type is <jk>null</jk>.
	 *
	 * <p>
	 * Shorthand for the common pattern:
	 * <p class='bjava'>
	 * 	<jv>cls</jv> == <jk>null</jk> ? <jv>defaultValue</jv> : BeanInstantiator.<jsm>of</jsm>(<jv>cls</jv>).run()
	 * </p>
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.  Can be <jk>null</jk>.
	 * @param defaultValue The value to return if {@code beanType} is <jk>null</jk>.  Can be <jk>null</jk>.
	 * @return A new bean instance, or {@code defaultValue} if the type is <jk>null</jk>.
	 */
	public static <T> T createOrDefault(Class<T> beanType, T defaultValue) {
		return beanType == null ? defaultValue : of(beanType).run();
	}

	// =========================================================================
	// Outer (immutable) wrapper state and methods
	// =========================================================================

	private final Builder<T> b;

	BeanInstantiator(Builder<T> b) {
		this.b = b;
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
		return b.runImpl();
	}

	/**
	 * Attempts to create the bean, returning an {@link Optional} instead of throwing an exception on failure.
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
	 * Converts this creator into a memoizer.
	 *
	 * @return A resettable supplier that caches the created bean until reset.
	 */
	public Memoizer<T> asMemoizer() {
		return new Memoizer<>(this::run);
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
	 * Resets the cached bean instance and the inner Builder's resolved memoizers.
	 *
	 * @return This object.
	 */
	public BeanInstantiator<T> reset() {
		b.reset();
		return this;
	}

	/**
	 * Returns the name of the bean being created.
	 *
	 * @return The bean name, or <jk>null</jk> if not set.
	 */
	public String getName() {
		return b.name;
	}

	/**
	 * Returns the bean subtype class info if specified, or the normal bean type if not.
	 *
	 * @return The bean subtype class info, or the bean type class info if no subtype was specified.
	 */
	protected ClassInfo getBeanSubType() {
		return b.beanSubType;
	}

	/**
	 * Returns the list of bean subtype classes, including the bean subtype, bean type, and all classes in between.
	 *
	 * @return A list of bean subtype class info objects, never <jk>null</jk>.
	 */
	protected List<ClassInfo> getBeanSubTypes() {
		return b.getBeanSubTypes();
	}

	/**
	 * Returns the builder instance if one has been created or specified.
	 *
	 * @param <B> The builder type.
	 * @return The builder instance wrapped in an {@link Optional}, or an empty optional if no builder exists.
	 */
	public <B> Optional<B> getBuilder() {
		return b.getBuilder();
	}

	/**
	 * Returns the builder type class info determined by builder type detection.
	 *
	 * @return The builder type class info, or <jk>null</jk> if no builder type was determined.
	 */
	protected ClassInfo getBuilderType() {
		return b.getBuilderType();
	}

	/**
	 * Returns the list of builder types, including the primary builder type and any builder types found in the builder's parent hierarchy.
	 *
	 * @return A list of builder type class info objects, never <jk>null</jk>.
	 */
	protected List<ClassInfo> getBuilderTypes() {
		return b.getBuilderTypes();
	}

	/**
	 * Returns an unmodifiable list of debug log entries from the last bean creation attempt.
	 *
	 * @return An unmodifiable list of debug log entries.
	 */
	public List<String> getDebugLog() {
		return b.getDebugLog();
	}

	// =========================================================================
	// Builder (mutable, single-thread configuration) inner class
	// =========================================================================

	/**
	 * Builder for {@link BeanInstantiator}.
	 *
	 * <p>
	 * Holds all mutable configuration. Configure on a single thread, then call {@link #build()} to obtain
	 * an immutable {@link BeanInstantiator} suitable for shared concurrent use. Most call sites use the
	 * {@link #run()} shorthand which is equivalent to {@code build().run()}.
	 *
	 * <h5 class='section'>Thread Safety:</h5>
	 * <p>
	 * Builder instances are <b>not</b> thread-safe. Configure on one thread, then either call {@link #build()}
	 * to produce a thread-safe immutable {@link BeanInstantiator}, or call {@link #run()} to one-shot create a
	 * bean from a single thread.
	 *
	 * @param <T> The bean type being created.
	 */
	public static class Builder<T> {

	final BeanStore parentStore;
	@SuppressWarnings({
		"resource" // transient build-time scratch store; lifetime is bounded by the Builder itself, no foreign resources are captured
	})
	final BasicBeanStore store;
	final ClassInfoTyped<T> beanType;
	final NullableReference<List<String>> debug = NullableReference.empty();

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
	private boolean factoryAbstainOnNull = false;
	private boolean preferZeroArgConstructor = false;
	private boolean silent = false;
	private String description = null;
	private List<UnaryOperator<T>> wrappers = new ArrayList<>();
	private List<Validation<T>> validators = new ArrayList<>();
	private List<Builder<? extends T>> alternatives = new ArrayList<>();
	private List<Consumer<Object>> builderInitializers = new ArrayList<>();
	private boolean injectBuilder = false;
	private boolean autoWireBuilder = false;
	private boolean autoWireUnwrapSuppliers = true;
	private boolean noBuilder = false;

	private Memoizer<ClassInfo> builderType = memoizer(() -> findBuilderType());
	private Memoizer<List<ClassInfo>> builderTypes = memoizer(() -> findBuilderTypes());
	private Memoizer<List<ClassInfo>> beanSubTypes = memoizer(() -> findBeanSubTypes());
	private Memoizer<Object> builderMemoizer = memoizer(() -> findBuilder());
	private Memoizer<T> beanImpl = memoizer(() -> processBean(findBeanImpl()));

	/**
	 * Constructor.
	 *
	 * @param beanType The bean type being created.
	 * @param parentStore The parent bean store to use for resolving dependencies. Can be <jk>null</jk>.
	 * @param name The bean name. Can be <jk>null</jk>.
	 * @param enclosingInstance The enclosing instance object. Can be <jk>null</jk>.
	 */
	protected Builder(Class<T> beanType, BeanStore parentStore, String name, Object enclosingInstance) {
		this.beanType = info(assertArgNotNull(ARG_beanType, beanType));
		this.beanSubType = this.beanType;
		this.parentStore = parentStore;
		this.store = new BasicBeanStore(this.parentStore);
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
		store.add(type, bean);
		reset();
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
	public <T2> Builder<T> addBean(Class<T2> type, T2 bean) {
		store.add(type, bean);
		reset();
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
	public <T2> Builder<T> addBean(Class<T2> type, T2 bean, String name) {
		store.add(type, bean, name);
		reset();
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
	 * 	<li class='note'>The created bean is cached via {@link Memoizer}, so subsequent calls return the same instance
	 * 		unless {@link #reset()} is called or configuration changes.
	 * 	<li class='note'>Post-creation hooks are still executed if creation succeeds.
	 * 	<li class='note'>Other exceptions (e.g., from post-creation hooks or fallback suppliers) are NOT caught
	 * 		and will propagate to the caller.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Try to create, use default if it fails</jc>
	 * 	MyBean <jv>bean</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.asOptional()
	 * 		.orElse(<jk>new</jk> DefaultMyBean());
	 *
	 * 	<jc>// Chain multiple creation attempts</jc>
	 * 	MyService <jv>service</jv> = BeanInstantiator.<jsm>of</jsm>(AdvancedService.<jk>class</jk>, <jv>store</jv>)
	 * 		.asOptional()
	 * 		.or(() -&gt; BeanInstantiator.<jsm>of</jsm>(BasicService.<jk>class</jk>, <jv>store</jv>)
	 * 			.asOptional())
	 * 		.orElseGet(() -&gt; <jk>new</jk> FallbackService());
	 *
	 * 	<jc>// Check if optional feature is available</jc>
	 * 	Optional&lt;OptionalFeature&gt; <jv>feature</jv> = BeanInstantiator
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
		return build().asOptional();
	}

	/**
	 * Converts this creator into a memoizer.
	 *
	 * <p>
	 * A {@link Memoizer} caches the result of the first {@link Memoizer#get()} call
	 * and returns that cached value for subsequent calls. The {@link Memoizer#reset()} method
	 * can be used to clear the cache, forcing the bean to be recreated on the next get() call.
	 *
	 * <p>
	 * This is particularly useful when the bean store contents change and you want to recreate the bean
	 * with updated dependencies.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The returned {@link Memoizer} is thread-safe.
	 * 	<li class='note'>Each call to {@link Memoizer#get()} after a reset() will invoke
	 * 		{@link #run()}, which may trigger dependency injection and post-creation hooks.
	 * 	<li class='note'>The memoizer inherits all Optional-like methods from {@link NullableSupplier}.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a memoizer for a bean with dependencies</jc>
	 * 	BeanStore <jv>store</jv> = <jk>new</jk> BasicBeanStore(<jk>null</jk>);
	 * 	<jv>store</jv>.addBean(String.<jk>class</jk>, <js>"initial"</js>);
	 *
	 * 	Memoizer&lt;MyBean&gt; <jv>supplier</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>store</jv>)
	 * 		.asMemoizer();
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
	public Memoizer<T> asMemoizer() {
		return build().asMemoizer();
	}

	/**
	 * Converts this creator into a supplier.
	 *
	 * @return A supplier that returns the results of the {@link #run()} method.
	 */
	public Supplier<T> asSupplier() {
		return build().asSupplier();
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
	public Builder<T> type(Class<? extends T> value) {
		assertArgNotNull(ARG_value, value);
		beanSubType = info(value);
		assertArg(beanType.isAssignableFrom(beanSubType), "type must be a subclass of beanType. beanType={0}, type={1}", cn(beanType), cn(beanSubType));
		reset();
		return this;
	}

	/**
	 * Specifies a subclass of the bean type to create, via a {@link ClassInfo} reference.
	 *
	 * <p>
	 * Convenience overload of {@link #type(Class)} that accepts {@link ClassInfo} (or any subtype such
	 * as {@code ClassMeta}).  The underlying class is extracted via {@link ClassInfo#inner()} and the
	 * subtype-relationship check happens at runtime.
	 *
	 * @param value The subclass type to create.  Cannot be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or its underlying class is not a subclass of {@code beanType}.
	 */
	@SuppressWarnings({
		"unchecked" // Reflective builder lookup returns Object; cast is safe by construction.
	})
	public Builder<T> type(ClassInfo value) {
		assertArgNotNull(ARG_value, value);
		return type((Class<? extends T>) value.inner());
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
 * 	<li>A package-private constructor on the builder type with parameters that can be resolved from the bean store.
 * 		Private constructors are deliberately excluded.
 * </ol>
	 *
	 * @param value The builder type. Cannot be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If the builder type is invalid (does not have a valid build/create/get method).
	 */
	public Builder<T> builder(Class<?> value) {
		explicitBuilderType = info(assertArgNotNull(ARG_value, value));
		builderType.set(explicitBuilderType);
		assertArg(isValidBuilderType(explicitBuilderType), "Invalid builder type {0} for bean type {1}. Builder must have a build(), create(), or get() method that returns {1} (or a parent of {1}). The method may have @Inject annotation to allow injected parameters; otherwise, it must have no parameters.", cn(explicitBuilderType), cn(beanType));
		builderTypes.get();  // Triggers validation on type hierarchy.
		reset();
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
	public Builder<T> builder(Object value) {
		builder(value.getClass());
		explicitBuilder = assertArgNotNull(ARG_value, value);
		reset();
		return this;
	}

	/**
	 * Specifies custom class names to look for when auto-detecting builder inner classes.
	 *
	 * <p>
	 * By default, {@code BeanInstantiator} looks for inner classes named {@code "Builder"}.
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
	 * 	MyBean <jv>bean</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.builderClassNames(<js>"BuilderImpl"</js>, <js>"Factory"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param names The builder class names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public Builder<T> builderClassNames(String... names) {
		builderClassNames = set(assertArgNoNulls(ARG_names, names));
		reset();
		return this;
	}

	/**
	 * Specifies custom method names to look for when auto-detecting builder factory methods.
	 *
	 * <p>
	 * By default, {@code BeanInstantiator} looks for static methods named {@code "create"} or {@code "builder"}
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
	 * 	MyBean <jv>bean</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.builderMethodNames(<js>"newBuilder"</js>, <js>"instance"</js>)
		.run();
	 * </p>
	 *
	 * @param names The builder factory method names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public Builder<T> builderMethodNames(String... names) {
		builderMethodNames = set(assertArgNoNulls(ARG_names, names));
		reset();
		return this;
	}

	/**
	 * Specifies custom method names to look for when calling build methods on builder instances.
	 *
	 * <p>
	 * By default, {@code BeanInstantiator} looks for instance methods named {@code "build"}, {@code "create"}, or {@code "get"}
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
	 * 	MyBean <jv>bean</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.buildMethodNames(<js>"execute"</js>, <js>"make"</js>)
		.run();
	 * </p>
	 *
	 * @param names The build method names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public Builder<T> buildMethodNames(String... names) {
		buildMethodNames = set(assertArgNoNulls(ARG_names, names));
		reset();
		return this;
	}

	/**
	 * Enables caching mode.
	 *
	 * <p>
	 * When caching mode is enabled, bean instances are cached using {@link Memoizer},
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
	 * 	<jk>var</jk> <jv>creator</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.<jsm>cached</jsm>();
	 * 	MyBean <jv>bean1</jv> = <jv>creator</jv>.<jsm>run</jsm>();
	 * 	MyBean <jv>bean2</jv> = <jv>creator</jv>.<jsm>run</jsm>();
	 * 	<jc>// bean1 and bean2 are the same instance</jc>
	 * </p>
	 *
	 * @return This object.
	 */
	public Builder<T> cached() {
		cached = true;
		return this;
	}

	/**
	 * Treat a {@code null} return value from a static factory method as a deliberate "abstain" signal.
	 *
	 * <p>
	 * By default, when a static factory method (one of {@link #factoryMethodNames(String...)}) matches and is
	 * invoked, but returns {@code null}, {@link #run()} falls through to constructor lookup as if the factory
	 * method weren't there. This works well when the factory method is just an alternate construction path.
	 *
	 * <p>
	 * Some factory-method patterns instead use a {@code null} return value as a deliberate "this implementation
	 * does not handle the input — try the next strategy" signal, expecting the caller to interpret the {@code null}
	 * as a definitive answer. The classic example is a {@code RestOpArg} subclass whose
	 * {@code static create(ParameterInfo)} returns {@code null} when the parameter isn't annotated with the
	 * marker the subclass handles, so the caller can move on to the next subclass in the chain. Falling through
	 * to the (always-present) constructor in that case would silently materialize an arg that doesn't apply.
	 *
	 * <p>
	 * When this flag is enabled, a {@code null} return from the matching factory method is propagated out of
	 * {@link #run()} unchanged — constructor lookup is skipped. This matches legacy {@code BeanCreator}
	 * semantics for static {@code create()} / {@code builder()} / {@code getInstance()} methods.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// AttributeArg.create(ParameterInfo) returns null if @Attr is not present.</jc>
	 * 	<jc>// Without factoryAbstainOnNull, BeanInstantiator would then invoke the AttributeArg(ParameterInfo)</jc>
	 * 	<jc>// constructor and return a bogus instance.</jc>
	 * 	RestOpArg <jv>arg</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(RestOpArg.<jk>class</jk>, <jv>store</jv>)
	 * 		.type(AttributeArg.<jk>class</jk>)
	 * 		.factoryMethodNames(<js>"getInstance"</js>, <js>"create"</js>)
	 * 		.factoryAbstainOnNull()
	 * 		.run();
	 * </p>
	 *
	 * @return This object.
	 */
	public Builder<T> factoryAbstainOnNull() {
		factoryAbstainOnNull = true;
		return this;
	}

	/**
	 * Prefer a zero-argument constructor when one exists, instead of injecting via the longest constructor.
	 *
	 * <p>
	 * By default, {@code BeanInstantiator} sorts public constructors by parameter count <i>descending</i>: the
	 * constructor with the most resolvable parameters wins, encouraging full constructor injection. This is the
	 * right default for most beans, but it doesn't fit every use case.
	 *
	 * <p>
	 * The classic mismatch is parameterized container types like {@link java.util.TreeMap}, which expose:
	 * <ul>
	 * 	<li>{@code TreeMap()}
	 * 	<li>{@code TreeMap(Comparator)}
	 * 	<li>{@code TreeMap(Map<? extends K, ? extends V>)}
	 * 	<li>{@code TreeMap(SortedMap)}
	 * </ul>
	 * The default selection picks the {@code Map} copy-constructor — but that {@code Map<? extends K, ? extends V>}
	 * parameter doesn't satisfy v2 inject-collection auto-resolution (which only handles {@code Map<String, T>}),
	 * so injection fails at runtime even though {@link ParameterInfo#canResolve(BeanStore, Object...)} optimistically
	 * said yes. Callers that just want "give me a fresh empty container" should use this flag.
	 *
	 * <p>
	 * When this flag is enabled, if {@code beanSubType} declares a public no-arg constructor, that constructor is
	 * used unconditionally and other constructors are ignored. This matches legacy {@code BeanCreator} behavior for
	 * the same use case (legacy filtered out the multi-arg ctors via {@code hasAllParams} returning false, since the
	 * legacy bean store didn't auto-resolve raw {@code Map}/{@code Collection} parameter types).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Instantiating an arbitrary Map subclass — prefer the no-arg ctor over copy-ctors.</jc>
	 * 	Map&lt;?,?&gt; <jv>m</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(Map.<jk>class</jk>)
	 * 		.type(treeMapClass)
	 * 		.preferZeroArgConstructor()
	 * 		.run();
	 * </p>
	 *
	 * @return This object.
	 */
	public Builder<T> preferZeroArgConstructor() {
		preferZeroArgConstructor = true;
		return this;
	}

	/**
	 * Compiles this builder into a {@link BeanInstantiator}.
	 *
	 * <p>
	 * The returned instance exposes only run/read methods (no setters). The compiled instance shares
	 * its caching state with this builder so that {@link #cached()} memoization is consistent across
	 * both compiled and shorthand call paths.
	 *
	 * @return A new {@link BeanInstantiator} backed by this builder's configuration.
	 */
	public BeanInstantiator<T> build() {
		return new BeanInstantiator<>(this);
	}

	/**
	 * Shortcut for {@code build().run()}.
	 *
	 * <p>
	 * Most call sites do not store the {@link BeanInstantiator} and therefore call this shortcut.
	 * Repeated calls share caching with any compiled {@link BeanInstantiator} produced by {@link #build()}.
	 *
	 * @return A new bean with all dependencies injected.
	 * @throws ExecutableException if bean could not be created.
	 */
	public T run() {
		return runImpl();
	}

	T runImpl() {
		debug.ifPresent(x -> x.clear());

		try {
			return runImplOnce();
		} catch (RuntimeException e) {
			if (alternatives.isEmpty())
				throw e;
			for (var alt : alternatives) {
				try {
					return processBean(alt.runImpl());
				} catch (@SuppressWarnings("unused") RuntimeException ignored) {
					// Try next alternative
				}
			}
			throw e;
		}
	}

	private T runImplOnce() {
		if (neq(beanSubType, beanType))
			log("Subtype specified: %s", beanSubType.getName());

		if (explicitImplementation != null) {
			log("Using pre-configured impl() instance");
			return processBean(runPostCreateHooks(inject(explicitImplementation)));
		}

		if (cached) {
			log("Using cached instance");
			return beanImpl.get();
		}

		log("Using new instance");
		return processBean(findBeanImpl());
	}

	private T processBean(T bean) {
		for (var w : wrappers)
			bean = w.apply(bean);
		for (var v : validators) {
			if (! v.predicate().test(bean))
				throw new ExecutableException(v.message() + " (bean=" + bean + ")");
		}
		return bean;
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
	 * 	<jk>var</jk> <jv>creator</jv> = BeanInstantiator
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
	public Builder<T> debug() {
		debug.set(synchronizedList(new ArrayList<>()));
		return this;
	}


	/**
	 * Specifies custom static factory method names to look for when creating the bean.
	 *
	 * <p>
	 * By default, {@link BeanInstantiator} looks for a static {@code getInstance()} method when attempting
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
	 * 	MyBean <jv>bean</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.factoryMethodNames(<js>"of"</js>, <js>"from"</js>, <js>"create"</js>, <js>"newInstance"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param names The factory method names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public Builder<T> factoryMethodNames(String... names) {
		factoryMethodNames = set(assertArgNoNulls(ARG_names, names));
		reset();
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
	 * 	<li class='note'>If the fallback is used, that instance is cached via {@link Memoizer} and returned on subsequent calls
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
	 * 	MyService <jv>service</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(MyService.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.fallback(() -&gt; <jk>new</jk> DefaultMyService())
	 * 		.run();
	 *
	 * 	<jc>// Use a pre-created instance as fallback</jc>
	 * 	MyService <jv>defaultService</jv> = <jk>new</jk> DefaultMyService();
	 * 	MyService <jv>service2</jv> = BeanInstantiator
	 * 		.<jsm>of</jsm>(MyService.<jk>class</jk>, <jv>myBeanStore</jv>)
	 * 		.fallback(() -&gt; <jv>defaultService</jv>)
	 * 		.run();
	 * </p>
	 *
	 * @param fallback The fallback supplier. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public Builder<T> fallback(Supplier<? extends T> fallback) {
		assertArgNotNull(ARG_fallback, fallback);
		this.fallbackSupplier = fallback;
		return this;
	}

	/**
	 * Returns the bean subtype class info if specified, or the normal bean type if not.
	 *
	 * <p>
	 * If {@link #type(Class)} was called to specify a subtype, returns that subtype.
	 * Otherwise, returns the original bean type passed to {@link #of(Class)}.
	 *
	 * @return The bean subtype class info, or the bean type class info if no subtype was specified.
	 */
	protected ClassInfo getBeanSubType() {
		return beanSubType;
	}

	/**
	 * Returns the name of the bean being created.
	 *
	 * <p>
	 * Returns the name that was passed to {@link #of(Class, BeanStore, String, Object)}, or <jk>null</jk> if no name was specified.
	 *
	 * @return The bean name, or <jk>null</jk> if not set.
	 */
	public String getName() {
		return name;
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
		return beanSubTypes.get();
	}

	/**
	 * Returns the builder instance if one has been created or specified.
	 *
	 * @param <B> The builder type.
	 * @return The builder instance wrapped in an {@link Optional}, or an empty optional if no builder exists.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast to B for builder retrieval
	})
	public <B> Optional<B> getBuilder() {
		beanImpl.reset();
		return (Optional<B>)builderMemoizer.toOptional();
	}

	/**
	 * Returns the builder type class info determined by {@link #findBuilderType()}.
	 *
	 * @return The builder type class info, or <jk>null</jk> if no builder type was determined.
	 */
	protected ClassInfo getBuilderType() {
		return builderType.get();
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
		return builderTypes.get();
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
	 * 	<li class='note'>Since {@link #run()} uses a cached {@link Memoizer}, the log reflects only the first creation
	 * 		(subsequent calls return the cached instance unless {@link #reset()} is called).
	 * 	<li class='note'>The returned list is unmodifiable.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>var</jk> <jv>creator</jv> = BeanInstantiator.<jsm>of</jsm>(MyBean.<jk>class</jk>, <jv>store</jv>)
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
	public Builder<T> impl(T value) {
		this.explicitImplementation = value;
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
	 * 		{@link Memoizer}, hooks are only executed once unless {@link #reset()} is called.
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
	 * 	MyService <jv>service</jv> = BeanInstantiator
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
	public Builder<T> postCreateHook(Consumer<T> hook) {
		assertArgNotNull(ARG_hook, hook);
		postCreateHooks.add(hook);
		return this;
	}

	/**
	 * Suppresses warning-level log output from this builder.
	 *
	 * <p>
	 * Useful when {@link #asOptional()} is preferred and you don't want the underlying log noise that
	 * accompanies fallback or factory-abstention paths.
	 *
	 * <p>
	 * Errors and exceptions are still thrown normally; only descriptive log entries are suppressed.
	 *
	 * @return This object.
	 */
	public Builder<T> silent() {
		silent = true;
		return this;
	}

	/**
	 * Sets a human-readable description used in log entries and error messages.
	 *
	 * <p>
	 * Useful when the bean type alone doesn't provide enough context (e.g. multiple instances of the same type).
	 *
	 * @param value The description. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public Builder<T> description(String value) {
		assertArgNotNull(ARG_value, value);
		description = value;
		return this;
	}

	/**
	 * Registers a transformation to apply to the created bean before it is returned.
	 *
	 * <p>
	 * Multiple wrappers may be registered; they are applied in registration order. Each wrapper receives
	 * the output of the previous wrapper.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BeanInstantiator.<jsm>of</jsm>(MyService.<jk>class</jk>, store)
	 * 		.wrap(LoggingProxy::wrap)
	 * 		.wrap(MetricsProxy::wrap)
	 * 		.run();
	 * </p>
	 *
	 * @param wrapper The wrapper. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public Builder<T> wrap(UnaryOperator<T> wrapper) {
		assertArgNotNull(ARG_value, wrapper);
		wrappers.add(wrapper);
		return this;
	}

	/**
	 * Registers a post-creation validator.
	 *
	 * <p>
	 * The predicate is invoked on the created bean. If it returns <jk>false</jk>, an
	 * {@link ExecutableException} is thrown with the supplied message.
	 *
	 * @param predicate The predicate. Cannot be <jk>null</jk>.
	 * @param message The error message used when the predicate returns <jk>false</jk>. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public Builder<T> validate(Predicate<T> predicate, String message) {
		assertArgNotNull(ARG_value, predicate);
		assertArgNotNull(ARG_value, message);
		validators.add(new Validation<>(predicate, message));
		return this;
	}

	/**
	 * Registers an alternative {@link BeanInstantiator.Builder} to consult if this builder fails to create the bean.
	 *
	 * <p>
	 * Multiple alternatives may be registered. They are tried in registration order. The first one that
	 * succeeds returns its bean; if all fail, the last exception is propagated.
	 *
	 * @param alternative The alternative builder. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public Builder<T> or(Builder<? extends T> alternative) {
		assertArgNotNull(ARG_value, alternative);
		alternatives.add(alternative);
		return this;
	}

	/**
	 * Registers a callback that is invoked on the builder instance before {@code build()} is called.
	 *
	 * <p>
	 * Multiple initializers may be registered; they are applied in registration order. The callback
	 * receives the builder instance after it has been retrieved (from the bean store, static factory,
	 * or constructor) but before any auto-wiring or {@link #injectBuilder() inject-based} processing.
	 *
	 * <p>
	 * The callback is typed as {@code Consumer<Object>} because the builder type is generally not
	 * statically known at the call site (an unchecked cast will typically be required inside the
	 * lambda).
	 *
	 * @param initializer The initializer. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public Builder<T> builderInitializer(Consumer<Object> initializer) {
		assertArgNotNull(ARG_value, initializer);
		builderInitializers.add(initializer);
		return this;
	}

	/**
	 * Enables {@code @Inject} / {@code @Autowired} field and method injection on the builder instance.
	 *
	 * <p>
	 * When set, after the builder is retrieved (from the bean store, static factory, or constructor),
	 * its fields and methods annotated with {@code @Inject} or {@code @Autowired} are auto-resolved
	 * from the bean store. This is similar to the post-construction injection performed on the
	 * resulting bean, but applied to the builder.
	 *
	 * <p>
	 * Default is <jk>false</jk> (opt-in).
	 *
	 * @return This object.
	 */
	public Builder<T> injectBuilder() {
		injectBuilder = true;
		return this;
	}

	/**
	 * Enables auto-wiring of bean store entries into matching setter methods on the builder.
	 *
	 * <p>
	 * When set, after the builder is retrieved, every public single-argument setter method is
	 * inspected. If the bean store has a bean of a type assignable to the setter's parameter type,
	 * the setter is invoked with that bean.
	 *
	 * <p>
	 * If a parameter is of type {@code Supplier<X>} and the bean store has a bean of type {@code X},
	 * the bean is wrapped in a supplier and passed to the setter (when {@code unwrapSuppliers}
	 * defaulting to true).
	 *
	 * <p>
	 * Default is <jk>false</jk> (opt-in).
	 *
	 * @return This object.
	 */
	public Builder<T> autoWireBuilder() {
		autoWireBuilder = true;
		return this;
	}

	/**
	 * Disables builder-based bean creation entirely.
	 *
	 * <p>
	 * When set, builder detection is skipped and the bean is instantiated directly via static factory
	 * methods or constructors. This is useful when a Builder class is co-located with the bean type
	 * (e.g. {@code Foo.Builder}) but the caller wants to use a builder-taking constructor instead.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BeanInstantiator.<jsm>of</jsm>(BasicSwaggerProvider.<jk>class</jk>, beanStore)
	 * 		.add(SwaggerProvider.Builder.<jk>class</jk>, <jv>this</jv>)
	 * 		.noBuilder()
	 * 		.run();
	 * </p>
	 *
	 * @return This object.
	 */
	public Builder<T> noBuilder() {
		noBuilder = true;
		return this;
	}

	/**
	 * Sets the lifecycle scope of the bean being created.
	 *
	 * <p>
	 * {@link Scope#SINGLETON} is equivalent to calling {@link #cached()};
	 * {@link Scope#PROTOTYPE} resets the cached flag.
	 *
	 * @param value The scope. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public Builder<T> scope(Scope value) {
		assertArgNotNull(ARG_value, value);
		cached = (value == Scope.SINGLETON);
		return this;
	}

	/**
	 * Returns a copy of this builder with the same configuration.
	 *
	 * <p>
	 * The returned builder shares no mutable state with this one and may be configured independently.
	 *
	 * @return A new builder pre-populated with this builder's configuration.
	 */
	public Builder<T> copy() {
		var c = new Builder<T>(beanType.inner(), parentStore, name, enclosingInstance);
		c.beanSubType = beanSubType;
		c.explicitBuilderType = explicitBuilderType;
		c.explicitBuilder = explicitBuilder;
		c.explicitImplementation = explicitImplementation;
		c.postCreateHooks = new ArrayList<>(postCreateHooks);
		c.factoryMethodNames = factoryMethodNames;
		c.builderMethodNames = builderMethodNames;
		c.buildMethodNames = buildMethodNames;
		c.builderClassNames = builderClassNames;
		c.fallbackSupplier = fallbackSupplier;
		c.cached = cached;
		c.factoryAbstainOnNull = factoryAbstainOnNull;
		c.preferZeroArgConstructor = preferZeroArgConstructor;
		c.silent = silent;
		c.description = description;
		c.wrappers = new ArrayList<>(wrappers);
		c.validators = new ArrayList<>(validators);
		c.alternatives = new ArrayList<>(alternatives);
		c.builderInitializers = new ArrayList<>(builderInitializers);
		c.injectBuilder = injectBuilder;
		c.autoWireBuilder = autoWireBuilder;
		c.autoWireUnwrapSuppliers = autoWireUnwrapSuppliers;
		c.noBuilder = noBuilder;
		debug.ifPresent(x -> c.debug.set(new ArrayList<>()));
		return c;
	}

	/**
	 * Holds a single validator predicate plus its error message.
	 */
	private static record Validation<T>(Predicate<T> predicate, String message) {}

	/**
	 * Resets the builder, builderType, and beanImpl suppliers.
	 *
	 * <p>
	 * This causes them to be re-evaluated on the next call to {@link #run()}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>If an explicit builder instance was set via {@link #builder(Object)},
	 * 		it is preserved and not cleared by this method.
	 * 	<li class='note'>If an explicit implementation was set via {@link #impl(Object)},
	 * 		it is preserved and not cleared by this method.
	 * 	<li class='note'>Only the cached results from suppliers are reset, not the configuration itself.
	 * </ul>
	 *
	 * @return This object.
	 */
	public Builder<T> reset() {
		if (explicitBuilder == null) {
			builderMemoizer.reset();
		}
		builderType.reset();
		if (explicitImplementation == null) {
			beanImpl.reset();
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
	@SuppressWarnings({
		"unchecked", // Type erasure requires unchecked casts
		"java:S3776", // Cognitive complexity acceptable for this specific logic
		"java:S6541", // Single-threaded context; synchronization unnecessary
	})
	private T findBeanImpl() {
		var store2 = this.store;
		var builder2 = explicitBuilder != null ? explicitBuilder : this.builderMemoizer.get();  // Use explicit builder if set, otherwise get from supplier
		T bean = null;
		var methodComparator = comparing(MethodInfo::getParameterCount).reversed();
		var constructorComparator = comparing(ConstructorInfo::getParameterCount).reversed();

		boolean builderAttempted = false;
		if (builder2 != null) {
			log("Builder detected: %s", builder2.getClass().getName());

			// Initialization hooks: builder initializers, then optional inject, then optional auto-wire.
			for (var init : builderInitializers)
				init.accept(builder2);
			if (injectBuilder) {
				log("Injecting builder via @Inject/@Autowired");
				inject(builder2);
			}
			if (autoWireBuilder) {
				log("Auto-wiring builder setters from bean store");
				autoWireBuilder(builder2, store2);
			}

			// Look for Builder.build()/create()/get() - REQUIRED
			// Uses buildMethodNames configuration (defaults to "build", "create", "get")
			// Check declared methods first to get the most specific override, then fall back to public methods
			var buildMethod = info(builder2.getClass()).getPublicMethods().stream()
				.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
				.filter(x -> buildMethodNames.contains(x.getNameSimple()))
				.filter(x -> opt(x).map(x2 -> x2.getReturnType()).filter(x2 -> x2.is(beanSubType.inner()) || x2.isParentOf(beanSubType)).isPresent()) // Accept methods that return beanSubType or a parent type of beanSubType
				.filter(x -> x.getAnnotations().stream().anyMatch(JsrSupport::isInjectAnnotation) ? x.canResolveAllParameters(store2) : x.getParameterCount() == 0)
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
				boolean hasInject = method.getAnnotations().stream().anyMatch(JsrSupport::isInjectAnnotation);
				if (hasInject) {
					log("Method has @Inject annotation, resolving parameters from bean store");
				} else {
					log("Method has no parameters, calling without injection");
				}

				builderAttempted = true;
				// Call the builder method
				Object builtBean = method.inject(store2, builder2);

				// Validate the produced bean (loose-builder mode). We accept the result if either:
				//   1. The build method's declared return type is exactly beanSubType, OR
				//   2. The runtime instance produced by the build method is assignment-compatible with beanSubType.
				// Case 2 supports the legacy pattern where a parent class's Builder.build() declares the parent
				// type but constructs a configured subclass at runtime (e.g. DebugEnablement.Builder.build() returning
				// a BasicDebugEnablement because the builder's internal type-binding was preset).
				// If neither matches, we fall through to factory-method / constructor resolution on beanSubType
				// rather than throwing — that lets standard "subclass adds its own constructor" patterns succeed.
				if (returnType.is(beanSubType.inner()) || (builtBean != null && beanSubType.inner().isInstance(builtBean))) {
					bean = (T)beanType.cast(builtBean);
				} else {
					log("Builder method %s returned %s but expected %s; falling through to factory methods/constructors", method.getNameFull(), builtBean == null ? "null" : builtBean.getClass().getName(), beanSubType.getName());
				}
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
				.filter(x -> x.getAnnotations().stream().anyMatch(JsrSupport::isInjectAnnotation) ? x.canResolveAllParameters(store2) : x.getParameterCount() == 0)
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
			} else if (builderAttempted) {
				// We tried the builder's build method but it produced a runtime instance that wasn't the expected
				// beanSubType (loose-builder fallthrough above).  Don't throw; fall through to factory-method /
				// constructor resolution on beanSubType so subclasses with their own constructor/factory still resolve.
				log("Builder %s build method produced wrong runtime type; falling through to factory methods/constructors", builder2.getClass().getName());
			} else if (hasBuildMethodWithRightReturnType || isExplicitBuilder || isValidBuilder) {
				// Builder has a build method with right return type (even if can't be called) or was explicitly set
				// Throw exception unless fallback exists
				log("Builder detected but no appropriate build method found. Builder type: %s. Expected method names: %s", builder2.getClass().getName(), buildMethodNames);

				if (fallbackSupplier != null) {
					log(CONST_usingFallbackSupplier);
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
		var factoryMethod = beanSubType.getPublicMethods().stream()
			.filter(x -> x.isAll(STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
			.filter(x -> factoryMethodNames.contains(x.getNameSimple()))
			.filter(x -> x.hasReturnType(beanSubType))
			.filter(x -> x.canResolveAllParameters(store2, factoryMethodExtraBeans))
			.sorted(methodComparator)
			.findFirst();
		if (factoryMethod.isPresent()) {
			log("Found factory method: %s", factoryMethod.get().getNameFull());
			bean = (T) beanType.cast(factoryMethod.get().inject(store2, null, factoryMethodExtraBeans));
			// When factoryAbstainOnNull is set, treat a null result from the factory method as a
			// deliberate "abstain" signal and return null without falling through to constructor
			// lookup.  Used by callers like RestOpArg subclasses whose create(ParameterInfo)
			// returns null when the parameter isn't annotated with the marker the subclass
			// handles (e.g. AttributeArg.create() returns null when @Attr isn't present).
			if (bean == null && factoryAbstainOnNull)
				return null;
		}

		// Look for Bean().
		// Skip constructor invocation when beanSubType is abstract or an interface — Class.newInstance() would
		// throw InstantiationException, masking the more useful "class is abstract"/"class is an interface"
		// fallthrough below (which honors the registered fallback supplier when present).
		if (bean == null && ! beanSubType.isInterface() && ! beanSubType.isAbstract()) {
			log("Attempting Bean() constructor");
			// If preferZeroArgConstructor is set and a no-arg constructor exists, short-circuit to it.
			// This skips the longest-resolvable-constructor heuristic for callers that just want a fresh
			// empty instance — see preferZeroArgConstructor() Javadoc for the parameterized-container rationale.
			if (preferZeroArgConstructor) {
				var zeroArgCtor = beanSubType.getPublicConstructors().stream()
					.filter(x -> x.isAll(NOT_DEPRECATED))
					.filter(x -> x.isDeclaringClass(beanSubType))
					.filter(x -> x.getParameterCount() == 0)
					.findFirst();
				if (zeroArgCtor.isPresent()) {
					log("Using zero-arg constructor (preferZeroArgConstructor): %s", zeroArgCtor.get().getNameFull());
					bean = (T) beanType.cast(zeroArgCtor.get().inject(store2, enclosingInstance));
				}
			}
			// If builder was detected but has no build method, pass it as extra bean for constructors
			Object[] constructorExtraBeans = builder2 != null ? new Object[]{builder2} : new Object[0];
			if (bean == null) {
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
			// Fallback: try protected constructors (legacy BeanCreator behavior).
			// Many framework classes use a protected (Builder) constructor with public static create() factory.
			if (bean == null) {
				bean = beanSubType.getDeclaredConstructors().stream()
					.filter(x -> x.isAll(PROTECTED, NOT_DEPRECATED))
					.filter(x -> x.isDeclaringClass(beanSubType))
					.filter(x -> x.canResolveAllParameters(store2, enclosingInstance, constructorExtraBeans))
					.sorted(constructorComparator)
					.findFirst()
					.map(x -> {
						log("Found protected constructor: %s", x.getNameFull());
						return (T)beanType.cast(x.inject(store2, enclosingInstance, constructorExtraBeans));
					})
					.orElse(null);
			}
			// Fallback: try package-private constructors.
			// Useful for @Configuration classes and other framework types that intentionally restrict
			// instantiation to their declaring package but still want to participate in the bean store.
			// Private constructors are deliberately excluded — they signal "do not instantiate".
			if (bean == null) {
				bean = beanSubType.getDeclaredConstructors().stream()
					.filter(x -> x.isAll(NOT_PUBLIC, NOT_PROTECTED, NOT_PRIVATE, NOT_DEPRECATED))
					.filter(x -> x.isDeclaringClass(beanSubType))
					.filter(x -> x.canResolveAllParameters(store2, enclosingInstance, constructorExtraBeans))
					.sorted(constructorComparator)
					.findFirst()
					.map(x -> {
						log("Found package-private constructor: %s", x.getNameFull());
						return (T)beanType.cast(x.inject(store2, enclosingInstance, constructorExtraBeans));
					})
					.orElse(null);
			}
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
				log(CONST_usingFallbackSupplier);
				T fallbackBean = fallbackSupplier.get();
				runPostCreateHooks(fallbackBean);
				return fallbackBean;
			}
			throw exex("Could not instantiate class {0}: {1}.", beanSubType.getName(), "Class is an interface");
		}

		if (beanSubType.isAbstract()) {
			log("Bean type is abstract");
			if (fallbackSupplier != null) {
				log(CONST_usingFallbackSupplier);
				T fallbackBean = fallbackSupplier.get();
				runPostCreateHooks(fallbackBean);
				return fallbackBean;
			}
			throw exex("Could not instantiate class {0}: {1}.", beanSubType.getName(), "Class is abstract");
		}

		if (fallbackSupplier != null) {
			log(CONST_usingFallbackSupplier);
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

		// noBuilder() short-circuits all builder detection.
		if (noBuilder)
			return null;

		var bs = store;
		var builderType2 = this.builderType.get();

		// If no builder type was determined, return null (builder not needed)
		if (builderType2 == null)
			return null;

		Optional<Object> r;

		// Step 0: Look for a pre-registered builder of the builder type in the bean store.
		// This handles scenarios where the caller has already configured a builder externally
		// (e.g. legacy framework code that registers a configured builder bean).
		r = bs.getBean(builderType2.inner()).map(Object.class::cast);
		if (r.isPresent()) {
			log("Using pre-registered builder from bean store: %s", builderType2.getName());
			return r.get();
		}

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
		if (r.isPresent())
			return r.get();

		// Step 4: Look for a package-private constructor on the builder type.
		// Mirrors the package-private fallback in findBeanImpl() for symmetry: any builder co-located in
		// the bean's declaring package can be discovered without forcing the constructor to be public/protected.
		r = builderType2.getDeclaredConstructors().stream()
			.filter(x -> x.isAll(NOT_PUBLIC, NOT_PROTECTED, NOT_PRIVATE, NOT_DEPRECATED))
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
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast to ClassInfo for builder type
	})
	private ClassInfo findBuilderType() {
		log("Finding builder type...");

		// noBuilder() short-circuits all builder detection.
		if (noBuilder) {
			log("noBuilder() set; skipping builder type detection");
			return null;
		}

		// Priority 1: Explicitly set builderType
		if (explicitBuilderType != null) {
			log("Using explicitly set builder type: %s", explicitBuilderType.getName());
			return explicitBuilderType;
		}

		// Priority 2: @Builder annotation on beanSubType
		// Check declared annotations first to ensure child's annotation overrides parent's
		var r = beanSubType.getDeclaredAnnotations().stream()
			.filter(a -> a.isType(org.apache.juneau.commons.annotation.Builder.class))
			.map(a -> (AnnotationInfo<org.apache.juneau.commons.annotation.Builder>)a)
			.map(x -> ClassInfo.class.cast(info(x.inner().value())))
			.findFirst();
		if (r.isPresent()) {
			log("Found builder via @Builder annotation on beanSubType: %s", r.get().getName());
			return r.get();
		}
		// Fall back to inherited annotations if no declared annotation found
		r = beanSubType.getAnnotations(org.apache.juneau.commons.annotation.Builder.class)
			.map(x -> ClassInfo.class.cast(info(x.inner().value())))
			.findFirst();
		if (r.isPresent()) {
			log("Found builder via @Builder annotation (inherited): %s", r.get().getName());
			return r.get();
		}

		// Priority 3: Autodetect
		// 3a: Look for static create/builder method that returns a potential builder type.
		var staticBuilderTypes = beanSubType.getPublicMethods().stream()
			.filter(x -> x.isAll(STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
			.filter(x -> builderMethodNames.contains(x.getNameSimple()))
			.filter(x -> ! x.hasReturnType(beanSubType)) // Must not return the bean type itself
			.filter(x -> isValidBuilderType(x.getReturnType()))
			.map(MethodInfo::getReturnType)
			.toList();
		if (! staticBuilderTypes.isEmpty()) {
			// Prefer a factory whose builder builds the EXACT requested type (or a
			// subtype) over one that only promises a supertype — e.g. an inherited generic builder(Class)
			// whose build() erases to a base class. A supertype-only factory must not displace a stricter path.
			var strict = staticBuilderTypes.stream().filter(this::isStrictBuilderType).findFirst();
			if (strict.isPresent()) {
				log("Found builder via static method (strict): %s", strict.get().getName());
				return strict.get();
			}
			var weak = gateWeakBuilder(staticBuilderTypes.get(0));
			if (weak != null) {
				log("Found builder via static method: %s", weak.getName());
				return weak;
			}
			// Weak/supertype-only static factory rejected in favor of a direct constructor; fall through to
			// 3b/3c (a stricter inner Builder may still exist) and ultimately the constructor path.
		}

		// 3b: Look for inner Builder class
		log("Looking for inner builder class (names: %s)...", builderClassNames);
		r = beanSubType.getDeclaredMemberClasses().stream()
			.filter(x -> builderClassNames.contains(x.getNameSimple()))
			.findFirst();
		if (r.isPresent()) {
			var builderClass = r.get();
			if (isValidBuilderType(builderClass)) {
				var gated = isStrictBuilderType(builderClass) ? builderClass : gateWeakBuilder(builderClass);
				if (gated != null) {
					log("Found builder via inner class: %s", gated.getName());
					return gated;
				}
				// Weak inner builder rejected in favor of a direct constructor; fall through.
			} else {
				// Still return it so we can provide a better error message when trying to use it
				log("Found builder class via inner class but it is not valid: %s", builderClass.getName());
				return builderClass;
			}
		}

		// 3c: Look for builder in parent classes and implemented interfaces (skip beanSubType itself,
		// which was already searched in 3b). getAllParents returns parents (child-to-parent) followed by
		// interfaces, so we walk the whole chain in a uniform order.
		var allParents = beanSubType.getAllParents();
		for (int i = 1; i < allParents.size(); i++) {
			var parentClass = allParents.get(i);
			r = parentClass.getDeclaredMemberClasses().stream()
				.filter(x -> builderClassNames.contains(x.getNameSimple()))
				.findFirst();
			if (r.isPresent()) {
				var builderClass = r.get();
				if (isValidBuilderType(builderClass)) {
					var gated = isStrictBuilderType(builderClass) ? builderClass : gateWeakBuilder(builderClass);
					if (gated != null) {
						log("Found builder via parent inner class: %s", gated.getName());
						return gated;
					}
					// Weak parent inner builder rejected in favor of a direct constructor; keep scanning.
					continue;
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
	 * Tests whether a builder candidate builds the <b>exact</b> requested bean type (or a subtype of it),
	 * as opposed to only promising a supertype.
	 *
	 * <p>
	 * Used by {@link #findBuilderType()} to prefer a precise builder over an inherited
	 * generic base builder whose {@code build()} erases to a parent class (e.g. a self-typed REST
	 * {@code DefaultBuilder} whose {@code build()} returns {@code RestMixin}).
	 *
	 * @param builderCandidate The builder type to test.
	 * @return <jk>true</jk> if the candidate has a 0-arg (or {@code @Inject}) build/create/get method whose
	 * 	return type is {@code beanSubType} or a subtype of it.
	 */
	private boolean isStrictBuilderType(ClassInfo builderCandidate) {
		return builderCandidate.getPublicMethods().stream()
			.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
			.filter(x -> buildMethodNames.contains(x.getNameSimple()))
			.filter(x -> { var rt = x.getReturnType(); return rt.is(beanSubType.inner()) || beanSubType.isParentOf(rt); })
			.anyMatch(x -> x.getAnnotations().stream().anyMatch(JsrSupport::isInjectAnnotation) || x.getParameterCount() == 0);
	}

	/**
	 * A builder candidate that only builds a <i>supertype</i> of the requested bean
	 * type must not be used when the requested type is concretely instantiable via a direct constructor.
	 *
	 * <p>
	 * Returns the candidate unchanged when it is safe to use, or {@code null} to signal "prefer the
	 * constructor path". This is only invoked for non-strict (supertype-only) candidates.
	 *
	 * @param builderCandidate The (weak) builder candidate.
	 * @return The candidate, or {@code null} to prefer the direct-constructor path.
	 */
	private ClassInfo gateWeakBuilder(ClassInfo builderCandidate) {
		if (hasUsableDirectConstructor()) {
			log("Builder candidate %s only builds a supertype of %s and a usable direct constructor exists; preferring the constructor.", builderCandidate.getName(), beanSubType.getName());
			return null;
		}
		return builderCandidate;
	}

	/**
	 * Tests whether the requested bean type is concretely instantiable via a non-private constructor whose
	 * parameters can all be resolved from the bean store (a no-arg constructor always qualifies).
	 *
	 * @return <jk>true</jk> if a usable direct constructor exists on {@code beanSubType}.
	 */
	private boolean hasUsableDirectConstructor() {
		if (beanSubType.isAbstract() || beanSubType.isInterface())
			return false;
		return beanSubType.getDeclaredConstructors().stream()
			.filter(x -> x.isAll(NOT_DEPRECATED, NOT_PRIVATE))
			.filter(x -> x.isDeclaringClass(beanSubType))
			.anyMatch(x -> x.canResolveAllParameters(store, enclosingInstance));
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
	 * Auto-wires bean store entries into matching public single-argument setter methods of the builder.
	 *
	 * <p>
	 * Setters are public, non-static, non-deprecated methods taking exactly one argument. The bean store
	 * is queried for a bean assignable to the parameter's type. If the parameter is {@code Supplier<X>}
	 * and {@link #autoWireUnwrapSuppliers} is true, the bean of type {@code X} is wrapped before being
	 * passed in.
	 *
	 * <p>
	 * Setters whose argument cannot be resolved are silently skipped.
	 */
	@SuppressWarnings({
		"java:S3776", // Best-effort auto-wiring branches on reflection and type-shape checks.
		"java:S3011" // Reflection access override is required for invoking discovered setters uniformly.
	})
	private void autoWireBuilder(Object builder2, BeanStore bs) {
		var ci = info(builder2.getClass());
		ci.getPublicMethods().stream()
			.filter(m -> m.isAll(NOT_STATIC, NOT_DEPRECATED, NOT_SYNTHETIC, NOT_BRIDGE))
			.filter(m -> m.getParameterCount() == 1)
			.forEach(m -> {
				try {
					var p0 = m.getParameterTypes().get(0);
					Class<?> rawType = p0.inner();
					Object value = null;
					if (autoWireUnwrapSuppliers && Supplier.class.isAssignableFrom(rawType)) {
						// Best-effort: try to resolve the wrapped type from the parameter's parameterized signature.
						var generic = m.inner().getGenericParameterTypes()[0];
						if (generic instanceof java.lang.reflect.ParameterizedType pt) {
							var args = pt.getActualTypeArguments();
							if (args.length == 1 && args[0] instanceof Class<?> wrapped) {
								var wrappedBean = bs.getBean(wrapped);
								if (wrappedBean.isPresent()) {
									Object wb = wrappedBean.get();
									value = (Supplier<Object>) () -> wb;
								}
							}
						}
					} else {
						var bean = bs.getBean(rawType);
						if (bean.isPresent())
							value = bean.get();
					}
					if (value != null) {
						m.inner().setAccessible(true);
						m.inner().invoke(builder2, value);
						log("Auto-wired setter %s with bean of type %s", m.getNameSimple(), rawType.getName());
					}
				} catch (@SuppressWarnings("unused") Exception e) {
					// Swallow auto-wire errors silently; auto-wire is a best-effort.
				}
			});
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
			.anyMatch(x -> x.getAnnotations().stream().anyMatch(JsrSupport::isInjectAnnotation) || x.getParameterCount() == 0);

		if (hasBuildMethod) {
			log("Builder is valid: has build/create/get method returning bean type");
			return true;
		}

		log("Builder is NOT valid: no build/create/get method returning bean type");
		return false;
	}

	private void log(String message, Object... args) {
		if (silent && ! debug.isPresent())
			return;
		var prefix = description != null ? (description + " (" + beanType.getName() + ")") : beanType.getName();
		logger.fine(prefix + ": " + message, args);
		debug.ifPresent(x -> x.add(args.length == 0 ? message : String.format(message, args)));
	}

	T runPostCreateHooks(T bean) {
		postCreateHooks.forEach(x -> x.accept(bean));
		return bean;
	}
	} // class Builder
}