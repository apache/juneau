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
package org.apache.juneau.http.resource;

import org.apache.juneau.http.entity.*;

/**
 * A repeatable resource that obtains its content from a byte array.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.HttpParts}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class ByteArrayResource extends BasicResource {

	/**
	 * Creates a new {@link ByteArrayResource} builder.
	 *
	 * @return A new {@link ByteArrayResource} builder.
	 */
	public static HttpResourceBuilder<ByteArrayResource> create() {
		return new HttpResourceBuilder<>(ByteArrayResource.class, ByteArrayEntity.class);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The resource builder.
	 */
	public ByteArrayResource(HttpResourceBuilder<?> builder) {
		super(builder);
	}

	/**
	 * Creates a new {@link ByteArrayResource} builder initialized with the contents of this entity.
	 *
	 * @return A new {@link ByteArrayResource} builder initialized with the contents of this entity.
	 */
	@Override /* BasicResource */
	public HttpResourceBuilder<ByteArrayResource> copy() {
		return new HttpResourceBuilder<>(this);
	}
}