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

import static org.apache.juneau.collections.OMap.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static java.util.Optional.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Java bean store.
 *
 * <p>
 * Used for bean injection.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class BeanStore {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Non-existent bean store.
	 */
	public static final class Null extends BeanStore {}

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
	public static class Builder extends BeanBuilder<BeanStore> {

		BeanStore parent;
		boolean readOnly;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(BeanStore.class);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean store to copy from.
		 */
		protected Builder(BeanStore copyFrom) {
			super(copyFrom.getClass());
			parent = copyFrom.parent.orElse(null);
			readOnly = copyFrom.readOnly;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean store to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			parent = copyFrom.parent;
			readOnly = copyFrom.readOnly;
		}

		@Override /* BeanBuilder */
		protected BeanStore buildDefault() {
			return new BeanStore(this);
		}

		@Override /* BeanBuilder */
		public Builder copy() {
			return new Builder(this);
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

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder beanStore(BeanStore value) {
			super.beanStore(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Map<String,Supplier<?>> beanMap = new ConcurrentHashMap<>();
	final Optional<BeanStore> parent;
	final Optional<Object> outer;
	final boolean readOnly;

	BeanStore() {
		this.parent = empty();
		this.outer = empty();
		this.readOnly = false;
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	protected BeanStore(Builder builder) {
		this.parent = ofNullable(builder.parent);
		this.outer = builder.outer();
		this.readOnly = builder.readOnly;
	}

	/**
	 * Creates a copy of this bean store.
	 *
	 * @return A mutable copy of this bean store.
	 */
	public Builder copy() {
		return new Builder(this);
	}

	/**
	 * Returns the named bean of the specified type.
	 *
	 * @param <T> The type of bean to return.
	 * @param c The type of bean to return.
	 * @param name The bean name.
	 * @return The bean.
	 */
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getBean(String name, Class<T> c) {
		String key = name == null ? c.getName() : name;
		Supplier<?> o = beanMap.get(key);
		if (o == null && parent.isPresent())
			return parent.get().getBean(name, c);
		T t = (T)(o == null ? null : o.get());
		return Optional.ofNullable(t);
	}

	/**
	 * Returns the bean of the specified type.
	 *
	 * @param <T> The type of bean to return.
	 * @param c The type of bean to return.
	 * @return The bean.
	 */
	public <T> Optional<T> getBean(Class<T> c) {
		return getBean(c.getName(), c);
	}

	/**
	 * Adds a bean of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param c The class to associate this bean with.
	 * @param t The bean.
	 * @return This object.
	 */
	public <T> BeanStore addBean(Class<T> c, T t) {
		assertCanWrite();
		return addBean(c.getName(), t);
	}

	/**
	 * Same as {@link #addBean(Class, Object)} but returns the bean instead of this object.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param c The class to associate this bean with.
	 * @param t The bean.
	 * @return The bean.
	 */
	public <T> T add(Class<T> c, T t) {
		assertCanWrite();
		addBean(c.getName(), t);
		return t;
	}

	/**
	 * Same as {@link #addBean(String, Object)} but returns the bean instead of this object.
	 *
	 * @param name The name to associate this bean with.
	 * @param t The bean.
	 * @return The bean.
	 */
	public <T> T add(String name, T t) {
		assertCanWrite();
		addBean(name, t);
		return t;
	}

	/**
	 * Adds a named bean of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param t The bean.
	 * @param name The bean name if this is a named bean.
	 * @return This object.
	 */
	public <T> BeanStore addBean(String name, T t) {
		assertCanWrite();
		if (t == null)
			beanMap.remove(name);
		else
			beanMap.put(name, ()->t);
		return this;
	}

	/**
	 * Same as {@link #addBean(Class, Object)} but also adds subtypes of the bean.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param c The class to associate this bean with.
	 * @param t The bean.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public <T> BeanStore addBeans(Class<T> c, T t) {
		assertCanWrite();
		if (t == null)
			beanMap.remove(c.getName());
		else {
			addBean(c, t);
			Class<T> c2 = (Class<T>)t.getClass();
			while (c2 != c) {
				addBean(c2, t);
				c2 = (Class<T>) c2.getSuperclass();
			}
		}
		return this;
	}

	/**
	 * Adds a bean supplier of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param c The class to associate this bean with.
	 * @param t The bean supplier.
	 * @return This object.
	 */
	public <T> BeanStore addSupplier(Class<T> c, Supplier<T> t) {
		assertCanWrite();
		return addSupplier(c.getName(), t);
	}

	/**
	 * Adds a named bean supplier of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param t The bean supplier.
	 * @param name The bean name.
	 * @return This object.
	 */
	public <T> BeanStore addSupplier(String name, Supplier<T> t) {
		assertCanWrite();
		if (t == null)
			beanMap.remove(name);
		else
			beanMap.put(name, t);
		return this;
	}

	/**
	 * Removes a bean from this store.
	 *
	 * <p>
	 * This is equivalent to setting the bean type bean to <jk>null</jk>.
	 *
	 * @param c The bean type being removed.
	 * @return This object.
	 */
	public BeanStore removeBean(Class<?> c) {
		return addBean(c.getName(), null);
	}

	/**
	 * Returns <jk>true</jk> if this factory contains the specified bean type instance.
	 *
	 * @param c The bean type to check.
	 * @return <jk>true</jk> if this factory contains the specified bean type instance.
	 */
	public boolean hasBean(Class<?> c) {
		return hasBean(c.getName());
	}

	/**
	 * Returns <jk>true</jk> if this factory contains a bean with the specified name.
	 *
	 * @param name The bean name.
	 * @return <jk>true</jk> if this factory contains a bean with the specified name.
	 */
	public boolean hasBean(String name) {
		if (getBean(name, Object.class).isPresent())
			return true;
		if (parent.isPresent())
			return parent.get().hasBean(name);
		return false;
	}

	/**
	 * Instantiates a bean creator.
	 *
	 * @param c The bean type to create.
	 * @return A new bean creator.
	 */
	public <T> BeanCreator<T> creator(Class<T> c) {
		return new BeanCreator<>(c).store(this).outer(outer.orElse(null));
	}

	/**
	 * Create a method finder for finding bean creation methods.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// The bean we want to create.</jc>
	 * 	<jk>public class</jk> A {}
	 *
	 * 	<jc>// The bean that has a creator method for the bean above.</jc>
	 * 	<jk>public class</jk> B {
	 *
	 * 		<jc>// Creator method.</jc>
	 * 		<jc>// Bean store must have a C bean and optionally a D bean.</jc>
	 * 		<jk>public</jk> A createA(C <mv>c</mv>, Optional&lt;D&gt; <mv>d</mv>) {
	 * 			<jk>return new</jk> A(<mv>c</mv>, <mv>d</mv>.orElse(<jk>null</jk>));
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Instantiate the bean with the creator method.</jc>
	 * 	B <mv>b</mv> = <jk>new</jk> B();
	 *
	 *  <jc>// Create a bean store with some mapped beans.</jc>
	 * 	BeanStore <mv>beanStore</mv> = BeanStore.<jsm>create</jsm>().addBean(C.<jk>class</jk>, <jk>new</jk> C());
	 *
	 * 	<jc>// Instantiate the bean using the creator method.</jc>
	 * 	A <mv>a</mv> = <mv>beanStore</mv>
	 * 		.beanCreateMethodFinder(A.<jk>class</jk>, <mv>b</mv>)  <jc>// Looking for creator for A on b object.</jc>
	 * 		.find(<js>"createA"</js>)                         <jc>// Look for method called "createA".</jc>
	 * 		.thenFind(<js>"createA2"</js>)                    <jc>// Then look for method called "createA2".</jc>
	 * 		.withDefault(()-&gt;<jk>new</jk> A())                        <jc>// Optionally supply a default value if method not found.</jc>
	 * 		.run();                                  <jc>// Execute.</jc>
	 * </p>
	 *
	 * @param <T> The bean type to create.
	 * @param c The bean type to create.
	 * @param resource The class containing the bean creator method.
	 * @return The created bean or the default value if method could not be found.
	 */
	public <T> BeanCreateMethodFinder<T> createMethodFinder(Class<T> c, Object resource) {
		return new BeanCreateMethodFinder<>(c, resource, this);
	}

	/**
	 * Constructor.
	 *
	 * @param c The bean type to create.
	 * @param resourceClass The class containing the bean creator method.
	 * @return The created bean or the default value if method could not be found.
	 */
	public <T> BeanCreateMethodFinder<T> createMethodFinder(Class<T> c, Class<?> resourceClass) {
		return new BeanCreateMethodFinder<>(c, resourceClass, this);
	}

	/**
	 * Constructor.
	 *
	 * @param c The bean type to create.
	 * @return The created bean or the default value if method could not be found.
	 */
	public <T> BeanCreateMethodFinder<T> beanCreateMethodFinder(Class<T> c) {
		if (outer == null)
			throw runtimeException("Method cannot be used without outer bean definition.");
		return new BeanCreateMethodFinder<>(c, outer, this);
	}

	/**
	 * Given the list of param types, returns a list of types that are missing from this factory.
	 *
	 * @param params The param types to chec.
	 * @return A list of types that are missing from this factory.
	 */
	public List<ClassInfo> getMissingParamTypes(List<ParamInfo> params) {
		List<ClassInfo> l = AList.create();
		loop: for (int i = 0; i < params.size(); i++) {
			ParamInfo pi = params.get(i);
			ClassInfo pt = pi.getParameterType();
			ClassInfo ptu = pt.unwrap(Optional.class);
			if (i == 0 && ptu.isInstance(outer.orElse(null)))
				continue loop;
			if (pt.is(Optional.class))
				continue loop;
			String beanName = findBeanName(pi);
			if (beanName == null)
				beanName = ptu.inner().getName();
			if (! hasBean(beanName))
				l.add(pt);
		}
		return l;
	}

	/**
	 * Returns the corresponding beans in this factory for the specified param types.
	 *
	 * @param params The parameters to get from this factory.
	 * @return The corresponding beans in this factory for the specified param types.
	 */
	public Object[] getParams(List<ParamInfo> params) {
		Object[] o = new Object[params.size()];
		for (int i = 0; i < params.size(); i++) {
			ParamInfo pi = params.get(i);
			ClassInfo pt = pi.getParameterType();
			ClassInfo ptu = pt.unwrap(Optional.class);
			if (i == 0 && ptu.isInstance(outer.orElse(null)))
				o[i] = outer.get();
			else {
				String beanName = findBeanName(pi);
				if (pt.is(Optional.class)) {
					o[i] = getBean(beanName, ptu.inner());
				} else {
					o[i] = getBean(beanName, ptu.inner()).get();
				}
			}
		}
		return o;
	}

	private String findBeanName(ParamInfo pi) {
		Optional<Annotation> namedAnnotation = pi.getAnnotations(Annotation.class).stream().filter(x->x.annotationType().getSimpleName().equals("Named")).findFirst();
		if (namedAnnotation.isPresent())
			return AnnotationInfo.of((ClassInfo)null, namedAnnotation.get()).getValue(String.class, "value").orElse(null);
		return null;
	}

	private void assertCanWrite() {
		if (readOnly)
			throw runtimeException("Method cannot be used because BeanStore is read-only.");
	}

	OMap properties() {
		return filteredMap()
			.a("beanMap", beanMap.keySet())
			.a("outer", ObjectUtils.identity(outer))
			.a("parent", parent.map(x->x.properties()).orElse(null));
	}

	@Override /* Object */
	public String toString() {
		return properties().asString();
	}
}
