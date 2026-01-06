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

import java.util.*;
import java.util.function.Supplier;

/**
 * Spring-bean-like interface for looking up beans by type and name.
 *
 * <p>
 * This interface provides a minimal contract for bean lookup operations, similar to Spring's
 * {@link org.springframework.beans.factory.BeanFactory BeanFactory} interface.
 *
 * <p>
 * Implementations of this interface can be backed by various sources:
 * <ul>
 * 	<li>Spring's {@link org.springframework.context.ApplicationContext ApplicationContext}
 * 	<li>Custom in-memory stores
 * 	<li>Other dependency injection frameworks
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	BeanStore <jv>beanStore</jv> = ...;
 *
 * 	<jc>// Get bean by type</jc>
 * 	Optional&lt;MyService&gt; <jv>service</jv> = <jv>beanStore</jv>.getBean(MyService.<jk>class</jk>);
 *
 * 	<jc>// Get bean by name</jc>
 * 	Optional&lt;Object&gt; <jv>bean</jv> = <jv>beanStore</jv>.getBean(<js>"myBean"</js>);
 *
 * 	<jc>// Get bean by type and name</jc>
 * 	Optional&lt;MyService&gt; <jv>namedService</jv> = <jv>beanStore</jv>.getBean(MyService.<jk>class</jk>, <js>"primary"</js>);
 *
 * 	<jc>// Get all beans of a type</jc>
 * 	Map&lt;String, MyService&gt; <jv>allServices</jv> = <jv>beanStore</jv>.getBeansOfType(MyService.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://docs.spring.io/spring-framework/reference/core/beans.html">Spring Framework - The IoC Container</a>
 * </ul>
 */
public interface BeanStore {

	/**
	 * Returns a bean of the specified type.
	 *
	 * <p>
	 * If multiple beans of the specified type exist, the behavior is implementation-dependent.
	 * Typically, implementations will return the primary bean or throw an exception.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @return The bean, or {@link Optional#empty()} if no bean of the specified type exists.
	 */
	<T> Optional<T> getBean(Class<T> beanType);

	/**
	 * Returns a bean of the specified type and name.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return The bean, or {@link Optional#empty()} if no bean of the specified type and name exists.
	 */
	<T> Optional<T> getBean(Class<T> beanType, String name);

	/**
	 * Returns all beans of the specified type, keyed by bean name.
	 *
	 * <p>
	 * The map keys are the bean names, and the values are the bean instances.
	 * If no beans of the specified type exist, returns an empty map.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @return A map of bean names to bean instances.  Never <jk>null</jk>.
	 */
	<T> Map<String, T> getBeansOfType(Class<T> beanType);

	/**
	 * Returns <jk>true</jk> if this store contains at least one bean of the specified type.
	 *
	 * @param beanType The bean type.
	 * @return <jk>true</jk> if this store contains at least one bean of the specified type.
	 */
	boolean hasBean(Class<?> beanType);

	/**
	 * Returns <jk>true</jk> if this store contains a bean of the specified type and name.
	 *
	 * @param beanType The bean type.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if this store contains a bean of the specified type and name.
	 */
	boolean hasBean(Class<?> beanType, String name);

	/**
	 * Returns the supplier for an unnamed bean of the specified type.
	 *
	 * <p>
	 * If no supplier is found in this store, searches the parent store recursively.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @return The supplier, or {@link Optional#empty()} if no supplier of the specified type exists.
	 */
	<T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType);

	/**
	 * Returns the supplier for a named bean of the specified type.
	 *
	 * <p>
	 * If no supplier with the specified name is found in this store, searches the parent store recursively.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param name The bean name.  Can be <jk>null</jk>.
	 * @return The supplier, or {@link Optional#empty()} if no supplier of the specified type and name exists.
	 */
	<T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name);
}

