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

import org.apache.juneau.collections.*;

/**
 * Category of headers that consist of simple comma-delimited lists of strings with q-values.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Accept-Encoding: compress;q=0.5, gzip;q=1.0
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
public class BasicRangeArrayHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	final StringRange[] typeRanges;
	private final List<StringRange> typeRangesList;

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The raw header value.
	 */
	public BasicRangeArrayHeader(String name, String value) {
		super(name, value);
		this.typeRanges = StringRange.parse(value);
		this.typeRangesList = AList.unmodifiable(typeRanges);
	}

	/**
	 * Given a list of type values, returns the best match for this header.
	 *
	 * @param types The types to match against.
	 * @return The index into the array of the best match, or <c>-1</c> if no suitable matches could be found.
	 */
	public int findMatch(String[] types) {

		// Type ranges are ordered by 'q'.
		// So we only need to search until we've found a match.
		for (StringRange mr : typeRanges)
			for (int i = 0; i < types.length; i++)
				if (mr.matches(types[i]))
					return i;

		return -1;
	}

	/**
	 * Returns the list of the types ranges that make up this header.
	 *
	 * <p>
	 * The types ranges in the list are sorted by their q-value in descending order.
	 *
	 * @return An unmodifiable list of type ranges.
	 */
	public List<StringRange> asSimpleRanges() {
		return typeRangesList;
	}
}