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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.Version.*;
import static org.junit.runners.MethodSorters.*;

import static java.util.Optional.*;

import org.apache.juneau.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class VersionAssertion_Test {

	private VersionAssertion test(Version value) {
		return assertVersion(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {

		assertThrown(()->test((Version)null).exists()).message().is("Value was null.");
		test(of("1")).exists();
		test(of("1")).exists();

		assertThrown(()->test(of("1")).doesNotExist()).message().is("Value was not null.");


		assertThrown(()->test(of("1")).isEqual(of("2"))).message().contains("Unexpected value.");

		assertThrown(()->test(of("1")).isGreaterThan(null)).message().is("Argument 'value' cannot be null.");
		test(of("2")).isGreaterThan(of("1"));
		assertThrown(()->test(of("1")).isGreaterThan(of("2"))).message().is("Value was not greater than expected.\n\tExpect='2'.\n\tActual='1'.");
		assertThrown(()->test(of("1")).isGreaterThan(of("1"))).message().is("Value was not greater than expected.\n\tExpect='1'.\n\tActual='1'.");

		assertThrown(()->test(of("1")).isGt(null)).message().is("Argument 'value' cannot be null.");
		test(of("2")).isGt(of("1"));
		assertThrown(()->test(of("1")).isGt(of("2"))).message().is("Value was not greater than expected.\n\tExpect='2'.\n\tActual='1'.");
		assertThrown(()->test(of("1")).isGt(of("1"))).message().is("Value was not greater than expected.\n\tExpect='1'.\n\tActual='1'.");

		assertThrown(()->test(of("1")).isGreaterThanOrEqual(null)).message().is("Argument 'value' cannot be null.");
		test(of("2")).isGreaterThanOrEqual(of("1"));
		assertThrown(()->test(of("1")).isGreaterThanOrEqual(of("2"))).message().is("Value was not greater than or equals to expected.\n\tExpect='2'.\n\tActual='1'.");
		test(of("1")).isGreaterThanOrEqual(of("1"));

		assertThrown(()->test(of("1")).isGte(null)).message().is("Argument 'value' cannot be null.");
		test(of("2")).isGte(of("1"));
		assertThrown(()->test(of("1")).isGte(of("2"))).message().is("Value was not greater than or equals to expected.\n\tExpect='2'.\n\tActual='1'.");
		test(of("1")).isGte(of("1"));

		assertThrown(()->test(of("1")).isLessThan(null)).message().is("Argument 'value' cannot be null.");
		test(of("1")).isLessThan(of("2"));
		assertThrown(()->test(of("2")).isLessThan(of("1"))).message().is("Value was not less than expected.\n\tExpect='1'.\n\tActual='2'.");
		assertThrown(()->test(of("1")).isLessThan(of("1"))).message().is("Value was not less than expected.\n\tExpect='1'.\n\tActual='1'.");

		assertThrown(()->test(of("1")).isLt(null)).message().is("Argument 'value' cannot be null.");
		test(of("1")).isLt(of("2"));
		assertThrown(()->test(of("2")).isLt(of("1"))).message().is("Value was not less than expected.\n\tExpect='1'.\n\tActual='2'.");
		assertThrown(()->test(of("1")).isLt(of("1"))).message().is("Value was not less than expected.\n\tExpect='1'.\n\tActual='1'.");

		assertThrown(()->test(of("1")).isLessThanOrEqual(null)).message().is("Argument 'value' cannot be null.");
		test(of("1")).isLessThanOrEqual(of("2"));
		assertThrown(()->test(of("2")).isLessThanOrEqual(of("1"))).message().is("Value was not less than or equals to expected.\n\tExpect='1'.\n\tActual='2'.");
		test(of("1")).isLessThanOrEqual(of("1"));

		assertThrown(()->test(of("1")).isLte(null)).message().is("Argument 'value' cannot be null.");
		test(of("1")).isLte(of("2"));
		assertThrown(()->test(of("2")).isLte(of("1"))).message().is("Value was not less than or equals to expected.\n\tExpect='1'.\n\tActual='2'.");
		test(of("1")).isLte(of("1"));

		assertThrown(()->test(of("2")).isBetween(null,of("3"))).message().is("Argument 'lower' cannot be null.");
		assertThrown(()->test(of("2")).isBetween(of("1"),null)).message().is("Argument 'upper' cannot be null.");
		test(of("2")).isBetween(of("1"),of("3"));
		test(of("1")).isBetween(of("1"),of("3"));
		test(of("3")).isBetween(of("1"),of("3"));
		assertThrown(()->test(of("2")).isBetween(of("1"),of("1"))).message().is("Value was not less than or equals to expected.\n\tExpect='1'.\n\tActual='2'.");
		assertThrown(()->test(of("2")).isBetween(of("3"),of("3"))).message().is("Value was not greater than or equals to expected.\n\tExpect='3'.\n\tActual='2'.");

		test(of("2")).isNot("2");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test((Version)null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test((Version)null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test((Version)null).stdout();
	}
}
