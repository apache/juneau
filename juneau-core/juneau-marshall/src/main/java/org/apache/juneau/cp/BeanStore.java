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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.reflect.ReflectFlags.*;
import static java.util.Optional.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Java bean store.
 *
 * <p>
 * Used for bean injection.
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
	public static class Builder {

		Class<? extends BeanStore> implClass = BeanStore.class;
		BeanStore impl;
		Object outer;
		BeanStore parent;
		boolean readOnly;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean store to copy from.
		 */
		protected Builder(BeanStore copyFrom) {
			implClass = copyFrom.getClass();
			outer = copyFrom.outer.orElse(null);
			parent = copyFrom.parent.orElse(null);
			readOnly = copyFrom.readOnly;
		}

		/**
		 * Create a new {@link BeanStore} using this builder.
		 *
		 * @return A new {@link BeanStore}
		 */
		public BeanStore build() {
			try {
				if (impl != null)
					return impl;
				if (implClass == BeanStore.class)
					return new BeanStore(this);
				Class<? extends BeanStore> ic = isConcrete(implClass) ? implClass : BeanStore.class;
				return new BeanStore().addBeans(Builder.class, this).createBean(ic);
			} catch (ExecutableException e) {
				throw runtimeException(e);
			}
		}

		/**
		 * Specifies a subclass of {@link BeanStore} to create when the {@link #build()} method is called.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		@FluentSetter
		public Builder implClass(Class<? extends BeanStore> value) {
			implClass = value;
			return this;
		}

		/**
		 * Specifies the parent bean store.
		 *
		 * <p>
		 * Bean searches are performed recursively up this parent chain.
		 *
		 * @param value The new value for this setting.
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
		 * Specifies the outer bean context.
		 *
		 * <p>
		 * Used when calling {@link BeanStore#createBean(Class)} on a non-static inner class.
		 * This should be the instance of the outer object such as the servlet object when constructing inner classes
		 * of the servlet class.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		@FluentSetter
		public Builder outer(Object value) {
			outer = value;
			return this;
		}

		/**
		 * Specifies an implementation of a bean store.
		 *
		 * <p>
		 * Causes the {@link #build()} method to return a predefined bean instead of a new one.
		 *
		 * @param value The bean that this builder should return.
		 * @return  This object.
		 */
		public Builder impl(BeanStore value) {
			this.impl = value;
			return this;
		}
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
		this.outer = ofNullable(builder.outer);
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
	 * Creates a bean of the specified type.
	 *
	 * @param <T> The bean type to create.
	 * @param c The bean type to create.  Can be <jk>null</jk>.
	 * @return A newly-created bean, or <jk>null</jk> if the type was <jk>null</jk>.
	 * @throws ExecutableException If bean could not be created.
	 */
	public <T> T createBean(Class<T> c) throws ExecutableException {

		if (c == null)
			return null;

		Optional<T> o = getBean(c);
		if (o.isPresent())
			return o.get();

		ClassInfo ci = ClassInfo.of(c);

		Supplier<String> msg = null;

		MethodInfo matchedCreator = null;
		int matchedCreatorParams = -1;

		for (MethodInfo m : ci.getPublicMethods()) {

			if (m.isAll(STATIC, NOT_DEPRECATED) && m.hasReturnType(c) && (!m.hasAnnotation(BeanIgnore.class))) {
				String n = m.getSimpleName();
				if (isOneOf(n, "create","getInstance")) {
					List<ClassInfo> missing = getMissingParamTypes(m.getParams());
					if (missing.isEmpty()) {
						if (m.getParamCount() > matchedCreatorParams) {
							matchedCreatorParams = m.getParamCount();
							matchedCreator = m;
						}
					} else {
						msg = ()-> "Static creator found but could not find prerequisites: " + missing.stream().map(x->x.getSimpleName()).collect(Collectors.joining(","));
					}
				}
			}
		}

		if (matchedCreator != null)
			return matchedCreator.invoke(null, getParams(matchedCreator.getParams()));

		if (ci.isInterface())
			throw new ExecutableException("Could not instantiate class {0}: {1}.", c.getName(), msg != null ? msg.get() : "Class is an interface");
		if (ci.isAbstract())
			throw new ExecutableException("Could not instantiate class {0}: {1}.", c.getName(), msg != null ? msg.get() : "Class is abstract");

		ConstructorInfo matchedConstructor = null;
		int matchedConstructorParams = -1;

		for (ConstructorInfo cc : ci.getPublicConstructors()) {
			List<ClassInfo> missing = getMissingParamTypes(cc.getParams());
			if (missing.isEmpty()) {
				if (cc.getParamCount() > matchedConstructorParams) {
					matchedConstructorParams = cc.getParamCount();
					matchedConstructor = cc;
				}
			} else {
				msg = ()-> "Public constructor found but could not find prerequisites: " + missing.stream().map(x->x.getSimpleName()).collect(Collectors.joining(","));
			}
		}

		if (matchedConstructor == null) {
			for (ConstructorInfo cc : ci.getDeclaredConstructors()) {
				if (cc.isProtected()) {
					List<ClassInfo> missing = getMissingParamTypes(cc.getParams());
					if (missing.isEmpty()) {
						if (cc.getParamCount() > matchedConstructorParams) {
							matchedConstructorParams = cc.getParamCount();
							matchedConstructor = cc.accessible();
						}
					} else {
						msg = ()-> "Protected constructor found but could not find prerequisites: " + missing.stream().map(x->x.getSimpleName()).collect(Collectors.joining(","));
					}
				}
			}
		}

		// Look for static-builder/protected-constructor pair.
		if (matchedConstructor == null) {
			for (ConstructorInfo cc2 : ci.getDeclaredConstructors()) {
				if (cc2.getParamCount() == 1 && cc2.isVisible(Visibility.PROTECTED)) {
					Class<?> pt = cc2.getParam(0).getParameterType().inner();
					for (MethodInfo m : ci.getPublicMethods()) {
						if (m.isAll(STATIC, NOT_DEPRECATED) && m.hasReturnType(pt) && (!m.hasAnnotation(BeanIgnore.class))) {
							Object builder = m.invoke(null);
							return cc2.accessible().invoke(builder);
						}
					}
				}
			}
		}

		if (matchedConstructor != null)
			return matchedConstructor.invoke(getParams(matchedConstructor.getParams()));

		if (msg == null)
			msg = () -> "Public constructor or creator not found";

		throw new ExecutableException("Could not instantiate class {0}: {1}.", c.getName(), msg.get());
	}

	/**
	 * Same as {@link #createBean(Class)} but allows you to validate that the specified type is of the specified parent class.
	 *
	 * @param <T> The bean type to create.
	 * @param c The bean type to create.
	 * @param type The bean subtype to create.
	 * @return A newly-created bean.
	 * @throws ExecutableException If bean could not be created.
	 */
	public <T> T createBean(Class<T> c, Class<? extends T> type) {
		if (type != null && ! c.isAssignableFrom(type))
			throw new ExecutableException("Could not instantiate class of type {0} because it was not a subtype of the class: {1}.", c.getName(), type);
		return createBean(type);
	}

	/**
	 * Same as {@link #createBean(Class)} but returns the bean creation wrapped in a supplier.
	 *
	 * @param c The bean type to create.
	 * @return A supplier for the newly-created bean.
	 */
	public <T> Supplier<T> createBeanSupplier(Class<T> c) {
		return () -> {
			try {
				return createBean(c);
			} catch (ExecutableException e) {
				throw runtimeException(e);
			}
		};
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
	public <T> BeanCreateMethodFinder<T> beanCreateMethodFinder(Class<T> c, Object resource) {
		return new BeanCreateMethodFinder<>(c, resource, this);
	}

	/**
	 * Given the list of param types, returns a list of types that are missing from this factory.
	 *
	 * @param params The param types to chec.
	 * @return A list of types that are missing from this factory.
	 */
	public List<ClassInfo> getMissingParamTypes(List<ParamInfo> params) {
		List<ClassInfo> l = AList.create();
		for (int i = 0; i < params.size(); i++) {
			ParamInfo pi = params.get(i);
			ClassInfo pt = pi.getParameterType();
			ClassInfo ptu = pt.unwrap(Optional.class);
			if (i == 0 && ptu.isInstance(outer.orElse(null)))
				continue;
			if (pt.is(Optional.class))
				continue;
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

	/**
	 * Returns the properties defined on this bean as a simple map for debugging purposes.
	 *
	 * <p>
	 * Use <c>SimpleJson.<jsf>DEFAULT</jsf>.println(<jv>thisBean</jv>)</c> to dump the contents of this bean to the console.
	 *
	 * @return A new map containing this bean's properties.
	 */
	public OMap toMap() {
		return OMap
			.create()
			.filtered()
			.a("beanMap", beanMap.keySet())
			.a("outer", ObjectUtils.identity(outer))
			.a("parent", parent.orElse(null));
	}

	private void assertCanWrite() {
		if (readOnly)
			throw runtimeException("Method cannot be used because BeanStore is read-only.");
	}

	@Override /* Object */
	public String toString() {
		return toMap().toString();
	}
}
