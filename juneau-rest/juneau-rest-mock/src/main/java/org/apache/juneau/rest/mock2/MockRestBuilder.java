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
package org.apache.juneau.rest.mock2;

import static org.apache.juneau.rest.util.RestUtils.*;

import javax.servlet.http.*;

/**
 * Builder class for {@link MockRest} objects.
 */
public class MockRestBuilder {

	Object impl;
	String contextPath = "", servletPath = "";

	MockRestBuilder(Object impl) {
		this.impl = impl;
	}

	/**
	 * Identifies the context path for the REST resource.
	 *
	 * <p>
	 * 	This value is used to deconstruct the request URL and set the appropriate URL getters on the {@link HttpServletRequest}
	 * 	object correctly.
	 *
	 * <p>
	 * 	Should either be a value such as <js>"/foo"</js> or an empty string.
	 *
	 * <p>
	 * 	The following fixes are applied to non-conforming strings.
	 * <ul>
	 * 	<li><jk>nulls</jk> and <js>"/"</js> are converted to empty strings.
	 * 	<li>Trailing slashes are trimmed.
	 * 	<li>Leading slash is added if needed.
	 * </ul>
	 *
	 * @param value The context path.
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder contextPath(String value) {
		this.contextPath = toValidContextPath(value);
		return this;
	}

	/**
	 * Identifies the servlet path for the REST resource.
	 *
	 * <p>
	 * 	This value is used to deconstruct the request URL and set the appropriate URL getters on the {@link HttpServletRequest}
	 * 	object correctly.
	 *
	 * <p>
	 * 	Should either be a value such as <js>"/foo"</js> or an empty string.
	 *
	 * <p>
	 * 	The following fixes are applied to non-conforming strings.
	 * <ul>
	 * 	<li><jk>nulls</jk> and <js>"/"</js> are converted to empty strings.
	 * 	<li>Trailing slashes are trimmed.
	 * 	<li>Leading slash is added if needed.
	 * </ul>
	 *
	 * @param value The context path.
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder servletPath(String value) {
		this.servletPath = toValidContextPath(value);
		return this;
	}

	/**
	 * Create a new {@link MockRest} object based on the settings on this builder.
	 *
	 * @return A new {@link MockRest} object.
	 */
	public MockRest build() {
		return new MockRest(this);
	}
}