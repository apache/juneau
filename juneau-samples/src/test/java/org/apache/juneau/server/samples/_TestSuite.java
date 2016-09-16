package org.apache.juneau.server.samples;

import org.apache.juneau.microservice.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

/**
 * Runs all the testcases in this project.
 * Starts a REST service running org.apache.juneau.server.samples.RootResources on port 10000.
 * Stops the REST service after running the tests.
 */
@RunWith(Suite.class)
@SuiteClasses({
	AddressBookResourceTest.class,
	RootResourcesTest.class,
	SampleRemoteableServicesResourceTest.class,
	TestMultiPartFormPostsTest.class
})
public class _TestSuite {
	static RestMicroservice microservice;

   @BeforeClass
   public static void setUp() {
   	try {
			microservice = new RestMicroservice(new String[0]);
			microservice.start();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
   }
   
   @AfterClass
   public static void tearDown() {
   	try {
			microservice.stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
   }
}
