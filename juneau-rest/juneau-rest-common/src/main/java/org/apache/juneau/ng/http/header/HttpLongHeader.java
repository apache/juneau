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


import static org.apache.juneau.commons.utils.ThrowableUtils.rex;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

/**
 * Base for long headers.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160",
	"unchecked"
})
public class HttpLongHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_LONG = 1;

	private final Long value;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	protected HttpLongHeader(String name, String wireValue) {
		super(name, wireValue);
		this.value = parseLong(wireValue);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpLongHeader(String name, Long typedValue) {
		super(name, s(typedValue));
		this.value = typedValue;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpLongHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? () -> ((Supplier<String>) supplier).get()
			: () -> s(((Supplier<Long>) supplier).get()));
		this.value = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<Long> asLong() {
		return opt(toLong());
	}

	@Override
	public String getValue() {
		return s(toLong());
	}

	public Long orElse(Long other) {
		var x = toLong();
		return nn(x) ? x : other;
	}

	public Long toLong() {
		if (lazyMode == LAZY_LONG)
			return ((Supplier<Long>) lazySupplier).get();
		if (value != null)
			return value;
		var v = super.getValue();
		return v == null ? null : parseLong(v);
	}

	private static Long parseLong(String value) {
		try {
			return value == null ? null : Long.parseLong(value);
		} catch (NumberFormatException e) {
			throw rex(e, "Value ''{0}'' could not be parsed as a long.", value);
		}
	}
}
