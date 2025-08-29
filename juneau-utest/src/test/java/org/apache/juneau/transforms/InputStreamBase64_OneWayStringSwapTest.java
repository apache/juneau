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
package org.apache.juneau.transforms;

import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;

class InputStreamBase64_OneWayStringSwapTest extends OneWayStringSwapTest_Base {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	private static BeanSession BS = BeanContext.DEFAULT_SESSION;
	private static InputStreamSwap SWAP = new InputStreamSwap.Base64();

	private static <T> OneWayStringSwapTester<T> tester(int index, String label, T object, StringSwap<T> swap, String expected, BeanSession bs) {
		return OneWayStringSwapTester.create(index, label, object, swap, expected, bs).build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Parameters
	//------------------------------------------------------------------------------------------------------------------

	private static final OneWayStringSwapTester<?>[] TESTERS = {
		tester(1, "Basic string", inputStream("foo"), SWAP, "Zm9v", BS),
		tester(2, "Blank string", inputStream(""), SWAP, "", BS),
		tester(3, "null", null, SWAP, "", BS)
	};

	static OneWayStringSwapTester<?>[] testers() {
		return TESTERS;
	}
}