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
 * </ul>
 *
 * <p>
 * Beans are retrieved through the following methods:
 * <ul class='javatreec'>
 * 	<li class='jm'>{@link #getBean(Class) getBean(Class)}
 * 	<li class='jm'>{@link #getBean(Class,String) getBean(Class,String)}
 * 	<li class='jm'>{@link #stream(Class) stream(Class)}
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
@SuppressWarnings("java:S115")
public class BasicBeanStore {

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

	final Optional<BasicBeanStore> parent;
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
		readOnly = builder.readOnly;
		threadSafe = builder.threadSafe;
		lock = threadSafe ? new SimpleReadWriteLock() : SimpleReadWriteLock.NO_OP;
		entries = threadSafe ? new ConcurrentLinkedDeque<>() : new LinkedList<>();
		unnamedEntries = threadSafe ? new ConcurrentHashMap<>() : map();
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
	 * Clears out all bean in this bean store.
	 *
	 * <p>
	 * Does not affect the parent bean store.
	 *
	 * @return This object.
	 */
	public BasicBeanStore clear() {
		assertCanWrite();
		try (var x = lock.write()) {
			unnamedEntries.clear();
			entries.clear();
		}
		return this;
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
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getBean(Class<T> beanType) {
		try (var x = lock.read()) {
			var e = (Entry<T>)unnamedEntries.get(beanType);
			if (nn(e))
				return opt(e.get());
			if (parent.isPresent())
				return parent.get().getBean(beanType);
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
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getBean(Class<T> beanType, String name) {
		try (var x = lock.read()) {
			var e = (Entry<T>)entries.stream().filter(x2 -> x2.matches(beanType, name)).findFirst().orElse(null);
			if (nn(e))
				return opt(e.get());
			if (parent.isPresent())
				return parent.get().getBean(beanType, name);
			return opte();
		}
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
	public boolean hasBean(Class<?> beanType) {
		return unnamedEntries.containsKey(beanType) || parent.map(x -> x.hasBean(beanType)).orElse(false);
	}

	/**
	 * Returns <jk>true</jk> if this store contains the specified named bean type.
	 *
	 * @param beanType The bean type to check.
	 * @param name The bean name.
	 * @return <jk>true</jk> if this store contains the specified named bean type.
	 */
	public boolean hasBean(Class<?> beanType, String name) {
		return entries.stream().anyMatch(x -> x.matches(beanType, name)) || parent.map(x -> x.hasBean(beanType, name)).orElse(false);
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