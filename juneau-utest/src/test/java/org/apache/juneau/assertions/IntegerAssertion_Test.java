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
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import static java.util.Optional.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class IntegerAssertion_Test {

	private IntegerAssertion test(Integer value) {
		return assertInteger(value).silent();
	}

	private IntegerAssertion test(Optional<Integer> value) {
		return assertInteger(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {

		assertThrown(()->test((Integer)null).exists()).is("Value was null.");
		test(1).exists();
		assertThrown(()->test(empty()).exists()).is("Value was null.");
		test(1).exists();

		test(empty()).doesNotExist();
		assertThrown(()->test(1).doesNotExist()).is("Value was not null.");

		test(empty()).isEqual(null);
		test(1).isEqual(1);
		test(of(1)).isEqual(1);

		assertThrown(()->test(1).isEqual(2)).contains("Unexpected value.");
		test(empty()).isEqual(null);

		assertThrown(()->test(empty()).isGreaterThan(1)).is("Value was null.");
		assertThrown(()->test(1).isGreaterThan(null)).is("Argument \"value\" cannot be null.");
		test(2).isGreaterThan(1);
		assertThrown(()->test(1).isGreaterThan(2)).is("Value was not greater than expected.\n\tExpect=\"2\".\n\tActual=\"1\".");
		assertThrown(()->test(1).isGreaterThan(1)).is("Value was not greater than expected.\n\tExpect=\"1\".\n\tActual=\"1\".");

		assertThrown(()->test(empty()).isGt(1)).is("Value was null.");
		assertThrown(()->test(1).isGt(null)).is("Argument \"value\" cannot be null.");
		test(2).isGt(1);
		assertThrown(()->test(1).isGt(2)).is("Value was not greater than expected.\n\tExpect=\"2\".\n\tActual=\"1\".");
		assertThrown(()->test(1).isGt(1)).is("Value was not greater than expected.\n\tExpect=\"1\".\n\tActual=\"1\".");

		assertThrown(()->test(empty()).isGreaterThanOrEqual(1)).is("Value was null.");
		assertThrown(()->test(1).isGreaterThanOrEqual(null)).is("Argument \"value\" cannot be null.");
		test(2).isGreaterThanOrEqual(1);
		assertThrown(()->test(1).isGreaterThanOrEqual(2)).is("Value was not greater than or equals to expected.\n\tExpect=\"2\".\n\tActual=\"1\".");
		test(1).isGreaterThanOrEqual(1);

		assertThrown(()->test(empty()).isGte(1)).is("Value was null.");
		assertThrown(()->test(1).isGte(null)).is("Argument \"value\" cannot be null.");
		test(2).isGte(1);
		assertThrown(()->test(1).isGte(2)).is("Value was not greater than or equals to expected.\n\tExpect=\"2\".\n\tActual=\"1\".");
		test(1).isGte(1);

		assertThrown(()->test(empty()).isLessThan(1)).is("Value was null.");
		assertThrown(()->test(1).isLessThan(null)).is("Argument \"value\" cannot be null.");
		test(1).isLessThan(2);
		assertThrown(()->test(2).isLessThan(1)).is("Value was not less than expected.\n\tExpect=\"1\".\n\tActual=\"2\".");
		assertThrown(()->test(1).isLessThan(1)).is("Value was not less than expected.\n\tExpect=\"1\".\n\tActual=\"1\".");

		assertThrown(()->test(empty()).isLt(1)).is("Value was null.");
		assertThrown(()->test(1).isLt(null)).is("Argument \"value\" cannot be null.");
		test(1).isLt(2);
		assertThrown(()->test(2).isLt(1)).is("Value was not less than expected.\n\tExpect=\"1\".\n\tActual=\"2\".");
		assertThrown(()->test(1).isLt(1)).is("Value was not less than expected.\n\tExpect=\"1\".\n\tActual=\"1\".");

		assertThrown(()->test(empty()).isLessThanOrEqual(1)).is("Value was null.");
		assertThrown(()->test(1).isLessThanOrEqual(null)).is("Argument \"value\" cannot be null.");
		test(1).isLessThanOrEqual(2);
		assertThrown(()->test(2).isLessThanOrEqual(1)).is("Value was not less than or equals to expected.\n\tExpect=\"1\".\n\tActual=\"2\".");
		test(1).isLessThanOrEqual(1);

		assertThrown(()->test(empty()).isLte(1)).is("Value was null.");
		assertThrown(()->test(1).isLte(null)).is("Argument \"value\" cannot be null.");
		test(1).isLte(2);
		assertThrown(()->test(2).isLte(1)).is("Value was not less than or equals to expected.\n\tExpect=\"1\".\n\tActual=\"2\".");
		test(1).isLte(1);

		assertThrown(()->test(empty()).isBetween(1,3)).is("Value was null.");
		assertThrown(()->test(2).isBetween(null,3)).is("Argument \"lower\" cannot be null.");
		assertThrown(()->test(2).isBetween(1,null)).is("Argument \"upper\" cannot be null.");
		test(2).isBetween(1,3);
		test(1).isBetween(1,3);
		test(3).isBetween(1,3);
		assertThrown(()->test(2).isBetween(1,1)).is("Value was not less than or equals to expected.\n\tExpect=\"1\".\n\tActual=\"2\".");
		assertThrown(()->test(2).isBetween(3,3)).is("Value was not greater than or equals to expected.\n\tExpect=\"3\".\n\tActual=\"2\".");

		test(2).isNot("2");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test((Integer)null).msg("Foo {0}", 1).exists()).is("Foo 1");
		assertThrown(()->test((Integer)null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).is("Foo 1");
		test((Integer)null).stdout();
	}
}
