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

import java.text.*;
import java.util.*;

import javax.xml.bind.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.transform.*;

/**
 * Transforms {@link Calendar Calendars} to {@link String Strings}.
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for convenience to the following formats:
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
public class CalendarSwap extends PojoSwap<Calendar,String> {

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
	 * 	If <jk>null</jk>, <code>style</code> is used instead.
	 * @param dateStyle The {@link DateFormat} date style (e.g. {@link DateFormat#SHORT}).
	 * 	Ignored if <code>pattern</code> is not <jk>null</jk>.
	 * 	Ignored if <code>-1</code>.
	 * @param timeStyle The {@link DateFormat} time style (e.g. {@link DateFormat#SHORT}).
	 * 	Ignored if <code>pattern</code> is not <jk>null</jk>.
	 * 	Ignored if <code>-1</code>.
	 * @param timeZone The timeZone to use for dates.  If <jk>null</jk> then either the
	 * 	timezone specified on the {@link Calendar} object or the timezone returned
	 * 	by {@link BeanSession#getTimeZone()} is used.
	 */
	protected CalendarSwap(String pattern, int dateStyle, int timeStyle, TimeZone timeZone) {
		this.pattern = pattern;
		this.dateStyle = dateStyle;
		this.timeStyle = timeStyle;
		this.timeZone = timeZone;
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link String Strings} using the {@code Date.toString()} method.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * 	<ul>
	 * 	<li><js>"Wed Jul 04 15:30:45 EST 2001"</js>
	 * </ul>
	 */
	public static class ToString extends CalendarSwap {
		/** Constructor */
		public ToString() {
			super("EEE MMM dd HH:mm:ss zzz yyyy", -1, -1, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to ISO8601 date-time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
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
	public static class ISO8601DT extends CalendarSwap {

		/** Constructor */
		public ISO8601DT() {
			super(null, -1, -1, null);
		}

		@Override /* PojoSwap */
		public Calendar unswap(BeanSession session, String o, ClassMeta<?> hint) throws ParseException {
			try {
				if (StringUtils.isEmpty(o))
					return null;
				return convert(DatatypeConverter.parseDateTime(o), hint);
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Calendar o) {
			o = setTimeZone(session, o);
			return DatatypeConverter.printDateTime(o);
		}
	}

	/**
	 * Same as {@link ISO8601DT}, except always serializes in GMT.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <js>"2001-07-04T15:30:45Z"</js>
	 */
	public static class ISO8601DTZ extends CalendarSwap {

		/** Constructor */
		public ISO8601DTZ() {
			super(null, -1, -1, null);
		}

		@Override /* PojoSwap */
		public Calendar unswap(BeanSession session, String o, ClassMeta<?> hint) throws ParseException {
			try {
				if (StringUtils.isEmpty(o))
					return null;
				return convert(DatatypeConverter.parseDateTime(o), hint);
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}

		@Override /* PojoSwap */
		public String swap(BeanSession session, Calendar o) {
			if (o.getTimeZone().getRawOffset() != 0) {
				Calendar c = Calendar.getInstance(GMT);
				c.setTime(o.getTime());
				o = c;
			}
			return DatatypeConverter.printDateTime(o);
		}
	}

	/**
	 * Same as {@link CalendarSwap.ISO8601DT} except serializes to millisecond precision.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <js>"2001-07-04T15:30:45.123Z"</js>
	 */
	public static class ISO8601DTP extends ISO8601DT {

		@Override /* PojoSwap */
		public String swap(BeanSession session, Calendar o) {
			String s = super.swap(session, o);
			return String.format("%s.%03d%s", s.substring(0, 19), o.get(Calendar.MILLISECOND), s.substring(19));
		}
	}

	/**
	 * Same as {@link CalendarSwap.ISO8601DTZ} except serializes to millisecond precision.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <js>"2001-07-04T15:30:45.123"</js>
	 */
	public static class ISO8601DTPZ extends ISO8601DTZ {

		@Override /* PojoSwap */
		public String swap(BeanSession session, Calendar o) {
			String s = super.swap(session, o);
			return String.format("%s.%03d%s", s.substring(0, 19), o.get(Calendar.MILLISECOND), s.substring(19));
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to RFC2822 date-time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"Sat, 03 Mar 2001 10:11:12 +0000"</js> <jc>// en_US</jc>
	 * 	<li><js>"土, 03 3 2001 10:11:12 +0000"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"토, 03 3월 2001 10:11:12 +0000"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class RFC2822DT extends CalendarSwap {
		/** Constructor */
		public RFC2822DT() {
			super("EEE, dd MMM yyyy HH:mm:ss Z", -1, -1, null);
		}
	}

	/**
	 * Same as {@link RFC2822DT}, except always serializes in GMT.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"Sat, 03 Mar 2001 10:11:12 GMT"</js> <jc>// en_US</jc>
	 * 	<li><js>"土, 03 3 2001 10:11:12 GMT"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"토, 03 3월 2001 10:11:12 GMT"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class RFC2822DTZ extends CalendarSwap {
		/** Constructor */
		public RFC2822DTZ() {
			super("EEE, dd MMM yyyy HH:mm:ss 'GMT'", -1, -1, GMT);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to RFC2822 date strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"03 Mar 2001"</js> <jc>// en_US</jc>
	 * 	<li><js>"03 3 2001"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"03 3월 2001"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class RFC2822D extends CalendarSwap {
		/** Constructor */
		public RFC2822D() {
			super("dd MMM yyyy", -1, -1, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to simple <js>"yyyy/MM/dd HH:mm:ss"</js> date-time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"2001/03/03 10:11:12"</js>
	 * </ul>
	 */
	public static class DateTimeSimple extends CalendarSwap {
		/** Constructor */
		public DateTimeSimple() {
			super("yyyy/MM/dd HH:mm:ss", -1, -1, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to simple <js>"yyyy/MM/dd"</js> date strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"2001/03/03"</js>
	 * </ul>
	 */
	public static class DateSimple extends CalendarSwap {
		/** Constructor */
		public DateSimple() {
			super("yyyy/MM/dd", -1, -1, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to simple <js>"HH:mm:ss"</js> time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"10:11:12"</js>
	 * </ul>
	 */
	public static class TimeSimple extends CalendarSwap {
		/** Constructor */
		public TimeSimple() {
			super("HH:mm:ss", -1, -1, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#FULL} date strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"Saturday, March 3, 2001"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001年3月3日"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001년 3월 3일 토요일"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateFull extends CalendarSwap {
		/** Constructor */
		public DateFull() {
			super(null, DateFormat.FULL, -1, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#LONG} date strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"March 3, 2001"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001/03/03"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001년 3월 3일 (토)"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateLong extends CalendarSwap {
		/** Constructor */
		public DateLong() {
			super(null, DateFormat.LONG, -1, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#MEDIUM} date strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"Mar 3, 2001"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001/03/03"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001. 3. 3"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateMedium extends CalendarSwap {
		/** Constructor */
		public DateMedium() {
			super(null, DateFormat.MEDIUM, -1, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#SHORT} date strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"3/3/01"</js> <jc>// en_US</jc>
	 * 	<li><js>"01/03/03"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"01. 3. 3"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateShort extends CalendarSwap {
		/** Constructor */
		public DateShort() {
			super(null, DateFormat.SHORT, -1, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#FULL} time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"10:11:12 AM GMT"</js> <jc>// en_US</jc>
	 * 	<li><js>"10時11分12秒 GMT"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"오전 10시 11분 12초 GMT"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class TimeFull extends CalendarSwap {
		/** Constructor */
		public TimeFull() {
			super(null, -1, DateFormat.FULL, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#LONG} time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"10:11:12 AM GMT"</js> <jc>// en_US</jc>
	 * 	<li><js>"10:11:12 GMT"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"오전 10시 11분 12초"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class TimeLong extends CalendarSwap {
		/** Constructor */
		public TimeLong() {
			super(null, -1, DateFormat.LONG, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#MEDIUM} time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"10:11:12 AM"</js> <jc>// en_US</jc>
	 * 	<li><js>"10:11:12"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"오전 10:11:12"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class TimeMedium extends CalendarSwap {
		/** Constructor */
		public TimeMedium() {
			super(null, -1, DateFormat.MEDIUM, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#SHORT} time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"10:11 AM"</js> <jc>// en_US</jc>
	 * 	<li><js>"10:11 AM"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"오전 10:11"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class TimeShort extends CalendarSwap {
		/** Constructor */
		public TimeShort() {
			super(null, -1, DateFormat.SHORT, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#FULL} date-time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"Saturday, March 3, 2001 10:11:12 AM GMT"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001年3月3日 10時11分12秒 GMT"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001년 3월 3일 토요일 오전 10시 11분 12초 GMT"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateTimeFull extends CalendarSwap {
		/** Constructor */
		public DateTimeFull() {
			super(null, DateFormat.FULL, DateFormat.FULL, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#LONG} date-time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"March 3, 2001 10:11:12 AM GMT"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001/03/03 10:11:12 GMT"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001년 3월 3일 (토) 오전 10시 11분 12초"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateTimeLong extends CalendarSwap {
		/** Constructor */
		public DateTimeLong() {
			super(null, DateFormat.LONG, DateFormat.LONG, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#MEDIUM} date-time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"Mar 3, 2001 10:11:12 AM"</js> <jc>// en_US</jc>
	 * 	<li><js>"2001/03/03 10:11:12"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"2001. 3. 3 오전 10:11:12"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateTimeMedium extends CalendarSwap {
		/** Constructor */
		public DateTimeMedium() {
			super(null, DateFormat.MEDIUM, DateFormat.MEDIUM, null);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#SHORT} date-time strings.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <ul>
	 * 	<li><js>"3/3/01 10:11 AM"</js> <jc>// en_US</jc>
	 * 	<li><js>"01/03/03 10:11"</js> <jc>// ja_JP</jc>
	 * 	<li><js>"01. 3. 3 오전 10:11"</js> <jc>// ko_KR</jc>
	 * </ul>
	 */
	public static class DateTimeShort extends CalendarSwap {
		/** Constructor */
		public DateTimeShort() {
			super(null, DateFormat.SHORT, DateFormat.SHORT, null);
		}
	}

	/**
	 * Returns the {@link DateFormat} object for this session for formatting dates.
	 *
	 * @param session The current bean session.
	 * @param c Optional <code>Calendar</code> object to copy <code>TimeZone</code> from if not specified in session or <code>timeZone</code> setting.
	 * @return The {@link DateFormat} object.  Multiple calls to this method on the same
	 * 	session will return a cached copy of date format object.
	 */
	protected DateFormat getDateFormat(BeanSession session, Calendar c) {
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
			else if (c != null && ! c.getTimeZone().equals(df.getTimeZone())) {
				// Don't cache it if we're using the Calendar timezone.
				df.setTimeZone(c.getTimeZone());
				return df;
			}
			session.addToCache(this.getClass().getName(), df);
		}
		return df;
	}

	/**
	 * Converts the specified {@link Calendar} to a {@link String}.
	 */
	@Override /* PojoSwap */
	public String swap(BeanSession session, Calendar o) {
		return getDateFormat(session, o).format(o.getTime());
	}

	/**
	 * Converts the specified {@link String} to a {@link Calendar}.
	 */
	@Override /* PojoSwap */
	public Calendar unswap(BeanSession session, String o, ClassMeta<?> hint) throws ParseException {
		try {
			if (StringUtils.isEmpty(o))
				return null;
			return convert(getDateFormat(session, null).parse(o), hint, session);
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	private static Calendar convert(Calendar in, ClassMeta<?> hint) throws Exception {
		if (hint.isInstance(in) || ! hint.canCreateNewInstance())
			return in;
		Calendar c = (Calendar)hint.newInstance();
		c.setTime(in.getTime());
		c.setTimeZone(in.getTimeZone());
		return c;
	}

	private static Calendar convert(Date in, ClassMeta<?> hint, BeanSession session) throws Exception {
		if (hint == null || ! hint.canCreateNewInstance())
			hint = session.getClassMeta(GregorianCalendar.class);
		Calendar c = (Calendar)hint.newInstance();
		c.setTime(in);
		return c;
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
