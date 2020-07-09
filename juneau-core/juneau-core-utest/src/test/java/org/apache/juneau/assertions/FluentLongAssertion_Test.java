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
public class FluentLongAssertion_Test {

	@Test
	public void a01_basic() throws Exception {

		assertThrown(()->assertLong(null).exists()).is("Value was null.");
		assertLong(1l).exists();

		assertLong(null).doesNotExist();
		assertThrown(()->assertLong(1l).doesNotExist()).is("Value was not null.");

		assertLong(null).isEqual(null);
		assertLong(1l).isEqual(1l);

		assertThrown(()->assertLong(1l).isEqual(2)).contains("Unexpected value.");
		assertLong(null).isEqual(null);

		assertThrown(()->assertLong(null).isGreaterThan(1)).is("Value was null.");
		assertThrown(()->assertLong(1l).isGreaterThan(null)).is("Parameter cannot be null.");
		assertLong(2l).isGreaterThan(1);
		assertThrown(()->assertLong(1l).isGreaterThan(2l)).is("Value was not greater than expected.\n\tExpected=[2]\n\tActual=[1]");
		assertThrown(()->assertLong(1l).isGreaterThan(1l)).is("Value was not greater than expected.\n\tExpected=[1]\n\tActual=[1]");

		assertLong(2l).integer().isGreaterThan(1);
		assertLong(null).integer().isNull();

		assertThrown(()->assertLong(null).isGt(1l)).is("Value was null.");
		assertThrown(()->assertLong(1l).isGt(null)).is("Parameter cannot be null.");
		assertLong(2l).isGt(1);
		assertThrown(()->assertLong(1l).isGt(2)).is("Value was not greater than expected.\n\tExpected=[2]\n\tActual=[1]");
		assertThrown(()->assertLong(1l).isGt(1)).is("Value was not greater than expected.\n\tExpected=[1]\n\tActual=[1]");

		assertThrown(()->assertLong(null).isGreaterThanOrEqual(1)).is("Value was null.");
		assertThrown(()->assertLong(1l).isGreaterThanOrEqual(null)).is("Parameter cannot be null.");
		assertLong(2l).isGreaterThanOrEqual(1);
		assertThrown(()->assertLong(1l).isGreaterThanOrEqual(2)).is("Value was not greater than or equals to expected.\n\tExpected=[2]\n\tActual=[1]");
		assertLong(1l).isGreaterThanOrEqual(1);

		assertThrown(()->assertLong(null).isGte(1)).is("Value was null.");
		assertThrown(()->assertLong(1l).isGte(null)).is("Parameter cannot be null.");
		assertLong(2l).isGte(1);
		assertThrown(()->assertLong(1l).isGte(2l)).is("Value was not greater than or equals to expected.\n\tExpected=[2]\n\tActual=[1]");
		assertLong(1l).isGte(1l);

		assertThrown(()->assertLong(null).isLessThan(1)).is("Value was null.");
		assertThrown(()->assertLong(1l).isLessThan(null)).is("Parameter cannot be null.");
		assertLong(1l).isLessThan(2l);
		assertThrown(()->assertLong(2l).isLessThan(1)).is("Value was not less than expected.\n\tExpected=[1]\n\tActual=[2]");
		assertThrown(()->assertLong(1l).isLessThan(1)).is("Value was not less than expected.\n\tExpected=[1]\n\tActual=[1]");

		assertThrown(()->assertLong(null).isLt(1)).is("Value was null.");
		assertThrown(()->assertLong(1l).isLt(null)).is("Parameter cannot be null.");
		assertLong(1l).isLt(2);
		assertThrown(()->assertLong(2l).isLt(1)).is("Value was not less than expected.\n\tExpected=[1]\n\tActual=[2]");
		assertThrown(()->assertLong(1l).isLt(1)).is("Value was not less than expected.\n\tExpected=[1]\n\tActual=[1]");

		assertThrown(()->assertLong(null).isLessThanOrEqual(1)).is("Value was null.");
		assertThrown(()->assertLong(1l).isLessThanOrEqual(null)).is("Parameter cannot be null.");
		assertLong(1l).isLessThanOrEqual(2);
		assertThrown(()->assertLong(2l).isLessThanOrEqual(1)).is("Value was not less than or equals to expected.\n\tExpected=[1]\n\tActual=[2]");
		assertLong(1l).isLessThanOrEqual(1);

		assertThrown(()->assertLong(null).isLte(1)).is("Value was null.");
		assertThrown(()->assertLong(1l).isLte(null)).is("Parameter cannot be null.");
		assertLong(1l).isLte(2);
		assertThrown(()->assertLong(2l).isLte(1)).is("Value was not less than or equals to expected.\n\tExpected=[1]\n\tActual=[2]");
		assertLong(1l).isLte(1);

		assertThrown(()->assertLong(null).isBetween(1,3)).is("Value was null.");
		assertThrown(()->assertLong(2l).isBetween(null,3)).is("Parameter 'lower' cannot be null.");
		assertThrown(()->assertLong(2l).isBetween(1,null)).is("Parameter 'upper' cannot be null.");
		assertLong(2l).isBetween(1,3);
		assertLong(1l).isBetween(1,3);
		assertLong(3l).isBetween(1,3);
		assertThrown(()->assertLong(2l).isBetween(1,1)).is("Value was not less than or equals to expected.\n\tExpected=[1]\n\tActual=[2]");
		assertThrown(()->assertLong(2l).isBetween(3,3)).is("Value was not greater than or equals to expected.\n\tExpected=[3]\n\tActual=[2]");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->LongAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		LongAssertion.create(null).stdout().stderr();
	}
}
