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
package org.apache.juneau.microservice.management;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.health.*;
import org.apache.juneau.rest.server.management.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Convenience assembly of the Actuator-style management surface under a single configurable path prefix.
 *
 * <p>
 * Mounts the management endpoints by composing their {@code *Mixin} flavors via
 * {@link Rest#mixins() @Rest(mixins=...)} on top of {@link BasicRestServletGroup}:
 * <ul>
 * 	<li>{@code /healthz}, {@code /readyz}, {@code /livez} &mdash; {@link HealthMixin} (read-through health view)
 * 	<li>{@code /threaddump}, {@code /heapdump} &mdash; {@link DumpsMixin} (deny-by-default diagnostics)
 * </ul>
 *
 * <p>
 * <b>{@code /info} and {@code /loggers} are off by default</b> &mdash; unlike health, they are not mounted
 * on the bare group, since {@link InfoMixin} discloses the full manifest and {@link LoggersMixin}'s read side
 * discloses logger topology, neither behind any auth. Turn either back on (either approach suffices):
 * <ul>
 * 	<li><b>A-la-carte (recommended):</b> mount {@link InfoMixin} / {@link LoggersMixin} on a resource you
 * 		control, e.g. a {@code BasicActuatorGroup} subclass with
 * 		{@code @Rest(mixins={InfoMixin.class, LoggersMixin.class})} &mdash; mixins declared on a subclass are
 * 		additive to the mixins inherited from this class, so {@code HealthMixin}/{@code DumpsMixin} stay mounted
 * 		too.
 * 	<li><b>Zero-code:</b> not currently supported &mdash; {@code @Rest(mixins=...)} is a compile-time class
 * 		list, and the {@code RestContext.Builder} injection point that would have let a system property add a
 * 		mixin at init time was removed prior to 10.0.0 (see {@link RestInit}).  Use the a-la-carte subclass above.
 * </ul>
 *
 * <p>
 * <b>Path prefix:</b> defaults to {@code /actuator} and is configurable via the {@code juneau.actuator.path}
 * system property (resolved through the standard {@code $S{...}} SVL var).  Mount the group at a custom prefix
 * either by setting that property or by subclassing with your own {@link Rest#path() @Rest(path=...)}.
 *
 * <p>
 * <b>Composition is convenience, not the only way in:</b> every endpoint remains independently mountable
 * a-la-carte via its standalone {@code *Mixin}/{@code *Resource} flavor.  The {@code /metrics} endpoint is
 * <b>not</b> assembled here &mdash; it lives in the {@code juneau-rest-server-metrics-micrometer} module
 * (which this module does not depend on), so add {@code MetricsMixin} a-la-carte when that module is present.
 *
 * <p>
 * <b>Exposure policy:</b> of the mounted endpoints, health is on; the mutating/sensitive ones
 * ({@code /threaddump}, {@code /heapdump}) are deny-by-default via {@link DumpsSettings}.  No auth provider is
 * auto-wired (explicit-over-magic).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ManagementSurface">Management Surface</a>
 * </ul>
 *
 * @serial exclude
 * @since 10.0.0
 */
@Rest(
	path="$S{juneau.actuator.path,/actuator}",
	title="Management",
	description="Actuator-style management surface.",
	mixins={
		HealthMixin.class,
		DumpsMixin.class
	}
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for microservice hierarchy.
})
public class BasicActuatorGroup extends BasicRestServletGroup {
	private static final long serialVersionUID = 1L;
}
