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
package org.apache.juneau.reflect;

/**
 * An interface for creating objects from other objects such as a <c>String</c> or <c>Reader</c>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <I> Input type.
 * @param <O> Output type.
 */
public abstract class Mutater<I,O> {

	/**
	 * Method for instantiating an object from another object.
	 *
	 * @param in The input object.
	 * @return The output object.
	 */
	public O mutate(I in) {
		return mutate(null, in);
	}

	/**
	 * Method for instantiating an object from another object.
	 *
	 * @param outer The context object.
	 * @param in The input object.
	 * @return The output object.
	 */
	public abstract O mutate(Object outer, I in);
}
