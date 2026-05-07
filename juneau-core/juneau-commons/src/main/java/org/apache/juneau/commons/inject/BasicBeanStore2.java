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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.commons.collections.*;

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
 * 	<li>{@linkplain #BasicBeanStore2(BeanStore,BeanStore) Overriding parent} (e.g. a Spring application context bridge), if non-{@code null}.
 * 	<li>Local entries added via {@link #addBean(Class,Object) addBean} / {@link #addSupplier(Class,Supplier) addSupplier}.
 * 	<li>Regular {@linkplain #BasicBeanStore2(BeanStore) parent}, if non-{@code null}.
 * 	<li>Local default suppliers added via {@link #addDefaultSupplier(Class,Supplier) addDefaultSupplier} (memoizer-backed framework defaults).
 * </ol>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanStore} - Read-only bean lookup interface
 * 	<li class='jc'>{@link WritableBeanStore} - Writable bean store interface
 * </ul>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., PROP_bean)
})
public class BasicBeanStore2 implements WritableBeanStore {

	// Property name constants
	private static final String PROP_bean = "bean";
	private static final String PROP_defaults = "defaults";
	private static final String PROP_entries = "entries";
	private static final String PROP_identity = "identity";
	private static final String PROP_name = "name";
	private static final String PROP_overridingParent = "overridingParent";
	private static final String PROP_parent = "parent";
	private static final String PROP_type = "type";

	private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Supplier<?>>> entries;
	private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Supplier<?>>> defaults;
	private final BeanStore parent;
	private final BeanStore overridingParent;

	/**
	 * Constructor.
	 *
	 * @param parent The parent bean store.  Can be <jk>null</jk>.  Bean searches are performed recursively up this parent chain
	 * 	<i>after</i> local entries are checked.
	 */
	public BasicBeanStore2(BeanStore parent) {
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
	public BasicBeanStore2(BeanStore parent, BeanStore overridingParent) {
		this.parent = parent;
		this.overridingParent = overridingParent;
		entries = new ConcurrentHashMap<>();
		defaults = new ConcurrentHashMap<>();
		addSupplier(BasicBeanStore2.class, ()->this, null);
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
	 * Adds an unnamed bean of the specified type to this store.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore2 addBean(Class<T> beanType, T bean) {
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
	public <T> BasicBeanStore2 addBean(Class<T> beanType, T bean, String name) {
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
	public <T> BasicBeanStore2 addSupplier(Class<T> beanType, Supplier<T> bean) {
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
	public <T> BasicBeanStore2 addSupplier(Class<T> beanType, Supplier<T> bean, String name) {
		var typeMap = entries.computeIfAbsent(beanType, k -> new ConcurrentHashMap<>());
		var key = emptyIfNull(name);
		typeMap.put(key, bean);
		return this;
	}

	/**
	 * Adds a fallback supplier for an unnamed bean of the specified type to this store.
	 *
	 * <p>
	 * Default suppliers are consulted only after local {@linkplain #addBean(Class,Object) entries} and the regular
	 * {@linkplain #BasicBeanStore2(BeanStore) parent} have been searched.  They are intended for memoizer-backed
	 * framework defaults that should not shadow explicit user registrations or beans inherited from an
	 * {@linkplain #BasicBeanStore2(BeanStore,BeanStore) overriding parent} (e.g. Spring).
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param supplier The bean supplier.
	 * @return This object.
	 */
	@Override
	public <T> BasicBeanStore2 addDefaultSupplier(Class<T> beanType, Supplier<T> supplier) {
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
	public <T> BasicBeanStore2 addDefaultSupplier(Class<T> beanType, Supplier<T> supplier, String name) {
		var typeMap = defaults.computeIfAbsent(beanType, k -> new ConcurrentHashMap<>());
		var key = emptyIfNull(name);
		typeMap.put(key, supplier);
		return this;
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
	public BasicBeanStore2 clear() {
		entries.clear();
		defaults.clear();
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
		return resolve(beanType, name).map(Supplier::get);
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
		// Build the result respecting the priority order used by getBean / resolve:
		//   defaults (lowest) < parent < entries (local) < overridingParent (highest)
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
		return result;
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
				return opt((Supplier<T>)supplier);
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
				return opt((Supplier<T>)supplier);
		}
		return opte();
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
		return resolve(beanType, name);
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
			if (overridingParent instanceof BasicBeanStore2 op2)
				overridingParentValue = op2.properties();
			else
				overridingParentValue = s(overridingParent);
		}
		return filteredBeanPropertyMap()
			.a(PROP_entries, entryList)
			.a(PROP_defaults, defaultList.isEmpty() ? null : defaultList)
			.a(PROP_overridingParent, overridingParentValue)
			.a(PROP_identity, id(this))
			.a(PROP_parent, parent instanceof BasicBeanStore2 parent2 ? parent2.properties() : s(parent));
		// @formatter:on
	}
}