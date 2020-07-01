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

import org.apache.http.*;
import org.apache.juneau.internal.*;

/**
 * Specifies a dynamic supplier of {@link Header} objects.
 *
 * This class is thread safe.
 */
public class HeaderSupplier implements Iterable<Header> {

	/** Represents no header supplier */
	public final class Null extends HeaderSupplier {}

	/**
	 * Convenience creator.
	 *
	 * @return A new {@link HeaderSupplier} object.
	 */
	public static HeaderSupplier create() {
		return new HeaderSupplier();
	}

	private final List<Iterable<Header>> headers = new CopyOnWriteArrayList<>();

	/**
	 * Add a header to this supplier.
	 *
	 * @param h The header to add. <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HeaderSupplier add(Header h) {
		if (h != null)
			headers.add(Collections.singleton(h));
		return this;
	}

	/**
	 * Add a supplier to this supplier.
	 *
	 * @param h The supplier to add. <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HeaderSupplier add(HeaderSupplier h) {
		if (h != null)
			headers.add(h);
		return this;
	}

	@Override
	public Iterator<Header> iterator() {
		return CollectionUtils.iterator(headers);
	}
}
