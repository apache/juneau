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
public class PatternReplaceVarTest {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		VarResolver vr = VarResolver.create().vars(PatternReplaceVar.class, SystemPropertiesVar.class).build();

		System.setProperty("PatternReplaceVarTest.test", "foo bar");
		System.setProperty("PatternReplaceVarTest.test2", "size=23;display=none;");

		// Simple replacements
		assertEquals("bar bar", vr.resolve("$PR{$S{PatternReplaceVarTest.test},foo,bar}"));
		assertEquals("coo bar", vr.resolve("$PR{$S{PatternReplaceVarTest.test},^f?,co}"));
		assertEquals("fine", vr.resolve("$PR{$S{PatternReplaceVarTest.test},oo*,ine}"));
		assertEquals("FOO FOO", vr.resolve("$PR{$S{PatternReplaceVarTest.test},([a-z]+),FOO}"));

		// Replacements using matched sub classes
		assertEquals("size=23px;display=none;", vr.resolve("$PR{$S{PatternReplaceVarTest.test2},(size=([\\d]+)),\\$1px}"));
		assertEquals("size=??px;display=none;", vr.resolve("$PR{$S{PatternReplaceVarTest.test2},[\\d]+,??px}"));
		assertEquals("size=23;display=none", vr.resolve("$PR{$S{PatternReplaceVarTest.test2},;\\$,}"));
		assertEquals("size=23;none=display;", vr.resolve("$PR{$S{PatternReplaceVarTest.test2},;(*)=(*[^;]),;\\$2=\\$1}"));


	}
}
