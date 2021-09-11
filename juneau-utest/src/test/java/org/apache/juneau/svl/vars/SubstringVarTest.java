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
public class SubstringVarTest {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		VarResolver vr = VarResolver.create().vars(SubstringVar.class, SystemPropertiesVar.class).build();

		System.setProperty("SubstringVarTest.test", "foo bar");

		// $ST{stringArg, start} examples
		// Notice that -start also works here
		assertEquals("o bar", vr.resolve("$ST{$S{SubstringVarTest.test},2}"));
		assertEquals("", vr.resolve("$ST{$S{SubstringVarTest.test},24}"));
		assertEquals("foo bar", vr.resolve("$ST{$S{SubstringVarTest.test},0}"));
		assertEquals("", vr.resolve("$ST{$S{SubstringVarTest.test},-24}"));
		assertEquals("bar", vr.resolve("$ST{$S{SubstringVarTest.test},-3}"));
		assertEquals("foo bar", vr.resolve("$ST{$S{SubstringVarTest.test},-7}"));

		// $ST{stringArg, start, end}
		assertEquals("foo bar", vr.resolve("$ST{$S{SubstringVarTest.test},0,7}"));
		assertEquals("oo", vr.resolve("$ST{$S{SubstringVarTest.test},1,3}"));
		assertEquals("", vr.resolve("$ST{$S{SubstringVarTest.test},-2, 2}"));

	}
}
