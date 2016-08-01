/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

/**
 * Stores a set of ASCII characters for quick lookup.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class AsciiSet {
	final boolean[] store = new boolean[128];

	/**
	 * Constructor.
	 *
	 * @param chars The characters to keep in this store.
	 */
	public AsciiSet(String chars) {
		for (int i = 0; i < chars.length(); i++) {
			char c = chars.charAt(i);
			if (c < 128)
				store[c] = true;
		}
	}

	/**
	 * Returns <jk>true<jk> if the specified character is in this store.
	 *
	 * @param c The character to check.
	 * @return <jk>true<jk> if the specified character is in this store.
	 */
	public boolean contains(char c) {
		if (c > 127)
			return false;
		return store[c];
	}

	/**
	 * Returns <jk>true<jk> if the specified character is in this store.
	 *
	 * @param c The character to check.
	 * @return <jk>true<jk> if the specified character is in this store.
	 */
	public boolean contains(int c) {
		if (c < 0 || c > 127)
			return false;
		return store[c];
	}
}
