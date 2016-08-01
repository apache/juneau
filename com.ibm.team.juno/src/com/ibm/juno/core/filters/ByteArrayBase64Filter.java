/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filters;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Transforms <code><jk>byte</jk>[]</code> arrays to BASE-64 encoded {@link String Strings}.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ByteArrayBase64Filter extends PojoFilter<byte[],String> {

	/**
	 * Converts the specified <code><jk>byte</jk>[]</code> to a {@link String}.
	 */
	@Override /* PojoFilter */
	public String filter(byte[] b) throws SerializeException {
		try {
			return StringUtils.base64Encode(b);
		} catch (Exception e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Converts the specified {@link String} to a <code><jk>byte</jk>[]</code>.
	 */
	@Override /* PojoFilter */
	public byte[] unfilter(String s, ClassMeta<?> hint) throws ParseException {
		try {
			return StringUtils.base64Decode(s);
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}
