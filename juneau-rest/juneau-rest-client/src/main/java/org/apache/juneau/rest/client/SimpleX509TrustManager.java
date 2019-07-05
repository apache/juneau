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
package org.apache.juneau.rest.client;

import java.security.*;
import java.security.cert.*;

import javax.net.ssl.*;

/**
 * A trust manager that optionally allows for self-signed certificates.
 */
public final class SimpleX509TrustManager implements X509TrustManager {

	private X509TrustManager baseTrustManager;  // The JRE-provided trust manager used to validate certificates presented by a server.

	/**
	 * Constructor.
	 *
	 * @param lax If <jk>true</jk>, allow self-signed and expired certificates.
	 * @throws KeyStoreException Generic keystore exception.
	 * @throws NoSuchAlgorithmException Unknown cryptographic algorithm.
	 */
	public SimpleX509TrustManager(boolean lax) throws KeyStoreException, NoSuchAlgorithmException {
		if (! lax) {
			// Find the JRE-provided X509 trust manager.
			KeyStore ks = KeyStore.getInstance("jks");
			TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			factory.init(ks);
			for (TrustManager tm : factory.getTrustManagers()) {
				if (tm instanceof X509TrustManager) {
					baseTrustManager = (X509TrustManager)tm; // Take the first X509TrustManager we find
					return;
				}
			}
			throw new IllegalStateException("Couldn't find JRE's X509TrustManager");
		}
	}

	@Override /* X509TrustManager */
	public X509Certificate[] getAcceptedIssuers() {
		return baseTrustManager == null ? new X509Certificate[0] : baseTrustManager.getAcceptedIssuers();
	}

	@Override /* X509TrustManager */
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		if (baseTrustManager != null)
			baseTrustManager.checkClientTrusted(chain, authType);
	}

	@Override /* X509TrustManager */
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		if (baseTrustManager != null)
			baseTrustManager.checkServerTrusted(chain, authType);
	}
}
