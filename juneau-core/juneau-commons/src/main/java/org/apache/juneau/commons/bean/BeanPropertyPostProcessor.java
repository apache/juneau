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
package org.apache.juneau.commons.bean;

/**
 * Bean-modeling SPI seam that applies marshalling-side post-processing to a {@code BeanPropertyMeta.Builder}
 * after {@code BeanPropertyMeta.Builder#validate()} succeeds.
 *
 * <p>
 * On the marshalling-side path, post-processing reads {@code @MarshalledProp} / {@code @Swap} / {@code @Uri}
 * annotations and installs swap-aware read/write transforms on the builder.  The bean-modeling layer in
 * {@code commons.bean} never touches those marshalling-side concerns directly; it just invokes this hook.
 *
 * <p>
 * The default {@link #NOOP} implementation does nothing and is wired on the commons-side path.
 *
 * <p>
 * The {@code builder} parameter is typed as {@link Object} during the in-flight TODO-5 move of
 * {@code BeanPropertyMeta} into {@code commons.bean}: until that physical move completes, the SPI must
 * not reference the marshalling-side {@code BeanPropertyMeta.Builder} from {@code commons.bean}.  After
 * the move, the parameter can be retyped.
 *
 * @see BeanConfigContext.Builder#beanPropertyPostProcessor(BeanPropertyPostProcessor)
 */
@FunctionalInterface
public interface BeanPropertyPostProcessor {

	/** Singleton commons-side default — no marshalling-side post-processing. */
	BeanPropertyPostProcessor NOOP = (marshallingContext, builder) -> {};

	/**
	 * Applies marshalling-side post-processing to the supplied builder.
	 *
	 * @param marshallingContext The marshalling-side context (as an opaque {@link Object}).  May be <jk>null</jk>
	 *	when invoked from the commons-side construction path.
	 * @param builder The bean-property builder to mutate (a {@code BeanPropertyMeta.Builder}).  Must not be
	 *	<jk>null</jk>.  Typed as {@link Object} until {@code BeanPropertyMeta} physically moves into
	 *	{@code commons.bean}; the marshalling-side implementation casts internally.
	 */
	void process(Object marshallingContext, Object builder);
}
