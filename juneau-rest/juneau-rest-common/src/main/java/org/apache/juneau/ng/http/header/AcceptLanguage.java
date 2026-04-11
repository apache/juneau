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
 * Represents an HTTP <c>Accept-Language</c> header.
 *
 * <p>
 * Acceptable languages for the response.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
public class AcceptLanguage extends HttpStringRangesHeader {

	public static final String NAME = "Accept-Language";

	public AcceptLanguage(String value) {
		super(NAME, value);
	}

	public AcceptLanguage(StringRanges value) {
		super(NAME, value);
	}

	private AcceptLanguage(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static AcceptLanguage of(String value) {
		return new AcceptLanguage(value);
	}

	public static AcceptLanguage of(StringRanges value) {
		return new AcceptLanguage(value);
	}

	public static AcceptLanguage ofLazyWire(Supplier<String> supplier) {
		return new AcceptLanguage(supplier, LAZY_WIRE_STRING);
	}

	public static AcceptLanguage ofLazyParsed(Supplier<StringRanges> supplier) {
		return new AcceptLanguage(supplier, LAZY_STRING_RANGES);
	}
}
