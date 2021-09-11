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
package org.apache.juneau.svl.vars;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.svl.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class LenVarTest {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		VarResolver vr = VarResolver.create().vars(LenVar.class, SystemPropertiesVar.class).build();

		System.setProperty("LenVarTest.test", "foo bar");
		System.setProperty("LenVarTest.test2", "1.2.3.4.5");

		// $LN{stringArg} examples
		assertEquals("1", vr.resolve("$LN{J}"));
		assertEquals("len=7", vr.resolve("len=$LN{$S{LenVarTest.test}}"));

		// $LN{stringArg, delimiter} examples
		assertEquals("5", vr.resolve("$LN{$S{LenVarTest.test2},.}"));


	}
}
