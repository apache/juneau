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
package org.apache.juneau.rest.exception;

import static org.apache.juneau.rest.exception.Unauthorized.*;

import java.text.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Exception representing an HTTP 401 (Unauthorized).
 * 
 * <p>
 * Similar to <code>403 Forbidden</code>, but specifically for use when authentication is required and has failed or has not yet been provided. 
 * <br>The response must include a WWW-Authenticate header field containing a challenge applicable to the requested resource. 
 * <br>401 semantically means "unauthenticated",i.e. the user does not have the necessary credentials.
 * <br>Note: Some sites issue HTTP 401 when an IP address is banned from the website (usually the website domain) and that specific address is refused permission to access a website.
 */
@ResponseInfo(
	code=CODE,
	description=MESSAGE
)
public class Unauthorized extends RestException {
	private static final long serialVersionUID = 1L;
	
	/** Default message */
	public static final String MESSAGE = "Unauthorized";
	
	/** HTTP status code */
	public static final int CODE = 401;

	/**
	 * Constructor.
	 * 
	 * @param cause The cause.  Can be <jk>null</jk>. 
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public Unauthorized(Throwable cause, String msg, Object...args) {
		super(cause, CODE, getMessage(cause, msg, MESSAGE), args);
	}
	
	/**
	 * Constructor.
	 */
	public Unauthorized() {
		this((Throwable)null, MESSAGE);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public Unauthorized(String msg, Object...args) {
		this(null, msg, args);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param cause The cause.  Can be <jk>null</jk>. 
	 */
	public Unauthorized(Throwable cause) {
		this(cause, null);
	}
}