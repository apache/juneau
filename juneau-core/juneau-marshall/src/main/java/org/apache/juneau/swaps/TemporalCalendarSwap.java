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
package org.apache.juneau.swaps;

import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.swap.*;

/**
 * Swap that converts {@link Calendar} objects to and from strings.
 *
 * <p>
 * Uses the {@link DateTimeFormatter} class for converting {@link Calendar} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 */
public class TemporalCalendarSwap extends StringSwap<Calendar> {

	/**
	 * Default swap to {@link DateTimeFormatter#BASIC_ISO_DATE}.
	 * <p>
	 * Example: <js>"20111203"</js>
	 */
	public static class BasicIsoDate extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new BasicIsoDate();

		/** Constructor.*/
		public BasicIsoDate() {
			super("BASIC_ISO_DATE");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_DATE}.
	 * <p>
	 * Example: <js>"2011-12-03+01:00"</js> or <js>"2011-12-03"</js>
	 */
	public static class IsoDate extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoDate();

		/** Constructor.*/
		public IsoDate() {
			super("ISO_DATE");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30+01:00[Europe/Paris]"</js>
	 */
	public static class IsoDateTime extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoDateTime();

		/** Constructor.*/
		public IsoDateTime() {
			super("ISO_DATE_TIME");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_INSTANT}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30Z"</js>
	 */
	public static class IsoInstant extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoInstant();

		/** Constructor.*/
		public IsoInstant() {
			super("ISO_INSTANT");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_LOCAL_DATE}.
	 * <p>
	 * Example: <js>"2011-12-03"</js>
	 */
	public static class IsoLocalDate extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoLocalDate();

		/** Constructor.*/
		public IsoLocalDate() {
			super("ISO_LOCAL_DATE");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30"</js>
	 */
	public static class IsoLocalDateTime extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoLocalDateTime();

		/** Constructor.*/
		public IsoLocalDateTime() {
			super("ISO_LOCAL_DATE_TIME");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_LOCAL_TIME}.
	 * <p>
	 * Example: <js>"10:15:30"</js>
	 */
	public static class IsoLocalTime extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoLocalTime();

		/** Constructor.*/
		public IsoLocalTime() {
			super("ISO_LOCAL_TIME");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_OFFSET_DATE}.
	 * <p>
	 * Example: <js>"2011-12-03"</js>
	 */
	public static class IsoOffsetDate extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoOffsetDate();

		/** Constructor.*/
		public IsoOffsetDate() {
			super("ISO_OFFSET_DATE");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30+01:00"</js>
	 */
	public static class IsoOffsetDateTime extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoOffsetDateTime();

		/** Constructor.*/
		public IsoOffsetDateTime() {
			super("ISO_OFFSET_DATE_TIME");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_OFFSET_TIME}.
	 * <p>
	 * Example: <js>"10:15:30+01:00"</js>
	 */
	public static class IsoOffsetTime extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoOffsetTime();

		/** Constructor.*/
		public IsoOffsetTime() {
			super("ISO_OFFSET_TIME");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_ORDINAL_DATE}.
	 * <p>
	 * Example: <js>"2012-337"</js>
	 */
	public static class IsoOrdinalDate extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoOrdinalDate();

		/** Constructor.*/
		public IsoOrdinalDate() {
			super("ISO_ORDINAL_DATE");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_TIME}.
	 * <p>
	 * Example: <js>"10:15:30+01:00"</js> or <js>"10:15:30"</js>
	 */
	public static class IsoTime extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoTime();

		/** Constructor.*/
		public IsoTime() {
			super("ISO_TIME");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_WEEK_DATE}.
	 * <p>
	 * Example: <js>"2012-W48-6"</js>
	 */
	public static class IsoWeekDate extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoWeekDate();

		/** Constructor.*/
		public IsoWeekDate() {
			super("ISO_WEEK_DATE");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30+01:00[Europe/Paris]"</js>
	 */
	public static class IsoZonedDateTime extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new IsoZonedDateTime();

		/** Constructor.*/
		public IsoZonedDateTime() {
			super("ISO_ZONED_DATE_TIME");
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#RFC_1123_DATE_TIME}.
	 * <p>
	 * Example: <js>"Tue, 3 Jun 2008 11:05:30 GMT"</js>
	 */
	public static class Rfc1123DateTime extends TemporalCalendarSwap {

		/** Default instance.*/
		public static final TemporalCalendarSwap DEFAULT = new Rfc1123DateTime();

		/** Constructor.*/
		public Rfc1123DateTime() {
			super("RFC_1123_DATE_TIME");
		}
	};


	private final DateTimeFormatter formatter;

	/**
	 * Constructor.
	 *
	 * @param pattern The timestamp format or name of predefined {@link DateTimeFormatter}.
	 */
	public TemporalCalendarSwap(String pattern) {
		super(Calendar.class);
		this.formatter = DateUtils.getFormatter(pattern);
	}

	@Override /* ObjectSwap */
	public String swap(BeanSession session, Calendar o) throws Exception {
		if (o == null)
			return null;
		ZonedDateTime t = o instanceof GregorianCalendar ? ((GregorianCalendar)o).toZonedDateTime() : o.toInstant().atZone(session.getTimeZoneId());
		return formatter.format(t);
	}

	@Override /* ObjectSwap */
	public Calendar unswap(BeanSession session, String f, ClassMeta<?> hint) throws Exception {
		if (f == null)
			return null;

		ZoneId offset = session.getTimeZoneId();
		TemporalAccessor ta = new DefaultingTemporalAccessor(formatter.parse(f), offset);
		return GregorianCalendar.from(ZonedDateTime.from(ta));
	}
}
