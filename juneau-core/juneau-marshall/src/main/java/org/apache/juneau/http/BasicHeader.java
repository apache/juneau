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

import org.apache.juneau.annotation.*;

/**
 * Superclass of all headers defined in this package.
 */
@BeanIgnore
public class BasicHeader extends org.apache.http.message.BasicHeader {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param name Header name.
	 * @param value Header value.
	 */
	public BasicHeader(String name, String value) {
		super(name, value);
	}

	/**
	 * Returns this header as a simple string value.
	 *
	 * <p>
	 * Functionally equivalent to calling {@link #toString()}.
	 *
	 * @return This header as a simple string.
	 */
	public String asString() {
		return getValue();
	}


	/**
	 * Returns <jk>true</jk> if the specified value is the same using {@link String#equalsIgnoreCase(String)}.
	 *
	 * @param compare The value to compare against.
	 * @return <jk>true</jk> if the specified value is the same.
	 */
	public boolean eqIC(String compare) {
		return asString().equalsIgnoreCase(compare);
	}

	/**
	 * Returns <jk>true</jk> if the specified value is the same using {@link String#equals(Object)}.
	 *
	 * @param compare The value to compare against.
	 * @return <jk>true</jk> if the specified value is the same.
	 */
	public boolean eq(String compare) {
		return asString().equals(compare);
	}

	@Override /* Object */
	public String toString() {
		return asString();
	}
}
