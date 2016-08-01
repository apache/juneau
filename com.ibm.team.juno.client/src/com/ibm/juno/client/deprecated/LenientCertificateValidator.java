/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2010, 2015. All Rights Reserved.
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client.deprecated;

import java.security.cert.*;

/**
 * Lenient certificate validator that always accepts invalid certificates.
 */
@Deprecated // Use SimpleX509TrustManager
public final class LenientCertificateValidator implements ICertificateValidator {

	/** Singleton */
	public static final ICertificateValidator INSTANCE = new LenientCertificateValidator();

	@Override /* ICertificateValidator */
	public Trust validate(X509Certificate certificate, CertificateException problem) {
		return Trust.ACCEPT_CONNECTION;
	}
}
