/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.ng.rest.client;

import java.io.*;

import org.apache.juneau.ng.http.*;

/**
 * Converts a Java object passed to {@link NgRestRequest#body(Object)} into a {@link TransportBody}.
 *
 * <p>
 * A list of converters is consulted in order; the first converter whose {@link #canConvert(Object)} returns
 * {@code true} wins. The default converter list (applied automatically by {@link NgRestClient}) handles
 * the common cases:
 * <ol>
 * 	<li>{@link HttpBody} — passed through directly (streaming preferred over buffering)
 * 	<li>{@link InputStream} — wrapped in a streaming body
 * 	<li>{@code byte[]} — wrapped in a byte-array body
 * 	<li>{@link java.io.File} — wrapped in a file body
 * </ol>
 *
 * <p>
 * Custom converters are registered on the builder and are prepended to the default list
 * (checked before the defaults). Use the {@link #of(Class, ThrowingConverter)} factory for concise lambdas:
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	NgRestClient <jv>client</jv> = NgRestClient.<jsm>builder</jsm>()
 * 		.bodyConverter(BodyConverter.<jsm>of</jsm>(MyBean.<jk>class</jk>,
 * 			<jv>bean</jv> -&gt; TransportBody.<jsm>of</jsm>(StringBody.<jsm>of</jsm>(<jv>mySerializer</jv>.serialize(<jv>bean</jv>)))))
 * 		.build();
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @param <T> The type this converter handles.
 * @since 9.2.1
 */
public interface BodyConverter<T> {

	/**
	 * Returns {@code true} if this converter can handle the given value.
	 *
	 * @param value The value passed to {@link NgRestRequest#body(Object)}. May be <jk>null</jk>.
	 * @return {@code true} if {@link #convert(Object)} should be called with this value.
	 */
	boolean canConvert(Object value);

	/**
	 * Converts the value to a {@link TransportBody}.
	 *
	 * @param value The value. Never <jk>null</jk> (converters are only called when {@link #canConvert} returns true).
	 * @return The transport body. Never <jk>null</jk>.
	 * @throws IOException If conversion fails.
	 */
	TransportBody convert(Object value) throws IOException;

	/**
	 * Creates a {@link BodyConverter} from a type predicate and a converting lambda.
	 *
	 * <p>
	 * The converter matches any value that is an instance of the given type.
	 *
	 * @param <T> The type to handle.
	 * @param type The type to match via {@code instanceof}. Must not be <jk>null</jk>.
	 * @param converter The conversion function. Must not be <jk>null</jk>.
	 * @return A new converter. Never <jk>null</jk>.
	 */
	static <T> BodyConverter<T> of(Class<T> type, ThrowingConverter<T> converter) {
		return new BodyConverter<>() {
			@Override public boolean canConvert(Object value) { return type.isInstance(value); }
			@Override public TransportBody convert(Object value) throws IOException { return converter.apply(type.cast(value)); }
		};
	}

	/**
	 * A checked function used by {@link BodyConverter#of(Class, ThrowingConverter)}.
	 *
	 * @param <T> The input type.
	 * @since 9.2.1
	 */
	@FunctionalInterface
	interface ThrowingConverter<T> {

		/**
		 * Converts the given value to a {@link TransportBody}.
		 *
		 * @param value The value. Never <jk>null</jk>.
		 * @return The transport body. Never <jk>null</jk>.
		 * @throws IOException If conversion fails.
		 */
		TransportBody apply(T value) throws IOException;
	}
}
