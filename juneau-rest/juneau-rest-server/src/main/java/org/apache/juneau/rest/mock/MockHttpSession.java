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
package org.apache.juneau.rest.mock;

import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * An implementation of {@link HttpSession} for mocking purposes.
 *
 * <div class='warn'>
 * 	<b>Deprecated</b> - Use <c>org.apache.juneau.restmock2</c>
 * </div>
 */
@Deprecated
public class MockHttpSession implements HttpSession {

	/**
	 * Creates a new HTTP session.
	 *
	 * @return A new HTTP session.
	 */
	public static MockHttpSession create() {
		return new MockHttpSession();
	}

	@Override /* HttpSession */
	public long getCreationTime() {
		return 0;
	}

	@Override /* HttpSession */
	public String getId() {
		return null;
	}

	@Override /* HttpSession */
	public long getLastAccessedTime() {
		return 0;
	}

	@Override /* HttpSession */
	public ServletContext getServletContext() {
		return null;
	}

	@Override /* HttpSession */
	public void setMaxInactiveInterval(int interval) {
	}

	@Override /* HttpSession */
	public int getMaxInactiveInterval() {
		return 0;
	}

	@Override /* HttpSession */
	public HttpSessionContext getSessionContext() {
		return null;
	}

	@Override /* HttpSession */
	public Object getAttribute(String name) {
		return null;
	}

	@Override /* HttpSession */
	public Object getValue(String name) {
		return null;
	}

	@Override /* HttpSession */
	public Enumeration<String> getAttributeNames() {
		return null;
	}

	@Override /* HttpSession */
	public String[] getValueNames() {
		return null;
	}

	@Override /* HttpSession */
	public void setAttribute(String name, Object value) {
	}

	@Override /* HttpSession */
	public void putValue(String name, Object value) {
	}

	@Override /* HttpSession */
	public void removeAttribute(String name) {
	}

	@Override /* HttpSession */
	public void removeValue(String name) {
	}

	@Override /* HttpSession */
	public void invalidate() {
	}

	@Override /* HttpSession */
	public boolean isNew() {
		return false;
	}
}
