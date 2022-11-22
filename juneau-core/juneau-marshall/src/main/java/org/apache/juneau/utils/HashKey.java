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

import static org.apache.juneau.collections.JsonMap.*;

import java.util.*;

/**
 * Represents a list of objects used to compare objects for equality.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class HashKey {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param array The contents of the key.
	 * @return A new bean.
	 */
	public static HashKey of(Object...array) {
		return new HashKey(array);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final int hashCode;
	private final Object[] array;

	HashKey(Object[] array) {
		this.array = array;
		this.hashCode = Arrays.hashCode(array);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		HashKey x = (HashKey)o;
		if (array.length != x.array.length)
			return false;
		for (int i = 0; i < array.length; i++)
			if (! Objects.equals(array[i], x.array[i]))
				return false;
		return true;
	}

	@Override /* Object */
	public String toString() {
		return filteredMap()
			.append("hashCode", hashCode())
			.append("array", array)
			.asString();
	}
}
