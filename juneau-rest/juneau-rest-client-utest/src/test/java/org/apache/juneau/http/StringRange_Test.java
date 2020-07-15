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

import static org.apache.juneau.assertions.Assertions.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StringRange_Test {

	@Test
	public void a01_basic() throws Exception {
		StringRange x1 = new StringRange("foo;a=1;q=0.5;b=2");
		assertString(x1.getQValue()).is("0.5");
		assertObject(x1.getExtensions()).json().is("['a=1','b=2']");

		StringRange x2 = new StringRange("foo;q=1.0");
		assertString(x2.getQValue()).is("1.0");
		assertObject(x2.getExtensions()).json().is("[]");

		StringRange x3 = new StringRange("foo;a=1");
		assertString(x3.getQValue()).is("1.0");
		assertObject(x3.getExtensions()).json().is("['a=1']");

		StringRange x4 = new StringRange("foo;a=1");
		assertObject(x3).is(x4);
		assertObject(x3).isNot(x2);
		assertObject(x3).isNot("foo");
		assertObject(x3.hashCode()).is(x4.hashCode());

		StringRange x5 = new StringRange((String)null);
		assertString(x5).is("*");

		StringRange x6 = new StringRange("foo;q=0");
		assertString(x6.match("foo")).is("0");
	}
}
