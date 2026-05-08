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
package org.apache.juneau.cp;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;

import java.util.function.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.concurrent.*;
import org.apache.juneau.commons.inject.BeanCreationException;
import org.apache.juneau.commons.inject.WritableBeanStore;
import org.apache.juneau.commons.reflect.*;

/**
 * Java bean store.
 *
 * <p>
 * A simple storage database for beans keyed by type and name.
 * Used to retrieve and instantiate beans using an injection-like API.
 * It's similar in concept to the injection framework of Spring but greatly simplified in function and not intended to implement a full-fledged injection framework.
 *
 * <p>
 * Beans can be stored with or without names.  Named beans are typically resolved using
 * the <ja>@Name</ja> or <ja>@Qualified</ja> annotations on constructor or method parameters.
 *
 * <p>
 * Beans are added through the following methods:
 * <ul class='javatreec'>
 * 	<li class='jm'>{@link #add(Class,Object) add(Class,Object)}
 * 	<li class='jm'>{@link #add(Class,Object,String) add(Class,Object,String)}
 * 	<li class='jm'>{@link #addBean(Class,Object) addBean(Class,Object)}
 * 	<li class='jm'>{@link #addBean(Class,Object,String) addBean(Class,Object,String)}
 * 	<li class='jm'>{@link #addSupplier(Class,Supplier) addSupplier(Class,Supplier)}
 * 	<li class='jm'>{@link #addSupplier(Class,Supplier,String) addSupplier(Class,Supplier,String)}
 * 	<li class='jm'>{@link #addDefaultSupplier(Class,Supplier) addDefaultSupplier(Class,Supplier)} (last-resort fallback)
 * 	<li class='jm'>{@link #addDefaultSupplier(Class,Supplier,String) addDefaultSupplier(Class,Supplier,String)}
 * </ul>
 *
 * <h5 class='section'>Resolution order:</h5>
 * <p>
 * Bean lookups consult the following sources in this order, returning the first match:
 * <ol>
 * 	<li>{@linkplain Builder#overridingParent(BasicBeanStore) Overriding parent} (e.g. a Spring application context bridge), if non-{@code null}.
 * 	<li>Local entries added via {@link #addBean(Class,Object) addBean} / {@link #addSupplier(Class,Supplier) addSupplier}.
 * 	<li>Regular {@linkplain Builder#parent(BasicBeanStore) parent}, if non-{@code null}.
 * 	<li>Local default suppliers added via {@link #addDefaultSupplier(Class,Supplier) addDefaultSupplier} (memoizer-backed framework defaults).
 * </ol>
 *
 * <p>
 * Beans are retrieved through the following methods:
 * <ul class='javatreec'>
 * 	<li class='jm'>{@link #getBean(Class) getBean(Class)}
 * 	<li class='jm'>{@link #getBean(Class,String) getBean(Class,String)}
 * </ul>
 *
 * <p>
 * Type bindings (a class type mapped to an implementation class for deferred construction) are managed through:
 * <ul class='javatreec'>
 * 	<li class='jm'>{@link #addBeanType(Class,Class) addBeanType(Class,Class)}
 * 	<li class='jm'>{@link #getBeanType(Class) getBeanType(Class)}
 * </ul>
 *
 * <p>
 * Beans are created through the following methods:
 * <ul class='javatreec'>
 * 	<li class='jm'>{@link BeanCreator#of(Class,BasicBeanStore) BeanCreator.of(Class,BasicBeanStore)}
 * 	<li class='jc'>{@link BeanCreateMethodFinder#BeanCreateMethodFinder(Class,Class,BasicBeanStore) BeanCreateMethodFinder(Class,Class,BasicBeanStore)}
 * 	<li class='jc'>{@link BeanCreateMethodFinder#BeanCreateMethodFinder(Class,Object,BasicBeanStore) BeanCreateMethodFinder(Class,Object,BasicBeanStore)}
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Bean stores can be nested using {@link Builder#parent(BasicBeanStore)}.
 * 	<li class='note'>Bean stores can be made read-only using {@link Builder#readOnly()}.
 * 	<li class='note'>Bean stores can be made thread-safe using {@link Builder#threadSafe()}.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class BasicBeanStore implements WritableBeanStore {

	// Argument name constants for assertArgNotNull
	private static final String ARG_bean = "bean";
	private static final String ARG_type = "type";

	// Property name constants
	private static final String PROP_entries = "entries";
	private static final String PROP_identity = "identity";
	private static final String PROP_parent = "parent";
	private static final String PROP_readOnly = "readOnly";
	private static final String PROP_threadSafe = "threadSafe";

	/**
	 * Builder class.
	 */
	public static class Builder {

		BasicBeanStore parent;
		BasicBeanStore overridingParent;
		boolean readOnly;
		boolean threadSafe;
		Class<? extends BasicBeanStore> type;
		BasicBeanStore impl;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Instantiates this bean store.
		 *
		 * @return A new bean store.
		 */
		public BasicBeanStore build() {
			if (nn(impl))
				return impl;
			if (type == null || type == BasicBeanStore.class)
				return new BasicBeanStore(this);

			var c = info(type);

			// @formatter:off
			var result = c.getDeclaredMethod(
				x -> x.isPublic()
				&& x.getParameterCount() == 0
				&& x.isStatic()
				&& x.hasName("getInstance")
			).map(m -> m.<BasicBeanStore>invoke(null));
			// @formatter:on
			if (result.isPresent())
				return result.get();

			result = c.getPublicConstructor(x -> x.canAccept(this)).map(ci -> ci.<BasicBeanStore>newInstance(this));
			if (result.isPresent())
				return result.get();

			result = c.getDeclaredConstructor(x -> x.isProtected() && x.canAccept(this)).map(ci -> ci.accessible().<BasicBeanStore>newInstance(this));
			if (result.isPresent())
				return result.get();

			throw rex("Could not find a way to instantiate class {0}", cn(type));
		}

		/**
		 * Overrides the bean to return from the {@link #build()} method.
		 *
		 * @param value The bean to return from the {@link #build()} method.
		 * @return This object.
		 */
		public Builder impl(BasicBeanStore value) {
			impl = value;
			return this;
		}


		/**
		 * Specifies the parent bean store.
		 *
		 * <p>
		 * Bean searches are performed recursively up this parent chain.
		 *
		 * @param value The setting value.
		 * @return  This object.
		 */
		public Builder parent(BasicBeanStore value) {
			parent = value;
			return this;
		}

		/**
		 * Specifies an overriding-parent bean store.
		 *
		 * <p>
		 * The overriding parent is consulted <i>before</i> any local entries during bean lookup, allowing an outer
		 * scope (typically a Spring {@code ApplicationContext} bridge) to take precedence over local registrations.
		 * The regular {@link #parent(BasicBeanStore)} continues to be consulted as a fallback after local entries.
		 *
		 * @param value The overriding-parent bean store.  Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder overridingParent(BasicBeanStore value) {
			overridingParent = value;
			return this;
		}

		/**
		 * Specifies that the bean store is read-only.
		 *
		 * <p>
		 * This means methods such as {@link BasicBeanStore#addBean(Class, Object)} cannot be used.
		 *
		 * @return  This object.
		 */
		public Builder readOnly() {
			readOnly = true;
			return this;
		}

		/**
		 * Specifies that the bean store being created should be thread-safe.
		 *
		 * @return  This object.
		 */
		public Builder threadSafe() {
			threadSafe = true;
			return this;
		}

		/**
		 * Overrides the bean store type.
		 *
		 * <p>
		 * The specified type must have one of the following:
		 * <ul>
		 * 	<li>A static <c>getInstance()</c> method.
		 * 	<li>A public constructor that takes in this builder.
		 * 	<li>A protected constructor that takes in this builder.
		 * </ul>
		 *
		 * @param value The bean store type.
		 * @return This object.
		 */
		public Builder type(Class<? extends BasicBeanStore> value) {
			type = value;
			return this;
		}
	}

	/**
	 * Non-existent bean store.
	 */
	public static final class Void extends BasicBeanStore {}

	/**
	 * Static read-only reusable instance.
	 */
	public static final BasicBeanStore INSTANCE = create().readOnly().build();

	/**
	 * Static creator.
	 *
	 * @return A new {@link Builder} object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Static creator.
	 *
	 * @param parent Parent bean store.  Can be <jk>null</jk> if this is the root resource.
	 * @return A new {@link BasicBeanStore} object.
	 */
	public static BasicBeanStore of(BasicBeanStore parent) {
		return create().parent(parent).build();
	}

	private final Deque<Entry<?>> entries;
	private final Map<Class<?>,Entry<?>> unnamedEntries;
	private final Deque<Entry<?>> defaults;
	private final Map<Class<?>,Entry<?>> unnamedDefaults;
	private final Map<Class<?>,Class<?>> beanTypes;

	final Optional<BasicBeanStore> parent;
	final Optional<BasicBeanStore> overridingParent;
	final boolean readOnly;
	final boolean threadSafe;
	final SimpleReadWriteLock lock;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	protected BasicBeanStore(Builder builder) {
		parent = opt(builder.parent);
		overridingParent = opt(builder.overridingParent);
		readOnly = builder.readOnly;
		threadSafe = builder.threadSafe;
		lock = threadSafe ? new SimpleReadWriteLock() : SimpleReadWriteLock.NO_OP;
		entries = threadSafe ? new ConcurrentLinkedDeque<>() : new LinkedList<>();
		unnamedEntries = threadSafe ? new ConcurrentHashMap<>() : map();
		defaults = threadSafe ? new ConcurrentLinkedDeque<>() : new LinkedList<>();
		unnamedDefaults = threadSafe ? new ConcurrentHashMap<>() : map();
		beanTypes = threadSafe ? new ConcurrentHashMap<>() : map();
		var e = createEntry(BasicBeanStore.class, ()->this, null);
		entries.addFirst(e);
		unnamedEntries.put(BasicBeanStore.class, e);
	}

	BasicBeanStore() {
		this(create());
	}

	/**
	 * Same as {@link #addBean(Class,Object)} but returns the bean instead of this object for fluent calls.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean.  Can be <jk>null</jk>.
	 * @return The bean.
	 */
	public <T> T add(Class<T> beanType, T bean) {
		add(beanType, bean, null);
		return bean;
	}

	/**
	 * Same as {@link #addBean(Class,Object,String)} but returns the bean instead of this object for fluent calls.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean.  Can be <jk>null</jk>.
	 * @param name The bean name if this is a named bean.  Can be <jk>null</jk>.
	 * @return The bean.
	 */
	public <T> T add(Class<T> beanType, T bean, String name) {
		addBean(beanType, bean, name);
		return bean;
	}

	/**
	 * Adds an unnamed bean of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore addBean(Class<T> beanType, T bean) {
		return addBean(beanType, bean, null);
	}

	/**
	 * Adds a named bean of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean.  Can be <jk>null</jk>.
	 * @param name The bean name if this is a named bean.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore addBean(Class<T> beanType, T bean, String name) {
		return addSupplier(beanType, () -> bean, name);
	}

	/**
	 * Adds a supplier for an unnamed bean of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean supplier.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore addSupplier(Class<T> beanType, Supplier<T> bean) {
		return addSupplier(beanType, bean, null);
	}

	/**
	 * Adds a supplier for a named bean of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean supplier.
	 * @param name The bean name if this is a named bean.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore addSupplier(Class<T> beanType, Supplier<T> bean, String name) {
		assertCanWrite();
		var e = createEntry(beanType, bean, name);
		try (var x = lock.write()) {
			entries.addFirst(e);
			if (e(name))
				unnamedEntries.put(beanType, e);
		}
		return this;
	}

	/**
	 * Adds a fallback supplier for an unnamed bean of the specified type to this factory.
	 *
	 * <p>
	 * Default suppliers are consulted only after local {@linkplain #addBean(Class,Object) entries} and the regular
	 * {@linkplain Builder#parent(BasicBeanStore) parent} have been searched.  They are intended for memoizer-backed
	 * framework defaults that should not shadow explicit user registrations or beans inherited from an
	 * {@linkplain Builder#overridingParent(BasicBeanStore) overriding parent} (e.g. Spring).
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean supplier.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore addDefaultSupplier(Class<T> beanType, Supplier<T> bean) {
		return addDefaultSupplier(beanType, bean, null);
	}

	/**
	 * Adds a fallback supplier for a named bean of the specified type to this factory.
	 *
	 * <p>
	 * See {@link #addDefaultSupplier(Class,Supplier)} for ordering semantics.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean supplier.
	 * @param name The bean name if this is a named bean.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore addDefaultSupplier(Class<T> beanType, Supplier<T> bean, String name) {
		assertCanWrite();
		var e = createEntry(beanType, bean, name);
		try (var x = lock.write()) {
			defaults.addFirst(e);
			if (e(name))
				unnamedDefaults.put(beanType, e);
		}
		return this;
	}

	/**
	 * Clears out all bean in this bean store.
	 *
	 * <p>
	 * Does not affect the parent bean store.
	 *
	 * @return This object.
	 */
	@Override
	public BasicBeanStore clear() {
		assertCanWrite();
		try (var x = lock.write()) {
			unnamedEntries.clear();
			entries.clear();
			unnamedDefaults.clear();
			defaults.clear();
			beanTypes.clear();
		}
		return this;
	}

	/**
	 * Adds a type binding to this bean store.
	 *
	 * <p>
	 * Type bindings allow a class type to be mapped to an implementation class for deferred construction.
	 * Unlike {@link #addBean(Class,Object)} which stores an instance, this method stores only the implementation
	 * class to be instantiated later by the caller.
	 *
	 * <p>
	 * Type bindings are inherited through the parent chain via {@link #getBeanType(Class)}.
	 *
	 * @param <T> The class type to associate with the implementation.
	 * @param beanType The class type to associate with the implementation.
	 * @param implType The implementation class.  Must be a subtype of <c>beanType</c>.
	 * @return This object.
	 */
	public <T> BasicBeanStore addBeanType(Class<T> beanType, Class<? extends T> implType) {
		assertCanWrite();
		try (var x = lock.write()) {
			beanTypes.put(beanType, implType);
		}
		return this;
	}

	/**
	 * Returns the implementation class registered for the specified bean type.
	 *
	 * <p>
	 * If no binding is found in this store, the parent chain is consulted recursively.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to look up.
	 * @return The registered implementation class, or {@link Optional#empty()} if no binding exists.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked cast; addBeanType enforces the bound at insertion time.
	})
	public <T> Optional<Class<? extends T>> getBeanType(Class<T> beanType) {
		try (var x = lock.read()) {
			var c = beanTypes.get(beanType);
			if (nn(c))
				return opt((Class<? extends T>) c);
			if (parent.isPresent())
				return parent.get().getBeanType(beanType);
			return opte();
		}
	}

	/**
	 * Instantiates a bean creator.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link BeanCreator} for usage.
	 * </ul>
	 *


	/**
	 * Returns the unnamed bean of the specified type.
	 *
	 * @param <T> The type of bean to return.
	 * @param beanType The type of bean to return.
	 * @return The bean.
	 */
	@Override
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked cast
	})
	public <T> Optional<T> getBean(Class<T> beanType) {
		try (var x = lock.read()) {
			// (1) Overriding parent (e.g. Spring) — wins over local entries.
			if (overridingParent.isPresent()) {
				var fromOverriding = overridingParent.get().getBean(beanType);
				if (fromOverriding.isPresent())
					return fromOverriding;
			}
			// (2) Local entries.
			var e = (Entry<T>)unnamedEntries.get(beanType);
			if (nn(e))
				return opt(e.get());
			// (3) Regular parent fallback.
			if (parent.isPresent()) {
				var fromParent = parent.get().getBean(beanType);
				if (fromParent.isPresent())
					return fromParent;
			}
			// (4) Local default suppliers.
			var d = (Entry<T>)unnamedDefaults.get(beanType);
			if (nn(d))
				return opt(d.get());
			return opte();
		}
	}

	/**
	 * Returns the named bean of the specified type.
	 *
	 * @param <T> The type of bean to return.
	 * @param beanType The type of bean to return.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return The bean.
	 */
	@Override
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked cast
	})
	public <T> Optional<T> getBean(Class<T> beanType, String name) {
		try (var x = lock.read()) {
			// (1) Overriding parent.
			if (overridingParent.isPresent()) {
				var fromOverriding = overridingParent.get().getBean(beanType, name);
				if (fromOverriding.isPresent())
					return fromOverriding;
			}
			// (2) Local entries.
			var e = (Entry<T>)entries.stream().filter(x2 -> x2.matches(beanType, name)).findFirst().orElse(null);
			if (nn(e))
				return opt(e.get());
			// (3) Regular parent fallback.
			if (parent.isPresent()) {
				var fromParent = parent.get().getBean(beanType, name);
				if (fromParent.isPresent())
					return fromParent;
			}
			// (4) Local default suppliers.
			var d = (Entry<T>)defaults.stream().filter(x2 -> x2.matches(beanType, name)).findFirst().orElse(null);
			if (nn(d))
				return opt(d.get());
			return opte();
		}
	}

	/**
	 * Returns all beans of the specified type, keyed by bean name.
	 *
	 * <p>
	 * Higher-priority sources overwrite lower-priority ones with the same name. The priority order
	 * mirrors {@link #getBean(Class)}: defaults &lt; parent &lt; local entries &lt; overriding parent.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @return A map of bean names to bean instances. Never <jk>null</jk>.
	 */
	@Override
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked cast
	})
	public <T> Map<String,T> getBeansOfType(Class<T> beanType) {
		try (var x = lock.read()) {
			Map<String,T> result = map();
			defaults.stream().filter(e -> e.matches(beanType)).forEach(e -> result.put(emptyIfNull(e.getName()), (T)e.get()));
			parent.ifPresent(p -> p.getBeansOfType(beanType).forEach(result::put));
			entries.stream().filter(e -> e.matches(beanType)).forEach(e -> result.put(emptyIfNull(e.getName()), (T)e.get()));
			overridingParent.ifPresent(op -> op.getBeansOfType(beanType).forEach(result::put));
			return result;
		}
	}

	/**
	 * Returns the supplier for an unnamed bean of the specified type.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @return The supplier, or {@link Optional#empty()} if no supplier of the specified type exists.
	 */
	@Override
	public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType) {
		return getBeanSupplier(beanType, null);
	}

	/**
	 * Returns the supplier for a named bean of the specified type.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return The supplier, or {@link Optional#empty()} if no supplier of the specified type and name exists.
	 */
	@Override
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked cast for supplier resolution
	})
	public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name) {
		try (var x = lock.read()) {
			if (overridingParent.isPresent()) {
				var fromOverriding = overridingParent.get().getBeanSupplier(beanType, name);
				if (fromOverriding.isPresent())
					return fromOverriding;
			}
			var e = entries.stream().filter(x2 -> x2.matches(beanType, name)).findFirst().orElse(null);
			if (nn(e))
				return opt((Supplier<T>) e.bean);
			if (parent.isPresent()) {
				var fromParent = parent.get().getBeanSupplier(beanType, name);
				if (fromParent.isPresent())
					return fromParent;
			}
			var d = defaults.stream().filter(x2 -> x2.matches(beanType, name)).findFirst().orElse(null);
			if (nn(d))
				return opt((Supplier<T>) d.bean);
			return opte();
		}
	}

	/**
	 * Finds and invokes a factory method that produces a bean of type <c>beanType</c>.
	 *
	 * <p>
	 * If <c>onClassOrObject</c> is a {@link Class}, only static methods are eligible.
	 * Otherwise, both instance and static methods on the object's class are eligible.
	 * A <jk>null</jk> <c>filter</c> accepts any qualifying method.
	 *
	 * @param <T> The bean type.
	 * @param beanType The type of bean to create.  Must not be <jk>null</jk>.
	 * @param onClassOrObject The object instance or {@link Class} whose public methods are searched.
	 * 	Must not be <jk>null</jk>.
	 * @param filter Optional predicate restricting which methods are eligible.  Can be <jk>null</jk>.
	 * @param extraBeans Optional bean instances visible to parameter resolution for this call only.
	 * @return The created bean wrapped in an {@link Optional}, or {@link Optional#empty()} if no matching
	 * 	factory method was found.
	 * @throws BeanCreationException If a matching method was found but threw an exception during invocation.
	 */
	@Override
	public <T> Optional<T> createBeanFromMethod(Class<T> beanType, Object onClassOrObject, Predicate<MethodInfo> filter, Object... extraBeans) {
		Object resource = onClassOrObject instanceof Class ? null : onClassOrObject;
		Class<?> resourceClass = onClassOrObject instanceof Class<?> c ? c : onClassOrObject.getClass();
		return info(resourceClass)
			.getPublicMethod(m ->
				m.isNotDeprecated()
				&& m.hasReturnType(beanType)
				&& (filter == null || filter.test(m))
				&& (m.isStatic() || nn(resource))
				&& m.canResolveAllParameters(this, extraBeans))
			.map(m -> {
				try {
					return m.<T>inject(this, resource, extraBeans);
				} catch (Exception e) {
					throw new BeanCreationException("Failed to create bean of type [" + beanType.getSimpleName() + "] via method [" + m.getName() + "]", e);
				}
			});
	}

	/**
	 * Given an executable, returns a list of types that are missing from this factory.
	 *
	 * @param executable The constructor or method to get the params for.
	 * @param outer The outer object to use when instantiating inner classes.  Can be <jk>null</jk>.
	 * @return A comma-delimited list of types that are missing from this factory, or <jk>null</jk> if none are missing.
	 */
	public String getMissingParams(ExecutableInfo executable, Object outer) {
		var params = executable.getParameters();
		List<String> l = list();
		for (int i = 0; i < params.size(); i++) {
			var pi = params.get(i);
			var pt = pi.getParameterType();
			// Skip first parameter if it matches outer instance, or skip Optional parameters
			if ((i == 0 && nn(outer) && pt.isInstance(outer)) || pt.is(Optional.class))
				continue;
			var beanName = pi.getResolvedQualifier();  // Use @Named for bean injection
			var ptc = pt.inner();
			if (beanName == null && ! hasBean(ptc))
				l.add(pt.getNameSimple());
			if (nn(beanName) && ! hasBean(ptc, beanName))
				l.add(pt.getNameSimple() + '@' + beanName);
		}
		return l.isEmpty() ? null : l.stream().sorted().collect(joining(","));
	}

	/**
	 * Returns the corresponding beans in this factory for the specified param types.
	 *
	 * @param executable The constructor or method to get the params for.
	 * @param outer The outer object to use when instantiating inner classes.  Can be <jk>null</jk>.
	 * @return The corresponding beans in this factory for the specified param types.
	 */
	public Object[] getParams(ExecutableInfo executable, Object outer) {
		var o = new Object[executable.getParameterCount()];
		for (var i = 0; i < executable.getParameterCount(); i++) {
			var pi = executable.getParameter(i);
			var pt = pi.getParameterType();
			if (i == 0 && nn(outer) && pt.isInstance(outer)) {
				o[i] = outer;
			} else {
				var beanQualifier = pi.getResolvedQualifier();
				var ptc = pt.unwrap(Optional.class).inner();
				var o2 = beanQualifier == null ? getBean(ptc) : getBean(ptc, beanQualifier);
				o[i] = pt.is(Optional.class) ? o2 : o2.orElse(null);
			}
		}
		return o;
	}

	/**
	 * Given the list of param types, returns <jk>true</jk> if this factory has all the parameters for the specified executable.
	 *
	 * @param executable The constructor or method to get the params for.
	 * @param outer The outer object to use when instantiating inner classes.  Can be <jk>null</jk>.
	 * @return A comma-delimited list of types that are missing from this factory.
	 */
	public boolean hasAllParams(ExecutableInfo executable, Object outer) {
		for (int i = 0; i < executable.getParameterCount(); i++) {
			var pi = executable.getParameter(i);
			var pt = pi.getParameterType();
			// Skip first parameter if it matches outer instance, or skip Optional parameters
			if ((i == 0 && nn(outer) && pt.isInstance(outer)) || pt.is(Optional.class))
				continue;
			var beanQualifier = pi.getResolvedQualifier();
			var ptc = pt.inner();
			if ((beanQualifier == null && ! hasBean(ptc)) || (nn(beanQualifier) && ! hasBean(ptc, beanQualifier)))
				return false;
		}
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this store contains the specified unnamed bean type.
	 *
	 * @param beanType The bean type to check.
	 * @return <jk>true</jk> if this store contains the specified unnamed bean type.
	 */
	@Override
	public boolean hasBean(Class<?> beanType) {
		return overridingParent.map(x -> x.hasBean(beanType)).orElse(false)
			|| unnamedEntries.containsKey(beanType)
			|| parent.map(x -> x.hasBean(beanType)).orElse(false)
			|| unnamedDefaults.containsKey(beanType);
	}

	/**
	 * Returns <jk>true</jk> if this store contains the specified named bean type.
	 *
	 * @param beanType The bean type to check.
	 * @param name The bean name.
	 * @return <jk>true</jk> if this store contains the specified named bean type.
	 */
	@Override
	public boolean hasBean(Class<?> beanType, String name) {
		return overridingParent.map(x -> x.hasBean(beanType, name)).orElse(false)
			|| entries.stream().anyMatch(x -> x.matches(beanType, name))
			|| parent.map(x -> x.hasBean(beanType, name)).orElse(false)
			|| defaults.stream().anyMatch(x -> x.matches(beanType, name));
	}

	/**
	 * Returns <jk>true</jk> if this store has a {@linkplain #addDefaultSupplier(Class,Supplier) default supplier}
	 * registered locally for the specified unnamed bean type.
	 *
	 * <p>
	 * This is intended for callers that need to distinguish "framework default present" from "any binding
	 * exists" — for example, the {@code RestContext} {@code @RestInject} eager walk uses this signal to
	 * skip types that are managed by an internal memoizer (replacing the legacy {@code DELAYED_INJECTION}
	 * skip-list).  Parent and overriding-parent stores are <i>not</i> consulted.
	 *
	 * @param beanType The bean type to check.
	 * @return <jk>true</jk> if a default supplier for the unnamed bean type is registered on this store.
	 */
	@Override
	public boolean hasDefaultSupplier(Class<?> beanType) {
		return unnamedDefaults.containsKey(beanType);
	}

	/**
	 * Returns <jk>true</jk> if this store has a {@linkplain #addDefaultSupplier(Class,Supplier,String) default supplier}
	 * registered locally for the specified bean type and name.
	 *
	 * <p>
	 * See {@link #hasDefaultSupplier(Class)} for the rationale.  Parent and overriding-parent stores are
	 * <i>not</i> consulted.
	 *
	 * @param beanType The bean type to check.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return <jk>true</jk> if a default supplier for the bean type and name is registered on this store.
	 */
	@Override
	public boolean hasDefaultSupplier(Class<?> beanType, String name) {
		return defaults.stream().anyMatch(x -> x.matches(beanType, name));
	}

	protected FluentMap<String,Object> properties() {
		// @formatter:off
		return filteredBeanPropertyMap()
			.a(PROP_entries, entries.stream().map(Entry::properties).toList())
			.a(PROP_identity, id(this))
			.a(PROP_parent, parent.map(BasicBeanStore::properties).orElse(null))
			.ai(readOnly, PROP_readOnly, readOnly)
			.ai(threadSafe, PROP_threadSafe, threadSafe);
		// @formatter:on
	}

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}

	private void assertCanWrite() {
		if (readOnly)
			throw new IllegalStateException("Method cannot be used because BasicBeanStore is read-only.");
	}

	/**
	 * Creates an entry in this store for the specified bean.
	 *
	 * <p>
	 * Subclasses can override this method to create their own entry subtypes.
	 *
	 * @param <T> The class type to associate with the bean.
	 * @param type The class type to associate with the bean.
	 * @param bean The bean supplier.
	 * @param name Optional name to associate with the bean.  Can be <jk>null</jk>.
	 * @return A new bean store entry.
	 */
	protected <T> Entry<T> createEntry(Class<T> type, Supplier<T> bean, String name) {
		return Entry.create(type, bean, name);
	}

	/**
	 * Represents a bean in a {@link BasicBeanStore}.
	 *
	 * <p>
	 * A bean entry consists of the following:
	 * <ul>
	 * 	<li>A class type.
	 * 	<li>A bean or bean supplier that returns an instance of the class type.  This can be a subclass of the type.
	 * 	<li>An optional name.
	 * </ul>
	 *
	 * @param <T> The bean type.
	 */
	public static class Entry<T> {

		// Property name constants
		private static final String PROP_bean = "bean";
		private static final String PROP_name = "name";
		private static final String PROP_type = "type";

		/**
		 * Static creator.
		 *
		 * @param <T> The class type to associate with the bean.
		 * @param type The class type to associate with the bean.
		 * @param bean The bean supplier.
		 * @param name Optional name to associate with the bean.  Can be <jk>null</jk>.
		 * @return A new bean store entry.
		 */
		public static <T> Entry<T> create(Class<T> type, Supplier<T> bean, String name) {
			return new Entry<>(type, bean, name);
		}

		final Supplier<T> bean;
		final Class<T> type;
		final String name;

		/**
		 * Constructor.
		 *
		 * @param type The class type to associate with the bean.
		 * @param bean The bean supplier.
		 * @param name Optional name to associate with the bean.  Can be <jk>null</jk>.
		 */
		protected Entry(Class<T> type, Supplier<T> bean, String name) {
			this.bean = assertArgNotNull(ARG_bean, bean);
			this.type = assertArgNotNull(ARG_type, type);
			this.name = nullIfEmpty(name);
		}

		/**
		 * Returns the bean associated with this entry.
		 *
		 * @return The bean associated with this entry.
		 */
		public T get() {
			return bean.get();
		}

		/**
		 * Returns the name associated with this entry.
		 *
		 * @return the name associated with this entry.  <jk>null</jk> if no name is associated.
		 */
		public String getName() { return name; }

		/**
		 * Returns the type this bean is associated with.
		 *
		 * @return The type this bean is associated with.
		 */
		public Class<T> getType() { return type; }

		/**
		 * Returns <jk>true</jk> if this bean is exactly of the specified type.
		 *
		 * @param type The class to check.  Returns <jk>false</jk> if <jk>null</jk>.
		 * @return <jk>true</jk> if this bean is exactly of the specified type.
		 */
		public boolean matches(Class<?> type) {
			return this.type.equals(type);
		}

		/**
		 * Returns <jk>true</jk> if this bean is exactly of the specified type and has the specified name.
		 *
		 * @param type The class to check.  Returns <jk>false</jk> if <jk>null</jk>.
		 * @param name The name to check.  Can be <jk>null</jk> to only match if name of entry is <jk>null</jk>.
		 * @return <jk>true</jk> if this bean is exactly of the specified type and has the specified name.
		 */
		public boolean matches(Class<?> type, String name) {
			name = nullIfEmpty(name);
			return matches(type) && eq(this.name, name);
		}

		/**
		 * Returns the properties in this object as a simple map for debugging purposes.
		 *
		 * @return The properties in this object as a simple map.
		 */
		protected FluentMap<String,Object> properties() {
			// @formatter:off
			return filteredBeanPropertyMap()
				.a(PROP_type, cns(getType()))
				.a(PROP_bean, id(get()))
				.a(PROP_name, getName());
			// @formatter:on
		}
	}
}