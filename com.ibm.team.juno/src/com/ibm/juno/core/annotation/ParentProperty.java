/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import com.ibm.juno.core.ini.*;

/**
 * Identifies a setter as a method for adding a parent reference to a child object.
 * <p>
 * Used by the parsers to add references to parent objects in child objects.
 * For example, the {@link Section} class cannot exist outside the scope of a parent
 * {@link ConfigFileImpl} class, so parsers will add a reference to the config file
 * using the {@link Section#setParent(ConfigFileImpl)} method.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Target({METHOD})
@Retention(RUNTIME)
@Inherited
public @interface ParentProperty {}
