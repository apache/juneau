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
package org.apache.juneau.assertions;

import static org.apache.juneau.assertions.Verify.*;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Verify_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	private StringAssertion test(Object value) {
		return assertString(value).setSilent();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic() {
		Verify x1 = verify("foo"), x2 = verify(null), x3 = verify(new Date(0));

		test(x1.is("foo")).isNull();
		test(x1.is("bar")).is("Expected 'bar' but was 'foo'.");
		test(x1.is(null)).is("Expected 'null' but was 'foo'.");
		test(x2.is(null)).isNull();
		test(x2.is("foo")).is("Expected 'foo' but was 'null'.");
		test(x3.is(new Date(0))).isNull();

		test(x1.isType(String.class)).isNull();
		test(x1.isType(Integer.class)).is("Expected type 'java.lang.Integer' but was 'java.lang.String'.");
		test(x2.isType(null)).isNull();
		test(x2.isType(String.class)).is("Expected type 'java.lang.String' but was 'null'.");
		test(x1.isType(null)).is("Expected type 'null' but was 'java.lang.String'.");


		test(verify(true).isTrue()).isNull();
		test(verify(false).isFalse()).isNull();
		test(verify(null).isTrue()).is("Expected 'true' but was 'null'.");
		test(verify(null).isFalse()).is("Expected 'false' but was 'null'.");
		test(verify(Boolean.TRUE).isTrue()).isNull();
		test(verify(Boolean.FALSE).isFalse()).isNull();
		test(x1.is("foo")).isNull();

		test(verify("foo").msg("bar{0}", "baz").is("foo")).isNull();
		test(verify("foo").msg("bar{0}", "baz").is("bar")).is("barbaz");
		test(verify("foo").msg("bar{0}", "baz").isType(Integer.class)).is("barbaz");
		test(verify(null).msg("bar{0}", "baz").is("bar")).is("barbaz");
		test(verify("foo").msg("bar{0}", "baz").is(null)).is("barbaz");
	}
}
