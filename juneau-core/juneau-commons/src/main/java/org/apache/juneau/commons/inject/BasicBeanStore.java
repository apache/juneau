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
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.settings.*;

/**
 * Basic implementation of {@link WritableBeanStore}.
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
 * This implementation is thread-safe and supports parent bean stores for hierarchical bean resolution.
 *
 * <h5 class='section'>Resolution order:</h5>
 * <p>
 * Bean lookups consult the following sources in this order, returning the first match:
 * <ol>
 * 	<li>{@linkplain #BasicBeanStore(BeanStore,BeanStore) Overriding parent} (e.g. a Spring application context bridge), if non-{@code null}.
 * 	<li>Local entries added via {@link #addBean(Class,Object) addBean} / {@link #addSupplier(Class,Supplier) addSupplier}.
 * 	<li>Regular {@linkplain #BasicBeanStore(BeanStore) parent}, if non-{@code null}.
 * 	<li>Local default suppliers added via {@link #addDefaultSupplier(Class,Supplier) addDefaultSupplier} (memoizer-backed framework defaults).
 * </ol>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanStore} - Read-only bean lookup interface
 * 	<li class='jc'>{@link WritableBeanStore} - Writable bean store interface
 * </ul>
 */
@SuppressWarnings({
	"java:S115",  // Constants use UPPER_snakeCase convention (e.g., PROP_bean)
	"java:S135",  // @PreDestroy/@Bean discovery loops use per-element continue guards for clarity; refactoring obscures the per-annotation filter chain
	"java:S3011", // setAccessible(true) is required to invoke private/package-private @Bean members and @PreDestroy methods via reflection
	"java:S3776", // matchesConditions and registerConfiguration intentionally centralize the @Conditional/@Bean discovery state machine; splitting hurts cohesion
	"resource" // BeanStore is a fluent AutoCloseable; self-returns and owned/sentinel stores are not new resources to close.
})
public class BasicBeanStore implements WritableBeanStore {
	private static final Logger LOGGER = Logger.getLogger(BasicBeanStore.class.getName());

	/**
	 * Static reusable empty instance.
	 *
	 * <p>
	 * Useful as a placeholder when an API requires a {@link BeanStore} but the caller has no beans to
	 * register &mdash; e.g. as the parent argument to a builder that only needs the bean store for
	 * downstream parameter resolution.
	 *
	 * <p>
	 * <b>Treat this instance as read-only.</b>  It's typed as the concrete class so it can be passed
	 * to APIs that take {@link WritableBeanStore} or {@code BasicBeanStore}, but callers should not
	 * call any mutating methods on it &mdash; doing so would leak state between unrelated callers.
	 * Code that legitimately needs to add beans should construct its own {@code new BasicBeanStore()}.
	 */
	public static final BasicBeanStore INSTANCE = new BasicBeanStore();

	// Property name constants
	private static final String PROP_bean = "bean";
	private static final String PROP_defaults = "defaults";
	private static final String PROP_entries = "entries";
	private static final String PROP_identity = "identity";
	private static final String PROP_name = "name";
	private static final String PROP_overlayStack = "overlayStack";
	private static final String PROP_overridingParent = "overridingParent";
	private static final String PROP_parent = "parent";
	private static final String PROP_type = "type";

	// Argument name constants for assertArgNotNull
	private static final String ARG_beanType = "beanType";
	private static final String ARG_onClassOrObject = "onClassOrObject";

	private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Supplier<?>>> entries;
	private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Supplier<?>>> defaults;
	private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, BeanSourceMeta>> entryMetadata;
	private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, BeanSourceMeta>> defaultMetadata;
	private final ConcurrentHashMap<Class<?>, Class<?>> typeBindings;
	private final List<Object> resolvedBeans;
	private final Set<Object> resolvedIdentities;
	private final Set<Class<?>> registeredConfigurations;
	private final BeanStore parent;
	private final BeanStore overridingParent;
	@SuppressWarnings({
		"java:S3077" // volatile is required here for correct double-checked-locking safe-publication of the lazily-created StackOverlay in pushOverlay(); the reference is publish-once and never compound-mutated.
	})
	private volatile StackOverlay overlayStack;
	private volatile boolean closed;

	/**
	 * No-arg constructor.  Equivalent to {@code new BasicBeanStore(null)}.
	 *
	 * <p>
	 * Creates a standalone, parent-less bean store.  All lookups consult only locally-registered beans and
	 * default suppliers.
	 */
	public BasicBeanStore() {
		this(null, null);
	}

	/**
	 * Constructor.
	 *
	 * @param parent The parent bean store.  Can be <jk>null</jk>.  Bean searches are performed recursively up this parent chain
	 * 	<i>after</i> local entries are checked.
	 */
	public BasicBeanStore(BeanStore parent) {
		this(parent, null);
	}

	/**
	 * Constructor that accepts an overriding parent bean store.
	 *
	 * <p>
	 * The {@code overridingParent} is consulted <i>before</i> any local entries during bean lookup, allowing an outer
	 * scope (typically a Spring {@code ApplicationContext} bridge) to take precedence over local registrations.  The
	 * regular {@code parent} continues to be consulted as a fallback after local entries.
	 *
	 * <p>
	 * Final resolution order:
	 * <ol>
	 * 	<li>{@code overridingParent} (if non-{@code null})
	 * 	<li>local entries (added via {@link #addBean(Class,Object) addBean} / {@link #addSupplier(Class,Supplier) addSupplier})
	 * 	<li>regular {@code parent} (if non-{@code null})
	 * 	<li>local default suppliers (added via {@link #addDefaultSupplier(Class,Supplier) addDefaultSupplier})
	 * </ol>
	 *
	 * @param parent The parent bean store, used as a fallback after local entries.  Can be <jk>null</jk>.
	 * @param overridingParent The overriding parent bean store, consulted before local entries.  Can be <jk>null</jk>.
	 */
	public BasicBeanStore(BeanStore parent, BeanStore overridingParent) {
		this.parent = parent;
		this.overridingParent = overridingParent;
		entries = new ConcurrentHashMap<>();
		defaults = new ConcurrentHashMap<>();
		entryMetadata = new ConcurrentHashMap<>();
		defaultMetadata = new ConcurrentHashMap<>();
		typeBindings = new ConcurrentHashMap<>();
		resolvedBeans = Collections.synchronizedList(new ArrayList<>());
		resolvedIdentities = Collections.newSetFromMap(new IdentityHashMap<>());
		registeredConfigurations = Collections.synchronizedSet(new HashSet<>());
		addSupplier(BasicBeanStore.class, ()->this, null);
		addSupplier(BeanStore.class, ()->this, null);
		addSupplier(WritableBeanStore.class, ()->this, null);
	}

	/**
	 * Same as {@link #addBean(Class,Object)} but returns the bean instead of this object for fluent calls.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean.  Can be <jk>null</jk>.
	 * @return The bean.
	 */
	@Override
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
	@Override
	public <T> T add(Class<T> beanType, T bean, String name) {
		addBean(beanType, bean, name);
		return bean;
	}

	/**
	 * Adds an unnamed bean of the specified type to this store.
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
	 * Adds a named bean of the specified type to this store.
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
	 * Adds a supplier for an unnamed bean of the specified type to this store.
	 *
	 * <p>
	 * The supplier will be invoked lazily when the bean is first requested.
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
	 * Adds a supplier for a named bean of the specified type to this store.
	 *
	 * <p>
	 * The supplier will be invoked lazily when the bean is first requested.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean supplier.
	 * @param name The bean name if this is a named bean.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore addSupplier(Class<T> beanType, Supplier<T> bean, String name) {
		checkOpen();
		var typeMap = entries.computeIfAbsent(beanType, k -> new ConcurrentHashMap<>());
		var key = emptyIfNull(name);
		typeMap.put(key, bean);
		entryMetadata.computeIfAbsent(beanType, k -> new ConcurrentHashMap<>()).put(key, BeanSourceMeta.DEFAULT);
		return this;
	}

	/**
	 * Adds a fallback supplier for an unnamed bean of the specified type to this store.
	 *
	 * <p>
	 * Default suppliers are consulted only after local {@linkplain #addBean(Class,Object) entries} and the regular
	 * {@linkplain #BasicBeanStore(BeanStore) parent} have been searched.  They are intended for memoizer-backed
	 * framework defaults that should not shadow explicit user registrations or beans inherited from an
	 * {@linkplain #BasicBeanStore(BeanStore,BeanStore) overriding parent} (e.g. Spring).
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param supplier The bean supplier.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore addDefaultSupplier(Class<T> beanType, Supplier<T> supplier) {
		return addDefaultSupplier(beanType, supplier, null);
	}

	/**
	 * Adds a fallback supplier for a named bean of the specified type to this store.
	 *
	 * <p>
	 * See {@link #addDefaultSupplier(Class,Supplier)} for ordering semantics.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param supplier The bean supplier.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore addDefaultSupplier(Class<T> beanType, Supplier<T> supplier, String name) {
		checkOpen();
		var typeMap = defaults.computeIfAbsent(beanType, k -> new ConcurrentHashMap<>());
		var key = emptyIfNull(name);
		typeMap.put(key, supplier);
		defaultMetadata.computeIfAbsent(beanType, k -> new ConcurrentHashMap<>()).put(key, BeanSourceMeta.DEFAULT);
		return this;
	}

	/**
	 * Promotes every locally-registered default supplier to a local entry supplier of the same name and type.
	 *
	 * <p>
	 * Default suppliers normally resolve at tier 4 (after the regular parent walk), which means a parent
	 * store's default supplier wins over this store's default supplier when both are registered for the same
	 * type.  Promoting moves each default into the tier-2 local-entry slot so that this store's framework
	 * defaults take precedence over the parent's same-type defaults &mdash; useful for embedded sub-contexts
	 * (e.g. per-mixin {@code RestContext}s) that need their own framework objects (serializer set, parser set,
	 * call logger, etc.) even though the parent is parent-linked for non-framework bean lookups.
	 *
	 * <p>
	 * The original default-supplier registrations are left in place; promoted entries that already exist as
	 * local entries are not overwritten.  Promotion is idempotent and only affects suppliers registered prior
	 * to the call &mdash; later {@code addDefaultSupplier(...)} calls do not retroactively promote.
	 *
	 * @return This object.
	 * @since 10.0.0
	 */
	public BasicBeanStore promoteDefaultsToLocalSuppliers() {
		checkOpen();
		defaults.forEach((beanType, typeMap) -> typeMap.forEach((name, supplier) -> {
			var localTypeMap = entries.computeIfAbsent(beanType, k -> new ConcurrentHashMap<>());
			localTypeMap.putIfAbsent(name, supplier);
			entryMetadata.computeIfAbsent(beanType, k -> new ConcurrentHashMap<>()).putIfAbsent(name, BeanSourceMeta.DEFAULT);
		}));
		return this;
	}

	@Override
	public Snapshot pushOverlay(BeanStore overlay) {
		Objects.requireNonNull(overlay, "overlay must not be null");
		checkOpen();
		var stack = overlayStack;
		if (stack == null) {
			synchronized (this) {
				stack = overlayStack;
				if (stack == null) {
					stack = new StackOverlay();
					overlayStack = stack;
				}
			}
		}
		stack.push(overlay);
		return new Snapshot(this, overlay);
	}

	@Override
	public void popOverlay(Snapshot snapshot) {
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		checkOpen();
		if (snapshot.owner() != this)
			throw new IllegalStateException("Snapshot does not belong to this BeanStore (foreign-snapshot pop).");
		var stack = overlayStack;
		if (stack == null || stack.depth() == 0)
			throw new IllegalStateException("popOverlay invoked on an empty overlay stack.");
		// LIFO discipline — the snapshot must identify the current top-of-stack frame.
		var top = stack.peek();
		if (top != snapshot.frame())
			throw new IllegalStateException(
				"Out-of-order popOverlay: supplied snapshot does not identify the top-of-stack overlay frame.");
		stack.pop();
	}

	/**
	 * Removes all beans from this store.
	 *
	 * <p>
	 * This operation only affects this store and does not affect the parent bean store.
	 *
	 * @return This object.
	 */
	@Override
	public BasicBeanStore clear() {
		checkOpen();
		entries.clear();
		defaults.clear();
		entryMetadata.clear();
		defaultMetadata.clear();
		registeredConfigurations.clear();
		return this;
	}

	/**
	 * Returns the unnamed bean of the specified type.
	 *
	 * <p>
	 * If no unnamed bean is found in this store, searches the parent store recursively.
	 *
	 * @param <T> The type of bean to return.
	 * @param beanType The type of bean to return.
	 * @return The bean, or {@link Optional#empty()} if not found.
	 */
	@Override
	public <T> Optional<T> getBean(Class<T> beanType) {
		return getBean(beanType, null);
	}


	/**
	 * Returns the named bean of the specified type.
	 *
	 * <p>
	 * If no bean with the specified name is found in this store, searches the parent store recursively.
	 *
	 * @param <T> The type of bean to return.
	 * @param beanType The type of bean to return.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return The bean, or {@link Optional#empty()} if not found.
	 */
	@Override
	public <T> Optional<T> getBean(Class<T> beanType, String name) {
		checkOpen();
		if (name == null) {
			var unnamed = resolve(beanType, null).map(Supplier::get);
			if (unnamed.isPresent())
				return unnamed.map(this::trackResolved);
			return selectPrimary(beanType).map(this::trackResolved);
		}
		return resolve(beanType, name).map(Supplier::get).map(this::trackResolved);
	}

	/**
	 * Returns all beans of the specified type, keyed by bean name.
	 *
	 * <p>
	 * The map keys are the bean names (empty string for unnamed beans), and the values are the bean instances.
	 * Results from the parent store are included first, then beans from this store (which override parent beans with the same name).
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @return A map of bean names to bean instances.  Never <jk>null</jk>.
	 */
	@Override
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast to Map<String,T>
	})
	public <T> Map<String,T> getBeansOfType(Class<T> beanType) {
		checkOpen();
		// Build the result respecting the priority order used by getBean / resolve:
		//   defaults (lowest) < parent < entries (local) < overridingParent < overlayStack (highest)
		// Higher-priority maps overwrite lower-priority ones with the same name.
		Map<String,T> result = map();
		var defaultMap = defaults.get(beanType);
		if (nn(defaultMap))
			defaultMap.forEach((name, supplier) -> result.put(name, (T)supplier.get()));
		if (nn(parent))
			parent.getBeansOfType(beanType).forEach(result::put);
		var typeMap = entries.get(beanType);
		if (nn(typeMap))
			typeMap.forEach((name, supplier) -> result.put(name, (T)supplier.get()));
		if (nn(overridingParent))
			overridingParent.getBeansOfType(beanType).forEach(result::put);
		var stack = overlayStack;
		if (nn(stack) && stack.depth() > 0)
			stack.getBeansOfType(beanType).forEach(result::put);
		if (result.isEmpty())
			return result;
		return result.entrySet().stream()
			.sorted(Comparator.comparingInt(e -> beanOrder(beanType, e.getKey())))
			.collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), trackResolved(e.getValue())), Map::putAll);
	}

	/**
	 * Returns <jk>true</jk> if this store has a default supplier registered locally for the specified unnamed bean type.
	 *
	 * <p>
	 * Parent and overriding-parent stores are <i>not</i> consulted.
	 *
	 * @param beanType The bean type to check.
	 * @return <jk>true</jk> if a default supplier for the unnamed bean type is registered on this store.
	 */
	@Override
	public boolean hasDefaultSupplier(Class<?> beanType) {
		return hasDefaultSupplier(beanType, null);
	}

	/**
	 * Returns <jk>true</jk> if this store has a default supplier registered locally for the specified bean type and name.
	 *
	 * <p>
	 * Parent and overriding-parent stores are <i>not</i> consulted.
	 *
	 * @param beanType The bean type to check.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return <jk>true</jk> if a default supplier for the bean type and name is registered on this store.
	 */
	@Override
	public boolean hasDefaultSupplier(Class<?> beanType, String name) {
		var typeMap = defaults.get(beanType);
		return nn(typeMap) && typeMap.containsKey(emptyIfNull(name));
	}

	/**
	 * Returns the default supplier registered locally for the specified unnamed bean type, or empty if none.
	 *
	 * <p>
	 * Parent and overriding-parent stores are <i>not</i> consulted.  This returns the supplier itself,
	 * unwrapped from any resolution chain &mdash; it lets callers promote a memoizer-backed default
	 * supplier into a higher-precedence layer (e.g. a local entry) without re-invoking the underlying
	 * factory.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to look up.
	 * @return The locally-registered default supplier, or {@link Optional#empty()} if not present.
	 */
	public <T> Optional<Supplier<T>> getDefaultSupplier(Class<T> beanType) {
		return getDefaultSupplier(beanType, null);
	}

	/**
	 * Returns the default supplier registered locally for the specified bean type and name, or empty if none.
	 *
	 * <p>
	 * Parent and overriding-parent stores are <i>not</i> consulted.  This returns the supplier itself,
	 * unwrapped from any resolution chain &mdash; it lets callers promote a memoizer-backed default
	 * supplier into a higher-precedence layer (e.g. a local entry) without re-invoking the underlying
	 * factory.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to look up.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return The locally-registered default supplier, or {@link Optional#empty()} if not present.
	 */
	@SuppressWarnings({
		"unchecked" // Cast is safe: parameterization is verified at construction.
	})
	public <T> Optional<Supplier<T>> getDefaultSupplier(Class<T> beanType, String name) {
		var typeMap = defaults.get(beanType);
		if (typeMap == null)
			return oe();
		var supplier = typeMap.get(emptyIfNull(name));
		return supplier == null ? oe() : o((Supplier<T>) supplier);
	}

	/**
	 * Returns <jk>true</jk> if this store contains at least one unnamed bean of the specified type.
	 *
	 * <p>
	 * If not found in this store, searches the parent store recursively.
	 *
	 * @param beanType The bean type to check.
	 * @return <jk>true</jk> if this store contains at least one unnamed bean of the specified type.
	 */
	@Override
	public boolean hasBean(Class<?> beanType) {
		return hasBean(beanType, null);
	}

	/**
	 * Returns <jk>true</jk> if this store contains a bean of the specified type and name.
	 *
	 * <p>
	 * If not found in this store, searches the parent store recursively.
	 *
	 * @param beanType The bean type to check.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if this store contains a bean of the specified type and name.
	 */
	@Override
	public boolean hasBean(Class<?> beanType, String name) {
		return resolve(beanType, name).isPresent();
	}

	@Override
	public <T> WritableBeanStore addBeanType(Class<T> beanType, Class<? extends T> implType) {
		checkOpen();
		typeBindings.put(beanType, implType);
		return this;
	}

	@Override
	@SuppressWarnings({
		"unchecked" // Cast is safe: parameterization is verified at construction.
	})
	public <T> Optional<Class<? extends T>> getBeanType(Class<T> beanType) {
		var v = (Class<? extends T>) typeBindings.get(beanType);
		if (nn(v))
			return o(v);
		if (nn(parent))
			return parent.getBeanType(beanType);
		return oe();
	}

	/**
	 * Returns the supplier for an unnamed bean of the specified type.
	 *
	 * <p>
	 * If no supplier is found in this store, searches the parent store recursively.
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
	 * Resolves a supplier for a bean of the specified type and name.
	 *
	 * <p>
	 * If no supplier with the specified name is found in this store, searches the parent store recursively.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return The supplier, or {@link Optional#empty()} if no supplier of the specified type and name exists.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for supplier resolution
	})
	protected <T> Optional<Supplier<T>> resolve(Class<T> beanType, String name) {
		// (0) Push/pop overlay stack — wins over the construction-time overridingParent slot because pushed
		// frames are "more recent".  Lazily initialized; null until the first pushOverlay() call.
		var stack = overlayStack;
		if (nn(stack) && stack.depth() > 0) {
			var fromStack = stack.getBeanSupplier(beanType, name);
			if (fromStack.isPresent())
				return fromStack;
		}
		// (1) Overriding parent (e.g. Spring) — wins over local entries.
		if (nn(overridingParent)) {
			var fromOverriding = overridingParent.getBeanSupplier(beanType, name);
			if (fromOverriding.isPresent())
				return fromOverriding;
		}
		// (2) Local regular entries.
		var typeMap = entries.get(beanType);
		if (nn(typeMap)) {
			var key = emptyIfNull(name);
			var supplier = typeMap.get(key);
			if (nn(supplier))
				return o((Supplier<T>)supplier);
		}
		// (3) Regular parent fallback.
		if (nn(parent)) {
			var fromParent = parent.getBeanSupplier(beanType, name);
			if (fromParent.isPresent())
				return fromParent;
		}
		// (4) Local default suppliers (memoizer-backed framework defaults).
		var defaultMap = defaults.get(beanType);
		if (nn(defaultMap)) {
			var key = emptyIfNull(name);
			var supplier = defaultMap.get(key);
			if (nn(supplier))
				return o((Supplier<T>)supplier);
		}
		return oe();
	}

	/**
	 * Returns the supplier for a named bean of the specified type.
	 *
	 * <p>
	 * If no supplier with the specified name is found in this store, searches the parent store recursively.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return The supplier, or {@link Optional#empty()} if no supplier of the specified type and name exists.
	 */
	@Override
	public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name) {
		checkOpen();
		return resolve(beanType, name);
	}

	@Override
	public WritableBeanStore registerConfiguration(Class<?> configType) {
		checkOpen();
		if (configType == null)
			return this;
		registerConfiguration(configType, registeredConfigurations);
		return this;
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
		assertArgNotNull(ARG_beanType, beanType);
		assertArgNotNull(ARG_onClassOrObject, onClassOrObject);
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

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}

	protected FluentMap<String,Object> properties() {
		// @formatter:off
		var entryList = list();
		entries.forEach((type, typeMap) -> typeMap.forEach((name, supplier) -> entryList.add(filteredBeanPropertyMap()
			.a(PROP_type, cns(type))
			.a(PROP_bean, id(supplier.get()))
			.a(PROP_name, name))));
		var defaultList = list();
		defaults.forEach((type, typeMap) -> typeMap.forEach((name, supplier) -> defaultList.add(filteredBeanPropertyMap()
			.a(PROP_type, cns(type))
			.a(PROP_bean, id(supplier.get()))
			.a(PROP_name, name))));
		Object overridingParentValue = null;
		if (nn(overridingParent)) {
			if (overridingParent instanceof BasicBeanStore op2)
				overridingParentValue = op2.properties();
			else
				overridingParentValue = s(overridingParent);
		}
		var stack = overlayStack;
		Object overlayStackValue = (nn(stack) && stack.depth() > 0) ? s(stack) : null;
		return filteredBeanPropertyMap()
			.a(PROP_entries, entryList)
			.a(PROP_defaults, defaultList.isEmpty() ? null : defaultList)
			.a(PROP_overlayStack, overlayStackValue)
			.a(PROP_overridingParent, overridingParentValue)
			.a(PROP_identity, id(this))
			.a(PROP_parent, parent instanceof BasicBeanStore parent2 ? parent2.properties() : s(parent));
		// @formatter:on
	}

	@Override
	public void close() throws BeanCreationException {
		if (closed)
			return;
		closed = true;
		var errors = new BeanCreationException("Errors while invoking @PreDestroy callbacks.");
		boolean hasErrors = false;
		var beans = new ArrayList<>(resolvedBeans);
		Collections.reverse(beans);
		for (var bean : beans) {
			for (var method : ClassInfo.of(bean).getAllMethods()) {
				if (!method.getReturnType().is(void.class) || method.getParameterCount() != 0 || method.isAbstract() || method.isStatic())
					continue;
				if (method.getAnnotations().stream().noneMatch(JsrSupport::isPreDestroyAnnotation))
					continue;
				try {
					method.invoke(bean);
				} catch (Exception e) {
					hasErrors = true;
					errors.addSuppressed(e);
				}
			}
		}
		if (hasErrors)
			throw errors;
	}

	@SuppressWarnings({
		"unchecked" // reflective @Bean discovery — element types resolve at runtime to the value's actual class
	})
	private void registerConfiguration(Class<?> configType, Set<Class<?>> visited) {
		if (!visited.add(configType))
			return;
		var cfg = configType.getAnnotation(Configuration.class);
		if (cfg == null)
			throw new BeanCreationException("Type is not @Configuration: " + configType.getName());
		if (!matchesConditions(configType))
			return;
		// Annotation arrays cannot contain null elements at runtime — Java rejects null defaults at
		// declaration time and the compiler refuses null literals in annotation values.  No defensive
		// null check is needed here.
		for (var imported : cfg.imports())
			registerConfiguration(imported, visited);

		// Walk the type and its superclasses; collect @Bean members on each level.  This honours
		// the plan's "superclass inheritance of @Bean members" requirement.  Order matters here:
		// process the highest ancestor first so subclass @Bean methods can depend on superclass
		// beans (analogous to Spring's @Configuration inheritance).
		Object instance = null;
		var hierarchy = new ArrayList<Class<?>>();
		for (var c = configType; c != null && c != Object.class; c = c.getSuperclass())
			hierarchy.add(c);
		Collections.reverse(hierarchy);
		for (var c : hierarchy) {
			for (var field : c.getDeclaredFields()) {
				var beanAnn = field.getAnnotation(Bean.class);
				if (beanAnn == null)
					continue;
				if (!matchesConditions(field))
					continue;
				try {
					if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && instance == null)
						instance = BeanInstantiator.of(configType, this).run();
					field.setAccessible(true);
					var value = field.get(java.lang.reflect.Modifier.isStatic(field.getModifiers()) ? null : instance);
					addBeanWithMeta((Class<Object>)field.getType(), value, emptyIfNull(beanName(beanAnn)), BeanSourceMeta.from(field, beanAnn));
				} catch (Exception e) {
					throw new BeanCreationException("Failed to register @Bean field: " + field, e);
				}
			}
			for (var method : c.getDeclaredMethods()) {
				var beanAnn = method.getAnnotation(Bean.class);
				if (beanAnn == null)
					continue;
				if (!matchesConditions(method))
					continue;
				try {
					if (!java.lang.reflect.Modifier.isStatic(method.getModifiers()) && instance == null)
						instance = BeanInstantiator.of(configType, this).run();
					method.setAccessible(true);
					var value = MethodInfo.of(method).inject(this, java.lang.reflect.Modifier.isStatic(method.getModifiers()) ? null : instance);
					addBeanWithMeta((Class<Object>)method.getReturnType(), value, emptyIfNull(beanName(beanAnn)), BeanSourceMeta.from(method, beanAnn));
				} catch (Exception e) {
					throw new BeanCreationException("Failed to register @Bean method: " + method, e);
				}
			}
		}
	}

	private boolean matchesConditions(java.lang.reflect.AnnotatedElement element) {
		var classLoader = element instanceof Class<?> c ? c.getClassLoader() : element.getClass().getClassLoader();
		var ctx = new ConditionContext(this, Settings.get(), classLoader, element);
		for (var c : element.getAnnotationsByType(Conditional.class)) {
			if (!BeanInstantiator.of(c.value(), this).run().matches(ctx)) {
				LOGGER.fine(() -> "Skipping conditional element due to @Conditional: " + element);
				return false;
			}
		}
		for (var c : element.getAnnotationsByType(ConditionalOnClass.class)) {
			try {
				Class.forName(c.value(), false, classLoader);
			} catch (@SuppressWarnings("unused") ClassNotFoundException e) {
				LOGGER.fine(() -> "Skipping conditional element due to missing class " + c.value() + ": " + element);
				return false;
			}
		}
		for (var c : element.getAnnotationsByType(ConditionalOnMissingBean.class)) {
			var type = c.value();
			var name = c.name().isEmpty() ? null : c.name();
			if (type == Object.class) {
				if (name != null && anyLocalBeanNamed(name))
					return false;
			} else if (hasBean(type, name)) {
				return false;
			}
		}
		for (var c : element.getAnnotationsByType(ConditionalOnProperty.class)) {
			var value = Settings.get().get(c.name()).orElse(null);
			if (value == null && !c.matchIfMissing())
				return false;
			if (value != null && !c.havingValue().isEmpty() && !c.havingValue().equals(value))
				return false;
		}
		return true;
	}

	private boolean anyLocalBeanNamed(String name) {
		var key = emptyIfNull(name);
		return entries.values().stream().anyMatch(m -> m.containsKey(key))
			|| defaults.values().stream().anyMatch(m -> m.containsKey(key));
	}

	private String beanName(Bean bean) {
		return bean.name().isEmpty() ? bean.value() : bean.name();
	}

	private <T> void addBeanWithMeta(Class<T> beanType, T bean, String name, BeanSourceMeta meta) {
		var key = emptyIfNull(name);
		// Duplicate-check against LOCAL entries only.  Beans present via the overridingParent or parent
		// chain are allowed to coexist with a local registration here — the resolution order
		// (overridingParent > local > parent > defaults) ensures the right entry wins at lookup time.
		// This is what lets a test-time overlay installed in the overridingParent slot coexist with a
		// local @Bean factory of the same (type, name) declared on a @Configuration class without
		// triggering a spurious duplicate-bean error.
		var typeMap = entries.computeIfAbsent(beanType, k -> new ConcurrentHashMap<>());
		if (typeMap.containsKey(key))
			throw new BeanCreationException("Duplicate bean: type=" + beanType.getName() + ", name=" + key);
		typeMap.put(key, () -> bean);
		entryMetadata.computeIfAbsent(beanType, k -> new ConcurrentHashMap<>()).put(key, meta);
	}

	private <T> T trackResolved(T bean) {
		if (bean == null)
			return null;
		synchronized (resolvedIdentities) {
			if (resolvedIdentities.add(bean))
				resolvedBeans.add(bean);
		}
		return bean;
	}

	private <T> Optional<T> selectPrimary(Class<T> beanType) {
		// @Primary is a local-entry-only signal.  Default suppliers added via addDefaultSupplier()
		// always carry BeanSourceMeta.DEFAULT (primary = false), so iterating defaultMetadata never
		// contributes a primary candidate — only entryMetadata is consulted here.  Parent and
		// overridingParent stores (including Spring-backed delegates) are similarly excluded because
		// @Primary metadata only exists on beans this store registered itself.
		Set<String> primaryNames = new LinkedHashSet<>();
		var em = entryMetadata.get(beanType);
		if (nn(em))
			em.forEach((name, meta) -> { if (meta.primary) primaryNames.add(name); });
		if (primaryNames.isEmpty())
			return oe();
		if (primaryNames.size() > 1)
			throw new BeanCreationException("Multiple @Primary candidates of type " + beanType.getName());
		var primaryName = primaryNames.iterator().next();
		return resolve(beanType, primaryName).map(Supplier::get);
	}

	private int beanOrder(Class<?> type, String name) {
		// @Order / priority is sourced from registered entries only.  Default suppliers always carry
		// BeanSourceMeta.DEFAULT (which already returns Integer.MAX_VALUE/2), so we fall through to
		// the same constant rather than consulting defaultMetadata redundantly.
		var key = emptyIfNull(name);
		var m = entryMetadata.get(type);
		if (m != null)
			return m.getOrDefault(key, BeanSourceMeta.DEFAULT).orderValue();
		return Integer.MAX_VALUE / 2;
	}

	private void checkOpen() {
		if (closed)
			throw new IllegalStateException("BeanStore has been closed.");
	}

	private static class BeanSourceMeta {
		static final BeanSourceMeta DEFAULT = new BeanSourceMeta(false, null, Integer.MAX_VALUE / 2);
		final boolean primary;
		final Integer order;
		final int priority;

		BeanSourceMeta(boolean primary, Integer order, int priority) {
			this.primary = primary;
			this.order = order;
			this.priority = priority;
		}

		int orderValue() {
			return order == null ? priority : order.intValue();
		}

		static BeanSourceMeta from(java.lang.reflect.AnnotatedElement element, Bean bean) {
			var order = element.getAnnotation(Order.class);
			return new BeanSourceMeta(element.getAnnotation(Primary.class) != null, order == null ? null : order.value(), bean.priority());
		}
	}
}