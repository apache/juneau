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
 * Next-generation HTTP primitives — JDK-native replacements for the Apache HttpCore-based types in
 * {@code org.apache.juneau.http}.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This entire package tree ({@code org.apache.juneau.ng.http.*}) is part of the
 * next-generation REST client and HTTP stack.
 * Binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release (and possibly earlier).
 * For production use cases that require long-term binary stability, continue using the existing
 * {@code juneau-rest-client} and {@code juneau-rest-common} APIs until the {@code ng} stack is declared stable.
 *
 * <p>
 * Key contracts defined here:
 * <ul>
 * 	<li>{@link org.apache.juneau.ng.http.HttpHeader} — replaces {@code org.apache.http.Header}
 * 	<li>{@link org.apache.juneau.ng.http.HttpPart} — replaces {@code org.apache.http.NameValuePair}
 * 	<li>{@link org.apache.juneau.ng.http.HttpBody} — replaces {@code org.apache.http.HttpEntity}
 * 	<li>{@link org.apache.juneau.ng.http.HttpStatusLine} — replaces {@code org.apache.http.StatusLine}
 * </ul>
 * Additional response message types ({@link org.apache.juneau.ng.http.response.HttpResponseMessage},
 * {@link org.apache.juneau.ng.http.response.HttpStatusLineBean}) live in {@code org.apache.juneau.ng.http.response}.
 */
package org.apache.juneau.ng.http;
