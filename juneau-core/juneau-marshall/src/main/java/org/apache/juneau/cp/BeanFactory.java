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

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;

/**
 * Factory for creating beans.
 */
public class BeanFactory {

	private final Map<Class<?>,Object> beanMap = new ConcurrentHashMap<>();
	private final BeanFactory parent;
	private final Object outer;

	/**
	 * Default constructor.
	 */
	public BeanFactory() {
		this.parent = null;
		this.outer = null;
	}

	/**
	 * Constructor.
	 *
	 * @param parent - Optional parent bean factory.
	 * @param outer Outer bean context to use when instantiating local classes.
	 */
	public BeanFactory(BeanFactory parent, Object outer) {
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
		T t = (T)beanMap.get(c);
		if (t == null && parent != null)
			return parent.getBean(c);
		return Optional.ofNullable(t);
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
		beanMap.put(c, t);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if this factory contains the specified bean type instance.
	 *
	 * @param <T> The bean type to check.
	 * @param c The bean type to check.
	 * @return <jk>true</jk> if this factory contains the specified bean type instance.
	 */
	public <T> boolean hasBean(Class<T> c) {
		return getBean(c).isPresent();
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

		ClassInfo ci = ClassInfo.of(c);

		for (MethodInfo m : ci.getPublicMethods()) {
			if (m.isAll(STATIC, NOT_DEPRECATED) && m.hasReturnType(c) && hasAllParamTypes(m.getParamTypes())) {
				String n = m.getSimpleName();
				if (isOneOf(n, "create","getInstance"))
					return m.invoke(null, getParams(m.getParamTypes()));
			}
		}

		if (ci.isInterface())
			throw new ExecutableException("Could not instantiate class {0} because it is an interface.", c.getName());
		if (ci.isAbstract())
			throw new ExecutableException("Could not instantiate class {0} because it is abstract.", c.getName());

		for (ConstructorInfo cc : ci.getPublicConstructors())
			if (hasAllParamTypes(cc.getParamTypes()))
				return cc.invoke(getParams(cc.getParamTypes()));

		throw new ExecutableException("Could not instantiate class {0}.  Constructor or creator not found.", c.getName());
	}

	private boolean hasAllParamTypes(List<ClassInfo> paramTypes) {
		for (ClassInfo ci : paramTypes)
			if (! hasBean(ci.inner()))
				if (outer == null || ! ci.inner().isInstance(outer))
					return false;
		return true;
	}

	private Object[] getParams(List<ClassInfo> paramTypes) {
		Object[] o = new Object[paramTypes.size()];
		for (int i = 0; i < paramTypes.size(); i++) {
			ClassInfo pt = paramTypes.get(i);
			if (i == 0 && pt.inner().isInstance(outer))
				o[i] = outer;
			else
				o[i] = getBean(paramTypes.get(i).inner());
		}
		return o;
	}
}
