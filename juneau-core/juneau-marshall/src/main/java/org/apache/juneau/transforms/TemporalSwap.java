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
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Swap that converts {@link Temporal} objects to strings.
 *
 * <p>
 * Uses the {@link DateTimeFormatter} class for converting {@link Temporal} objects to and from strings.
 *
 * <p>
 * Supports any of the following temporal objects:
 * <ul>
 * 	<li>{@link HijrahDate}
 * 	<li>{@link Instant}
 * 	<li>{@link JapaneseDate}
 * 	<li>{@link LocalDate}
 * 	<li>{@link LocalDateTime}
 * 	<li>{@link LocalTime}
 * 	<li>{@link MinguoDate}
 * 	<li>{@link OffsetDateTime}
 * 	<li>{@link OffsetTime}
 * 	<li>{@link ThaiBuddhistDate}
 * 	<li>{@link Year}
 * 	<li>{@link YearMonth}
 * 	<li>{@link ZonedDateTime}
 * </ul>
 */
public class TemporalSwap extends StringSwap<Temporal> {

	/**
	 * Default swap to {@link DateTimeFormatter#BASIC_ISO_DATE}.
	 * <p>
	 * Example: <js>"20111203"</js>
	 */
	public static class BasicIsoDate extends TemporalSwap {
		/** Constructor.*/
		public BasicIsoDate() {
			super(
				"BASIC_ISO_DATE",
				LocalDate.class,
				ASet.create(LocalDate.class, LocalDateTime.class),
				LocalDate.class,
				ASet.create(LocalDate.class, Year.class, YearMonth.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_DATE}.
	 * <p>
	 * Example: <js>"2011-12-03+01:00"</js> or <js>"2011-12-03"</js>
	 */
	public static class IsoDate extends TemporalSwap {
		/** Constructor.*/
		public IsoDate() {
			super(
				"ISO_DATE",
				LocalDate.class,
				ASet.create(LocalDate.class, LocalDateTime.class, OffsetDateTime.class, ZonedDateTime.class),
				LocalDate.class,
				ASet.create(LocalDate.class),
				true
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30+01:00[Europe/Paris]"</js>
	 */
	public static class IsoDateTime extends TemporalSwap {
		/** Constructor.*/
		public IsoDateTime() {
			super(
				"ISO_DATE_TIME",
				ZonedDateTime.class,
				ASet.create(OffsetDateTime.class, ZonedDateTime.class),
				ZonedDateTime.class,
				ASet.create(Instant.class, LocalDate.class, LocalDateTime.class, LocalTime.class, OffsetDateTime.class, OffsetTime.class, Year.class, YearMonth.class, ZonedDateTime.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_INSTANT}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30Z"</js>
	 */
	public static class IsoInstant extends TemporalSwap {
		/** Constructor.*/
		public IsoInstant() {
			super(
				"ISO_INSTANT",
				Instant.class,
				ASet.create(Instant.class, OffsetDateTime.class, ZonedDateTime.class),
				Instant.class,
				ASet.create(Instant.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_LOCAL_DATE}.
	 * <p>
	 * Example: <js>"2011-12-03"</js>
	 */
	public static class IsoLocalDate extends TemporalSwap {
		/** Constructor.*/
		public IsoLocalDate() {
			super(
				"ISO_LOCAL_DATE",
				LocalDate.class,
				ASet.create(LocalDate.class, LocalDateTime.class, OffsetDateTime.class, ZonedDateTime.class),
				LocalDate.class,
				ASet.create(LocalDate.class, Year.class, YearMonth.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30"</js>
	 */
	public static class IsoLocalDateTime extends TemporalSwap {
		/** Constructor.*/
		public IsoLocalDateTime() {
			super(
				"ISO_LOCAL_DATE_TIME",
				LocalDateTime.class,
				ASet.create(LocalDateTime.class, OffsetDateTime.class, ZonedDateTime.class),
				LocalDateTime.class,
				ASet.create(LocalDate.class, LocalDateTime.class, LocalTime.class, Year.class, YearMonth.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_LOCAL_TIME}.
	 * <p>
	 * Example: <js>"10:15:30"</js>
	 */
	public static class IsoLocalTime extends TemporalSwap {
		/** Constructor.*/
		public IsoLocalTime() {
			super(
				"ISO_LOCAL_TIME",
				LocalTime.class,
				ASet.create(LocalDateTime.class, LocalTime.class, OffsetDateTime.class, OffsetTime.class, ZonedDateTime.class),
				LocalTime.class,
				ASet.create(LocalTime.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_OFFSET_DATE}.
	 * <p>
	 * Example: <js>"2011-12-03"</js>
	 */
	public static class IsoOffsetDate extends TemporalSwap {
		/** Constructor.*/
		public IsoOffsetDate() {
			super(
				"ISO_OFFSET_DATE",
				ZonedDateTime.class,
				ASet.create(OffsetDateTime.class, ZonedDateTime.class),
				LocalDate.class,
				ASet.create(LocalDate.class, Year.class, YearMonth.class),
				true
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30+01:00"</js>
	 */
	public static class IsoOffsetDateTime extends TemporalSwap {
		/** Constructor.*/
		public IsoOffsetDateTime() {
			super(
				"ISO_OFFSET_DATE_TIME",
				OffsetDateTime.class,
				ASet.create(OffsetDateTime.class, ZonedDateTime.class),
				OffsetDateTime.class,
				ASet.create(Instant.class, LocalDate.class, LocalDateTime.class, LocalTime.class, OffsetDateTime.class, OffsetTime.class, Year.class, YearMonth.class, ZonedDateTime.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_OFFSET_TIME}.
	 * <p>
	 * Example: <js>"10:15:30+01:00"</js>
	 */
	public static class IsoOffsetTime extends TemporalSwap {
		/** Constructor.*/
		public IsoOffsetTime() {
			super(
				"ISO_OFFSET_TIME",
				OffsetTime.class,
				ASet.create(OffsetDateTime.class, OffsetTime.class, ZonedDateTime.class),
				OffsetTime.class,
				ASet.create(LocalTime.class, OffsetTime.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_ORDINAL_DATE}.
	 * <p>
	 * Example: <js>"2012-337"</js>
	 */
	public static class IsoOrdinalDate extends TemporalSwap {
		/** Constructor.*/
		public IsoOrdinalDate() {
			super(
				"ISO_ORDINAL_DATE",
				LocalDate.class,
				ASet.create(LocalDate.class, LocalDateTime.class, OffsetDateTime.class, ZonedDateTime.class),
				LocalDate.class,
				ASet.create(LocalDate.class, Year.class, YearMonth.class),
				true
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_TIME}.
	 * <p>
	 * Example: <js>"10:15:30+01:00"</js> or <js>"10:15:30"</js>
	 */
	public static class IsoTime extends TemporalSwap {
		/** Constructor.*/
		public IsoTime() {
			super(
				"ISO_TIME",
				OffsetTime.class,
				ASet.create(LocalTime.class, OffsetDateTime.class, OffsetTime.class, ZonedDateTime.class),
				OffsetTime.class,
				ASet.create(LocalTime.class, OffsetTime.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_WEEK_DATE}.
	 * <p>
	 * Example: <js>"2012-W48-6"</js>
	 */
	public static class IsoWeekDate extends TemporalSwap {
		/** Constructor.*/
		public IsoWeekDate() {
			super(
				"ISO_WEEK_DATE",
				LocalDate.class,
				ASet.create(LocalDate.class, LocalDateTime.class, OffsetDateTime.class, ZonedDateTime.class),
				LocalDate.class,
				ASet.create(LocalDate.class, Year.class, YearMonth.class),
				true
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_WEEK_DATE}.
	 * <p>
	 * Example: <js>"2011"</js>
	 */
	public static class IsoYear extends TemporalSwap {
		/** Constructor.*/
		public IsoYear() {
			super(
				"uuuu",
				Year.class,
				ASet.create(LocalDate.class, LocalDateTime.class, OffsetDateTime.class, Year.class, YearMonth.class, ZonedDateTime.class),
				Year.class,
				ASet.create(Year.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_WEEK_DATE}.
	 * <p>
	 * Example: <js>"2011-12"</js>
	 */
	public static class IsoYearMonth extends TemporalSwap {
		/** Constructor.*/
		public IsoYearMonth() {
			super(
				"uuuu-MM",
				YearMonth.class,
				ASet.create(LocalDate.class, LocalDateTime.class, OffsetDateTime.class, YearMonth.class, ZonedDateTime.class),
				YearMonth.class,
				ASet.create(Year.class, YearMonth.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}.
	 * <p>
	 * Example: <js>"2011-12-03T10:15:30+01:00[Europe/Paris]"</js>
	 */
	public static class IsoZonedDateTime extends TemporalSwap {
		/** Constructor.*/
		public IsoZonedDateTime() {
			super(
				"ISO_ZONED_DATE_TIME",
				ZonedDateTime.class,
				ASet.create(LocalDate.class, OffsetDateTime.class, ZonedDateTime.class),
				ZonedDateTime.class,
				ASet.create(Instant.class, LocalDate.class, LocalDateTime.class, LocalTime.class, OffsetDateTime.class, OffsetTime.class, Year.class, YearMonth.class, ZonedDateTime.class),
				false
			);
		}
	}

	/**
	 * Default swap to {@link DateTimeFormatter#RFC_1123_DATE_TIME}.
	 * <p>
	 * Example: <js>"Tue, 3 Jun 2008 11:05:30 GMT"</js>
	 */
	public static class Rfc1123DateTime extends TemporalSwap {
		/** Constructor.*/
		public Rfc1123DateTime() {
			super(
				"RFC_1123_DATE_TIME",
				ZonedDateTime.class,
				ASet.create(OffsetDateTime.class, ZonedDateTime.class),
				ZonedDateTime.class,
				ASet.create(Instant.class, LocalDate.class, LocalDateTime.class, LocalTime.class, OffsetDateTime.class, OffsetTime.class, Year.class, YearMonth.class, ZonedDateTime.class),
				false
			);
		}
	}


	private final TemporalParser<? extends Temporal> intermediateParser;
	private final Class<? extends Temporal> intermediateSerializeType;
	private final DateTimeFormatter formatter;
	private final Set<Class<? extends Temporal>> directSerializeClasses, directParseClasses;
	private final boolean parseTimezoneSeparately;

	/**
	 * Constructor.
	 *
	 * @param pattern The timestamp format or name of predefined {@link DateTimeFormatter}.
	 * @param intermediateSerializeType The intermediate Java 8 data type to convert to before serializing.
	 * @param directSerializeClasses Classes that can be serialized directly into without having to go through the intermediate type.
	 * @param intermediateParseType The intermediate Java 8 data type to parse into before converting to a Date object.
	 * @param directParseClasses Classes that can be parsed directly into without having to go through the intermediate type.
	 * @param parseTimezoneSeparately <jk>true</jk> if the time zone should be persisted on parsing when {@link LocalDate} is the intermediate type.
	 */
	public TemporalSwap(String pattern, Class<? extends Temporal> intermediateSerializeType, Set<Class<? extends Temporal>> directSerializeClasses, Class<? extends Temporal> intermediateParseType, Set<Class<? extends Temporal>> directParseClasses, boolean parseTimezoneSeparately) {
		super(Temporal.class);
		this.intermediateParser = TemporalParserCache.getTemporalParser(intermediateParseType);
		this.formatter = DateUtils.getFormatter(pattern);
		this.directSerializeClasses = directSerializeClasses == null ? Collections.emptySet() : directSerializeClasses;
		this.directParseClasses = directParseClasses == null ? Collections.emptySet() : directParseClasses;
		this.intermediateSerializeType = intermediateSerializeType;
		this.parseTimezoneSeparately = parseTimezoneSeparately;
	}

	@Override /* PojoSwap */
	public String swap(BeanSession session, Temporal o) throws Exception {
		if (o == null)
			return null;
		if (directSerializeClasses.contains(o.getClass()))
			return formatter.format(o);
		Temporal o2 = DateUtils.toTemporal(o, intermediateSerializeType, session.getTimeZoneId());
		return formatter.format(o2);
	}

	@SuppressWarnings("unchecked")
	@Override /* PojoSwap */
	public Temporal unswap(BeanSession session, String f, ClassMeta<?> hint) throws Exception {
		if (hint == null)
			hint = session.getClassMeta(Instant.class);

		Class<? extends Temporal> tc = (Class<? extends Temporal>)hint.getInnerClass();

		if (directParseClasses.contains(tc))
			return TemporalParserCache.getTemporalParser(tc).parse(f, formatter);

		Temporal t = intermediateParser.parse(f, formatter);

		if (parseTimezoneSeparately) {
			TemporalAccessor ta = formatter.parse(f);
			if (ta.query(TemporalQueries.zone()) != null) {
				ZoneId offset = ZoneId.from(ta);
				t = ((LocalDate)t).atStartOfDay(offset);
			}
 		}

		return DateUtils.toTemporal(t, tc, session.getTimeZoneId());
	}
}
