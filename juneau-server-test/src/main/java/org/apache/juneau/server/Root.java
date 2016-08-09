/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.server.labels.*;

@RestResource(
	path="/",
	children={
		TestAcceptCharset.class,
		TestBeanContextProperties.class,
		TestCallbackStrings.class,
		TestCharsetEncodings.class,
		TestClientVersion.class,
		TestConfig.class,
		TestContent.class,
		TestDefaultContentTypes.class,
		TestErrorConditions.class,
		TestTransforms.class,
		TestGroups.class,
		TestGzip.TestGzipOff.class,
		TestGzip.TestGzipOn.class,
		TestInheritance.TestEncoders.class,
		TestInheritance.TestTransforms.class,
		TestInheritance.TestParsers.class,
		TestInheritance.TestProperties.class,
		TestInheritance.TestSerializers.class,
		TestLargePojos.class,
		TestMessages.TestMessages2.class,
		TestMessages.class,
		TestNls.class,
		TestNlsProperty.class,
		TestNoParserInput.class,
		TestOnPostCall.class,
		TestOnPreCall.class,
		TestOptionsWithoutNls.class,
		TestOverlappingMethods.class,
		TestParams.class,
		TestParsers.class,
		TestPath.class,
		TestPaths.class,
		TestProperties.class,
		TestRestClient2.class,
		TestSerializers.class,
		TestStaticFiles.class,
		TestUris.class,
		TestUrlContent.class,
		ShutdownResource.class
	}
)
public class Root extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@RestMethod(name="GET", path="/")
	public ChildResourceDescriptions doGet(RestRequest req) {
		return new ChildResourceDescriptions(this, req);
	}
}