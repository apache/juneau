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

import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.resource.*;
import org.eclipse.jetty.xml.*;

/**
 * Basic implementation of a Jetty server factory.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-microservice-jetty">juneau-microservice-jetty</a>
 * </ul>
 */
public class BasicJettyServerFactory implements JettyServerFactory {

	@Override
	public Server create(String jettyXml) throws Exception {
		if (jettyXml == null)
			throw new RuntimeException("jetty.xml file location was not specified in the configuration file (Jetty/config) or manifest file (Jetty-Config) or found on the file system or classpath.");
		File f = FileUtils.createTempFile("jetty.xml");
		try (Reader r = new StringReader(jettyXml); Writer w = new FileWriter(f)) {
			IOUtils.pipe(r, w);
			w.flush();
		}
		XmlConfiguration xmlConfiguration = new XmlConfiguration(Resource.newResource(f));
		return (Server)xmlConfiguration.configure();
	}
}
