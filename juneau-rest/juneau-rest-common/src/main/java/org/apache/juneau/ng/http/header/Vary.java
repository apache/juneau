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

/**
 * Represents an HTTP <c>Vary</c> header.
 *
 * <p>
 * Response fields that determine cache matching.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
import java.util.function.*;

/**
 * @since 9.2.1
 */
public class Vary extends HttpCsvHeader {

	public static final String NAME = "Vary";

	public Vary(String value) {
		super(NAME, value);
	}

	public Vary(String... values) {
		super(NAME, values);
	}

	private Vary(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static Vary of(String value) {
		return new Vary(value);
	}

	public static Vary of(String... values) {
		return new Vary(values);
	}

	public static Vary ofLazyWire(Supplier<String> supplier) {
		return new Vary(supplier, LAZY_WIRE_STRING);
	}

	public static Vary ofLazyTokens(Supplier<String[]> supplier) {
		return new Vary(supplier, LAZY_TOKENS);
	}
}
