/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2010, 2015. All Rights Reserved.
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client.deprecated;

import java.io.*;
import java.security.*;
import java.security.cert.*;

import javax.net.ssl.*;

/**
 * A trust manager that will call a registered {@link ICertificateValidator} in
 * the event that a problematic (e.g. expired, not yet valid) or untrusted
 * certificate is presented by a server, and react appropriately. This trust
 * manager will rely on multiple key stores, and manage one of its own. The
 * managed key store and the session-accepted key store are shared by all trust
 * manager instances.
 */
@Deprecated // Use SimpleX509TrustManager
public final class ValidatingX509TrustManager implements X509TrustManager {

	// The JRE-provided trust manager used to validate certificates presented by a server.
	private X509TrustManager baseTrustManager;

	// The registered certificate validator, may be null, called when the base
	// trust manager rejects a certificate presented by a server.
	private ICertificateValidator validator;

	private ITrustStoreProvider trustStoreProvider;

	/**
	 * Construct a new ValidatingX509TrustManager.
	 *
	 * @param validator Certificate validator to consult regarding problematic
	 * 	certificates, or <code>null</code> to always reject them.
	 */
	public ValidatingX509TrustManager(ICertificateValidator validator) throws KeyStoreException, NoSuchAlgorithmException {
		this.validator = validator;
		this.trustStoreProvider = new SharedTrustStoreProvider();

		// Initialize the base X509 trust manager that will be used to evaluate
		// certificates presented by the server against the runtime trust store.
		TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		factory.init(trustStoreProvider.getRuntimeTrustStore().getKeyStore());
		TrustManager[] managers = factory.getTrustManagers();
		for (TrustManager manager : managers) {
			if (manager instanceof X509TrustManager) {
				baseTrustManager = (X509TrustManager) manager; // Take the first X509TrustManager we find
				return;
			}
		}
		throw new IllegalStateException("Couldn't find JRE's X509TrustManager"); //$NON-NLS-1$
	}

	@Override /* X509TrustManager */
	public X509Certificate[] getAcceptedIssuers() {
		return baseTrustManager.getAcceptedIssuers();
	}

	@Override /* X509TrustManager */
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		baseTrustManager.checkClientTrusted(chain, authType);
	}

	@Override /* X509TrustManager */
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		X509Certificate cert = chain[0];

		// Has the certificate been OK'd for the session?
		try {
			if (trustStoreProvider.getSessionTrustStore().containsCertificate(cert))
				return;
		} catch (KeyStoreException e) {
			// Ignore; proceed to try base trust manager
		}

		try {
			// Rely on the base trust manager to check the certificate against the assembled runtime key store
			baseTrustManager.checkServerTrusted(chain, authType);
		} catch (CertificateException certEx) {

			// Done if there isn't a validator to consult
			if (validator == null)
				throw certEx; // Rejected!

			// Ask the registered certificate validator to rule on the certificate
			ICertificateValidator.Trust disposition = validator.validate(cert, certEx);
			switch (disposition) {
				case REJECT:				throw certEx;
				case ACCEPT_CONNECTION: break;
				case ACCEPT_SESSION:		enterCertificate(cert, false); break;
				case ACCEPT_PERMANENT:	enterCertificate(cert, true); break;
			}
		}
	}

	private void enterCertificate(X509Certificate cert, boolean permanent) throws CertificateException {
		try {
			trustStoreProvider.getSessionTrustStore().enterCertificate(cert);
			if (permanent)
				trustStoreProvider.installCertificate(cert);
		} catch (KeyStoreException e) {
		} catch (NoSuchAlgorithmException e) {
		} catch (IOException e) {
		}
	}
}
