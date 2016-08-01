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
 * Identifies a setter as a method for setting the name of a POJO as it's known by
 * its parent object.
 * <p>
 * For example, the {@link Section} class must know the name it's known by it's parent
 * {@link ConfigFileImpl} class, so parsers will call this method with the sectio name
 * using the {@link Section#setName(String)} method.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Target({METHOD})
@Retention(RUNTIME)
@Inherited
public @interface NameProperty {}
