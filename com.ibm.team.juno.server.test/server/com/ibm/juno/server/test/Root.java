/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.test;

import com.ibm.juno.microservice.resources.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.labels.*;

@RestResource(
	path="/",
	children={
		TestAcceptCharset.class,
		TestBeanContextProperties.class,
		TestCallbackStrings.class,
		TestCharsetEncodings.class,
		TestConfig.class,
		TestContent.class,
		TestDefaultContentTypes.class,
		TestErrorConditions.class,
		TestFilters.class,
		TestGroups.class,
		TestGzip.TestGzipOff.class,
		TestGzip.TestGzipOn.class,
		TestInheritance.TestEncoders.class,
		TestInheritance.TestFilters.class,
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
		TestRestClient.class,
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