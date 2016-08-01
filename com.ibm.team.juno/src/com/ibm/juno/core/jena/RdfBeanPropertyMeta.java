/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jena;

import static com.ibm.juno.core.jena.RdfCollectionFormat.*;

import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.jena.annotation.*;
import com.ibm.juno.core.xml.*;

/**
 * Metadata on bean properties specific to the RDF serializers and parsers pulled from the {@link Rdf @Rdf} annotation on the bean property.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The bean class.
 */
public class RdfBeanPropertyMeta<T> {

	private RdfCollectionFormat collectionFormat = DEFAULT;
	private Namespace namespace = null;

	/**
	 * Constructor.
	 *
	 * @param bpMeta The metadata of the bean property of this additional metadata.
	 */
	public RdfBeanPropertyMeta(BeanPropertyMeta<T> bpMeta) {

		List<Rdf> rdfs = bpMeta.findAnnotations(Rdf.class);
		List<RdfSchema> schemas = bpMeta.findAnnotations(RdfSchema.class);

		for (Rdf rdf : rdfs)
			if (collectionFormat == DEFAULT)
				collectionFormat = rdf.collectionFormat();

		namespace = RdfUtils.findNamespace(rdfs, schemas);
	}

	/**
	 * Returns the RDF collection format of this property from the {@link Rdf#collectionFormat} annotation on this bean property.
	 *
	 * @return The RDF collection format, or {@link RdfCollectionFormat#DEFAULT} if annotation not specified.
	 */
	protected RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Returns the RDF namespace associated with this bean property.
	 * <p>
	 * 	Namespace is determined in the following order:
	 * <ol>
	 * 	<li>{@link Rdf#prefix()} annotation defined on bean property field.
	 * 	<li>{@link Rdf#prefix()} annotation defined on bean getter.
	 * 	<li>{@link Rdf#prefix()} annotation defined on bean setter.
	 * 	<li>{@link Rdf#prefix()} annotation defined on bean.
	 * 	<li>{@link Rdf#prefix()} annotation defined on bean package.
	 * 	<li>{@link Rdf#prefix()} annotation defined on bean superclasses.
	 * 	<li>{@link Rdf#prefix()} annotation defined on bean superclass packages.
	 * 	<li>{@link Rdf#prefix()} annotation defined on bean interfaces.
	 * 	<li>{@link Rdf#prefix()} annotation defined on bean interface packages.
	 * </ol>
	 *
	 * @return The namespace associated with this bean property, or <jk>null</jk> if no namespace is
	 * 	associated with it.
	 */
	public Namespace getNamespace() {
		return namespace;
	}
}
