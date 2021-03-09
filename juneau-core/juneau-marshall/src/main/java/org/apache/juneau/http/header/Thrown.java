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
package org.apache.juneau.http.header;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Thrown</l> HTTP request header.
 *
 * <p>
 * Contains exception information including name and optionally a message.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Thrown: org.apache.juneau.http.response.NotFound, Resource was not found
 * </p>
 *
 * <p>
 * This header isn't part of the RFC2616 specification, but is provided to allow for Java exception information
 * to be delivered to remote REST Java interfaces.
 */
@Header("Thrown")
public class Thrown extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * An empty unmodifiable Thrown header.
	 */
	public static final Thrown EMPTY = new Thrown((String)null);

	private final String className, message;

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The header value.
	 * @return A new {@link Exception} object.
	 */
	public static Thrown of(Throwable value) {
		if (value == null)
			return null;
		return new Thrown(value);
	}

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The header value.
	 * @return A new {@link Exception} object.
	 */
	public static Thrown of(String value) {
		if (value == null)
			return null;
		return new Thrown(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 */
	public Thrown(Throwable value) {
		super("Thrown", value);
		className = stripInvalidHttpHeaderChars(value.getClass().getName());
		message = stripInvalidHttpHeaderChars(value.getMessage());
	}

	@Override /* Header */
	public String getValue() {
		if (message == null)
			return className;
		return className + ", " + message;
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 */
	public Thrown(String value) {
		super("Thrown", value);
		if (value != null) {
			int i = value.indexOf(',');
			if (i != -1) {
				className = value.substring(0, i).trim();
				message = value.substring(i+1).trim();
			} else {
				className = value;
				message = null;
			}
		} else {
			className = null;
			message = null;
		}
	}

	/**
	 * Returns the class name portion of the header.
	 *
	 * @return The class name portion of the header, or <jk>null</jk> if not there.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Returns the class name portion of the header.
	 *
	 * @return The class name portion of the header, never <jk>null</jk>.
	 */
	public Optional<String> className() {
		return Optional.ofNullable(className);
	}

	/**
	 * Returns the message portion of the header.
	 *
	 * @return The message portion of the header, or <jk>null</jk> if not there.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the message portion of the header.
	 *
	 * @return The message portion of the header, never <jk>null</jk>.
	 */
	public Optional<String> message() {
		return Optional.ofNullable(message);
	}
}
