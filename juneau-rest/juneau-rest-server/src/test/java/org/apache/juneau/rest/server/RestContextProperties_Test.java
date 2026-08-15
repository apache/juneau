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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link RestContextProperties} in isolation, binding directly via
 * {@link ConfigPropertiesBinder} (no {@link RestContext} dependency).
 *
 * @since 10.0.0
 */
class RestContextProperties_Test extends TestBase {

	private static PropertySource source(Map<String,String> m) {
		return name -> m.containsKey(name) ? PropertyLookupResult.present(Optional.of(m.get(name))) : PropertyLookupResult.missing();
	}

	@Test void a01_defaultsMatchOriginalValueAnnotationLiterals() {
		var settings = Settings.create().build();
		var p = ConfigPropertiesBinder.of(new RestContextProperties(), "RestContext").settings(settings).run();
		assertEquals("Accept,Content-Type", p.getAllowedHeaderParams());
		assertEquals("", p.getAllowedMethodHeaders());
		assertEquals("HEAD,OPTIONS", p.getAllowedMethodParams());
		assertEquals("false", p.getDisableContentParamRaw());
		assertEquals("false", p.getRenderResponseStackTracesRaw());
		assertEquals("false", p.getProblemDetailsRaw());
		assertEquals("false", p.getVirtualThreadsRaw());
		assertEquals("true", p.getResponseTraceparentRaw());
		assertEquals("true", p.getMdcAsyncPropagationRaw());
		assertEquals("false", p.getEagerInitRaw());
		assertEquals("false", p.getLazyChildrenRaw());
		assertEquals("Client-Version", p.getClientVersionHeader());
		assertEquals("", p.getUriRelativityRaw());
		assertEquals("", p.getUriResolutionRaw());
	}

	@Test void a02_bindsStringAndBooleanPropertiesFromSource() {
		var settings = Settings.create().addSource(source(Map.of(
			"RestContext.allowedHeaderParams", "Accept",
			"RestContext.renderResponseStackTraces", "true",
			"RestContext.clientVersionHeader", "X-Client-Version"
		))).build();
		var p = ConfigPropertiesBinder.of(new RestContextProperties(), "RestContext").settings(settings).run();
		assertEquals("Accept", p.getAllowedHeaderParams());
		assertEquals("true", p.getRenderResponseStackTracesRaw());
		assertEquals("X-Client-Version", p.getClientVersionHeader());
	}

	@Test void a03_bindsEnumPropertiesFromSource() {
		var settings = Settings.create().addSource(source(Map.of(
			"RestContext.uriRelativity", "PATH_INFO",
			"RestContext.uriResolution", "ABSOLUTE"
		))).build();
		var p = ConfigPropertiesBinder.of(new RestContextProperties(), "RestContext").settings(settings).run();
		assertEquals("PATH_INFO", p.getUriRelativityRaw());
		assertEquals("ABSOLUTE", p.getUriResolutionRaw());
	}

	@Test void a04_relaxedEnvStyleKeyMatches() {
		var settings = Settings.create().addSource(source(Map.of(
			"REST_CONTEXT_PROBLEM_DETAILS", "true"
		))).build();
		var p = ConfigPropertiesBinder.of(new RestContextProperties(), "RestContext").settings(settings).run();
		assertEquals("true", p.getProblemDetailsRaw());
	}
}
