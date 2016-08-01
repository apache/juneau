/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.annotation;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.*;

/**
 * Identifies a class as being thread-safe.
 * <p>
 * Used for documentation purposes only.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(TYPE)
@Inherited
public @interface ThreadSafe {}
