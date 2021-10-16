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

import static java.util.stream.Collectors.*;
import static org.apache.juneau.reflect.ReflectFlags.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.reflect.*;

/**
 * Utility class for creating beans.
 *
 * @param <T> The bean type being created.
 */
public class BeanCreator<T> {

	private ClassInfo type;
	private BeanStore store = BeanStore.INSTANCE;
	private Object outer;
	private Object builder;
	private boolean findSingleton;
	private T impl;

	/**
	 * Static creator.
	 *
	 * @param type The bean type being created.
	 * @return A new creator object.
	 */
	public static <T> BeanCreator<T> of(Class<T> type) {
		return new BeanCreator<>(type);
	}

	/**
	 * Constructor.
	 *
	 * @param type The bean type being created.
	 */
	protected BeanCreator(Class<T> type) {
		this.type = ClassInfo.ofc(type);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The creator being copied.
	 */
	protected BeanCreator(BeanCreator<T> copyFrom) {
		type = copyFrom.type;
		store = copyFrom.store;
		outer = copyFrom.outer;
		builder = copyFrom.builder;
		findSingleton = copyFrom.findSingleton;
		impl = copyFrom.impl;
	}

	/**
	 * Creates a copy of this bean creator.
	 *
	 * @return A copy of this bean creator.
	 */
	public BeanCreator<T> copy() {
		return new BeanCreator<>(this);
	}

	/**
	 * Allows you to specify a subclass of the specified bean type to create.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	public BeanCreator<T> type(Class<?> value) {
		if (value != null && ! type.inner().isAssignableFrom(value))
			throw new ExecutableException("Could not instantiate class of type {0} because it was not a subtype of the class: {1}.", type, value);
		type = ClassInfo.ofc(value);
		return this;
	}

	/**
	 * Allows you to specify a specific instance for the build method to return.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public BeanCreator<T> impl(Object value) {
		impl = (T)value;
		return this;
	}
	/**
	 * Specifies a bean store for looking up matching parameters on constructors and static creator methods.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	public BeanCreator<T> store(BeanStore value) {
		if (value != null)
			store = value;
		return this;
	}

	/**
	 * Specifies the outer "this" parameter to an inner class constructor.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	public BeanCreator<T> outer(Object value) {
		outer = value;
		return this;
	}

	/**
	 * Specifies a builder object for the bean type.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	public BeanCreator<T> builder(Object value) {
		builder = value;
		return this;
	}

	/**
	 * Specifies to look for the existence of a <c>getInstance()</c> method that returns a singleton.
	 *
	 * @return This object.
	 */
	public BeanCreator<T> findSingleton() {
		findSingleton = true;
		return this;
	}

	/**
	 * Creates the bean.
	 *
	 * @return A new bean.
	 */
	public T run() {

		if (impl != null)
			return impl;

		if (type == null)
			return null;

		String found = null;

		// Look for getInstance().
		if (builder == null || findSingleton)
			for (MethodInfo m : type.getPublicMethods())
				if (isSingletonMethod(m))
					return m.invoke(null);

		if (builder == null) {
			// Look for static creator methods.

			MethodInfo matchedCreator = null;
			int matchedCreatorParams = -1;

			// Look for static creator method.
			for (MethodInfo m : type.getPublicMethods()) {
				if (isStaticCreateMethod(m)) {
					found = "STATIC_CREATOR";
					if (hasAllParams(m.getParams())) {
						if (m.getParamCount() > matchedCreatorParams) {
							matchedCreatorParams = m.getParamCount();
							matchedCreator = m;
						}
					}
				}
			}

			if (matchedCreator != null)
				return matchedCreator.invoke(null, getParams(matchedCreator.getParams()));
		}

		if (type.isInterface())
			throw new ExecutableException("Could not instantiate class {0}: {1}.", type.getName(), "Class is an interface");

		if (type.isAbstract())
			throw new ExecutableException("Could not instantiate class {0}: {1}.", type.getName(), "Class is abstract");

		ConstructorInfo matchedConstructor = null;
		int matchedConstructorParams = -1;

		// Look for public constructor.
		for (ConstructorInfo cc : type.getPublicConstructors()) {
			if (found == null)
				found = "PUBLIC_CONSTRUCTOR";
			if (hasAllParams(cc.getParams())) {
				if (cc.getParamCount() > matchedConstructorParams) {
					matchedConstructorParams = cc.getParamCount();
					matchedConstructor = cc;
				}
			}
		}

		// Look for protected constructor.
		if (matchedConstructor == null) {
			for (ConstructorInfo cc : type.getDeclaredConstructors()) {
				if (cc.isProtected()) {
					if (found == null)
						found = "PROTECTED_CONSTRUCTOR";
					if (hasAllParams(cc.getParams())) {
						if (cc.getParamCount() > matchedConstructorParams) {
							matchedConstructorParams = cc.getParamCount();
							matchedConstructor = cc.accessible();
						}
					}
				}
			}
		}

		// Execute.
		if (matchedConstructor != null)
			return matchedConstructor.invoke(getParams(matchedConstructor.getParams()));

		if (builder == null) {
			// Look for static-builder/protected-constructor pair.
			for (ConstructorInfo cc2 : type.getDeclaredConstructors()) {
				if (cc2.getParamCount() == 1 && cc2.isVisible(Visibility.PROTECTED)) {
					Class<?> pt = cc2.getParam(0).getParameterType().inner();
					for (MethodInfo m : type.getPublicMethods()) {
						if (isStaticCreateMethod(m, pt)) {
							Object builder = m.invoke(null);
							return cc2.accessible().invoke(builder);
						}
					}
				}
			}
		}

		String msg = null;
		if (found == null) {
			msg = "No public/protected constructors found";
		} else if (found.equals("STATIC_CREATOR")) {
			msg = "Static creator found but could not find prerequisites: " + type.getPublicMethods().stream().filter(x -> isStaticCreateMethod(x)).map(x -> getMissingParams(x.getParams())).sorted().collect(joining(" or "));
		} else if (found.equals("PUBLIC_CONSTRUCTOR")) {
			msg = "Public constructor found but could not find prerequisites: " + type.getPublicConstructors().stream().map(x -> getMissingParams(x.getParams())).sorted().collect(joining(" or "));
		} else {
			msg = "Protected constructor found but could not find prerequisites: " + type.getDeclaredConstructors().stream().filter(x -> x.isProtected()).map(x -> getMissingParams(x.getParams())).sorted().collect(joining(" or "));
		}
		throw new ExecutableException("Could not instantiate class {0}: {1}.", type.getName(), msg);
	}

	/**
	 * Converts this creator into a supplier.
	 *
	 * @return A supplier that returns the results of the {@link #run()} method.
	 */
	public Supplier<T> supplier() {
		return ()->run();
	}

	private boolean hasAllParams(List<ParamInfo> params) {
		loop: for (int i = 0; i < params.size(); i++) {
			ParamInfo pi = params.get(i);
			ClassInfo pt = pi.getParameterType();
			ClassInfo ptu = pt.unwrap(Optional.class);
			if (i == 0 && ptu.isInstance(outer))
				continue loop;
			if (pt.is(Optional.class))
				continue loop;
			if (ptu.isInstance(builder))
					continue loop;
			String beanName = findBeanName(pi);
			if (beanName == null)
				beanName = ptu.inner().getName();
			if (! store.hasBean(beanName))
				return false;
		}
		return true;
	}

	private boolean isStaticCreateMethod(MethodInfo m) {
		return isStaticCreateMethod(m, type.inner());
	}

	private boolean isStaticCreateMethod(MethodInfo m, Class<?> type) {
		return m.isAll(STATIC, NOT_DEPRECATED)
			&& m.hasReturnType(type)
			&& (!m.hasAnnotation(BeanIgnore.class))
			&& m.getSimpleName().equals("create");
	}

	private boolean isSingletonMethod(MethodInfo m) {
		return m.isAll(STATIC, NOT_DEPRECATED)
			&& m.getParamCount() == 0
			&& m.hasReturnType(type)
			&& (!m.hasAnnotation(BeanIgnore.class))
			&& m.getSimpleName().equals("getInstance");
	}

	private Object[] getParams(List<ParamInfo> params) {
		Object[] o = new Object[params.size()];
		for (int i = 0; i < params.size(); i++) {
			ParamInfo pi = params.get(i);
			ClassInfo pt = pi.getParameterType();
			ClassInfo ptu = pt.unwrap(Optional.class);
			if (i == 0 && ptu.isInstance(outer))
				o[i] = outer;
			else {
				if (pt.isInstance(builder))
					o[i] = builder;
				else {
					String beanName = findBeanName(pi);
					if (pt.is(Optional.class)) {
						o[i] = store.getBean(beanName, ptu.inner());
					} else {
						o[i] = store.getBean(beanName, ptu.inner()).get();
					}
				}
			}
		}
		return o;
	}

	private String getMissingParams(List<ParamInfo> params) {
		List<String> l = AList.create();
		loop: for (int i = 0; i < params.size(); i++) {
			ParamInfo pi = params.get(i);
			ClassInfo pt = pi.getParameterType();
			ClassInfo ptu = pt.unwrap(Optional.class);
			if (i == 0 && ptu.isInstance(outer))
				continue loop;
			if (pt.is(Optional.class))
				continue loop;
			String beanName = findBeanName(pi);
			if (beanName != null) {
				if (! store.hasBean(beanName))
					l.add(beanName);
			} else {
				if (! store.hasBean(pt.inner()))
					l.add(pt.inner().getSimpleName());
			}
		}
		return l.stream().sorted().collect(joining(","));
	}

	private String findBeanName(ParamInfo pi) {
		Optional<Annotation> namedAnnotation = pi.getAnnotations(Annotation.class).stream().filter(x->x.annotationType().getSimpleName().equals("Named")).findFirst();
		if (namedAnnotation.isPresent())
			return AnnotationInfo.of((ClassInfo)null, namedAnnotation.get()).getValue(String.class, "value").orElse(null);
		return null;
	}
}
