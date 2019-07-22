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
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.transform.*;

/**
 * Swap that converts {@link Date} objects to and from strings.
 *
 * <p>
 * Uses the {@link DateTimeFormatter} class for converting {@link Date} objects.
 *
 * <p>
 * Serialization is relatively straightforward.  The <c>Date</c> object is converted to a {@linked ZonedDateTime}
 * object using the time zone defined by the {@link BeanSession#getTimeZoneId()} method.  Then the
 * <c>ZonedDateTime</c> object is passed to the {@link DateTimeFormatter#format(TemporalAccessor)} method to produce
 * the string value.
 *
 * <p>
 * Parsing is a little more complicated.  The string is parsed into an intermediate {@link Temporal} object before
 * being converted into a <c>Date</c>.  The intermediate type can be any of the following:
 * <ul>
 * 	<li>{@link LocalDate} - For patterns containing dates without timezones.
 * 	<li>{@link LocalTime} - For patterns containing times without timezones.
 * 	<li>{@link LocalDateTime} - For patterns containing date-times without timezones.
 * 	<li>{@link ZonedDateTime} - For patterns containing date-times with timezones.
 * 	<li>{@link Instant} - For date-times with Zulu timezone.
 * 	<li>{@link OffsetDateTime} - For date-times with timezone offsets.
 * 	<li>{@link OffsetTime} - For times with timezone offsets.
 * </ul>
 */
public class TemporalDateSwap extends StringSwap<Date> {

	/**
	 * Default swap to {@link DateTimeFormatter#BASIC_ISO_DATE}.
	 * <p>
	 * Example: <js>"20111203"</js>
	 */
	public static class BasicIsoDate extends TemporalDateSwap {
		/** Constructor.*/
		public BasicIsoDate() {
			super("BASIC_ISO_DATE", LocalDate.class, true);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_DATE}.
	 * <p>
	 * Example: <js>"2011-12-03+01:00"</js> or <js>"2011-12-03"</js>
	 */
	public static class IsoDate extends TemporalDateSwap {
		/** Constructor.*/
		public IsoDate() {
			super("ISO_DATE", LocalDate.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30+01:00[Europe/Paris]"</js>
	 */
	public static class IsoDateTime extends TemporalDateSwap {
		/** Constructor.*/
		public IsoDateTime() {
			super("ISO_DATE_TIME", ZonedDateTime.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_INSTANT}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30Z"</js>
	 */
	public static class IsoInstant extends TemporalDateSwap {
		/** Constructor.*/
		public IsoInstant() {
			super("ISO_INSTANT", Instant.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_LOCAL_DATE}.
	 * <p>
	 * Example: <js>"2011-12-03"</js>
	 */
	public static class IsoLocalDate extends TemporalDateSwap {
		/** Constructor.*/
		public IsoLocalDate() {
			super("ISO_LOCAL_DATE", LocalDate.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30"</js>
	 */
	public static class IsoLocalDateTime extends TemporalDateSwap {
		/** Constructor.*/
		public IsoLocalDateTime() {
			super("ISO_LOCAL_DATE_TIME", LocalDateTime.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_LOCAL_TIME}.
	 * <p>
	 * Example: <js>"10:15:30"</js>
	 */
	public static class IsoLocalTime extends TemporalDateSwap {
		/** Constructor.*/
		public IsoLocalTime() {
			super("ISO_LOCAL_TIME", LocalTime.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_OFFSET_DATE}.
	 * <p>
	 * Example: <js>"2011-12-03"</js>
	 */
	public static class IsoOffsetDate extends TemporalDateSwap {
		/** Constructor.*/
		public IsoOffsetDate() {
			super("ISO_OFFSET_DATE", LocalDate.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30+01:00"</js>
	 */
	public static class IsoOffsetDateTime extends TemporalDateSwap {
		/** Constructor.*/
		public IsoOffsetDateTime() {
			super("ISO_OFFSET_DATE_TIME", OffsetDateTime.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_OFFSET_TIME}.
	 * <p>
	 * Example: <js>"10:15:30+01:00"</js>
	 */
	public static class IsoOffsetTime extends TemporalDateSwap {
		/** Constructor.*/
		public IsoOffsetTime() {
			super("ISO_OFFSET_TIME", OffsetTime.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_ORDINAL_DATE}.
	 * <p>
	 * Example: <js>"2012-337"</js>
	 */
	public static class IsoOrdinalDate extends TemporalDateSwap {
		/** Constructor.*/
		public IsoOrdinalDate() {
			super("ISO_ORDINAL_DATE", LocalDate.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_TIME}.
	 * <p>
	 * Example: <js>"10:15:30+01:00"</js> or <js>"10:15:30"</js>
	 */
	public static class IsoTime extends TemporalDateSwap {
		/** Constructor.*/
		public IsoTime() {
			super("ISO_TIME", OffsetTime.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_WEEK_DATE}.
	 * <p>
	 * Example: <js>"2012-W48-6"</js>
	 */
	public static class IsoWeekDate extends TemporalDateSwap {
		/** Constructor.*/
		public IsoWeekDate() {
			super("ISO_WEEK_DATE", LocalDate.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30+01:00[Europe/Paris]"</js>
	 */
	public static class IsoZonedDateTime extends TemporalDateSwap {
		/** Constructor.*/
		public IsoZonedDateTime() {
			super("ISO_ZONED_DATE_TIME", ZonedDateTime.class, false);
		}
	};

	/**
	 * Default swap to {@link DateTimeFormatter#RFC_1123_DATE_TIME}.
	 * <p>
	 * Example: <js>"Tue, 3 Jun 2008 11:05:30 GMT"</js>
	 */
	public static class Rfc1123DateTime extends TemporalDateSwap {
		/** Constructor.*/
		public Rfc1123DateTime() {
			super("RFC_1123_DATE_TIME", ZonedDateTime.class, false);
		}
	};


	private final TemporalParser<? extends Temporal> intermediateParser;
	private final DateTimeFormatter formatter;
	private final boolean serializeWithoutTimezone;

	/**
	 * Constructor.
	 *
	 * @param pattern The timestamp format or name of predefined {@link DateTimeFormatter}.
	 * @param intermediateType The intermediate Java 8 data type to parse into before converting to a Date object.
	 * @param serializeWithoutTimezone <jk>true</jk> if the date should be converted to local-date-time before serialization.
	 */
	public TemporalDateSwap(String pattern, Class<? extends Temporal> intermediateType, boolean serializeWithoutTimezone) {
		super(Date.class);
		this.intermediateParser = TemporalParserCache.getTemporalParser(intermediateType);
		this.formatter = DateUtils.getFormatter(pattern);
		this.serializeWithoutTimezone = serializeWithoutTimezone;
	}

	/**
	 * Converts the specified intermediate {@link Temporal} object to a {@link Date} object.
	 *
	 * @param bs The current bean session.
	 * @param temporal The intermediate temporal object.
	 * @return The converted date.
	 */
	protected Date toDate(BeanSession bs, Temporal temporal) {
		return DateUtils.toDate(temporal, bs.getTimeZoneId());
	}

	@Override /* PojoSwap */
	public String swap(BeanSession session, Date o) throws Exception {
		if (o == null)
			return null;
		ZonedDateTime t = o.toInstant().atZone(session.getTimeZoneId());
		return formatter.format(serializeWithoutTimezone ? t.toLocalDateTime() : t);
	}

	@Override /* PojoSwap */
	public Date unswap(BeanSession session, String f, ClassMeta<?> hint) throws Exception {
		if (f == null)
			return null;
		Temporal t = intermediateParser.parse(f, formatter);
		return toDate(session, t);
	}
}
