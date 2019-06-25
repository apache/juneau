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
package org.apache.juneau.rest;

import static org.apache.juneau.rest.util.RestUtils.*;

import javax.servlet.http.*;

/**
 * A wrapper class that allows you to override basic fields.
 */
class OverrideableHttpServletRequest extends HttpServletRequestWrapper {

	private String pathInfo, servletPath;

	/**
	 * Constructor.
	 *
	 * @param request The wrapped servlet request.
	 */
	public OverrideableHttpServletRequest(HttpServletRequest request) {
		super(request);
	}

	public OverrideableHttpServletRequest pathInfo(String value) {
		validatePathInfo(value);
		if (value == null)
			value = "\u0000";
		this.pathInfo = value;
		return this;
	}

	public OverrideableHttpServletRequest servletPath(String value) {
		validateServletPath(value);
		this.servletPath = value;
		return this;
	}

	@Override /* HttpServletRequest */
	public String getPathInfo() {
		// Note that pathInfo can never be empty.
		return pathInfo == null ? super.getPathInfo() : pathInfo.charAt(0) == (char)0 ? null : pathInfo;
	}

	@Override /* HttpServletRequest */
	public String getServletPath() {
		return servletPath == null ? super.getServletPath() : servletPath;
	}
}
