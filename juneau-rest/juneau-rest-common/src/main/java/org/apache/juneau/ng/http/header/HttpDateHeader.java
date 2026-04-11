/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.ng.http.header;


import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.juneau.commons.utils.Utils.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

/**
 * Base for HTTP-date headers.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160",
	"unchecked"
})
public class HttpDateHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_ZONED_DATE_TIME = 1;

	private final ZonedDateTime cachedZdt;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	protected HttpDateHeader(String name, String value) {
		super(name, (String)null);
		this.cachedZdt = e(value) ? null : parseHttpDate(value);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpDateHeader(String name, ZonedDateTime value) {
		super(name, (String)null);
		this.cachedZdt = value;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpDateHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? () -> ((Supplier<String>) supplier).get()
			: () -> {
				var z = ((Supplier<ZonedDateTime>) supplier).get();
				return z == null ? null : RFC_1123_DATE_TIME.format(z);
			});
		this.cachedZdt = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<ZonedDateTime> asZonedDateTime() {
		return opt(toZonedDateTime());
	}

	@Override
	public String getValue() {
		if (lazyMode == LAZY_ZONED_DATE_TIME) {
			var x = ((Supplier<ZonedDateTime>) lazySupplier).get();
			return x == null ? null : RFC_1123_DATE_TIME.format(x);
		}
		if (cachedZdt != null)
			return RFC_1123_DATE_TIME.format(cachedZdt);
		return super.getValue();
	}

	public ZonedDateTime orElse(ZonedDateTime other) {
		var x = toZonedDateTime();
		return nn(x) ? x : other;
	}

	public ZonedDateTime toZonedDateTime() {
		if (lazyMode == LAZY_ZONED_DATE_TIME)
			return ((Supplier<ZonedDateTime>) lazySupplier).get();
		if (cachedZdt != null)
			return cachedZdt;
		var v = super.getValue();
		return e(v) ? null : parseHttpDate(v);
	}

	private static ZonedDateTime parseHttpDate(String value) {
		return ZonedDateTime.from(RFC_1123_DATE_TIME.parse(value)).truncatedTo(SECONDS);
	}
}
