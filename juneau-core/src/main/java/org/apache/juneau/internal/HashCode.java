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
 * Utility class for generating integer hash codes.
 * <p>
 * 	General usage:
 * <p class='bcode'>
 * 	int hashCode = new HashCode().add("foobar").add(myobject).add(123).get();
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class HashCode {

	private int hashCode = 1;

	/**
	 * Create a new HashCode object.
	 *
	 * @return A new HashCode object.
	 */
	public static final HashCode create() {
		return new HashCode();
	}

	/**
	 * Hashes the hashcode of the specified object into this object.
	 *
	 * @param o The object whose hashcode will be hashed with this object.
	 * @return This object (for method chaining).
	 */
	public HashCode add(Object o) {
		o = unswap(o);
		add(o == null ? 1 : o.hashCode());
		return this;
	}

	/**
	 * Hashes the hashcode into this object.
	 * <p>
	 * The formula is simply <code>hashCode = 31*hashCode + i;</code>
	 *
	 * @param i The hashcode to hash into this object's hashcode.
	 * @return This object (for method chaining).
	 */
	public HashCode add(int i) {
      hashCode = 31*hashCode + i;
		return this;
	}

	/**
	 * Return the calculated hashcode value.
	 *
	 * @return The calculated hashcode.
	 */
	public int get() {
      return hashCode;
	}

	/**
	 * Converts the object to a normalized form before grabbing it's hashcode.
	 * Subclasses can override this method to provide specialized handling
	 * 	(e.g. converting numbers to strings so that <code>123</code> and <js>"123"</js>
	 * 	end up creating the same hashcode.)
	 * Default implementation does nothing.
	 *
	 * @param o The object to normalize before getting it's hashcode.
	 * @return The normalized object.
	 */
	protected Object unswap(Object o) {
		return o;
	}
}
