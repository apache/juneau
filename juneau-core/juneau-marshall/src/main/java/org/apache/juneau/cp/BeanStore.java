// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.cp;

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static java.util.stream.Collectors.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.reflect.*;

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
 * the <ja>@Named</ja> or <ja>@Qualified</ja> annotations on constructor or method parameters.
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
 * 	<li class='jm'>{@link #createBean(Class) createBean(Class)}
 * 	<li class='jm'>{@link #createMethodFinder(Class) createMethodFinder(Class)}
 * 	<li class='jm'>{@link #createMethodFinder(Class,Class) createMethodFinder(Class,Class)}
 * 	<li class='jm'>{@link #createMethodFinder(Class,Object) createMethodFinder(Class,Object)}
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Bean stores can be nested using {@link Builder#parent(BeanStore)}.
 * 	<li class='note'>Bean stores can be made read-only using {@link Builder#readOnly()}.
 * 	<li class='note'>Bean stores can be made thread-safe using {@link Builder#threadSafe()}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class BeanStore {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Non-existent bean store.
	 */
	public static final class Void extends BeanStore {}

	/**
	 * Static read-only reusable instance.
	 */
	public static final BeanStore INSTANCE = create().readOnly().build();

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
	 * @return A new {@link BeanStore} object.
	 */
	public static BeanStore of(BeanStore parent) {
		return create().parent(parent).build();
	}

	/**
	 * Static creator.
	 *
	 * @param parent Parent bean store.  Can be <jk>null</jk> if this is the root resource.
	 * @param outer The outer bean used when instantiating inner classes.  Can be <jk>null</jk>.
	 * @return A new {@link BeanStore} object.
	 */
	public static BeanStore of(BeanStore parent, Object outer) {
		return create().parent(parent).outer(outer).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder {

		BeanStore parent;
		boolean readOnly, threadSafe;
		Object outer;
		Class<? extends BeanStore> type;
		BeanStore impl;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Instantiates this bean store.
		 *
		 * @return A new bean store.
		 */
		public BeanStore build() {
			if (impl != null)
				return impl;
			if (type == null || type == BeanStore.class)
				return new BeanStore(this);

			ClassInfo c = ClassInfo.of(type);

			MethodInfo m = c.getDeclaredMethod(
				x -> x.isPublic()
				&& x.hasNoParams()
				&& x.isStatic()
				&& x.hasName("getInstance")
			);
			if (m != null)
				return m.invoke(null);

			ConstructorInfo ci = c.getPublicConstructor(x -> x.canAccept(this));
			if (ci != null)
				return ci.invoke(this);

			ci = c.getDeclaredConstructor(x -> x.isProtected() && x.canAccept(this));
			if (ci != null)
				return ci.accessible().invoke(this);

			throw new BasicRuntimeException("Could not find a way to instantiate class {0}", type);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Specifies the parent bean store.
		 *
		 * <p>
		 * Bean searches are performed recursively up this parent chain.
		 *
		 * @param value The setting value.
		 * @return  This object.
		 */
		@FluentSetter
		public Builder parent(BeanStore value) {
			parent = value;
			return this;
		}

		/**
		 * Specifies that the bean store is read-only.
		 *
		 * <p>
		 * This means methods such as {@link BeanStore#addBean(Class, Object)} cannot be used.
		 *
		 * @return  This object.
		 */
		@FluentSetter
		public Builder readOnly() {
			readOnly = true;
			return this;
		}

		/**
		 * Specifies that the bean store being created should be thread-safe.
		 *
		 * @return  This object.
		 */
		@FluentSetter
		public Builder threadSafe() {
			threadSafe = true;
			return this;
		}

		/**
		 * Specifies the outer bean context.
		 *
		 * <p>
		 * The outer context bean to use when calling constructors on inner classes.
		 *
		 * @param value The outer bean context.  Can be <jk>null</jk>.
		 * @return  This object.
		 */
		@FluentSetter
		public Builder outer(Object value) {
			this.outer = value;
			return this;
		}

		/**
		 * Overrides the bean to return from the {@link #build()} method.
		 *
		 * @param value The bean to return from the {@link #build()} method.
		 * @return This object.
		 */
		@FluentSetter
		public Builder impl(BeanStore value) {
			this.impl = value;
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
		@FluentSetter
		public Builder type(Class<? extends BeanStore> value) {
			this.type = value;
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Deque<BeanStoreEntry<?>> entries;
	private final Map<Class<?>,BeanStoreEntry<?>> unnamedEntries;

	final Optional<BeanStore> parent;
	final Optional<Object> outer;
	final boolean readOnly, threadSafe;
	final SimpleReadWriteLock lock;

	BeanStore() {
		this(create());
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	protected BeanStore(Builder builder) {
		parent = optional(builder.parent);
		outer = optional(builder.outer);
		readOnly = builder.readOnly;
		threadSafe = builder.threadSafe;
		lock = threadSafe ? new SimpleReadWriteLock() : SimpleReadWriteLock.NO_OP;
		entries = threadSafe ? new ConcurrentLinkedDeque<>() : linkedList();
		unnamedEntries = threadSafe ? new ConcurrentHashMap<>() : map();
	}

	/**
	 * Adds an unnamed bean of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public <T> BeanStore addBean(Class<T> beanType, T bean) {
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
	public <T> BeanStore addBean(Class<T> beanType, T bean, String name) {
		return addSupplier(beanType, ()->bean, name);
	}

	/**
	 * Adds a supplier for an unnamed bean of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param beanType The class to associate this bean with.
	 * @param bean The bean supplier.
	 * @return This object.
	 */
	public <T> BeanStore addSupplier(Class<T> beanType, Supplier<T> bean) {
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
	public <T> BeanStore addSupplier(Class<T> beanType, Supplier<T> bean, String name) {
		assertCanWrite();
		BeanStoreEntry<T> e = createEntry(beanType, bean, name);
		try (SimpleLock x = lock.write()) {
			entries.addFirst(e);
			if (isEmpty(name))
				unnamedEntries.put(beanType, e);
		}
		return this;
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
	 * Clears out all bean in this bean store.
	 *
	 * <p>
	 * Does not affect the parent bean store.
	 *
	 * @return This object.
	 */
	public BeanStore clear() {
		assertCanWrite();
		try (SimpleLock x = lock.write()) {
			unnamedEntries.clear();
			entries.clear();
		}
		return this;
	}

	/**
	 * Returns the unnamed bean of the specified type.
	 *
	 * @param <T> The type of bean to return.
	 * @param beanType The type of bean to return.
	 * @return The bean.
	 */
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getBean(Class<T> beanType) {
		try (SimpleLock x = lock.read()) {
			BeanStoreEntry<T> e = (BeanStoreEntry<T>) unnamedEntries.get(beanType);
			if (e != null)
				return optional(e.get());
			if (parent.isPresent())
				return parent.get().getBean(beanType);
			return empty();
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
	public <T> Optional<T> getBean(Class<T> beanType, String name)  {
		try (SimpleLock x = lock.read()) {
			BeanStoreEntry<T> e = (BeanStoreEntry<T>)entries.stream().filter(x2 -> x2.matches(beanType, name)).findFirst().orElse(null);
			if (e != null)
				return optional(e.get());
			if (parent.isPresent())
				return parent.get().getBean(beanType, name);
			return empty();
		}
	}

	/**
	 * Returns all the beans in this store of the specified type.
	 *
	 * <p>
	 * Returns both named and unnamed beans.
	 *
	 * <p>
	 * The results from the parent bean store are appended to the list of beans from this beans store.
	 *
	 * @param <T> The bean type to return.
	 * @param beanType The bean type to return.
	 * @return The bean entries.  Never <jk>null</jk>.
	 */
	public <T> Stream<BeanStoreEntry<T>> stream(Class<T> beanType)  {
		@SuppressWarnings("unchecked")
		Stream<BeanStoreEntry<T>> s = entries.stream().filter(x -> x.matches(beanType)).map(x -> (BeanStoreEntry<T>)x);
		if (parent.isPresent())
			s = Stream.concat(s, parent.get().stream(beanType));
		return s;
	}

	/**
	 * Removes an unnamed bean from this store.
	 *
	 * @param beanType The bean type being removed.
	 * @return This object.
	 */
	public BeanStore removeBean(Class<?> beanType) {
		return removeBean(beanType, null);
	}

	/**
	 * Removes a named bean from this store.
	 *
	 * @param beanType The bean type being removed.
	 * @param name The bean name to remove.
	 * @return This object.
	 */
	public BeanStore removeBean(Class<?> beanType, String name) {
		assertCanWrite();
		try (SimpleLock x = lock.write()) {
			if (name == null)
				unnamedEntries.remove(beanType);
			entries.removeIf(y -> y.matches(beanType, name));
		}
		return this;
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

	/**
	 * Instantiates a bean creator.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link BeanCreator} for usage.
	 * </ul>
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @return A new bean creator.
	 */
	public <T> BeanCreator<T> createBean(Class<T> beanType) {
		return new BeanCreator<>(beanType, this);
	}

	/**
	 * Create a method finder for finding bean creation methods.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link BeanCreateMethodFinder} for usage.
	 * </ul>
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @param resource The class containing the bean creator method.
	 * @return The method finder.  Never <jk>null</jk>.
	 */
	public <T> BeanCreateMethodFinder<T> createMethodFinder(Class<T> beanType, Object resource) {
		return new BeanCreateMethodFinder<>(beanType, resource, this);
	}

	/**
	 * Create a method finder for finding bean creation methods.
	 *
	 * <p>
	 * Same as {@link #createMethodFinder(Class,Class)} but looks for only static methods on the specified resource class
	 * and not also instance methods within the context of a bean.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link BeanCreateMethodFinder} for usage.
	 * </ul>
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @param resourceClass The class containing the bean creator method.
	 * @return The method finder.  Never <jk>null</jk>.
	 */
	public <T> BeanCreateMethodFinder<T> createMethodFinder(Class<T> beanType, Class<?> resourceClass) {
		return new BeanCreateMethodFinder<>(beanType, resourceClass , this);
	}

	/**
	 * Create a method finder for finding bean creation methods.
	 *
	 * <p>
	 * Same as {@link #createMethodFinder(Class,Object)} but uses {@link Builder#outer(Object)} as the resource bean.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link BeanCreateMethodFinder} for usage.
	 * </ul>
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @return The method finder.  Never <jk>null</jk>.
	 */
	public <T> BeanCreateMethodFinder<T> createMethodFinder(Class<T> beanType) {
		return new BeanCreateMethodFinder<>(beanType, outer.orElseThrow(()->new RuntimeException("Method cannot be used without outer bean definition.")), this);
	}

	/**
	 * Given an executable, returns a list of types that are missing from this factory.
	 *
	 * @param executable The constructor or method to get the params for.
	 * @return A comma-delimited list of types that are missing from this factory, or <jk>null</jk> if none are missing.
	 */
	public String getMissingParams(ExecutableInfo executable) {
		List<ParamInfo> params = executable.getParams();
		List<String> l = list();
		loop: for (int i = 0; i < params.size(); i++) {
			ParamInfo pi = params.get(i);
			ClassInfo pt = pi.getParameterType();
			if (i == 0 && outer.isPresent() && pt.isInstance(outer.get()))
				continue loop;
			if (pt.is(Optional.class) || pt.is(BeanStore.class))
				continue loop;
			String beanName = findBeanName(pi);
			Class<?> ptc = pt.inner();
			if (beanName == null && !hasBean(ptc))
				l.add(pt.getSimpleName());
			if (beanName != null && !hasBean(ptc, beanName))
				l.add(pt.getSimpleName() + '@' + beanName);
		}
		return l.isEmpty() ? null : l.stream().sorted().collect(joining(","));
	}

	/**
	 * Given the list of param types, returns <jk>true</jk> if this factory has all the parameters for the specified executable.
	 *
	 * @param executable The constructor or method to get the params for.
	 * @return A comma-delimited list of types that are missing from this factory.
	 */
	public boolean hasAllParams(ExecutableInfo executable) {
		loop: for (int i = 0; i < executable.getParamCount(); i++) {
			ParamInfo pi = executable.getParam(i);
			ClassInfo pt = pi.getParameterType();
			if (i == 0 && outer.isPresent() && pt.isInstance(outer.get()))
				continue loop;
			if (pt.is(Optional.class) || pt.is(BeanStore.class))
				continue loop;
			String beanName = findBeanName(pi);
			Class<?> ptc = pt.inner();
			if ((beanName == null && !hasBean(ptc)) || (beanName != null && !hasBean(ptc, beanName)))
				return false;
		}
		return true;
	}


	/**
	 * Returns the corresponding beans in this factory for the specified param types.
	 *
	 * @param executable The constructor or method to get the params for.
	 * @return The corresponding beans in this factory for the specified param types.
	 */
	public Object[] getParams(ExecutableInfo executable) {
		Object[] o = new Object[executable.getParamCount()];
		for (int i = 0; i < executable.getParamCount(); i++) {
			ParamInfo pi = executable.getParam(i);
			ClassInfo pt = pi.getParameterType();
			if (i == 0 && outer.isPresent() && pt.isInstance(outer.get())) {
				o[i] = outer.get();
			} else if (pt.is(BeanStore.class)) {
				o[i] = this;
			} else {
				String beanName = findBeanName(pi);
				Class<?> ptc = pt.unwrap(Optional.class).inner();
				Optional<?> o2 = beanName == null ? getBean(ptc) : getBean(ptc, beanName);
				o[i] = pt.is(Optional.class) ? o2 : o2.orElse(null);
			}
		}
		return o;
	}

	@Override /* Object */
	public String toString() {
		return Json5.of(properties());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extension methods
	//-----------------------------------------------------------------------------------------------------------------

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
	protected <T> BeanStoreEntry<T> createEntry(Class<T> type, Supplier<T> bean, String name) {
		return BeanStoreEntry.create(type, bean, name);
	}

	// <FluentSetters>

		// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------
	private String findBeanName(ParamInfo pi) {
		Annotation n = pi.getAnnotation(Annotation.class, x -> x.annotationType().getSimpleName().equals("Named"));
		if (n != null)
			return AnnotationInfo.of((ClassInfo)null, n).getValue(String.class, "value", NOT_EMPTY).orElse(null);
		return null;
	}

	private void assertCanWrite() {
		if (readOnly)
			throw new RuntimeException("Method cannot be used because BeanStore is read-only.");
	}

	private JsonMap properties() {
		Predicate<Boolean> nf = ObjectUtils::isTrue;
		return filteredMap()
			.append("identity", ObjectUtils.identity(this))
			.append("entries", entries.stream().map(x -> x.properties()).collect(toList()))
			.append("outer", ObjectUtils.identity(outer.orElse(null)))
			.append("parent", parent.map(x->x.properties()).orElse(null))
			.appendIf(nf, "readOnly", readOnly)
			.appendIf(nf, "threadSafe", threadSafe)
		;
	}
}
