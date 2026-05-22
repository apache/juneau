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
package org.apache.juneau;

import java.math.*;
import java.time.*;
import java.util.regex.*;

/**
 * Supported wire formats for {@link Duration} values.
 */
public enum DurationFormat {

	NOT_SET,
	ISO_8601,
	ISO_8601_WITH_DAYS,
	NANOS,
	MILLIS,
	SECONDS,
	HOCON;

	private static final Pattern HOCON_PATTERN = Pattern.compile("^([+-]?\\d+(?:\\.\\d+)?)(ns|us|ms|s|m|h|d)$", Pattern.CASE_INSENSITIVE);
	private static final BigDecimal NANOS_PER_SECOND = BigDecimal.valueOf(1_000_000_000L);

	/**
	 * Formats the specified duration using this format.
	 *
	 * @param value The value to format.
	 * @return The formatted value.
	 */
	public String format(Duration value) {
		if (value == null)
			return null;
		return switch (this) {
			case NOT_SET, ISO_8601 -> value.toString();
			case ISO_8601_WITH_DAYS -> formatIso8601WithDays(value);
			case NANOS -> Long.toString(value.toNanos());
			case MILLIS -> Long.toString(value.toMillis());
			case SECONDS -> {
				long seconds = value.getSeconds();
				int nanos = value.getNano();
				yield seconds + "." + String.format("%09d", nanos);
			}
			case HOCON -> formatHocon(value);
		};
	}

	/**
	 * Parses the specified wire value using this format.
	 *
	 * @param value The value to parse.
	 * @return The parsed duration.
	 */
	public Duration parse(String value) {
		if (value == null)
			return null;
		String s = value.trim();
		if (s.isEmpty())
			return null;
		return switch (this) {
			case NOT_SET, ISO_8601, ISO_8601_WITH_DAYS -> Duration.parse(normalizeIso(s));
			case NANOS -> Duration.ofNanos(Long.parseLong(s));
			case MILLIS -> Duration.ofMillis(Long.parseLong(s));
			case SECONDS -> parseSeconds(s);
			case HOCON -> parseHocon(s);
		};
	}

	/**
	 * Returns <jk>true</jk> if this format is numeric on the wire.
	 *
	 * @return <jk>true</jk> if this format emits numeric wire values.
	 */
	public boolean isNumeric() {
		return this == NANOS || this == MILLIS || this == SECONDS;
	}

	private static String normalizeIso(String s) {
		String x = s;
		if (x.startsWith("PT-"))
			x = "-PT" + x.substring(3);
		return x;
	}

	private static Duration parseSeconds(String s) {
		var bd = new BigDecimal(s);
		long seconds = bd.longValue();
		BigDecimal frac = bd.subtract(BigDecimal.valueOf(seconds));
		int nanos = frac.multiply(NANOS_PER_SECOND).intValue();
		return Duration.ofSeconds(seconds, nanos);
	}

	private static String formatIso8601WithDays(Duration value) {
		boolean neg = value.isNegative();
		Duration d = neg ? value.abs() : value;
		long totalSeconds = d.getSeconds();
		int nanos = d.getNano();
		long days = totalSeconds / 86400;
		long rem = totalSeconds % 86400;
		long hours = rem / 3600;
		rem %= 3600;
		long minutes = rem / 60;
		long seconds = rem % 60;

		var sb = new StringBuilder();
		if (neg)
			sb.append('-');
		sb.append('P');
		if (days > 0)
			sb.append(days).append('D');
		if (hours > 0 || minutes > 0 || seconds > 0 || nanos > 0 || days == 0) {
			sb.append('T');
			if (hours > 0)
				sb.append(hours).append('H');
			if (minutes > 0)
				sb.append(minutes).append('M');
			if (seconds > 0 || nanos > 0 || (hours == 0 && minutes == 0))
				sb.append(seconds).append(nanos == 0 ? "" : "." + String.format("%09d", nanos).replaceAll("0+$", "")).append('S');
		}
		return sb.toString();
	}

	private static String formatHocon(Duration value) {
		long nanos = value.toNanos();
		long abs = Math.abs(nanos);
		if (abs == 0)
			return "0s";
		long sign = nanos < 0 ? -1 : 1;
		long[] divisors = {86_400_000_000_000L, 3_600_000_000_000L, 60_000_000_000L, 1_000_000_000L, 1_000_000L, 1_000L, 1L};
		String[] suffixes = {"d", "h", "m", "s", "ms", "us", "ns"};
		for (int i = 0; i < divisors.length; i++) {
			long divisor = divisors[i];
			if (abs % divisor == 0)
				return (sign * (abs / divisor)) + suffixes[i];
		}
		return nanos + "ns";
	}

	private static Duration parseHocon(String s) {
		Matcher m = HOCON_PATTERN.matcher(s);
		if (! m.matches())
			throw new IllegalArgumentException("Invalid HOCON duration: " + s);
		BigDecimal value = new BigDecimal(m.group(1));
		String unit = m.group(2).toLowerCase();
		BigDecimal nanos = switch (unit) {
			case "d" -> value.multiply(BigDecimal.valueOf(86_400_000_000_000L));
			case "h" -> value.multiply(BigDecimal.valueOf(3_600_000_000_000L));
			case "m" -> value.multiply(BigDecimal.valueOf(60_000_000_000L));
			case "s" -> value.multiply(BigDecimal.valueOf(1_000_000_000L));
			case "ms" -> value.multiply(BigDecimal.valueOf(1_000_000L));
			case "us" -> value.multiply(BigDecimal.valueOf(1_000L));
			case "ns" -> value;
			default -> throw new IllegalArgumentException("Invalid HOCON duration unit: " + unit);
		};
		return Duration.ofNanos(nanos.longValue());
	}
}
