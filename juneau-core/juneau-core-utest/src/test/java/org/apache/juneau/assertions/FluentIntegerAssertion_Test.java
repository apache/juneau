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
public class FluentIntegerAssertion_Test {

	@Test
	public void a01_basic() throws Exception {

		assertThrown(()->assertInteger(null).exists()).is("Value was null.");
		assertInteger(1).exists();

		assertInteger(null).doesNotExist();
		assertThrown(()->assertInteger(1).doesNotExist()).is("Value was not null.");

		assertInteger(null).isEqual(null);
		assertInteger(1).isEqual(1);

		assertThrown(()->assertInteger(1).isEqual(2)).contains("Unexpected value.");
		assertInteger(null).isEqual(null);

		assertThrown(()->assertInteger(null).isGreaterThan(1)).is("Value was null.");
		assertThrown(()->assertInteger(1).isGreaterThan(null)).is("Parameter cannot be null.");
		assertInteger(2).isGreaterThan(1);
		assertThrown(()->assertInteger(1).isGreaterThan(2)).is("Value was not greater than expected.\n\tExpected=[2]\n\tActual=[1]");
		assertThrown(()->assertInteger(1).isGreaterThan(1)).is("Value was not greater than expected.\n\tExpected=[1]\n\tActual=[1]");

		assertThrown(()->assertInteger(null).isGt(1)).is("Value was null.");
		assertThrown(()->assertInteger(1).isGt(null)).is("Parameter cannot be null.");
		assertInteger(2).isGt(1);
		assertThrown(()->assertInteger(1).isGt(2)).is("Value was not greater than expected.\n\tExpected=[2]\n\tActual=[1]");
		assertThrown(()->assertInteger(1).isGt(1)).is("Value was not greater than expected.\n\tExpected=[1]\n\tActual=[1]");

		assertThrown(()->assertInteger(null).isGreaterThanOrEqual(1)).is("Value was null.");
		assertThrown(()->assertInteger(1).isGreaterThanOrEqual(null)).is("Parameter cannot be null.");
		assertInteger(2).isGreaterThanOrEqual(1);
		assertThrown(()->assertInteger(1).isGreaterThanOrEqual(2)).is("Value was not greater than or equals to expected.\n\tExpected=[2]\n\tActual=[1]");
		assertInteger(1).isGreaterThanOrEqual(1);

		assertThrown(()->assertInteger(null).isGte(1)).is("Value was null.");
		assertThrown(()->assertInteger(1).isGte(null)).is("Parameter cannot be null.");
		assertInteger(2).isGte(1);
		assertThrown(()->assertInteger(1).isGte(2)).is("Value was not greater than or equals to expected.\n\tExpected=[2]\n\tActual=[1]");
		assertInteger(1).isGte(1);

		assertThrown(()->assertInteger(null).isLessThan(1)).is("Value was null.");
		assertThrown(()->assertInteger(1).isLessThan(null)).is("Parameter cannot be null.");
		assertInteger(1).isLessThan(2);
		assertThrown(()->assertInteger(2).isLessThan(1)).is("Value was not less than expected.\n\tExpected=[1]\n\tActual=[2]");
		assertThrown(()->assertInteger(1).isLessThan(1)).is("Value was not less than expected.\n\tExpected=[1]\n\tActual=[1]");

		assertThrown(()->assertInteger(null).isLt(1)).is("Value was null.");
		assertThrown(()->assertInteger(1).isLt(null)).is("Parameter cannot be null.");
		assertInteger(1).isLt(2);
		assertThrown(()->assertInteger(2).isLt(1)).is("Value was not less than expected.\n\tExpected=[1]\n\tActual=[2]");
		assertThrown(()->assertInteger(1).isLt(1)).is("Value was not less than expected.\n\tExpected=[1]\n\tActual=[1]");

		assertThrown(()->assertInteger(null).isLessThanOrEqual(1)).is("Value was null.");
		assertThrown(()->assertInteger(1).isLessThanOrEqual(null)).is("Parameter cannot be null.");
		assertInteger(1).isLessThanOrEqual(2);
		assertThrown(()->assertInteger(2).isLessThanOrEqual(1)).is("Value was not less than or equals to expected.\n\tExpected=[1]\n\tActual=[2]");
		assertInteger(1).isLessThanOrEqual(1);

		assertThrown(()->assertInteger(null).isLte(1)).is("Value was null.");
		assertThrown(()->assertInteger(1).isLte(null)).is("Parameter cannot be null.");
		assertInteger(1).isLte(2);
		assertThrown(()->assertInteger(2).isLte(1)).is("Value was not less than or equals to expected.\n\tExpected=[1]\n\tActual=[2]");
		assertInteger(1).isLte(1);

		assertThrown(()->assertInteger(null).isBetween(1,3)).is("Value was null.");
		assertThrown(()->assertInteger(2).isBetween(null,3)).is("Parameter 'lower' cannot be null.");
		assertThrown(()->assertInteger(2).isBetween(1,null)).is("Parameter 'upper' cannot be null.");
		assertInteger(2).isBetween(1,3);
		assertInteger(1).isBetween(1,3);
		assertInteger(3).isBetween(1,3);
		assertThrown(()->assertInteger(2).isBetween(1,1)).is("Value was not less than or equals to expected.\n\tExpected=[1]\n\tActual=[2]");
		assertThrown(()->assertInteger(2).isBetween(3,3)).is("Value was not greater than or equals to expected.\n\tExpected=[3]\n\tActual=[2]");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->IntegerAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		IntegerAssertion.create(null).stdout().stderr();
	}
}
