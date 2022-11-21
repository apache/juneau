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
package org.apache.juneau.testutils;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.DateUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import javax.xml.bind.*;

/**
 * Utility class for converting {@link Calendar} and {@link Date} objects to common serialized forms.
 */
public class CalendarUtils {

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	/**
	 * Valid conversion formats.
	 */
	public static enum Format {

		/**
		 * Transform to ISO8601 date-time-local strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"2001-07-04T15:30:45"</js>
		 * </ul>
		 *
		 * <h5 class='section'>Example input:</h5>
		 * <ul>
		 * 	<li><js>"2001-07-04T15:30:45"</js>
		 * 	<li><js>"2001-07-04T15:30:45.1"</js>
		 * 	<li><js>"2001-07-04T15:30"</js>
		 * 	<li><js>"2001-07-04"</js>
		 * 	<li><js>"2001-07"</js>
		 * 	<li><js>"2001"</js>
		 * </ul>
		 */
		ISO8601_DTL,

		/**
		 * Transform to ISO8601 date-time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"2001-07-04T15:30:45-05:00"</js>
		 * 	<li><js>"2001-07-04T15:30:45Z"</js>
		 * </ul>
		 *
		 * <h5 class='section'>Example input:</h5>
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
		ISO8601_DT,

		/**
		 * Same as {@link CalendarUtils.Format#ISO8601_DT}, except always serializes in GMT.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <js>"2001-07-04T15:30:45Z"</js>
		 */
		ISO8601_DTZ,

		/**
		 * Same as {@link CalendarUtils.Format#ISO8601_DT} except serializes to millisecond precision.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <js>"2001-07-04T15:30:45.123Z"</js>
		 */
		ISO8601_DTP,

		/**
		 * Same as {@link CalendarUtils.Format#ISO8601_DTZ} except serializes to millisecond precision.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <js>"2001-07-04T15:30:45.123"</js>
		 */
		ISO8601_DTPZ,

		/**
		 * ISO8601 date only.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <js>"2001-07-04"</js>
		 */
		ISO8601_D,

		/**
		 * Transform to {@link String Strings} using the {@code Date.toString()} method.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"Wed Jul 04 15:30:45 EST 2001"</js>
		 * </ul>
		 */
		TO_STRING,

		/**
		 * Transform to RFC2822 date-time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"Sat, 03 Mar 2001 10:11:12 +0000"</js> <jc>// en_US</jc>
		 * 	<li><js>"土, 03 3 2001 10:11:12 +0000"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"토, 03 3월 2001 10:11:12 +0000"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		RFC2822_DT,

		/**
		 * Same as {@link CalendarUtils.Format#RFC2822_DT}, except always serializes in GMT.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"Sat, 03 Mar 2001 10:11:12 GMT"</js> <jc>// en_US</jc>
		 * 	<li><js>"土, 03 3 2001 10:11:12 GMT"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"토, 03 3월 2001 10:11:12 GMT"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		RFC2822_DTZ,

		/**
		 * Transform to RFC2822 date strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"03 Mar 2001"</js> <jc>// en_US</jc>
		 * 	<li><js>"03 3 2001"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"03 3월 2001"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		RFC2822_D,

		/**
		 * Transform to simple <js>"yyyy/MM/dd HH:mm:ss"</js> date-time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"2001/03/03 10:11:12"</js>
		 * </ul>
		 */
		SIMPLE_DT,

		/**
		 * Transform to simple <js>"yyyy/MM/dd"</js> date strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"2001/03/03"</js>
		 * </ul>
		 */
		SIMPLE_D,

		/**
		 * Transform to simple <js>"HH:mm:ss"</js> time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"10:11:12"</js>
		 * </ul>
		 */
		SIMPLE_T,

		/**
		 * Transform to {@link DateFormat#FULL} date strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"Saturday, March 3, 2001"</js> <jc>// en_US</jc>
		 * 	<li><js>"2001年3月3日"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"2001년 3월 3일 토요일"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		FULL_D,

		/**
		 * Transform to {@link DateFormat#LONG} date strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"March 3, 2001"</js> <jc>// en_US</jc>
		 * 	<li><js>"2001/03/03"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"2001년 3월 3일 (토)"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		LONG_D,

		/**
		 * Transform to {@link DateFormat#MEDIUM} date strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"Mar 3, 2001"</js> <jc>// en_US</jc>
		 * 	<li><js>"2001/03/03"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"2001. 3. 3"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		MEDIUM_D,

		/**
		 * Transform to {@link DateFormat#SHORT} date strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"3/3/01"</js> <jc>// en_US</jc>
		 * 	<li><js>"01/03/03"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"01. 3. 3"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		SHORT_D,

		/**
		 * Transform to {@link DateFormat#FULL} time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"10:11:12 AM GMT"</js> <jc>// en_US</jc>
		 * 	<li><js>"10時11分12秒 GMT"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"오전 10시 11분 12초 GMT"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		FULL_T,

		/**
		 * Transform to {@link DateFormat#LONG} time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"10:11:12 AM GMT"</js> <jc>// en_US</jc>
		 * 	<li><js>"10:11:12 GMT"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"오전 10시 11분 12초"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		LONG_T,

		/**
		 * Transform to {@link DateFormat#MEDIUM} time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"10:11:12 AM"</js> <jc>// en_US</jc>
		 * 	<li><js>"10:11:12"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"오전 10:11:12"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		MEDIUM_T,

		/**
		 * Transform to {@link DateFormat#SHORT} time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"10:11 AM"</js> <jc>// en_US</jc>
		 * 	<li><js>"10:11 AM"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"오전 10:11"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		SHORT_T,

		/**
		 * Transform to {@link DateFormat#FULL} date-time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"Saturday, March 3, 2001 10:11:12 AM GMT"</js> <jc>// en_US</jc>
		 * 	<li><js>"2001年3月3日 10時11分12秒 GMT"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"2001년 3월 3일 토요일 오전 10시 11분 12초 GMT"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		FULL_DT,

		/**
		 * Transform to {@link DateFormat#LONG} date-time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"March 3, 2001 10:11:12 AM GMT"</js> <jc>// en_US</jc>
		 * 	<li><js>"2001/03/03 10:11:12 GMT"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"2001년 3월 3일 (토) 오전 10시 11분 12초"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		LONG_DT,

		/**
		 * Transform to {@link DateFormat#MEDIUM} date-time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"Mar 3, 2001 10:11:12 AM"</js> <jc>// en_US</jc>
		 * 	<li><js>"2001/03/03 10:11:12"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"2001. 3. 3 오전 10:11:12"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		MEDIUM_DT,

		/**
		 * Transform to {@link DateFormat#SHORT} date-time strings.
		 *
		 * <h5 class='section'>Example Output:</h5>
		 * <ul>
		 * 	<li><js>"3/3/01 10:11 AM"</js> <jc>// en_US</jc>
		 * 	<li><js>"01/03/03 10:11"</js> <jc>// ja_JP</jc>
		 * 	<li><js>"01. 3. 3 오전 10:11"</js> <jc>// ko_KR</jc>
		 * </ul>
		 */
		SHORT_DT
	}

	private static ThreadLocal<Map<DateFormatKey,DateFormat>> patternCache = new ThreadLocal<>();

	static class DateFormatKey {
		final CalendarUtils.Format format;
		final Locale locale;
		final TimeZone timeZone;
		final int hashCode;

		DateFormatKey(CalendarUtils.Format format, Locale locale, TimeZone timeZone) {
			this.format = format;
			this.locale = locale;
			this.timeZone = timeZone;
			this.hashCode = format.hashCode() + locale.hashCode() + timeZone.hashCode();
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof DateFormatKey) && eq(this, (DateFormatKey)o, (x,y)->eq(x.format, y.format) && eq(x.locale, y.locale) && eq(x.timeZone, y.timeZone));
		}
	}


	private static DateFormat getFormat(CalendarUtils.Format format, Locale locale, TimeZone timeZone) {

		if (locale == null)
			locale = Locale.getDefault();

		if (timeZone == null)
			timeZone = TimeZone.getDefault();

		DateFormatKey key = new DateFormatKey(format, locale, timeZone);

		Map<DateFormatKey,DateFormat> m1 = patternCache.get();
		if (m1 == null) {
			m1 = new ConcurrentHashMap<>();
			patternCache.set(m1);
		}

		DateFormat df = m1.get(key);

		if (df == null) {
			String p = null;
			switch (format) {
				case ISO8601_DTL: p = "yyyy-MM-dd'T'HH:mm:ss"; break;
				case ISO8601_D: p = "yyyy-MM-dd"; break;
				case TO_STRING: p = "EEE MMM dd HH:mm:ss zzz yyyy"; break;
				case RFC2822_DT: p = "EEE, dd MMM yyyy HH:mm:ss Z"; break;
				case RFC2822_DTZ: p = "EEE, dd MMM yyyy HH:mm:ss 'GMT'"; break;
				case RFC2822_D: p = "dd MMM yyyy"; break;
				case SIMPLE_DT: p = "yyyy/MM/dd HH:mm:ss"; break;
				case SIMPLE_D: p = "yyyy/MM/dd"; break;
				case SIMPLE_T: p = "HH:mm:ss"; break;
				case FULL_D: df = DateFormat.getDateInstance(DateFormat.FULL, locale); break;
				case LONG_D: df = DateFormat.getDateInstance(DateFormat.LONG, locale); break;
				case MEDIUM_D: df = DateFormat.getDateInstance(DateFormat.MEDIUM, locale); break;
				case SHORT_D: df = DateFormat.getDateInstance(DateFormat.SHORT, locale); break;
				case FULL_T: df = DateFormat.getTimeInstance(DateFormat.FULL, locale); break;
				case LONG_T: df = DateFormat.getTimeInstance(DateFormat.LONG, locale); break;
				case MEDIUM_T: df = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale); break;
				case SHORT_T: df = DateFormat.getTimeInstance(DateFormat.SHORT, locale); break;
				case FULL_DT: df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale); break;
				case LONG_DT: df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale); break;
				case MEDIUM_DT: df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale); break;
				case SHORT_DT: df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale); break;
				default: return null;
			}
			if (p != null) {
				df = new SimpleDateFormat(p, locale);
			}
			if (df != null)
				df.setTimeZone(timeZone);
			m1.put(key, df);
		}

		return df;
	}

	/**
	 * Converts the specified calendar to a string of the specified format.
	 *
	 * @param c The calendar to serialize.
	 * @param format The date format.
	 * @param locale The locale to use.  If <jk>null</jk>, uses {@link Locale#getDefault()}.
	 * @param timeZone The time zone to use.  If <jk>null</jk>, uses {@link TimeZone#getDefault()}.
	 * @return The serialized date, or <jk>null</jk> if the calendar was <jk>null</jk>.
	 */
	public static final String serialize(Calendar c, CalendarUtils.Format format, Locale locale, TimeZone timeZone) {
		if (c == null)
			return null;
		if (timeZone == null)
			timeZone = c.getTimeZone();
		switch(format) {
			case ISO8601_DTL:
			case ISO8601_D:
			case RFC2822_D:
			case RFC2822_DT:
			case TO_STRING:
			case FULL_D:
			case FULL_DT:
			case FULL_T:
			case LONG_D:
			case LONG_DT:
			case LONG_T:
			case MEDIUM_D:
			case MEDIUM_DT:
			case MEDIUM_T:
			case SHORT_D:
			case SHORT_DT:
			case SHORT_T:
			case SIMPLE_D:
			case SIMPLE_DT:
			case SIMPLE_T:
				return serializeFromDateFormat(c.getTime(), format, locale, timeZone);
			case ISO8601_DT:
				return DatatypeConverter.printDateTime(setTimeZone(c, timeZone));
			case ISO8601_DTP:
				String s = DatatypeConverter.printDateTime(setTimeZone(c, timeZone));
				return String.format("%s.%03d%s", s.substring(0, 19), c.get(Calendar.MILLISECOND), s.substring(19));
			case ISO8601_DTZ:
				if (c.getTimeZone().getRawOffset() != 0) {
					Calendar c2 = Calendar.getInstance(GMT);
					c2.setTime(c.getTime());
					c = c2;
				}
				return DatatypeConverter.printDateTime(c);
			case ISO8601_DTPZ:
				if (c.getTimeZone().getRawOffset() != 0) {
					Calendar c2 = Calendar.getInstance(GMT);
					c2.setTime(c.getTime());
					c = c2;
				}
				s = DatatypeConverter.printDateTime(c);
				return String.format("%s.%03d%s", s.substring(0, 19), c.get(Calendar.MILLISECOND), s.substring(19));
			case RFC2822_DTZ:
				return serializeFromDateFormat(c.getTime(), format, locale, GMT);
		default:
			break;
		}
		return null;
	}

	/**
	 * Converts the specified date to a string of the specified format.
	 *
	 * @param format The date format.
	 * @param d The date to serialize.
	 * @param locale The locale to use.  If <jk>null</jk>, uses {@link Locale#getDefault()}.
	 * @param timeZone The time zone to use.  If <jk>null</jk>, uses {@link TimeZone#getDefault()}.
	 * @return The serialized date, or <jk>null</jk> if the calendar was <jk>null</jk>.
	 */
	public static final String serialize(Date d, CalendarUtils.Format format, Locale locale, TimeZone timeZone) {
		if (d == null)
			return null;
		if (timeZone == null)
			timeZone = TimeZone.getDefault();
		switch(format) {
			case ISO8601_DTL:
			case ISO8601_D:
			case RFC2822_D:
			case RFC2822_DT:
			case TO_STRING:
			case FULL_D:
			case FULL_DT:
			case FULL_T:
			case LONG_D:
			case LONG_DT:
			case LONG_T:
			case MEDIUM_D:
			case MEDIUM_DT:
			case MEDIUM_T:
			case SHORT_D:
			case SHORT_DT:
			case SHORT_T:
			case SIMPLE_D:
			case SIMPLE_DT:
			case SIMPLE_T:
				return serializeFromDateFormat(d, format, locale, timeZone);
			case ISO8601_DT:
				Calendar c = new GregorianCalendar();
				c.setTime(d);
				c.setTimeZone(timeZone);
				return DatatypeConverter.printDateTime(c);
			case ISO8601_DTP:
				c = new GregorianCalendar();
				c.setTime(d);
				c.setTimeZone(timeZone);
				String s = DatatypeConverter.printDateTime(setTimeZone(c, timeZone));
				return String.format("%s.%03d%s", s.substring(0, 19), c.get(Calendar.MILLISECOND), s.substring(19));
			case ISO8601_DTZ:
				c = new GregorianCalendar();
				c.setTime(d);
				c.setTimeZone(GMT);
				return DatatypeConverter.printDateTime(c);
			case ISO8601_DTPZ:
				c = new GregorianCalendar();
				c.setTime(d);
				c.setTimeZone(GMT);
				s = DatatypeConverter.printDateTime(c);
				return String.format("%s.%03d%s", s.substring(0, 19), c.get(Calendar.MILLISECOND), s.substring(19));
			case RFC2822_DTZ:
				return serializeFromDateFormat(d, format, locale, GMT);
		}
		return null;
	}


	/**
	 * Converts the specified serialized date back into a {@link Calendar} object.
	 *
	 * @param format The date format.
	 * @param in The serialized date.
	 * @param locale
	 * 	The locale to use.
	 * 	If <jk>null</jk>, uses {@link Locale#getDefault()}.
	 * @param timeZone
	 * 	The timezone to assume if input string doesn't contain timezone info.
	 * 	If <jk>null</jk>, uses {@link TimeZone#getDefault()}.
	 * @return The date as a {@link Calendar}, or <jk>null</jk> if the input was <jk>null</jk> or empty.
	 * @throws java.text.ParseException Malformed input encountered.
	 */
	public static final Calendar parseCalendar(String in, CalendarUtils.Format format, Locale locale, TimeZone timeZone) throws java.text.ParseException {
		if (isEmpty(in))
			return null;
		if (timeZone == null)
			timeZone = TimeZone.getDefault();
		Date d = null;
		switch(format) {

			// These use DatatypeConverter to parse the date.
			case ISO8601_DTL:
			case ISO8601_DT:
			case ISO8601_DTZ:
			case ISO8601_DTP:
			case ISO8601_DTPZ:
			case ISO8601_D:
				return DatatypeConverter.parseDateTime(toValidISO8601DT(in));

			// These don't specify timezones, so we have to assume the timezone is whatever is specified.
			case RFC2822_D:
			case SIMPLE_DT:
			case SIMPLE_D:
			case SIMPLE_T:
			case FULL_D:
			case LONG_D:
			case MEDIUM_D:
			case SHORT_D:
			case MEDIUM_T:
			case SHORT_T:
			case MEDIUM_DT:
			case SHORT_DT:
				d = getFormat(format, locale, GMT).parse(in);
				d.setTime(d.getTime() - timeZone.getRawOffset());
				break;

			// This is always in GMT.
			case RFC2822_DTZ:
				DateFormat f  = getFormat(format, locale, GMT);
				d = f.parse(in);
				break;

			// These specify timezones in the strings, so we don't use the specified timezone.
			case TO_STRING:
			case FULL_DT:
			case FULL_T:
			case LONG_DT:
			case LONG_T:
			case RFC2822_DT:
				d = getFormat(format, locale, timeZone).parse(in);
				break;
		}
		if (d == null)
			return null;
		Calendar c = new GregorianCalendar();
		c.setTime(d);
		c.setTimeZone(timeZone);
		return c;
	}

	/**
	 * Converts the specified serialized date back into a {@link Date} object.
	 *
	 * @param format The date format.
	 * @param in The serialized date.
	 * @param locale
	 * 	The locale to use.
	 * 	If <jk>null</jk>, uses {@link Locale#getDefault()}.
	 * @param timeZone
	 * 	The timezone to assume if input string doesn't contain timezone info.
	 * 	If <jk>null</jk>, uses {@link TimeZone#getDefault()}.
	 * @return The date as a {@link Date}, or <jk>null</jk> if the input was <jk>null</jk> or empty.
	 * @throws java.text.ParseException Malformed input encountered.
	 */
	public static final Date parseDate(String in, CalendarUtils.Format format, Locale locale, TimeZone timeZone) throws java.text.ParseException {
		if (isEmpty(in))
			return null;
		if (timeZone == null)
			timeZone = TimeZone.getDefault();
		switch(format) {

			// These use DatatypeConverter to parse the date.
			case ISO8601_DTL:
			case ISO8601_D:
			case ISO8601_DT:
			case ISO8601_DTZ:
			case ISO8601_DTP:
			case ISO8601_DTPZ:
				return DatatypeConverter.parseDateTime(toValidISO8601DT(in)).getTime();

			// These don't specify timezones, so we have to assume the timezone is whatever is specified.
			case FULL_D:
			case LONG_D:
			case MEDIUM_D:
			case MEDIUM_DT:
			case MEDIUM_T:
			case RFC2822_D:
			case SHORT_D:
			case SHORT_DT:
			case SHORT_T:
			case SIMPLE_D:
			case SIMPLE_DT:
			case SIMPLE_T:
				return getFormat(format, locale, timeZone).parse(in);

			// This is always in GMT.
			case RFC2822_DTZ:
				Date d = getFormat(format, locale, TimeZone.getDefault()).parse(in);
				d.setTime(d.getTime() + TimeZone.getDefault().getRawOffset());
				return d;

			// These specify timezones in the strings, so we don't use the specified timezone.
			case TO_STRING:
			case FULL_DT:
			case FULL_T:
			case LONG_DT:
			case LONG_T:
			case RFC2822_DT:
				return getFormat(format, locale, timeZone).parse(in);

		}
		return null;
	}

	private static String serializeFromDateFormat(Date date, CalendarUtils.Format format, Locale locale, TimeZone timeZone) {
		DateFormat df = getFormat(format, locale, timeZone);
		String s = df.format(date);
		return s;
	}

	private static Calendar setTimeZone(Calendar c, TimeZone tz) {
		if (tz != null && ! tz.equals(c.getTimeZone())) {
			c = (Calendar)c.clone();
			c.setTimeZone(tz);
		}
		return c;
	}
}
