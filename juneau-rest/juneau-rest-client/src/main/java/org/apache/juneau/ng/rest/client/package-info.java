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
 * Next-generation REST client — transport-agnostic HTTP client built on Juneau's serialization stack.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This entire package ({@code org.apache.juneau.ng.rest.client}) is part of the
 * next-generation REST client and HTTP stack.
 * Binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release (and possibly earlier).
 * For production use cases that require long-term binary stability, continue using the existing
 * {@code juneau-rest-client} APIs until the {@code ng} stack is declared stable.
 *
 * <p>
 * Key types:
 * <ul>
 * 	<li>{@link org.apache.juneau.ng.rest.client.HttpTransport} — SPI for pluggable HTTP transports
 * 	<li>{@link org.apache.juneau.ng.rest.client.TransportRequest} — transport-layer request DTO
 * 	<li>{@link org.apache.juneau.ng.rest.client.TransportResponse} — transport-layer response DTO
 * 	<li>{@link org.apache.juneau.ng.rest.client.NgRestClient} — high-level REST client
 * 	<li>{@link org.apache.juneau.ng.rest.client.NgRestRequest} — fluent request builder
 * 	<li>{@link org.apache.juneau.ng.rest.client.NgRestResponse} — response wrapper with deserialization support
 * </ul>
 */
package org.apache.juneau.ng.rest.client;
