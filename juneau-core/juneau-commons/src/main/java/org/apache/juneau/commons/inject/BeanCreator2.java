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
import static org.apache.juneau.commons.inject.InjectUtils.*;
import static java.util.Comparator.*;
import static org.apache.juneau.commons.reflect.ElementFlag.*;


import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.function.*;
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
 * 		<jk>private</jk> MyBean(Builder <jv>builder</jv>) {
 * 			<jk>this</jk>.<jf>value</jf> = <jv>builder</jv>.<jf>value</jf>;
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
 * 				<jk>return new</jk> MyBean(<jk>this</jk>);
 * 			}
 * 		}
 * 	}
 *
 * 	<jc>// Create bean using BeanCreator2</jc>
 * 	MyBean <jv>bean</jv> = BeanCreator2
 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
 * 		.beanStore(<jv>myBeanStore</jv>)
 * 		.create();
 * </p>
 *
 * <h5 class='section'>Bean Creation Order:</h5>
 * <p>
 * The creator attempts to instantiate beans in the following order:
 * <ol class='spaced-list'>
 * 	<li><b>Using a Builder</b> (if a builder is found or specified):
 * 		<ol>
 * 			<li>Calls a <c>build()</c>/<c>create()</c>/<c>get()</c> method on the builder that returns the bean type.
 * 			<li>Calls a static <c>getInstance(Builder)</c> method on the bean class.
 * 			<li>Calls a constructor on the bean class that accepts the builder.
 * 			<li>Calls any method on the builder that returns the bean type.
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
 * 				(method names can be customized via {@link #builderMethod(String...)}).
 * 			<li>Inner class named <c>Builder</c>.
 * 		</ul>
 * </ol>
 *
 * <h5 class='section'>Dependency Injection:</h5>
 * <p>
 * Both builder instances and the final bean have automatic dependency injection performed via
 * {@link InjectUtils#injectBeans(Object, BeanStore)}, which processes {@code @Inject} and {@code @Autowired} annotations
 * on fields and methods.
 *
 * <p>
 * This means that after a bean is created, all of its fields and methods annotated with {@code @Inject} or {@code @Autowired}
 * will automatically be populated with beans from the bean store.
 *
 * <h5 class='section'>Notes:</h5><ul>
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
public class BeanCreator2<T> {

	/** Default factory method names used for bean instantiation. */
	public static final Set<String> DEFAULT_FACTORY_METHOD_NAMES = u(set("getInstance"));

	/** Default builder method names used for builder instantiation. */
	public static final Set<String> DEFAULT_BUILDER_METHOD_NAMES = u(set("create", "builder"));

	/** Default builder class names for auto-detection. */
	public static final Set<String> DEFAULT_BUILDER_CLASS_NAMES = u(set("Builder"));

	/**
	 * Creates a new bean creator.
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @return A new bean creator.
	 */
	public static <T> BeanCreator2<T> of(Class<T> beanType) {
		return new BeanCreator2<>(beanType);
	}

	private BeanStore parentStore;
	private Supplier<BasicBeanStore2> store = mem(()-> new BasicBeanStore2(parentStore));
	private ClassInfoTyped<T> beanType;
	private ClassInfo beanSubType;
	private ClassInfo builderType;
	private ResettableSupplier<Object> builder = memr(() -> findBuilder());
	private T beanImpl;
	private Object enclosingInstance;
	private boolean singleton = false;
	private T cachedInstance = null;
	private List<Consumer<T>> postCreateHooks = new ArrayList<>();
	private Set<String> factoryMethodNames = DEFAULT_FACTORY_METHOD_NAMES;
	private Set<String> builderMethodNames = DEFAULT_BUILDER_METHOD_NAMES;
	private Set<String> builderClassNames = DEFAULT_BUILDER_CLASS_NAMES;
	private Consumer<Object> builderCustomizer = null;
	private Supplier<T> fallbackSupplier = null;
	private Predicate<T> validator = null;
	private boolean debug = false;
	private List<String> debugLog = new ArrayList<>();
	private boolean addToStore = false;
	private String addToStoreName = null;

	/**
	 * Constructor.
	 *
	 * @param beanType The bean type being created.
	 */
	protected BeanCreator2(Class<T> beanType) {
		this.beanType = info(assertArgNotNull("beanType", beanType));
		this.beanSubType = this.beanType;
	}

	/**
	 * Specifies the bean store to use for resolving constructor/method parameters.
	 *
	 * @param value The bean store to use.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> beanStore(BeanStore value) {
		parentStore = value;
		return this;
	}
	/**
	 * Specifies the enclosing instance object to use when instantiating inner classes.
	 *
	 * @param outer The enclosing instance object.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> enclosingInstance(Object outer) {
		this.enclosingInstance = outer;
		return this;
	}

	/**
	 * Specifies an existing bean implementation to use instead of creating a new instance.
	 *
	 * <p>
	 * When specified, the bean creation logic is bypassed and this implementation is returned
	 * directly from {@link #create()}, after performing dependency injection on it.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The bean implementation will still have its fields and methods injected via {@link InjectUtils#injectBeans(Object, BeanStore)}.
	 * 	<li class='note'>This is useful when you want to provide a pre-configured instance but still benefit from dependency injection.
	 * </ul>
	 *
	 * @param value The bean implementation instance.
	 * @return This object.
	 */
	public BeanCreator2<T> implementation(T value) {
		this.beanImpl = value;
		return this;
	}

	/**
	 * Enables singleton mode for this bean creator.
	 *
	 * <p>
	 * When enabled, the first call to {@link #create()} will create and cache the bean instance.
	 * Subsequent calls to {@link #create()} will return the same cached instance instead of creating
	 * a new one.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The cached instance is stored in this creator and will be returned on all subsequent calls.
	 * 	<li class='note'>Dependency injection via {@link InjectUtils#injectBeans(Object, BeanStore)} is only performed once
	 * 		during the initial creation.
	 * 	<li class='note'>If {@link #impl(Object)} is used to provide an existing instance, that instance becomes the cached singleton.
	 * 	<li class='note'>This is useful for expensive-to-construct beans that should be shared across multiple uses.
	 * 	<li class='note'>Thread safety: The cached instance is stored in a non-volatile field. If thread safety is required,
	 * 		external synchronization should be used or the bean should be created once and stored externally.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a singleton bean</jc>
	 * 	BeanCreator2&lt;MyService&gt; <jv>creator</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyService.<jk>class</jk>)
	 * 		.beanStore(<jv>myBeanStore</jv>)
	 * 		.singleton();
	 *
	 * 	MyService <jv>service1</jv> = <jv>creator</jv>.create();  <jc>// Creates new instance</jc>
	 * 	MyService <jv>service2</jv> = <jv>creator</jv>.create();  <jc>// Returns cached instance</jc>
	 * 	<jsm>assertSame</jsm>(<jv>service1</jv>, <jv>service2</jv>);  <jc>// true</jc>
	 * </p>
	 *
	 * @return This object.
	 */
	public BeanCreator2<T> singleton() {
		this.singleton = true;
		return this;
	}

	/**
	 * Registers a post-creation hook to run after the bean is created.
	 *
	 * <p>
	 * Post-creation hooks are executed after the bean is instantiated and after dependency injection
	 * has been performed via {@link InjectUtils#injectBeans(Object, BeanStore)}.
	 *
	 * <p>
	 * Multiple hooks can be registered and they will be executed in the order they were added.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Hooks are executed every time {@link #create()} is called, unless {@link #singleton()} mode is enabled,
	 * 		in which case hooks are only executed once when the bean is first created.
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
	 * 	<jc>// Register multiple initialization hooks</jc>
	 * 	MyService <jv>service</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyService.<jk>class</jk>)
	 * 		.beanStore(<jv>myBeanStore</jv>)
	 * 		.run(<jv>s</jv> -&gt; <jv>s</jv>.initialize())
	 * 		.run(<jv>s</jv> -&gt; <jv>s</jv>.loadConfiguration())
	 * 		.run(<jv>s</jv> -&gt; <jv>logger</jv>.info(<js>"Created service: "</js> + <jv>s</jv>))
	 * 		.create();
	 * </p>
	 *
	 * @param initializer The initialization hook to run after bean creation. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> run(Consumer<T> initializer) {
		assertArgNotNull("initializer", initializer);
		postCreateHooks.add(initializer);
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
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.beanStore(<jv>myBeanStore</jv>)
	 * 		.factoryMethod(<js>"of"</js>, <js>"from"</js>, <js>"create"</js>, <js>"newInstance"</js>)
	 * 		.create();
	 * </p>
	 *
	 * @param names The factory method names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public BeanCreator2<T> factoryMethod(String... names) {
		factoryMethodNames = set(assertArgNoNulls("names", names));
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
	 * 		.beanStore(<jv>myBeanStore</jv>)
	 * 		.builderMethod(<js>"newBuilder"</js>, <js>"instance"</js>)
	 * 		.create();
	 * </p>
	 *
	 * @param names The builder factory method names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public BeanCreator2<T> builderMethod(String... names) {
		builderMethodNames = set(assertArgNoNulls("names", names));
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
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.beanStore(<jv>myBeanStore</jv>)
	 * 		.builderClass(<js>"BuilderImpl"</js>, <js>"Factory"</js>)
	 * 		.create();
	 * </p>
	 *
	 * @param names The builder class names to look for. Cannot be <jk>null</jk> or contain <jk>null</jk> elements.
	 * @return This object.
	 */
	public BeanCreator2<T> builderClass(String... names) {
		builderClassNames = set(assertArgNoNulls("names", names));
		return this;
	}

	/**
	 * Registers a customizer to configure the builder before it is used to create the bean.
	 *
	 * <p>
	 * Builder customizers are executed after the builder is instantiated (via factory method or constructor)
	 * and after dependency injection has been performed on the builder via {@link InjectUtils#injectBeans(Object, BeanStore)},
	 * but before the builder's {@code build()}/{@code create()}/{@code get()} method is called to create the final bean.
	 *
	 * <p>
	 * This allows you to configure builder properties that aren't available in the bean store or to apply
	 * default configurations, conditional logic, or custom setup to the builder.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The customizer is only called if a builder is used to create the bean.
	 * 		It will not be called if the bean is created via constructor, factory method, or {@link #impl(Object)}.
	 * 	<li class='note'>The customizer receives the builder instance after dependency injection, so any
	 * 		{@code @Inject} fields and methods on the builder will have been populated.
	 * 	<li class='note'>Only one customizer can be registered. Subsequent calls to this method will replace
	 * 		the previous customizer.
	 * 	<li class='note'>If the customizer throws an exception, bean creation will fail and the exception will propagate to the caller.
	 * 	<li class='note'>The customizer receives the builder as {@code Object}, so you'll need to cast it to the
	 * 		appropriate builder type inside your customizer lambda.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Customize builder before creating bean</jc>
	 * 	MyBean <jv>bean</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.beanStore(<jv>myBeanStore</jv>)
	 * 		.customizeBuilder(<jv>b</jv> -&gt; {
	 * 			MyBean.Builder <jv>builder</jv> = (MyBean.Builder)<jv>b</jv>;
	 * 			<jv>builder</jv>.timeout(5000);
	 * 			<jv>builder</jv>.retries(3);
	 * 			<jv>builder</jv>.enabled(<jk>true</jk>);
	 * 		})
	 * 		.create();
	 * </p>
	 *
	 * @param customizer The builder customizer. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> customizeBuilder(Consumer<Object> customizer) {
		assertArgNotNull("customizer", customizer);
		this.builderCustomizer = customizer;
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
	 * 	<li class='note'>In singleton mode, if the fallback is used, that instance becomes the cached singleton.
	 * 	<li class='note'>Only one fallback can be registered. Subsequent calls to {@code orElse()} will replace
	 * 		the previous fallback.
	 * 	<li class='note'>If the fallback supplier itself throws an exception, that exception will propagate to the caller.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Provide a default instance if creation fails</jc>
	 * 	MyService <jv>service</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyService.<jk>class</jk>)
	 * 		.beanStore(<jv>myBeanStore</jv>)
	 * 		.orElse(() -&gt; <jk>new</jk> DefaultMyService())
	 * 		.create();
	 * </p>
	 *
	 * @param fallback The fallback supplier. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> orElse(Supplier<T> fallback) {
		assertArgNotNull("fallback", fallback);
		this.fallbackSupplier = fallback;
		return this;
	}

	/**
	 * Specifies a fallback instance to use if bean creation fails.
	 *
	 * <p>
	 * This is a convenience method that wraps the provided instance in a supplier.
	 * See {@link #orElse(Supplier)} for full details.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Use a pre-created default instance if creation fails</jc>
	 * 	MyService <jv>defaultService</jv> = <jk>new</jk> DefaultMyService();
	 * 	MyService <jv>service</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyService.<jk>class</jk>)
	 * 		.beanStore(<jv>myBeanStore</jv>)
	 * 		.orElse(<jv>defaultService</jv>)
	 * 		.create();
	 * </p>
	 *
	 * @param defaultInstance The default instance to use if creation fails. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> orElse(T defaultInstance) {
		assertArgNotNull("defaultInstance", defaultInstance);
		this.fallbackSupplier = () -> defaultInstance;
		return this;
	}

	/**
	 * Registers a validator to check the created bean before returning it.
	 *
	 * <p>
	 * The validator is executed after bean creation, dependency injection, and post-creation hooks,
	 * but before the bean is returned to the caller. If the validator returns {@code false},
	 * bean creation fails with an {@link ExecutableException}.
	 *
	 * <p>
	 * This allows enforcing runtime constraints, business rules, or state validation on created beans.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The validator is called after all dependencies are injected and all post-creation hooks have run.
	 * 	<li class='note'>If validation fails, an {@link ExecutableException} is thrown with a descriptive message.
	 * 	<li class='note'>In singleton mode, validation is performed on the first creation and the validated instance is cached.
	 * 		Subsequent calls return the cached instance without re-validation.
	 * 	<li class='note'>Only one validator can be registered. Subsequent calls to this method will replace the previous validator.
	 * 	<li class='note'>If the validator throws an exception, that exception propagates to the caller.
	 * 	<li class='note'>The validator is NOT called on fallback instances provided via {@link #orElse(Supplier)} or {@link #orElse(Object)}.
	 * 	<li class='note'>The validator is NOT called on instances provided via {@link #impl(Object)}.
	 * 	<li class='note'>Validation failures are caught by {@link #tryCreate()}, which returns {@link Optional#empty()} on failure.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validate bean state</jc>
	 * 	MyBean <jv>bean</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.beanStore(<jv>myBeanStore</jv>)
	 * 		.validate(<jv>b</jv> -&gt; <jv>b</jv>.isValid())
	 * 		.create();
	 *
	 * 	<jc>// Enforce business rules</jc>
	 * 	Service <jv>service</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(Service.<jk>class</jk>)
	 * 		.beanStore(<jv>store</jv>)
	 * 		.validate(<jv>s</jv> -&gt; <jv>s</jv>.getConnectionCount() &gt; 0)
	 * 		.create();
	 *
	 * 	<jc>// Validate multiple conditions</jc>
	 * 	Config <jv>config</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(Config.<jk>class</jk>)
	 * 		.beanStore(<jv>store</jv>)
	 * 		.validate(<jv>c</jv> -&gt;
	 * 			<jv>c</jv>.getHost() != <jk>null</jk> &amp;&amp;
	 * 			<jv>c</jv>.getPort() &gt; 0 &amp;&amp;
	 * 			<jv>c</jv>.getTimeout() &gt; 0
	 * 		)
	 * 		.create();
	 * </p>
	 *
	 * @param validator The validator predicate. Must return {@code true} if the bean is valid, {@code false} otherwise. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> validate(Predicate<T> validator) {
		assertArgNotNull("validator", validator);
		this.validator = validator;
		return this;
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
	 * 	<li class='note'>The debug log is reset each time {@link #create()} or {@link #tryCreate()} is called.
	 * 	<li class='note'>Debug mode has minimal performance impact but should generally be disabled in production.
	 * 	<li class='note'>The debug log is stored in memory and can be retrieved after bean creation.
	 * 	<li class='note'>In singleton mode, the log reflects the first creation only (subsequent calls return cached instance).
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Enable debug mode</jc>
	 * 	<jk>var</jk> <jv>creator</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.beanStore(<jv>store</jv>)
	 * 		.debug();
	 *
	 * 	MyBean <jv>bean</jv> = <jv>creator</jv>.create();
	 *
	 * 	<jc>// Print debug log</jc>
	 * 	<jv>creator</jv>.getDebugLog().forEach(System.<jf>out</jf>::println);
	 * </p>
	 *
	 * @return This object.
	 */
	public BeanCreator2<T> debug() {
		this.debug = true;
		return this;
	}

	/**
	 * Returns an unmodifiable list of debug log entries from the last bean creation attempt.
	 *
	 * <p>
	 * This method returns the detailed log of steps taken during the most recent call to
	 * {@link #create()} or {@link #tryCreate()}. The log is only populated when {@link #debug()}
	 * mode is enabled.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Returns an empty list if {@link #debug()} was not called or if no creation has been attempted.
	 * 	<li class='note'>The log is reset at the beginning of each {@link #create()} call.
	 * 	<li class='note'>In singleton mode, the log reflects only the first creation (cached instances don't regenerate logs).
	 * 	<li class='note'>The returned list is unmodifiable.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>var</jk> <jv>creator</jv> = BeanCreator2.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.beanStore(<jv>store</jv>)
	 * 		.debug();
	 *
	 * 	<jk>try</jk> {
	 * 		<jv>creator</jv>.create();
	 * 	} <jk>catch</jk> (ExecutableException <jv>e</jv>) {
	 * 		<jc>// Print debug log to understand why creation failed</jc>
	 * 		<jv>creator</jv>.getDebugLog().forEach(System.<jf>err</jf>::println);
	 * 	}
	 * </p>
	 *
	 * @return An unmodifiable list of debug log entries.
	 */
	public List<String> getDebugLog() {
		return Collections.unmodifiableList(debugLog);
	}

	/**
	 * Automatically adds the created bean to the bean store after creation.
	 *
	 * <p>
	 * When this option is enabled, the successfully created bean will be automatically added to the
	 * bean store (if it's a {@link WritableBeanStore}) using the bean's type as the key.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Only works if the bean store is an instance of {@link WritableBeanStore}.
	 * 	<li class='note'>The bean is added after successful creation, injection, hooks, and validation.
	 * 	<li class='note'>In singleton mode, the bean is added only on the first creation (not on cached returns).
	 * 	<li class='note'>If creation fails, nothing is added to the store.
	 * 	<li class='note'>If the bean store already contains a bean with the same type, it will be replaced.
	 * 	<li class='note'>Fallback instances and {@link #impl(Object)} instances are also added to the store.
	 * 	<li class='note'>The bean type used as the key is the type passed to {@link #of(Class)}, not the subtype.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create bean and add it to the store</jc>
	 * 	MyBean <jv>bean</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.beanStore(<jv>writableStore</jv>)
	 * 		.addToStore()
	 * 		.create();
	 *
	 * 	<jc>// Bean is now available in the store</jc>
	 * 	MyBean <jv>sameBeanFromStore</jv> = <jv>writableStore</jv>.getBean(MyBean.<jk>class</jk>).get();
	 * 	<jsm>assertSame</jsm>(<jv>bean</jv>, <jv>sameBeanFromStore</jv>);
	 * </p>
	 *
	 * @return This object.
	 */
	public BeanCreator2<T> addToStore() {
		this.addToStore = true;
		return this;
	}

	/**
	 * Automatically adds the created bean to the bean store with a specific name.
	 *
	 * <p>
	 * When this option is enabled, the successfully created bean will be automatically added to the
	 * bean store (if it's a {@link WritableBeanStore}) using the bean's type and the specified name as the key.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Only works if the bean store is an instance of {@link WritableBeanStore}.
	 * 	<li class='note'>The bean is added after successful creation, injection, hooks, and validation.
	 * 	<li class='note'>In singleton mode, the bean is added only on the first creation (not on cached returns).
	 * 	<li class='note'>If creation fails, nothing is added to the store.
	 * 	<li class='note'>If the bean store already contains a bean with the same type and name, it will be replaced.
	 * 	<li class='note'>Fallback instances and {@link #impl(Object)} instances are also added to the store.
	 * 	<li class='note'>The bean type used as the key is the type passed to {@link #of(Class)}, not the subtype.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create multiple beans of the same type with different names</jc>
	 * 	UserService <jv>adminService</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(UserService.<jk>class</jk>)
	 * 		.beanStore(<jv>writableStore</jv>)
	 * 		.run(<jv>s</jv> -&gt; <jv>s</jv>.setRole(<js>"admin"</js>))
	 * 		.addToStore(<js>"adminUserService"</js>)
	 * 		.create();
	 *
	 * 	UserService <jv>guestService</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(UserService.<jk>class</jk>)
	 * 		.beanStore(<jv>writableStore</jv>)
	 * 		.run(<jv>s</jv> -&gt; <jv>s</jv>.setRole(<js>"guest"</js>))
	 * 		.addToStore(<js>"guestUserService"</js>)
	 * 		.create();
	 *
	 * 	<jc>// Retrieve by name</jc>
	 * 	UserService <jv>admin</jv> = <jv>writableStore</jv>.getBean(UserService.<jk>class</jk>, <js>"adminUserService"</js>).get();
	 * 	UserService <jv>guest</jv> = <jv>writableStore</jv>.getBean(UserService.<jk>class</jk>, <js>"guestUserService"</js>).get();
	 * </p>
	 *
	 * @param name The name to associate with the bean in the store. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> addToStore(String name) {
		assertArgNotNull("name", name);
		this.addToStore = true;
		this.addToStoreName = name;
		return this;
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
		store.get().add(type, bean);
		return this;
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
		store.get().add(type, bean);
		return bean;
	}

	/**
	 * Specifies an existing builder instance to use for creating the bean.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>When specified, the builder type detection is bypassed.
	 * 	<li class='note'>The builder will be used to create the bean via one of:
	 * 		<ul>
	 * 			<li>A <c>build()</c>/<c>create()</c>/<c>get()</c> method on the builder.
	 * 			<li>A static <c>getInstance(Builder)</c> method on the bean class.
	 * 			<li>A constructor on the bean class that accepts the builder.
	 * 		</ul>
	 * </ul>
	 *
	 * @param value The builder instance.
	 * @return This object.
	 */
	public BeanCreator2<T> builder(Object value) {
		builder.set(assertArgNotNull("value", value));
		return this;
	}

	/**
	 * Returns the builder instance if one has been created or specified.
	 *
	 * @param <B> The builder type.
	 * @return The builder instance wrapped in an {@link Optional}, or an empty optional if no builder exists.
	 */
	@SuppressWarnings("unchecked")
	public <B> Optional<B> getBuilder() {
		return (Optional<B>)builder.toOptional();
	}

	/**
	 * Specifies the builder type to use for creating the bean.
	 *
	 * <p>
	 * This method triggers builder type detection and instantiation for the specified type.
	 *
	 * <p>
	 * The builder will be instantiated by looking for:
	 * <ol>
	 * 	<li>A static <c>create()</c> or <c>builder()</c> method that returns the builder type.
	 * 	<li>A public constructor on the builder type with parameters that can be resolved from the bean store.
	 * 	<li>A protected constructor on the builder type with parameters that can be resolved from the bean store.
	 * </ol>
	 *
	 * @param value The builder type.
	 * @return This object.
	 */
	public BeanCreator2<T> builder(Class<?> value) {
		builderType = info(assertArgNotNull("value", value));
		return this;
	}

	private Object findBuilder() {

		var bs = store.get();
		ClassInfo builderType = determineBuilderType();

		// If no builder type was determined, return null (builder not needed)
		if (builderType == null)
			return null;

		Optional<Object> r;

		// Step 1: Look for a static create/builder method that returns the builder type
		r = findBuilderFromStaticMethod(beanSubType, builderType, bs);
		if (r.isPresent())
			return r.get();

		// Step 2: Look for a public constructor on the builder type
		r = builderType.getPublicConstructors().stream()
			.filter(x -> x.is(NOT_DEPRECATED))
			.filter(x -> hasAllParameters(x, bs, enclosingInstance))
			.sorted(comparing(ConstructorInfo::getParameterCount).reversed())
			.findFirst()
			.map(x -> injectBeans(invoke(x, bs, enclosingInstance), bs));

		if (r.isPresent())
			return r.get();

		// Step 3: Look for a protected constructor on the builder type
		r = builderType.getDeclaredConstructors().stream()
			.filter(x -> x.isAll(ElementFlag.PROTECTED, NOT_DEPRECATED))
			.filter(x -> hasAllParameters(x, bs, enclosingInstance))
			.sorted(comparing(ConstructorInfo::getParameterCount).reversed())
			.findFirst()
			.map(x -> injectBeans(invoke(x, bs, enclosingInstance), bs));

		return r.orElse(null);
	}

	/**
	 * Determines the builder type based on priority:
	 * 1. Explicitly set builderType
	 * 2. @Builder annotation on beanSubType (includes inherited annotations)
	 * 3. Autodetect from static methods or inner classes
	 */
	private ClassInfo determineBuilderType() {
		log("Determining builder type...");

		// Priority 1: Explicitly set builderType
		if (builderType != null) {
			log("Using explicitly set builder type: %s", builderType.getName());
			return builderType;
		}

		// Priority 2: @Builder annotation on beanSubType
		Optional<ClassInfo> r = beanSubType.getAnnotations(Builder.class)
			.map(x -> ClassInfo.class.cast(info(x.inner().value())))
			.findFirst();
		if (r.isPresent()) {
			log("Found builder via @Builder annotation: %s", r.get().getName());
			return r.get();
		}

		// Priority 3: Autodetect
		// 3a: Look for static create/builder method that returns a potential builder type
		log("Looking for builder via static method...");
		r = findBuilderTypeFromStaticMethod(beanSubType);
		if (r.isPresent()) {
			log("Found builder via static method: %s", r.get().getName());
			return r.get();
		}

		// 3b: Look for inner Builder class
		log("Looking for inner builder class (names: %s)...", builderClassNames);
		r = findInnerBuilderClass(beanSubType);
		if (r.isPresent()) {
			log("Found builder via inner class: %s", r.get().getName());
			return r.get();
		}

		if (beanSubType != beanType) {
			r = findInnerBuilderClass(beanType);
			if (r.isPresent()) {
				log("Found builder via parent inner class: %s", r.get().getName());
				return r.get();
			}
		}

		// No builder type found
		log("No builder type found");
		return null;
	}

	/**
	 * Looks for a static create/builder method that returns a builder instance.
	 */
	private Optional<Object> findBuilderFromStaticMethod(ClassInfo type, ClassInfo builderType, BasicBeanStore2 bs) {
		return type.getPublicMethods().stream()
			.filter(x -> x.isAll(STATIC, NOT_DEPRECATED))
			.filter(x -> hasName(x, builderMethodNames.toArray(new String[0])))
			.filter(x -> x.hasReturnType(builderType))
			.filter(x -> hasAllParameters(x, bs, enclosingInstance))
			.sorted(comparing(MethodInfo::getParameterCount).reversed())
			.findFirst()
			.map(x -> injectBeans(invoke(x, bs, enclosingInstance), bs));
	}

	/**
	 * Autodetects builder type from static create/builder methods.
	 * Returns the return type of a static create/builder method if it can be used as a builder.
	 */
	private Optional<ClassInfo> findBuilderTypeFromStaticMethod(ClassInfo type) {
		return type.getPublicMethods().stream()
			.filter(x -> x.isAll(STATIC, NOT_DEPRECATED))
			.filter(x -> hasName(x, builderMethodNames.toArray(new String[0])))
			.filter(x -> !x.hasReturnType(type)) // Must not return the bean type itself
			.filter(x -> isValidBuilderType(x.getReturnType()))
			.findFirst()
			.map(MethodInfo::getReturnType);
	}

	/**
	 * Looks for an inner class with a name matching builderClassNames.
	 */
	private Optional<ClassInfo> findInnerBuilderClass(ClassInfo type) {
		return type.getDeclaredMemberClasses().stream()
			.filter(x -> builderClassNames.contains(x.getNameSimple()))
			.filter(x -> isValidBuilderType(x))
			.findFirst();
	}

	/**
	 * Checks if a type can be used as a builder.
	 * A valid builder type must have either:
	 * - A build/create/get method that returns the bean type
	 * - Can be passed to a bean constructor
	 */
	private boolean isValidBuilderType(ClassInfo builderCandidate) {
		log("Validating builder candidate: %s", builderCandidate.getName());

		// Check if it has a build/create/get method that returns beanSubType or beanType
		boolean hasBuildMethod = builderCandidate.getPublicMethods().stream()
			.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED))
			.filter(x -> hasName(x, "build", "create", "get"))
			.filter(x -> hasReturnType(x, beanSubType, beanType))
			.findFirst()
			.isPresent();

		if (hasBuildMethod) {
			log("Builder is valid: has build/create/get method returning bean type");
			return true;
		}

		// Check if beanSubType has a constructor that accepts this builder type
		boolean hasConstructor = beanSubType.getPublicConstructors().stream()
			.filter(x -> x.is(NOT_DEPRECATED))
			.filter(x -> x.hasParameterTypeParent(builderCandidate))
			.findFirst()
			.isPresent();

		if (hasConstructor) {
			log("Builder is valid: bean has constructor accepting this builder");
			return true;
		}

		log("Builder is NOT valid: no build method returning bean type and no constructor accepting builder");
		return false;
	}

	/**
	 * Creates the bean.
	 *
	 * <p>
	 * After creating the bean, this method automatically injects dependencies into fields and methods
	 * annotated with {@code @Inject} or {@code @Autowired} using {@link InjectUtils#injectBeans(Object, BeanStore)}.
	 *
	 * @return A new bean with all dependencies injected.
	 * @throws ExecutableException if bean could not be created.
	 */
	@SuppressWarnings("unchecked")
	public T create() {
		// @formatter:off

		// Reset debug log if debug is enabled
		if (debug) {
			debugLog.clear();
			log("Starting bean creation for type: %s", beanType.getName());
			if (neq(beanSubType, beanType))
				log("Subtype specified: %s", beanSubType.getName());
		}

		// Return cached singleton if available
		if (singleton && cachedInstance != null) {
			log("Returning cached singleton instance");
			return cachedInstance;
		}

		var store = this.store.get();

		if (nn(beanImpl)) {
			log("Using pre-configured impl() instance");
			runPostCreateHooks(beanImpl);
			addBeanToStore(beanImpl, store);
			if (singleton)
				cachedInstance = beanImpl;
			return beanImpl;
		}

		var builder = this.builder.get();
		T bean = null;
		var methodComparator = comparing(MethodInfo::getParameterCount).reversed();
		var constructorComparator = comparing(ConstructorInfo::getParameterCount).reversed();

		if (builder != null) {
			log("Builder detected: %s", builder.getClass().getName());

			// Customize builder if customizer is registered
			if (builderCustomizer != null) {
				log("Applying builder customizer");
				builderCustomizer.accept(builder);
			}

			// Look for Builder.build().
			log("Attempting Builder.build/create/get()");
			bean = info(builder.getClass()).getPublicMethods().stream()
				.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED))
				.filter(x -> hasName(x, "build", "create", "get"))
				.filter(x -> x.hasReturnType(beanSubType))
				.filter(x -> hasAllParameters(x, store))
				.sorted(methodComparator)
				.findFirst()
				.map(x -> {
					log("Found builder method: %s", x.getFullName());
					return (T)beanType.cast(invoke(x, store, builder));
				})
				.orElse((T)null);

			// Look for Bean.factoryMethod(Builder).
			if (bean == null) {
				log("Attempting Bean.factoryMethod(Builder)");
				bean = beanType.getPublicMethods().stream()
					.filter(x -> x.isAll(STATIC, NOT_DEPRECATED))
					.filter(x -> isFactoryMethod(x))
					.filter(x -> x.hasReturnType(beanSubType))
					.filter(x -> hasAllParameters(x, store, builder))
					.filter(x -> x.hasParameter(builder))
					.sorted(methodComparator)
					.findFirst()
					.map(x -> {
						log("Found factory method accepting builder: %s", x.getFullName());
						return (T)beanType.cast(invoke(x, store, null, builder));
					})
					.orElse(null);
			}

			// Look for Bean(Builder).
			if (bean == null) {
				log("Attempting Bean(Builder) constructor");
				bean = beanSubType.getPublicConstructors().stream()
					.filter(x -> x.is(NOT_DEPRECATED))
					.filter(x -> x.isDeclaringClass(beanSubType))
					.filter(x -> hasAllParameters(x, store, enclosingInstance, builder))
					.sorted(constructorComparator)
					.findFirst()
					.map(x -> {
						log("Found constructor accepting builder: %s", x.getFullName());
						return (T)beanType.cast(invoke(x, store, enclosingInstance, builder));
					})
					.orElse(null);
			}

			// Look for Builder.anything().
			if (bean == null) {
				log("Attempting Builder.anything() - any method returning bean type");
				bean = info(builder.getClass()).getPublicMethods().stream()
					.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED))
					.filter(x -> x.hasReturnType(beanSubType))
					.filter(x -> hasAllParameters(x, store))
					.sorted(methodComparator)
					.findFirst()
					.map(x -> {
						log("Found builder method: %s", x.getFullName());
						return (T)beanType.cast(invoke(x, store, builder));
					})
					.orElse(null);
			}

			if (bean != null) {
				log("Bean created successfully via builder");
				bean = injectBeans(bean, store);
				runPostCreateHooks(bean);
				addBeanToStore(bean, store);
				if (singleton)
					cachedInstance = bean;
				return bean;
			}

			if (fallbackSupplier != null) {
				log("Using fallback supplier");
				T fallbackBean = fallbackSupplier.get();
				runPostCreateHooks(fallbackBean);
				addBeanToStore(fallbackBean, store);
				if (singleton)
					cachedInstance = fallbackBean;
				return fallbackBean;
			}

			log("Failed to create bean using builder");
			throw exex("Could not instantiate class {0} using builder type {1}.", beanSubType.getName(), builderType);
		}

		// Look for Bean.factoryMethod().
		log("Attempting Bean.factoryMethod()");
		bean = beanSubType.getPublicMethods().stream()
			.filter(x -> x.isAll(STATIC, NOT_DEPRECATED))
			.filter(x -> isFactoryMethod(x))
			.filter(x -> x.hasReturnType(beanSubType))
			.sorted(methodComparator)
			.findFirst()
			.map(x -> {
				log("Found factory method: %s", x.getFullName());
				return (T)beanType.cast(invoke(x, store, null));
			})
			.orElse(null);

		// Look for Bean().
		if (bean == null) {
			log("Attempting Bean() constructor");
			bean = beanSubType.getPublicConstructors().stream()
				.filter(x -> x.isAll(NOT_DEPRECATED))
				.filter(x -> x.isDeclaringClass(beanSubType))
				.filter(x -> hasAllParameters(x, store, enclosingInstance))
				.sorted(constructorComparator)
				.findFirst()
				.map(x -> {
					log("Found constructor: %s", x.getFullName());
					return (T)beanType.cast(invoke(x, store, enclosingInstance));
				})
				.orElse(null);
		}

		if (bean != null) {
			log("Bean created successfully");
			bean = injectBeans(bean, store);
			runPostCreateHooks(bean);
			validateBean(bean);
			addBeanToStore(bean, store);
			if (singleton)
				cachedInstance = bean;
			return bean;
		}

		if (beanSubType.isInterface()) {
			log("Bean type is an interface");
			if (fallbackSupplier != null) {
				log("Using fallback supplier");
				T fallbackBean = fallbackSupplier.get();
				runPostCreateHooks(fallbackBean);
				addBeanToStore(fallbackBean, store);
				if (singleton)
					cachedInstance = fallbackBean;
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
				addBeanToStore(fallbackBean, store);
				if (singleton)
					cachedInstance = fallbackBean;
				return fallbackBean;
			}
			throw exex("Could not instantiate class {0}: {1}.", beanSubType.getName(), "Class is abstract");
		}

		if (fallbackSupplier != null) {
			log("Using fallback supplier");
			T fallbackBean = fallbackSupplier.get();
			runPostCreateHooks(fallbackBean);
			addBeanToStore(fallbackBean, store);
			if (singleton)
				cachedInstance = fallbackBean;
			return fallbackBean;
		}

		log("Failed to create bean: no methods/constructors found with matching parameters");
		throw exex("Could not instantiate class {0}:  No methods/constructors found with matching parameters", beanSubType.getName());
	}

	/**
	 * Attempts to create the bean, returning an {@link Optional} instead of throwing an exception on failure.
	 *
	 * <p>
	 * This is a non-throwing variant of {@link #create()} that wraps the result in an {@link Optional}.
	 * If bean creation fails for any reason (missing dependencies, abstract class, interface, no matching constructor, etc.),
	 * this method returns {@link Optional#empty()} instead of throwing {@link ExecutableException}.
	 *
	 * <p>
	 * This method is useful when bean creation failure is expected and should not be treated as exceptional,
	 * such as when implementing optional features or trying multiple creation strategies.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Unlike {@link #create()}, this method catches {@link ExecutableException} and returns empty.
	 * 	<li class='note'>If a fallback is registered via {@link #orElse(Supplier)} or {@link #orElse(Object)},
	 * 		it will be used before returning empty, so this method will only return empty if both
	 * 		normal creation AND fallback fail.
	 * 	<li class='note'>In singleton mode, if creation succeeds, the instance is cached for subsequent calls.
	 * 	<li class='note'>Post-creation hooks are still executed if creation succeeds.
	 * 	<li class='note'>Other exceptions (e.g., from post-creation hooks or fallback suppliers) are NOT caught
	 * 		and will propagate to the caller.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Try to create, use default if it fails</jc>
	 * 	MyBean <jv>bean</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.beanStore(<jv>myBeanStore</jv>)
	 * 		.asOptional()
	 * 		.orElse(<jk>new</jk> DefaultMyBean());
	 *
	 * 	<jc>// Chain multiple creation attempts</jc>
	 * 	MyService <jv>service</jv> = BeanCreator2.<jsm>of</jsm>(AdvancedService.<jk>class</jk>)
	 * 		.beanStore(<jv>store</jv>)
	 * 		.asOptional()
	 * 		.or(() -&gt; BeanCreator2.<jsm>of</jsm>(BasicService.<jk>class</jk>)
	 * 			.beanStore(<jv>store</jv>)
	 * 			.asOptional())
	 * 		.orElseGet(() -&gt; <jk>new</jk> FallbackService());
	 *
	 * 	<jc>// Check if optional feature is available</jc>
	 * 	Optional&lt;OptionalFeature&gt; <jv>feature</jv> = BeanCreator2
	 * 		.<jsm>of</jsm>(OptionalFeature.<jk>class</jk>)
	 * 		.beanStore(<jv>store</jv>)
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
			return Optional.of(create());
		} catch (ExecutableException e) {
			return Optional.empty();
		}
	}

	/**
	 * Converts this creator into a supplier.
	 *
	 * @return A supplier that returns the results of the {@link #create()} method.
	 */
	public Supplier<T> asSupplier() {
		return () -> create();
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
	 * 	<li class='note'>If {@link #singleton()} mode is enabled on this creator, the singleton cache
	 * 		is separate from the ResettableSupplier's cache. Calling reset() on the supplier will NOT
	 * 		clear the creator's singleton cache.
	 * 	<li class='note'>Each call to {@link ResettableSupplier#get()} after a reset() will invoke
	 * 		{@link #create()}, which may trigger dependency injection and post-creation hooks.
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
	 * 		.<jsm>of</jsm>(MyBean.<jk>class</jk>)
	 * 		.beanStore(<jv>store</jv>)
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
		return new ResettableSupplier<>(() -> create());
	}

	/**
	 * Specifies a subclass of the bean type to create.
	 *
	 * <p>
	 * This allows you to create a specific implementation when the bean type is an interface, abstract class,
	 * or when you want to instantiate a customized subclass of a concrete class.
	 *
	 * @param value The subclass type to create. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> beanSubType(Class<T> value) {
		beanSubType = info(assertArgNotNull("value", value));
		return this;
	}

	private void runPostCreateHooks(T bean) {
		for (Consumer<T> hook : postCreateHooks) {
			hook.accept(bean);
		}
	}

	private void validateBean(T bean) {
		if (validator != null && !validator.test(bean)) {
			throw exex("Bean validation failed for {0}", beanType.getName());
		}
	}

	private void log(String message, Object... args) {
		if (debug) {
			debugLog.add(args.length == 0 ? message : String.format(message, args));
		}
	}

	private void addBeanToStore(T bean, BeanStore store) {
		if (addToStore && parentStore instanceof WritableBeanStore) {
			WritableBeanStore writableStore = (WritableBeanStore) parentStore;
			if (addToStoreName != null) {
				log("Adding bean to store with name: %s", addToStoreName);
				log("  beanType.inner() = %s", beanType.inner());
				log("  bean = %s", bean);
				log("  parentStore = %s", parentStore);
				writableStore.addBean(beanType.inner(), bean, addToStoreName);
				log("Bean added successfully with name");
			} else {
				log("Adding bean to store with type: %s", beanType.getName());
				log("  beanType.inner() = %s", beanType.inner());
				log("  bean = %s @ %s", bean, System.identityHashCode(bean));
				log("  parentStore = %s @ %s", parentStore, System.identityHashCode(parentStore));
				WritableBeanStore result = writableStore.addBean(beanType.inner(), bean);
				log("  addBean returned: %s @ %s", result, System.identityHashCode(result));
				log("Bean added successfully without name");
			}
		} else if (addToStore) {
			log("Parent store is not WritableBeanStore, cannot add bean");
		}
	}

	private boolean isFactoryMethod(MethodInfo mi) {
		for (String name : factoryMethodNames) {
			if (mi.hasName(name))
				return true;
		}
		return false;
	}

	private static boolean hasName(MethodInfo ei, String...names) {
		for (var s : names)
			if (ei.hasName(s))
				return true;
		return false;
	}

	private static boolean hasReturnType(MethodInfo mi, ClassInfo...types) {
		for (var type : types)
			if (mi.hasReturnType(type))
				return true;
		return false;
	}
}