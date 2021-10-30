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

import org.apache.juneau.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;
import org.apache.juneau.testutils.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
public class TemporalCalendarSwapTest extends RoundTripStringSwapTest<Calendar> {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	@BeforeClass
	public static void beforeClass() {
		TestUtils.setTimeZone("GMT-5");
	}

	@AfterClass
	public static void afterClass() {
		TestUtils.unsetTimeZone();
	}

	private static BeanSession
		BS_DEFAULT = BeanContext.DEFAULT_SESSION,
		BS_PST = BeanContext.DEFAULT.createSession().timeZone(TimeZone.getTimeZone("PST")).build();

	private static GregorianCalendar T_Calendar = GregorianCalendar.from(ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse("2012-12-21T12:34:56Z")));
	static {
		T_Calendar.setTimeZone(TimeZone.getTimeZone("GMT-5"));
	}

	public TemporalCalendarSwapTest(String label, Calendar o, StringSwap<Calendar> s, String r, BeanSession bs) throws Exception {
		super(label, o, s, r, bs);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Parameters
	//------------------------------------------------------------------------------------------------------------------

	@Parameterized.Parameters
	public static Collection<Object[]> getPairs() {
		return Arrays.asList(new Object[][] {

			//----------------------------------------------------------------------------------------------------------
			// BasicIsoDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[0] BasicIsoDate, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.BasicIsoDate(),
				"20121221-0500",
				BS_DEFAULT
			},
			{
				"[1] BasicIsoDate, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.BasicIsoDate(),
				"20121221-0500",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[2] IsoDate, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[3] IsoDate, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoDate(),
				"2012-12-21-05:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoDateTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[4] IsoDateTime, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoDateTime(),
				"2012-12-21T07:34:56-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[5] IsoDateTime, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoDateTime(),
				"2012-12-21T07:34:56-05:00[GMT-05:00]",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoInstant
			//----------------------------------------------------------------------------------------------------------
			{
				"[6] IsoInstant, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoInstant(),
				"2012-12-21T12:34:56Z",
				BS_DEFAULT
			},
			{
				"[7] IsoInstant, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoInstant(),
				"2012-12-21T12:34:56Z",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[8] IsoLocalDate, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[9] IsoLocalDate, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalDateTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[10] IsoLocalDateTime, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoLocalDateTime(),
				"2012-12-21T07:34:56",
				BS_DEFAULT
			},
			{
				"[11] IsoLocalDateTime, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoLocalDateTime(),
				"2012-12-21T07:34:56",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[12] IsoLocalTime, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoLocalTime(),
				"07:34:56",
				BS_DEFAULT
			},
			{
				"[13] IsoLocalTime, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoLocalTime(),
				"07:34:56",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[14] IsoOffsetDate, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[15] IsoOffsetDate, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetDateTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[16] IsoOffsetDateTime, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoOffsetDateTime(),
				"2012-12-21T07:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[17] IsoOffsetDateTime, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoOffsetDateTime(),
				"2012-12-21T07:34:56-05:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[18] IsoOffsetTime, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoOffsetTime(),
				"07:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[19] IsoOffsetTime, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoOffsetTime(),
				"07:34:56-05:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOrdinalDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[20] IsoOrdinalDate, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoOrdinalDate(),
				"2012-356-05:00",
				BS_DEFAULT
			},
			{
				"[21] IsoOrdinalDate, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoOrdinalDate(),
				"2012-356-05:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[22] IsoTime, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoTime(),
				"07:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[23] IsoTime, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoTime(),
				"07:34:56-05:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoWeekDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[24] IsoWeekDate, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoWeekDate(),
				"2012-W51-5-05:00",
				BS_DEFAULT
			},
			{
				"[25] IsoWeekDate, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoWeekDate(),
				"2012-W51-5-05:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoZonedDateTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[26] IsoZonedDateTime, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoZonedDateTime(),
				"2012-12-21T07:34:56-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[27] IsoZonedDateTime, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.IsoZonedDateTime(),
				"2012-12-21T07:34:56-05:00[GMT-05:00]",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// Rfc1123DateTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[28] Rfc1123DateTime, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 07:34:56 -0500",
				BS_DEFAULT
			},
			{
				"[29] Rfc1123DateTime, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 07:34:56 -0500",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// Custom, Year
			//----------------------------------------------------------------------------------------------------------
			{
				"[30] YEAR, Default timezone",
				T_Calendar,
				new TemporalCalendarSwap("uuuu"),
				"2012",
				BS_DEFAULT
			},
			{
				"[31] YEAR, Pacific timezone",
				T_Calendar,
				new TemporalCalendarSwap("uuuu"),
				"2012",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// Edge cases
			//----------------------------------------------------------------------------------------------------------
			{
				"[32] BasicIsoDate, null",
				null,
				new TemporalCalendarSwap.BasicIsoDate(),
				null,
				BS_DEFAULT
			},

		});
	}
}