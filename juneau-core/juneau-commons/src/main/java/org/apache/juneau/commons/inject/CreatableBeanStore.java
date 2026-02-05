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
package org.apache.juneau.commons.inject;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * A bean store that provides convenient access to {@link BeanCreator2} instances for creating beans.
 *
 * <p>
 * This class extends {@link BasicBeanStore2} and adds methods to manage {@link BeanCreator2} instances
 * for different bean types. Creators are lazily created and cached for efficient reuse.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a creatable bean store</jc>
 * 	CreatableBeanStore <jv>store</jv> = <jk>new</jk> CreatableBeanStore(<jk>null</jk>);
 *
 * 	<jc>// Get or create a creator for MyBean</jc>
 * 	BeanCreator2&lt;MyBean&gt; <jv>creator</jv> = <jv>store</jv>.getCreator(MyBean.<jk>class</jk>);
 *
 * 	<jc>// Use the creator to create a bean</jc>
 * 	MyBean <jv>bean</jv> = <jv>creator</jv>.create();
 *
 * 	<jc>// Explicitly add a creator (optional, getCreator will create if missing)</jc>
 * 	<jv>store</jv>.addCreator(MyOtherBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicBeanStore2}
 * 	<li class='jc'>{@link BeanCreator2}
 * </ul>
 */
@SuppressWarnings("java:S115")
public class CreatableBeanStore extends BasicBeanStore2 {

	// Argument name constants for assertArgNotNull
	private static final String ARG_beanType = "beanType";

	private final ConcurrentHashMap<Class<?>, BeanCreator2<?>> creators = new ConcurrentHashMap<>();
	private final Object enclosingInstance;

	/**
	 * Constructor.
	 *
	 * @param parent The parent bean store.  Can be <jk>null</jk>.  Bean searches are performed recursively up this parent chain.
	 * @param enclosingInstance The enclosing instance object to use when instantiating inner classes. Can be <jk>null</jk>.
	 */
	public CreatableBeanStore(BeanStore parent, Object enclosingInstance) {
		super(parent);
		this.enclosingInstance = enclosingInstance;
	}

	/**
	 * Same as {@link #addCreator(Class)} but returns the creator instead of this object for fluent calls.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to create a creator for. Cannot be <jk>null</jk>.
	 * @return The creator that was created and stored.
	 */
	public <T> BeanCreator2<T> add(Class<T> beanType) {
		assertArgNotNull(ARG_beanType, beanType);
		var creator = BeanCreator2.of(beanType, this, null, enclosingInstance);
		creators.put(beanType, creator);
		return creator;
	}

	/**
	 * Same as {@link #addCreator(Class, String)} but returns the creator instead of this object for fluent calls.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to create a creator for. Cannot be <jk>null</jk>.
	 * @param name The bean name. Can be <jk>null</jk>.
	 * @return The creator that was created and stored.
	 */
	public <T> BeanCreator2<T> add(Class<T> beanType, String name) {
		assertArgNotNull(ARG_beanType, beanType);
		var creator = BeanCreator2.of(beanType, this, name, enclosingInstance);
		creators.put(beanType, creator);
		return creator;
	}

	/**
	 * Creates and stores a {@link BeanCreator2} for the specified bean type.
	 *
	 * <p>
	 * If a creator for this type already exists, it is replaced with a new one.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create and store a creator</jc>
	 * 	<jv>store</jv>.addCreator(MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Get the creator</jc>
	 * 	BeanCreator2&lt;MyBean&gt; <jv>creator</jv> = <jv>store</jv>.getCreator(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to create a creator for. Cannot be <jk>null</jk>.
	 * @return This object.
	 */
	public <T> CreatableBeanStore addCreator(Class<T> beanType) {
		assertArgNotNull(ARG_beanType, beanType);
		var creator = BeanCreator2.of(beanType, this, null, enclosingInstance);
		creators.put(beanType, creator);
		return this;
	}

	/**
	 * Creates and stores a {@link BeanCreator2} for the specified bean type with a name.
	 *
	 * <p>
	 * If a creator for this type already exists, it is replaced with a new one.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create and store a creator with a name</jc>
	 * 	<jv>store</jv>.addCreator(MyBean.<jk>class</jk>, <js>"myBean"</js>);
	 *
	 * 	<jc>// Get the creator</jc>
	 * 	BeanCreator2&lt;MyBean&gt; <jv>creator</jv> = <jv>store</jv>.getCreator(MyBean.<jk>class</jk>, <js>"myBean"</js>);
	 * </p>
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to create a creator for. Cannot be <jk>null</jk>.
	 * @param name The bean name. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public <T> CreatableBeanStore addCreator(Class<T> beanType, String name) {
		assertArgNotNull(ARG_beanType, beanType);
		var creator = BeanCreator2.of(beanType, this, name, enclosingInstance);
		creators.put(beanType, creator);
		return this;
	}

	/**
	 * Returns the {@link BeanCreator2} for the specified bean type, creating it if it doesn't exist.
	 *
	 * <p>
	 * If a creator for this type doesn't exist, a new one is created with this bean store configured
	 * and stored for future use.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get or create a creator</jc>
	 * 	BeanCreator2&lt;MyBean&gt; <jv>creator</jv> = <jv>store</jv>.getCreator(MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Use the creator to create a bean</jc>
	 * 	MyBean <jv>bean</jv> = <jv>creator</jv>.create();
	 * </p>
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to get a creator for. Cannot be <jk>null</jk>.
	 * @return The creator for the specified bean type. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <T> BeanCreator2<T> getCreator(Class<T> beanType) {
		assertArgNotNull(ARG_beanType, beanType);
		return (BeanCreator2<T>)creators.computeIfAbsent(beanType, k -> BeanCreator2.of((Class<T>)k, this, null, enclosingInstance));
	}

	/**
	 * Returns the {@link BeanCreator2} for the specified bean type with a name, creating it if it doesn't exist.
	 *
	 * <p>
	 * If a creator for this type doesn't exist, a new one is created with this bean store configured
	 * and stored for future use.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get or create a creator with a name</jc>
	 * 	BeanCreator2&lt;MyBean&gt; <jv>creator</jv> = <jv>store</jv>.getCreator(MyBean.<jk>class</jk>, <js>"myBean"</js>);
	 *
	 * 	<jc>// Use the creator to create a bean</jc>
	 * 	MyBean <jv>bean</jv> = <jv>creator</jv>.create();
	 * </p>
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to get a creator for. Cannot be <jk>null</jk>.
	 * @param name The bean name. Can be <jk>null</jk>.
	 * @return The creator for the specified bean type. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <T> BeanCreator2<T> getCreator(Class<T> beanType, String name) {
		assertArgNotNull(ARG_beanType, beanType);
		return (BeanCreator2<T>)creators.computeIfAbsent(beanType, k -> BeanCreator2.of((Class<T>)k, this, name, enclosingInstance));
	}

	/**
	 * Resolves a bean supplier by checking creators for matching bean or builder types.
	 *
	 * <p>
	 * This method searches through all registered creators to find one that can create the requested bean type.
	 * It checks both the creator's bean type and any builder types associated with the creator.
	 *
	 * <p>
	 * If no matching creator is found, delegates to the parent store's resolve method.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type to resolve.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return The supplier, or {@link Optional#empty()} if no matching creator or supplier exists.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected <T> Optional<Supplier<T>> resolve(Class<T> beanType, String name) {
		// First check if there's a creator for the exact bean type
		var creator = creators.get(beanType);
		if (nn(creator)) {
			return opt(() -> (T)creator.run());
		}

		// Check all creators for matching bean types (for inheritance)
		for (var entry : creators.entrySet()) {
			var creatorBeanType = entry.getKey();
			var c = entry.getValue();

			// Check if creator's bean type is assignable to the requested type
			// (i.e., creator creates a subtype that can be cast to requested type)
			if (nn(creatorBeanType) && beanType.isAssignableFrom(creatorBeanType)) {
				return opt(() -> (T)c.run());
			}

			// Check builder types - if requested type matches a builder type, return the creator
			var builderTypes = safeOpt(c::getBuilderTypes);
			if (builderTypes.isPresent()) {
				for (var builderType : builderTypes.get()) {
					if (nn(builderType) && builderType.is(beanType)) {
						return opt(() -> (T)c.getBuilder());
					}
				}
			}
		}

		// If not found, delegate to parent
		return super.resolve(beanType, name);
	}

	@Override
	public CreatableBeanStore clear() {
		super.clear();
		creators.clear();
		return this;
	}
}
