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

/**
 * Represents a validator value.
 * <p>
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	ETag: "123456789"    – A strong ETag validator
 * 	ETag: W/"123456789"  – A weak ETag validator
 * </p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'>
 * 		<a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public class EntityValidator {

	private final String value;
	private final boolean isWeak;

	/**
	 * Constructor.
	 *
	 * @param value The validator string value.
	 */
	protected EntityValidator(String value) {
		value = value.trim();
		isWeak = value.startsWith("W/");
		if (isWeak)
			value = value.substring(2);
		if (value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length()-1) == '"')
			value = value.substring(1, value.length()-1);
		this.value = value;
	}

	/**
	 * Returns the validator value stripped of quotes and weak tag.
	 *
	 * @return The validator value.
	 */
	public String asString() {
		return value;
	}

	/**
	 * Returns <jk>true</jk> if the weak flag is present in the value.
	 *
	 * @return <jk>true</jk> if the weak flag is present in the value.
	 */
	public boolean isWeak() {
		return isWeak;
	}

	/**
	 * Returns <jk>true</jk> if the validator string value is <code>*</code>.
	 *
	 * @return <jk>true</jk> if the validator string value is <code>*</code>.
	 */
	public boolean isAny() {
		return "*".equals(value);
	}

	@Override
	public String toString() {
		return (isWeak ? "W/" : "") + (isAny() ? "*" : ('"' + value + '"'));
	}
}
