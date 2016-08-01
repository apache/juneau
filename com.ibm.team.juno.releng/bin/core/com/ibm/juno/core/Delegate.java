/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

/**
 * An object that represents another object, often wrapping that object.
 * <p>
 * <b>*** Internal Interface - Not intended for external use ***</b>
 * <p>
 * 	For example, {@link BeanMap} is a map representation of a bean.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The represented class type.
 */
public interface Delegate<T> {

	/**
	 * The {@link ClassMeta} of the class of the represented object.
	 *
	 * @return The class type of the represented object.
	 */
	public ClassMeta<T> getClassMeta();
}
