/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

/**
 * Exception that gets thrown when trying to modify settings on a locked {@link Lockable} object.
 * <p>
 * A locked exception indicates a programming error.
 * Certain objects that are meant for reuse, such as serializers and parsers, provide
 * the ability to lock the current settings so that they cannot be later changed.
 * This exception indicates that a setting change was attempted on a previously locked object.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class LockedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	LockedException() {
		super("Object is locked and object settings cannot be modified.");
	}
}
