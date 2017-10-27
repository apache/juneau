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

import static org.apache.juneau.utils.CalendarUtils.Format.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Transforms {@link Date Dates} to {@link String Strings}.
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 *
 * The following direct subclasses are provided for convenience to the following formats:
 * <ul>
 * 	<li>{@link ToString} - To {@link String Strings} using the {@code Date.toString()} method.
 * 	<li>{@link ISO8601DT} - To ISO8601 date-time strings.
 * 	<li>{@link ISO8601DTZ} - Same as {@link ISO8601DT}, except always serializes in GMT.
 * 	<li>{@link ISO8601DTP} - Same as {@link ISO8601DT} except with millisecond precision.
 * 	<li>{@link ISO8601DTPZ} - Same as {@link ISO8601DTZ} except with millisecond precision.
 * 	<li>{@link RFC2822DT} - To RFC2822 date-time strings.
 * 	<li>{@link RFC2822DTZ} - Same as {@link RFC2822DT}, except always serializes in GMT.
 * 	<li>{@link RFC2822D} - To RFC2822 date strings.
 * 	<li>{@link DateTimeSimple} - To simple <js>"yyyy/MM/dd HH:mm:ss"</js> date-time strings.
 * 	<li>{@link DateSimple} - To simple <js>"yyyy/MM/dd"</js> date strings.
 * 	<li>{@link TimeSimple} - To simple <js>"HH:mm:ss"</js> time strings.
 * 	<li>{@link DateFull} - To {@link DateFormat#FULL} date strings.
 * 	<li>{@link DateLong} - To {@link DateFormat#LONG} date strings.
 * 	<li>{@link DateMedium} - To {@link DateFormat#MEDIUM} date strings.
 * 	<li>{@link DateShort} - To {@link DateFormat#SHORT} date strings.
 * 	<li>{@link TimeFull} - To {@link DateFormat#FULL} time strings.
 * 	<li>{@link TimeLong} - To {@link DateFormat#LONG} time strings.
 * 	<li>{@link TimeMedium} - To {@link DateFormat#MEDIUM} time strings.
 * 	<li>{@link TimeShort} - To {@link DateFormat#SHORT} time strings.
 * 	<li>{@link DateTimeFull} - To {@link DateFormat#FULL} date-time strings.
 * 	<li>{@link DateTimeLong} - To {@link DateFormat#LONG} date-time strings.
 * 	<li>{@link DateTimeMedium} - To {@link DateFormat#MEDIUM} date-time strings.
 * 	<li>{@link DateTimeShort} - To {@link DateFormat#SHORT} date-time strings.
 * </ul>
 */
public class DateSwap extends StringSwap<Date> {

	/**
	 * Transforms {@link Date Dates} to {@link String Strings} using the {@code Date.toString()} method.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"Wed Jul 04 15:30:45 EST 2001"</js>
	 * </ul>
	 */
	public static class ToString extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return convert(CalendarUtils.parseDate(o, TO_STRING, session.getLocale(), session.getTimeZone()), hint);
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, TO_STRING, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to ISO8601 date-time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"2001-07-04T15:30:45-05:00"</js>
	 * 	<li><js>"2001-07-04T15:30:45Z"</js>
	 * </ul>
	 *
	 * <h6 class='topic'>Example input:</h6>
	 * <ul>
	 * 	<li><js>"2001-07-04T15:30:45-05:00"</js>
	 * 	<li><js>"2001-07-04T15:30:45Z"</js>
	 * 	<li><js>"2001-07-04T15:30:45.1Z"</js>
	 * 	<li><js>"2001-07-04T15:30Z"</js>
	 * 	<li><js>"2001-07-04"</js>
	 * 	<li><js>"2001-07"</js>
	 * 	<li><js>"2001"</js>
	 * </ul>
	 */
	public static class ISO8601DT extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return convert(CalendarUtils.parseDate(o, ISO8601_DT, session.getLocale(), session.getTimeZone()), hint);
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, ISO8601_DT, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to ISO8601 date-time-local strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"2001-07-04T15:30:45"</js>
	 * </ul>
	 *
	 * <h6 class='topic'>Example input:</h6>
	 * <ul>
	 * 	<li><js>"2001-07-04T15:30:45"</js>
	 * 	<li><js>"2001-07-04T15:30:45.1"</js>
	 * 	<li><js>"2001-07-04T15:30"</js>
	 * 	<li><js>"2001-07-04"</js>
	 * 	<li><js>"2001-07"</js>
	 * 	<li><js>"2001"</js>
	 * </ul>
	 */
	public static class ISO8601DTL extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return convert(CalendarUtils.parseDate(o, ISO8601_DTL, session.getLocale(), session.getTimeZone()), hint);
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, ISO8601_DTL, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Same as {@link ISO8601DT}, except always serializes in GMT.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <js>"2001-07-04T15:30:45Z"</js>
	 */
	public static class ISO8601DTZ extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, ISO8601_DTZ, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, ISO8601_DTZ, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Same as {@link CalendarSwap.ISO8601DT} except serializes to millisecond precision.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <js>"2001-07-04T15:30:45.123Z"</js>
	 */
	public static class ISO8601DTP extends ISO8601DT {

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, ISO8601_DTP, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Same as {@link CalendarSwap.ISO8601DTZ} except serializes to millisecond precision.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <js>"2001-07-04T15:30:45.123"</js>
	 */
	public static class ISO8601DTPZ extends ISO8601DTZ {

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, ISO8601_DTPZ, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * ISO8601 date only.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <js>"2001-07-04"</js>
	 */
	public static class ISO8601D extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, ISO8601_D, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, ISO8601_D, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to RFC2822 date-time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"Sat, 03 Mar 2001 10:11:12 +0000"</js> <jc>// en_US</jc>
	 * 	<li><js>"土, 03 3 2001 10:11:12 +0000"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"토, 03 3월 2001 10:11:12 +0000"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class RFC2822DT extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, RFC2822_DT, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, RFC2822_DT, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Same as {@link DateSwap.RFC2822DT}, except always serializes in GMT.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"Sat, 03 Mar 2001 10:11:12 GMT"</js> <jc>// en_US</jc>
	 * 	<li><js>"土, 03 3 2001 10:11:12 GMT"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"토, 03 3월 2001 10:11:12 GMT"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class RFC2822DTZ extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, RFC2822_DTZ, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, RFC2822_DTZ, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to RFC2822 date strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"03 Mar 2001"</js> <jc>// en_US</jc>
	 * 	<li><js>"03 3 2001"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"03 3월 2001"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class RFC2822D extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, RFC2822_D, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, RFC2822_D, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to simple <js>"yyyy/MM/dd HH:mm:ss"</js> date-time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"2001/03/03 10:11:12"</js>
	 * </ul>
	 */
	public static class DateTimeSimple extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, SIMPLE_DT, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, SIMPLE_DT, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to simple <js>"yyyy/MM/dd"</js> date strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"2001/03/03"</js>
	 * </ul>
	 */
	public static class DateSimple extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, SIMPLE_D, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, SIMPLE_D, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to simple <js>"HH:mm:ss"</js> time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"10:11:12"</js>
	 * </ul>
	 */
	public static class TimeSimple extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, SIMPLE_T, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, SIMPLE_T, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#FULL} date strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"Saturday, March 3, 2001"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001年3月3日"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001년 3월 3일 토요일"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateFull extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, FULL_D, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, FULL_D, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#LONG} date strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"March 3, 2001"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001/03/03"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001년 3월 3일 (토)"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateLong extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, LONG_D, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, LONG_D, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#MEDIUM} date strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"Mar 3, 2001"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001/03/03"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001. 3. 3"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateMedium extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, MEDIUM_D, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, MEDIUM_D, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#SHORT} date strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"3/3/01"</js> <jc>// en_US</jc>
	 * 	<li><js>"01/03/03"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"01. 3. 3"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateShort extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, SHORT_D, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, SHORT_D, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#FULL} time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"10:11:12 AM GMT"</js> <jc>// en_US</jc>
	 * 	<li><js>"10時11分12秒 GMT"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"오전 10시 11분 12초 GMT"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class TimeFull extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, FULL_T, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, FULL_T, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#LONG} time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"10:11:12 AM GMT"</js> <jc>// en_US</jc>
	 * 	<li><js>"10:11:12 GMT"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"오전 10시 11분 12초"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class TimeLong extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, LONG_T, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, LONG_T, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#MEDIUM} time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"10:11:12 AM"</js> <jc>// en_US</jc>
	 * 	<li><js>"10:11:12"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"오전 10:11:12"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class TimeMedium extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, MEDIUM_T, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, MEDIUM_T, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#SHORT} time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"10:11 AM"</js> <jc>// en_US</jc>
	 * 	<li><js>"10:11 AM"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"오전 10:11"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class TimeShort extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, SHORT_T, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, SHORT_T, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#FULL} date-time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"Saturday, March 3, 2001 10:11:12 AM GMT"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001年3月3日 10時11分12秒 GMT"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001년 3월 3일 토요일 오전 10시 11분 12초 GMT"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateTimeFull extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, FULL_DT, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, FULL_DT, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#LONG} date-time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"March 3, 2001 10:11:12 AM GMT"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001/03/03 10:11:12 GMT"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001년 3월 3일 (토) 오전 10시 11분 12초"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateTimeLong extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, LONG_DT, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, LONG_DT, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#MEDIUM} date-time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"Mar 3, 2001 10:11:12 AM"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001/03/03 10:11:12"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001. 3. 3 오전 10:11:12"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateTimeMedium extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, MEDIUM_DT, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, MEDIUM_DT, session.getLocale(), session.getTimeZone());
		}
	}

	/**
	 * Transforms {@link Date Dates} to {@link DateFormat#SHORT} date-time strings.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <ul>
	 * 	<li><js>"3/3/01 10:11 AM"</js> <jc>// en_US</jc>
	 * 	<li><js>"01/03/03 10:11"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"01. 3. 3 오전 10:11"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateTimeShort extends DateSwap {

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws Exception {
			return CalendarUtils.parseDate(o, SHORT_DT, session.getLocale(), session.getTimeZone());
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return CalendarUtils.serialize(o, SHORT_DT, session.getLocale(), session.getTimeZone());
		}
	}

	static final Date convert(Date in, ClassMeta<?> hint) throws Exception {
		if (in == null)
			return null;
		if (hint == null || hint.isInstance(in))
			return in;
		Class<?> c = hint.getInnerClass();
		if (c == java.util.Date.class)
			return in;
		if (c == java.sql.Date.class)
			return new java.sql.Date(in.getTime());
		if (c == java.sql.Time.class)
			return new java.sql.Time(in.getTime());
		if (c == java.sql.Timestamp.class)
			return new java.sql.Timestamp(in.getTime());
		throw new ParseException("DateSwap is unable to narrow object of type ''{0}''", c);
	}
}
