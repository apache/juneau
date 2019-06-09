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

import static org.apache.juneau.BeanContext.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.transform.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Parameterized.class)
public class LocalizedDatesTest {

	private static Calendar testDate = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
	static {
		testDate.setTimeInMillis(0);
		testDate.set(2001, 2, 3, 10, 11, 12);
	}

	private static TimeZone prevTimeZone;

	@BeforeClass
	public static void before() {
		prevTimeZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
	}

	@AfterClass
	public static void after() {
		TimeZone.setDefault(prevTimeZone);
	}


	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {

			// CalendarSwap.ToString
			{ "000", testDate, CalendarSwap.ToString.class, null, null, null, null, "Sat Mar 03 10:11:12 GMT 2001" },
			{ "001", testDate, CalendarSwap.ToString.class, "ja_JP", null, null, null, "土 3 03 10:11:12 GMT 2001" },
			{ "002", testDate, CalendarSwap.ToString.class, Locale.JAPAN, null, null, null, "土 3 03 10:11:12 GMT 2001" },
			{ "003", testDate, CalendarSwap.ToString.class, null, Locale.JAPAN, null, null, "土 3 03 10:11:12 GMT 2001" },
			{ "004", testDate, CalendarSwap.ToString.class, "en_UK", Locale.JAPAN, null, null, "土 3 03 10:11:12 GMT 2001" },
			{ "005", testDate, CalendarSwap.ToString.class, "ja_JP", Locale.KOREA, null, null, "토 3월 03 10:11:12 GMT 2001" },
			{ "006", testDate, CalendarSwap.ToString.class, "en_US", null, "PST", null, "Sat Mar 03 02:11:12 PST 2001" },
			{ "007", testDate, CalendarSwap.ToString.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Sat Mar 03 02:11:12 PST 2001" },
			{ "008", testDate, CalendarSwap.ToString.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Sat Mar 03 05:11:12 EST 2001" },
			{ "009", testDate, CalendarSwap.ToString.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Sat Mar 03 05:11:12 EST 2001" },

			// CalendarSwap.ISO8601DT
			{ "010", testDate, CalendarSwap.ISO8601DT.class, null, null, null, null, "2001-03-03T10:11:12Z" },
			{ "011", testDate, CalendarSwap.ISO8601DT.class, "ja_JP", null, null, null, "2001-03-03T10:11:12Z" },
			{ "012", testDate, CalendarSwap.ISO8601DT.class, Locale.JAPAN, null, null, null, "2001-03-03T10:11:12Z" },
			{ "013", testDate, CalendarSwap.ISO8601DT.class, null, Locale.JAPAN, null, null, "2001-03-03T10:11:12Z" },
			{ "014", testDate, CalendarSwap.ISO8601DT.class, "en_UK", Locale.JAPAN, null, null, "2001-03-03T10:11:12Z" },
			{ "015", testDate, CalendarSwap.ISO8601DT.class, "ja_JP", Locale.KOREA, null, null, "2001-03-03T10:11:12Z" },
			{ "016", testDate, CalendarSwap.ISO8601DT.class, "en_US", null, "PST", null, "2001-03-03T02:11:12-08:00" },
			{ "017", testDate, CalendarSwap.ISO8601DT.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001-03-03T02:11:12-08:00" },
			{ "018", testDate, CalendarSwap.ISO8601DT.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001-03-03T05:11:12-05:00" },
			{ "019", testDate, CalendarSwap.ISO8601DT.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001-03-03T05:11:12-05:00" },

			// CalendarSwap.ISO8601DTZ
			{ "020", testDate, CalendarSwap.ISO8601DTZ.class, null, null, null, null, "2001-03-03T10:11:12Z" },
			{ "021", testDate, CalendarSwap.ISO8601DTZ.class, "ja_JP", null, null, null, "2001-03-03T10:11:12Z" },
			{ "022", testDate, CalendarSwap.ISO8601DTZ.class, Locale.JAPAN, null, null, null, "2001-03-03T10:11:12Z" },
			{ "023", testDate, CalendarSwap.ISO8601DTZ.class, null, Locale.JAPAN, null, null, "2001-03-03T10:11:12Z" },
			{ "024", testDate, CalendarSwap.ISO8601DTZ.class, "en_UK", Locale.JAPAN, null, null, "2001-03-03T10:11:12Z" },
			{ "025", testDate, CalendarSwap.ISO8601DTZ.class, "ja_JP", Locale.KOREA, null, null, "2001-03-03T10:11:12Z" },
			{ "026", testDate, CalendarSwap.ISO8601DTZ.class, "en_US", null, "PST", null, "2001-03-03T10:11:12Z" },
			{ "027", testDate, CalendarSwap.ISO8601DTZ.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001-03-03T10:11:12Z" },
			{ "028", testDate, CalendarSwap.ISO8601DTZ.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001-03-03T10:11:12Z" },
			{ "029", testDate, CalendarSwap.ISO8601DTZ.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001-03-03T10:11:12Z" },

			// CalendarSwap.ISO8601DTP
			{ "030", testDate, CalendarSwap.ISO8601DTP.class, null, null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "031", testDate, CalendarSwap.ISO8601DTP.class, "ja_JP", null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "032", testDate, CalendarSwap.ISO8601DTP.class, Locale.JAPAN, null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "033", testDate, CalendarSwap.ISO8601DTP.class, null, Locale.JAPAN, null, null, "2001-03-03T10:11:12.000Z" },
			{ "034", testDate, CalendarSwap.ISO8601DTP.class, "en_UK", Locale.JAPAN, null, null, "2001-03-03T10:11:12.000Z" },
			{ "035", testDate, CalendarSwap.ISO8601DTP.class, "ja_JP", Locale.KOREA, null, null, "2001-03-03T10:11:12.000Z" },
			{ "036", testDate, CalendarSwap.ISO8601DTP.class, "en_US", null, "PST", null, "2001-03-03T02:11:12.000-08:00" },
			{ "037", testDate, CalendarSwap.ISO8601DTP.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001-03-03T02:11:12.000-08:00" },
			{ "038", testDate, CalendarSwap.ISO8601DTP.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001-03-03T05:11:12.000-05:00" },
			{ "039", testDate, CalendarSwap.ISO8601DTP.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001-03-03T05:11:12.000-05:00" },

			// CalendarSwap.ISO8601DTPNZ
			{ "040", testDate, CalendarSwap.ISO8601DTPZ.class, null, null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "041", testDate, CalendarSwap.ISO8601DTPZ.class, "ja_JP", null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "042", testDate, CalendarSwap.ISO8601DTPZ.class, Locale.JAPAN, null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "043", testDate, CalendarSwap.ISO8601DTPZ.class, null, Locale.JAPAN, null, null, "2001-03-03T10:11:12.000Z" },
			{ "044", testDate, CalendarSwap.ISO8601DTPZ.class, "en_UK", Locale.JAPAN, null, null, "2001-03-03T10:11:12.000Z" },
			{ "045", testDate, CalendarSwap.ISO8601DTPZ.class, "ja_JP", Locale.KOREA, null, null, "2001-03-03T10:11:12.000Z" },
			{ "046", testDate, CalendarSwap.ISO8601DTPZ.class, "en_US", null, "PST", null, "2001-03-03T10:11:12.000Z" },
			{ "047", testDate, CalendarSwap.ISO8601DTPZ.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001-03-03T10:11:12.000Z" },
			{ "048", testDate, CalendarSwap.ISO8601DTPZ.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001-03-03T10:11:12.000Z" },
			{ "049", testDate, CalendarSwap.ISO8601DTPZ.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001-03-03T10:11:12.000Z" },

			// CalendarSwap.RFC2822DT
			{ "050", testDate, CalendarSwap.RFC2822DT.class, null, null, null, null, "Sat, 03 Mar 2001 10:11:12 +0000" },
			{ "051", testDate, CalendarSwap.RFC2822DT.class, "ja_JP", null, null, null, "土, 03 3 2001 10:11:12 +0000" },
			{ "052", testDate, CalendarSwap.RFC2822DT.class, Locale.JAPAN, null, null, null, "土, 03 3 2001 10:11:12 +0000" },
			{ "053", testDate, CalendarSwap.RFC2822DT.class, null, Locale.JAPAN, null, null, "土, 03 3 2001 10:11:12 +0000" },
			{ "054", testDate, CalendarSwap.RFC2822DT.class, "en_UK", Locale.JAPAN, null, null, "土, 03 3 2001 10:11:12 +0000" },
			{ "055", testDate, CalendarSwap.RFC2822DT.class, "ja_JP", Locale.KOREA, null, null, "토, 03 3월 2001 10:11:12 +0000" },
			{ "056", testDate, CalendarSwap.RFC2822DT.class, "en_US", null, "PST", null, "Sat, 03 Mar 2001 02:11:12 -0800" },
			{ "057", testDate, CalendarSwap.RFC2822DT.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Sat, 03 Mar 2001 02:11:12 -0800" },
			{ "058", testDate, CalendarSwap.RFC2822DT.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Sat, 03 Mar 2001 05:11:12 -0500" },
			{ "059", testDate, CalendarSwap.RFC2822DT.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Sat, 03 Mar 2001 05:11:12 -0500" },

			// CalendarSwap.RFC2822DTZ
			{ "060", testDate, CalendarSwap.RFC2822DTZ.class, null, null, null, null, "Sat, 03 Mar 2001 10:11:12 GMT" },
			{ "061", testDate, CalendarSwap.RFC2822DTZ.class, "ja_JP", null, null, null, "土, 03 3 2001 10:11:12 GMT" },
			{ "062", testDate, CalendarSwap.RFC2822DTZ.class, Locale.JAPAN, null, null, null, "土, 03 3 2001 10:11:12 GMT" },
			{ "063", testDate, CalendarSwap.RFC2822DTZ.class, null, Locale.JAPAN, null, null, "土, 03 3 2001 10:11:12 GMT" },
			{ "064", testDate, CalendarSwap.RFC2822DTZ.class, "en_UK", Locale.JAPAN, null, null, "土, 03 3 2001 10:11:12 GMT" },
			{ "065", testDate, CalendarSwap.RFC2822DTZ.class, "ja_JP", Locale.KOREA, null, null, "토, 03 3월 2001 10:11:12 GMT" },
			{ "066", testDate, CalendarSwap.RFC2822DTZ.class, "en_US", null, "PST", null, "Sat, 03 Mar 2001 10:11:12 GMT" },
			{ "067", testDate, CalendarSwap.RFC2822DTZ.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Sat, 03 Mar 2001 10:11:12 GMT" },
			{ "068", testDate, CalendarSwap.RFC2822DTZ.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Sat, 03 Mar 2001 10:11:12 GMT" },
			{ "069", testDate, CalendarSwap.RFC2822DTZ.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Sat, 03 Mar 2001 10:11:12 GMT" },

			// CalendarSwap.RFC2822D
			{ "070", testDate, CalendarSwap.RFC2822D.class, null, null, null, null, "03 Mar 2001" },
			{ "071", testDate, CalendarSwap.RFC2822D.class, "ja_JP", null, null, null, "03 3 2001" },
			{ "072", testDate, CalendarSwap.RFC2822D.class, Locale.JAPAN, null, null, null, "03 3 2001" },
			{ "073", testDate, CalendarSwap.RFC2822D.class, null, Locale.JAPAN, null, null, "03 3 2001" },
			{ "074", testDate, CalendarSwap.RFC2822D.class, "en_UK", Locale.JAPAN, null, null, "03 3 2001" },
			{ "075", testDate, CalendarSwap.RFC2822D.class, "ja_JP", Locale.KOREA, null, null, "03 3월 2001" },
			// Must use timezones to the east of GMT so that date doesn't roll back in test.
			{ "076", testDate, CalendarSwap.RFC2822D.class, "en_US", null, "WET", null, "03 Mar 2001" },
			{ "077", testDate, CalendarSwap.RFC2822D.class, "en_US", null, TimeZone.getTimeZone("WET"), null, "03 Mar 2001" },
			{ "078", testDate, CalendarSwap.RFC2822D.class, "en_US", null, null, TimeZone.getTimeZone("WET"), "03 Mar 2001" },
			{ "079", testDate, CalendarSwap.RFC2822D.class, "en_US", null, "PST", TimeZone.getTimeZone("WET"), "03 Mar 2001" },

			// CalendarSwap.DateSimple
			{ "080", testDate, CalendarSwap.DateSimple.class, null, null, null, null, "2001/03/03" },
			{ "081", testDate, CalendarSwap.DateSimple.class, "ja_JP", null, null, null, "2001/03/03" },
			{ "082", testDate, CalendarSwap.DateSimple.class, Locale.JAPAN, null, null, null, "2001/03/03" },
			{ "083", testDate, CalendarSwap.DateSimple.class, null, Locale.JAPAN, null, null, "2001/03/03" },
			{ "084", testDate, CalendarSwap.DateSimple.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03" },
			{ "085", testDate, CalendarSwap.DateSimple.class, "ja_JP", Locale.KOREA, null, null, "2001/03/03" },
			{ "086", testDate, CalendarSwap.DateSimple.class, "en_US", null, "PST", null, "2001/03/03" },
			{ "087", testDate, CalendarSwap.DateSimple.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001/03/03" },
			{ "088", testDate, CalendarSwap.DateSimple.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001/03/03" },
			{ "089", testDate, CalendarSwap.DateSimple.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001/03/03" },

			// CalendarSwap.TimeSimple
			{ "090", testDate, CalendarSwap.TimeSimple.class, null, null, null, null, "10:11:12" },
			{ "091", testDate, CalendarSwap.TimeSimple.class, "ja_JP", null, null, null, "10:11:12" },
			{ "092", testDate, CalendarSwap.TimeSimple.class, Locale.JAPAN, null, null, null, "10:11:12" },
			{ "093", testDate, CalendarSwap.TimeSimple.class, null, Locale.JAPAN, null, null, "10:11:12" },
			{ "094", testDate, CalendarSwap.TimeSimple.class, "en_UK", Locale.JAPAN, null, null, "10:11:12" },
			{ "095", testDate, CalendarSwap.TimeSimple.class, "ja_JP", Locale.KOREA, null, null, "10:11:12" },
			{ "096", testDate, CalendarSwap.TimeSimple.class, "en_US", null, "PST", null, "02:11:12" },
			{ "097", testDate, CalendarSwap.TimeSimple.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "02:11:12" },
			{ "098", testDate, CalendarSwap.TimeSimple.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "05:11:12" },
			{ "099", testDate, CalendarSwap.TimeSimple.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "05:11:12" },

			// CalendarSwap.DateTimeSimple
			{ "100", testDate, CalendarSwap.DateTimeSimple.class, null, null, null, null, "2001/03/03 10:11:12" },
			{ "101", testDate, CalendarSwap.DateTimeSimple.class, "ja_JP", null, null, null, "2001/03/03 10:11:12" },
			{ "102", testDate, CalendarSwap.DateTimeSimple.class, Locale.JAPAN, null, null, null, "2001/03/03 10:11:12" },
			{ "103", testDate, CalendarSwap.DateTimeSimple.class, null, Locale.JAPAN, null, null, "2001/03/03 10:11:12" },
			{ "104", testDate, CalendarSwap.DateTimeSimple.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03 10:11:12" },
			{ "105", testDate, CalendarSwap.DateTimeSimple.class, "ja_JP", Locale.KOREA, null, null, "2001/03/03 10:11:12" },
			{ "106", testDate, CalendarSwap.DateTimeSimple.class, "en_US", null, "PST", null, "2001/03/03 02:11:12" },
			{ "107", testDate, CalendarSwap.DateTimeSimple.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001/03/03 02:11:12" },
			{ "108", testDate, CalendarSwap.DateTimeSimple.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001/03/03 05:11:12" },
			{ "109", testDate, CalendarSwap.DateTimeSimple.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001/03/03 05:11:12" },

			// CalendarSwap.DateFull
			{ "110", testDate, CalendarSwap.DateFull.class, null, null, null, null, "Saturday, March 3, 2001" },
			{ "111", testDate, CalendarSwap.DateFull.class, "ja_JP", null, null, null, "2001年3月3日" },
			{ "112", testDate, CalendarSwap.DateFull.class, Locale.JAPAN, null, null, null, "2001年3月3日" },
			{ "113", testDate, CalendarSwap.DateFull.class, null, Locale.JAPAN, null, null, "2001年3月3日" },
			{ "114", testDate, CalendarSwap.DateFull.class, "en_UK", Locale.JAPAN, null, null, "2001年3月3日" },
			{ "115", testDate, CalendarSwap.DateFull.class, "en_UK", Locale.KOREA, null, null, "2001년 3월 3일 토요일" },
			{ "116", testDate, CalendarSwap.DateFull.class, "en_US", null, "PST", null, "Saturday, March 3, 2001" },
			{ "117", testDate, CalendarSwap.DateFull.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Saturday, March 3, 2001" },
			{ "118", testDate, CalendarSwap.DateFull.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Saturday, March 3, 2001" },
			{ "119", testDate, CalendarSwap.DateFull.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Saturday, March 3, 2001" },

			// CalendarSwap.DateLong
			{ "120", testDate, CalendarSwap.DateLong.class, null, null, null, null, "March 3, 2001" },
			{ "121", testDate, CalendarSwap.DateLong.class, "ja_JP", null, null, null, "2001/03/03" },
			{ "122", testDate, CalendarSwap.DateLong.class, Locale.JAPAN, null, null, null, "2001/03/03" },
			{ "123", testDate, CalendarSwap.DateLong.class, null, Locale.JAPAN, null, null, "2001/03/03" },
			{ "124", testDate, CalendarSwap.DateLong.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03" },
			{ "125", testDate, CalendarSwap.DateLong.class, "en_UK", Locale.KOREA, null, null, "2001년 3월 3일 (토)" },
			{ "126", testDate, CalendarSwap.DateLong.class, "en_US", null, "PST", null, "March 3, 2001" },
			{ "127", testDate, CalendarSwap.DateLong.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "March 3, 2001" },
			{ "128", testDate, CalendarSwap.DateLong.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "March 3, 2001" },
			{ "129", testDate, CalendarSwap.DateLong.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "March 3, 2001" },

			// CalendarSwap.DateMedium
			{ "130", testDate, CalendarSwap.DateMedium.class, null, null, null, null, "Mar 3, 2001" },
			{ "131", testDate, CalendarSwap.DateMedium.class, "ja_JP", null, null, null, "2001/03/03" },
			{ "132", testDate, CalendarSwap.DateMedium.class, Locale.JAPAN, null, null, null, "2001/03/03" },
			{ "133", testDate, CalendarSwap.DateMedium.class, null, Locale.JAPAN, null, null, "2001/03/03" },
			{ "134", testDate, CalendarSwap.DateMedium.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03" },
			{ "135", testDate, CalendarSwap.DateMedium.class, "en_UK", Locale.KOREA, null, null, "2001. 3. 3" },
			{ "136", testDate, CalendarSwap.DateMedium.class, "en_US", null, "PST", null, "Mar 3, 2001" },
			{ "137", testDate, CalendarSwap.DateMedium.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Mar 3, 2001" },
			{ "138", testDate, CalendarSwap.DateMedium.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Mar 3, 2001" },
			{ "139", testDate, CalendarSwap.DateMedium.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Mar 3, 2001" },

			// CalendarSwap.DateShort
			{ "140", testDate, CalendarSwap.DateShort.class, null, null, null, null, "3/3/01" },
			{ "141", testDate, CalendarSwap.DateShort.class, "ja_JP", null, null, null, "01/03/03" },
			{ "142", testDate, CalendarSwap.DateShort.class, Locale.JAPAN, null, null, null, "01/03/03" },
			{ "143", testDate, CalendarSwap.DateShort.class, null, Locale.JAPAN, null, null, "01/03/03" },
			{ "144", testDate, CalendarSwap.DateShort.class, "en_UK", Locale.JAPAN, null, null, "01/03/03" },
			{ "145", testDate, CalendarSwap.DateShort.class, "en_UK", Locale.KOREA, null, null, "01. 3. 3" },
			{ "146", testDate, CalendarSwap.DateShort.class, "en_US", null, "PST", null, "3/3/01" },
			{ "147", testDate, CalendarSwap.DateShort.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "3/3/01" },
			{ "148", testDate, CalendarSwap.DateShort.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "3/3/01" },
			{ "149", testDate, CalendarSwap.DateShort.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "3/3/01" },

			// CalendarSwap.TimeFull
			{ "150", testDate, CalendarSwap.TimeFull.class, null, null, null, null, "10:11:12 AM GMT" },
			{ "151", testDate, CalendarSwap.TimeFull.class, "ja_JP", null, null, null, "10時11分12秒 GMT" },
			{ "152", testDate, CalendarSwap.TimeFull.class, Locale.JAPAN, null, null, null, "10時11分12秒 GMT" },
			{ "153", testDate, CalendarSwap.TimeFull.class, null, Locale.JAPAN, null, null, "10時11分12秒 GMT" },
			{ "154", testDate, CalendarSwap.TimeFull.class, "en_UK", Locale.JAPAN, null, null, "10時11分12秒 GMT" },
			{ "155", testDate, CalendarSwap.TimeFull.class, "en_UK", Locale.KOREA, null, null, "오전 10시 11분 12초 GMT" },
			{ "156", testDate, CalendarSwap.TimeFull.class, "en_US", null, "PST", null, "2:11:12 AM PST" },
			{ "157", testDate, CalendarSwap.TimeFull.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2:11:12 AM PST" },
			{ "158", testDate, CalendarSwap.TimeFull.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "5:11:12 AM EST" },
			{ "159", testDate, CalendarSwap.TimeFull.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "5:11:12 AM EST" },

			// CalendarSwap.TimeLong
			{ "160", testDate, CalendarSwap.TimeLong.class, null, null, null, null, "10:11:12 AM GMT" },
			{ "161", testDate, CalendarSwap.TimeLong.class, "ja_JP", null, null, null, "10:11:12 GMT" },
			{ "162", testDate, CalendarSwap.TimeLong.class, Locale.JAPAN, null, null, null, "10:11:12 GMT" },
			{ "163", testDate, CalendarSwap.TimeLong.class, null, Locale.JAPAN, null, null, "10:11:12 GMT" },
			{ "164", testDate, CalendarSwap.TimeLong.class, "en_UK", Locale.JAPAN, null, null, "10:11:12 GMT" },
			{ "165", testDate, CalendarSwap.TimeLong.class, "en_UK", Locale.KOREA, null, null, "오전 10시 11분 12초" },
			{ "166", testDate, CalendarSwap.TimeLong.class, "en_US", null, "PST", null, "2:11:12 AM PST" },
			{ "167", testDate, CalendarSwap.TimeLong.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2:11:12 AM PST" },
			{ "168", testDate, CalendarSwap.TimeLong.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "5:11:12 AM EST" },
			{ "169", testDate, CalendarSwap.TimeLong.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "5:11:12 AM EST" },

			// CalendarSwap.TimeMedium
			{ "170", testDate, CalendarSwap.TimeMedium.class, null, null, null, null, "10:11:12 AM" },
			{ "171", testDate, CalendarSwap.TimeMedium.class, "ja_JP", null, null, null, "10:11:12" },
			{ "172", testDate, CalendarSwap.TimeMedium.class, Locale.JAPAN, null, null, null, "10:11:12" },
			{ "173", testDate, CalendarSwap.TimeMedium.class, null, Locale.JAPAN, null, null, "10:11:12" },
			{ "174", testDate, CalendarSwap.TimeMedium.class, "en_UK", Locale.JAPAN, null, null, "10:11:12" },
			{ "175", testDate, CalendarSwap.TimeMedium.class, "en_UK", Locale.KOREA, null, null, "오전 10:11:12" },
			{ "176", testDate, CalendarSwap.TimeMedium.class, "en_US", null, "PST", null, "2:11:12 AM" },
			{ "177", testDate, CalendarSwap.TimeMedium.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2:11:12 AM" },
			{ "178", testDate, CalendarSwap.TimeMedium.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "5:11:12 AM" },
			{ "179", testDate, CalendarSwap.TimeMedium.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "5:11:12 AM" },

			// CalendarSwap.TimeShort
			{ "180", testDate, CalendarSwap.TimeShort.class, null, null, null, null, "10:11 AM" },
			{ "181", testDate, CalendarSwap.TimeShort.class, "ja_JP", null, null, null, "10:11" },
			{ "182", testDate, CalendarSwap.TimeShort.class, Locale.JAPAN, null, null, null, "10:11" },
			{ "183", testDate, CalendarSwap.TimeShort.class, null, Locale.JAPAN, null, null, "10:11" },
			{ "184", testDate, CalendarSwap.TimeShort.class, "en_UK", Locale.JAPAN, null, null, "10:11" },
			{ "185", testDate, CalendarSwap.TimeShort.class, "en_UK", Locale.KOREA, null, null, "오전 10:11" },
			{ "186", testDate, CalendarSwap.TimeShort.class, "en_US", null, "PST", null, "2:11 AM" },
			{ "187", testDate, CalendarSwap.TimeShort.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2:11 AM" },
			{ "188", testDate, CalendarSwap.TimeShort.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "5:11 AM" },
			{ "189", testDate, CalendarSwap.TimeShort.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "5:11 AM" },

			// CalendarSwap.DateTimeFull
			{ "190", testDate, CalendarSwap.DateTimeFull.class, null, null, null, null, "Saturday, March 3, 2001 10:11:12 AM GMT" },
			{ "191", testDate, CalendarSwap.DateTimeFull.class, "ja_JP", null, null, null, "2001年3月3日 10時11分12秒 GMT" },
			{ "192", testDate, CalendarSwap.DateTimeFull.class, Locale.JAPAN, null, null, null, "2001年3月3日 10時11分12秒 GMT" },
			{ "193", testDate, CalendarSwap.DateTimeFull.class, null, Locale.JAPAN, null, null, "2001年3月3日 10時11分12秒 GMT" },
			{ "194", testDate, CalendarSwap.DateTimeFull.class, "en_UK", Locale.JAPAN, null, null, "2001年3月3日 10時11分12秒 GMT" },
			{ "195", testDate, CalendarSwap.DateTimeFull.class, "en_UK", Locale.KOREA, null, null, "2001년 3월 3일 토요일 오전 10시 11분 12초 GMT" },
			{ "196", testDate, CalendarSwap.DateTimeFull.class, "en_US", null, "PST", null, "Saturday, March 3, 2001 2:11:12 AM PST" },
			{ "197", testDate, CalendarSwap.DateTimeFull.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Saturday, March 3, 2001 2:11:12 AM PST" },
			{ "198", testDate, CalendarSwap.DateTimeFull.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Saturday, March 3, 2001 5:11:12 AM EST" },
			{ "199", testDate, CalendarSwap.DateTimeFull.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Saturday, March 3, 2001 5:11:12 AM EST" },

			// CalendarSwap.DateTimeLong
			{ "200", testDate, CalendarSwap.DateTimeLong.class, null, null, null, null, "March 3, 2001 10:11:12 AM GMT" },
			{ "201", testDate, CalendarSwap.DateTimeLong.class, "ja_JP", null, null, null, "2001/03/03 10:11:12 GMT" },
			{ "202", testDate, CalendarSwap.DateTimeLong.class, Locale.JAPAN, null, null, null, "2001/03/03 10:11:12 GMT" },
			{ "203", testDate, CalendarSwap.DateTimeLong.class, null, Locale.JAPAN, null, null, "2001/03/03 10:11:12 GMT" },
			{ "204", testDate, CalendarSwap.DateTimeLong.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03 10:11:12 GMT" },
			{ "205", testDate, CalendarSwap.DateTimeLong.class, "en_UK", Locale.KOREA, null, null, "2001년 3월 3일 (토) 오전 10시 11분 12초" },
			{ "206", testDate, CalendarSwap.DateTimeLong.class, "en_US", null, "PST", null, "March 3, 2001 2:11:12 AM PST" },
			{ "207", testDate, CalendarSwap.DateTimeLong.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "March 3, 2001 2:11:12 AM PST" },
			{ "208", testDate, CalendarSwap.DateTimeLong.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "March 3, 2001 5:11:12 AM EST" },
			{ "209", testDate, CalendarSwap.DateTimeLong.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "March 3, 2001 5:11:12 AM EST" },

			// CalendarSwap.DateTimeMedium
			{ "210", testDate, CalendarSwap.DateTimeMedium.class, null, null, null, null, "Mar 3, 2001 10:11:12 AM" },
			{ "211", testDate, CalendarSwap.DateTimeMedium.class, "ja_JP", null, null, null, "2001/03/03 10:11:12" },
			{ "212", testDate, CalendarSwap.DateTimeMedium.class, Locale.JAPAN, null, null, null, "2001/03/03 10:11:12" },
			{ "213", testDate, CalendarSwap.DateTimeMedium.class, null, Locale.JAPAN, null, null, "2001/03/03 10:11:12" },
			{ "214", testDate, CalendarSwap.DateTimeMedium.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03 10:11:12" },
			{ "215", testDate, CalendarSwap.DateTimeMedium.class, "en_UK", Locale.KOREA, null, null, "2001. 3. 3 오전 10:11:12" },
			{ "216", testDate, CalendarSwap.DateTimeMedium.class, "en_US", null, "PST", null, "Mar 3, 2001 2:11:12 AM" },
			{ "217", testDate, CalendarSwap.DateTimeMedium.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Mar 3, 2001 2:11:12 AM" },
			{ "218", testDate, CalendarSwap.DateTimeMedium.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Mar 3, 2001 5:11:12 AM" },
			{ "219", testDate, CalendarSwap.DateTimeMedium.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Mar 3, 2001 5:11:12 AM" },

			// CalendarSwap.DateTimeShort
			{ "220", testDate, CalendarSwap.DateTimeShort.class, null, null, null, null, "3/3/01 10:11 AM" },
			{ "221", testDate, CalendarSwap.DateTimeShort.class, "ja_JP", null, null, null, "01/03/03 10:11" },
			{ "222", testDate, CalendarSwap.DateTimeShort.class, Locale.JAPAN, null, null, null, "01/03/03 10:11" },
			{ "223", testDate, CalendarSwap.DateTimeShort.class, null, Locale.JAPAN, null, null, "01/03/03 10:11" },
			{ "224", testDate, CalendarSwap.DateTimeShort.class, "en_UK", Locale.JAPAN, null, null, "01/03/03 10:11" },
			{ "225", testDate, CalendarSwap.DateTimeShort.class, "en_UK", Locale.KOREA, null, null, "01. 3. 3 오전 10:11" },
			{ "226", testDate, CalendarSwap.DateTimeShort.class, "en_US", null, "PST", null, "3/3/01 2:11 AM" },
			{ "227", testDate, CalendarSwap.DateTimeShort.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "3/3/01 2:11 AM" },
			{ "228", testDate, CalendarSwap.DateTimeShort.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "3/3/01 5:11 AM" },
			{ "229", testDate, CalendarSwap.DateTimeShort.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "3/3/01 5:11 AM" },

			// DateSwap.ToString
			{ "230", testDate.getTime(), DateSwap.ToString.class, null, null, null, null, "Sat Mar 03 10:11:12 GMT 2001" },
			{ "231", testDate.getTime(), DateSwap.ToString.class, "ja_JP", null, null, null, "土 3 03 10:11:12 GMT 2001" },
			{ "232", testDate.getTime(), DateSwap.ToString.class, Locale.JAPAN, null, null, null, "土 3 03 10:11:12 GMT 2001" },
			{ "233", testDate.getTime(), DateSwap.ToString.class, null, Locale.JAPAN, null, null, "土 3 03 10:11:12 GMT 2001" },
			{ "234", testDate.getTime(), DateSwap.ToString.class, "en_UK", Locale.JAPAN, null, null, "土 3 03 10:11:12 GMT 2001" },
			{ "235", testDate.getTime(), DateSwap.ToString.class, "ja_JP", Locale.KOREA, null, null, "토 3월 03 10:11:12 GMT 2001" },
			{ "236", testDate.getTime(), DateSwap.ToString.class, "en_US", null, "PST", null, "Sat Mar 03 02:11:12 PST 2001" },
			{ "237", testDate.getTime(), DateSwap.ToString.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Sat Mar 03 02:11:12 PST 2001" },
			{ "238", testDate.getTime(), DateSwap.ToString.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Sat Mar 03 05:11:12 EST 2001" },
			{ "239", testDate.getTime(), DateSwap.ToString.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Sat Mar 03 05:11:12 EST 2001" },

			// DateSwap.ISO8601DT
			{ "240", testDate.getTime(), DateSwap.ISO8601DT.class, null, null, null, null, "2001-03-03T10:11:12Z" },
			{ "241", testDate.getTime(), DateSwap.ISO8601DT.class, "ja_JP", null, null, null, "2001-03-03T10:11:12Z" },
			{ "242", testDate.getTime(), DateSwap.ISO8601DT.class, Locale.JAPAN, null, null, null, "2001-03-03T10:11:12Z" },
			{ "243", testDate.getTime(), DateSwap.ISO8601DT.class, null, Locale.JAPAN, null, null, "2001-03-03T10:11:12Z" },
			{ "244", testDate.getTime(), DateSwap.ISO8601DT.class, "en_UK", Locale.JAPAN, null, null, "2001-03-03T10:11:12Z" },
			{ "245", testDate.getTime(), DateSwap.ISO8601DT.class, "ja_JP", Locale.KOREA, null, null, "2001-03-03T10:11:12Z" },
			{ "246", testDate.getTime(), DateSwap.ISO8601DT.class, "en_US", null, "PST", null, "2001-03-03T02:11:12-08:00" },
			{ "247", testDate.getTime(), DateSwap.ISO8601DT.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001-03-03T02:11:12-08:00" },
			{ "248", testDate.getTime(), DateSwap.ISO8601DT.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001-03-03T05:11:12-05:00" },
			{ "249", testDate.getTime(), DateSwap.ISO8601DT.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001-03-03T05:11:12-05:00" },

			// DateSwap.ISO8601DTZ
			{ "250", testDate.getTime(), DateSwap.ISO8601DTZ.class, null, null, null, null, "2001-03-03T10:11:12Z" },
			{ "251", testDate.getTime(), DateSwap.ISO8601DTZ.class, "ja_JP", null, null, null, "2001-03-03T10:11:12Z" },
			{ "252", testDate.getTime(), DateSwap.ISO8601DTZ.class, Locale.JAPAN, null, null, null, "2001-03-03T10:11:12Z" },
			{ "253", testDate.getTime(), DateSwap.ISO8601DTZ.class, null, Locale.JAPAN, null, null, "2001-03-03T10:11:12Z" },
			{ "254", testDate.getTime(), DateSwap.ISO8601DTZ.class, "en_UK", Locale.JAPAN, null, null, "2001-03-03T10:11:12Z" },
			{ "255", testDate.getTime(), DateSwap.ISO8601DTZ.class, "ja_JP", Locale.KOREA, null, null, "2001-03-03T10:11:12Z" },
			{ "256", testDate.getTime(), DateSwap.ISO8601DTZ.class, "en_US", null, "PST", null, "2001-03-03T10:11:12Z" },
			{ "257", testDate.getTime(), DateSwap.ISO8601DTZ.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001-03-03T10:11:12Z" },
			{ "258", testDate.getTime(), DateSwap.ISO8601DTZ.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001-03-03T10:11:12Z" },
			{ "259", testDate.getTime(), DateSwap.ISO8601DTZ.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001-03-03T10:11:12Z" },

			// DateSwap.ISO8601DTP
			{ "260", testDate.getTime(), DateSwap.ISO8601DTP.class, null, null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "261", testDate.getTime(), DateSwap.ISO8601DTP.class, "ja_JP", null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "262", testDate.getTime(), DateSwap.ISO8601DTP.class, Locale.JAPAN, null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "263", testDate.getTime(), DateSwap.ISO8601DTP.class, null, Locale.JAPAN, null, null, "2001-03-03T10:11:12.000Z" },
			{ "264", testDate.getTime(), DateSwap.ISO8601DTP.class, "en_UK", Locale.JAPAN, null, null, "2001-03-03T10:11:12.000Z" },
			{ "265", testDate.getTime(), DateSwap.ISO8601DTP.class, "ja_JP", Locale.KOREA, null, null, "2001-03-03T10:11:12.000Z" },
			{ "266", testDate.getTime(), DateSwap.ISO8601DTP.class, "en_US", null, "PST", null, "2001-03-03T02:11:12.000-08:00" },
			{ "267", testDate.getTime(), DateSwap.ISO8601DTP.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001-03-03T02:11:12.000-08:00" },
			{ "268", testDate.getTime(), DateSwap.ISO8601DTP.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001-03-03T05:11:12.000-05:00" },
			{ "269", testDate.getTime(), DateSwap.ISO8601DTP.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001-03-03T05:11:12.000-05:00" },

			// DateSwap.ISO8601DTPNZ
			{ "270", testDate.getTime(), DateSwap.ISO8601DTPZ.class, null, null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "271", testDate.getTime(), DateSwap.ISO8601DTPZ.class, "ja_JP", null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "272", testDate.getTime(), DateSwap.ISO8601DTPZ.class, Locale.JAPAN, null, null, null, "2001-03-03T10:11:12.000Z" },
			{ "273", testDate.getTime(), DateSwap.ISO8601DTPZ.class, null, Locale.JAPAN, null, null, "2001-03-03T10:11:12.000Z" },
			{ "274", testDate.getTime(), DateSwap.ISO8601DTPZ.class, "en_UK", Locale.JAPAN, null, null, "2001-03-03T10:11:12.000Z" },
			{ "275", testDate.getTime(), DateSwap.ISO8601DTPZ.class, "ja_JP", Locale.KOREA, null, null, "2001-03-03T10:11:12.000Z" },
			{ "276", testDate.getTime(), DateSwap.ISO8601DTPZ.class, "en_US", null, "PST", null, "2001-03-03T10:11:12.000Z" },
			{ "277", testDate.getTime(), DateSwap.ISO8601DTPZ.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001-03-03T10:11:12.000Z" },
			{ "278", testDate.getTime(), DateSwap.ISO8601DTPZ.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001-03-03T10:11:12.000Z" },
			{ "279", testDate.getTime(), DateSwap.ISO8601DTPZ.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001-03-03T10:11:12.000Z" },

			// DateSwap.RFC2822DT
			{ "280", testDate.getTime(), DateSwap.RFC2822DT.class, null, null, null, null, "Sat, 03 Mar 2001 10:11:12 +0000" },
			{ "281", testDate.getTime(), DateSwap.RFC2822DT.class, "ja_JP", null, null, null, "土, 03 3 2001 10:11:12 +0000" },
			{ "282", testDate.getTime(), DateSwap.RFC2822DT.class, Locale.JAPAN, null, null, null, "土, 03 3 2001 10:11:12 +0000" },
			{ "283", testDate.getTime(), DateSwap.RFC2822DT.class, null, Locale.JAPAN, null, null, "土, 03 3 2001 10:11:12 +0000" },
			{ "284", testDate.getTime(), DateSwap.RFC2822DT.class, "en_UK", Locale.JAPAN, null, null, "土, 03 3 2001 10:11:12 +0000" },
			{ "285", testDate.getTime(), DateSwap.RFC2822DT.class, "ja_JP", Locale.KOREA, null, null, "토, 03 3월 2001 10:11:12 +0000" },
			{ "286", testDate.getTime(), DateSwap.RFC2822DT.class, "en_US", null, "PST", null, "Sat, 03 Mar 2001 02:11:12 -0800" },
			{ "287", testDate.getTime(), DateSwap.RFC2822DT.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Sat, 03 Mar 2001 02:11:12 -0800" },
			{ "288", testDate.getTime(), DateSwap.RFC2822DT.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Sat, 03 Mar 2001 05:11:12 -0500" },
			{ "289", testDate.getTime(), DateSwap.RFC2822DT.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Sat, 03 Mar 2001 05:11:12 -0500" },

			// DateSwap.RFC2822DTZ
			{ "290", testDate.getTime(), DateSwap.RFC2822DTZ.class, null, null, null, null, "Sat, 03 Mar 2001 10:11:12 GMT" },
			{ "291", testDate.getTime(), DateSwap.RFC2822DTZ.class, "ja_JP", null, null, null, "土, 03 3 2001 10:11:12 GMT" },
			{ "292", testDate.getTime(), DateSwap.RFC2822DTZ.class, Locale.JAPAN, null, null, null, "土, 03 3 2001 10:11:12 GMT" },
			{ "293", testDate.getTime(), DateSwap.RFC2822DTZ.class, null, Locale.JAPAN, null, null, "土, 03 3 2001 10:11:12 GMT" },
			{ "294", testDate.getTime(), DateSwap.RFC2822DTZ.class, "en_UK", Locale.JAPAN, null, null, "土, 03 3 2001 10:11:12 GMT" },
			{ "295", testDate.getTime(), DateSwap.RFC2822DTZ.class, "ja_JP", Locale.KOREA, null, null, "토, 03 3월 2001 10:11:12 GMT" },
			{ "296", testDate.getTime(), DateSwap.RFC2822DTZ.class, "en_US", null, "PST", null, "Sat, 03 Mar 2001 10:11:12 GMT" },
			{ "297", testDate.getTime(), DateSwap.RFC2822DTZ.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Sat, 03 Mar 2001 10:11:12 GMT" },
			{ "298", testDate.getTime(), DateSwap.RFC2822DTZ.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Sat, 03 Mar 2001 10:11:12 GMT" },
			{ "299", testDate.getTime(), DateSwap.RFC2822DTZ.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Sat, 03 Mar 2001 10:11:12 GMT" },

			// DateSwap.RFC2822D
			{ "300", testDate.getTime(), DateSwap.RFC2822D.class, null, null, null, null, "03 Mar 2001" },
			{ "301", testDate.getTime(), DateSwap.RFC2822D.class, "ja_JP", null, null, null, "03 3 2001" },
			{ "302", testDate.getTime(), DateSwap.RFC2822D.class, Locale.JAPAN, null, null, null, "03 3 2001" },
			{ "303", testDate.getTime(), DateSwap.RFC2822D.class, null, Locale.JAPAN, null, null, "03 3 2001" },
			{ "304", testDate.getTime(), DateSwap.RFC2822D.class, "en_UK", Locale.JAPAN, null, null, "03 3 2001" },
			{ "305", testDate.getTime(), DateSwap.RFC2822D.class, "ja_JP", Locale.KOREA, null, null, "03 3월 2001" },
			{ "306", testDate.getTime(), DateSwap.RFC2822D.class, "en_US", null, "PST", null, "03 Mar 2001" },
			{ "307", testDate.getTime(), DateSwap.RFC2822D.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "03 Mar 2001" },
			{ "308", testDate.getTime(), DateSwap.RFC2822D.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "03 Mar 2001" },
			{ "309", testDate.getTime(), DateSwap.RFC2822D.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "03 Mar 2001" },

			// DateSwap.DateSimple
			{ "310", testDate.getTime(), DateSwap.DateSimple.class, null, null, null, null, "2001/03/03" },
			{ "311", testDate.getTime(), DateSwap.DateSimple.class, "ja_JP", null, null, null, "2001/03/03" },
			{ "312", testDate.getTime(), DateSwap.DateSimple.class, Locale.JAPAN, null, null, null, "2001/03/03" },
			{ "313", testDate.getTime(), DateSwap.DateSimple.class, null, Locale.JAPAN, null, null, "2001/03/03" },
			{ "314", testDate.getTime(), DateSwap.DateSimple.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03" },
			{ "315", testDate.getTime(), DateSwap.DateSimple.class, "ja_JP", Locale.KOREA, null, null, "2001/03/03" },
			{ "316", testDate.getTime(), DateSwap.DateSimple.class, "en_US", null, "PST", null, "2001/03/03" },
			{ "317", testDate.getTime(), DateSwap.DateSimple.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001/03/03" },
			{ "318", testDate.getTime(), DateSwap.DateSimple.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001/03/03" },
			{ "319", testDate.getTime(), DateSwap.DateSimple.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001/03/03" },

			// DateSwap.TimeSimple
			{ "320", testDate.getTime(), DateSwap.TimeSimple.class, null, null, null, null, "10:11:12" },
			{ "321", testDate.getTime(), DateSwap.TimeSimple.class, "ja_JP", null, null, null, "10:11:12" },
			{ "322", testDate.getTime(), DateSwap.TimeSimple.class, Locale.JAPAN, null, null, null, "10:11:12" },
			{ "323", testDate.getTime(), DateSwap.TimeSimple.class, null, Locale.JAPAN, null, null, "10:11:12" },
			{ "324", testDate.getTime(), DateSwap.TimeSimple.class, "en_UK", Locale.JAPAN, null, null, "10:11:12" },
			{ "325", testDate.getTime(), DateSwap.TimeSimple.class, "ja_JP", Locale.KOREA, null, null, "10:11:12" },
			{ "326", testDate.getTime(), DateSwap.TimeSimple.class, "en_US", null, "PST", null, "02:11:12" },
			{ "327", testDate.getTime(), DateSwap.TimeSimple.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "02:11:12" },
			{ "328", testDate.getTime(), DateSwap.TimeSimple.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "05:11:12" },
			{ "329", testDate.getTime(), DateSwap.TimeSimple.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "05:11:12" },

			// DateSwap.DateTimeSimple
			{ "330", testDate.getTime(), DateSwap.DateTimeSimple.class, null, null, null, null, "2001/03/03 10:11:12" },
			{ "331", testDate.getTime(), DateSwap.DateTimeSimple.class, "ja_JP", null, null, null, "2001/03/03 10:11:12" },
			{ "332", testDate.getTime(), DateSwap.DateTimeSimple.class, Locale.JAPAN, null, null, null, "2001/03/03 10:11:12" },
			{ "333", testDate.getTime(), DateSwap.DateTimeSimple.class, null, Locale.JAPAN, null, null, "2001/03/03 10:11:12" },
			{ "334", testDate.getTime(), DateSwap.DateTimeSimple.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03 10:11:12" },
			{ "335", testDate.getTime(), DateSwap.DateTimeSimple.class, "ja_JP", Locale.KOREA, null, null, "2001/03/03 10:11:12" },
			{ "336", testDate.getTime(), DateSwap.DateTimeSimple.class, "en_US", null, "PST", null, "2001/03/03 02:11:12" },
			{ "337", testDate.getTime(), DateSwap.DateTimeSimple.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2001/03/03 02:11:12" },
			{ "338", testDate.getTime(), DateSwap.DateTimeSimple.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "2001/03/03 05:11:12" },
			{ "339", testDate.getTime(), DateSwap.DateTimeSimple.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "2001/03/03 05:11:12" },

			// DateSwap.DateFull
			{ "340", testDate.getTime(), DateSwap.DateFull.class, null, null, null, null, "Saturday, March 3, 2001" },
			{ "341", testDate.getTime(), DateSwap.DateFull.class, "ja_JP", null, null, null, "2001年3月3日" },
			{ "342", testDate.getTime(), DateSwap.DateFull.class, Locale.JAPAN, null, null, null, "2001年3月3日" },
			{ "343", testDate.getTime(), DateSwap.DateFull.class, null, Locale.JAPAN, null, null, "2001年3月3日" },
			{ "344", testDate.getTime(), DateSwap.DateFull.class, "en_UK", Locale.JAPAN, null, null, "2001年3月3日" },
			{ "345", testDate.getTime(), DateSwap.DateFull.class, "en_UK", Locale.KOREA, null, null, "2001년 3월 3일 토요일" },
			{ "346", testDate.getTime(), DateSwap.DateFull.class, "en_US", null, "PST", null, "Saturday, March 3, 2001" },
			{ "347", testDate.getTime(), DateSwap.DateFull.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Saturday, March 3, 2001" },
			{ "348", testDate.getTime(), DateSwap.DateFull.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Saturday, March 3, 2001" },
			{ "349", testDate.getTime(), DateSwap.DateFull.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Saturday, March 3, 2001" },

			// DateSwap.DateLong
			{ "350", testDate.getTime(), DateSwap.DateLong.class, null, null, null, null, "March 3, 2001" },
			{ "351", testDate.getTime(), DateSwap.DateLong.class, "ja_JP", null, null, null, "2001/03/03" },
			{ "352", testDate.getTime(), DateSwap.DateLong.class, Locale.JAPAN, null, null, null, "2001/03/03" },
			{ "353", testDate.getTime(), DateSwap.DateLong.class, null, Locale.JAPAN, null, null, "2001/03/03" },
			{ "354", testDate.getTime(), DateSwap.DateLong.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03" },
			{ "355", testDate.getTime(), DateSwap.DateLong.class, "en_UK", Locale.KOREA, null, null, "2001년 3월 3일 (토)" },
			{ "356", testDate.getTime(), DateSwap.DateLong.class, "en_US", null, "PST", null, "March 3, 2001" },
			{ "357", testDate.getTime(), DateSwap.DateLong.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "March 3, 2001" },
			{ "358", testDate.getTime(), DateSwap.DateLong.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "March 3, 2001" },
			{ "359", testDate.getTime(), DateSwap.DateLong.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "March 3, 2001" },

			// DateSwap.DateMedium
			{ "360", testDate.getTime(), DateSwap.DateMedium.class, null, null, null, null, "Mar 3, 2001" },
			{ "361", testDate.getTime(), DateSwap.DateMedium.class, "ja_JP", null, null, null, "2001/03/03" },
			{ "362", testDate.getTime(), DateSwap.DateMedium.class, Locale.JAPAN, null, null, null, "2001/03/03" },
			{ "363", testDate.getTime(), DateSwap.DateMedium.class, null, Locale.JAPAN, null, null, "2001/03/03" },
			{ "364", testDate.getTime(), DateSwap.DateMedium.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03" },
			{ "365", testDate.getTime(), DateSwap.DateMedium.class, "en_UK", Locale.KOREA, null, null, "2001. 3. 3" },
			{ "366", testDate.getTime(), DateSwap.DateMedium.class, "en_US", null, "PST", null, "Mar 3, 2001" },
			{ "367", testDate.getTime(), DateSwap.DateMedium.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Mar 3, 2001" },
			{ "368", testDate.getTime(), DateSwap.DateMedium.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Mar 3, 2001" },
			{ "369", testDate.getTime(), DateSwap.DateMedium.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Mar 3, 2001" },

			// DateSwap.DateShort
			{ "370", testDate.getTime(), DateSwap.DateShort.class, null, null, null, null, "3/3/01" },
			{ "371", testDate.getTime(), DateSwap.DateShort.class, "ja_JP", null, null, null, "01/03/03" },
			{ "372", testDate.getTime(), DateSwap.DateShort.class, Locale.JAPAN, null, null, null, "01/03/03" },
			{ "373", testDate.getTime(), DateSwap.DateShort.class, null, Locale.JAPAN, null, null, "01/03/03" },
			{ "374", testDate.getTime(), DateSwap.DateShort.class, "en_UK", Locale.JAPAN, null, null, "01/03/03" },
			{ "375", testDate.getTime(), DateSwap.DateShort.class, "en_UK", Locale.KOREA, null, null, "01. 3. 3" },
			{ "376", testDate.getTime(), DateSwap.DateShort.class, "en_US", null, "PST", null, "3/3/01" },
			{ "377", testDate.getTime(), DateSwap.DateShort.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "3/3/01" },
			{ "378", testDate.getTime(), DateSwap.DateShort.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "3/3/01" },
			{ "379", testDate.getTime(), DateSwap.DateShort.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "3/3/01" },

			// DateSwap.TimeFull
			{ "380", testDate.getTime(), DateSwap.TimeFull.class, null, null, null, null, "10:11:12 AM GMT" },
			{ "381", testDate.getTime(), DateSwap.TimeFull.class, "ja_JP", null, null, null, "10時11分12秒 GMT" },
			{ "382", testDate.getTime(), DateSwap.TimeFull.class, Locale.JAPAN, null, null, null, "10時11分12秒 GMT" },
			{ "383", testDate.getTime(), DateSwap.TimeFull.class, null, Locale.JAPAN, null, null, "10時11分12秒 GMT" },
			{ "384", testDate.getTime(), DateSwap.TimeFull.class, "en_UK", Locale.JAPAN, null, null, "10時11分12秒 GMT" },
			{ "385", testDate.getTime(), DateSwap.TimeFull.class, "en_UK", Locale.KOREA, null, null, "오전 10시 11분 12초 GMT" },
			{ "386", testDate.getTime(), DateSwap.TimeFull.class, "en_US", null, "PST", null, "2:11:12 AM PST" },
			{ "387", testDate.getTime(), DateSwap.TimeFull.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2:11:12 AM PST" },
			{ "388", testDate.getTime(), DateSwap.TimeFull.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "5:11:12 AM EST" },
			{ "389", testDate.getTime(), DateSwap.TimeFull.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "5:11:12 AM EST" },

			// DateSwap.TimeLong
			{ "390", testDate.getTime(), DateSwap.TimeLong.class, null, null, null, null, "10:11:12 AM GMT" },
			{ "391", testDate.getTime(), DateSwap.TimeLong.class, "ja_JP", null, null, null, "10:11:12 GMT" },
			{ "392", testDate.getTime(), DateSwap.TimeLong.class, Locale.JAPAN, null, null, null, "10:11:12 GMT" },
			{ "393", testDate.getTime(), DateSwap.TimeLong.class, null, Locale.JAPAN, null, null, "10:11:12 GMT" },
			{ "394", testDate.getTime(), DateSwap.TimeLong.class, "en_UK", Locale.JAPAN, null, null, "10:11:12 GMT" },
			{ "395", testDate.getTime(), DateSwap.TimeLong.class, "en_UK", Locale.KOREA, null, null, "오전 10시 11분 12초" },
			{ "396", testDate.getTime(), DateSwap.TimeLong.class, "en_US", null, "PST", null, "2:11:12 AM PST" },
			{ "397", testDate.getTime(), DateSwap.TimeLong.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2:11:12 AM PST" },
			{ "398", testDate.getTime(), DateSwap.TimeLong.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "5:11:12 AM EST" },
			{ "399", testDate.getTime(), DateSwap.TimeLong.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "5:11:12 AM EST" },

			// DateSwap.TimeMedium
			{ "400", testDate.getTime(), DateSwap.TimeMedium.class, null, null, null, null, "10:11:12 AM" },
			{ "401", testDate.getTime(), DateSwap.TimeMedium.class, "ja_JP", null, null, null, "10:11:12" },
			{ "402", testDate.getTime(), DateSwap.TimeMedium.class, Locale.JAPAN, null, null, null, "10:11:12" },
			{ "403", testDate.getTime(), DateSwap.TimeMedium.class, null, Locale.JAPAN, null, null, "10:11:12" },
			{ "404", testDate.getTime(), DateSwap.TimeMedium.class, "en_UK", Locale.JAPAN, null, null, "10:11:12" },
			{ "405", testDate.getTime(), DateSwap.TimeMedium.class, "en_UK", Locale.KOREA, null, null, "오전 10:11:12" },
			{ "406", testDate.getTime(), DateSwap.TimeMedium.class, "en_US", null, "PST", null, "2:11:12 AM" },
			{ "407", testDate.getTime(), DateSwap.TimeMedium.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2:11:12 AM" },
			{ "408", testDate.getTime(), DateSwap.TimeMedium.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "5:11:12 AM" },
			{ "409", testDate.getTime(), DateSwap.TimeMedium.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "5:11:12 AM" },

			// DateSwap.TimeShort
			{ "410", testDate.getTime(), DateSwap.TimeShort.class, null, null, null, null, "10:11 AM" },
			{ "411", testDate.getTime(), DateSwap.TimeShort.class, "ja_JP", null, null, null, "10:11" },
			{ "412", testDate.getTime(), DateSwap.TimeShort.class, Locale.JAPAN, null, null, null, "10:11" },
			{ "413", testDate.getTime(), DateSwap.TimeShort.class, null, Locale.JAPAN, null, null, "10:11" },
			{ "414", testDate.getTime(), DateSwap.TimeShort.class, "en_UK", Locale.JAPAN, null, null, "10:11" },
			{ "415", testDate.getTime(), DateSwap.TimeShort.class, "en_UK", Locale.KOREA, null, null, "오전 10:11" },
			{ "416", testDate.getTime(), DateSwap.TimeShort.class, "en_US", null, "PST", null, "2:11 AM" },
			{ "417", testDate.getTime(), DateSwap.TimeShort.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "2:11 AM" },
			{ "418", testDate.getTime(), DateSwap.TimeShort.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "5:11 AM" },
			{ "419", testDate.getTime(), DateSwap.TimeShort.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "5:11 AM" },

			// DateSwap.DateTimeFull
			{ "420", testDate.getTime(), DateSwap.DateTimeFull.class, null, null, null, null, "Saturday, March 3, 2001 10:11:12 AM GMT" },
			{ "421", testDate.getTime(), DateSwap.DateTimeFull.class, "ja_JP", null, null, null, "2001年3月3日 10時11分12秒 GMT" },
			{ "422", testDate.getTime(), DateSwap.DateTimeFull.class, Locale.JAPAN, null, null, null, "2001年3月3日 10時11分12秒 GMT" },
			{ "423", testDate.getTime(), DateSwap.DateTimeFull.class, null, Locale.JAPAN, null, null, "2001年3月3日 10時11分12秒 GMT" },
			{ "424", testDate.getTime(), DateSwap.DateTimeFull.class, "en_UK", Locale.JAPAN, null, null, "2001年3月3日 10時11分12秒 GMT" },
			{ "425", testDate.getTime(), DateSwap.DateTimeFull.class, "en_UK", Locale.KOREA, null, null, "2001년 3월 3일 토요일 오전 10시 11분 12초 GMT" },
			{ "426", testDate.getTime(), DateSwap.DateTimeFull.class, "en_US", null, "PST", null, "Saturday, March 3, 2001 2:11:12 AM PST" },
			{ "427", testDate.getTime(), DateSwap.DateTimeFull.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Saturday, March 3, 2001 2:11:12 AM PST" },
			{ "428", testDate.getTime(), DateSwap.DateTimeFull.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Saturday, March 3, 2001 5:11:12 AM EST" },
			{ "429", testDate.getTime(), DateSwap.DateTimeFull.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Saturday, March 3, 2001 5:11:12 AM EST" },

			// DateSwap.DateTimeLong
			{ "430", testDate.getTime(), DateSwap.DateTimeLong.class, null, null, null, null, "March 3, 2001 10:11:12 AM GMT" },
			{ "431", testDate.getTime(), DateSwap.DateTimeLong.class, "ja_JP", null, null, null, "2001/03/03 10:11:12 GMT" },
			{ "432", testDate.getTime(), DateSwap.DateTimeLong.class, Locale.JAPAN, null, null, null, "2001/03/03 10:11:12 GMT" },
			{ "433", testDate.getTime(), DateSwap.DateTimeLong.class, null, Locale.JAPAN, null, null, "2001/03/03 10:11:12 GMT" },
			{ "434", testDate.getTime(), DateSwap.DateTimeLong.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03 10:11:12 GMT" },
			{ "435", testDate.getTime(), DateSwap.DateTimeLong.class, "en_UK", Locale.KOREA, null, null, "2001년 3월 3일 (토) 오전 10시 11분 12초" },
			{ "436", testDate.getTime(), DateSwap.DateTimeLong.class, "en_US", null, "PST", null, "March 3, 2001 2:11:12 AM PST" },
			{ "437", testDate.getTime(), DateSwap.DateTimeLong.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "March 3, 2001 2:11:12 AM PST" },
			{ "438", testDate.getTime(), DateSwap.DateTimeLong.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "March 3, 2001 5:11:12 AM EST" },
			{ "439", testDate.getTime(), DateSwap.DateTimeLong.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "March 3, 2001 5:11:12 AM EST" },

			// DateSwap.DateTimeMedium
			{ "440", testDate.getTime(), DateSwap.DateTimeMedium.class, null, null, null, null, "Mar 3, 2001 10:11:12 AM" },
			{ "441", testDate.getTime(), DateSwap.DateTimeMedium.class, "ja_JP", null, null, null, "2001/03/03 10:11:12" },
			{ "442", testDate.getTime(), DateSwap.DateTimeMedium.class, Locale.JAPAN, null, null, null, "2001/03/03 10:11:12" },
			{ "443", testDate.getTime(), DateSwap.DateTimeMedium.class, null, Locale.JAPAN, null, null, "2001/03/03 10:11:12" },
			{ "444", testDate.getTime(), DateSwap.DateTimeMedium.class, "en_UK", Locale.JAPAN, null, null, "2001/03/03 10:11:12" },
			{ "445", testDate.getTime(), DateSwap.DateTimeMedium.class, "en_UK", Locale.KOREA, null, null, "2001. 3. 3 오전 10:11:12" },
			{ "446", testDate.getTime(), DateSwap.DateTimeMedium.class, "en_US", null, "PST", null, "Mar 3, 2001 2:11:12 AM" },
			{ "447", testDate.getTime(), DateSwap.DateTimeMedium.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "Mar 3, 2001 2:11:12 AM" },
			{ "448", testDate.getTime(), DateSwap.DateTimeMedium.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "Mar 3, 2001 5:11:12 AM" },
			{ "449", testDate.getTime(), DateSwap.DateTimeMedium.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "Mar 3, 2001 5:11:12 AM" },

			// DateSwap.DateTimeShort
			{ "450", testDate.getTime(), DateSwap.DateTimeShort.class, null, null, null, null, "3/3/01 10:11 AM" },
			{ "451", testDate.getTime(), DateSwap.DateTimeShort.class, "ja_JP", null, null, null, "01/03/03 10:11" },
			{ "452", testDate.getTime(), DateSwap.DateTimeShort.class, Locale.JAPAN, null, null, null, "01/03/03 10:11" },
			{ "453", testDate.getTime(), DateSwap.DateTimeShort.class, null, Locale.JAPAN, null, null, "01/03/03 10:11" },
			{ "454", testDate.getTime(), DateSwap.DateTimeShort.class, "en_UK", Locale.JAPAN, null, null, "01/03/03 10:11" },
			{ "455", testDate.getTime(), DateSwap.DateTimeShort.class, "en_UK", Locale.KOREA, null, null, "01. 3. 3 오전 10:11" },
			{ "456", testDate.getTime(), DateSwap.DateTimeShort.class, "en_US", null, "PST", null, "3/3/01 2:11 AM" },
			{ "457", testDate.getTime(), DateSwap.DateTimeShort.class, "en_US", null, TimeZone.getTimeZone("PST"), null, "3/3/01 2:11 AM" },
			{ "458", testDate.getTime(), DateSwap.DateTimeShort.class, "en_US", null, null, TimeZone.getTimeZone("EST"), "3/3/01 5:11 AM" },
			{ "459", testDate.getTime(), DateSwap.DateTimeShort.class, "en_US", null, "PST", TimeZone.getTimeZone("EST"), "3/3/01 5:11 AM" },
		});
	}

	@Before
	public void beforeTest() {
		TestUtils.setLocale(Locale.US);
		TestUtils.setTimeZone("GMT");
	}

	@After
	public void afterTest() {
		TestUtils.unsetLocale();
		TestUtils.unsetTimeZone();
	}

	@SuppressWarnings("unused")
	private String label, expected;
	private Object calendar;
	private BeanContext bc;
	private Locale sessionLocale;
	private TimeZone sessionTimeZone;

	public LocalizedDatesTest(String label, Object calendar, Class<? extends PojoSwap<?,?>> swap, Object contextLocale, Locale sessionLocale, Object contextTimeZone, TimeZone sessionTimeZone, String expected) throws Exception {
		this.label = label;
		this.expected = expected;
		this.calendar = calendar;
		this.sessionLocale = sessionLocale;
		this.sessionTimeZone = sessionTimeZone;
		BeanContextBuilder bcb = BeanContext.create();
		bcb.pojoSwaps(swap);
		if (contextLocale != null)
			bcb.set(BEAN_locale, contextLocale);
		if (contextTimeZone != null)
			bcb.set(BEAN_timeZone, contextTimeZone);
		bc = bcb.build();
	}

	@Test
	public void test() {
		BeanSession session = bc.createSession(
			new BeanSessionArgs()
				.locale(sessionLocale)
				.timeZone(sessionTimeZone)
		);
		String actual = session.convertToType(calendar, String.class);
		String actual2 = actual;
		if (expected.indexOf('(') == -1)
			actual2 = actual2.replaceAll("\\([^\\)]+\\) ", "").replaceAll(" \\([^\\)]+\\)", "");  // Fix Windows-specific formatting difference
		assertEquals(expected, actual2);
		Object c2 = session.convertToType(actual, calendar.getClass());
		actual2 = session.convertToType(c2, String.class);
		if (expected.indexOf('(') == -1)
			actual2 = actual2.replaceAll("\\([^\\)]+\\) ", "").replaceAll(" \\([^\\)]+\\)", "");  // Fix Windows-specific formatting difference
		assertEquals(expected, actual2);
	}
}