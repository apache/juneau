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
 * Validator of certificates presented by a server when establishing an SSL
 * connection.
 */
@Deprecated // Use SimpleX509TrustManager
public interface ICertificateValidator {

	/** Action to take for a server-supplied certificate. */
	public enum Trust {

		/** Do not accept the certificate. */
		REJECT,

		/** Accept the certificate temporarily for the current connection. */
		ACCEPT_CONNECTION,

		/** Accept the certificate temporarily for the current session. */
		ACCEPT_SESSION,

		/** Accept the certificate permanently, by saving it in the user's trust store.*/
		ACCEPT_PERMANENT
	}

	/**
	 * There is a problem accepting the server-supplied certificate. What should
	 * be done?
	 *
	 * @param cert The problematic certificate presented by the server
	 * @param problem The {@link CertificateException} that may indicate the specific
	 * 	problem with the certificate, e.g. {@link CertificateExpiredException}.
	 * @return The disposition on the certificate.
	 */
	Trust validate(X509Certificate cert, CertificateException problem);
}
