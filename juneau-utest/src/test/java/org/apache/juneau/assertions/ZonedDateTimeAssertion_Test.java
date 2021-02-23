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
import static java.util.Optional.*;

import java.time.*;
import java.time.temporal.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ZonedDateTimeAssertion_Test {

	@Test
	public void a01_basic() throws Exception {
		ZonedDateTime x1 = ZonedDateTime.parse("1950-01-01T12:34:56Z"), x2 = ZonedDateTime.parse("2050-01-01T12:34:56Z");
		ZonedDateTime now = ZonedDateTime.now();

		assertThrown(()->assertZonedDateTime((ZonedDateTime)null).exists()).is("Value was null.");
		assertZonedDateTime(x1).exists();
		assertThrown(()->assertZonedDateTime(empty()).exists()).is("Value was null.");
		assertZonedDateTime(x1).exists();

		assertZonedDateTime(empty()).doesNotExist();
		assertThrown(()->assertZonedDateTime(x1).doesNotExist()).is("Value was not null.");

		assertZonedDateTime(empty()).isEqual(null);
		assertZonedDateTime(x1).isEqual(x1);
		assertZonedDateTime(of(x1)).isEqual(x1);
		assertZonedDateTime(x1).isEqual(x1, ChronoUnit.DAYS);
		assertThrown(()->assertZonedDateTime(x2).isEqual(x1, ChronoUnit.DAYS)).contains("Unexpected value.");

		assertZonedDateTime(empty()).isEqual(null);
		assertZonedDateTime(x1).isEqual(x1);
		assertZonedDateTime(x1).isEqual(x1, ChronoUnit.DAYS);
		assertThrown(()->assertZonedDateTime(x2).isEqual(x1, ChronoUnit.DAYS)).contains("Unexpected value.");

		assertThrown(()->assertZonedDateTime(empty()).isBefore(x1)).is("Value was null.");
		assertThrown(()->assertZonedDateTime(x1).isBefore(null)).is("Parameter 'value' cannot be null.");
		assertZonedDateTime(x1).isBefore(x2);
		assertThrown(()->assertZonedDateTime(x2).isBefore(x1)).contains("Value was not before expected.");

		assertThrown(()->assertZonedDateTime(empty()).isAfter(x1)).is("Value was null.");
		assertThrown(()->assertZonedDateTime(x1).isAfter(null)).is("Parameter 'value' cannot be null.");
		assertZonedDateTime(x2).isAfter(x1);
		assertThrown(()->assertZonedDateTime(x1).isAfter(x2)).contains("Value was not after expected.");

		assertThrown(()->assertZonedDateTime(empty()).isBeforeNow()).is("Value was null.");
		assertZonedDateTime(x1).isBeforeNow();
		assertThrown(()->assertZonedDateTime(x2).isBeforeNow()).contains("Value was not before expected.");

		assertThrown(()->assertZonedDateTime(empty()).isAfterNow()).is("Value was null.");
		assertZonedDateTime(x2).isAfterNow();
		assertThrown(()->assertZonedDateTime(x1).isAfterNow()).contains("Value was not after expected.");

		assertThrown(()->assertZonedDateTime(empty()).isBetween(x1,x2)).is("Value was null.");
		assertThrown(()->assertZonedDateTime(now).isBetween(null,x2)).is("Parameter 'lower' cannot be null.");
		assertThrown(()->assertZonedDateTime(now).isBetween(x1,null)).is("Parameter 'upper' cannot be null.");
		assertZonedDateTime(now).isBetween(x1, x2);
		assertThrown(()->assertZonedDateTime(x1).isBetween(now,x2)).contains("Value was not after expected.");
		assertThrown(()->assertZonedDateTime(x2).isBetween(x1,now)).contains("Value was not before expected.");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->ZonedDateTimeAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		ZonedDateTimeAssertion.create(null).stdout().silent();
	}
}
