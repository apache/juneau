/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.mock;

import java.util.*;

import org.apache.juneau.common.utils.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * An implementation of {@link HttpSession} for mocking purposes.
 *
 * <p>
 * Session-based tests can use this API to create customized instances of {@link HttpSession} objects
 * that can be passed to the {@link MockRestRequest#httpSession(HttpSession)} method.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestMockBasics">juneau-rest-mock Basics</a>
 * </ul>
 */
public class MockHttpSession implements HttpSession {

	/**
	 * Creates a new HTTP session.
	 *
	 * @return A new HTTP session.
	 */
	public static MockHttpSession create() {
		return new MockHttpSession();
	}

	private Map<String,Object> attributes = CollectionUtils.map();
	private long creationTime, lastAccessedTime;
	private int maxInactiveInterval;
	private String id;
	private ServletContext servletContext;

	private boolean isNew;

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

	@Override /* Overridden from HttpSession */
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override /* Overridden from HttpSession */
	public Enumeration<String> getAttributeNames() { return Collections.enumeration(attributes.keySet()); }

	@Override /* Overridden from HttpSession */
	public long getCreationTime() { return creationTime; }

	@Override /* Overridden from HttpSession */
	public String getId() { return id; }

	@Override /* Overridden from HttpSession */
	public long getLastAccessedTime() { return lastAccessedTime; }

	@Override /* Overridden from HttpSession */
	public int getMaxInactiveInterval() { return maxInactiveInterval; }

	@Override /* Overridden from HttpSession */
	public ServletContext getServletContext() { return servletContext; }

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

	@Override /* Overridden from HttpSession */
	public void invalidate() {}

	@Override /* Overridden from HttpSession */
	public boolean isNew() { return isNew; }

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

	@Override /* Overridden from HttpSession */
	public void removeAttribute(String name) {
		attributes.remove(name);
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

	@Override /* Overridden from HttpSession */
	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	@Override /* Overridden from HttpSession */
	public void setMaxInactiveInterval(int value) { this.maxInactiveInterval = value; }
}