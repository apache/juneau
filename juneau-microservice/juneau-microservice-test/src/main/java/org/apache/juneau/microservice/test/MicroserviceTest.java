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
package org.apache.juneau.microservice.test;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

/**
 * Boots a whole {@link org.apache.juneau.microservice.Microservice Microservice} (config + lifecycle +
 * embedded Jetty server) for the annotated JUnit 5 test class &mdash; the standalone-microservice analog of
 * Spring's {@code @SpringBootTest}.
 *
 * <p>
 * {@code @MicroserviceTest} is a single, server-agnostic meta-annotation: it composes
 * {@link ExtendWith @ExtendWith}({@link MicroserviceExtension}) plus a {@code @Tag("microservice")} marker.
 * The extension builds the microservice on an OS-assigned ephemeral port before the test class runs, and
 * {@link org.apache.juneau.microservice.Microservice#stop() stop()}s it afterward (releasing the port and
 * firing {@code @PreDestroy} hooks).
 *
 * <h5 class='topic'>Specifying the system under test</h5>
 *
 * <p>
 * The SUT is declared <i>explicitly</i> (no classpath scanning), two complementary ways:
 * <ol>
 * 	<li>{@link #configurations()} &mdash; one or more {@code @Configuration} classes whose {@code @Bean Servlet}
 * 		methods are auto-mounted by the microservice (the common case).
 * 	<li>A {@code static} method on the test class returning a
 * 		{@link org.apache.juneau.microservice.Microservice.Builder Microservice.Builder} &mdash; for full control
 * 		over the builder. Discovered by name {@value MicroserviceExtension#BUILDER_SUPPLIER_METHOD} (override via
 * 		{@link #builderMethod()}). Configurations from {@link #configurations()} are appended to whatever the
 * 		supplier returns.
 * </ol>
 *
 * <p>
 * In both cases the extension additionally installs an {@link EphemeralJettyServerConfig} (binding a Jetty
 * {@code Server} to port 0) and {@code JettyConfiguration}, so tests only contribute their resources.
 *
 * <h5 class='topic'>Mock-bean injection</h5>
 *
 * <p>
 * Declare collaborators to substitute with the existing {@link org.apache.juneau.junit5.TestBean @TestBean}
 * (the {@code @MockBean} analog) &mdash; no parallel injection annotation. By default these are installed via
 * <b>Mode INJECT</b> (through {@code Microservice.Builder.overridingBeanStore(...)}) <i>before</i> boot, so the
 * service sees them from startup. {@code @TestBean(mode = Mode.OVERLAY)} pushes/pops against the already-booted
 * instance instead.
 *
 * <h5 class='topic'>What gets injected into tests</h5>
 *
 * <p>
 * Test methods (and lifecycle methods) may declare parameters resolved by the extension:
 * {@link org.apache.juneau.rest.client.RestClient RestClient} (bound to the booted server's root URL &mdash; the
 * primary convenience), {@link org.apache.juneau.microservice.Microservice Microservice}, the
 * {@link org.apache.juneau.commons.inject.WritableBeanStore WritableBeanStore}, and the bound port (as {@code int}
 * / {@code Integer}). The same {@code TestBeanStore} parameter the underlying
 * {@link org.apache.juneau.junit5.JuneauBeanStoreExtension} resolves is available too.
 *
 * <h5 class='topic'>When to use this vs. {@code MockRestClient}</h5>
 *
 * <p>
 * Use {@code @MicroserviceTest} for a genuine full-microservice integration test &mdash; real server, real
 * connectors, real lifecycle, over HTTP. For an in-JVM test of a single {@code @Rest} resource (no server, no
 * sockets), use {@code MockRestClient} directly; the two paths are intentionally distinct.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@MicroserviceTest</ja>(configurations=MyServerConfig.<jk>class</jk>)
 * 	<jk>class</jk> MyServiceTest {
 *
 * 		<ja>@Configuration</ja>
 * 		<jk>static class</jk> MyServerConfig {
 * 			<ja>@Bean</ja> Servlet myService() { <jk>return new</jk> MyRestService(); }
 * 		}
 *
 * 		<ja>@TestBean</ja>
 * 		MyExternalApi <jv>mockApi</jv> = Mockito.<jsm>mock</jsm>(MyExternalApi.<jk>class</jk>);
 *
 * 		<ja>@Test</ja>
 * 		<jk>void</jk> aTest(RestClient <jv>client</jv>) {
 * 			<jv>client</jv>.get(<js>"/widgets/1"</js>).run().assertStatus().is(200);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MicroserviceExtension}
 * 	<li class='jc'>{@link org.apache.juneau.junit5.TestBean}
 * </ul>
 *
 * @since 10.0.0
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@ExtendWith(MicroserviceExtension.class)
@Tag("microservice")
public @interface MicroserviceTest {

	/**
	 * The {@code @Configuration} classes whose {@code @Bean Servlet} methods the microservice auto-mounts.
	 *
	 * <p>
	 * Appended after any builder produced by a {@link #builderMethod()} supplier, and before the framework's
	 * {@link EphemeralJettyServerConfig} + {@code JettyConfiguration}.
	 *
	 * @return The configuration classes. Empty by default (rely on a {@link #builderMethod()} supplier).
	 */
	Class<?>[] configurations() default {};

	/**
	 * Name of an optional {@code static} method on the test class returning a
	 * {@link org.apache.juneau.microservice.Microservice.Builder Microservice.Builder} to seed the boot.
	 *
	 * <p>
	 * The method must be {@code static}, take no arguments, and return a {@code Microservice.Builder}. When absent,
	 * a fresh {@code Microservice.create()} builder is used. {@link #configurations()} are appended either way.
	 *
	 * @return The supplier method name. Defaults to {@value MicroserviceExtension#BUILDER_SUPPLIER_METHOD}.
	 */
	String builderMethod() default MicroserviceExtension.BUILDER_SUPPLIER_METHOD;
}
