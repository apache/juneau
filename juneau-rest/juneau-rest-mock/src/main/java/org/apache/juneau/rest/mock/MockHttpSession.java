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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * An implementation of {@link HttpSession} for mocking purposes.
 *
 * <p>
 * Session-based tests can use this API to create customized instances of {@link HttpSession} objects
 * that can be passed to the {@link MockRestRequest#httpSession(HttpSession)} method.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-mock">juneau-rest-mock</a>
 * </ul>
 */
public class MockHttpSession implements HttpSession {

	private Map<String,Object> attributes = map(), values = map();

	private long creationTime, lastAccessedTime;
	private int maxInactiveInterval;
	private String id;
	private ServletContext servletContext;
	private boolean isNew = false;

	/**
	 * Creates a new HTTP session.
	 *
	 * @return A new HTTP session.
	 */
	public static MockHttpSession create() {
		return new MockHttpSession();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Setter methods
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the creation time on this session.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpSession#getCreationTime()}.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public MockHttpSession creationTime(long value) {
		this.creationTime = value;
		return this;
	}

	/**
	 * Sets the last-accessed time on this session.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpSession#getLastAccessedTime()}.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public MockHttpSession lastAccessedTime(long value) {
		this.lastAccessedTime = value;
		return this;
	}

	/**
	 * Sets the max-inactive interval time on this session.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpSession#getMaxInactiveInterval()}.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public MockHttpSession maxInactiveInterval(int value) {
		this.maxInactiveInterval = value;
		return this;
	}

	/**
	 * Sets the id on this session.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpSession#getId()}.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public MockHttpSession id(String value) {
		this.id = value;
		return this;
	}

	/**
	 * Sets the servlet context on this session.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpSession#getServletContext()}.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public MockHttpSession servletContext(ServletContext value) {
		this.servletContext = value;
		return this;
	}

	/**
	 * Sets the is-new value on this session.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpSession#isNew()}.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public MockHttpSession isNew(boolean value) {
		this.isNew = value;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpSession methods
	//------------------------------------------------------------------------------------------------------------------

	@Override /* HttpSession */
	public long getCreationTime() {
		return creationTime;
	}

	@Override /* HttpSession */
	public String getId() {
		return id;
	}

	@Override /* HttpSession */
	public long getLastAccessedTime() {
		return lastAccessedTime;
	}

	@Override /* HttpSession */
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override /* HttpSession */
	public void setMaxInactiveInterval(int value) {
		this.maxInactiveInterval = value;
	}

	@Override /* HttpSession */
	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	@SuppressWarnings("deprecation")
	@Override /* HttpSession */
	public HttpSessionContext getSessionContext() {
		return null;
	}

	@Override /* HttpSession */
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override /* HttpSession */
	public Object getValue(String name) {
		return values.get(name);
	}

	@Override /* HttpSession */
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(attributes.keySet());
	}

	@Override /* HttpSession */
	public String[] getValueNames() {
		return values.keySet().toArray(new String[0]);
	}

	@Override /* HttpSession */
	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	@Override /* HttpSession */
	public void putValue(String name, Object value) {
		values.put(name, value);
	}

	@Override /* HttpSession */
	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	@Override /* HttpSession */
	public void removeValue(String name) {
		values.remove(name);
	}

	@Override /* HttpSession */
	public void invalidate() {
	}

	@Override /* HttpSession */
	public boolean isNew() {
		return isNew;
	}
}
