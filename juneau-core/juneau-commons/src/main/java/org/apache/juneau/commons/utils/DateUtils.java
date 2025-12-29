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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.time.format.*;

import org.apache.juneau.commons.collections.*;

/**
 * A utility class for parsing and formatting HTTP dates as used in cookies and other headers.
 *
 * <p>
 * This class handles dates as defined by RFC 2616 section 3.3.1 as well as some other common non-standard formats.
 *
 * <p>
 * This class was copied from HttpClient 4.3.
 *
 */
public class DateUtils {

	private static final Cache<String, DateTimeFormatter> DATE_TIME_FORMATTER_CACHE = Cache.of(String.class, DateTimeFormatter.class)
		.maxSize(100)
		.supplier(pattern -> {
			if (isEmpty(pattern))
				return DateTimeFormatter.ISO_INSTANT;
			try {
				for (var f : DateTimeFormatter.class.getFields()) {
					if (f.getName().equals(pattern)) {
						return (DateTimeFormatter)f.get(null);
					}
				}
				return DateTimeFormatter.ofPattern(pattern);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw toRex(e);
			}
		})
		.build();

	/**
	 * Returns a {@link DateTimeFormatter} using either a pattern or predefined pattern name.
	 *
	 * @param pattern The pattern (e.g. <js>"yyyy-MM-dd"</js>) or pattern name (e.g. <js>"ISO_INSTANT"</js>).
	 * @return The formatter.
	 */
	public static DateTimeFormatter getDateTimeFormatter(String pattern) {
		return DATE_TIME_FORMATTER_CACHE.get(pattern);
	}
}