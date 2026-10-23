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

/**
 * Servlet filters that augment Juneau REST server request handling.
 *
 * <p>
 * Includes {@link org.apache.juneau.rest.server.filter.RequestIdFilter RequestIdFilter}, which assigns a unique
 * request ID to each incoming request for correlation in logs and responses, and
 * {@link org.apache.juneau.rest.server.filter.RequestBoundaryFilter RequestBoundaryFilter}, which applies a
 * {@link org.apache.juneau.rest.server.filter.RequestBoundary RequestBoundary} to every request and rejects those that did not
 * originate from a page the application served.
 *
 * <p>
 * {@link org.apache.juneau.rest.server.filter.LoopbackBoundary LoopbackBoundary} is the boundary for a loopback-bound application, and
 * {@link org.apache.juneau.rest.server.filter.LoopbackBoundaryFilter LoopbackBoundaryFilter} applies it.  Every boundary reports its decision
 * as a {@link org.apache.juneau.rest.server.filter.BoundaryResult BoundaryResult} carrying a
 * {@link org.apache.juneau.rest.server.filter.BoundaryReason BoundaryReason}.
 *
 * <p>
 * {@link org.apache.juneau.rest.server.filter.CsrfBoundary CsrfBoundary} is the boundary for an application reachable from other
 * hosts: it checks the request's origin against the application's own and, on a write, a CSRF token issued by
 * {@link org.apache.juneau.rest.server.filter.CsrfTokens CsrfTokens}, either bound to the session or shared by the process.
 * Applied through {@link org.apache.juneau.rest.server.filter.RequestBoundaryFilter RequestBoundaryFilter}, an allowed request carries the
 * token for its page both as a single value and as a supplier of freshly masked values, one per embed; and a safe
 * page load gets a session when the token is bound to one, so the page it is served carries a token that verifies.
 */
package org.apache.juneau.rest.server.filter;
