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
import java.util.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ZonedDateTimeAssertion_Test {

	private ZonedDateTimeAssertion test(ZonedDateTime value) {
		return assertZonedDateTime(value).silent();
	}

	private ZonedDateTimeAssertion test(Optional<ZonedDateTime> value) {
		return assertZonedDateTime(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {
		ZonedDateTime x1 = ZonedDateTime.parse("1950-01-01T12:34:56Z"), x2 = ZonedDateTime.parse("2050-01-01T12:34:56Z");
		ZonedDateTime now = ZonedDateTime.now();

		assertThrown(()->test((ZonedDateTime)null).exists()).is("Value was null.");
		test(x1).exists();
		assertThrown(()->test(empty()).exists()).is("Value was null.");
		test(x1).exists();

		test(empty()).doesNotExist();
		assertThrown(()->test(x1).doesNotExist()).is("Value was not null.");

		test(empty()).isEqual(null);
		test(x1).isEqual(x1);
		test(of(x1)).isEqual(x1);
		test(x1).isEqual(x1, ChronoUnit.DAYS);
		assertThrown(()->test(x2).isEqual(x1, ChronoUnit.DAYS)).contains("Unexpected value.");

		test(empty()).isEqual(null);
		test(x1).isEqual(x1);
		test(x1).isEqual(x1, ChronoUnit.DAYS);
		assertThrown(()->test(x2).isEqual(x1, ChronoUnit.DAYS)).contains("Unexpected value.");

		assertThrown(()->test(empty()).isBefore(x1)).is("Value was null.");
		assertThrown(()->test(x1).isBefore(null)).is("Parameter 'value' cannot be null.");
		test(x1).isBefore(x2);
		assertThrown(()->test(x2).isBefore(x1)).contains("Value was not before expected.");

		assertThrown(()->test(empty()).isAfter(x1)).is("Value was null.");
		assertThrown(()->test(x1).isAfter(null)).is("Parameter 'value' cannot be null.");
		test(x2).isAfter(x1);
		assertThrown(()->test(x1).isAfter(x2)).contains("Value was not after expected.");

		assertThrown(()->test(empty()).isBeforeNow()).is("Value was null.");
		test(x1).isBeforeNow();
		assertThrown(()->test(x2).isBeforeNow()).contains("Value was not before expected.");

		assertThrown(()->test(empty()).isAfterNow()).is("Value was null.");
		test(x2).isAfterNow();
		assertThrown(()->test(x1).isAfterNow()).contains("Value was not after expected.");

		assertThrown(()->test(empty()).isBetween(x1,x2)).is("Value was null.");
		assertThrown(()->test(now).isBetween(null,x2)).is("Parameter 'lower' cannot be null.");
		assertThrown(()->test(now).isBetween(x1,null)).is("Parameter 'upper' cannot be null.");
		test(now).isBetween(x1, x2);
		assertThrown(()->test(x1).isBetween(now,x2)).contains("Value was not after expected.");
		assertThrown(()->test(x2).isBetween(x1,now)).contains("Value was not before expected.");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test((ZonedDateTime)null).msg("Foo {0}", 1).exists()).is("Foo 1");
		test((ZonedDateTime)null).stdout();
	}
}
