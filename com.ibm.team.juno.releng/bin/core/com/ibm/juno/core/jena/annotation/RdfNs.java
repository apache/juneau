/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jena.annotation;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Namespace name/URL mapping pair.
 * <p>
 * 	Used to identify a namespace/URI pair on a {@link RdfSchema#rdfNs()} annotation.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target({})
@Retention(RUNTIME)
@Inherited
public @interface RdfNs {

	/**
	 * RDF namespace prefix.
	 */
	String prefix();

	/**
	 * RDF namespace URL.
	 */
	String namespaceURI();
}
