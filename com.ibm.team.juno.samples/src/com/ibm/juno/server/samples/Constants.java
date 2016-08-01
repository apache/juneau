/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples;


public class Constants {

	private static String junoSampleUrl = System.getProperty("JUNO_SAMPLE_URL");

	/**
	 * Returns the value of the "JUNO_SAMPLE_URL" system property, or throws a {@link RuntimeException}
	 * if it's not set.
	 */
	public static String getSampleUrl() {
		if (junoSampleUrl == null)
			return "http://localhost:10000";
		return junoSampleUrl;
	}
}
