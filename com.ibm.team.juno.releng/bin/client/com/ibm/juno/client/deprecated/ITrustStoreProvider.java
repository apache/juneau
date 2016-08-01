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
 * Utility class for handling certificate stores.
 */
@Deprecated // Use SimpleX509TrustManager
public interface ITrustStoreProvider {

	/**
	 * Returns the store of all certificates trusted for the lifetime
	 * of this trust provider
	 */
	CertificateStore getSessionTrustStore();

	/**
	 * Returns the store of all permanently trusted certificates.
	 */
	CertificateStore getRuntimeTrustStore();

    /**
     * Install a certificate in the user's application-specific on-disk key
     * store, if possible.
     */
    public void installCertificate(Certificate cert) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException;

}
