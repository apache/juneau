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

import java.util.*;

import org.apache.juneau.*;
import static org.apache.juneau.StringRanges.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StringRanges_Test {

	@Test
	public void a01_match() throws Exception {
		List<String> x = alist("foo","bar","baz");

		assertInteger(of((String)null).match(x)).is(-1);

		assertInteger(of("foo;q=0.5,bar").match(x)).is(1);
		assertInteger(of("foo;q=0.5,bar").match(x)).is(1);
		assertInteger(of("foo,*").match(x)).is(0);
		assertInteger(of("*,bar").match(x)).is(1);
		assertInteger(of("foo;q=0.6,bar;q=0.5").match(x)).is(0);
		assertInteger(of("foo;q=0.6,bar;q=0.5,qux").match(x)).is(0);
		assertInteger(of("qux").match(x)).is(-1);
		assertInteger(of("qux,q2x;q=0").match(x)).is(-1);
		assertInteger(of("foo;q=0").match(x)).is(-1);
	}

	@Test
	public void a02_getRange() throws Exception {
		assertString(of("foo").getRange(0)).isNotNull();
		assertString(of((String)null).getRange(0)).isNull();
		assertString(of("").getRange(0)).isNull();
		assertString(of((String)null).getRange(-1)).isNull();
		assertString(of((String)null).getRange(1)).isNull();
	}

	@Test
	public void a03_getRanges() throws Exception {
		assertObject(of("foo").toList()).asJson().is("['foo']");
		assertObject(of((String)null).toList()).asJson().is("[]");
	}

	@Test
	public void a04_toString() throws Exception {
		assertString(new StringRange("*")).is("*");
		assertString(of("foo;q=0.6,bar;q=0.9,qux")).is("qux, bar;q=0.9, foo;q=0.6");
	}
}
