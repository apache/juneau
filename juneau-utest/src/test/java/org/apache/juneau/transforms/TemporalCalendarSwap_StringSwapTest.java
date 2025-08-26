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
package org.apache.juneau.transforms;

import java.time.*;
import java.time.format.*;
import java.util.*;
import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;
import org.junit.jupiter.api.*;

class TemporalCalendarSwap_StringSwapTest extends StringSwapTest_Base {

	@BeforeAll
	static void beforeClass() {
		setTimeZone("GMT-5");
	}

	@AfterAll
	static void afterClass() {
		unsetTimeZone();
	}

	private static BeanSession
		BS_DEFAULT = BeanContext.DEFAULT_SESSION,
		BS_PST = BeanContext.DEFAULT.createSession().timeZone(TimeZone.getTimeZone("PST")).build();

	private static GregorianCalendar T_Calendar = GregorianCalendar.from(ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse("2012-12-21T12:34:56Z")));
	static {
		T_Calendar.setTimeZone(TimeZone.getTimeZone("GMT-5"));
	}

	private static <T> StringSwapTester<T> tester(int index, String label, T object, StringSwap<T> swap, String expected, BeanSession bs) {
		return StringSwapTester.create(index, label, object, swap, expected, bs).build();
	}

	private static final StringSwapTester<?>[] TESTERS = {
		tester(1, "BasicIsoDate, Default timezone", T_Calendar, new TemporalCalendarSwap.BasicIsoDate(), "20121221-0500", BS_DEFAULT),
		tester(2, "BasicIsoDate, Pacific timezone", T_Calendar, new TemporalCalendarSwap.BasicIsoDate(), "20121221-0500", BS_PST),
		tester(3, "IsoDate, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoDate(), "2012-12-21-05:00", BS_DEFAULT),
		tester(4, "IsoDate, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoDate(), "2012-12-21-05:00", BS_PST),
		tester(5, "IsoDateTime, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoDateTime(), "2012-12-21T07:34:56-05:00[GMT-05:00]", BS_DEFAULT),
		tester(6, "IsoDateTime, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoDateTime(), "2012-12-21T07:34:56-05:00[GMT-05:00]", BS_PST),
		tester(7, "IsoInstant, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoInstant(), "2012-12-21T12:34:56Z", BS_DEFAULT),
		tester(8, "IsoInstant, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoInstant(), "2012-12-21T12:34:56Z", BS_PST),
		tester(9, "IsoLocalDate, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoLocalDate(), "2012-12-21", BS_DEFAULT),
		tester(10, "IsoLocalDate, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoLocalDate(), "2012-12-21", BS_PST),
		tester(11, "IsoLocalDateTime, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoLocalDateTime(), "2012-12-21T07:34:56", BS_DEFAULT),
		tester(12, "IsoLocalDateTime, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoLocalDateTime(), "2012-12-21T07:34:56", BS_PST),
		tester(13, "IsoLocalTime, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoLocalTime(), "07:34:56", BS_DEFAULT),
		tester(14, "IsoLocalTime, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoLocalTime(), "07:34:56", BS_PST),
		tester(15, "IsoOffsetDate, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoOffsetDate(), "2012-12-21-05:00", BS_DEFAULT),
		tester(16, "IsoOffsetDate, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoOffsetDate(), "2012-12-21-05:00", BS_PST),
		tester(17, "IsoOffsetDateTime, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoOffsetDateTime(), "2012-12-21T07:34:56-05:00", BS_DEFAULT),
		tester(18, "IsoOffsetDateTime, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoOffsetDateTime(), "2012-12-21T07:34:56-05:00", BS_PST),
		tester(19, "IsoOffsetTime, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoOffsetTime(), "07:34:56-05:00", BS_DEFAULT),
		tester(20, "IsoOffsetTime, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoOffsetTime(), "07:34:56-05:00", BS_PST),
		tester(21, "IsoOrdinalDate, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoOrdinalDate(), "2012-356-05:00", BS_DEFAULT),
		tester(22, "IsoOrdinalDate, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoOrdinalDate(), "2012-356-05:00", BS_PST),
		tester(23, "IsoTime, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoTime(), "07:34:56-05:00", BS_DEFAULT),
		tester(24, "IsoTime, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoTime(), "07:34:56-05:00", BS_PST),
		tester(25, "IsoWeekDate, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoWeekDate(), "2012-W51-5-05:00", BS_DEFAULT),
		tester(26, "IsoWeekDate, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoWeekDate(), "2012-W51-5-05:00", BS_PST),
		tester(27, "IsoZonedDateTime, Default timezone", T_Calendar, new TemporalCalendarSwap.IsoZonedDateTime(), "2012-12-21T07:34:56-05:00[GMT-05:00]", BS_DEFAULT),
		tester(28, "IsoZonedDateTime, Pacific timezone", T_Calendar, new TemporalCalendarSwap.IsoZonedDateTime(), "2012-12-21T07:34:56-05:00[GMT-05:00]", BS_PST),
		tester(29, "Rfc1123DateTime, Default timezone", T_Calendar, new TemporalCalendarSwap.Rfc1123DateTime(), "Fri, 21 Dec 2012 07:34:56 -0500", BS_DEFAULT),
		tester(30, "Rfc1123DateTime, Pacific timezone", T_Calendar, new TemporalCalendarSwap.Rfc1123DateTime(), "Fri, 21 Dec 2012 07:34:56 -0500", BS_PST),
		tester(31, "YEAR, Default timezone", T_Calendar, new TemporalCalendarSwap("uuuu"), "2012", BS_DEFAULT),
		tester(32, "YEAR, Pacific timezone", T_Calendar, new TemporalCalendarSwap("uuuu"), "2012", BS_DEFAULT),
		tester(33, "BasicIsoDate, null", null, new TemporalCalendarSwap.BasicIsoDate(), null, BS_DEFAULT)
	};

	static StringSwapTester<?>[] testers() {
		return TESTERS;
	}
}