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

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class IntegerAssertion_Test {

	private IntegerAssertion test(Integer value) {
		return assertInteger(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {

		assertThrown(()->test((Integer)null).exists()).message().is("Value was null.");
		test(1).exists();
		test(1).exists();

		assertThrown(()->test(1).isNull()).message().is("Value was not null.");

		test(1).isEqual(1);

		assertThrown(()->test(1).isEqual(2)).message().contains("Unexpected value.");

		assertThrown(()->test(1).isGt(null)).message().is("Argument 'value' cannot be null.");
		test(2).isGt(1);
		assertThrown(()->test(1).isGt(2)).message().is("Value was not greater than expected.\n\tExpect='2'.\n\tActual='1'.");
		assertThrown(()->test(1).isGt(1)).message().is("Value was not greater than expected.\n\tExpect='1'.\n\tActual='1'.");

		assertThrown(()->test(1).isGte(null)).message().is("Argument 'value' cannot be null.");
		test(2).isGte(1);
		assertThrown(()->test(1).isGte(2)).message().is("Value was not greater than or equals to expected.\n\tExpect='2'.\n\tActual='1'.");
		test(1).isGte(1);

		assertThrown(()->test(1).isLt(null)).message().is("Argument 'value' cannot be null.");
		test(1).isLt(2);
		assertThrown(()->test(2).isLt(1)).message().is("Value was not less than expected.\n\tExpect='1'.\n\tActual='2'.");
		assertThrown(()->test(1).isLt(1)).message().is("Value was not less than expected.\n\tExpect='1'.\n\tActual='1'.");

		assertThrown(()->test(1).isLte(null)).message().is("Argument 'value' cannot be null.");
		test(1).isLte(2);
		assertThrown(()->test(2).isLte(1)).message().is("Value was not less than or equals to expected.\n\tExpect='1'.\n\tActual='2'.");
		test(1).isLte(1);

		assertThrown(()->test(2).isBetween(null,3)).message().is("Argument 'lower' cannot be null.");
		assertThrown(()->test(2).isBetween(1,null)).message().is("Argument 'upper' cannot be null.");
		test(2).isBetween(1,3);
		test(1).isBetween(1,3);
		test(3).isBetween(1,3);
		assertThrown(()->test(2).isBetween(1,1)).message().is("Value was not less than or equals to expected.\n\tExpect='1'.\n\tActual='2'.");
		assertThrown(()->test(2).isBetween(3,3)).message().is("Value was not greater than or equals to expected.\n\tExpect='3'.\n\tActual='2'.");

		test(2).isNot("2");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test((Integer)null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test((Integer)null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test((Integer)null).stdout();
	}
}
