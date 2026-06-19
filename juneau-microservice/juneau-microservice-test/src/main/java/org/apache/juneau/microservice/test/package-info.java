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
 * JUnit 5 whole-microservice integration-test support &mdash; the {@code @SpringBootTest} analog for the
 * standalone Juneau microservice.
 *
 * <p>
 * {@link org.apache.juneau.microservice.test.MicroserviceTest @MicroserviceTest} boots a whole
 * {@link org.apache.juneau.microservice.Microservice Microservice} (config + lifecycle + embedded Jetty on an
 * ephemeral port) for a test class, composing the existing {@code juneau-junit5}
 * {@link org.apache.juneau.junit5.TestBean @TestBean} mock-bean substrate for collaborator substitution and
 * resolving a {@link org.apache.juneau.rest.client.RestClient RestClient} bound to the booted server for tests.
 *
 * <p>
 * Use it for genuine full-microservice integration tests (real server, connectors, lifecycle, over HTTP). For an
 * in-JVM single-{@code @Rest}-resource test, use {@code MockRestClient} directly.
 *
 * @since 10.0.0
 */
package org.apache.juneau.microservice.test;
