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

import static org.apache.juneau.rest.exception.PreconditionRequired.*;

import java.text.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Exception representing an HTTP 428 (Precondition Required).
 * 
 * <p>
 * The origin server requires the request to be conditional. 
 * <br>Intended to prevent the 'lost update' problem, where a client GETs a resource's state, modifies it, and PUTs it back to the server, when meanwhile a third party has modified the state on the server, leading to a conflict.
 */
@ResponseInfo(
	code=CODE,
	description=MESSAGE
)
public class PreconditionRequired extends RestException {
	private static final long serialVersionUID = 1L;
	
	/** Default message */
	public static final String MESSAGE = "Precondition Required";
	
	/** HTTP status code */
	public static final int CODE = 428;

	/**
	 * Constructor.
	 * 
	 * @param cause The cause.  Can be <jk>null</jk>. 
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public PreconditionRequired(Throwable cause, String msg, Object...args) {
		super(cause, CODE, getMessage(cause, msg, MESSAGE), args);
	}
	
	/**
	 * Constructor.
	 */
	public PreconditionRequired() {
		this((Throwable)null, MESSAGE);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public PreconditionRequired(String msg, Object...args) {
		this(null, msg, args);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param cause The cause.  Can be <jk>null</jk>. 
	 */
	public PreconditionRequired(Throwable cause) {
		this(cause, null);
	}
}