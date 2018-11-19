// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.examples.rest.springboot;

import static org.apache.juneau.internal.SystemUtils.*;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

/**
 * Entry point for Examples REST application when deployed as a Spring Boot application.
 */
@SpringBootApplication
@Controller
@Import({AppConfiguration.class, AppServletConfiguration.class})
public class App {

	public static int counter = 0;
	private static volatile ConfigurableApplicationContext context;

	public static void main(String[] args) {
		try {
			setProperty("juneau.configFile", "examples.cfg", false);
			context = SpringApplication.run(App.class, args);
			if (context == null)
				System.exit(2); // Probably port in use?
			AppConfiguration.setAppContext(context);
			setProperty("juneau.serverPort", context.getEnvironment().getProperty("server.port"), false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void start() {
		main(new String[0]);
	}

	public static void stop() {
		context.stop();
	}
}
