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
package org.apache.juneau.server;

import java.net.*;


public class Constants {

	private static String juneauSampleUrl = System.getProperty("JUNO_SAMPLE_URL", "http://localhost:10000");
	private static URI juneauSampleUri = (juneauSampleUrl == null ? null : URI.create(juneauSampleUrl));

	/**
	 * Returns the value of the "JUNO_SAMPLE_URL" system property, or throws a {@link RuntimeException}
	 * if it's not set.
	 */
	public static String getJuneauSamplesUrl() {
		if (juneauSampleUrl == null)
			throw new RuntimeException("'JUNO_SAMPLE_URL' system property not set to URL of juneau.sample.war location.");
		return juneauSampleUrl;
	}

	public static URI getJuneauSamplesUri() {
		if (juneauSampleUri == null)
			throw new RuntimeException("'JUNO_SAMPLE_URL' system property not set to URL of juneau.sample.war location.");
		return juneauSampleUri;
	}

	private static String juneauServerTestUrl = System.getProperty("JUNO_SERVER_TEST_URL", "http://localhost:10001");
	private static URI juneauServerTestUri = (juneauServerTestUrl == null ? null : URI.create(juneauServerTestUrl));

	public static String getServerTestUrl() {
		if (juneauServerTestUrl == null)
			throw new RuntimeException("'JUNO_SERVER_TEST_URL' system property not set to URL of juneau.sample.war location.");
		return juneauServerTestUrl;
	}

	public static URI getServerTestUri() {
		if (juneauServerTestUri == null)
			throw new RuntimeException("'JUNO_SERVER_TEST_URL' system property not set to URL of juneau.sample.war location.");
		return juneauServerTestUri;
	}
}
