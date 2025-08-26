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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.swaps.*;
import org.junit.jupiter.api.*;

class IteratorSwapTest extends SimpleTestBase {

	//====================================================================================================
	// test
	//====================================================================================================
	@Test void a01_test() throws Exception {
		var s = JsonSerializer.create().json5().swaps(IteratorSwap.class).build();

		// Iterators
		var l = new ArrayList<>(Arrays.asList(new String[]{"foo","bar","baz"}));
		var i = l.iterator();
		assertEquals("['foo','bar','baz']", s.serialize(i));
	}
}