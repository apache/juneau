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

import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class WeightedAverageTest extends SimpleTestBase {

	@Test void a01_empty() {
		var w = new WeightedAverage();
		assertEquals(0f, w.getValue(), 0.01);
	}

	@Test void a02_basic() {
		var w = new WeightedAverage();
		w.add(0,100).add(1,1).add(1,2).add(1,3).add(0,100);
		assertEquals(2f, w.getValue(), 0.01);
	}

	@Test void a03_basicWithNullValue() {
		var w = new WeightedAverage();
		w.add(1,1).add(1,null).add(1,3);
		assertEquals(2f, w.getValue(), 0.01);
	}

	@Test void a04_differingWeights() {
		var w = new WeightedAverage();
		w.add(10,1).add(20,3);
		assertEquals(2.33f, w.getValue(), 0.01);
	}
}