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

import static java.util.stream.Collectors.*;
import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.reflect.Visibility.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.commons.inject.InjectUtils.*;
import static java.util.Comparator.*;
import static org.apache.juneau.commons.reflect.ElementFlag.*;


import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;

/**
 * Utility class for creating beans through constructors, creator methods, and builders.
 *
 * <p>
 * Uses a {@link BasicBeanStore} to find available ways to construct beans via injection of beans from the store.
 *
 * <p>
 * This class is instantiated through the following method:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link BasicBeanStore}
 * 		<ul class='javatreec'>
 * 			<li class='jc'>{@link BeanCreator2#of(Class,BasicBeanStore)}
 * 		</ul>
 * 	</li>
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct and throw a RuntimeException using a bean store.</jc>
 * 	<jk>throw</jk> BasicBeanStore
 * 		.<jsm>create</jsm>()
 * 		.build()
 * 		.addBean(Throwable.<jk>class</jk>, <jv>cause</jv>)
 * 		.addBean(String.<jk>class</jk>, <jv>msg</jv>)
 * 		.addBean(Object[].<jk>class</jk>, <jv>args</jv>)
 * 		<ja>// Use BeanCreator.of(RuntimeException.class, beanStore)</ja>
 * 		BeanCreator.<jsm>of</jsm>(RuntimeException.<jk>class</jk>, <jv>beanStore</jv>)
 * 		.run();
 * </p>
 *
 * <p>
 * Looks for the following methods for creating a bean:
 * <ol class='spaced-list'>
 * 	<li>Looks for a singleton no-arg method of the form:
 * 		<p class='bjava'>
 * 	<jk>public static</jk> MyClass <jsm>getInstance</jsm>();
 * 		</p>
 * 		<ul>
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * 	<li>Looks for a static creator method of the form:
 * 		<p class='bjava'>
 * 	<jk>public static</jk> MyClass <jsm>create</jsm>(<ja>&lt;args&gt;</ja>);
 * 		</p>
 * 		<ul>
 * 			<li>All arguments except {@link Optional} and {@link List} parameters must have beans available in the store.
 * 			<li>If multiple methods are found, the one with the most matching parameters is used.
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 * 		</ul>
 * 	<li>Looks for a public constructor of the form:
 * 		<p class='bjava'>
 * 	<jk>public</jk> MyClass(<ja>&lt;args&gt;</ja>);
 * 		</p>
 * 		<ul>
 * 			<li>All arguments except {@link Optional} and {@link List} parameters must have beans available in the store.
 * 			<li>If multiple methods are found, the one with the most matching parameters is used.
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * 	<li>Looks for a protected constructor of the form:
 * 		<p class='bjava'>
 * 	<jk>protected</jk> MyClass(<ja>&lt;args&gt;</ja>);
 * 		</p>
 * 		<ul>
 * 			<li>All arguments except {@link Optional} and {@link List} parameters must have beans available in the store.
 * 			<li>If multiple methods are found, the one with the most matching parameters is used.
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * 	<li>Looks for a static no-arg create method that returns a builder object that can be passed in to a protected constructor.
 * 		<p class='bjava'>
 * 	<jk>public static</jk> MyClass.Builder <jsm>create</jsm>();
 *
 * 	<jk>protected</jk> MyClass(MyClass.Builder <jv>builder</jv>);
 * 		</p>
 * 		<ul>
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * </ol>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>The {@link #builder(Class,Object)} method can be used to set an existing initialized builder object to pass to a constructor.
 * 	<li class='note'>An existing initialized builder can be set using the {@link #builder(Class,Object)} method.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicBeanStore}
 * </ul>
 *
 * @param <T> The bean beanSubType being created.
 */
public class BeanCreator2<T> {

	/**
	 * Shortcut for calling <c>BeanCreator.of(beanSubType, BasicBeanStore.INSTANCE)</c>.
	 *
	 * @param <T> The bean beanSubType to create.
	 * @param beanSubType The bean beanSubType to create.
	 * @return A new creator.
	 */
	public static <T> BeanCreator2<T> of(Class<T> beanType) {
		return new BeanCreator2<>(beanType);
	}

	private BeanStore parentStore;
	private Supplier<BasicBeanStore2> store = mem(()-> new BasicBeanStore2(parentStore));
	private ClassInfoTyped<T> beanType;
	private ClassInfo beanSubType;
	private ResettableSupplier<ClassInfo> builderType = memr(() -> findBuilderType());;
	private ResettableSupplier<Object> builder = memr(() -> findBuilder());
	private Supplier<ExecutableInfo> builderAcceptor = memr(() -> findBuilderAcceptor());
	private T beanImpl;
	private Object enclosingInstance;
	private boolean silent;

	/**
	 * Constructor.
	 *
	 * @param beanSubType The bean type being created.
	 */
	protected BeanCreator2(Class<T> beanType) {
		this.beanType = info(assertArgNotNull("beanType", beanType));
		this.beanSubType = this.beanType;
	}

	public BeanCreator2 beanStore(BeanStore value) {
		parentStore = value;
		return this;
	}
	/**
	 * Specifies the enclosingInstance object to use if/when instantiating inner classes.
	 *
	 * @param enclosingInstance The enclosingInstance object.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public BeanCreator2<T> enclosingInstance(Object outer) {
		this.enclosingInstance = outer;
		return this;
	}

	public <T2> BeanCreator2<T> addBean(Class<T2> type, T2 bean) {
		store.get().add(type, bean);
		return this;
	}

	public <T2> T2 add(Class<T2> type, T2 bean) {
		store.get().add(type, bean);
		return bean;
	}

	/**
	 * Specifies a builder object for the bean beanSubType.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>When specified, we don't look for a static creator method.
	 * </ul>
	 *
	 * @param beanSubType The class beanSubType of the builder.
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BeanCreator2<T> builder(Object value) {
		builder.set(assertArgNotNull("value", value));
		return this;
	}

	/**
	 * Creates a builder instance of the specified beanSubType.
	 *
	 * <p>
	 * Looks for a builder instance in the following order:
	 * <ol>
	 * 	<li>A public static method on the enclosingInstance class that returns the builder beanSubType.
	 * 		The method must be named <c>create</c> or <c>builder</c>, be static, not deprecated,
	 * 		and have parameters that can be resolved from the bean store.
	 * 	<li>A public constructor on the builder beanSubType with parameters that can be resolved from the bean store.
	 * </ol>
	 *
	 * @param <B> The builder beanSubType.
	 * @param builderType The builder beanSubType to create.
	 * @return A builder instance.
	 * @throws ExecutableException If the builder could not be created.
	 */
	@SuppressWarnings("unchecked")
	public BeanCreator2<T> builder(Class<?> value) {
		builderType.set(info(assertArgNotNull("value", value)));
		return this;
	}

	private Object findBuilder() {

//		var bs = store.get();
//
//		// First, look for a static create/builder method on the enclosingInstance class that returns the builder beanSubType
//		var m1 = beanSubType.getPublicMethods().stream()
//			.filter(x -> x.isStatic() && x.isNotDeprecated() && x.hasReturnType(builderType) && (x.hasName("create") || x.hasName("builder")))
//			.filter(x -> hasAllParameters(x, bs, enclosingInstance))
//			.sorted(comparing(MethodInfo::getParameterCount).reversed())
//			.findFirst();
//
//		if (m1.isPresent()) {
//			builder = add(builderType, injectBeans(invoke(m1.get(), bs, enclosingInstance), bs));
//			return (B)builder;
//		}
//
//		// Second, look for a public constructor on the builder beanSubType
//		var m2 = bt.getPublicConstructors().stream()
//			.filter(x -> hasAllParameters(x, bs, enclosingInstance))
//			.sorted(comparing(ConstructorInfo::getParameterCount).reversed())
//			.findFirst();
//
//		if (m2.isPresent()) {
//			builder = add(builderType, injectBeans(invoke(m2.get(), bs, enclosingInstance), bs));
//			return (B)builder;
//		}
//
//		// If silent mode, return null; otherwise throw exception
//		if (silent)
//			return null;
//
//		throw exex("Could not create builder {0} for enclosingInstance class {1}: No suitable static create/builder method or public constructor found with resolvable parameters.", cn(builderType), cn(beanSubType));
		return null;
	}

	private ExecutableInfo findBuilderAcceptor() {
		return null;
	}

	private ClassInfo findBuilderType() {
		return null;
	}

	/**
	 * Creates the bean.
	 *
	 * @return A new bean.
	 * @throws ExecutableException if bean could not be created and {@link #silent()} was not enabled.
	 */
	public Optional<T> run() {
		// @formatter:off

		if (nn(beanImpl))
			return opt(beanImpl);

		var store = this.store.get();
		var builder = findBuilder();
		var builderType = findBuilderType();
		Optional<T> r;
		var methodComparator = comparing(MethodInfo::getParameterCount).reversed();
		var constructorComparator = comparing(ConstructorInfo::getParameterCount).reversed();
//
//		if (builder != null) {
//
//			// Look for Builder.build().
//			r = builderType.getPublicMethods().stream()
//				.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED) && hasName(x, "build", "create", "get") && x.hasReturnType(beanSubType) && hasAllParameters(x, store))
//				.sorted(methodComparator)
//				.findFirst()
//				.map(x -> beanType.cast(invoke(x, store, builder)));
//
//			// Look for Bean.getInstance(Builder).
//			if (r.isEmpty())
//				r = beanType.getPublicMethods().stream()
//					.filter(x -> x.isAll(STATIC, NOT_DEPRECATED) && hasName(x, "getInstance") && x.hasReturnType(beanSubType) && hasAllParameters(x, store, builder) && x.hasParameter(builder))
//					.sorted(methodComparator)
//					.findFirst()
//					.map(x -> beanType.cast(invoke(x, store, builder)));
//
//			// Look for Bean(Builder).
//			if (r.isEmpty())
//				r = beanSubType.getPublicConstructors().stream()
//					.filter(x -> x.is(NOT_DEPRECATED) && x.isDeclaringClass(beanSubType) && hasAllParameters(x, store, enclosingInstance, builder))
//					.sorted(constructorComparator)
//					.findFirst()
//					.map(x -> beanType.cast(invoke(x, store, enclosingInstance, builder)));
//
//			// Look for Builder.anything().
//			if (r.isEmpty())
//				r = builderType.getPublicMethods().stream()
//					.filter(x -> x.isAll(NOT_STATIC, NOT_DEPRECATED) && x.hasReturnType(beanSubType) && hasAllParameters(x, store))
//					.sorted(methodComparator)
//					.findFirst()
//					.map(x -> beanType.cast(invoke(x, store, builder)));
//
//			if (r.isPresent())
//				return r;
//
//			if (silent)
//				return null;
//
//			throw new ExecutableException("Could not instantiate class {0} using builder type {1}.", beanSubType.getName(), builderType);
//		}
//
//		// Look for Bean.getInstance().
//		r = beanSubType.getPublicMethods().stream()
//			.filter(x -> x.isAll(STATIC, NOT_DEPRECATED) && hasName(x, "getInstance") && x.hasReturnType(beanSubType))
//			.sorted(methodComparator)
//			.findFirst()
//			.map(x -> beanType.cast(invoke(x, store)));
//
//		// Look for Bean().
//		beanSubType.getPublicConstructors().stream()
//			.filter(x -> x.isAll(NOT_DEPRECATED) && x.isDeclaringClass(beanSubType) && hasAllParams(x, store, enclosingInstance))
//			.sorted(constructorComparator)
//			.findFirst()
//			.map(x -> beanType.cast(invoke(x, store)));
//
//
//		if (beanSubType.isInterface()) {
//			if (silent)
//				return null;
//			throw new ExecutableException("Could not instantiate class {0}: {1}.", beanSubType.getName(), "Class is an interface");
//		}
//
//		if (beanSubType.isAbstract()) {
//			if (silent)
//				return null;
//			throw new ExecutableException("Could not instantiate class {0}: {1}.", beanSubType.getName(), "Class is abstract");
//		}

		return null;

	}

	/**
	 * Suppresses throwing of {@link ExecutableException ExecutableExceptions} from the {@link #run()} method when
	 * a form of creation cannot be found.
	 *
	 * @return This object.
	 */
	public BeanCreator2<T> silent() {
		silent = true;
		return this;
	}

	/**
	 * Converts this creator into a supplier.
	 *
	 * @return A supplier that returns the results of the {@link #run()} method.
	 */
	public Supplier<T> supplier() {
		return () -> run().get();
	}

	/**
	 * Allows you to specify a subclass of the specified bean beanSubType to create.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	public BeanCreator2<T> type(Class<T> value) {
		beanSubType = opt(value).map(x -> info(x)).orElse(null);
		return this;
	}

	/**
	 * Allows you to specify a subclass of the specified bean beanSubType to create.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	public BeanCreator2<T> type(ClassInfo value) {
		return type(value == null ? null : value.inner());
	}

	private boolean isStaticCreateMethod(MethodInfo m) {
		return isStaticCreateMethod(m, beanSubType.inner());
	}

	private static boolean isStaticCreateMethod(MethodInfo m, Class<?> type) {
		// @formatter:off
		return m.isStatic()
			&& m.isNotDeprecated()
			&& m.hasReturnType(type)
			&& (m.hasName("create") || m.hasName("builder"));
		// @formatter:on
	}

	private static boolean hasName(MethodInfo ei, String...names) {
		for (var s : names)
			if (ei.hasName(s))
				return true;
		return false;
	}
}