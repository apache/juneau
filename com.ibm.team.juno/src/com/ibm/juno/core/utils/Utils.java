/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

/**
 * Various utility methods.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class Utils {

	/**
	 * Compare two integers numerically.
	 *
	 * @param i1 Integer #1
	 * @param i2 Integer #2
     * @return	the value <code>0</code> if Integer #1 is
     * 		equal to Integer #2; a value less than
     * 		<code>0</code> if Integer #1 numerically less
     * 		than Integer #2; and a value greater
     * 		than <code>0</code> if Integer #1 is numerically
     * 		 greater than Integer #2 (signed
     * 		 comparison).
	 */
	public static final int compare(int i1, int i2) {
		return (i1<i2 ? -1 : (i1==i2 ? 0 : 1));
	}
}
