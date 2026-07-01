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
package org.apache.juneau.rest.server.docs;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.openapi3.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.openapi.*;
import org.apache.juneau.rest.server.springboot.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;
import org.springframework.context.annotation.*;

/**
 * Spring-side {@link OpenApiProvider} resolution semantics for the api-docs mixin pack.
 *
 * <p>
 * Documents and pins down the two corner cases a Spring Boot app can hit when registering more
 * than one {@link OpenApiProvider} {@code @Bean}:
 *
 * <ol>
 * 	<li><b>{@code @Primary} wins.</b> When two providers are registered, one of them marked
 * 		{@link Primary @Primary}, the
 * 		{@link SpringBeanStore SpringBeanStore.getBean(OpenApiProvider.class)} call returns the
 * 		{@code @Primary} one.
 * 	<li><b>Two unmarked providers &rarr; {@link BeanDefinitionOverrideException}.</b> Spring
 * 		refuses to start the context when two {@code @Bean} methods declare beans with the same
 * 		name (the default name is derived from the method name; we collide them on purpose using
 * 		{@code @Bean(name=...)} so both compete for the same bean id). This is the documented
 * 		"known rough edge" from resolved decisions: Juneau cannot give the user a
 * 		friendlier error here because Spring fails the context-load before Juneau gets a chance to
 * 		look the provider up.
 * </ol>
 *
 * <p>
 * Both scenarios exercise the {@link SpringBeanStore} adapter end-to-end &mdash; no mocking
 * &mdash; because {@code @Primary} resolution is delegated to
 * {@link org.springframework.context.ApplicationContext#getBeanProvider(Class)
 * ApplicationContext.getBeanProvider(...).getIfAvailable()} inside the adapter, and mocking that
 * call would invalidate the test.
 *
 * <p>
 * Uses {@link SpringApplicationBuilder} (not {@code @SpringBootTest}) because the failure case
 * needs to assert against context-load failure, which {@code @SpringBootTest} cannot easily do
 * within a single class. Both apps are configured {@link WebApplicationType#NONE non-web} so the
 * test stays fast and self-contained.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.SpringbootTest
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class BasicApiDocs_Springboot_MultiOpenApiProvider_Test {

	/** Spring Boot app config with two {@link OpenApiProvider} beans, one {@code @Primary}. */
	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class PrimaryWinsApp {

		@Bean
		@Primary
		public OpenApiProvider primaryProvider() {
			return new MarkerOpenApiProvider("primary");
		}

		@Bean
		public OpenApiProvider secondaryProvider() {
			return new MarkerOpenApiProvider("secondary");
		}
	}

	/**
	 * Spring Boot app config with two {@link OpenApiProvider} beans colliding on the same bean id
	 * (so Spring refuses to register the second one and throws on context load).
	 */
	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class CollidingApp {

		@Bean(name = "collidingProvider")
		public OpenApiProvider providerOne() {
			return new MarkerOpenApiProvider("one");
		}

		@Bean(name = "collidingProvider")
		public OpenApiProvider providerTwo() {
			return new MarkerOpenApiProvider("two");
		}
	}

	/**
	 * Marker {@link OpenApiProvider} implementation that just records which bean we got back.
	 * {@link #getOpenApi(RestContext, Locale)} is never called &mdash; we only care about
	 * {@code BeanStore} resolution, not about actually building an OpenAPI doc.
	 */
	static final class MarkerOpenApiProvider implements OpenApiProvider {
		final String name;

		MarkerOpenApiProvider(String name) {
			this.name = name;
		}

		@Override
		public OpenApi getOpenApi(RestContext context, Locale locale) {
			throw new UnsupportedOperationException("Marker provider; not for real use");
		}

		@Override
		public String toString() {
			return "MarkerOpenApiProvider(" + name + ")";
		}
	}

	@Test
	void a01_primaryProviderIsResolvedThroughSpringBeanStore() {
		try (var ctx = new SpringApplicationBuilder(PrimaryWinsApp.class)
			.web(WebApplicationType.NONE)
			.run("--spring.main.banner-mode=off")) {

			var beanStore = new SpringBeanStore(ctx, null);
			var resolved = beanStore.getBean(OpenApiProvider.class).orElse(null);

			assertNotNull(resolved, "SpringBeanStore should resolve an OpenApiProvider via @Primary");
			assertInstanceOf(MarkerOpenApiProvider.class, resolved,
				"Resolved bean should be one of the registered MarkerOpenApiProvider beans");
			assertEquals("primary", ((MarkerOpenApiProvider) resolved).name,
				"Expected the @Primary bean to win; got: " + resolved);
		}
	}

	@Test
	void a02_collidingProvidersFailContextLoadWithBeanDefinitionOverrideException() {
		var ex = assertThrows(BeanDefinitionOverrideException.class, () ->
			new SpringApplicationBuilder(CollidingApp.class)
				.web(WebApplicationType.NONE)
				.run(
					"--spring.main.banner-mode=off",
					"--spring.main.allow-bean-definition-overriding=false"
				).close());

		// Spring's exact message text drifts across versions, so we only assert the bean id appears
		// in the message — enough to confirm Spring caught the collision on the expected bean.
		assertTrue(ex.getMessage().contains("collidingProvider"),
			"Expected exception message to reference the colliding bean id 'collidingProvider'; got: "
				+ ex.getMessage());
	}
}
