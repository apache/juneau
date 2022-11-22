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
package org.apache.juneau.http.header;

import static org.apache.juneau.common.internal.ArgUtils.*;

import java.util.NoSuchElementException;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.juneau.common.internal.*;

/**
 * Basic implementation of a {@link HeaderIterator}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public class BasicHeaderIterator implements HeaderIterator {

	private final Header[] entries;
	private final String name;
	private final boolean caseSensitive;

	private int currentIndex;

	/**
	 * Creates a new header iterator.
	 *
	 * @param headers An array of headers over which to iterate.
	 * @param name The name of the headers over which to iterate, or <jk>null</jk> for all.
	 * @param caseSensitive Use case-sensitive matching for part name.
	 */
	public BasicHeaderIterator(Header[] headers, String name, boolean caseSensitive) {
		this.entries = assertArgNotNull("headers", headers);
		this.name = name;
		this.caseSensitive = caseSensitive;
		this.currentIndex = findNext(-1);
	}

	private int findNext(int pos) {

		int from = pos;

		int to = entries.length - 1;
		boolean found = false;
		while (!found && (from < to)) {
			from++;
			found = filter(from);
		}

		return found ? from : -1;
	}

	private boolean filter(int index) {
		return (name == null) || eq(name, entries[index].getName());
	}

	@Override /* HeaderIterator */
	public boolean hasNext() {
		return (currentIndex >= 0);
	}

	@Override /* HeaderIterator */
	public Header nextHeader() throws NoSuchElementException {

		int current = currentIndex;

		if (current < 0)
			throw new NoSuchElementException("Iteration already finished.");

		currentIndex = findNext(current);

		return entries[current];
	}

	@Override /* HeaderIterator */
	public final Object next() throws NoSuchElementException {
		return nextHeader();
	}

	/**
	 * Not supported.
	 */
	@Override /* HeaderIterator */
	public void remove() {
		throw new UnsupportedOperationException("Not supported.");
	}

	private boolean eq(String s1, String s2) {
		return StringUtils.eq(!caseSensitive, s1, s2);
	}
}
