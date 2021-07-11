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
import org.apache.juneau.testutils.*;
import org.apache.juneau.transform.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
public class TemporalDateSwapTest extends RoundTripStringSwapTest<Date> {

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
		BS_PST = BeanContext.DEFAULT.createBeanSession(BeanSessionArgs.create().timeZone(TimeZone.getTimeZone("PST")));

	private static Date T_Date = Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2012-12-21T12:34:56Z")));

	public TemporalDateSwapTest(String label, Date o, StringSwap<Date> s, String r, BeanSession bs) throws Exception {
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
				T_Date,
				new TemporalDateSwap.BasicIsoDate(),
				"20121221-0500",
				BS_DEFAULT
			},
			{
				"[1] BasicIsoDate, Pacific timezone",
				T_Date,
				new TemporalDateSwap.BasicIsoDate(),
				"20121221-0800",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[2] IsoDate, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[3] IsoDate, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoDate(),
				"2012-12-21-08:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoDateTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[4] IsoDateTime, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoDateTime(),
				"2012-12-21T07:34:56-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[5] IsoDateTime, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoDateTime(),
				"2012-12-21T04:34:56-08:00[America/Los_Angeles]",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoInstant
			//----------------------------------------------------------------------------------------------------------
			{
				"[6] IsoInstant, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoInstant(),
				"2012-12-21T12:34:56Z",
				BS_DEFAULT
			},
			{
				"[7] IsoInstant, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoInstant(),
				"2012-12-21T12:34:56Z",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[8] IsoLocalDate, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[9] IsoLocalDate, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalDateTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[10] IsoLocalDateTime, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoLocalDateTime(),
				"2012-12-21T07:34:56",
				BS_DEFAULT
			},
			{
				"[11] IsoLocalDateTime, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoLocalDateTime(),
				"2012-12-21T04:34:56",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[12] IsoLocalTime, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoLocalTime(),
				"07:34:56",
				BS_DEFAULT
			},
			{
				"[13] IsoLocalTime, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoLocalTime(),
				"04:34:56",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[14] IsoOffsetDate, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[15] IsoOffsetDate, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoOffsetDate(),
				"2012-12-21-08:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetDateTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[16] IsoOffsetDateTime, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoOffsetDateTime(),
				"2012-12-21T07:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[17] IsoOffsetDateTime, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoOffsetDateTime(),
				"2012-12-21T04:34:56-08:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[18] IsoOffsetTime, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoOffsetTime(),
				"07:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[19] IsoOffsetTime, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoOffsetTime(),
				"04:34:56-08:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOrdinalDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[20] IsoOrdinalDate, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoOrdinalDate(),
				"2012-356-05:00",
				BS_DEFAULT
			},
			{
				"[21] IsoOrdinalDate, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoOrdinalDate(),
				"2012-356-08:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[22] IsoTime, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoTime(),
				"07:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[23] IsoTime, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoTime(),
				"04:34:56-08:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoWeekDate
			//----------------------------------------------------------------------------------------------------------
			{
				"[24] IsoWeekDate, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoWeekDate(),
				"2012-W51-5-05:00",
				BS_DEFAULT
			},
			{
				"[25] IsoWeekDate, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoWeekDate(),
				"2012-W51-5-08:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoZonedDateTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[26] IsoZonedDateTime, Default timezone",
				T_Date,
				new TemporalDateSwap.IsoZonedDateTime(),
				"2012-12-21T07:34:56-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[27] IsoZonedDateTime, Pacific timezone",
				T_Date,
				new TemporalDateSwap.IsoZonedDateTime(),
				"2012-12-21T04:34:56-08:00[America/Los_Angeles]",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// Rfc1123DateTime
			//----------------------------------------------------------------------------------------------------------
			{
				"[28] Rfc1123DateTime, Default timezone",
				T_Date,
				new TemporalDateSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 07:34:56 -0500",
				BS_DEFAULT
			},
			{
				"[29] Rfc1123DateTime, Pacific timezone",
				T_Date,
				new TemporalDateSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 04:34:56 -0800",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// Custom, Year
			//----------------------------------------------------------------------------------------------------------
			{
				"[30] YEAR, Default timezone",
				T_Date,
				new TemporalDateSwap("uuuu"),
				"2012",
				BS_DEFAULT
			},
			{
				"[31] YEAR, Pacific timezone",
				T_Date,
				new TemporalDateSwap("uuuu"),
				"2012",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// Edge cases
			//----------------------------------------------------------------------------------------------------------
			{
				"[32] BasicIsoDate, null",
				null,
				new TemporalDateSwap.BasicIsoDate(),
				null,
				BS_DEFAULT
			},

		});
	}
}