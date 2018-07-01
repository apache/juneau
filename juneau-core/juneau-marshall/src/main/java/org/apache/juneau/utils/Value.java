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

/**
 * Represents a simple settable value.
 *
 * <p>
 * This object is not thread safe.
 *
 * @param <T> The value type.
 */
public class Value<T> {

	private T t;
	private ValueListener<T> listener;

	/**
	 * Constructor.
	 */
	public Value() {
	}

	/**
	 * Constructor.
	 *
	 * @param t Initial value.
	 */
	public Value(T t) {
		set(t);
	}

	/**
	 * Adds a listener for this value.
	 *
	 * @param listener The new listener for this value.
	 * @return This object (for method chaining).
	 */
	public Value<T> listener(ValueListener<T> listener) {
		this.listener = listener;
		return this;
	}

	/**
	 * Sets the value.
	 *
	 * @param t The new value.
	 * @return This object (for method chaining).
	 */
	public Value<T> set(T t) {
		this.t = t;
		if (listener != null)
			listener.onSet(t);
		return this;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value, or <jk>null</jk> if it is not set.
	 */
	public T get() {
		return t;
	}

	/**
	 * Returns <jk>true</jk> if the value is set.
	 *
	 * @return <jk>true</jk> if the value is set.
	 */
	public boolean isSet() {
		return get() != null;
	}
}
