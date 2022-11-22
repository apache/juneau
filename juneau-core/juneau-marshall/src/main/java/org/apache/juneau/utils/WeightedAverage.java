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
 * A simple weighted average of numbers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class WeightedAverage {
	private Double value = 0d;
	private int weight = 0;

	/**
	 * Add a number with a weight to this average.
	 *
	 * @param w The weight of the new value.
	 * @param v The new value.
	 * @return This object.
	 */
	public WeightedAverage add(int w, Number v) {
		if (v != null) {
			try {
				double w1 = weight, w2 = w;
				weight = Math.addExact(weight, w);
				if (weight != 0) {
					value = (value * (w1/weight)) + (v.floatValue() * (w2/weight));
				}
			} catch (ArithmeticException ae) {
				throw new ArithmeticException("Weight overflow.");
			}
		}
		return this;
	}

	/**
	 * Returns the weighted average of all numbers.
	 *
	 * @return The weighted average of all numbers.
	 */
	public double getValue() {
		return value;
	}
}
