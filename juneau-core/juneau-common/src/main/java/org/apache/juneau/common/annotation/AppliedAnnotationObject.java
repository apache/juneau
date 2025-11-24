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
	 *
	 * @param <B> The actual builder class.
	 */
	public static class Builder<B> extends AnnotationObject.Builder<B> {

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
		public B on(String...values) {
			for (var v : values)
				on = addAll(on, v);
			return asThis();
		}
	}

	/**
	 * Builder for applied annotations targeting classes.
	 *
	 * @param <B> The actual builder class.
	 */
	public static class BuilderT<B> extends Builder<B> {

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
		public B on(Class<?>...value) {
			for (var v : value)
				on = addAll(on, v.getName());
			return asThis();
		}

		/**
		 * Appends the classes that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public B onClass(Class<?>...value) {
			for (var v : value)
				onClass = addAll(onClass, v);
			return asThis();
		}
	}

	/**
	 * Builder for applied annotations targeting methods.
	 *
	 * @param <B> The actual builder class.
	 */
	public static class BuilderM<B> extends Builder<B> {

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
		public B on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return asThis();
		}
	}

	/**
	 * Builder for applied annotations targeting constructors.
	 *
	 * @param <B> The actual builder class.
	 */
	public static class BuilderC<B> extends Builder<B> {

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
		public B on(Constructor<?>...value) {
			for (var v : value)
				on(info(v).getFullName());
			return asThis();
		}
	}

	/**
	 * Builder for applied annotations targeting methods and fields.
	 *
	 * @param <B> The actual builder class.
	 */
	public static class BuilderMF<B> extends Builder<B> {

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
		public B on(Field...value) {
			for (var v : value)
				on(info(v).getFullName());
			return asThis();
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public B on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return asThis();
		}
	}

	/**
	 * Builder for applied annotations targeting classes and methods.
	 *
	 * @param <B> The actual builder class.
	 */
	public static class BuilderTM<B> extends BuilderT<B> {

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
		public B on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return asThis();
		}
	}

	/**
	 * Builder for applied annotations targeting classes, methods, and fields.
	 *
	 * @param <B> The actual builder class.
	 */
	public static class BuilderTMF<B> extends BuilderT<B> {

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
		public B on(Field...value) {
			for (var v : value)
				on(info(v).getFullName());
			return asThis();
		}

		/**
		 * Appends the methods that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public B on(Method...value) {
			for (var v : value)
				on(info(v).getFullName());
			return asThis();
		}
	}

	/**
	 * Builder for applied annotations targeting methods, fields, and constructors.
	 */
	public static class BuilderMFC extends BuilderMF<BuilderMFC> {

		/**
		 * Constructor.
		 *
		 * @param annotationType The annotation type of the annotation implementation class.
		 */
		public BuilderMFC(Class<? extends Annotation> annotationType) {
			super(annotationType);
		}

		/**
		 * Appends the constructors that this annotation applies to.
		 *
		 * @param value The values to append.
		 * @return This object.
		 */
		public BuilderMFC on(Constructor<?>...value) {
			for (var v : value)
				on(info(v).getFullName());
			return this;
		}
	}

	/**
	 * Builder for applied annotations targeting classes, methods, fields, and constructors.
	 *
	 * @param <B> The actual builder class.
	 */
	public static class BuilderTMFC<B> extends BuilderTMF<B> {

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
		public B on(Constructor<?>...value) {
			for (var v : value)
				on(info(v).getFullName());
			return asThis();
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
	public AppliedAnnotationObject(Builder<?> b) {
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
