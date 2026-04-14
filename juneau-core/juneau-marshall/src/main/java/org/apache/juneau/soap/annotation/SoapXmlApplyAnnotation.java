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
package org.apache.juneau.soap.annotation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link SoapXmlApply @SoapXmlApply} annotation.
 *
 */
public class SoapXmlApplyAnnotation {

	private SoapXmlApplyAnnotation() {}

	public static class Applier extends AnnotationApplier<SoapXmlApply,Context.Builder> {

		public Applier(VarResolverSession vr) {
			super(SoapXmlApply.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<SoapXmlApply> ai, Context.Builder b) {
			SoapXmlApply a = ai.inner();
			if (isEmptyArray(a.on()) && isEmptyArray(a.onClass()))
				return;
			b.annotations(a);
		}
	}

	public static class Builder extends AppliedAnnotationObject.BuilderTMF {

		SoapXml value = SoapXmlAnnotation.DEFAULT;

		protected Builder() {
			super(SoapXmlApply.class);
		}

		public Builder value(SoapXml value) {
			this.value = value;
			return this;
		}

		@Override public Builder on(String...value) { super.on(value); return this; }
		@Override public Builder on(Class<?>...value) { super.on(value); return this; }
		@Override public Builder onClass(Class<?>...value) { super.onClass(value); return this; }
		@Override public Builder on(Method...value) { super.on(value); return this; }
		@Override public Builder on(Field...value) { super.on(value); return this; }
		@Override public Builder on(ClassInfo...value) { super.on(value); return this; }
		@Override public Builder onClass(ClassInfo...value) { super.onClass(value); return this; }
		@Override public Builder on(FieldInfo...value) { super.on(value); return this; }
		@Override public Builder on(MethodInfo...value) { super.on(value); return this; }

		public SoapXmlApply build() {
			return new Object(this);
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AppliedOnClassAnnotationObject implements SoapXmlApply {

		private final SoapXml value;

		Object(SoapXmlApplyAnnotation.Builder b) {
			super(b);
			value = b.value;
		}

		@Override public SoapXml value() { return value; }
		@Override public String[] on() { return super.on(); }
		@Override public Class<?>[] onClass() { return super.onClass(); }
	}

	public static final SoapXmlApply DEFAULT = create().build();

	public static Builder create() { return new Builder(); }
	public static Builder create(Class<?>...on) { return create().on(on); }
	public static Builder create(String...on) { return create().on(on); }

	public static boolean empty(SoapXmlApply a) {
		return a == null || DEFAULT.equals(a);
	}
}
