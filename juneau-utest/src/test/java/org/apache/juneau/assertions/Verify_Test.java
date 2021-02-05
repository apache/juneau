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

	@Test
	public void a01_basic() {
		Verify x1 = verify("foo"), x2 = verify(null), x3 = verify(new Date(0));

		assertString(x1.is("foo")).doesNotExist();
		assertString(x1.is("bar")).is("Expected [bar] but was [foo].");
		assertString(x1.is(null)).is("Expected [null] but was [foo].");
		assertString(x2.is(null)).doesNotExist();
		assertString(x2.is("foo")).is("Expected [foo] but was [null].");
		assertString(x3.is(new Date(0))).doesNotExist();

		assertString(x1.isType(String.class)).doesNotExist();
		assertString(x1.isType(Integer.class)).is("Expected type [java.lang.Integer] but was [java.lang.String].");
		assertString(x2.isType(null)).doesNotExist();
		assertString(x2.isType(String.class)).is("Expected type [java.lang.String] but was [null].");
		assertString(x1.isType(null)).is("Expected type [null] but was [java.lang.String].");


		assertString(verify(true).isTrue()).doesNotExist();
		assertString(verify(false).isFalse()).doesNotExist();
		assertString(verify(null).isTrue()).is("Expected [true] but was [null].");
		assertString(verify(null).isFalse()).is("Expected [false] but was [null].");
		assertString(verify(Boolean.TRUE).isTrue()).doesNotExist();
		assertString(verify(Boolean.FALSE).isFalse()).doesNotExist();
		assertString(x1.is("foo")).doesNotExist();

		assertString(verify("foo").msg("bar{0}", "baz").is("foo")).doesNotExist();
		assertString(verify("foo").msg("bar{0}", "baz").is("bar")).is("barbaz");
		assertString(verify("foo").msg("bar{0}", "baz").isType(Integer.class)).is("barbaz");
		assertString(verify(null).msg("bar{0}", "baz").is("bar")).is("barbaz");
		assertString(verify("foo").msg("bar{0}", "baz").is(null)).is("barbaz");
	}
}
