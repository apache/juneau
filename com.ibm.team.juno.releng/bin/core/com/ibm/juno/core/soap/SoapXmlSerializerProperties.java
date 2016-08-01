/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.soap;

/**
 * Properties associated with the {@link SoapXmlSerializer} class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class SoapXmlSerializerProperties {

	/**
	 * The <code>SOAPAction</code> HTTP header value to set on responses.
	 * <p>
	 * Default is <js>"http://www.w3.org/2003/05/soap-envelope"</js>.
	 */
	public static final String SOAPXML_SOAPAction = "SoapXmlSerializer.SOAPAction";
}
