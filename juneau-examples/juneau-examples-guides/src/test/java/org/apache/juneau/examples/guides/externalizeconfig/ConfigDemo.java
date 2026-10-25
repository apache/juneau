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

// region demo
import org.apache.juneau.config.*;
import org.apache.juneau.config.store.*;

/**
 * Loads the same settings from INI and from YAML, reads them three ways, and shows a system-property override.
 */
public class ConfigDemo {

	public static void main(String[] args) throws Exception {
		System.out.println(run());
	}

	public static String run() throws Exception {
		// The file extension picks the format; the classpath store reads from src/main/resources.
		var ini = Config.create("ExternalizeConfig/app.ini").store(ClasspathStore.DEFAULT).build();
		var yaml = Config.create("ExternalizeConfig/app.yaml").store(ClasspathStore.DEFAULT).build();

		// One key at a time, converted to the type you ask for.
		var port = ini.get("server/port").asInteger().orElse(0);
		var user = ini.get("database/user").asString().orElse(null);
		var poolSize = yaml.get("database/poolSize").asInteger().orElse(0);

		// A whole section through an interface.
		var server = yaml.getSection("server").asInterface(AppSettings.class).orElseThrow();

		// $S{app.port,8080} is resolved every time the value is read.
		System.setProperty("app.port", "9090");
		String overridden;
		try {
			overridden = ini.get("server/baseUrl").asString().orElse(null);
		} finally {
			System.clearProperty("app.port");
		}

		return String.join("\n",
			"ini  server/port:       " + port,
			"ini  database/user:     " + user,
			"yaml database/poolSize: " + poolSize,
			"yaml server section:    " + server.getHost() + " " + server.getPort() + " " + server.getBaseUrl(),
			"with -Dapp.port=9090:   " + overridden);
	}
}
// endregion demo
