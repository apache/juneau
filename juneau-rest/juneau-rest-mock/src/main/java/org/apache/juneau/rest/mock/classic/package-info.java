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
 * Mocked REST client and supporting classes for in-JVM testing built on the classic Apache HttpClient API.
 *
 * <p>
 * Provides {@link org.apache.juneau.rest.mock.classic.MockRestClient} and its associated
 * {@link org.apache.juneau.rest.mock.classic.MockRestRequest request}/{@link org.apache.juneau.rest.mock.classic.MockRestResponse response}
 * wrappers, along with helpers for capturing console output and log messages, allowing REST resources to be
 * exercised without starting a server.
 */
package org.apache.juneau.rest.mock.classic;
