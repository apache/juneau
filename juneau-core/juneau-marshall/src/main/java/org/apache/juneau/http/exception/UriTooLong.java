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
package org.apache.juneau.http.exception;

import static org.apache.juneau.http.exception.UriTooLong.*;

import java.text.*;

import org.apache.juneau.http.annotation.*;

/**
 * Exception representing an HTTP 414 (URI Too Long).
 *
 * <p>
 * The URI provided was too long for the server to process.
 * <br>Often the result of too much data being encoded as a query-string of a GET request, in which case it should be converted to a POST request.
 */
@Response(code=CODE, description=MESSAGE)
public class UriTooLong extends HttpException {
	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int CODE = 414;

	/** Default message */
	public static final String MESSAGE = "URI Too Long";

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public UriTooLong(Throwable cause, String msg, Object...args) {
		super(cause, CODE, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 */
	public UriTooLong(String msg) {
		super(msg);
		setStatus(CODE);
	}

	/**
	 * Constructor.
	 */
	public UriTooLong() {
		this((Throwable)null, MESSAGE);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public UriTooLong(String msg, Object...args) {
		this(null, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 */
	public UriTooLong(Throwable cause) {
		this(cause, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - HttpException */
	public UriTooLong header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	// </FluentSetters>
}