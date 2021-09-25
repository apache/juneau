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
 * Represents a list of objects used to compare objects for equality.
 */
public class HashKey {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------
	
	/**
	 * Static creator.
	 * 
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * The builder class for this object.
	 */
	public static class Builder {
		List<Object> list = new ArrayList<>();

		/**
		 * Adds a list of objects to this builder.
		 * 
		 * @param values The values to add to this list.
		 * @return This object.
		 */
		public Builder add(Object...values) {
			Collections.addAll(list, values);
			return this;
		}

		/**
		 * Creates the key.
		 * 
		 * @return The key.
		 */
		public HashKey build() {
			return new HashKey(this);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final int hashCode;
	private final Object[] array;

	HashKey(Builder builder) {
		hashCode = builder.list.hashCode();
		array = builder.list.toArray(new Object[builder.list.size()]);
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
			if (! array[i].equals(x.array[i]))
				return false;
		return true;
	}
}
