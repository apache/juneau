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

import java.util.function.*;

/**
 * Extends {@link BeanStore} with methods for adding beans.
 *
 * <p>
 * This interface provides write operations for bean stores, allowing beans to be added
 * either as direct instances or as suppliers for lazy initialization.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	WritableBeanStore <jv>beanStore</jv> = ...;
 *
 * 	<jc>// Add unnamed bean</jc>
 * 	<jv>beanStore</jv>.addBean(MyService.<jk>class</jk>, <jk>new</jk> MyService());
 *
 * 	<jc>// Add named bean</jc>
 * 	<jv>beanStore</jv>.addBean(MyService.<jk>class</jk>, <jk>new</jk> MyService(), <js>"primary"</js>);
 *
 * 	<jc>// Add bean supplier for lazy initialization</jc>
 * 	<jv>beanStore</jv>.addSupplier(MyService.<jk>class</jk>, () -&gt; <jk>new</jk> MyService());
 *
 * 	<jc>// Add named bean supplier</jc>
 * 	<jv>beanStore</jv>.addSupplier(MyService.<jk>class</jk>, () -&gt; <jk>new</jk> MyService(), <js>"secondary"</js>);
 *
 * 	<jc>// Clear all beans</jc>
 * 	<jv>beanStore</jv>.clear();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanStore} - Read-only bean lookup interface
 * </ul>
 */
public interface WritableBeanStore extends BeanStore {

	/**
	 * Adds an unnamed bean of the specified type to this store.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param bean The bean instance.  Can be <jk>null</jk>.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addBean(Class<T> beanType, T bean);

	/**
	 * Adds a named bean of the specified type to this store.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param bean The bean instance.  Can be <jk>null</jk>.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addBean(Class<T> beanType, T bean, String name);

	/**
	 * Adds a supplier for an unnamed bean of the specified type to this store.
	 *
	 * <p>
	 * The supplier will be invoked lazily when the bean is first requested.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param supplier The bean supplier.  Must not be <jk>null</jk>.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addSupplier(Class<T> beanType, Supplier<T> supplier);

	/**
	 * Adds a supplier for a named bean of the specified type to this store.
	 *
	 * <p>
	 * The supplier will be invoked lazily when the bean is first requested.
	 *
	 * @param <T> The bean type.
	 * @param beanType The bean type.
	 * @param supplier The bean supplier.  Must not be <jk>null</jk>.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return This object for method chaining.
	 */
	<T> WritableBeanStore addSupplier(Class<T> beanType, Supplier<T> supplier, String name);

	/**
	 * Removes all beans from this store.
	 *
	 * <p>
	 * This operation only affects this store and does not affect any parent stores
	 * that may be associated with this store.
	 *
	 * @return This object for method chaining.
	 */
	WritableBeanStore clear();
}

