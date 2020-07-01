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
import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Specifies a dynamic supplier of {@link NameValuePair} objects.
 *
 * This class is thread safe.
 */
public class NameValuePairSupplier implements Iterable<NameValuePair> {

	/** Represents no header supplier */
	public final class Null extends NameValuePairSupplier {}

	/**
	 * Convenience creator.
	 *
	 * @return A new {@link NameValuePairSupplier} object.
	 */
	public static NameValuePairSupplier create() {
		return new NameValuePairSupplier();
	}

	/**
	 * Convenience creator.
	 *
	 * @param values
	 * 	The values to populate this supplier with.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}.
	 * 		<li>{@link NameValuePairSupplier}.
	 * 	</ul>
	 * @return A new {@link NameValuePairSupplier} object.
	 */
	public static NameValuePairSupplier of(Object...values) {
		NameValuePairSupplier s = NameValuePairSupplier.create();
		for (Object v : values) {
			if (v instanceof NameValuePair)
				s.add((NameValuePair)v);
			else if (v instanceof NameValuePairSupplier)
				s.add((NameValuePairSupplier)v);
			else if (v != null)
				throw new BasicRuntimeException("Invalid type passed to NameValuePairSupplier.of(): {0}", v.getClass().getName());
		}
		return s;
	}

	private final List<Iterable<NameValuePair>> pairs = new CopyOnWriteArrayList<>();

	/**
	 * Add a name-value pair to this supplier.
	 *
	 * @param h The name-value pair to add. <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public NameValuePairSupplier add(NameValuePair h) {
		if (h != null)
			pairs.add(Collections.singleton(h));
		return this;
	}

	/**
	 * Add a supplier to this supplier.
	 *
	 * @param h The supplier to add. <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public NameValuePairSupplier add(NameValuePairSupplier h) {
		if (h != null)
			pairs.add(h);
		return this;
	}

	@Override
	public Iterator<NameValuePair> iterator() {
		return CollectionUtils.iterator(pairs);
	}
}
