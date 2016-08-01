/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.annotation;

/**
 * Predefined string constants for the {@link Var#category()} annotation.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class VarCategory {

	/** Constant: 'attr'*/
	public static final String ATTR = "attr";

	/** Constant: 'param'*/
	public static final String PARAM = "param";

	/** Constant: 'content'*/
	public static final String CONTENT = "content";

	/** Constant: 'header'*/
	public static final String HEADER = "header";

	/** Constant: 'other'*/
	public static final String OTHER = "other";
}
