/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.juno.server.tests.sample.*;

@RunWith(Suite.class)
@SuiteClasses({
	CT_AddressBookResource.class,
	CT_JacocoDummy.class,
	CT_RestUtils.class,
	CT_RootResources.class,
	CT_SampleRemoteableServicesResource.class,
	CT_TestAcceptCharset.class,
	CT_TestBeanContextProperties.class,
	CT_TestCallbackStrings.class,
	CT_TestCharsetEncodings.class,
	CT_TestConfig.class,
	CT_TestContent.class,
	CT_TestDefaultContentTypes.class,
	CT_TestErrorConditions.class,
	CT_TestFilters.class,
	CT_TestGroups.class,
	CT_TestGzip.class,
	CT_TestInheritance.class,
	CT_TestLargePojos.class,
	CT_TestMessages.class,
	CT_TestMultiPartFormPosts.class,
	CT_TestNls.class,
	CT_TestNlsProperty.class,
	CT_TestNoParserInput.class,
	CT_TestOnPostCall.class,
	CT_TestOnPreCall.class,
	CT_TestOptionsWithoutNls.class,
	CT_TestOverlappingMethods.class,
	CT_TestParams.class,
	CT_TestParsers.class,
	CT_TestPath.class,
	CT_TestPaths.class,
	CT_TestProperties.class,
	CT_TestRestClient.class,
	CT_TestSerializers.class,
	CT_TestStaticFiles.class,
	CT_TestUris.class,
	CT_TestUrlContent.class,
	CT_UrlPathPattern.class,
})
public class AllTests {}
