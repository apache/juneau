/*******************************************************************************
 * Licensed Materials - Property of IBM
 * � Copyright IBM Corporation 2014, 2015. All Rights Reserved.
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
 * Identifies services whose Java class or methods can be invoked remotely.
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Remoteable {}
