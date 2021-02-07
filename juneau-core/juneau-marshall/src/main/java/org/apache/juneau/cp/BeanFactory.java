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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.reflect.ReflectFlags.*;

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
 * Factory for creating beans.
 */
public class BeanFactory {

	/**
	 * Non-existent bean factory.
	 */
	public static final class Null extends BeanFactory {}

	private final Map<Class<?>,Supplier<?>> beanMap = new ConcurrentHashMap<>();
	private final Optional<BeanFactory> parent;
	private final Optional<Object> outer;

	/**
	 * Static creator.
	 *
	 * @return A new {@link BeanFactoryBuilder} object.
	 */
	public static BeanFactoryBuilder create() {
		return new BeanFactoryBuilder();
	}

	/**
	 * Static creator.
	 *
	 * @param parent Parent bean factory.  Can be <jk>null</jk> if this is the root resource.
	 * @return A new {@link BeanFactory} object.
	 */
	public static BeanFactory of(BeanFactory parent) {
		return create().parent(parent).build();
	}

	/**
	 * Static creator.
	 *
	 * @param parent Parent bean factory.  Can be <jk>null</jk> if this is the root resource.
	 * @param outer The outer bean used when instantiating inner classes.  Can be <jk>null</jk>.
	 * @return A new {@link BeanFactory} object.
	 */
	public static BeanFactory of(BeanFactory parent, Object outer) {
		return create().parent(parent).outer(outer).build();
	}

	/**
	 * Default constructor.
	 */
	public BeanFactory() {
		this.parent = Optional.empty();
		this.outer = Optional.empty();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public BeanFactory(BeanFactoryBuilder builder) {
		this.parent = Optional.ofNullable(builder.parent);
		this.outer = Optional.ofNullable(builder.outer);
	}

	/**
	 * Returns the bean of the specified type.
	 *
	 * @param <T> The type of bean to return.
	 * @param c The type of bean to return.
	 * @return The bean.
	 */
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getBean(Class<T> c) {
		Supplier<?> o = beanMap.get(c);
		if (o == null && parent.isPresent())
			return parent.get().getBean(c);
		T t = (T)(o == null ? null : o.get());
		return Optional.ofNullable(t);
	}

	/**
	 * Returns the bean of the specified type.
	 *
	 * @param c The type of bean to return.
	 * @return The bean.
	 */
	public Optional<?> getBean(ClassInfo c) {
		return getBean(c.inner());
	}

	/**
	 * Adds a bean of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param c The class to associate this bean with.
	 * @param t The bean.
	 * @return This object (for method chaining).
	 */
	public <T> BeanFactory addBean(Class<T> c, T t) {
		if (t == null)
			beanMap.remove(c);
		else
			beanMap.put(c, ()->t);
		return this;
	}

	/**
	 * Same as {@link #addBean(Class, Object)} but also adds subtypes of the bean.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param c The class to associate this bean with.
	 * @param t The bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public <T> BeanFactory addBeans(Class<T> c, T t) {
		if (t == null)
			beanMap.remove(c);
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
	 * @return This object (for method chaining).
	 */
	public <T> BeanFactory addBeanSupplier(Class<T> c, Supplier<T> t) {
		if (t == null)
			beanMap.remove(c);
		else
			beanMap.put(c, t);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if this factory contains the specified bean type instance.
	 *
	 * @param c The bean type to check.
	 * @return <jk>true</jk> if this factory contains the specified bean type instance.
	 */
	public boolean hasBean(Class<?> c) {
		return getBean(c).isPresent();
	}

	/**
	 * Returns <jk>true</jk> if this factory contains the specified bean type instance.
	 *
	 * @param c The bean type to check.
	 * @return <jk>true</jk> if this factory contains the specified bean type instance.
	 */
	public final boolean hasBean(ClassInfo c) {
		return hasBean(c.inner());
	}

	/**
	 * Creates a bean of the specified type.
	 *
	 * @param <T> The bean type to create.
	 * @param c The bean type to create.
	 * @return A newly-created bean.
	 * @throws ExecutableException If bean could not be created.
	 */
	public <T> T createBean(Class<T> c) throws ExecutableException {

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
					List<ClassInfo> missing = getMissingParamTypes(m.getParamTypes());
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
			return matchedCreator.invoke(null, getParams(matchedCreator.getParamTypes()));

		if (ci.isInterface())
			throw new ExecutableException("Could not instantiate class {0}: {1}.", c.getName(), msg != null ? msg.get() : "Class is an interface");
		if (ci.isAbstract())
			throw new ExecutableException("Could not instantiate class {0}: {1}.", c.getName(), msg != null ? msg.get() : "Class is abstract");

		ConstructorInfo matchedConstructor = null;
		int matchedConstructorParams = -1;

		for (ConstructorInfo cc : ci.getPublicConstructors()) {
			List<ClassInfo> missing = getMissingParamTypes(cc.getParamTypes());
			if (missing.isEmpty()) {
				if (cc.getParamCount() > matchedConstructorParams) {
					matchedConstructorParams = cc.getParamCount();
					matchedConstructor = cc;
				}
			} else {
				msg = ()-> "Public constructor found but could not find prerequisites: " + missing.stream().map(x->x.getSimpleName()).collect(Collectors.joining(","));
			}
		}

		if (matchedConstructor != null)
			return matchedConstructor.invoke(getParams(matchedConstructor.getParamTypes()));

		if (msg == null)
			msg = () -> "Public constructor or creator not found";

		throw new ExecutableException("Could not instantiate class {0}: {1}.", c.getName(), msg.get());
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
	 * 		<jc>// Bean factory must have a C bean and optionally a D bean.</jc>
	 * 		<jk>public</jk> A createA(C <mv>c</mv>, Optional&lt;D&gt; <mv>d</mv>) {
	 * 			<jk>return new</jk> A(<mv>c</mv>, <mv>d</mv>.orElse(<jk>null</jk>));
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Instantiate the bean with the creator method.</jc>
	 * 	B <mv>b</mv> = <jk>new</jk> B();
	 *
	 *  <jc>// Create a bean factory with some mapped beans.</jc>
	 * 	BeanFactory <mv>beanFactory</mv> = BeanFactory.<jsm>create</jsm>().addBean(C.<jk>class</jk>, <jk>new</jk> C());
	 *
	 * 	<jc>// Instantiate the bean using the creator method.</jc>
	 * 	A <mv>a</mv> = <mv>beanFactory</mv>
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
	 * @param paramTypes The param types to chec.
	 * @return A list of types that are missing from this factory.
	 */
	public List<ClassInfo> getMissingParamTypes(List<ClassInfo> paramTypes) {
		List<ClassInfo> l = AList.create();
		for (int i = 0; i < paramTypes.size(); i++) {
			ClassInfo pt = paramTypes.get(i);
			ClassInfo ptu = pt.unwrap(Optional.class);
			if (i == 0 && ptu.isInstance(outer.orElse(null)))
				continue;
			if (! hasBean(ptu))
				if (! pt.is(Optional.class))
					l.add(pt);
		}
		return l;
	}

	/**
	 * Returns the corresponding beans in this factory for the specified param types.
	 *
	 * @param paramTypes The param types to get from this factory.
	 * @return The corresponding beans in this factory for the specified param types.
	 */
	public Object[] getParams(List<ClassInfo> paramTypes) {
		Object[] o = new Object[paramTypes.size()];
		for (int i = 0; i < paramTypes.size(); i++) {
			ClassInfo pt = paramTypes.get(i);
			ClassInfo ptu = pt.unwrap(Optional.class);
			if (i == 0 && ptu.isInstance(outer.orElse(null)))
				o[i] = outer.get();
			else {
				if (pt.is(Optional.class)) {
					o[i] = getBean(ptu);
				} else {
					o[i] = getBean(ptu).get();
				}
			}
		}
		return o;
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
			.a("beanMap", beanMap.keySet().stream().map(x -> x.getSimpleName()).collect(Collectors.toList()))
			.a("outer", ObjectUtils.identity(outer))
			.a("parent", parent.orElse(null));
	}

	@Override /* Object */
	public String toString() {
		return toMap().toString();
	}
}
