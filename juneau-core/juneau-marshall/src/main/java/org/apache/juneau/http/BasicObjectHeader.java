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

import java.util.function.*;

import org.apache.http.message.BasicHeader;
import org.apache.juneau.internal.*;

/**
 * An extension of a {@link BasicHeader} with an Object value that can be used to return different future values.
 */
public class BasicObjectHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	private final Object value;

	/**
	 * Convenience creator.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return A new {@link BasicObjectHeader} object.
	 */
	public static BasicObjectHeader of(String name, Object value) {
		return new BasicObjectHeader(name, value);
	}

	/**
	 * Convenience creator.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return A new {@link BasicObjectHeader} object.
	 */
	public static BasicObjectHeader of(String name, Supplier<?> value) {
		return new BasicObjectHeader(name, value);
	}

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value.
	 */
	public BasicObjectHeader(String name, Object value) {
		super(name, null);
		this.value = value;
	}

	@Override /* Header */
	public String getValue() {
		return StringUtils.asString(value);
	}
}


