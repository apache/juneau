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
 * Built-in health, readiness, and liveness probe support for Juneau REST servers.
 *
 * <p>
 * Provides the {@link org.apache.juneau.rest.server.health.HealthIndicator} SPI for reporting
 * per-component health, an {@link org.apache.juneau.rest.server.health.HealthAggregator} that combines
 * results, and servlet, child-resource, and mixin flavors 
 * ({@link org.apache.juneau.rest.server.health.HealthServlet},
 * {@link org.apache.juneau.rest.server.health.HealthResource},
 * {@link org.apache.juneau.rest.server.health.HealthMixin}) that expose the probe endpoints.
 */
package org.apache.juneau.rest.server.health;
