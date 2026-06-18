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
 * Actuator-style management-surface endpoints (server-module flavors).
 *
 * <p>
 * Server-resident pieces of the management surface that need only the REST server machinery:  the
 * {@code /loggers} runtime log-level endpoint (JUL) and the {@code /threaddump} / {@code /heapdump} diagnostic
 * dumps.  Each ships in both a composable {@code *Mixin} and a routed {@code *Resource} flavor sharing a single
 * worker, mirroring the health package.
 *
 * <p>
 * <b>Exposure policy (deny-by-default for mutating/sensitive ops):</b> the {@code /loggers} read endpoints are
 * always on, but the set-level writes are deny-by-default via {@link org.apache.juneau.rest.server.management.LoggersSettings};
 * the {@code /threaddump} and {@code /heapdump} diagnostics are deny-by-default via
 * {@link org.apache.juneau.rest.server.management.DumpsSettings}.  No auth provider is auto-wired &mdash; a
 * consumer opts each sensitive op in explicitly by registering the corresponding settings bean.
 *
 * <p>
 * The {@code /metrics} Micrometer-registry adapter lives in the {@code juneau-rest-server-metrics-micrometer}
 * module; the {@code /info} endpoint and the convenience {@code BasicActuatorGroup} assembly live in the
 * {@code juneau-microservice} module, where the manifest/build metadata they surface is available.
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.management;
