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
 * Represents an HTTP <c>Age</c> header.
 *
 * <p>
 * The age the object has been in a proxy cache in seconds.
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
public class Age extends HttpIntegerHeader {

	public static final String NAME = "Age";

	public Age(String value) {
		super(NAME, value);
	}

	public Age(int value) {
		super(NAME, Integer.valueOf(value));
	}

	public Age(Integer value) {
		super(NAME, value);
	}

	private Age(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static Age of(String value) {
		return new Age(value);
	}

	public static Age of(int value) {
		return new Age(value);
	}

	public static Age of(Integer value) {
		return new Age(value);
	}

	public static Age ofLazyWire(Supplier<String> supplier) {
		return new Age(supplier, LAZY_WIRE_STRING);
	}

	public static Age ofLazyParsed(Supplier<Integer> supplier) {
		return new Age(supplier, LAZY_INTEGER);
	}
}
