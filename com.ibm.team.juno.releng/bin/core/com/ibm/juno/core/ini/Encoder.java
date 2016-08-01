/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.ini;

/**
 * API for defining a string encoding/decoding mechanism for entries in {@link ConfigFile}.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public interface Encoder {

	/**
	 * Encode a string.
	 *
	 * @param fieldName The field name being encoded.
	 * @param in The unencoded input string.
	 * @return The encoded output string.
	 */
	public String encode(String fieldName, String in);

	/**
	 * Decode a string.
	 *
	 * @param fieldName The field name being decoded.
	 * @param in The encoded input string.
	 * @return The decoded output string.
	 */
	public String decode(String fieldName, String in);
}
