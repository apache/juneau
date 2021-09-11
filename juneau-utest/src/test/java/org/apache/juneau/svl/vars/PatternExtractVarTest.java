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
public class PatternExtractVarTest {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		VarResolver vr = VarResolver.create().vars(PatternExtractVar.class, SystemPropertiesVar.class).build();

		System.setProperty("PatternExtractVarTest.test", "foo bar");
		System.setProperty("PatternExtractVarTest.test2", "size=23");

		assertEquals("foo bar", vr.resolve("$PE{$S{PatternExtractVarTest.test},(.*),1}"));

		assertEquals("size", vr.resolve("$PE{$S{PatternExtractVarTest.test2},(.*)=([0-9]+),1}"));
		assertEquals("23", vr.resolve("$PE{$S{PatternExtractVarTest.test2},(.*)=([0-9]+),2}"));
		assertEquals("size=23", vr.resolve("$PE{$S{PatternExtractVarTest.test2},(.*)=([0-9]+),0}"));
		assertEquals("", vr.resolve("$PE{$S{PatternExtractVarTest.test2},(.*)=([0-9]+),-2}"));
		assertEquals("", vr.resolve("$PE{$S{PatternExtractVarTest.test2},(.*)=([0-9]+),54}"));


	}
}
