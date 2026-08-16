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
package com.example.myapp;

import org.apache.juneau.microservice.jetty.*;

/**
 * Jetty entry point.
 *
 * <p>Boots Jetty + Juneau on the port from the bundled {@code juneau.cfg} (default 10000) with the
 * interactive console enabled, mounting {@link RootResources} at its {@code @Rest(path)}.
 */
public class App {

	private App() {}

	/**
	 * Entry point.
	 *
	 * @param args Command-line arguments.
	 * @throws Exception If the microservice fails to start.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception mirrors the Microservice lifecycle contract.
	})
	public static void main(String[] args) throws Exception {
		JettyMicroservice.run(args, new RootResources()).join();
	}
}
