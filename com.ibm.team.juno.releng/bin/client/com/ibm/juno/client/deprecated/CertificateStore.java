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
import java.util.*;

/**
 * Specialized certificate storage based on {@link KeyStore} for managing trusted certificates.
 */
@Deprecated // Use SimpleX509TrustManager
public class CertificateStore {

	private final KeyStore keyStore;

	/**
	 * Get the underlying KeyStore.
	 */
	KeyStore getKeyStore() {
		return keyStore;
	}

	/**
	 * Helper method that creates a {@link KeyStore} by reading it from a file.
	 */
	static KeyStore load(File file, String password) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		try {
			InputStream input = new FileInputStream(file);
			try {
				ks.load(input, password == null ? null : password.toCharArray());
			} finally {
				input.close();
			}
		} catch (IOException e) {
			// Return an empty initialized KeyStore
			ks.load(null, null);
		}
		return ks;
	}

	/**
	 * Helper method that writes a {@link KeyStore} to a file.
	 */
	static void store(KeyStore ks, File file, String password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		OutputStream output = new FileOutputStream(file);
		try {
			ks.store(output, password == null ? null : password.toCharArray());
		} finally {
			output.close();
		}
	}

	/**
	 * Helper to compute a unique alias within the trust store for a specified certificate.
	 * @param cert The certificate to compute an alias for.
	 */
	static String computeAlias(Certificate cert) {
		// There appears to be no standard way to construct certificate aliases,
		// but this class never depends on looking up a certificate by its
		// computed alias, so just create an alias that's unique and be done.
		return UUID.randomUUID().toString();
	}

	/**
	 * Construct a new TrustStore initially containing no certificates.
	 */
	public CertificateStore() throws NoSuchAlgorithmException, CertificateException, IOException {
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		} catch (KeyStoreException e) {
			// If the code above caused a KeyStoreException, then the JVM classpath is probably messed up.
			throw new RuntimeException("KeyStoreException: ["+e.getLocalizedMessage()+"]. "
				+ "Likely cause is that the Java Cryptography Extension libraries are missing from the JRE classpath.  "
				+ "Make sure %JAVA_HOME%/lib/ext is specified in your JVM's java.ext.dirs system property.");
		}
		keyStore.load(null, null);
	}

	/**
	 * Does the trust store contain the specified certificate?
	 */
	public boolean containsCertificate(Certificate cert) throws KeyStoreException {
		return (keyStore.getCertificateAlias(cert) != null);
	}

	/**
	 * Enter the specified certificate into the trust store.
	 */
	public void enterCertificate(Certificate cert) throws KeyStoreException {
		if (! containsCertificate(cert))
			keyStore.setCertificateEntry(computeAlias(cert), cert);
	}

	/*
	 * Helper to copy all the certificate entries, and none of the other
	 * entries, from a {@link KeyStore} into the trust store.
	 */
	private void enterCertificates(KeyStore ks) throws KeyStoreException {
		for (Enumeration<String> e = ks.aliases(); e.hasMoreElements();) {
			String alias = e.nextElement();
			if (ks.isCertificateEntry(alias)) {
				Certificate cert = ks.getCertificate(alias);
				enterCertificate(cert);
			}
		}
	}

	/**
	 * Load the specified {@link KeyStore} file and copy all of the certificates
	 * it contains into the trust store. Only certificates, and not any other
	 * entries, are loaded.
	 */
	public void loadCertificates(File file, String password) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
		KeyStore ks = load(file, password);
		enterCertificates(ks);
	}
}
