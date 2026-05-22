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

import java.time.*;

/**
 * Supported wire formats for {@link Period} values.
 */
public enum PeriodFormat {

	NOT_SET,
	ISO_8601,
	DAYS;

	/**
	 * Formats the specified period using this format.
	 *
	 * @param value The value to format.
	 * @return The formatted value.
	 */
	public String format(Period value) {
		if (value == null)
			return null;
		return switch (this) {
			case NOT_SET, ISO_8601 -> value.toString();
			case DAYS -> Integer.toString(value.getYears() * 365 + value.getMonths() * 30 + value.getDays());
		};
	}

	/**
	 * Parses the specified wire value using this format.
	 *
	 * @param value The value to parse.
	 * @return The parsed period.
	 */
	public Period parse(String value) {
		if (value == null)
			return null;
		String s = value.trim();
		if (s.isEmpty())
			return null;
		return switch (this) {
			case NOT_SET, ISO_8601 -> Period.parse(s);
			case DAYS -> Period.ofDays(Integer.parseInt(s));
		};
	}
}
