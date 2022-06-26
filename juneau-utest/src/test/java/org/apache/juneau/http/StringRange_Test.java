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
package org.apache.juneau.http;

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import static org.apache.juneau.assertions.Assertions.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StringRange_Test {

	@Test
	public void a01_basic() throws Exception {
		StringRange x1 = of("foo;a=1;q=0.5;b=2");
		assertString(x1.getQValue()).is("0.5");
		assertObject(x1.getExtensions()).asJson().is("[{name:'a',value:'1'},{name:'b',value:'2'}]");

		StringRange x2 = of("foo;q=1.0");
		assertString(x2.getQValue()).is("1.0");
		assertObject(x2.getExtensions()).asJson().is("[]");

		StringRange x3 = of("foo;a=1");
		assertString(x3.getQValue()).is("1.0");
		assertObject(x3.getExtensions()).asJson().is("[{name:'a',value:'1'}]");

		StringRange x4 = of("foo;a=1");
		assertObject(x3).is(x4);
		assertObject(x3).isNot(x2);
		assertObject(x3).asString().isNot("foo");
		assertObject(x3.hashCode()).is(x4.hashCode());

		assertString(of((String)null)).is("*");

		assertString(of("foo;q=0").match("foo")).is("0");
	}

	@Test
	public void a02_match() throws Exception {
		assertInteger(of("foo").match("foo")).is(100);
		assertInteger(of("foo").match("bar")).is(0);
		assertInteger(of("foo").match(null)).is(0);
		assertInteger(of("*").match("foo")).is(50);
		assertInteger(of(null).match("foo")).is(50);
	}

	@Test
	public void a03_getName() throws Exception {
		assertString(of("foo;a=1;q=0.5;b=2").getName()).is("foo");
		assertString(of(null).getName()).is("*");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private StringRange of(String val) {
		return new StringRange(val);
	}
}
