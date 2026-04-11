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
package org.apache.juneau.ng.http;

import java.util.function.*;

import org.apache.juneau.ng.http.part.*;

/**
 * Static factory methods for creating HTTP parts (query parameters, form fields, path variables).
 *
 * <p>
 * Import statically for clean DSL-style usage:
 * <p class='bjava'>
 * 	import static org.apache.juneau.ng.http.HttpParts.*;
 *
 * 	PartList <jv>form</jv> = PartList.<jsm>of</jsm>(
 * 		part(<js>"username"</js>, <js>"alice"</js>),
 * 		part(<js>"password"</js>, <jv>passwordSupplier</jv>)
 * 	);
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
 * @since 9.2.1
 */
public final class HttpParts {

	private HttpParts() {}

	/**
	 * Creates an {@link HttpPart} with an eager string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The part value. May be <jk>null</jk>.
	 * @return A new part. Never <jk>null</jk>.
	 */
	public static HttpPart part(String name, String value) {
		return HttpPartBean.of(name, value);
	}

	/**
	 * Creates an {@link HttpPart} with a lazy value supplier.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param valueSupplier Supplier for the value. Must not be <jk>null</jk>.
	 * @return A new part. Never <jk>null</jk>.
	 */
	public static HttpPart part(String name, Supplier<String> valueSupplier) {
		return HttpPartBean.of(name, valueSupplier);
	}

	/**
	 * Creates a {@link PartList} from the given parts.
	 *
	 * @param parts The parts. Must not be <jk>null</jk>.
	 * @return A new list. Never <jk>null</jk>.
	 */
	public static PartList partList(HttpPart... parts) {
		return PartList.of(parts);
	}

	/**
	 * Creates a {@link PartList} from alternating name/value string pairs.
	 *
	 * @param pairs Alternating name/value strings. Must not be <jk>null</jk>. Length must be even.
	 * @return A new list. Never <jk>null</jk>.
	 */
	public static PartList partListOfPairs(String... pairs) {
		return PartList.ofPairs(pairs);
	}
}
