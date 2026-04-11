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


import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

/**
 * Base for simple comma-separated token lists.
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S2160",
	"unchecked"
})
public class HttpCsvHeader extends HttpHeaderBean {

	public static final int LAZY_WIRE_STRING = 0;
	public static final int LAZY_TOKENS = 1;

	private final String[] eagerTokens;
	private final Supplier<?> lazySupplier;
	private final int lazyMode;

	protected HttpCsvHeader(String name, String value) {
		super(name, value);
		this.eagerTokens = null;
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpCsvHeader(String name, String... values) {
		super(name, values == null || values.length == 0 ? null : join(values, ", "));
		this.eagerTokens = values == null ? null : copyOf(values);
		this.lazySupplier = null;
		this.lazyMode = -1;
	}

	protected HttpCsvHeader(String name, Supplier<?> supplier, int lazyMode) {
		super(name, lazyMode == LAZY_WIRE_STRING
			? () -> ((Supplier<String>) supplier).get()
			: () -> {
				var t = ((Supplier<String[]>) supplier).get();
				return t == null ? null : join(t, ", ");
			});
		this.eagerTokens = null;
		this.lazySupplier = supplier;
		this.lazyMode = lazyMode;
	}

	public Optional<String[]> asArray() {
		return opt(copyOf(csvTokens()));
	}

	public Optional<List<String>> asList() {
		return opt(u(l(csvTokens())));
	}

	public boolean contains(String val) {
		var t = csvTokens();
		if (t == null)
			return false;
		for (var v : t)
			if (eq(v, val))
				return true;
		return false;
	}

	public boolean containsIgnoreCase(String val) {
		var t = csvTokens();
		if (t == null)
			return false;
		for (var v : t)
			if (eqic(v, val))
				return true;
		return false;
	}

	@Override
	public String getValue() {
		var t = csvTokens();
		return t == null ? null : join(t, ", ");
	}

	public String[] orElse(String[] other) {
		var x = csvTokens();
		return nn(x) ? x : other;
	}

	public String[] toArray() {
		return copyOf(csvTokens());
	}

	public List<String> toList() {
		return u(l(csvTokens()));
	}

	private String[] csvTokens() {
		if (lazyMode == LAZY_TOKENS)
			return ((Supplier<String[]>) lazySupplier).get();
		if (eagerTokens != null)
			return eagerTokens;
		return splita(super.getValue());
	}
}
