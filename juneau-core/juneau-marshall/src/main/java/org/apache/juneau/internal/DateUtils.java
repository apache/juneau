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
package org.apache.juneau.internal;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.lang.ref.*;
import java.text.*;
import java.time.format.*;
import java.util.*;

import javax.xml.bind.*;

import org.apache.juneau.reflect.*;

/**
 * A utility class for parsing and formatting HTTP dates as used in cookies and other headers.
 *
 * <p>
 * This class handles dates as defined by RFC 2616 section 3.3.1 as well as some other common non-standard formats.
 *
 * <p>
 * This class was copied from HttpClient 4.3.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class DateUtils {

	/**
	 * Date format pattern used to parse HTTP date headers in RFC 1123 format.
	 */
	public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

	/**
	 * Date format pattern used to parse HTTP date headers in RFC 1036 format.
	 */
	public static final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";

	/**
	 * Date format pattern used to parse HTTP date headers in ANSI C <c>asctime()</c> format.
	 */
	public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	static {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(GMT);
		calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}

	/**
	 * Parses an ISO8601 string and converts it to a {@link Calendar}.
	 *
	 * @param s The string to parse.
	 * @return The parsed value, or <jk>null</jk> if the string was <jk>null</jk> or empty.
	 */
	public static Calendar parseISO8601Calendar(String s) {
		if (isEmpty(s))
			return null;
		return DatatypeConverter.parseDateTime(toValidISO8601DT(s));
	}

	/**
	 * Formats the given date according to the specified pattern.
	 *
	 * <p>
	 * The pattern must conform to that used by the {@link SimpleDateFormat simple date format} class.
	 *
	 * @param date The date to format.
	 * @param pattern The pattern to use for formatting the date.
	 * @return A formatted date string.
	 * @throws IllegalArgumentException If the given date pattern is invalid.
	 * @see SimpleDateFormat
	 */
	public static String formatDate(final Date date, final String pattern) {
		final SimpleDateFormat formatter = DateFormatHolder.formatFor(pattern);
		return formatter.format(date);
	}

	/**
	 * Clears thread-local variable containing {@link java.text.DateFormat} cache.
	 */
	public static void clearThreadLocal() {
		DateFormatHolder.clearThreadLocal();
	}

	/**
	 * A factory for {@link SimpleDateFormat}s.
	 *
	 * <p>
	 * The instances are stored in a thread-local way because SimpleDateFormat is not thread-safe as noted in
	 * {@link SimpleDateFormat its javadoc}.
	 */
	static final class DateFormatHolder {
		private static final ThreadLocal<SoftReference<Map<String,SimpleDateFormat>>> THREADLOCAL_FORMATS =
				new ThreadLocal<>() {
			@Override
			protected SoftReference<Map<String,SimpleDateFormat>> initialValue() {
				Map<String,SimpleDateFormat> m = new HashMap<>();
				return new SoftReference<>(m);
			}
		};

		/**
		 * Creates a {@link SimpleDateFormat} for the requested format string.
		 *
		 * @param pattern
		 * 	A non-<c>null</c> format String according to {@link SimpleDateFormat}.
		 * 	The format is not checked against <c>null</c> since all paths go through {@link DateUtils}.
		 * @return
		 * 	The requested format.
		 * 	This simple date-format should not be used to {@link SimpleDateFormat#applyPattern(String) apply} to a
		 * 	different pattern.
		 */
		public static SimpleDateFormat formatFor(final String pattern) {
			final SoftReference<Map<String,SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
			Map<String,SimpleDateFormat> formats = ref.get();
			if (formats == null) {
				formats = new HashMap<>();
				THREADLOCAL_FORMATS.set(new SoftReference<>(formats));
			}
			SimpleDateFormat format = formats.get(pattern);
			if (format == null) {
				format = new SimpleDateFormat(pattern, Locale.US);
				format.setTimeZone(TimeZone.getTimeZone("GMT"));
				formats.put(pattern, format);
			}
			return format;
		}

		public static void clearThreadLocal() {
			THREADLOCAL_FORMATS.remove();
		}
	}

	/**
	 * Pads out an ISO8601 string so that it can be parsed using {@link DatatypeConverter#parseDateTime(String)}.
	 *
	 * <ul>
	 * 	<li><js>"2001-07-04T15:30:45-05:00"</js> -&gt; <js>"2001-07-04T15:30:45-05:00"</js>
	 * 	<li><js>"2001-07-04T15:30:45Z"</js> -&gt; <js>"2001-07-04T15:30:45Z"</js>
	 * 	<li><js>"2001-07-04T15:30:45.1Z"</js> -&gt; <js>"2001-07-04T15:30:45.1Z"</js>
	 * 	<li><js>"2001-07-04T15:30Z"</js> -&gt; <js>"2001-07-04T15:30:00Z"</js>
	 * 	<li><js>"2001-07-04T15:30"</js> -&gt; <js>"2001-07-04T15:30:00"</js>
	 * 	<li><js>"2001-07-04"</js> -&gt; <li><js>"2001-07-04T00:00:00"</js>
	 * 	<li><js>"2001-07"</js> -&gt; <js>"2001-07-01T00:00:00"</js>
	 * 	<li><js>"2001"</js> -&gt; <js>"2001-01-01T00:00:00"</js>
	 * </ul>
	 *
	 * @param in The string to pad.
	 * @return The padded string.
	 */
	public static final String toValidISO8601DT(String in) {

		// "2001-07-04T15:30:45Z"
		final int
			S1 = 1, // Looking for -
			S2 = 2, // Found -, looking for -
			S3 = 3, // Found -, looking for T
			S4 = 4, // Found T, looking for :
			S5 = 5, // Found :, looking for :
			S6 = 6; // Found :

		int state = 1;
		boolean needsT = false;
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			if (state == S1) {
				if (c == '-')
					state = S2;
			} else if (state == S2) {
				if (c == '-')
					state = S3;
			} else if (state == S3) {
				if (c == 'T')
					state = S4;
				if (c == ' ') {
					state = S4;
					needsT = true;
				}
			} else if (state == S4) {
				if (c == ':')
					state = S5;
			} else if (state == S5) {
				if (c == ':')
					state = S6;
			}
		}

		if (needsT)
			in = in.replace(' ', 'T');
		switch(state) {
			case S1: return in + "-01-01T00:00:00";
			case S2: return in + "-01T00:00:00";
			case S3: return in + "T00:00:00";
			case S4: return in + ":00:00";
			case S5: return in + ":00";
			default: return in;
		}
	}

	/**
	 * Returns a {@link DateTimeFormatter} using either a pattern or predefined pattern name.
	 *
	 * @param pattern The pattern (e.g. <js>"yyyy-MM-dd"</js>) or pattern name (e.g. <js>"ISO_INSTANT"</js>).
	 * @return The formatter.
	 */
	public static DateTimeFormatter getFormatter(String pattern) {
		if (isEmpty(pattern))
			return DateTimeFormatter.ISO_INSTANT;
		try {
			FieldInfo fi = ClassInfo.of(DateTimeFormatter.class).getPublicField(x -> x.isStatic() && x.hasName(pattern));
			if (fi != null)
				return (DateTimeFormatter)fi.inner().get(null);
			return DateTimeFormatter.ofPattern(pattern);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw asRuntimeException(e);
		}
	}
}