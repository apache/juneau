/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import java.net.*;


public class Constants {

	private static String junoSampleUrl = System.getProperty("JUNO_SAMPLE_URL", "http://localhost:10000");
	private static URI junoSampleUri = (junoSampleUrl == null ? null : URI.create(junoSampleUrl));

	/**
	 * Returns the value of the "JUNO_SAMPLE_URL" system property, or throws a {@link RuntimeException}
	 * if it's not set.
	 */
	public static String getJunoSamplesUrl() {
		if (junoSampleUrl == null)
			throw new RuntimeException("'JUNO_SAMPLE_URL' system property not set to URL of juno.sample.war location.");
		return junoSampleUrl;
	}

	public static URI getJunoSamplesUri() {
		if (junoSampleUri == null)
			throw new RuntimeException("'JUNO_SAMPLE_URL' system property not set to URL of juno.sample.war location.");
		return junoSampleUri;
	}

	private static String junoServerTestUrl = System.getProperty("JUNO_SERVER_TEST_URL", "http://localhost:10001");
	private static URI junoServerTestUri = (junoServerTestUrl == null ? null : URI.create(junoServerTestUrl));

	public static String getServerTestUrl() {
		if (junoServerTestUrl == null)
			throw new RuntimeException("'JUNO_SERVER_TEST_URL' system property not set to URL of juno.sample.war location.");
		return junoServerTestUrl;
	}

	public static URI getServerTestUri() {
		if (junoServerTestUri == null)
			throw new RuntimeException("'JUNO_SERVER_TEST_URL' system property not set to URL of juno.sample.war location.");
		return junoServerTestUri;
	}
}
