/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2010, 2015. All Rights Reserved.
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client;

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
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
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
			throw new IllegalStateException("Couldn't find JRE's X509TrustManager"); //$NON-NLS-1$
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
