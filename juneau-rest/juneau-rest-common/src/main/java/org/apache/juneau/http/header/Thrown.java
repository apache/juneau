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
package org.apache.juneau.http.header;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.function.*;

/**
 * Represents an HTTP <c>Thrown</c> header.
 *
 * <p>
 * Serialized exception information.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 *
 * @since 9.2.1
 */
public class Thrown extends HttpCsvHeader {

	public static final String NAME = "Thrown";

	public Thrown(String value) {
		super(NAME, value);
	}

	public Thrown(String... values) {
		super(NAME, values);
	}

	private Thrown(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static Thrown of(String value) {
		return new Thrown(value);
	}

	public static Thrown of(String... values) {
		return new Thrown(values);
	}

	/**
	 * Creates a {@code Thrown} header value from one or more {@link Throwable} instances.
	 *
	 * <p>
	 * Each throwable is encoded as {@code <urlEncode(className)>;<urlEncode(message)>} and the entries
	 * are joined with {@code ", "}.
	 *
	 * @param values The throwables to encode. {@code null} entries are ignored.
	 * @return A new header. Never {@code null}.
	 */
	public static Thrown of(Throwable... values) {
		var sb = new StringBuilder();
		if (values != null) {
			var first = true;
			for (var v : values) {
				if (v == null)
					continue;
				if (!first)
					sb.append(", ");
				first = false;
				sb.append(urlEncode(cn(v))).append(';');
				if (v.getMessage() != null)
					sb.append(urlEncode(v.getMessage()));
			}
		}
		return new Thrown(sb.toString());
	}

	public static Thrown ofLazyWire(Supplier<String> supplier) {
		return new Thrown(supplier, LAZY_WIRE_STRING);
	}

	public static Thrown ofLazyTokens(Supplier<String[]> supplier) {
		return new Thrown(supplier, LAZY_TOKENS);
	}
}
