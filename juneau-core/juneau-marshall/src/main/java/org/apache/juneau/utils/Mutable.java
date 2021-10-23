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

import java.util.*;

/**
 * Represents a settable object.
 *
 * Typically passed as method parameters to provide by-reference support.
 *
 * <p class='bcode w800'>
 * 	Mutable&lt;String&gt; m = Mutable.<jsm>create</jsm>(String.<jk>class</jk>);
 * 	callSomeMethodThatSetsValue(m);
 * 	String val = m.get();
 * </p>
 *
 * <ul class='notes'>
 * 	<li>
 * 		This class is not thread safe.
 * 	<li>
 * 		This object can be used as hashmap keys.
 * </ul>
 *
 * @param <T> The inner object type.
 */
public class Mutable<T> {

	private T value;

	/**
	 * Creates an empty mutable.
	 *
	 * @param <T> The inner object type.
	 * @param c The inner object type.
	 * @return The new mutable object.
	 */
	public static <T> Mutable<T> create(Class<T> c) {
		return new Mutable<>();
	}

	/**
	 * Creates an empty mutable.
	 *
	 * @param <T> The inner object type.
	 * @return The new mutable object.
	 */
	public static <T> Mutable<T> create() {
		return new Mutable<>();
	}

	/**
	 * Creates a mutable initialized with the specified object.
	 *
	 * @param <T> The inner object type.
	 * @param t The inner object.
	 * @return The new mutable object.
	 */
	public static <T> Mutable<T> of(T t) {
		return new Mutable<>(t);
	}

	/**
	 * Creates an empty mutable.
	 */
	public Mutable() {}

	/**
	 * Creates a mutable initialized with the specified object.
	 *
	 * @param t The inner object.
	 */
	public Mutable(T t) {
		this.value = t;
	}

	/**
	 * Returns the inner object.
	 *
	 * @return The inner object, or <jk>null</jk> if empty.
	 */
	public T get() {
		return value;
	}

	/**
	 * Sets the inner object.
	 *
	 * @param t The inner object.
	 * @return This object.
	 */
	public Mutable<T> set(T t) {
		this.value = t;
		return this;
	}

	/**
	 * Returns <jk>true</jk> if inner object is set.
	 *
	 * @return <jk>true</jk> if inner object is set.
	 */
	public boolean isSet() {
		return value != null;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return Objects.equals(o, value);
	}

	@Override /* Object */
	public int hashCode() {
		return value == null ? 0 : value.hashCode();
	}

	@Override /* Object */
	public String toString() {
		return value == null ? "null" : value.toString();
	}
}
