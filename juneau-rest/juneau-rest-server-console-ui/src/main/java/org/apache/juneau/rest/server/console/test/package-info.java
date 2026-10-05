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
 * Test helpers for console pages: {@link org.apache.juneau.rest.server.console.test.PageContractAssert} asserts on the
 * {@code #juneau-page} contract of a rendered page instead of on rendered chrome HTML.
 *
 * <p>
 * Main scope on purpose (C1-D3): adopters already depend on {@code juneau-rest-server-console-ui}. The package has no
 * test-framework dependency; failures are plain {@link java.lang.AssertionError}s.
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.console.test;
