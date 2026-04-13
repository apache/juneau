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

import org.apache.juneau.commons.http.StringRanges;
import java.util.function.*;


/**
 * Represents an HTTP <c>Accept-Charset</c> header.
 *
 * <p>
 * Character sets that are acceptable.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
public class AcceptCharset extends HttpStringRangesHeader {

	public static final String NAME = "Accept-Charset";

	public AcceptCharset(String value) {
		super(NAME, value);
	}

	public AcceptCharset(StringRanges value) {
		super(NAME, value);
	}

	private AcceptCharset(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static AcceptCharset of(String value) {
		return new AcceptCharset(value);
	}

	public static AcceptCharset of(StringRanges value) {
		return new AcceptCharset(value);
	}

	public static AcceptCharset ofLazyWire(Supplier<String> supplier) {
		return new AcceptCharset(supplier, LAZY_WIRE_STRING);
	}

	public static AcceptCharset ofLazyParsed(Supplier<StringRanges> supplier) {
		return new AcceptCharset(supplier, LAZY_STRING_RANGES);
	}
}
