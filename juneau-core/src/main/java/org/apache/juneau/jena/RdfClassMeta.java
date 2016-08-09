/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.jena;

import java.util.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.*;

/**
 * Metadata on classes specific to the RDF serializers and parsers pulled from the {@link Rdf @Rdf} annotation on the class.
 *
 * @author James Bognar (james.bognar@salesforce.com)
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
