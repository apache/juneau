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
package org.apache.juneau.rest.ops;

import static org.apache.juneau.http.HttpMethod.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Mixin providing the residual {@code [* /error]} endpoint.
 *
 * <p>
 * Single-responsibility op-mixin carved out of the former {@code BasicRestOperations} interface. Compose
 * into a host via {@link Rest#mixins() @Rest(mixins=ErrorMixin.class)}; the {@link BasicRestServlet} family
 * pulls it in by default.
 *
 * <p>
 * Servlet chains often redirect to {@code /error} when an error condition occurs (such as failed
 * authentication) and set appropriate response parameters (such as a {@code WWW-Authenticate} header).
 *
 * @since 9.5.0
 */
@Rest
public class ErrorMixin extends RestMixin {

	/**
	 * [* /error] - Error occurred.
	 */
	@RestOp(
		method=ANY,
		path="/error",
		summary="Error occurred",
		description={
			"An error occurred during handling of the request.  ",
			"Servlet chains will often automatically redirect to '/error' when any sort of error condition occurs ",
			"(such as failed authentication) and will set appropriate response parameters ",
			"(such as an WWW-Authenticate response header)."
		}
	)
	public void error() { /* intentionally empty — triggers the exception handler for all error conditions */ }
}
