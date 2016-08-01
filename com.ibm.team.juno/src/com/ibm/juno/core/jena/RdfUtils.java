/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.jena;

import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.jena.annotation.*;
import com.ibm.juno.core.xml.*;

/**
 * Utility classes.
 */
public class RdfUtils {

	/**
	 * Find the namespace given a list of <ja>@Rdf</ja> and <ja>@RdfSchema</ja> annotations.
	 * The annotations should be a child-to-parent ordering of annotations found on
	 * 	a class or method.
	 *
	 * @param rdfs The <code>@Rdf</code> annotations to search.
	 * @param schemas The list of known RDF schemas.
	 * @return The resolved namespace, or <jk>null</jk> if the namespace could not be resolved.
	 */
	public static Namespace findNamespace(List<Rdf> rdfs, List<RdfSchema> schemas) {

		for (Rdf rdf : rdfs) {
			Namespace ns = findNamespace(rdf.prefix(), rdf.namespace(), rdfs, schemas);
			if (ns != null)
				return ns;
		}

		for (RdfSchema schema : schemas) {
			Namespace ns = findNamespace(schema.prefix(), schema.namespace(), null, schemas);
			if (ns != null)
				return ns;
		}

		return null;
	}

	private static Namespace findNamespace(String prefix, String ns, List<Rdf> rdfs, List<RdfSchema> schemas) {

		// If both prefix and namespace specified, use that Namespace mapping.
		if (! (prefix.isEmpty() || ns.isEmpty()))
			return NamespaceFactory.get(prefix, ns);

		// If only prefix specified, need to search for namespaceURI.
		if (! prefix.isEmpty()) {
			if (rdfs != null)
				for (Rdf rdf2 : rdfs)
					if (rdf2.prefix().equals(prefix) && ! rdf2.namespace().isEmpty())
						return NamespaceFactory.get(prefix, rdf2.namespace());
			for (RdfSchema schema : schemas) {
				if (schema.prefix().equals(prefix) && ! schema.namespace().isEmpty())
					return NamespaceFactory.get(prefix, schema.namespace());
				for (RdfNs rdfNs : schema.rdfNs())
					if (rdfNs.prefix().equals(prefix))
						return NamespaceFactory.get(prefix, rdfNs.namespaceURI());
			}
			throw new BeanRuntimeException("Found @Rdf.prefix annotation with no matching URI.  prefix='"+prefix+"'");
		}

		// If only namespaceURI specified, need to search for prefix.
		if (! ns.isEmpty()) {
			if (rdfs != null)
				for (Rdf rdf2 : rdfs)
					if (rdf2.namespace().equals(ns) && ! rdf2.prefix().isEmpty())
						return NamespaceFactory.get(rdf2.prefix(), ns);
			for (RdfSchema schema : schemas) {
				if (schema.namespace().equals(ns) && ! schema.prefix().isEmpty())
					return NamespaceFactory.get(schema.prefix(), ns);
				for (RdfNs rdfNs : schema.rdfNs())
					if (rdfNs.namespaceURI().equals(ns))
						return NamespaceFactory.get(rdfNs.prefix(), ns);
			}
		}

		return null;
	}
}
