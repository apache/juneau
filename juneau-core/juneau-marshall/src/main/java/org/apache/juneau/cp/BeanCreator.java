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
import static org.apache.juneau.Visibility.*;
import static org.apache.juneau.internal.CollectionUtils.*;

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
 * <p class='bjava'>
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
 * 		<p class='bjava'>
 * 	<jk>public static</jk> MyClass <jsm>getInstance</jsm>();
 * 		</p>
 * 		<ul>
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * 	<li>Looks for a static creator method of the form:
 * 		<p class='bjava'>
 * 	<jk>public static</jk> MyClass <jsm>create</jsm>(<ja>&lt;args&gt;</ja>);
 * 		</p>
 * 		<ul>
 * 			<li>All arguments except {@link Optional} and {@link List} parameters must have beans available in the store.
 * 			<li>If multiple methods are found, the one with the most matching parameters is used.
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 * 		</ul>
 * 	<li>Looks for a public constructor of the form:
 * 		<p class='bjava'>
 * 	<jk>public</jk> MyClass(<ja>&lt;args&gt;</ja>);
 * 		</p>
 * 		<ul>
 * 			<li>All arguments except {@link Optional} and {@link List} parameters must have beans available in the store.
 * 			<li>If multiple methods are found, the one with the most matching parameters is used.
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * 	<li>Looks for a protected constructor of the form:
 * 		<p class='bjava'>
 * 	<jk>protected</jk> MyClass(<ja>&lt;args&gt;</ja>);
 * 		</p>
 * 		<ul>
 * 			<li>All arguments except {@link Optional} and {@link List} parameters must have beans available in the store.
 * 			<li>If multiple methods are found, the one with the most matching parameters is used.
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * 	<li>Looks for a static no-arg create method that returns a builder object that can be passed in to a protected constructor.
 * 		<p class='bjava'>
 * 	<jk>public static</jk> MyClass.Builder <jsm>create</jsm>();
 *
 * 	<jk>protected</jk> MyClass(MyClass.Builder <jv>builder</jv>);
 * 		</p>
 * 		<ul>
 * 			<li>Deprecated and {@link BeanIgnore @BeanIgnore-annotated} methods are ignored.
 *		</ul>
 * </ol>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>The {@link #builder(Class,Object)} method can be used to set an existing initialized builder object to pass to a constructor.
 * 	<li class='note'>An existing initialized builder can be set using the {@link #builder(Class,Object)} method.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanStore}
 * </ul>
 *
 * @param <T> The bean type being created.
 */
public class BeanCreator<T> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Shortcut for calling <c>BeanStore.INSTANCE.createBean(beanType)</c>.
	 *
	 * @param <T> The bean type to create.
	 * @param beanType The bean type to create.
	 * @return A new creator.
	 */
	public static <T> BeanCreator<T> of(Class<T> beanType) {
		return BeanStore.INSTANCE.createBean(beanType);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final BeanStore store;
	private ClassInfo type;
	private Object builder;
	private T impl;
	private boolean silent;

	/**
	 * Constructor.
	 *
	 * @param type The bean type being created.
	 * @param store The bean store creating this creator.
	 */
	protected BeanCreator(Class<T> type, BeanStore store) {
		this.type = ClassInfo.of(type);
		this.store = BeanStore.of(store, store.outer.orElse(null));
	}

	/**
	 * Allows you to specify a subclass of the specified bean type to create.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	public BeanCreator<T> type(Class<?> value) {
		type = ClassInfo.of(value);
		return this;
	}

	/**
	 * Allows you to specify a subclass of the specified bean type to create.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	public BeanCreator<T> type(ClassInfo value) {
		return type(value == null ? null : value.inner());
	}

	/**
	 * Allows you to specify a specific instance for the build method to return.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	public BeanCreator<T> impl(T value) {
		impl = value;
		return this;
	}

	/**
	 * Adds an argument to this creator.
	 *
	 * @param <T2> The parameter type.
	 * @param beanType The parameter type.
	 * @param bean The parameter value.
	 * @return This object.
	 */
	public <T2> BeanCreator<T> arg(Class<T2> beanType, T2 bean) {
		store.add(beanType, bean);
		return this;
	}

	/**
	 * Suppresses throwing of {@link ExecutableException ExecutableExceptions} from the {@link #run()} method when
	 * a form of creation cannot be found.
	 *
	 * @return This object.
	 */
	public BeanCreator<T> silent() {
		silent = true;
		return this;
	}

	/**
	 * Specifies a builder object for the bean type.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>When specified, we don't look for a static creator method.
	 * </ul>
	 *
	 * @param <B> The class type of the builder.
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
	 * Same as {@link #run()} but returns the alternate value if a method of creation could not be found.
	 *
	 * @param other The other bean to use.
	 * @return Either the created or other bean.
	 */
	public T orElse(T other) {
		return execute().orElse(other);
	}

	/**
	 * Same as {@link #run()} but returns the value wrapped in an {@link Optional}.
	 *
	 * @return A new bean wrapped in an {@link Optional}.
	 */
	public Optional<T> execute() {
		return optional(silent().run());
	}

	/**
	 * Creates the bean.
	 *
	 * @return A new bean.
	 * @throws ExecutableException if bean could not be created and {@link #silent()} was not enabled.
	 */
	public T run() {

		if (impl != null)
			return impl;

		if (type == null)
			return null;

		Value<String> found = Value.empty();

		// Look for getInstance(Builder).
		if (builder != null) {
			MethodInfo m = type.getPublicMethod(
				x -> x.isStatic()
				&& x.isNotDeprecated()
				&& x.hasNumParams(1)
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
				&& x.hasNoParams()
				&& x.hasReturnType(type)
				&& x.hasNoAnnotation(BeanIgnore.class)
				&& x.hasName("getInstance")
			);
			if (m != null)
				return m.invoke(null);
		}

		if (builder == null) {
			// Look for static creator methods.

			Match<MethodInfo> match = new Match<>();

			// Look for static creator method.
			type.forEachPublicMethod(x -> isStaticCreateMethod(x), x -> {
				found.set("STATIC_CREATOR");
				if (hasAllParams(x))
					match.add(x);
			});

			if (match.isPresent())
				return match.get().invoke(null, getParams(match.get()));
		}

		if (type.isInterface()) {
			if (silent)
				return null;
			throw new ExecutableException("Could not instantiate class {0}: {1}.", type.getName(), "Class is an interface");
		}

		if (type.isAbstract()) {
			if (silent)
				return null;
			throw new ExecutableException("Could not instantiate class {0}: {1}.", type.getName(), "Class is abstract");
		}

		// Look for public constructor.
		Match<ConstructorInfo> constructorMatch = new Match<>();
		type.forEachPublicConstructor(x -> true, x -> {
			found.setIfEmpty("PUBLIC_CONSTRUCTOR");
			if (hasAllParams(x))
				constructorMatch.add(x);
		});

		// Look for protected constructor.
		if (! constructorMatch.isPresent()) {
			type.forEachDeclaredConstructor(x -> x.isProtected(), x -> {
				found.setIfEmpty("PROTECTED_CONSTRUCTOR");
				if (hasAllParams(x))
					constructorMatch.add(x);
			});
		}

		// Execute.
		if (constructorMatch.isPresent())
			return constructorMatch.get().invoke(getParams(constructorMatch.get()));

		if (builder == null) {
			// Look for static-builder/protected-constructor pair.
			Value<T> value = Value.empty();
			type.forEachDeclaredConstructor(x -> x.hasNumParams(1) && x.isVisible(PROTECTED), x -> {
				Class<?> pt = x.getParam(0).getParameterType().inner();
				MethodInfo m = type.getPublicMethod(y -> isStaticCreateMethod(y, pt));
				if (m != null) {
					Object builder = m.invoke(null);
					value.set(x.accessible().invoke(builder));
				}
			});
			if (value.isPresent())
				return value.get();
		}

		if (silent)
			return null;

		String msg = null;
		if (found.isEmpty()) {
			msg = "No public/protected constructors found";
		} else if (found.get().equals("STATIC_CREATOR")) {
			msg = "Static creator found but could not find prerequisites: " + type.getPublicMethods().stream().filter(x -> isStaticCreateMethod(x)).map(x -> getMissingParams(x)).sorted().collect(joining(" or "));
		} else if (found.get().equals("PUBLIC_CONSTRUCTOR")) {
			msg = "Public constructor found but could not find prerequisites: " + type.getPublicConstructors().stream().map(x -> getMissingParams(x)).sorted().collect(joining(" or "));
		} else {
			msg = "Protected constructor found but could not find prerequisites: " + type.getDeclaredConstructors().stream().filter(x -> x.isProtected()).map(x -> getMissingParams(x)).sorted().collect(joining(" or "));
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
		return m.isStatic()
			&& m.isNotDeprecated()
			&& m.hasReturnType(type)
			&& m.hasNoAnnotation(BeanIgnore.class)
			&& m.hasName("create");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	static class Match<T extends ExecutableInfo> {
		T executable = null;
		int numMatches = -1;

		@SuppressWarnings("unchecked")
		void add(T ei) {
			if (ei.getParamCount() > numMatches) {
				numMatches = ei.getParamCount();
				executable = (T)ei.accessible();
			}
		}

		boolean isPresent() {
			return executable != null;
		}

		T get() {
			return executable;
		}
	}

	private boolean hasAllParams(ExecutableInfo ei) {
		return store.hasAllParams(ei);
	}

	private Object[] getParams(ExecutableInfo ei) {
		return store.getParams(ei);
	}

	private String getMissingParams(ExecutableInfo ei) {
		return store.getMissingParams(ei);
	}
}
