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

import static org.apache.juneau.http.exception.LengthRequired.*;

import java.text.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Exception representing an HTTP 411 ().
 *
 * <p>
 * The request did not specify the length of its content, which is required by the requested resource.
 */
@Response(code=STATUS_CODE, description=REASON_PHRASE)
@FluentSetters
public class LengthRequired extends HttpException {
	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int STATUS_CODE = 411;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Length Required";

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public LengthRequired(Throwable cause, String msg, Object...args) {
		super(cause, STATUS_CODE, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 */
	public LengthRequired(String msg) {
		this((Throwable)null, msg);
	}

	/**
	 * Constructor.
	 */
	public LengthRequired() {
		this((Throwable)null, REASON_PHRASE);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public LengthRequired(String msg, Object...args) {
		this(null, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 */
	public LengthRequired(Throwable cause) {
		this(cause, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - HttpException */
	public LengthRequired header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	@Override /* GENERATED - HttpException */
	public LengthRequired status(int value) {
		super.status(value);
		return this;
	}

	// </FluentSetters>
}