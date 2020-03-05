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

import org.apache.juneau.internal.*;

/**
 * Category of headers that consist of a comma-delimited list of entity validator values.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	If-Match: "xyzzy"
 * 	If-Match: "xyzzy", "r2d2xxxx", "c3piozzzz"
 * 	If-Match: *
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
public class ComplexEntityValidatorArrayHeader extends ComplexHeader {

	private final EntityValidator[] value;

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The raw header value.
	 */
	protected ComplexEntityValidatorArrayHeader(String name, String value) {
		super(name);
		String[] s = StringUtils.split(value);
		this.value = new EntityValidator[s.length];
		for (int i = 0; i < s.length; i++) {
			this.value[i] = new EntityValidator(s[i]);
		}
	}

	/**
	 * Returns this header value as an array of {@link EntityValidator} objects.
	 *
	 * @return this header value as an array of {@link EntityValidator} objects.
	 */
	public EntityValidator[] asValidators() {
		return value;
	}

	@Override /* HttpHeader */
	public Object getValue() {
		return StringUtils.join(value, ", ");
	}

	@Override /* Object */
	public String toString() {
		return StringUtils.join(value, ", ");
	}
}
