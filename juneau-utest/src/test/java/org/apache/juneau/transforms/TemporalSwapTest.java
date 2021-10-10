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
import java.time.chrono.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.transform.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
public class TemporalSwapTest extends RoundTripStringSwapTest<Temporal> {

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

	private static Temporal
		DT_ZonedDateTime = ZonedDateTime.parse("2012-12-21T12:34:56Z"),
		DT_Instant = Instant.parse("2012-12-21T12:34:56Z"),
		DT_LocalDate = LocalDate.parse("2012-12-21"),
		DT_LocalDateTime = LocalDateTime.parse("2012-12-21T12:34:56"),
		DT_LocalTime = LocalTime.parse("12:34:56"),
		DT_OffsetDateTime = OffsetDateTime.parse("2012-12-21T12:34:56-05:00"),
		DT_OffsetTime = OffsetTime.parse("12:34:56-05:00"),
		DT_Year = Year.parse("2012"),
		DT_YearMonth = YearMonth.parse("2012-12");

	private static Temporal
		DT_HijrahDate = HijrahDate.from(DT_ZonedDateTime),
		DT_JapaneseDate = JapaneseDate.from(DT_ZonedDateTime),
		DT_MinguoDate = MinguoDate.from(DT_ZonedDateTime),
		DT_ThaiBuddhistDate = ThaiBuddhistDate.from(DT_ZonedDateTime);

	public TemporalSwapTest(String label, Temporal o, StringSwap<Temporal> s, String r, BeanSession bs) throws Exception {
		super(label, o, s, r, bs);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Parameters
	//------------------------------------------------------------------------------------------------------------------

	@Parameterized.Parameters
	public static Collection<Object[]> getPairs() {
		return Arrays.asList(new Object[][] {

			//----------------------------------------------------------------------------------------------------------
			// BasicIsoDate, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[0] BasicIsoDate, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.BasicIsoDate(),
				"20121221Z",
				BS_DEFAULT
			},
			{
				"[1] BasicIsoDate, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.BasicIsoDate(),
				"20121221Z",
				BS_DEFAULT
			},
			{
				"[2] BasicIsoDate, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_DEFAULT
			},
			{
				"[3] BasicIsoDate, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_DEFAULT
			},
			{
				"[4] BasicIsoDate, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.BasicIsoDate(),
				"19700101",
				BS_DEFAULT
			},
			{
				"[5] BasicIsoDate, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.BasicIsoDate(),
				"20121221-0500",
				BS_DEFAULT
			},
			{
				"[6] BasicIsoDate, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.BasicIsoDate(),
				"19700101-0500",
				BS_DEFAULT
			},
			{
				"[7] BasicIsoDate, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.BasicIsoDate(),
				"20120101",
				BS_DEFAULT
			},
			{
				"[8] BasicIsoDate, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.BasicIsoDate(),
				"20121201",
				BS_DEFAULT
			},
			{
				"[9] BasicIsoDate, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_DEFAULT
			},
			{
				"[10] BasicIsoDate, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_DEFAULT
			},
			{
				"[11] BasicIsoDate, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_DEFAULT
			},
			{
				"[12] BasicIsoDate, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// BasicIsoDate, PST timezone
			//----------------------------------------------------------------------------------------------------------

			{
				"[13] BasicIsoDate, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.BasicIsoDate(),
				"20121221Z",
				BS_PST
			},
			{
				"[14] BasicIsoDate, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.BasicIsoDate(),
				"20121221Z",
				BS_PST
			},
			{
				"[15] BasicIsoDate, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_PST
			},
			{
				"[16] BasicIsoDate, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_PST
			},
			{
				"[17] BasicIsoDate, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.BasicIsoDate(),
				"19700101",
				BS_PST
			},
			{
				"[18] BasicIsoDate, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.BasicIsoDate(),
				"20121221-0500",
				BS_PST
			},
			{
				"[19] BasicIsoDate, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.BasicIsoDate(),
				"19700101-0500",
				BS_PST
			},
			{
				"[20] BasicIsoDate, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.BasicIsoDate(),
				"20120101",
				BS_PST
			},
			{
				"[21] BasicIsoDate, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.BasicIsoDate(),
				"20121201",
				BS_PST
			},
			{
				"[22] BasicIsoDate, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_PST
			},
			{
				"[23] BasicIsoDate, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_PST
			},
			{
				"[24] BasicIsoDate, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_PST
			},
			{
				"[25] BasicIsoDate, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.BasicIsoDate(),
				"20121221",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoDate, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[26] IsoDate, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoDate(),
				"2012-12-21Z",
				BS_DEFAULT
			},
			{
				"[27] IsoDate, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoDate(),
				"2012-12-21Z",
				BS_DEFAULT
			},
			{
				"[28] IsoDate, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[29] IsoDate, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[30] IsoDate, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoDate(),
				"1970-01-01",
				BS_DEFAULT
			},
			{
				"[31] IsoDate, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[32] IsoDate, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoDate(),
				"1970-01-01-05:00",
				BS_DEFAULT
			},
			{
				"[33] IsoDate, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoDate(),
				"2012-01-01",
				BS_DEFAULT
			},
			{
				"[34] IsoDate, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoDate(),
				"2012-12-01",
				BS_DEFAULT
			},
			{
				"[35] IsoDate, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[36] IsoDate, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[37] IsoDate, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[38] IsoDate, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoDate, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[39] IsoDate, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoDate(),
				"2012-12-21Z",
				BS_PST
			},
			{
				"[40] IsoDate, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoDate(),
				"2012-12-21Z",
				BS_PST
			},
			{
				"[41] IsoDate, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[42] IsoDate, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[43] IsoDate, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoDate(),
				"1970-01-01",
				BS_PST
			},
			{
				"[44] IsoDate, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoDate(),
				"2012-12-21-05:00",
				BS_PST
			},
			{
				"[45] IsoDate, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoDate(),
				"1970-01-01-05:00",
				BS_PST
			},
			{
				"[46] IsoDate, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoDate(),
				"2012-01-01",
				BS_PST
			},
			{
				"[47] IsoDate, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoDate(),
				"2012-12-01",
				BS_PST
			},
			{
				"[48] IsoDate, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[49] IsoDate, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[50] IsoDate, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[51] IsoDate, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoDate(),
				"2012-12-21",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoDateTime, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[52] IsoDateTime, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T12:34:56Z",
				BS_DEFAULT
			},
			{
				"[53] IsoDateTime, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T12:34:56Z",
				BS_DEFAULT
			},
			{
				"[54] IsoDateTime, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T00:00:00",
				BS_DEFAULT
			},
			{
				"[55] IsoDateTime, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T12:34:56",
				BS_DEFAULT
			},
			{
				"[56] IsoDateTime, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoDateTime(),
				"1970-01-01T12:34:56",
				BS_DEFAULT
			},
			{
				"[57] IsoDateTime, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[58] IsoDateTime, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoDateTime(),
				"1970-01-01T12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[59] IsoDateTime, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoDateTime(),
				"2012-01-01T00:00:00",
				BS_DEFAULT
			},
			{
				"[60] IsoDateTime, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoDateTime(),
				"2012-12-01T00:00:00",
				BS_DEFAULT
			},
			{
				"[61] IsoDateTime, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T00:00:00",
				BS_DEFAULT
			},
			{
				"[62] IsoDateTime, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T00:00:00",
				BS_DEFAULT
			},
			{
				"[63] IsoDateTime, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T00:00:00",
				BS_DEFAULT
			},
			{
				"[64] IsoDateTime, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T00:00:00",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoDateTime, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[65] IsoDateTime, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T12:34:56Z",
				BS_PST
			},
			{
				"[66] IsoDateTime, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T12:34:56Z",
				BS_PST
			},
			{
				"[67] IsoDateTime, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T00:00:00",
				BS_PST
			},
			{
				"[68] IsoDateTime, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T12:34:56",
				BS_PST
			},
			{
				"[69] IsoDateTime, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoDateTime(),
				"1970-01-01T12:34:56",
				BS_PST
			},
			{
				"[70] IsoDateTime, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T12:34:56-05:00",
				BS_PST
			},
			{
				"[71] IsoDateTime, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoDateTime(),
				"1970-01-01T12:34:56-05:00",
				BS_PST
			},
			{
				"[72] IsoDateTime, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoDateTime(),
				"2012-01-01T00:00:00",
				BS_PST
			},
			{
				"[73] IsoDateTime, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoDateTime(),
				"2012-12-01T00:00:00",
				BS_PST
			},
			{
				"[74] IsoDateTime, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T00:00:00",
				BS_PST
			},
			{
				"[75] IsoDateTime, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T00:00:00",
				BS_PST
			},
			{
				"[76] IsoDateTime, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T00:00:00",
				BS_PST
			},
			{
				"[77] IsoDateTime, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoDateTime(),
				"2012-12-21T00:00:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoInstant, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[78] IsoInstant, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T12:34:56Z",
				BS_DEFAULT
			},
			{
				"[79] IsoInstant, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T12:34:56Z",
				BS_DEFAULT
			},
			{
				"[80] IsoInstant, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T05:00:00Z",
				BS_DEFAULT
			},
			{
				"[81] IsoInstant, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T17:34:56Z",
				BS_DEFAULT
			},
			{
				"[82] IsoInstant, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoInstant(),
				"1970-01-01T17:34:56Z",
				BS_DEFAULT
			},
			{
				"[83] IsoInstant, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T17:34:56Z",
				BS_DEFAULT
			},
			{
				"[84] IsoInstant, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoInstant(),
				"1970-01-01T17:34:56Z",
				BS_DEFAULT
			},
			{
				"[85] IsoInstant, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoInstant(),
				"2012-01-01T05:00:00Z",
				BS_DEFAULT
			},
			{
				"[86] IsoInstant, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoInstant(),
				"2012-12-01T05:00:00Z",
				BS_DEFAULT
			},
			{
				"[87] IsoInstant, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T05:00:00Z",
				BS_DEFAULT
			},
			{
				"[88] IsoInstant, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T05:00:00Z",
				BS_DEFAULT
			},
			{
				"[89] IsoInstant, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T05:00:00Z",
				BS_DEFAULT
			},
			{
				"[90] IsoInstant, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T05:00:00Z",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoInstant, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[91] IsoInstant, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T12:34:56Z",
				BS_PST
			},
			{
				"[92] IsoInstant, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T12:34:56Z",
				BS_PST
			},
			{
				"[93] IsoInstant, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T08:00:00Z",
				BS_PST
			},
			{
				"[94] IsoInstant, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T20:34:56Z",
				BS_PST
			},
			{
				"[95] IsoInstant, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoInstant(),
				"1970-01-01T20:34:56Z",
				BS_PST
			},
			{
				"[96] IsoInstant, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T17:34:56Z",
				BS_PST
			},
			{
				"[97] IsoInstant, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoInstant(),
				"1970-01-01T17:34:56Z",
				BS_PST
			},
			{
				"[98] IsoInstant, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoInstant(),
				"2012-01-01T08:00:00Z",
				BS_PST
			},
			{
				"[99] IsoInstant, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoInstant(),
				"2012-12-01T08:00:00Z",
				BS_PST
			},
			{
				"[100] IsoInstant, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T08:00:00Z",
				BS_PST
			},
			{
				"[101] IsoInstant, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T08:00:00Z",
				BS_PST
			},
			{
				"[102] IsoInstant, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T08:00:00Z",
				BS_PST
			},
			{
				"[103] IsoInstant, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoInstant(),
				"2012-12-21T08:00:00Z",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalDate, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[104] IsoLocalDate, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[105] IsoLocalDate, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[106] IsoLocalDate, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[107] IsoLocalDate, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[108] IsoLocalDate, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoLocalDate(),
				"1970-01-01",
				BS_DEFAULT
			},
			{
				"[109] IsoLocalDate, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[110] IsoLocalDate, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoLocalDate(),
				"1970-01-01",
				BS_DEFAULT
			},
			{
				"[111] IsoLocalDate, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoLocalDate(),
				"2012-01-01",
				BS_DEFAULT
			},
			{
				"[112] IsoLocalDate, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-01",
				BS_DEFAULT
			},
			{
				"[113] IsoLocalDate, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[114] IsoLocalDate, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[115] IsoLocalDate, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},
			{
				"[116] IsoLocalDate, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalDate, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[117] IsoLocalDate, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[118] IsoLocalDate, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[119] IsoLocalDate, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[120] IsoLocalDate, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[121] IsoLocalDate, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoLocalDate(),
				"1970-01-01",
				BS_PST
			},
			{
				"[122] IsoLocalDate, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[123] IsoLocalDate, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoLocalDate(),
				"1970-01-01",
				BS_PST
			},
			{
				"[124] IsoLocalDate, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoLocalDate(),
				"2012-01-01",
				BS_PST
			},
			{
				"[125] IsoLocalDate, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-01",
				BS_PST
			},
			{
				"[126] IsoLocalDate, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[127] IsoLocalDate, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[128] IsoLocalDate, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},
			{
				"[129] IsoLocalDate, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoLocalDate(),
				"2012-12-21",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalDateTime, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[130] IsoLocalDateTime, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T12:34:56",
				BS_DEFAULT
			},
			{
				"[131] IsoLocalDateTime, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T12:34:56",
				BS_DEFAULT
			},
			{
				"[132] IsoLocalDateTime, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T00:00:00",
				BS_DEFAULT
			},
			{
				"[133] IsoLocalDateTime, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T12:34:56",
				BS_DEFAULT
			},
			{
				"[134] IsoLocalDateTime, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoLocalDateTime(),
				"1970-01-01T12:34:56",
				BS_DEFAULT
			},
			{
				"[135] IsoLocalDateTime, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T12:34:56",
				BS_DEFAULT
			},
			{
				"[136] IsoLocalDateTime, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoLocalDateTime(),
				"1970-01-01T12:34:56",
				BS_DEFAULT
			},
			{
				"[137] IsoLocalDateTime, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-01-01T00:00:00",
				BS_DEFAULT
			},
			{
				"[138] IsoLocalDateTime, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-01T00:00:00",
				BS_DEFAULT
			},
			{
				"[139] IsoLocalDateTime, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T00:00:00",
				BS_DEFAULT
			},
			{
				"[140] IsoLocalDateTime, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T00:00:00",
				BS_DEFAULT
			},
			{
				"[141] IsoLocalDateTime, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T00:00:00",
				BS_DEFAULT
			},
			{
				"[142] IsoLocalDateTime, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T00:00:00",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalDateTime, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[143] IsoLocalDateTime, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T12:34:56",
				BS_PST
			},
			{
				"[144] IsoLocalDateTime, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T12:34:56",
				BS_PST
			},
			{
				"[145] IsoLocalDateTime, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T00:00:00",
				BS_PST
			},
			{
				"[146] IsoLocalDateTime, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T12:34:56",
				BS_PST
			},
			{
				"[147] IsoLocalDateTime, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoLocalDateTime(),
				"1970-01-01T12:34:56",
				BS_PST
			},
			{
				"[148] IsoLocalDateTime, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T12:34:56",
				BS_PST
			},
			{
				"[149] IsoLocalDateTime, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoLocalDateTime(),
				"1970-01-01T12:34:56",
				BS_PST
			},
			{
				"[150] IsoLocalDateTime, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-01-01T00:00:00",
				BS_PST
			},
			{
				"[151] IsoLocalDateTime, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-01T00:00:00",
				BS_PST
			},
			{
				"[152] IsoLocalDateTime, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T00:00:00",
				BS_PST
			},
			{
				"[153] IsoLocalDateTime, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T00:00:00",
				BS_PST
			},
			{
				"[154] IsoLocalDateTime, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T00:00:00",
				BS_PST
			},
			{
				"[155] IsoLocalDateTime, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoLocalDateTime(),
				"2012-12-21T00:00:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalTime, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[156] IsoLocalTime, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_DEFAULT
			},
			{
				"[157] IsoLocalTime, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_DEFAULT
			},
			{
				"[158] IsoLocalTime, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[159] IsoLocalTime, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_DEFAULT
			},
			{
				"[160] IsoLocalTime, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_DEFAULT
			},
			{
				"[161] IsoLocalTime, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_DEFAULT
			},
			{
				"[162] IsoLocalTime, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_DEFAULT
			},
			{
				"[163] IsoLocalTime, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[164] IsoLocalTime, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[165] IsoLocalTime, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[166] IsoLocalTime, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[167] IsoLocalTime, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[168] IsoLocalTime, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoLocalTime, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[169] IsoLocalTime, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_PST
			},
			{
				"[170] IsoLocalTime, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_PST
			},
			{
				"[171] IsoLocalTime, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[172] IsoLocalTime, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_PST
			},
			{
				"[173] IsoLocalTime, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_PST
			},
			{
				"[174] IsoLocalTime, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_PST
			},
			{
				"[175] IsoLocalTime, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoLocalTime(),
				"12:34:56",
				BS_PST
			},
			{
				"[176] IsoLocalTime, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[177] IsoLocalTime, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[178] IsoLocalTime, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[179] IsoLocalTime, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[180] IsoLocalTime, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[181] IsoLocalTime, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoLocalTime(),
				"00:00:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetDate, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[182] IsoOffsetDate, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21Z",
				BS_DEFAULT
			},
			{
				"[183] IsoOffsetDate, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21Z",
				BS_DEFAULT
			},
			{
				"[184] IsoOffsetDate, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[185] IsoOffsetDate, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[186] IsoOffsetDate, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoOffsetDate(),
				"1970-01-01-05:00",
				BS_DEFAULT
			},
			{
				"[187] IsoOffsetDate, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[188] IsoOffsetDate, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoOffsetDate(),
				"1970-01-01-05:00",
				BS_DEFAULT
			},
			{
				"[189] IsoOffsetDate, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoOffsetDate(),
				"2012-01-01-05:00",
				BS_DEFAULT
			},
			{
				"[190] IsoOffsetDate, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-01-05:00",
				BS_DEFAULT
			},
			{
				"[191] IsoOffsetDate, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[192] IsoOffsetDate, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[193] IsoOffsetDate, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},
			{
				"[194] IsoOffsetDate, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetDate, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[195] IsoOffsetDate, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21Z",
				BS_PST
			},
			{
				"[196] IsoOffsetDate, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21Z",
				BS_PST
			},
			{
				"[197] IsoOffsetDate, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-08:00",
				BS_PST
			},
			{
				"[198] IsoOffsetDate, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-08:00",
				BS_PST
			},
			{
				"[199] IsoOffsetDate, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoOffsetDate(),
				"1970-01-01-08:00",
				BS_PST
			},
			{
				"[200] IsoOffsetDate, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-05:00",
				BS_PST
			},
			{
				"[201] IsoOffsetDate, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoOffsetDate(),
				"1970-01-01-05:00",
				BS_PST
			},
			{
				"[202] IsoOffsetDate, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoOffsetDate(),
				"2012-01-01-08:00",
				BS_PST
			},
			{
				"[203] IsoOffsetDate, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-01-08:00",
				BS_PST
			},
			{
				"[204] IsoOffsetDate, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-08:00",
				BS_PST
			},
			{
				"[205] IsoOffsetDate, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-08:00",
				BS_PST
			},
			{
				"[206] IsoOffsetDate, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-08:00",
				BS_PST
			},
			{
				"[207] IsoOffsetDate, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoOffsetDate(),
				"2012-12-21-08:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetDateTime, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[208] IsoOffsetDateTime, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T12:34:56Z",
				BS_DEFAULT
			},
			{
				"[209] IsoOffsetDateTime, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T12:34:56Z",
				BS_DEFAULT
			},
			{
				"[210] IsoOffsetDateTime, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[211] IsoOffsetDateTime, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[212] IsoOffsetDateTime, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoOffsetDateTime(),
				"1970-01-01T12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[213] IsoOffsetDateTime, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[214] IsoOffsetDateTime, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoOffsetDateTime(),
				"1970-01-01T12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[215] IsoOffsetDateTime, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-01-01T00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[216] IsoOffsetDateTime, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-01T00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[217] IsoOffsetDateTime, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[218] IsoOffsetDateTime, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[219] IsoOffsetDateTime, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[220] IsoOffsetDateTime, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T00:00:00-05:00",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetDateTime, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[221] IsoOffsetDateTime, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T12:34:56Z",
				BS_PST
			},
			{
				"[222] IsoOffsetDateTime, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T12:34:56Z",
				BS_PST
			},
			{
				"[223] IsoOffsetDateTime, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T00:00:00-08:00",
				BS_PST
			},
			{
				"[224] IsoOffsetDateTime, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T12:34:56-08:00",
				BS_PST
			},
			{
				"[225] IsoOffsetDateTime, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoOffsetDateTime(),
				"1970-01-01T12:34:56-08:00",
				BS_PST
			},
			{
				"[226] IsoOffsetDateTime, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T12:34:56-05:00",
				BS_PST
			},
			{
				"[227] IsoOffsetDateTime, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoOffsetDateTime(),
				"1970-01-01T12:34:56-05:00",
				BS_PST
			},
			{
				"[228] IsoOffsetDateTime, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-01-01T00:00:00-08:00",
				BS_PST
			},
			{
				"[229] IsoOffsetDateTime, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-01T00:00:00-08:00",
				BS_PST
			},
			{
				"[230] IsoOffsetDateTime, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T00:00:00-08:00",
				BS_PST
			},
			{
				"[231] IsoOffsetDateTime, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T00:00:00-08:00",
				BS_PST
			},
			{
				"[232] IsoOffsetDateTime, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T00:00:00-08:00",
				BS_PST
			},
			{
				"[233] IsoOffsetDateTime, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoOffsetDateTime(),
				"2012-12-21T00:00:00-08:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetTime, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[234] IsoOffsetTime, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56Z",
				BS_DEFAULT
			},
			{
				"[235] IsoOffsetTime, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56Z",
				BS_DEFAULT
			},
			{
				"[236] IsoOffsetTime, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[237] IsoOffsetTime, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[238] IsoOffsetTime, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[239] IsoOffsetTime, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[240] IsoOffsetTime, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[241] IsoOffsetTime, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[242] IsoOffsetTime, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[243] IsoOffsetTime, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[244] IsoOffsetTime, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[245] IsoOffsetTime, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-05:00",
				BS_DEFAULT
			},
			{
				"[246] IsoOffsetTime, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-05:00",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOffsetTime, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[247] IsoOffsetTime, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56Z",
				BS_PST
			},
			{
				"[248] IsoOffsetTime, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56Z",
				BS_PST
			},
			{
				"[249] IsoOffsetTime, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-08:00",
				BS_PST
			},
			{
				"[250] IsoOffsetTime, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56-08:00",
				BS_PST
			},
			{
				"[251] IsoOffsetTime, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56-08:00",
				BS_PST
			},
			{
				"[252] IsoOffsetTime, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56-05:00",
				BS_PST
			},
			{
				"[253] IsoOffsetTime, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoOffsetTime(),
				"12:34:56-05:00",
				BS_PST
			},
			{
				"[254] IsoOffsetTime, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-08:00",
				BS_PST
			},
			{
				"[255] IsoOffsetTime, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-08:00",
				BS_PST
			},
			{
				"[256] IsoOffsetTime, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-08:00",
				BS_PST
			},
			{
				"[257] IsoOffsetTime, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-08:00",
				BS_PST
			},
			{
				"[258] IsoOffsetTime, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-08:00",
				BS_PST
			},
			{
				"[259] IsoOffsetTime, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoOffsetTime(),
				"00:00:00-08:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOrdinalDate, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[260] IsoOrdinalDate, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356Z",
				BS_DEFAULT
			},
			{
				"[261] IsoOrdinalDate, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356Z",
				BS_DEFAULT
			},
			{
				"[262] IsoOrdinalDate, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_DEFAULT
			},
			{
				"[263] IsoOrdinalDate, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_DEFAULT
			},
			{
				"[264] IsoOrdinalDate, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoOrdinalDate(),
				"1970-001",
				BS_DEFAULT
			},
			{
				"[265] IsoOrdinalDate, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356-05:00",
				BS_DEFAULT
			},
			{
				"[266] IsoOrdinalDate, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoOrdinalDate(),
				"1970-001-05:00",
				BS_DEFAULT
			},
			{
				"[267] IsoOrdinalDate, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-001",
				BS_DEFAULT
			},
			{
				"[268] IsoOrdinalDate, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-336",
				BS_DEFAULT
			},
			{
				"[269] IsoOrdinalDate, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_DEFAULT
			},
			{
				"[270] IsoOrdinalDate, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_DEFAULT
			},
			{
				"[271] IsoOrdinalDate, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_DEFAULT
			},
			{
				"[272] IsoOrdinalDate, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoOrdinalDate, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[273] IsoOrdinalDate, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356Z",
				BS_PST
			},
			{
				"[274] IsoOrdinalDate, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356Z",
				BS_PST
			},
			{
				"[275] IsoOrdinalDate, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_PST
			},
			{
				"[276] IsoOrdinalDate, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_PST
			},
			{
				"[277] IsoOrdinalDate, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoOrdinalDate(),
				"1970-001",
				BS_PST
			},
			{
				"[278] IsoOrdinalDate, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356-05:00",
				BS_PST
			},
			{
				"[279] IsoOrdinalDate, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoOrdinalDate(),
				"1970-001-05:00",
				BS_PST
			},
			{
				"[280] IsoOrdinalDate, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-001",
				BS_PST
			},
			{
				"[281] IsoOrdinalDate, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-336",
				BS_PST
			},
			{
				"[282] IsoOrdinalDate, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_PST
			},
			{
				"[283] IsoOrdinalDate, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_PST
			},
			{
				"[284] IsoOrdinalDate, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_PST
			},
			{
				"[285] IsoOrdinalDate, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoOrdinalDate(),
				"2012-356",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoTime, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[286] IsoTime, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoTime(),
				"12:34:56Z",
				BS_DEFAULT
			},
			{
				"[287] IsoTime, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoTime(),
				"12:34:56Z",
				BS_DEFAULT
			},
			{
				"[288] IsoTime, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[289] IsoTime, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoTime(),
				"12:34:56",
				BS_DEFAULT
			},
			{
				"[290] IsoTime, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoTime(),
				"12:34:56",
				BS_DEFAULT
			},
			{
				"[291] IsoTime, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoTime(),
				"12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[292] IsoTime, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoTime(),
				"12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[293] IsoTime, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[294] IsoTime, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[295] IsoTime, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[296] IsoTime, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[297] IsoTime, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_DEFAULT
			},
			{
				"[298] IsoTime, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoTime, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[299] IsoTime, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoTime(),
				"12:34:56Z",
				BS_PST
			},
			{
				"[300] IsoTime, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoTime(),
				"12:34:56Z",
				BS_PST
			},
			{
				"[301] IsoTime, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[302] IsoTime, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoTime(),
				"12:34:56",
				BS_PST
			},
			{
				"[303] IsoTime, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoTime(),
				"12:34:56",
				BS_PST
			},
			{
				"[304] IsoTime, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoTime(),
				"12:34:56-05:00",
				BS_PST
			},
			{
				"[305] IsoTime, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoTime(),
				"12:34:56-05:00",
				BS_PST
			},
			{
				"[306] IsoTime, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[307] IsoTime, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[308] IsoTime, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[309] IsoTime, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[310] IsoTime, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_PST
			},
			{
				"[311] IsoTime, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoTime(),
				"00:00:00",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoWeekDate, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[312] IsoWeekDate, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5Z",
				BS_DEFAULT
			},
			{
				"[313] IsoWeekDate, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5Z",
				BS_DEFAULT
			},
			{
				"[314] IsoWeekDate, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_DEFAULT
			},
			{
				"[315] IsoWeekDate, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_DEFAULT
			},
			{
				"[316] IsoWeekDate, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoWeekDate(),
				"1970-W01-4",
				BS_DEFAULT
			},
			{
				"[317] IsoWeekDate, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5-05:00",
				BS_DEFAULT
			},
			{
				"[318] IsoWeekDate, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoWeekDate(),
				"1970-W01-4-05:00",
				BS_DEFAULT
			},
			{
				"[319] IsoWeekDate, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoWeekDate(),
				"2011-W52-7",
				BS_DEFAULT
			},
			{
				"[320] IsoWeekDate, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoWeekDate(),
				"2012-W48-6",
				BS_DEFAULT
			},
			{
				"[321] IsoWeekDate, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_DEFAULT
			},
			{
				"[322] IsoWeekDate, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_DEFAULT
			},
			{
				"[323] IsoWeekDate, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_DEFAULT
			},
			{
				"[324] IsoWeekDate, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoWeekDate, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[325] IsoWeekDate, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5Z",
				BS_PST
			},
			{
				"[326] IsoWeekDate, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5Z",
				BS_PST
			},
			{
				"[327] IsoWeekDate, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_PST
			},
			{
				"[328] IsoWeekDate, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_PST
			},
			{
				"[329] IsoWeekDate, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoWeekDate(),
				"1970-W01-4",
				BS_PST
			},
			{
				"[330] IsoWeekDate, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5-05:00",
				BS_PST
			},
			{
				"[331] IsoWeekDate, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoWeekDate(),
				"1970-W01-4-05:00",
				BS_PST
			},
			{
				"[332] IsoWeekDate, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoWeekDate(),
				"2011-W52-7",
				BS_PST
			},
			{
				"[333] IsoWeekDate, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoWeekDate(),
				"2012-W48-6",
				BS_PST
			},
			{
				"[334] IsoWeekDate, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_PST
			},
			{
				"[335] IsoWeekDate, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_PST
			},
			{
				"[336] IsoWeekDate, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_PST
			},
			{
				"[337] IsoWeekDate, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoWeekDate(),
				"2012-W51-5",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoYear, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[338] IsoYear, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},
			{
				"[339] IsoYear, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},
			{
				"[340] IsoYear, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},
			{
				"[341] IsoYear, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},
			{
				"[342] IsoYear, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoYear(),
				"1970",
				BS_DEFAULT
			},
			{
				"[343] IsoYear, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},
			{
				"[344] IsoYear, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoYear(),
				"1970",
				BS_DEFAULT
			},
			{
				"[345] IsoYear, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},
			{
				"[346] IsoYear, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},
			{
				"[347] IsoYear, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},
			{
				"[348] IsoYear, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},
			{
				"[349] IsoYear, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},
			{
				"[350] IsoYear, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoYear, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[351] IsoYear, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},
			{
				"[352] IsoYear, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},
			{
				"[353] IsoYear, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},
			{
				"[354] IsoYear, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},
			{
				"[355] IsoYear, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoYear(),
				"1970",
				BS_PST
			},
			{
				"[356] IsoYear, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},
			{
				"[357] IsoYear, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoYear(),
				"1970",
				BS_PST
			},
			{
				"[358] IsoYear, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},
			{
				"[359] IsoYear, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},
			{
				"[360] IsoYear, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},
			{
				"[361] IsoYear, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},
			{
				"[362] IsoYear, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},
			{
				"[363] IsoYear, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoYear(),
				"2012",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoYearMonth, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[364] IsoYearMonth, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},
			{
				"[365] IsoYearMonth, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},
			{
				"[366] IsoYearMonth, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},
			{
				"[367] IsoYearMonth, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},
			{
				"[368] IsoYearMonth, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoYearMonth(),
				"1970-01",
				BS_DEFAULT
			},
			{
				"[369] IsoYearMonth, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},
			{
				"[370] IsoYearMonth, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoYearMonth(),
				"1970-01",
				BS_DEFAULT
			},
			{
				"[371] IsoYearMonth, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoYearMonth(),
				"2012-01",
				BS_DEFAULT
			},
			{
				"[372] IsoYearMonth, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},
			{
				"[373] IsoYearMonth, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},
			{
				"[374] IsoYearMonth, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},
			{
				"[375] IsoYearMonth, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},
			{
				"[376] IsoYearMonth, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoYearMonth, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[377] IsoYearMonth, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},
			{
				"[378] IsoYearMonth, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},
			{
				"[379] IsoYearMonth, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},
			{
				"[380] IsoYearMonth, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},
			{
				"[381] IsoYearMonth, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoYearMonth(),
				"1970-01",
				BS_PST
			},
			{
				"[382] IsoYearMonth, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},
			{
				"[383] IsoYearMonth, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoYearMonth(),
				"1970-01",
				BS_PST
			},
			{
				"[384] IsoYearMonth, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoYearMonth(),
				"2012-01",
				BS_PST
			},
			{
				"[385] IsoYearMonth, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},
			{
				"[386] IsoYearMonth, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},
			{
				"[387] IsoYearMonth, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},
			{
				"[388] IsoYearMonth, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},
			{
				"[389] IsoYearMonth, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoZonedDateTime, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[390] IsoZonedDateTime, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T12:34:56Z",
				BS_DEFAULT
			},
			{
				"[391] IsoZonedDateTime, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T12:34:56Z",
				BS_DEFAULT
			},
			{
				"[392] IsoYearMonth, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_DEFAULT
			},
			{
				"[393] IsoZonedDateTime, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T12:34:56-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[394] IsoZonedDateTime, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.IsoZonedDateTime(),
				"1970-01-01T12:34:56-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[395] IsoZonedDateTime, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[396] IsoZonedDateTime, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoZonedDateTime(),
				"1970-01-01T12:34:56-05:00",
				BS_DEFAULT
			},
			{
				"[397] IsoZonedDateTime, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-01-01T00:00:00-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[398] IsoZonedDateTime, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-01T00:00:00-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[399] IsoZonedDateTime, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T00:00:00-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[400] IsoZonedDateTime, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T00:00:00-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[401] IsoZonedDateTime, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T00:00:00-05:00[GMT-05:00]",
				BS_DEFAULT
			},
			{
				"[402] IsoZonedDateTime, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T00:00:00-05:00[GMT-05:00]",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// IsoZonedDateTime, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[403] IsoZonedDateTime, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T12:34:56Z",
				BS_PST
			},
			{
				"[404] IsoZonedDateTime, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T12:34:56Z",
				BS_PST
			},
			{
				"[405] IsoYearMonth, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.IsoYearMonth(),
				"2012-12",
				BS_PST
			},
			{
				"[406] IsoZonedDateTime, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T12:34:56-08:00[America/Los_Angeles]",
				BS_PST
			},
			{
				"[407] IsoZonedDateTime, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.IsoZonedDateTime(),
				"1970-01-01T12:34:56-08:00[America/Los_Angeles]",
				BS_PST
			},
			{
				"[408] IsoZonedDateTime, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T12:34:56-05:00",
				BS_PST
			},
			{
				"[409] IsoZonedDateTime, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.IsoZonedDateTime(),
				"1970-01-01T12:34:56-05:00",
				BS_PST
			},
			{
				"[410] IsoZonedDateTime, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-01-01T00:00:00-08:00[America/Los_Angeles]",
				BS_PST
			},
			{
				"[411] IsoZonedDateTime, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-01T00:00:00-08:00[America/Los_Angeles]",
				BS_PST
			},
			{
				"[412] IsoZonedDateTime, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T00:00:00-08:00[America/Los_Angeles]",
				BS_PST
			},
			{
				"[413] IsoZonedDateTime, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T00:00:00-08:00[America/Los_Angeles]",
				BS_PST
			},
			{
				"[414] IsoZonedDateTime, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T00:00:00-08:00[America/Los_Angeles]",
				BS_PST
			},
			{
				"[415] IsoZonedDateTime, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.IsoZonedDateTime(),
				"2012-12-21T00:00:00-08:00[America/Los_Angeles]",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// Rfc1123DateTime, Default timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[416] Rfc1123DateTime, DT_ZonedDateTime, Default timezone",
				DT_ZonedDateTime,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 12:34:56 GMT",
				BS_DEFAULT
			},
			{
				"[417] Rfc1123DateTime, DT_Instant, Default timezone",
				DT_Instant,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 12:34:56 GMT",
				BS_DEFAULT
			},
			{
				"[418] Rfc1123DateTime, DT_LocalDate, Default timezone",
				DT_LocalDate,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 00:00:00 -0500",
				BS_DEFAULT
			},
			{
				"[419] Rfc1123DateTime, DT_LocalDateTime, Default timezone",
				DT_LocalDateTime,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 12:34:56 -0500",
				BS_DEFAULT
			},
			{
				"[420] Rfc1123DateTime, DT_LocalTime, Default timezone",
				DT_LocalTime,
				new TemporalSwap.Rfc1123DateTime(),
				"Thu, 1 Jan 1970 12:34:56 -0500",
				BS_DEFAULT
			},
			{
				"[421] Rfc1123DateTime, DT_OffsetDateTime, Default timezone",
				DT_OffsetDateTime,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 12:34:56 -0500",
				BS_DEFAULT
			},
			{
				"[422] Rfc1123DateTime, DT_OffsetTime, Default timezone",
				DT_OffsetTime,
				new TemporalSwap.Rfc1123DateTime(),
				"Thu, 1 Jan 1970 12:34:56 -0500",
				BS_DEFAULT
			},
			{
				"[423] Rfc1123DateTime, DT_Year, Default timezone",
				DT_Year,
				new TemporalSwap.Rfc1123DateTime(),
				"Sun, 1 Jan 2012 00:00:00 -0500",
				BS_DEFAULT
			},
			{
				"[424] Rfc1123DateTime, DT_YearMonth, Default timezone",
				DT_YearMonth,
				new TemporalSwap.Rfc1123DateTime(),
				"Sat, 1 Dec 2012 00:00:00 -0500",
				BS_DEFAULT
			},
			{
				"[425] Rfc1123DateTime, DT_HijrahDate, Default timezone",
				DT_HijrahDate,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 00:00:00 -0500",
				BS_DEFAULT
			},
			{
				"[426] Rfc1123DateTime, DT_JapaneseDate, Default timezone",
				DT_JapaneseDate,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 00:00:00 -0500",
				BS_DEFAULT
			},
			{
				"[427] Rfc1123DateTime, DT_MinguoDate, Default timezone",
				DT_MinguoDate,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 00:00:00 -0500",
				BS_DEFAULT
			},
			{
				"[428] Rfc1123DateTime, DT_ThaiBuddhistDate, Default timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 00:00:00 -0500",
				BS_DEFAULT
			},

			//----------------------------------------------------------------------------------------------------------
			// Rfc1123DateTime, PST timezone
			//----------------------------------------------------------------------------------------------------------
			{
				"[429] Rfc1123DateTime, DT_ZonedDateTime, PST timezone",
				DT_ZonedDateTime,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 12:34:56 GMT",
				BS_PST
			},
			{
				"[430] Rfc1123DateTime, DT_Instant, PST timezone",
				DT_Instant,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 12:34:56 GMT",
				BS_PST
			},
			{
				"[431] Rfc1123DateTime, DT_LocalDate, PST timezone",
				DT_LocalDate,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 00:00:00 -0800",
				BS_PST
			},
			{
				"[432] Rfc1123DateTime, DT_LocalDateTime, PST timezone",
				DT_LocalDateTime,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 12:34:56 -0800",
				BS_PST
			},
			{
				"[433] Rfc1123DateTime, DT_LocalTime, PST timezone",
				DT_LocalTime,
				new TemporalSwap.Rfc1123DateTime(),
				"Thu, 1 Jan 1970 12:34:56 -0800",
				BS_PST
			},
			{
				"[434] Rfc1123DateTime, DT_OffsetDateTime, PST timezone",
				DT_OffsetDateTime,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 12:34:56 -0500",
				BS_PST
			},
			{
				"[435] Rfc1123DateTime, DT_OffsetTime, PST timezone",
				DT_OffsetTime,
				new TemporalSwap.Rfc1123DateTime(),
				"Thu, 1 Jan 1970 12:34:56 -0500",
				BS_PST
			},
			{
				"[436] Rfc1123DateTime, DT_Year, PST timezone",
				DT_Year,
				new TemporalSwap.Rfc1123DateTime(),
				"Sun, 1 Jan 2012 00:00:00 -0800",
				BS_PST
			},
			{
				"[437] Rfc1123DateTime, DT_YearMonth, PST timezone",
				DT_YearMonth,
				new TemporalSwap.Rfc1123DateTime(),
				"Sat, 1 Dec 2012 00:00:00 -0800",
				BS_PST
			},
			{
				"[438] Rfc1123DateTime, DT_HijrahDate, PST timezone",
				DT_HijrahDate,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 00:00:00 -0800",
				BS_PST
			},
			{
				"[439] Rfc1123DateTime, DT_JapaneseDate, PST timezone",
				DT_JapaneseDate,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 00:00:00 -0800",
				BS_PST
			},
			{
				"[440] Rfc1123DateTime, DT_MinguoDate, PST timezone",
				DT_MinguoDate,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 00:00:00 -0800",
				BS_PST
			},
			{
				"[441] Rfc1123DateTime, DT_ThaiBuddhistDate, PST timezone",
				DT_ThaiBuddhistDate,
				new TemporalSwap.Rfc1123DateTime(),
				"Fri, 21 Dec 2012 00:00:00 -0800",
				BS_PST
			},

			//----------------------------------------------------------------------------------------------------------
			// Edge cases
			//----------------------------------------------------------------------------------------------------------
			{
				"[442] BasicIsoDate, null",
				null,
				new TemporalCalendarSwap.BasicIsoDate(),
				null,
				BS_DEFAULT
			},

		});
	}
}