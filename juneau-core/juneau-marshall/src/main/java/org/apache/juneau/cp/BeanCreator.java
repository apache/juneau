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

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;

/**
 * Utility class for creating beans through constructors, creator methods, and builders.
 *
 * <p>
 * Uses a {@link BeanStore} to find available ways to construct beans via injection of beans from the store.
 *
 * <p>
 * This class is instantiated through the following method:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link BeanStore}
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link BeanStore#createBean(Class)}
 * 		</ul>
 * 	</li>
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Construct and throw a RuntimeException using a bean store.</jc>
 * 	<jk>throw</jk> BeanStore
 * 		.<jsm>create</jsm>()
 * 		.build()
 * 		.addBean(Throwable.<jk>class</jk>, <jv>cause</jv>)
 * 		.addBean(String.<jk>class</jk>, <jv>msg</jv>)
 * 		.addBean(Object[].<jk>class</jk>, <jv>args</jv>)
 * 		.createBean(RuntimeException.<jk>class</jk>)
 * 		.run();
 * </p>
 *
 * <p>
 * Looks for the following methods for creating a bean:
 * <ol class='spaced-list'>
 * 	<li>Looks for a singleton no-arg method of the form:
 * 		<p class='bcode w800'>
 * 	<jk>public static</jk> MyClass <jsm>getInstance</jsm>();
 * 		</p>
 * 		<ul>
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 * 			<li>Enabled by {@linnk #findSingleton()}.
 *		</ul>
 * 	<li>Looks for a static creator method of the form:
 * 		<p class='bcode w800'>
 * 	<jk>public static</jk> MyClass <jsm>create</jsm>(<ja>&lt;args&gt;</ja>);
 * 		</p>
 * 		<ul>
 * 			<li>All arguments except {@link Optional} and {@link List} parameters must have beans available in the store.
 * 			<li>If multiple methods are found, the one with the most matching parameters is used.
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 * 		</ul>
 * 	<li>Looks for a public constructor of the form:
 * 		<p class='bcode w800'>
 * 	<jk>public</jk> MyClass(<ja>&lt;args&gt;</ja>);
 * 		</p>
 * 		<ul>
 * 			<li>All arguments except {@link Optional} and {@link List} parameters must have beans available in the store.
 * 			<li>If multiple methods are found, the one with the most matching parameters is used.
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * 	<li>Looks for a protected constructor of the form:
 * 		<p class='bcode w800'>
 * 	<jk>protected</jk> MyClass(<ja>&lt;args&gt;</ja>);
 * 		</p>
 * 		<ul>
 * 			<li>All arguments except {@link Optional} and {@link List} parameters must have beans available in the store.
 * 			<li>If multiple methods are found, the one with the most matching parameters is used.
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * 	<li>Looks for a static no-arg create method that returns a builder object that can be passed in to a protected constructor.
 * 		<p class='bcode w800'>
 * 	<jk>public static</jk> MyClass.Builder <jsm>create</jsm>();
 *
 * 	<jk>protected</jk> MyClass(MyClass.Builder <jv>builder</jv>);
 * 		</p>
 * 		<ul>
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * </ol>
 *
 * <ul class='spaced-list'>
 * 	<li class='note'>The {@link #builder(Class,Object)} method can be used to set an existing initialized builder object to pass to a constructor.
 * 	<li class='note'>An existing initialized builder can be set using the {@link #builder(Class,Object)} method.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='jc'>{@link BeanStore}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The bean type being created.
 */
public class BeanCreator<T> {

	private final BeanStore store;
	private ClassInfo type;
	private Object builder;
	private T impl;

	/**
	 * Constructor.
	 *
	 * @param type The bean type being created.
	 * @param store The bean store creating this creator.
	 */
	protected BeanCreator(Class<T> type, BeanStore store) {
		this.type = ClassInfo.ofc(type);
		this.store = BeanStore.of(store, store.outer.orElse(null));
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
	 * Specifies a builder object for the bean type.
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>When specified, we don't look for a static creator method.
	 * </ul>
	 *
	 * @param type The class type of the builder.
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public <B> BeanCreator<T> builder(Class<B> type, B value) {
		builder = value;
		Class<?> t = value.getClass();
		do {
			store.add((Class<T>)t, (T)value);
			t = t.getSuperclass();
		} while(t != null && ! t.equals(type));
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

		// Look for getInstance(Builder).
		if (builder != null) {
			MethodInfo m = type.getPublicMethod(
				x -> x.isStatic()
				&& x.isNotDeprecated()
				&& x.getParamCount() == 1
				&& x.getParam(0).canAccept(builder)
				&& x.hasReturnType(type)
				&& x.hasNoAnnotation(BeanIgnore.class)
				&& x.hasName("getInstance")
			);
			if (m != null)
				return m.invoke(null, builder);
		}

		// Look for getInstance().
		if (builder == null) {
			MethodInfo m = type.getPublicMethod(
				x -> x.isStatic()
				&& x.isNotDeprecated()
				&& x.getParamCount() == 0
				&& x.hasReturnType(type)
				&& x.hasNoAnnotation(BeanIgnore.class)
				&& x.hasName("getInstance")
			);
			if (m != null)
				return m.invoke(null);
		}

		if (builder == null) {
			// Look for static creator methods.

			MethodInfo matchedCreator = null;
			int matchedCreatorParams = -1;

			// Look for static creator method.
			for (MethodInfo m : type.getPublicMethods()) {
				if (isStaticCreateMethod(m)) {
					found = "STATIC_CREATOR";
					if (store.hasAllParams(m)) {
						if (m.getParamCount() > matchedCreatorParams) {
							matchedCreatorParams = m.getParamCount();
							matchedCreator = m;
						}
					}
				}
			}

			if (matchedCreator != null)
				return matchedCreator.invoke(null, store.getParams(matchedCreator));
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
			if (store.hasAllParams(cc)) {
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
					if (store.hasAllParams(cc)) {
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
			return matchedConstructor.invoke(store.getParams(matchedConstructor));

		if (builder == null) {
			// Look for static-builder/protected-constructor pair.
			for (ConstructorInfo cc2 : type.getDeclaredConstructors()) {
				if (cc2.getParamCount() == 1 && cc2.isVisible(Visibility.PROTECTED)) {
					Class<?> pt = cc2.getParam(0).getParameterType().inner();
					MethodInfo m = type.getPublicMethod(x -> isStaticCreateMethod(x, pt));
					if (m != null) {
						Object builder = m.invoke(null);
						return cc2.accessible().invoke(builder);
					}
				}
			}
		}

		String msg = null;
		if (found == null) {
			msg = "No public/protected constructors found";
		} else if (found.equals("STATIC_CREATOR")) {

			msg = "Static creator found but could not find prerequisites: " + type.getPublicMethods().stream().filter(x -> isStaticCreateMethod(x)).map(x -> store.getMissingParams(x)).sorted().collect(joining(" or "));
		} else if (found.equals("PUBLIC_CONSTRUCTOR")) {
			msg = "Public constructor found but could not find prerequisites: " + type.getPublicConstructors().stream().map(x -> store.getMissingParams(x)).sorted().collect(joining(" or "));
		} else {
			msg = "Protected constructor found but could not find prerequisites: " + type.getDeclaredConstructors().stream().filter(x -> x.isProtected()).map(x -> store.getMissingParams(x)).sorted().collect(joining(" or "));
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

	private boolean isStaticCreateMethod(MethodInfo m) {
		return isStaticCreateMethod(m, type.inner());
	}

	private boolean isStaticCreateMethod(MethodInfo m, Class<?> type) {
		return m.isAll(STATIC, NOT_DEPRECATED)
			&& m.hasReturnType(type)
			&& m.hasNoAnnotation(BeanIgnore.class)
			&& m.hasName("create");
	}
}
