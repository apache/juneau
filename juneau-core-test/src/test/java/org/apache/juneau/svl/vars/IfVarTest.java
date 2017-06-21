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

import org.apache.juneau.svl.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class IfVarTest {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		VarResolver vr = new VarResolverBuilder().vars(IfVar.class, SystemPropertiesVar.class).build();

		for (String test : new String[]{"","0","false","FALSE","f","F","foobar"}) {
			System.setProperty("IfVarTest.test", test);
			assertEquals("NO", vr.resolve("$IF{$S{IfVarTest.test},YES,NO}"));
			assertEquals("x NO x", vr.resolve("x $IF{ $S{ IfVarTest.test } , YES , NO } x"));
			assertEquals("", vr.resolve("$IF{$S{IfVarTest.test},YES}"));
			assertEquals("x  x", vr.resolve("x $IF{ $S{ IfVarTest.test } , YES } x"));
		}

		for (String test : new String[]{"1","true","TRUE","t","T"}) {
			System.setProperty("IfVarTest.test", test);
			assertEquals("YES", vr.resolve("$IF{$S{IfVarTest.test},YES,NO}"));
			assertEquals("YES", vr.resolve("$IF{$S{IfVarTest.test},YES}"));
			assertEquals("x YES x", vr.resolve("x $IF{ $S{ IfVarTest.test } , YES , NO } x"));
			assertEquals("x YES x", vr.resolve("x $IF{ $S{ IfVarTest.test } , YES } x"));
		}
	}
}
