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
package org.apache.juneau.rest.client2.ext;

import org.apache.http.message.*;

/**
 * An extension of a {@link BasicHeader} with an Object value that can be used to return different future values.
 */
public class BasicObjectHeader extends BasicHeader {
	private static final long serialVersionUID = 1L;

	private final Object value;

	/**
	 * Constructor.
	 *
	 * @param name Header name.  Must not be <jk>null</jk>.
	 * @param value Header value.  Can be <jk>null</jk>.  Value returned by {@link Object#toString()} will be used
	 *  as the header value.
	 */
	public BasicObjectHeader(String name, Object value) {
		super(name, null);
		this.value = value;
	}

	@Override /* Header */
	public String getValue() {
		return value == null ? null : value.toString();
	}
}
