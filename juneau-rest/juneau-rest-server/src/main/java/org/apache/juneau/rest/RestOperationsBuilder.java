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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.exception.*;

/**
 * Builder for {@link RestOperations} object.
 */
public class RestOperationsBuilder  {

	TreeMap<String,TreeSet<RestOperationContext>> map = new TreeMap<>();
	Set<RestOperationContext> set = ASet.of();

	private BeanFactory beanFactory;
	private Class<? extends RestOperations> implClass;

	/**
	 * Instantiates a {@link RestOperations} object based on the contents of this builder.
	 *
	 * @return A new {@link RestOperations} object.
	 */
	public RestOperations build() {
		try {
			Class<? extends RestOperations> ic = firstNonNull(implClass, RestOperations.class);
			return BeanFactory.of(beanFactory).addBean(RestOperationsBuilder.class, this).createBean(ic);
		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * Adds a method context to this builder.
	 *
	 * @param mc The REST method context to add.
	 * @return Adds a method context to this builder.
	 */
	public RestOperationsBuilder add(RestOperationContext mc) {
		return add(mc.getHttpMethod(), mc);
	}

	/**
	 * Adds a method context to this builder.
	 *
	 * @param httpMethodName The HTTP method name.
	 * @param mc The REST method context to add.
	 * @return Adds a method context to this builder.
	 */
	public RestOperationsBuilder add(String httpMethodName, RestOperationContext mc) {
		httpMethodName = httpMethodName.toUpperCase();
		if (! map.containsKey(httpMethodName))
			map.put(httpMethodName, new TreeSet<>());
		map.get(httpMethodName).add(mc);
		set.add(mc);
		return this;
	}

	/**
	 * Specifies a {@link RestOperations} implementation subclass to use.
	 *
	 * <p>
	 * When specified, the {@link #build()} method will create an instance of that class instead of the default {@link RestOperations}.
	 *
	 * <p>
	 * The subclass must have a public constructor that takes in any of the following arguments:
	 * <ul>
	 * 	<li>{@link RestOperationsBuilder} - This object.
	 * 	<li>Any beans found in the specified {@link #beanFactory(BeanFactory) bean factory}.
	 * 	<li>Any {@link Optional} beans that may or may not be found in the specified {@link #beanFactory(BeanFactory) bean factory}.
	 * </ul>
	 *
	 * @param implClass The implementation class to build.
	 * @return This object (for method chaining).
	 */
	public RestOperationsBuilder implClass(Class<? extends RestOperations> implClass) {
		this.implClass = implClass;
		return this;
	}

	/**
	 * Specifies a {@link BeanFactory} to use when resolving constructor arguments.
	 *
	 * @param beanFactory The bean factory to use for resolving constructor arguments.
	 * @return This object (for method chaining).
	 */
	public RestOperationsBuilder beanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		return this;
	}
}