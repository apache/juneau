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
package org.apache.juneau.utils;

import static java.util.Optional.*;

import java.util.*;

/**
 * Represents a holder for a bean or bean type.
 * 
 * @param <T> The bean type.
 */
public class BeanRef<T> {

	private T value;
	private Class<? extends T> type;

	/**
	 * Creator.
	 *
	 * @param type The bean type.
	 * @return A new object.
	 */
	public static <T> BeanRef<T> of(Class<T> type) {
		return new BeanRef<>();
	}

	/**
	 * Sets the bean on this reference.
	 *
	 * @param value The bean.
	 * @return This object (for method chaining).
	 */
	public BeanRef<T> value(T value) {
		this.value = value;
		return this;
	}

	/**
	 * Sets the bean type on this reference.
	 *
	 * @param value The bean type.
	 * @return This object (for method chaining).
	 */
	public BeanRef<T> type(Class<? extends T> value) {
		this.type = value;
		return this;
	}

	/**
	 * Returns the bean on this reference if the reference contains an instantiated bean.
	 *
	 * @return The bean on this reference if the reference contains an instantiated bean.
	 */
	public Optional<T> value() {
		return ofNullable(value);
	}

	/**
	 * Returns the bean type on this reference if the reference contains a bean type.
	 *
	 * @return The bean type on this reference if the reference contains a bean type.
	 */
	public Optional<Class<? extends T>> type() {
		return ofNullable(type);
	}
}
