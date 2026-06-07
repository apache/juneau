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
package org.apache.juneau.rest.server.debug;

import jakarta.servlet.http.*;

/**
 * Input values used by {@link DebugFormat}.
 *
 * @param request The request.
 * @param response The response.
 * @param exception Exception on request, if present.
 * @param execTime Execution time in millis, if present.
 * @param requestContent Cached request content.
 * @param responseContent Cached response content.
 */
@SuppressWarnings({
	"java:S6218" // DebugFormatContext is a transient input holder for DebugFormat rendering; it is never compared for equality or used as a map key, so array-identity semantics on the content[] components are irrelevant.
})
public record DebugFormatContext(
	HttpServletRequest request,
	HttpServletResponse response,
	Throwable exception,
	Long execTime,
	byte[] requestContent,
	byte[] responseContent
) {
}
