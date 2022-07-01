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
package org.apache.juneau.microservice.jetty;

import java.io.*;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.xml.*;

/**
 * Basic implementation of a Jetty server factory.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-microservice-jetty}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class BasicJettyServerFactory implements JettyServerFactory {

	@Override
	public Server create(String jettyXml) throws Exception {
		if (jettyXml == null)
			throw new RuntimeException("jetty.xml file location was not specified in the configuration file (Jetty/config) or manifest file (Jetty-Config) or found on the file system or classpath.");
		XmlConfiguration xmlConfiguration = new XmlConfiguration(new ByteArrayInputStream(jettyXml.getBytes()));
		return (Server)xmlConfiguration.configure();
	}
}
