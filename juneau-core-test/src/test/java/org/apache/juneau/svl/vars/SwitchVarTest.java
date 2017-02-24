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

import org.apache.juneau.svl.VarResolver;
import org.junit.*;

@SuppressWarnings("javadoc")
public class SwitchVarTest {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		VarResolver vr = new VarResolver().addVars(SwitchVar.class, SystemPropertiesVar.class);
		
		System.setProperty("SwitchVarTest.test", "foobar");
		
		assertEquals("YES", vr.resolve("$SWITCH{$S{SwitchVarTest.test},foobar,YES}"));
		assertEquals("YES", vr.resolve("$SWITCH{ $S{ SwitchVarTest.test } , foobar , YES }"));
		assertEquals("", vr.resolve("$SWITCH{$S{SwitchVarTest.test},foobar2,YES}"));
		assertEquals("NO", vr.resolve("$SWITCH{$S{SwitchVarTest.test},foobar2,YES,NO}"));
		assertEquals("NO", vr.resolve("$SWITCH{ $S{ SwitchVarTest.test } , foobar2 , YES , NO }"));

		assertEquals("YES", vr.resolve("$SWITCH{$S{SwitchVarTest.test},foo*,YES,NO}"));
		assertEquals("YES", vr.resolve("$SWITCH{$S{SwitchVarTest.test},*bar,YES,NO}"));
		assertEquals("YES", vr.resolve("$SWITCH{$S{SwitchVarTest.test},*,YES,NO}"));
		assertEquals("YES", vr.resolve("$SWITCH{$S{SwitchVarTest.test},??????,YES,NO}"));

		assertEquals("NO", vr.resolve("$SWITCH{$S{SwitchVarTest.test},foox*,YES,NO}"));
		assertEquals("NO", vr.resolve("$SWITCH{$S{SwitchVarTest.test},*xbar,YES,NO}"));
		assertEquals("NO", vr.resolve("$SWITCH{$S{SwitchVarTest.test},?????,YES,NO}"));
		assertEquals("NO", vr.resolve("$SWITCH{$S{SwitchVarTest.test},???????,YES,NO}"));

		assertEquals("YES2", vr.resolve("$SWITCH{$S{SwitchVarTest.test},foox*,YES1,foo*,YES2}"));
		assertEquals("YES2", vr.resolve("$SWITCH{$S{SwitchVarTest.test},foox*,YES1,foo*,YES2,NO}"));

		assertEquals("NO", vr.resolve("$SWITCH{$S{SwitchVarTest.test},foox*,YES1,fooy*,YES2,NO}"));
	}
}
