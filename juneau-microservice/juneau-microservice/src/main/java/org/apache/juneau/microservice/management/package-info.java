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
 * Actuator-style management-surface endpoints (microservice-module flavors).
 *
 * <p>
 * Management-surface pieces that need the microservice runtime &mdash; principally the {@code /info}
 * endpoint, which surfaces the running application's {@link org.apache.juneau.commons.runtime.ManifestFile}
 * (build/version/git metadata) registered by {@code Microservice.init()}.  It ships in both a composable
 * {@code InfoMixin} and a routed {@code InfoResource} flavor sharing a single {@code InfoManager} worker,
 * mirroring the health and {@code /loggers} packages.
 *
 * <p>
 * The {@code /loggers}, {@code /metrics}, and dump endpoints live in the {@code juneau-rest-server} module's
 * {@code org.apache.juneau.rest.server.management} package, where only the REST server machinery is needed.
 *
 * @since 10.0.0
 */
package org.apache.juneau.microservice.management;
