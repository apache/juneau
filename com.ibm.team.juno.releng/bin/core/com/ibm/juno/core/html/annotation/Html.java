/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import com.ibm.juno.core.html.*;

/**
 * Annotation that can be applied to classes, fields, and methods to tweak how
 * they are handled by {@link HtmlSerializer}.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target({TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Html {

	/**
	 * Treat as XML.
	 * Useful when creating beans that model HTML elements.
	 */
	boolean asXml() default false;

	/**
	 * Treat as plain text.
	 * Object is serialized to a String using the <code>toString()</code> method and written directly to output.
	 * Useful when you want to serialize custom HTML.
	 */
	boolean asPlainText() default false;

	/**
	 * When <jk>true</jk>, collections of beans should be rendered as trees instead of tables.
	 * Default is <jk>false</jk>.
	 */
	boolean noTables() default false;

	/**
	 * When <jk>true</jk>, don't add headers to tables.
	 * Default is <jk>false</jk>.
	 */
	boolean noTableHeaders() default false;
}
