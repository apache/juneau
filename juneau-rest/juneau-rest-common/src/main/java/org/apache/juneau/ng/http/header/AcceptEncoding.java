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

import java.util.function.*;

import org.apache.juneau.*;

/**
 * Represents an HTTP <c>Accept-Encoding</c> header.
 *
 * <p>
 * Acceptable encodings.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
public class AcceptEncoding extends HttpStringRangesHeader {

	public static final String NAME = "Accept-Encoding";

	public AcceptEncoding(String value) {
		super(NAME, value);
	}

	public AcceptEncoding(StringRanges value) {
		super(NAME, value);
	}

	private AcceptEncoding(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static AcceptEncoding of(String value) {
		return new AcceptEncoding(value);
	}

	public static AcceptEncoding of(StringRanges value) {
		return new AcceptEncoding(value);
	}

	public static AcceptEncoding ofLazyWire(Supplier<String> supplier) {
		return new AcceptEncoding(supplier, LAZY_WIRE_STRING);
	}

	public static AcceptEncoding ofLazyParsed(Supplier<StringRanges> supplier) {
		return new AcceptEncoding(supplier, LAZY_STRING_RANGES);
	}
}
