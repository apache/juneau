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
package org.apache.juneau.internal;

/**
 * A simple settable integer value.
 */
public final class IntValue {

	private int value;

	/**
	 * Creates a new integer value initialized to <code>0</code>.
	 *
	 * @return A new integer value.
	 */
	public static IntValue create() {
		return of(0);
	}

	/**
	 * Creates an integer value with the specified initial state.
	 *
	 * @param value The initial state of the value.
	 * @return A new integer value.
	 */
	public static IntValue of(int value) {
		return new IntValue(value);
	}

	private IntValue(int value) {
		this.value = value;
	}

	/**
	 * Sets the value.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public IntValue set(int value) {
		this.value = value;
		return this;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public int get() {
		return value;
	}

	/**
	 * Returns the current value and then increments it.
	 *
	 * @return The current value.
	 */
	public int getAndIncrement() {
		int v = value;
		value++;
		return v;
	}
}
