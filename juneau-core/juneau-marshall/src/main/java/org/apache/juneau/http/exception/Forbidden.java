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

import static org.apache.juneau.http.exception.Forbidden.*;

import java.text.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Exception representing an HTTP 403 (Forbidden).
 *
 * <p>
 * The request was valid, but the server is refusing action.
 * <br>The user might not have the necessary permissions for a resource, or may need an account of some sort.
 */
@Response(code=STATUS_CODE, description=REASON_PHRASE)
@FluentSetters
public class Forbidden extends HttpException {
	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int STATUS_CODE = 403;

	/** Reason phrase */
	public static final String REASON_PHRASE = "Forbidden";

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public Forbidden(Throwable cause, String msg, Object...args) {
		super(cause, STATUS_CODE, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 */
	public Forbidden(String msg) {
		this((Throwable)null, msg);
	}

	/**
	 * Constructor.
	 */
	public Forbidden() {
		this((Throwable)null, REASON_PHRASE);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public Forbidden(String msg, Object...args) {
		this(null, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 */
	public Forbidden(Throwable cause) {
		this(cause, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - HttpException */
	public Forbidden header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	@Override /* GENERATED - HttpException */
	public Forbidden status(int value) {
		super.status(value);
		return this;
	}

	// </FluentSetters>
}