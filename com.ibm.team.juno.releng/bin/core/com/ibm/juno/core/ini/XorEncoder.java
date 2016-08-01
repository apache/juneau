/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.ini;

import com.ibm.juno.core.utils.*;

/**
 * Simply XOR+Base64 encoder for obscuring passwords and other sensitive data in INI config files.
 * <p>
 * This is not intended to be used as strong encryption.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class XorEncoder implements Encoder {

	/** Reusable XOR-Encoder instance. */
	public static final XorEncoder INSTANCE = new XorEncoder();

   private static final String key = System.getProperty("com.ibm.juno.core.ini.XorEncoder.key", "nuy7og796Vh6G9O6bG230SHK0cc8QYkH");	// The super-duper-secret key

	@Override /* Encoder */
	public String encode(String fieldName, String in) {
		byte[] b = in.getBytes(IOUtils.UTF8);
		for (int i = 0; i < b.length; i++) {
				int j = i % key.length();
			b[i] = (byte)(b[i] ^ key.charAt(j));
		}
		return StringUtils.base64Encode(b);
	}

	@Override /* Encoder */
	public String decode(String fieldName, String in) {
		byte[] b = StringUtils.base64Decode(in);
		for (int i = 0; i < b.length; i++) {
			int j = i % key.length();
			b[i] = (byte)(b[i] ^ key.charAt(j));
	}
		return new String(b, IOUtils.UTF8);
	}
}
