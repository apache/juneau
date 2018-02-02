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
 * Category of headers that consist of a single entity validator value.
 * 
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	ETag: "xyzzy"
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'>
 * 		<a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public class HeaderEntityValidator {

	private final EntityValidator value;

	/**
	 * Constructor.
	 * 
	 * @param value The raw header value.
	 */
	protected HeaderEntityValidator(String value) {
		this.value = new EntityValidator(value);
	}

	/**
	 * Returns this header value as a {@link EntityValidator} object.
	 * 
	 * @return this header value as a {@link EntityValidator} object.
	 */
	public EntityValidator asValidator() {
		return value;
	}

	@Override /* Object */
	public String toString() {
		return value.toString();
	}
}
