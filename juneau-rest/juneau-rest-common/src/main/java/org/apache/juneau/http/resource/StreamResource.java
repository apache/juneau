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

import java.io.*;
import org.apache.juneau.http.entity.*;

/**
 * A streamed, non-repeatable resource that obtains its content from an {@link InputStream}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-common}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class StreamResource extends BasicResource {

	/**
	 * Creates a new {@link StreamResource} builder.
	 *
	 * @return A new {@link StreamResource} builder.
	 */
	public static HttpResourceBuilder<StreamResource> create() {
		return new HttpResourceBuilder<>(StreamResource.class, StreamEntity.class);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The resource builder.
	 */
	public StreamResource(HttpResourceBuilder<?> builder) {
		super(builder);
	}

	/**
	 * Creates a new {@link StreamResource} builder initialized with the contents of this entity.
	 *
	 * @return A new {@link StreamResource} builder initialized with the contents of this entity.
	 */
	@Override /* BasicResource */
	public HttpResourceBuilder<StreamResource> copy() {
		return new HttpResourceBuilder<>(this);
	}
}