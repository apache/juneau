/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.marshall.swaps.*;

/**
 * Supported wire formats for {@link Calendar} values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#calendarFormat(CalendarFormat)},
 * {@link org.apache.juneau.marshall.Marshalled#calendarFormat()},
 * {@link org.apache.juneau.marshall.MarshalledProp#calendarFormat()}, and
 * {@link org.apache.juneau.marshall.MarshalledConfig#calendarFormat()} to control how
 * {@link Calendar} values (including {@link GregorianCalendar}) are written to and read from the wire.
 *
 * <p>
 * The default is {@link #ISO_OFFSET_DATE_TIME} which preserves the historical wire output produced
 * before this enum existed.
 *
 * <p>
 * Numeric formats ({@link #MILLIS}) emit native numeric wire types on binary serializers
 * (BSON / CBOR / MsgPack) and bare numbers on text formats. Text formats emit a quoted ISO 8601 string for
 * every other constant.
 *
 * <p>
 * {@link XMLGregorianCalendar} fields always emit {@code toXMLFormat()} regardless of the configured
 * {@code CalendarFormat}. The {@link #XML_FORMAT} constant exists so that regular {@code Calendar} /
 * {@code GregorianCalendar} fields can opt into the same wire form for symmetry with their
 * {@code XMLGregorianCalendar} siblings.
 */
public enum CalendarFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/** {@link DateTimeFormatter#BASIC_ISO_DATE}: <js>"20111203"</js>. */
	BASIC_ISO_DATE,

	/** {@link DateTimeFormatter#ISO_DATE}: <js>"2011-12-03+01:00"</js> or <js>"2011-12-03"</js>. */
	ISO_DATE,

	/** {@link DateTimeFormatter#ISO_DATE_TIME}: <js>"2011-12-03T10:15:30+01:00[Europe/Paris]"</js>. */
	ISO_DATE_TIME,

	/** {@link DateTimeFormatter#ISO_INSTANT}: <js>"2011-12-03T10:15:30Z"</js>. */
	ISO_INSTANT,

	/** {@link DateTimeFormatter#ISO_LOCAL_DATE}: <js>"2011-12-03"</js>. */
	ISO_LOCAL_DATE,

	/** {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}: <js>"2011-12-03T10:15:30"</js>. */
	ISO_LOCAL_DATE_TIME,

	/** {@link DateTimeFormatter#ISO_LOCAL_TIME}: <js>"10:15:30"</js>. */
	ISO_LOCAL_TIME,

	/** {@link DateTimeFormatter#ISO_OFFSET_DATE}: <js>"2011-12-03+01:00"</js>. */
	ISO_OFFSET_DATE,

	/** {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME} (the default): <js>"2011-12-03T10:15:30+01:00"</js>. */
	ISO_OFFSET_DATE_TIME,

	/** {@link DateTimeFormatter#ISO_OFFSET_TIME}: <js>"10:15:30+01:00"</js>. */
	ISO_OFFSET_TIME,

	/** {@link DateTimeFormatter#ISO_ORDINAL_DATE}: <js>"2012-337"</js>. */
	ISO_ORDINAL_DATE,

	/** {@link DateTimeFormatter#ISO_TIME}: <js>"10:15:30+01:00"</js> or <js>"10:15:30"</js>. */
	ISO_TIME,

	/** {@link DateTimeFormatter#ISO_WEEK_DATE}: <js>"2012-W48-6"</js>. */
	ISO_WEEK_DATE,

	/** {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}: <js>"2011-12-03T10:15:30+01:00[Europe/Paris]"</js>. */
	ISO_ZONED_DATE_TIME,

	/** {@link DateTimeFormatter#RFC_1123_DATE_TIME}: <js>"Tue, 3 Jun 2008 11:05:30 GMT"</js>. */
	RFC_1123_DATE_TIME,

	/**
	 * Epoch milliseconds as a numeric value.
	 *
	 * <p>
	 * Emitted as a native int64 by binary serializers (BSON / CBOR / MsgPack) and as a bare integer
	 * literal by text serializers.
	 */
	MILLIS,

	/**
	 * {@link XMLGregorianCalendar#toXMLFormat() XML Schema dateTime} form.
	 *
	 * <p>
	 * Lets regular {@link Calendar} / {@link GregorianCalendar} fields opt into the same wire form
	 * that {@link XMLGregorianCalendar} fields always use, for symmetry across mixed-type beans.
	 */
	XML_FORMAT;

	private static final ZoneId Z = ZoneId.of("Z");

	private static final DatatypeFactory DATATYPE_FACTORY;
	static {
		try {
			DATATYPE_FACTORY = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw toRex(e);
		}
	}

	/**
	 * Formats the specified value using this format.
	 *
	 * @param value The value to format. Can be <jk>null</jk>.
	 * @param zoneId The default zone id used for time-only / unzoned values.
	 * @return The formatted string, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public String format(Calendar value, ZoneId zoneId) {
		if (value == null)
			return null;
		if (this == MILLIS)
			return Long.toString(value.getTimeInMillis());
		if (this == XML_FORMAT) {
			var gc = value instanceof GregorianCalendar gc2 ? gc2 : GregorianCalendar.from(value.toInstant().atZone(value.getTimeZone().toZoneId()));
			return DATATYPE_FACTORY.newXMLGregorianCalendar(gc).toXMLFormat();
		}
		var zone = zoneId == null ? value.getTimeZone().toZoneId() : zoneId;
		var zdt = value instanceof GregorianCalendar gc ? gc.toZonedDateTime() : value.toInstant().atZone(zone);
		return formatter().format(zdt);
	}

	/**
	 * Parses the specified wire value using this format.
	 *
	 * @param value The wire value. Can be <jk>null</jk> or blank.
	 * @param zoneId The default zone id used for time-only / unzoned values.
	 * @return The parsed {@link Calendar}, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 */
	public Calendar parse(String value, ZoneId zoneId) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		var zone = zoneId == null ? ZoneId.systemDefault() : zoneId;
		if (this == MILLIS)
			return GregorianCalendar.from(Instant.ofEpochMilli(Long.parseLong(s)).atZone(zone));
		if (this == XML_FORMAT)
			return DATATYPE_FACTORY.newXMLGregorianCalendar(s).toGregorianCalendar();
		var ta = new DefaultingTemporalAccessor(formatter().parse(s), zone);
		return GregorianCalendar.from(ZonedDateTime.from(ta));
	}

	/**
	 * Returns <jk>true</jk> if this format is numeric on the wire.
	 *
	 * @return <jk>true</jk> if this format emits a numeric wire value.
	 */
	public boolean isNumeric() {
		return this == MILLIS;
	}

	private DateTimeFormatter formatter() {
		return switch (this) {
			case NOT_SET, ISO_OFFSET_DATE_TIME -> DateTimeFormatter.ISO_OFFSET_DATE_TIME;
			case BASIC_ISO_DATE -> DateTimeFormatter.BASIC_ISO_DATE;
			case ISO_DATE -> DateTimeFormatter.ISO_DATE;
			case ISO_DATE_TIME -> DateTimeFormatter.ISO_DATE_TIME;
			case ISO_INSTANT -> DateTimeFormatter.ISO_INSTANT;
			case ISO_LOCAL_DATE -> DateTimeFormatter.ISO_LOCAL_DATE;
			case ISO_LOCAL_DATE_TIME -> DateTimeFormatter.ISO_LOCAL_DATE_TIME;
			case ISO_LOCAL_TIME -> DateTimeFormatter.ISO_LOCAL_TIME;
			case ISO_OFFSET_DATE -> DateTimeFormatter.ISO_OFFSET_DATE;
			case ISO_OFFSET_TIME -> DateTimeFormatter.ISO_OFFSET_TIME;
			case ISO_ORDINAL_DATE -> DateTimeFormatter.ISO_ORDINAL_DATE;
			case ISO_TIME -> DateTimeFormatter.ISO_TIME;
			case ISO_WEEK_DATE -> DateTimeFormatter.ISO_WEEK_DATE;
			case ISO_ZONED_DATE_TIME -> DateTimeFormatter.ISO_ZONED_DATE_TIME;
			case RFC_1123_DATE_TIME -> DateTimeFormatter.RFC_1123_DATE_TIME;
			case XML_FORMAT, MILLIS -> throw new IllegalStateException("Format " + this + " does not use a DateTimeFormatter");
		};
	}

	/**
	 * Returns the {@link ZoneId} used by the {@link #ISO_INSTANT} variant.
	 *
	 * @return The fixed {@code "Z"} (UTC) zone id used by the {@link #ISO_INSTANT} variant.
	 */
	public static ZoneId zuluZone() {
		return Z;
	}
}
