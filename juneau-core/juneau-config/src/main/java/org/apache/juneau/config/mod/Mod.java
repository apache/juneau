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
package org.apache.juneau.config.mod;

import java.util.function.*;

/**
 * Specifies an entry modifier that is used to encode during write and decode during read of config entries.
 */
public class Mod {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** A no-op modifier. */
	public static final Mod NO_OP = new Mod(' ', x -> x, x -> x, x -> true);

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final char id;
	private final Function<String,String> removeFunction, applyFunction;
	private final Function<String,Boolean> detectFunction;

	/**
	 * Constructor.
	 *
	 * @param id The character identifier.
	 * @param applyFunction
	 * 	The function to apply when writing an entry.
	 * 	Can be <jk>null</jk> if you override the {@link #apply(String)} method.
	 * @param removeFunction
	 * 	The function to apply when reading an entry.
	 * 	Can be <jk>null</jk> if you override the {@link #remove(String)} method.
	 * @param detectFunction
	 * 	The function to apply to detect whether the modification has been made.
	 * 	Can be <jk>null</jk> if you override the {@link #isApplied(String)} method.
	 */
	public Mod(char id, Function<String,String> applyFunction, Function<String,String> removeFunction, Function<String,Boolean> detectFunction) {
		this.id = id;
		this.applyFunction = applyFunction;
		this.removeFunction = removeFunction;
		this.detectFunction = detectFunction;
	}

	/**
	 * Returns the modifier identifier character.
	 *
	 * @return The modifier identifier character.
	 */
	public char getId() {
		return id;
	}

	/**
	 * Detects whether this modification has been applied.
	 *
	 * @param value The entry value being tested.  Will never be <jk>null</jk>.
	 * @return <jk>true</jk> if the modification has been made to the entry.
	 */
	public boolean isApplied(String value) {
		return detectFunction.apply(value);
	}

	/**
	 * Applies this modification to the specified entry value.
	 *
	 * <p>
	 * Will only be called if {@link #isApplied(String)} returns <jk>false</jk>.
	 *
	 * @param value The entry value being written.  Will never be <jk>null</jk>.
	 * @return The modified value.
	 */
	public String apply(String value) {
		return applyFunction.apply(value);
	}

	/**
	 * Removes this modification to the specified entry value.
	 *
	 * <p>
	 * Will only be called if {@link #isApplied(String)} returns <jk>true</jk>.
	 *
	 * @param value The entry value being read.  Will never be <jk>null</jk>.
	 * @return The unmodified value.
	 */
	public String remove(String value) {
		return removeFunction.apply(value);
	}

	/**
	 * Applies this modification to the specified entry value if it isn't already applied.
	 *
	 * @param value The entry value being written.  Will never be <jk>null</jk>.
	 * @return The modified value.
	 */
	public final String doApply(String value) {
		return isApplied(value) ? value : apply(value);
	}

	/**
	 * Removes this modification from the specified entry value if it is applied.
	 *
	 * @param value The entry value being written.  Will never be <jk>null</jk>.
	 * @return The modified value.
	 */
	public final String doRemove(String value) {
		return isApplied(value) ? remove(value) : value;
	}
}
