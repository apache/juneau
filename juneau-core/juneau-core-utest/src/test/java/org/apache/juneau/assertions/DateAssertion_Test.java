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

import java.time.temporal.*;
import java.util.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class DateAssertion_Test {

	@Test
	public void a01_basic() throws Exception {
		Date x1 = new Date(0), x2 = new Date(Long.MAX_VALUE), now = new Date();

		assertThrown(()->assertDate(null).exists()).is("Value was null.");
		assertDate(x1).exists();

		assertDate(null).doesNotExist();
		assertThrown(()->assertDate(x1).doesNotExist()).is("Value was not null.");

		assertDate(null).isEqual(null);
		assertDate(x1).isEqual(x1);
		assertDate(x1).isEqual(x1, ChronoUnit.DAYS);
		assertThrown(()->assertDate(x2).isEqual(x1, ChronoUnit.DAYS)).contains("Unexpected value.");

		assertDate(null).isEqual(null);
		assertDate(x1).isEqual(x1);
		assertDate(x1).isEqual(x1, ChronoUnit.DAYS);
		assertThrown(()->assertDate(x2).isEqual(x1, ChronoUnit.DAYS)).contains("Unexpected value.");

		assertThrown(()->assertDate(null).isBefore(x1)).is("Value was null.");
		assertThrown(()->assertDate(x1).isBefore(null)).is("Parameter cannot be null.");
		assertDate(x1).isBefore(x2);
		assertThrown(()->assertDate(x2).isBefore(x1)).contains("Value was not before expected.");

		assertThrown(()->assertDate(null).isAfter(x1)).is("Value was null.");
		assertThrown(()->assertDate(x1).isAfter(null)).is("Parameter cannot be null.");
		assertDate(x2).isAfter(x1);
		assertThrown(()->assertDate(x1).isAfter(x2)).contains("Value was not after expected.");

		assertThrown(()->assertDate(null).isBeforeNow()).is("Value was null.");
		assertDate(x1).isBeforeNow();
		assertThrown(()->assertDate(x2).isBeforeNow()).contains("Value was not before expected.");

		assertThrown(()->assertDate(null).isAfterNow()).is("Value was null.");
		assertDate(x2).isAfterNow();
		assertThrown(()->assertDate(x1).isAfterNow()).contains("Value was not after expected.");

		assertThrown(()->assertDate(null).isBetween(x1,x2)).is("Value was null.");
		assertThrown(()->assertDate(now).isBetween(null,x2)).is("Parameter 'lower' cannot be null.");
		assertThrown(()->assertDate(now).isBetween(x1,null)).is("Parameter 'upper' cannot be null.");
		assertDate(now).isBetween(x1, x2);
		assertThrown(()->assertDate(x1).isBetween(now,x2)).contains("Value was not after expected.");
		assertThrown(()->assertDate(x2).isBetween(x1,now)).contains("Value was not before expected.");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->DateAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		DateAssertion.create(null).stdout().stderr();
	}
}
