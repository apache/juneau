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
	 * Default constructor.
	 */
	public BeanFactory() {
		this(Optional.empty(), Optional.empty());
	}

	/**
	 * Constructor.
	 *
	 * @param parent Parent bean factory.  Can be <jk>null</jk> if this is the root resource.
	 * @param outer Outer bean context to use when instantiating local classes.  Can be <jk>null</jk>.
	 */
	public BeanFactory(BeanFactory parent, Object outer) {
		this(Optional.ofNullable(parent), Optional.ofNullable(outer));
	}

	/**
	 * Constructor.
	 *
	 * @param parent - Optional parent bean factory.
	 * @param outer Outer bean context to use when instantiating local classes.
	 */
	public BeanFactory(Optional<BeanFactory> parent, Optional<Object> outer) {
		this.parent = parent;
		this.outer = outer;
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
	 * Adds a bean supplier of the specified type to this factory.
	 *
	 * @param <T> The class to associate this bean with.
	 * @param c The class to associate this bean with.
	 * @param t The bean supplier.
	 * @return This object (for method chaining).
	 */
	public <T> BeanFactory addBean(Class<T> c, Supplier<T> t) {
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

		for (MethodInfo m : ci.getPublicMethods()) {
			if (m.isAll(STATIC, NOT_DEPRECATED) && m.hasReturnType(c) && (!m.hasAnnotation(BeanIgnore.class))) {
				String n = m.getSimpleName();
				if (isOneOf(n, "create","getInstance")) {
					List<ClassInfo> missing = getMissingParamTypes(m.getParamTypes());
					if (missing.isEmpty())
						return m.invoke(null, getParams(m.getParamTypes()));
					msg = ()-> "Static creator found but could not find prerequisites: " + missing.stream().map(x->x.getSimpleName()).collect(Collectors.joining(","));
				}
			}
		}

		if (ci.isInterface())
			throw new ExecutableException("Could not instantiate class {0}: {1}.", c.getName(), msg != null ? msg.get() : "Class is an interface");
		if (ci.isAbstract())
			throw new ExecutableException("Could not instantiate class {0}: {1}.", c.getName(), msg != null ? msg.get() : "Class is abstract");

		for (ConstructorInfo cc : ci.getPublicConstructors()) {
			List<ClassInfo> missing = getMissingParamTypes(cc.getParamTypes());
			if (missing.isEmpty())
				return cc.invoke(getParams(cc.getParamTypes()));
			msg = ()-> "Public constructor found but could not find prerequisites: " + missing.stream().map(x->x.getSimpleName()).collect(Collectors.joining(","));
		}

		if (msg == null)
			msg = () -> "Public constructor or creator not found";

		throw new ExecutableException("Could not instantiate class {0}: {1}.", c.getName(), msg.get());
	}


	/**
	 * Creates a bean via a static or non-static method defined on the specified class.
	 *
	 * @param <T> The bean type to create.
	 * @param c The bean type to create.
	 * @param resource The object where the method is defined.
	 * @param methodName The method name on the object to call.
	 * @param requiredParams The parameter types that must be present on the method.
	 * @return A newly-created bean or <jk>null</jk> if method not found or it returns <jk>null</jk>.
	 * @throws ExecutableException If bean could not be created.
	 */
	public <T> T createBeanViaMethod(Class<T> c, Object resource, String methodName, Class<?>...requiredParams) throws ExecutableException {
		ClassInfo ci = ClassInfo.of(resource);
		for (MethodInfo m : ci.getPublicMethods()) {
			if (m.isAll(NOT_DEPRECATED) && m.hasReturnType(c) && m.getSimpleName().equals(methodName) && (!m.hasAnnotation(BeanIgnore.class))) {
				List<ClassInfo> missing = getMissingParamTypes(m.getParamTypes());
				if (missing.isEmpty() && m.hasAllArgs(requiredParams))
					return m.invoke(resource, getParams(m.getParamTypes()));
			}
		}
		return null;
	}

	/**
	 * Given the list of param types, returns a list of types that are missing from this factory.
	 *
	 * @param paramTypes The param types to chec.
	 * @return A list of types that are missing from this factory.
	 */
	public List<ClassInfo> getMissingParamTypes(List<ClassInfo> paramTypes) {
		List<ClassInfo> l = AList.of();
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
	 * Returns the contents of this bean factory as a readable map of values.
	 *
	 * @return The contents of this bean factory as a readable map of values.
	 */
	public OMap toMap() {
		return OMap.of()
			.a("beanMap", beanMap.keySet().stream().map(x -> x.getSimpleName()).collect(Collectors.toList()))
			.a("outer", ObjectUtils.identity(outer))
			.a("parent", ObjectUtils.identity(parent));
	}

	@Override /* Object */
	public String toString() {
		return toMap().toString();
	}
}
