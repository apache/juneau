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
package org.apache.juneau.urlencoding;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.urlencoding.annotation.*; 

/**
 * Metadata on classes specific to the URL-Encoding serializers and parsers pulled from the {@link UrlEncoding @UrlEncoding} annotation on the class.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class UrlEncodingClassMeta extends ClassMetaExtended {

	private final UrlEncoding urlEncoding;
	private final boolean expandedParams;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 */
	public UrlEncodingClassMeta(ClassMeta<?> cm) {
		super(cm);
		this.urlEncoding = ReflectionUtils.getAnnotation(UrlEncoding.class, getInnerClass());
		if (urlEncoding != null) {
			expandedParams = urlEncoding.expandedParams();
		} else {
			expandedParams = false;
		}
	}

	/**
	 * Returns the {@link UrlEncoding} annotation defined on the class.
	 *
	 * @return The value of the {@link UrlEncoding} annotation, or <jk>null</jk> if annotation is not specified.
	 */
	protected UrlEncoding getAnnotation() {
		return urlEncoding;
	}

	/**
	 * Returns the {@link UrlEncoding#expandedParams()} annotation defined on the class.
	 *
	 * @return The value of the {@link UrlEncoding#expandedParams()} annotation.
	 */
	protected boolean isExpandedParams() {
		return expandedParams;
	}
}
