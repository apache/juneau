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
package org.apache.juneau.ng.http;

import org.apache.juneau.ng.http.response.*;

/**
 * Static factory methods for creating pre-defined HTTP response and exception objects.
 *
 * <p>
 * Import statically for clean DSL-style usage in REST server code:
 * <p class='bjava'>
 * 	import static org.apache.juneau.ng.http.HttpResponses.*;
 *
 * 	<ja>@RestGet</ja>(<js>"/users/{id}"</js>)
 * 	<jk>public</jk> User getUser(<ja>@Path</ja> String id) {
 * 		User <jv>user</jv> = userService.find(id);
 * 		<jk>if</jk> (<jv>user</jv> == <jk>null</jk>) <jk>throw</jk> notFound(<js>"User not found: "</js> + id);
 * 		<jk>return</jk> <jv>user</jv>;
 * 	}
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class HttpResponses {

	private HttpResponses() {}

	// ------------------------------------------------------------------------------------------------------------------
	// 1xx Informational
	// ------------------------------------------------------------------------------------------------------------------

	/** @return A new {@code 100 Continue} response. */
	public static Continue continueResponse() { return new Continue(); }

	/** @return A new {@code 101 Switching Protocols} response. */
	public static SwitchingProtocols switchingProtocols() { return new SwitchingProtocols(); }

	// ------------------------------------------------------------------------------------------------------------------
	// 2xx Success
	// ------------------------------------------------------------------------------------------------------------------

	/** @return A new {@code 200 OK} response. */
	public static Ok ok() { return new Ok(); }

	/** @param body The response body. @return A new {@code 200 OK} response. */
	public static Ok ok(String body) { return new Ok(body); }

	/** @return A new {@code 201 Created} response. */
	public static Created created() { return new Created(); }

	/** @param body The response body. @return A new {@code 201 Created} response. */
	public static Created created(String body) { return new Created(body); }

	/** @return A new {@code 202 Accepted} response. */
	public static Accepted accepted() { return new Accepted(); }

	/** @return A new {@code 204 No Content} response. */
	public static NoContent noContent() { return new NoContent(); }

	/** @return A new {@code 206 Partial Content} response. */
	public static PartialContent partialContent() { return new PartialContent(); }

	// ------------------------------------------------------------------------------------------------------------------
	// 3xx Redirection
	// ------------------------------------------------------------------------------------------------------------------

	/** @param location The redirect location. @return A new {@code 301 Moved Permanently} response. */
	public static BasicHttpResponse movedPermanently(String location) { return new MovedPermanently().withHeader("Location", location); }

	/** @param location The redirect location. @return A new {@code 302 Found} response. */
	public static BasicHttpResponse found(String location) { return new Found().withHeader("Location", location); }

	/** @param location The redirect location. @return A new {@code 303 See Other} response. */
	public static BasicHttpResponse seeOther(String location) { return new SeeOther().withHeader("Location", location); }

	/** @return A new {@code 304 Not Modified} response. */
	public static NotModified notModified() { return new NotModified(); }

	/** @param location The redirect location. @return A new {@code 307 Temporary Redirect} response. */
	public static BasicHttpResponse temporaryRedirect(String location) { return new TemporaryRedirect().withHeader("Location", location); }

	/** @param location The redirect location. @return A new {@code 308 Permanent Redirect} response. */
	public static BasicHttpResponse permanentRedirect(String location) { return new PermanentRedirect().withHeader("Location", location); }

	// ------------------------------------------------------------------------------------------------------------------
	// 4xx Client Errors
	// ------------------------------------------------------------------------------------------------------------------

	/** @return A new {@code 400 Bad Request} exception. */
	public static BadRequest badRequest() { return new BadRequest(); }

	/** @param message The error message. @return A new {@code 400 Bad Request} exception. */
	public static BadRequest badRequest(String message) { return new BadRequest(message); }

	/** @return A new {@code 401 Unauthorized} exception. */
	public static Unauthorized unauthorized() { return new Unauthorized(); }

	/** @param message The error message. @return A new {@code 401 Unauthorized} exception. */
	public static Unauthorized unauthorized(String message) { return new Unauthorized(message); }

	/** @return A new {@code 403 Forbidden} exception. */
	public static Forbidden forbidden() { return new Forbidden(); }

	/** @param message The error message. @return A new {@code 403 Forbidden} exception. */
	public static Forbidden forbidden(String message) { return new Forbidden(message); }

	/** @return A new {@code 404 Not Found} exception. */
	public static NotFound notFound() { return new NotFound(); }

	/** @param message The error message. @return A new {@code 404 Not Found} exception. */
	public static NotFound notFound(String message) { return new NotFound(message); }

	/** @return A new {@code 405 Method Not Allowed} exception. */
	public static MethodNotAllowed methodNotAllowed() { return new MethodNotAllowed(); }

	/** @return A new {@code 409 Conflict} exception. */
	public static Conflict conflict() { return new Conflict(); }

	/** @param message The error message. @return A new {@code 409 Conflict} exception. */
	public static Conflict conflict(String message) { return new Conflict(message); }

	/** @return A new {@code 410 Gone} exception. */
	public static Gone gone() { return new Gone(); }

	/** @return A new {@code 412 Precondition Failed} exception. */
	public static PreconditionFailed preconditionFailed() { return new PreconditionFailed(); }

	/** @return A new {@code 415 Unsupported Media Type} exception. */
	public static UnsupportedMediaType unsupportedMediaType() { return new UnsupportedMediaType(); }

	/** @param message The error message. @return A new {@code 415 Unsupported Media Type} exception. */
	public static UnsupportedMediaType unsupportedMediaType(String message) { return new UnsupportedMediaType(message); }

	/** @return A new {@code 422 Unprocessable Entity} exception. */
	public static UnprocessableEntity unprocessableEntity() { return new UnprocessableEntity(); }

	/** @param message The error message. @return A new {@code 422 Unprocessable Entity} exception. */
	public static UnprocessableEntity unprocessableEntity(String message) { return new UnprocessableEntity(message); }

	/** @return A new {@code 429 Too Many Requests} exception. */
	public static TooManyRequests tooManyRequests() { return new TooManyRequests(); }

	// ------------------------------------------------------------------------------------------------------------------
	// 5xx Server Errors
	// ------------------------------------------------------------------------------------------------------------------

	/** @return A new {@code 500 Internal Server Error} exception. */
	public static InternalServerError internalServerError() { return new InternalServerError(); }

	/** @param message The error message. @return A new {@code 500 Internal Server Error} exception. */
	public static InternalServerError internalServerError(String message) { return new InternalServerError(message); }

	/** @param cause The cause. @return A new {@code 500 Internal Server Error} exception. */
	public static InternalServerError internalServerError(Throwable cause) { return new InternalServerError(cause); }

	/** @return A new {@code 501 Not Implemented} exception. */
	public static NotImplemented notImplemented() { return new NotImplemented(); }

	/** @return A new {@code 503 Service Unavailable} exception. */
	public static ServiceUnavailable serviceUnavailable() { return new ServiceUnavailable(); }

	/** @param message The error message. @return A new {@code 503 Service Unavailable} exception. */
	public static ServiceUnavailable serviceUnavailable(String message) { return new ServiceUnavailable(message); }
}
