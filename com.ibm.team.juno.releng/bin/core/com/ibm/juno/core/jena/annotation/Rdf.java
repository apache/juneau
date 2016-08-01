/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jena.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import com.ibm.juno.core.jena.*;

/**
 * Annotation for specifying options for RDF serializers.
 * <p>
 * 	Can be applied to Java packages, types, fields, and methods.
 * <p>
 * 	Can be used for the following:
 * <ul>
 * 	<li>Override the default behavior of how collections and arrays are serialized.
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target({PACKAGE,TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Rdf {

	/**
	 * Sets the XML prefix of this property or class.
	 * <p>
	 * 	Must either be matched to a {@link #namespace()} annotation on the same object, parent object, or a {@link RdfNs} with the same name
	 * 	through the {@link RdfSchema#rdfNs()} annotation on the package.
	 * </p>
	 */
	String prefix() default "";

	/**
	 * Sets the namespace URI of this property or class.
	 * <p>
	 * 	Must be matched with a {@link #prefix()} annotation on this object, a parent object, or a {@link RdfNs} with the same name
	 * 	through the {@link RdfSchema#rdfNs()} annotation on the package.
	 */
	String namespace() default "";

	/**
	 * The format for how collections (e.g. lists and arrays) are serialized in RDF.
	 * @see RdfCollectionFormat
	 */
	RdfCollectionFormat collectionFormat() default RdfCollectionFormat.DEFAULT;
}
