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
package org.apache.juneau.soap.annotation;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Builder class for the {@link SoapXml} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class SoapXmlBuilder extends TargetedAnnotationTMFBuilder {

	/** Default value */
	public static final SoapXml DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static SoapXmlBuilder create() {
		return new SoapXmlBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static SoapXmlBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static SoapXmlBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements SoapXml {

		Impl(SoapXmlBuilder b) {
			super(b);
			postConstruct();
		}
	}


	/**
	 * Constructor.
	 */
	public SoapXmlBuilder() {
		super(SoapXml.class);
	}

	/**
	 * Instantiates a new {@link SoapXml @SoapXml} object initialized with this builder.
	 *
	 * @return A new {@link SoapXml @SoapXml} object.
	 */
	public SoapXml build() {
		return new Impl(this);
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public SoapXmlBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public SoapXmlBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public SoapXmlBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public SoapXmlBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public SoapXmlBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
