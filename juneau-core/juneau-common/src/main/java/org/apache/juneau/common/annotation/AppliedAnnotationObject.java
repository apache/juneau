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
package org.apache.juneau.common.annotation;

import static org.apache.juneau.common.reflect.ReflectionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

/**
 * An implementation of an annotation that has an <code>on</code> value targeting classes/methods/fields/constructors.
 *
 */
public class AppliedAnnotationObject extends AnnotationObject {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder for {@link AppliedAnnotationObject} objects.
	 */
	public static class Builder extends AnnotationObject.Builder {

		String[] on = {};

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public Builder(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * The targets this annotation applies to.
		 *
		 * @param values The targets this annotation applies to.
		 * @return This object.
		 */
		public Builder on(String...values) {
			for (var v : values)
				on = addAll(on, v);
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting classes.
	 */
	public static class BuilderT extends Builder {

		Class<?>[] onClass = {};

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderT(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the classes that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderT on(Class<?>...value) {
			for (var v : value)
				on = addAll(on, v.getName());
			return this;
		}

		/**
		 * Appends the classes that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public BuilderT onClass(Class<?>...value) {
			for (var v : value)
				onClass = addAll(onClass, v);
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting methods.
	 */
	public static class BuilderM extends Builder {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderM(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderM on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting constructors.
	 */
	public static class BuilderC extends Builder {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderC(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the constructors that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderC on(Constructor<?>...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting methods and fields.
	 */
	public static class BuilderMF extends Builder {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderMF(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the fields that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderMF on(Field...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderMF on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting classes and methods.
	 */
	public static class BuilderTM extends BuilderT {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderTM(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTM on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.Builder */
		public BuilderTM on(String...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedOnClassAnnotationObject.Builder */
		public BuilderTM onClass(Class<?>...value) {
			super.onClass(value);
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting classes, methods, and fields.
	 */
	public static class BuilderTMF extends BuilderT {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderTMF(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the fields that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTMF on(Field...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTMF on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting classes, methods, fields, and constructors.
	 */
	public static class BuilderTMFC extends BuilderTMF {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderTMFC(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the constructors that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderTMFC on(Constructor<?>...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String[] on;

	/**
	 * Constructor.
	 *
	 * @param b The builder used to instantiate the fields of this class.
	 */
	public AppliedAnnotationObject(Builder b) {
		super(b);
		this.on = copyOf(b.on);
	}

	/**
	 * The targets this annotation applies to.
	 *
	 * @return The targets this annotation applies to.
	 */
	public String[] on() {
		return on;
	}
}
