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
package org.apache.juneau.commons.bean;

import java.lang.reflect.Type;
import java.util.Optional;

import org.apache.juneau.commons.reflect.ClassInfoTyped;
import org.apache.juneau.commons.reflect.ExecutableException;

/**
 * Bean-modeling SPI seam that exposes the type-classification surface the bean-runtime types
 * ({@link BeanMeta}-equivalents, {@link BeanPropertyMeta}-equivalents, etc.) need without coupling
 * the bean-modeling layer to the marshalling-side {@code ClassMeta}.
 *
 * <p>
 * Marshalling-side {@code ClassMeta&lt;T&gt;} extends this class; bean-modeling-side code only sees
 * {@link BeanInfo}.  The bean-modeling-side never instantiates this class directly — it always
 * gets instances handed in by the marshalling layer (e.g. via builder calls like
 * {@code BeanPropertyMeta.Builder#rawMetaType(ClassMeta)}).
 *
 * <p>
 * All extra abstract methods declared here mirror methods that {@code ClassMeta} already implements,
 * so {@code ClassMeta} satisfies this contract by virtue of its existing implementation.
 *
 * <h5 class='topic'>Thread safety</h5>
 * Thread safety depends on implementation.
 *
 * @param <T> The raw class type this instance represents.
 */
public abstract class BeanInfo<T> extends ClassInfoTyped<T> {

	/**
	 * Constructor.
	 *
	 * @param inner The class type.
	 */
	protected BeanInfo(Class<T> inner) {
		super(inner);
	}

	/**
	 * Constructor.
	 *
	 * @param inner The class type.
	 * @param innerType The generic type (if parameterized type).
	 */
	protected BeanInfo(Class<T> inner, Type innerType) {
		super(inner, innerType);
	}

	/**
	 * Returns <jk>true</jk> if this class is a URI/URL or annotated with a URI marker.
	 *
	 * @return <jk>true</jk> if this class is a URI.
	 */
	public abstract boolean isUri();

	/**
	 * Returns <jk>true</jk> if this class is classified as a bean.
	 *
	 * @return <jk>true</jk> if this class is a bean.
	 */
	public abstract boolean isBean();

	/**
	 * For array and {@code Collection} types, returns the type info of the element type, or <jk>null</jk>
	 * if this is not an array/collection.
	 *
	 * @return The element type info, or <jk>null</jk>.
	 */
	@SuppressWarnings("java:S1452") // Element type is heterogeneous; wildcard is fundamental to the SPI contract.
	public abstract BeanInfo<?> getElementType();

	/**
	 * For {@code Map} types, returns the type info of the key type, or <jk>null</jk> if this is not a map.
	 *
	 * @return The key type info, or <jk>null</jk>.
	 */
	@SuppressWarnings("java:S1452") // Key type is heterogeneous; wildcard is fundamental to the SPI contract.
	public abstract BeanInfo<?> getKeyType();

	/**
	 * For {@code Map} types, returns the type info of the value type, or <jk>null</jk> if this is not a map.
	 *
	 * @return The value type info, or <jk>null</jk>.
	 */
	@SuppressWarnings("java:S1452") // Value type is heterogeneous; wildcard is fundamental to the SPI contract.
	public abstract BeanInfo<?> getValueType();

	/**
	 * Returns <jk>true</jk> if this class can be instantiated using a no-arg constructor.
	 *
	 * @return <jk>true</jk> if a new instance can be created.
	 */
	public abstract boolean canCreateNewInstance();

	/**
	 * Returns <jk>true</jk> if this class can be instantiated, optionally with the specified outer object
	 * for non-static inner classes.
	 *
	 * @param outer The outer object instance, or <jk>null</jk> if not applicable.
	 * @return <jk>true</jk> if a new instance can be created.
	 */
	public abstract boolean canCreateNewInstance(Object outer);

	/**
	 * Creates a new instance of this class.
	 *
	 * @return A new instance of the class.
	 * @throws ExecutableException If the class could not be instantiated.
	 */
	@Override /* Overridden from ClassInfo to narrow return type to T. */
	public abstract T newInstance() throws ExecutableException;

	/**
	 * For {@link Optional} types, returns an empty optional default value, or <jk>null</jk>
	 * if this isn't an Optional.
	 *
	 * <p>
	 * Returns {@link Object} rather than {@code T} because the marshalling-side {@code ClassMeta} returns
	 * a wildcard {@code Optional<?>} which is not assignable to {@code T} under Java's type system.
	 *
	 * @return The default empty Optional value, or <jk>null</jk>.
	 */
	public abstract Object getOptionalDefault();

	/**
	 * Returns the {@link BeanConfigContext} associated with this type-info.
	 *
	 * <p>
	 * On the marshalling-side this returns the bean-modeling subset of the {@code MarshallingContext} that
	 * carried this type-info; on commons-side {@link BeanInfo} construction paths (none exist in-tree currently;
	 * {@link BeanInfo} is currently always realized by marshalling-side {@code ClassMeta}),
	 * this should return the {@code BeanConfigContext} the type-info was built against.
	 *
	 * @return The bean-modeling configuration carried alongside this type-info.  Never <jk>null</jk>.
	 */
	public abstract BeanConfigContext getBeanConfigContext();

	/**
	 * Returns the marshalling-side context associated with this type-info, as an opaque {@link Object}.
	 *
	 * <p>
	 * On the marshalling-side this is the {@code MarshallingContext} that carried this type-info; the return
	 * type is widened to {@link Object} so callers in {@code commons.bean} do not need to import
	 * {@code MarshallingContext}.  Callers cast to the marshalling-side type at the use site.
	 *
	 * @return The marshalling context, or <jk>null</jk> if none is associated with this type-info.
	 */
	public abstract Object getMarshallingContext();
}
