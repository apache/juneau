/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.urlencoding.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import com.ibm.juno.core.urlencoding.*;

/**
 * Annotation that can be applied to classes, fields, and methods to tweak how
 * they are handled by {@link UrlEncodingSerializer} and {@link UrlEncodingParser}.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Inherited
public @interface UrlEncoding {

	/**
	 * When true, bean properties of type array or Collection will be expanded into multiple key=value pairings.
	 * <p>
	 * This annotation is identical in behavior to using the {@link UrlEncodingProperties#URLENC_expandedParams} 
	 * property, but applies to only instances of this bean.  
	 */
	boolean expandedParams() default false;
}
