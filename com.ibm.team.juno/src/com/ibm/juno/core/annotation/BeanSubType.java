/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.annotation;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Maps a bean subclass with a string identifier.
 * <p>
 * 	Used in conjunction with {@link Bean#subTypes()} for defining mappings of bean subclasses with string identifiers.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target({})
@Retention(RUNTIME)
@Inherited
public @interface BeanSubType {

	/**
	 * The bean subclass.
	 * <p>
	 * Must be a subclass or subinterface of the parent bean.
	 */
	Class<?> type();

	/**
	 * A string identifier for this subtype.
	 * <p>
	 * This identifier is used in conjunction with the {@link Bean#subTypeProperty()} during serialization
	 * 	to create a <code>{subType:<js>'id'</js>}</code> property on the serialized object.
	 */
	String id();
}