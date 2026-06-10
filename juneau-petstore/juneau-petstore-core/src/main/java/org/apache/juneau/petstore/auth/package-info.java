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
 * Petstore auth helpers — stub validators and other glue used by the deployment-side
 * {@code AuthFilterChain} configurations.
 *
 * <p>
 * The core ships only deployment-agnostic validators (e.g. {@link org.apache.juneau.petstore.auth.StubBearerTokenValidator}).
 * Each deployment ({@code juneau-petstore-jetty}, {@code juneau-petstore-springboot}) wires its own
 * {@code AuthFilterChain} bean composing these validators with the appropriate filter mounts.
 */
package org.apache.juneau.petstore.auth;
