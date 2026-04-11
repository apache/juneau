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


import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;

/**
 * Base for comma-separated tokens with optional q-values.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160",
	"unchecked"
})
public class HttpStringRangesHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_STRING_RANGES = 1;

	private final StringRanges cachedForStringOrDirect;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	protected HttpStringRangesHeader(String name, String value) {
		super(name, value);
		this.cachedForStringOrDirect = value == null ? null : StringRanges.of(value);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpStringRangesHeader(String name, StringRanges value) {
		super(name, value == null ? null : value.toString());
		this.cachedForStringOrDirect = value;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpStringRangesHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? () -> ((Supplier<String>) supplier).get()
			: () -> {
				var m = ((Supplier<StringRanges>) supplier).get();
				return m == null ? null : m.toString();
			});
		this.cachedForStringOrDirect = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<StringRanges> asStringRanges() {
		return opt(toStringRanges());
	}

	public StringRange getRange(int index) {
		var x = toStringRanges();
		return x == null ? null : x.getRange(index);
	}

	public int match(List<String> names) {
		var x = toStringRanges();
		return x == null ? -1 : x.match(names);
	}

	public StringRanges orElse(StringRanges other) {
		var x = toStringRanges();
		return nn(x) ? x : other;
	}

	public StringRanges toStringRanges() {
		if (lazyMode == LAZY_STRING_RANGES)
			return ((Supplier<StringRanges>) lazySupplier).get();
		if (cachedForStringOrDirect != null)
			return cachedForStringOrDirect;
		var v = super.getValue();
		return v == null ? null : StringRanges.of(v);
	}
}
