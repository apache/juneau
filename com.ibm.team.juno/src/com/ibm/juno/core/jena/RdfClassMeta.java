/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jena;

import java.util.*;

import com.ibm.juno.core.jena.annotation.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.*;

/**
 * Metadata on classes specific to the RDF serializers and parsers pulled from the {@link Rdf @Rdf} annotation on the class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class RdfClassMeta {

	private final Rdf rdf;
	private final RdfCollectionFormat collectionFormat;
	private final Namespace namespace;

	/**
	 * Constructor.
	 *
	 * @param c The class that this annotation is defined on.
	 */
	public RdfClassMeta(Class<?> c) {
		this.rdf = ReflectionUtils.getAnnotation(Rdf.class, c);
		if (rdf != null) {
			collectionFormat = rdf.collectionFormat();
		} else {
			collectionFormat = RdfCollectionFormat.DEFAULT;
		}
		List<Rdf> rdfs = ReflectionUtils.findAnnotations(Rdf.class, c);
		List<RdfSchema> schemas = ReflectionUtils.findAnnotations(RdfSchema.class, c);
		this.namespace = RdfUtils.findNamespace(rdfs, schemas);
	}

	/**
	 * Returns the {@link Rdf} annotation defined on the class.
	 *
	 * @return The value of the {@link Rdf} annotation, or <jk>null</jk> if annotation is not specified.
	 */
	protected Rdf getAnnotation() {
		return rdf;
	}

	/**
	 * Returns the {@link Rdf#collectionFormat()} annotation defined on the class.
	 *
	 * @return The value of the {@link Rdf#collectionFormat()} annotation, or <jk>null</jk> if annotation is not specified.
	 */
	protected RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Returns the RDF namespace associated with this class.
	 * <p>
	 * 	Namespace is determined in the following order:
	 * <ol>
	 * 	<li>{@link Rdf#prefix()} annotation defined on class.
	 * 	<li>{@link Rdf#prefix()} annotation defined on package.
	 * 	<li>{@link Rdf#prefix()} annotation defined on superclasses.
	 * 	<li>{@link Rdf#prefix()} annotation defined on superclass packages.
	 * 	<li>{@link Rdf#prefix()} annotation defined on interfaces.
	 * 	<li>{@link Rdf#prefix()} annotation defined on interface packages.
	 * </ol>
	 *
	 * @return The namespace associated with this class, or <jk>null</jk> if no namespace is
	 * 	associated with it.
	 */
	protected Namespace getNamespace() {
		return namespace;
	}
}
