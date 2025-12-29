/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.jena;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.*;

/**
 * Utility classes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}

 * </ul>
 */
public class RdfUtils {

	/**
	 * Find the namespace given a list of <ja>@Rdf</ja> and <ja>@RdfSchema</ja> annotations.
	 *
	 * <p>
	 * The annotations should be a child-to-parent ordering of annotations found on a class or method.
	 *
	 * @param rdfs The <c>@Rdf</c> annotations to search.
	 * @param schemas The list of known RDF schemas.
	 * @return The resolved namespace, or <jk>null</jk> if the namespace could not be resolved.
	 */
	public static Namespace findNamespace(List<Rdf> rdfs, List<RdfSchema> schemas) {

		for (var rdf : rdfs) {
			var ns = findNamespace(rdf.prefix(), rdf.namespace(), rdfs, schemas);
			if (nn(ns))
				return ns;
		}

		for (var schema : schemas) {
			var ns = findNamespace(schema.prefix(), schema.namespace(), null, schemas);
			if (nn(ns))
				return ns;
		}

		return null;
	}

	private static Namespace findNamespace(String prefix, String ns, List<Rdf> rdfs, List<RdfSchema> schemas) {

		// If both prefix and namespace specified, use that Namespace mapping.
		if (! (prefix.isEmpty() || ns.isEmpty()))
			return Namespace.of(prefix, ns);

		// If only prefix specified, need to search for namespaceURI.
		if (! prefix.isEmpty()) {
			if (nn(rdfs))
				for (var rdf2 : rdfs)
					if (rdf2.prefix().equals(prefix) && ! rdf2.namespace().isEmpty())
						return Namespace.of(prefix, rdf2.namespace());
			for (var schema : schemas) {
				if (schema.prefix().equals(prefix) && ! schema.namespace().isEmpty())
					return Namespace.of(prefix, schema.namespace());
				for (var rdfNs : schema.rdfNs())
					if (rdfNs.prefix().equals(prefix))
						return Namespace.of(prefix, rdfNs.namespaceURI());
			}
			throw bex("Found @Rdf.prefix annotation with no matching URI.  prefix='" + prefix + "'");
		}

		// If only namespaceURI specified, need to search for prefix.
		if (! ns.isEmpty()) {
			if (nn(rdfs))
				for (var rdf2 : rdfs)
					if (rdf2.namespace().equals(ns) && ! rdf2.prefix().isEmpty())
						return Namespace.of(rdf2.prefix(), ns);
			for (var schema : schemas) {
				if (schema.namespace().equals(ns) && ! schema.prefix().isEmpty())
					return Namespace.of(schema.prefix(), ns);
				for (var rdfNs : schema.rdfNs())
					if (rdfNs.namespaceURI().equals(ns))
						return Namespace.of(rdfNs.prefix(), ns);
			}
		}

		return null;
	}
}