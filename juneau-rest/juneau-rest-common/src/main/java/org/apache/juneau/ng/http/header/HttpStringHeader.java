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

/**
 * Base type for HTTP headers whose value is an opaque string (or lazy {@link Supplier} of string).
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
 * @since 9.2.1
 */
public class HttpStringHeader extends HttpHeaderBean {

	/**
	 * @param name Header name. Must not be {@code null}.
	 * @param value Wire value. May be {@code null}.
	 */
	protected HttpStringHeader(String name, String value) {
		super(name, value);
	}

	/**
	 * @param name Header name. Must not be {@code null}.
	 * @param valueSupplier Lazy wire value supplier. Must not be {@code null}.
	 */
	protected HttpStringHeader(String name, Supplier<String> valueSupplier) {
		super(name, valueSupplier);
	}
}
