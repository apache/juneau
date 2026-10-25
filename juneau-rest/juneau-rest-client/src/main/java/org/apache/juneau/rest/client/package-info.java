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
 * Key types:
 * <ul>
 * 	<li>{@link org.apache.juneau.rest.client.HttpTransport HttpTransport} — SPI for pluggable HTTP transports
 * 	<li>{@link org.apache.juneau.rest.client.TransportRequest TransportRequest} — transport-layer request DTO
 * 	<li>{@link org.apache.juneau.rest.client.TransportResponse TransportResponse} — transport-layer response DTO
 * 	<li>{@link org.apache.juneau.rest.client.RestClient RestClient} — high-level REST client
 * 	<li>{@link org.apache.juneau.rest.client.RestRequest RestRequest} — fluent request builder
 * 	<li>{@link org.apache.juneau.rest.client.RestResponse RestResponse} — response wrapper with deserialization support
 * </ul>
 */
package org.apache.juneau.rest.client;
