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

import static org.apache.juneau.internal.DateUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;
import java.util.*;

import javax.xml.bind.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.transform.*;

/**
 * Transforms {@link Date Dates} to {@link String Strings}.
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * The following direct subclasses are provided for convenience to the following formats:
 * <ul class='spaced-list'>
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

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private final int dateStyle, timeStyle;
	private final String pattern;
	private final TimeZone timeZone;

	/**
	 * Constructor.
	 * <p>
	 * Only one of the <code>pattern</code> or <code>style</code> parameters should
	 *
	 * @param pattern The {@link SimpleDateFormat} pattern.
	 * If <jk>null</jk>, <code>style</code> is used instead.
	 * @param dateStyle The {@link DateFormat} date style (e.g. {@link DateFormat#SHORT}).
	 * Ignored if <code>pattern</code> is not <jk>null</jk>.
	 * Ignored if <code>-1</code>.
	 * @param timeStyle The {@link DateFormat} time style (e.g. {@link DateFormat#SHORT}).
	 * Ignored if <code>pattern</code> is not <jk>null</jk>.
	 * Ignored if <code>-1</code>.
	 * @param timeZone The timeZone to use for dates.  If <jk>null</jk> then the timezone returned
	 * 	by {@link BeanSession#getTimeZone()} is used.
	 */
	protected DateSwap(String pattern, int dateStyle, int timeStyle, TimeZone timeZone) {
		this.pattern = pattern;
		this.dateStyle = dateStyle;
		this.timeStyle = timeStyle;
		this.timeZone = timeZone;
	}

	/**
	 * Transforms {@link Date Dates} to {@link String Strings} using the {@code Date.toString()} method.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * 	<ul>
	 * 	<li><js>"Wed Jul 04 15:30:45 EST 2001"</js>
	 * </ul>
	 */
	public static class ToString extends DateSwap {
		/** Constructor */
		public ToString() {
			super("EEE MMM dd HH:mm:ss zzz yyyy", -1, -1, null);
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

		/** Constructor */
		public ISO8601DT() {
			super(null, -1, -1, null);
		}

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws ParseException {
			try {
				if (isEmpty(o))
					return null;
				return convert(DatatypeConverter.parseDateTime(toValidISO8601DT(o)).getTime(), hint);
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) {
			if (o == null)
				return null;
			Calendar c = new GregorianCalendar();
			c.setTime(o);
			c = setTimeZone(session, c);
			return DatatypeConverter.printDateTime(c);
		}
	}

	/**
	 * Same as {@link ISO8601DT}, except always serializes in GMT.
	 *
	 * <h5 class='section'>Example output:</h5>
	 * <js>"2001-07-04T15:30:45Z"</js>
	 */
	public static class ISO8601DTZ extends DateSwap {

		/** Constructor */
		public ISO8601DTZ() {
			super(null, -1, -1, null);
		}

		@Override /* PojoSwap */
		public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws ParseException {
			try {
				if (isEmpty(o))
					return null;
				return convert(DatatypeConverter.parseDateTime(toValidISO8601DT(o)).getTime(), hint);
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Date o) {
			if (o == null)
				return null;
			Calendar c = new GregorianCalendar();
			c.setTime(o);
			if (c.getTimeZone().getRawOffset() != 0) {
				Calendar c2 = Calendar.getInstance(GMT);
				c2.setTime(c.getTime());
				c = c2;
			}
			return DatatypeConverter.printDateTime(c);
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
		public String swap(BeanSession session, Date o) {
			if (o == null)
				return null;
			Calendar c = new GregorianCalendar();
			c.setTime(o);
			String s = super.swap(session, o);
			return String.format("%s.%03d%s", s.substring(0, 19), c.get(Calendar.MILLISECOND), s.substring(19));
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
		public String swap(BeanSession session, Date o) {
			if (o == null)
				return null;
			Calendar c = new GregorianCalendar();
			c.setTime(o);
			String s = super.swap(session, o);
			return String.format("%s.%03d%s", s.substring(0, 19), c.get(Calendar.MILLISECOND), s.substring(19));
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
		/** Constructor */
		public RFC2822DT() {
			super("EEE, dd MMM yyyy HH:mm:ss Z", -1, -1, null);
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
		/** Constructor */
		public RFC2822DTZ() {
			super("EEE, dd MMM yyyy HH:mm:ss 'GMT'", -1, -1, GMT);
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
		/** Constructor */
		public RFC2822D() {
			super("dd MMM yyyy", -1, -1, null);
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
		/** Constructor */
		public DateTimeSimple() {
			super("yyyy/MM/dd HH:mm:ss", -1, -1, null);
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
		/** Constructor */
		public DateSimple() {
			super("yyyy/MM/dd", -1, -1, null);
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
		/** Constructor */
		public TimeSimple() {
			super("HH:mm:ss", -1, -1, null);
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
		/** Constructor */
		public DateFull() {
			super(null, DateFormat.FULL, -1, null);
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
		/** Constructor */
		public DateLong() {
			super(null, DateFormat.LONG, -1, null);
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
		/** Constructor */
		public DateMedium() {
			super(null, DateFormat.MEDIUM, -1, null);
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
		/** Constructor */
		public DateShort() {
			super(null, DateFormat.SHORT, -1, null);
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
		/** Constructor */
		public TimeFull() {
			super(null, -1, DateFormat.FULL, null);
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
		/** Constructor */
		public TimeLong() {
			super(null, -1, DateFormat.LONG, null);
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
		/** Constructor */
		public TimeMedium() {
			super(null, -1, DateFormat.MEDIUM, null);
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
		/** Constructor */
		public TimeShort() {
			super(null, -1, DateFormat.SHORT, null);
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
		/** Constructor */
		public DateTimeFull() {
			super(null, DateFormat.FULL, DateFormat.FULL, null);
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
		/** Constructor */
		public DateTimeLong() {
			super(null, DateFormat.LONG, DateFormat.LONG, null);
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
		/** Constructor */
		public DateTimeMedium() {
			super(null, DateFormat.MEDIUM, DateFormat.MEDIUM, null);
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
		/** Constructor */
		public DateTimeShort() {
			super(null, DateFormat.SHORT, DateFormat.SHORT, null);
		}
	}

	/**
	 * Returns the {@link DateFormat} object for this session for formatting dates.
	 *
	 * @param session The current bean session.
	 * @return The {@link DateFormat} object.  Multiple calls to this method on the same
	 * 	session will return a cached copy of date format object.
	 */
	protected DateFormat getDateFormat(BeanSession session) {
		DateFormat df = session.getFromCache(DateFormat.class, this.getClass().getName());
		if (df == null) {
			if (pattern != null)
				df = new SimpleDateFormat(pattern, session.getLocale());
			else {
				if (dateStyle == -1 && timeStyle != -1)
					df = DateFormat.getTimeInstance(timeStyle, session.getLocale());
				else if (dateStyle != -1 && timeStyle == -1)
					df = DateFormat.getDateInstance(dateStyle, session.getLocale());
				else
					df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, session.getLocale());
			}
			if (timeZone != null)
				df.setTimeZone(timeZone);
			else if (session.getTimeZone() != null)
				df.setTimeZone(session.getTimeZone());
			session.addToCache(this.getClass().getName(), df);
		}
		return df;
	}

	/**
	 * Converts the specified {@link Date} to a {@link String}.
	 */
	@Override /* PojoSwap */
	public String swap(BeanSession session, Date o) {
		if (o == null)
			return null;
		return getDateFormat(session).format(o.getTime());
	}

	/**
	 * Converts the specified {@link String} to a {@link Date}.
	 */
	@Override /* PojoSwap */
	public Date unswap(BeanSession session, String o, ClassMeta<?> hint) throws ParseException {
		try {
			if (isEmpty(o))
				return null;
			return convert(new Date(getDateFormat(session).parse(o).getTime()), hint);
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	private static Date convert(Date in, ClassMeta<?> hint) throws Exception {
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

	private static Calendar setTimeZone(BeanSession session, Calendar c) {
		TimeZone tz = session.getTimeZone();
		if (tz != null && ! tz.equals(c.getTimeZone())) {
			c = (Calendar)c.clone();
			c.setTimeZone(tz);
		}
		return c;
	}
}
