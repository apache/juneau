/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

import java.text.*;

/**
 * General invalid conversion exception.
 * <p>
 * 	Exception that gets thrown if you try to perform an invalid conversion, such as when calling {@code ObjectMap.getInt(...)} on a non-numeric <code>String</code>.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class InvalidDataConversionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param toType Attempting to convert to this class type.
	 * @param cause The cause.
	 * @param value The value being converted.
	 */
	public InvalidDataConversionException(Object value, Class<?> toType, Exception cause) {
		super(MessageFormat.format("Invalid data conversion from type ''{0}'' to type ''{1}''.  Value=''{2}''.", value == null ? null : value.getClass().getName(), toType.getName(), value), cause);
	}

}
