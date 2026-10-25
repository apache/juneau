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
package org.apache.juneau.examples.guides.externalizeconfig;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.examples.guides.*;
import org.junit.jupiter.api.*;

class ExternalizeConfig_Test extends TestBase {

	@Test void a01_runOutputMatchesGuide() throws Exception {
		assertEquals(GuideRegion.read("ExternalizeConfig/run-output.txt", "output"), GuideRegion.normalize(ConfigDemo.run()));
	}

	@Test void a02_iniAndYamlAgree() throws Exception {
		var ini = Config.create("ExternalizeConfig/app.ini").store(ClasspathStore.DEFAULT).build();
		var yaml = Config.create("ExternalizeConfig/app.yaml").store(ClasspathStore.DEFAULT).build();
		for (var key : new String[]{"server/host", "server/port", "server/baseUrl", "database/user", "database/poolSize"})
			assertEquals(ini.get(key).asString().orElse(null), yaml.get(key).asString().orElse(null), key);
	}

	@Test void a03_licenseHeaderIsJustComments() throws Exception {
		var ini = Config.create("ExternalizeConfig/app.ini").store(ClasspathStore.DEFAULT).build();
		assertEquals("[, server, database]", ini.getSectionNames().toString());
	}
}
