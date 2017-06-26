// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.jena;

import static org.apache.juneau.jena.RdfCollectionFormat.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.*;

/**
 * Metadata on bean properties specific to the RDF serializers and parsers pulled from the {@link Rdf @Rdf} annotation 
 * on the bean property.
 */
public class RdfBeanPropertyMeta extends BeanPropertyMetaExtended {

	private RdfCollectionFormat collectionFormat = DEFAULT;
	private Namespace namespace = null;
	private boolean isBeanUri;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 */
	public RdfBeanPropertyMeta(BeanPropertyMeta bpm) {
		super(bpm);

		List<Rdf> rdfs = bpm.findAnnotations(Rdf.class);
		List<RdfSchema> schemas = bpm.findAnnotations(RdfSchema.class);

		for (Rdf rdf : rdfs) {
			if (collectionFormat == DEFAULT)
				collectionFormat = rdf.collectionFormat();
			if (rdf.beanUri())
				isBeanUri = true;
		}

		namespace = RdfUtils.findNamespace(rdfs, schemas);
	}

	/**
	 * Returns the RDF collection format of this property from the {@link Rdf#collectionFormat} annotation on this bean 
	 * property.
	 *
	 * @return The RDF collection format, or {@link RdfCollectionFormat#DEFAULT} if annotation not specified.
	 */
	protected RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Returns the RDF namespace associated with this bean property.
	 * <p>
	 * Namespace is determined in the following order:
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
	 * @return The namespace associated with this bean property, or <jk>null</jk> if no namespace is associated with it.
	 */
	public Namespace getNamespace() {
		return namespace;
	}

	/**
	 * Returns <jk>true</jk> if this bean property is marked with {@link Rdf#beanUri()} as <jk>true</jk>.
	 *
	 * @return <jk>true</jk> if this bean property is marked with {@link Rdf#beanUri()} as <jk>true</jk>.
	 */
	public boolean isBeanUri() {
		return isBeanUri;
	}
}
