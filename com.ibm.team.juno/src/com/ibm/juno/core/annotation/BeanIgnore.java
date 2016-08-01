/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Ignore classes, fields, and methods from being interpreted as bean or bean components.
 * <p>
 * 	Applied to classes that may look like beans, but you want to be treated as non-beans.
 * 	For example, if you want to force a bean to be converted to a string using the <code>toString()</code>
 * 		method, use this annoation on the class.
 * <p>
 * 	Applies to fields that should not be interpreted as bean property fields.
 * <p>
 * 	Applies to getters or setters that should not be interpreted as bean property getters or setters.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target({FIELD,METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface BeanIgnore {}

