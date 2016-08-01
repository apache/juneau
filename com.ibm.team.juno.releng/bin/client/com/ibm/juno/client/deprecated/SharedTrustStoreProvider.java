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
import java.security.cert.Certificate;

/**
 * Trust store provider with shared static certificate stores.
 */
@Deprecated // Use SimpleX509TrustManager
public final class SharedTrustStoreProvider implements ITrustStoreProvider {

	// In-memory trust store of all certificates explicitly accepted by the
	// certificate validator during this session. The validator will not be
	// called again during this session for any of these certificates. These may
	// include expired, not yet valid, or otherwise untrusted certificates.
	// These are kept distinctly, rather than merged into the runtime trust
	// store, because the base trust manager will never accept expired, etc.
	// certificates, even if from a trusted source.
	private static CertificateStore sessionCerts;

	// In-memory trust store of all permanently trusted certificates, assembled
	// from a number of key store files. These are provided to the base trust
	// manager as the basis for its decision making.
	private static CertificateStore runtimeCerts;

	// Location and password of the user's private trust store for this application.
	private static String userTrustStoreLocation;
	private static String userTrustStorePassword;

	static {
		init();
	}

	private static final void init() {
		try {
			String userHome = System.getProperty("user.home");
			String javaHome = System.getProperty("java.home");

			userTrustStoreLocation = userHome + "/.jazzcerts";
			userTrustStorePassword = "ibmrationaljazz";

			sessionCerts = new CertificateStore();

			runtimeCerts = new CertificateStore();

			// JRE keystore override
			String file = System.getProperty("javax.net.ssl.trustStore");
			String password = System.getProperty("javax.net.ssl.trustStorePassword");
			addCertificatesFromStore(runtimeCerts, file, password);

			// JRE Signer CA keystore
			file = javaHome + "/lib/security/cacerts";
			addCertificatesFromStore(runtimeCerts, file, null);

			// JRE Secure Site CA keystore
			file =  (javaHome + "/lib/security/jssecacerts");
			addCertificatesFromStore(runtimeCerts, file, null);

			// Application-specific keystore for the current user
			addCertificatesFromStore(runtimeCerts, userTrustStoreLocation, userTrustStorePassword);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void addCertificatesFromStore(CertificateStore store, String file, String password) {
		try {
			File f = new File(file);
			if (f.canRead())
				store.loadCertificates(f, password);
		} catch (Exception e) {
			// Discard errors
		}
	}

	@Override /* ITrustStoreProvider */
	public CertificateStore getRuntimeTrustStore() {
		return runtimeCerts;
	}

	@Override /* ITrustStoreProvider */
	public CertificateStore getSessionTrustStore() {
		return sessionCerts;
	}

	@Override /* ITrustStoreProvider */
	public void installCertificate(Certificate cert) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
		File  f = new File(userTrustStoreLocation);
		KeyStore ks = CertificateStore.load(f, userTrustStorePassword);
		ks.setCertificateEntry(CertificateStore.computeAlias(cert), cert);
		CertificateStore.store(ks, f, userTrustStorePassword);
	}
}
