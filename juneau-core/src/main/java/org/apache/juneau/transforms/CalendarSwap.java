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
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>{@link ToString} - Transforms to {@link String Strings} using the {@code Date.toString()} method.
 * 	<li>{@link ISO8601DT} - Transforms to ISO8601 date-time strings.
 * 	<li>{@link ISO8601DTZ} - Same as {@link ISO8601DT}, except always serializes in GMT.
 * 	<li>{@link RFC2822DT} - Transforms to RFC2822 date-time strings.
 * 	<li>{@link RFC2822DTZ} - Same as {@link RFC2822DT}, except always serializes in GMT.
 * 	<li>{@link RFC2822D} - Transforms to RFC2822 date strings.
 * 	<li>{@link Simple} - Transforms to simple <js>"yyyy/MM/dd HH:mm:ss"</js> strings.
 * 	<li>{@link Medium} - Transforms to {@link DateFormat#MEDIUM} strings.
 * </ul>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class CalendarSwap extends PojoSwap<Calendar,String> {

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

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
			super("EEE MMM dd HH:mm:ss zzz yyyy");
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
	 *	 </ul>
	 */
	public static class ISO8601DT extends CalendarSwap {

		/** Constructor */
		public ISO8601DT() {}

		@Override /* PojoSwap */
		public Calendar unswap(String o, ClassMeta<?> hint, BeanContext bc) throws ParseException {
			try {
				if (StringUtils.isEmpty(o))
					return null;
				return convert(DatatypeConverter.parseDateTime(o), hint);
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}

		@Override /* PojoSwap */
		public String swap(Calendar o) {
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
		public ISO8601DTZ() {}

		@Override /* PojoSwap */
		public Calendar unswap(String o, ClassMeta<?> hint, BeanContext bc) throws ParseException {
			try {
				if (StringUtils.isEmpty(o))
					return null;
				return convert(DatatypeConverter.parseDateTime(o), hint);
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}

		@Override /* PojoSwap */
		public String swap(Calendar o) {
			if (o.getTimeZone().getRawOffset() != 0) {
				Calendar c = Calendar.getInstance(GMT);
				c.setTime(o.getTime());
				o = c;
			}
			return DatatypeConverter.printDateTime(o);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to RFC2822 date-time strings.
	 */
	public static class RFC2822DT extends CalendarSwap {
		/** Constructor */
		public RFC2822DT() {
			super("EEE, dd MMM yyyy HH:mm:ss Z");
		}
	}

	/**
	 * Same as {@link RFC2822DT}, except always serializes in GMT.
	 *
	 * <h6 class='topic'>Example output:</h6>
	 * <js>"Wed, 31 Jan 2001 12:34:56 +0000"</js>
	 */
	public static class RFC2822DTZ extends CalendarSwap {
		/** Constructor */
		public RFC2822DTZ() {
			super("EEE, dd MMM yyyy HH:mm:ss 'GMT'", GMT);
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to RFC2822 date strings.
	 */
	public static class RFC2822D extends CalendarSwap {
		/** Constructor */
		public RFC2822D() {
			super("dd MMM yyyy");
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to simple <js>"yyyy/MM/dd HH:mm:ss"</js> strings.
	 */
	public static class Simple extends CalendarSwap {
		/** Constructor */
		public Simple() {
			super("yyyy/MM/dd HH:mm:ss");
		}
	}

	/**
	 * Transforms {@link Calendar Calendars} to {@link DateFormat#MEDIUM} strings.
	 */
	public static class Medium extends CalendarSwap {
		/** Constructor */
		public Medium() {
			super(DateFormat.getDateInstance(DateFormat.MEDIUM));
		}
	}

	/** The formatter to convert dates to Strings. */
	private DateFormat format;

	private TimeZone timeZone;

	/**
	 * Default constructor.
	 * <p>
	 * 	This constructor is used when <code>swap()</code> and <code>unswap()</code> are overridden by subclasses.
	 */
	public CalendarSwap() {}

	/**
	 * Construct a transform using the specified date format string that will be
	 * 	used to construct a {@link SimpleDateFormat} that will be used to convert
	 * 	dates to strings.
	 *
	 * @param simpleDateFormat The {@link SimpleDateFormat} pattern.
	 */
	public CalendarSwap(String simpleDateFormat) {
		this(new SimpleDateFormat(simpleDateFormat));
	}

	/**
	 * Construct a transform using the specified date format string that will be
	 * 	used to construct a {@link SimpleDateFormat} that will be used to convert
	 * 	dates to strings.
	 *
	 * @param simpleDateFormat The {@link SimpleDateFormat} pattern.
	 * @param timeZone The time zone to associate with the date pattern.
	 */
	public CalendarSwap(String simpleDateFormat, TimeZone timeZone) {
		this(new SimpleDateFormat(simpleDateFormat));
		format.setTimeZone(timeZone);
		this.timeZone = timeZone;
	}

	/**
	 * Construct a transform using the specified {@link DateFormat} that will be used to convert
	 * 	dates to strings.
	 *
	 * @param format The format to use to convert dates to strings.
	 */
	public CalendarSwap(DateFormat format) {
		super();
		this.format = format;
	}

	/**
	 * Converts the specified {@link Calendar} to a {@link String}.
	 */
	@Override /* PojoSwap */
	public String swap(Calendar o) {
		DateFormat df = format;
		TimeZone tz1 = o.getTimeZone();
		TimeZone tz2 = format.getTimeZone();
		if (timeZone == null && ! tz1.equals(tz2)) {
			df = (DateFormat)format.clone();
			df.setTimeZone(tz1);
		}
		return df.format(o.getTime());
	}

	/**
	 * Converts the specified {@link String} to a {@link Calendar}.
	 */
	@Override /* PojoSwap */
	public Calendar unswap(String o, ClassMeta<?> hint, BeanContext bc) throws ParseException {
		try {
			if (StringUtils.isEmpty(o))
				return null;
			return convert(format.parse(o), hint);
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

	private static Calendar convert(Date in, ClassMeta<?> hint) throws Exception {
		if (hint == null || ! hint.canCreateNewInstance())
			hint = BeanContext.DEFAULT.getClassMeta(GregorianCalendar.class);
		Calendar c = (Calendar)hint.newInstance();
		c.setTime(in);
		return c;
	}
}
