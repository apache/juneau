// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.http;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.internal.*;

/**
 * Represents a parsed <code>Accept-Encoding:</code> HTTP header.
 * <p>
 * The formal RFC2616 header field definition is as follows:
 * <p class='bcode'>
 * 	The Accept-Encoding request-header field is similar to Accept, but restricts
 * 	the content-codings (section 3.5) that are acceptable in the response.
 *
 * 		Accept-Encoding  = "Accept-Encoding" ":"
 * 		                   1#( codings [ ";" "q" "=" qvalue ] )
 * 		codings          = ( content-coding | "*" )
 *
 * 	Examples of its use are:
 *
 * 		Accept-Encoding: compress, gzip
 * 		Accept-Encoding:
 * 		Accept-Encoding: *
 * 		Accept-Encoding: compress;q=0.5, gzip;q=1.0
 * 		Accept-Encoding: gzip;q=1.0, identity; q=0.5, *;q=0
 *
 * 	A server tests whether a content-coding is acceptable, according to an
 * 	Accept-Encoding field, using these rules:
 *
 * 	   1. If the content-coding is one of the content-codings listed in
 * 	      the Accept-Encoding field, then it is acceptable, unless it is
 * 	      accompanied by a qvalue of 0. (As defined in section 3.9, a
 * 	      qvalue of 0 means "not acceptable.")
 * 	   2. The special "*" symbol in an Accept-Encoding field matches any
 * 	      available content-coding not explicitly listed in the header
 * 	      field.
 * 	   3. If multiple content-codings are acceptable, then the acceptable
 * 	      content-coding with the highest non-zero qvalue is preferred.
 * 	   4. The "identity" content-coding is always acceptable, unless
 * 	      specifically refused because the Accept-Encoding field includes
 * 	      "identity;q=0", or because the field includes "*;q=0" and does
 * 	      not explicitly include the "identity" content-coding. If the
 * 	      Accept-Encoding field-value is empty, then only the "identity"
 * 	      encoding is acceptable.
 *
 * 	If an Accept-Encoding field is present in a request, and if the server cannot
 * 	send a response which is acceptable according to the Accept-Encoding header,
 * 	then the server SHOULD send an error response with the 406 (Not Acceptable) status code.
 *
 * 	If no Accept-Encoding field is present in a request, the server MAY assume
 * 	that the client will accept any content coding. In this case, if "identity"
 * 	is one of the available content-codings, then the server SHOULD use the "identity"
 * 	content-coding, unless it has additional information that a different content-coding
 * 	is meaningful to the client.
 *
 * 	      Note: If the request does not include an Accept-Encoding field,
 * 	      and if the "identity" content-coding is unavailable, then
 * 	      content-codings commonly understood by HTTP/1.0 clients (i.e.,
 * 	      "gzip" and "compress") are preferred; some older clients
 * 	      improperly display messages sent with other content-codings.  The
 * 	      server might also make this decision based on information about
 * 	      the particular user-agent or client.
 * 	      Note: Most HTTP/1.0 applications do not recognize or obey qvalues
 * 	      associated with content-codings. This means that qvalues will not
 * 	      work and are not permitted with x-gzip or x-compress.
 * </p>
 */
public final class AcceptEncoding {

	private static final boolean nocache = Boolean.getBoolean("juneau.http.AcceptEncoding.nocache");
	private static final ConcurrentHashMap<String,AcceptEncoding> cache = new ConcurrentHashMap<String,AcceptEncoding>();

	private final TypeRange[] typeRanges;
	private final List<TypeRange> typeRangesList;

	/**
	 * Returns a parsed <code>Accept-Encoding</code> header.
	 *
	 * @param s The <code>Accept-Encoding</code> header string.
	 * @return The parsed <code>Accept-Encoding</code> header, or <jk>null</jk> if the string was null.
	 */
	public static AcceptEncoding forString(String s) {
		if (s == null)
			return null;

		// Prevent OOM in case of DDOS
		if (cache.size() > 1000)
			cache.clear();

		while (true) {
			AcceptEncoding a = cache.get(s);
			if (a != null)
				return a;
			a = new AcceptEncoding(s);
			if (nocache)
				return a;
			cache.putIfAbsent(s, a);
		}
	}

	private AcceptEncoding(String raw) {
		this.typeRanges = TypeRange.parse(raw);
		this.typeRangesList = Collections.unmodifiableList(Arrays.asList(typeRanges));
	}

	/**
	 * Returns the list of the types ranges that make up this header.
	 * <p>
	 * The types ranges in the list are sorted by their q-value in descending order.
	 *
	 * @return An unmodifiable list of type ranges.
	 */
	public List<TypeRange> getTypeRanges() {
		return typeRangesList;
	}

	/**
	 * Given a list of content codings, returns the best match for this <code>Accept-Encoding</code> header.
	 * <p>
	 *
	 * @param contentCodings The codings to match against.
	 * @return The index into the array of the best match, or <code>-1</code> if no suitable matches could be found.
	 */
	public int findMatch(String[] contentCodings) {

		// Type ranges are ordered by 'q'.
		// So we only need to search until we've found a match.
		for (TypeRange mr : typeRanges)
			for (int i = 0; i < contentCodings.length; i++)
				if (mr.matches(contentCodings[i]))
					return i;

		return -1;
	}

	@Override /* Object */
	public String toString() {
		return StringUtils.join(typeRanges, ',');
	}
}
