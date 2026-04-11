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
package org.apache.juneau.ng.http.part;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.ng.http.*;

/**
 * An ordered, immutable list of {@link HttpPart} instances that also implements {@link HttpBody} for
 * URL-encoded form submission.
 *
 * <p>
 * Use this class to send a {@code application/x-www-form-urlencoded} request body, or to represent
 * a collection of query parameters, form fields, or path variables.
 *
 * <p>
 * Create instances via the static factory methods:
 * <p class='bjava'>
 * 	<jc>// From parts</jc>
 * 	PartList <jv>parts</jv> = PartList.<jsm>of</jsm>(
 * 		HttpPartBean.<jsm>of</jsm>(<js>"username"</js>, <js>"alice"</js>),
 * 		HttpPartBean.<jsm>of</jsm>(<js>"password"</js>, <js>"secret"</js>)
 * 	);
 *
 * 	<jc>// From alternating name/value strings</jc>
 * 	PartList <jv>parts2</jv> = PartList.<jsm>ofPairs</jsm>(<js>"username"</js>, <js>"alice"</js>, <js>"password"</js>, <js>"secret"</js>);
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
public final class PartList implements HttpBody, Iterable<HttpPart> {

	private static final Charset UTF_8 = StandardCharsets.UTF_8;

	private final List<HttpPart> parts;

	private PartList(List<HttpPart> parts) {
		this.parts = List.copyOf(parts);
	}

	/**
	 * Creates an empty {@link PartList}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static PartList empty() {
		return new PartList(List.of());
	}

	/**
	 * Creates a {@link PartList} from the given parts.
	 *
	 * @param parts The parts. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static PartList of(HttpPart... parts) {
		assertArgNotNull("parts", parts);
		return new PartList(Arrays.asList(parts));
	}

	/**
	 * Creates a {@link PartList} from the given list of parts.
	 *
	 * @param parts The parts. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static PartList of(List<HttpPart> parts) {
		assertArgNotNull("parts", parts);
		return new PartList(parts);
	}

	/**
	 * Creates a {@link PartList} from alternating name/value string pairs.
	 *
	 * <p>
	 * The number of arguments must be even.
	 *
	 * @param pairs Alternating name/value strings. Must not be <jk>null</jk>. Length must be even.
	 * @return A new instance. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the number of arguments is odd.
	 */
	public static PartList ofPairs(String... pairs) {
		assertArgNotNull("pairs", pairs);
		if (pairs.length % 2 != 0)
			throw new IllegalArgumentException("pairs length must be even, got: " + pairs.length);
		var list = new ArrayList<HttpPart>(pairs.length / 2);
		for (int i = 0; i < pairs.length; i += 2)
			list.add(HttpPartBean.of(pairs[i], pairs[i + 1]));
		return new PartList(list);
	}

	/**
	 * Returns the parts in this list.
	 *
	 * @return An unmodifiable list of parts. Never <jk>null</jk>.
	 */
	public List<HttpPart> getParts() {
		return parts;
	}

	/**
	 * Returns the first part with the given name (case-sensitive), or <jk>null</jk> if absent.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @return The first matching part, or <jk>null</jk>.
	 */
	public HttpPart getFirst(String name) {
		assertArgNotNull("name", name);
		return parts.stream().filter(p -> name.equals(p.getName())).findFirst().orElse(null);
	}

	/**
	 * Returns the number of parts in this list.
	 *
	 * @return The number of parts.
	 */
	public int size() {
		return parts.size();
	}

	/**
	 * Returns <jk>true</jk> if this list is empty.
	 *
	 * @return <jk>true</jk> if empty.
	 */
	public boolean isEmpty() {
		return parts.isEmpty();
	}

	@Override /* Iterable */
	public Iterator<HttpPart> iterator() {
		return parts.iterator();
	}

	// ------------------------------------------------------------------------------------------------------------------
	// HttpBody implementation — for application/x-www-form-urlencoded submission
	// ------------------------------------------------------------------------------------------------------------------

	@Override /* HttpBody */
	public String getContentType() {
		return "application/x-www-form-urlencoded";
	}

	@Override /* HttpBody */
	public long getContentLength() {
		return -1;
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* HttpBody */
	@SuppressWarnings({
		"resource" // OutputStream is owned by caller per HttpBody.writeTo contract; must not be closed here
	})
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull("out", out);
		var sb = new StringBuilder();
		var first = true;
		for (var part : parts) {
			var value = part.getValue();
			if (value == null)
				continue;
			if (!first)
				sb.append('&');
			sb.append(URLEncoder.encode(part.getName(), UTF_8))
				.append('=')
				.append(URLEncoder.encode(value, UTF_8));
			first = false;
		}
		out.write(sb.toString().getBytes(UTF_8));
	}

	@Override /* Object */
	public String toString() {
		var sb = new StringBuilder();
		var first = true;
		for (var part : parts) {
			var value = part.getValue();
			if (value == null)
				continue;
			if (!first)
				sb.append('&');
			sb.append(URLEncoder.encode(part.getName(), UTF_8))
				.append('=')
				.append(URLEncoder.encode(value, UTF_8));
			first = false;
		}
		return sb.toString();
	}
}
