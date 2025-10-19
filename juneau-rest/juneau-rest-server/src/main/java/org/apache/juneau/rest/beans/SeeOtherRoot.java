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
package org.apache.juneau.rest.beans;

import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;

/**
 * Convenience subclass of {@link SeeOther} for redirecting a response to the servlet root.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UtilityBeans">Utility Beans</a>
 * </ul>
 */
@Response
@Schema(description = "Redirect to servlet root")
public class SeeOtherRoot extends SeeOther {

	/**
	 * Reusable instance.
	 */
	public static final SeeOtherRoot INSTANCE = new SeeOtherRoot();

	/**
	 * Constructor.
	 */
	public SeeOtherRoot() {
		setLocation("servlet:/");
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public SeeOtherRoot(SeeOtherRoot copyFrom) {
		super(copyFrom);
	}

	/**
	 * Constructor with no redirect.
	 * <p>
	 * Used for end-to-end interfaces.
	 *
	 * @param content Message to send as the response.
	 */
	public SeeOtherRoot(String content) {
		setLocation("servlet:/");
		setContent(content);
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot copy() {
		return new SeeOtherRoot(this);
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setContent(HttpEntity value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setContent(String value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setHeader2(Header value) {
		super.setHeader2(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setHeader2(String name, String value) {
		super.setHeader2(name, value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setHeaders(HeaderList value) {
		super.setHeaders(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setHeaders(List<Header> values) {
		super.setHeaders(values);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setHeaders2(Header...values) {
		super.setHeaders2(values);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setLocale2(Locale value) {
		super.setLocale2(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setLocation(String value) {
		super.setLocation(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setLocation(URI value) {
		super.setLocation(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setProtocolVersion(ProtocolVersion value) {
		super.setProtocolVersion(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setReasonPhrase2(String value) {
		super.setReasonPhrase2(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		super.setReasonPhraseCatalog(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setStatusCode2(int value) {
		super.setStatusCode2(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setStatusLine(BasicStatusLine value) {
		super.setStatusLine(value);
		return this;
	}

	@Override /* Overridden from SeeOther */
	public SeeOtherRoot setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}
}