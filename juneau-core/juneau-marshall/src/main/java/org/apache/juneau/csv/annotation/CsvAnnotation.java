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
package org.apache.juneau.csv.annotation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link Csv @Csv} annotation.
 *
 */
public class CsvAnnotation {

	private CsvAnnotation() {}

	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};

		protected Builder() {
			super(Csv.class);
		}

		public Csv build() {
			return new Object(this);
		}

		public Builder description(String...value) {
			description = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Csv {

		private final String[] description;

		Object(CsvAnnotation.Builder b) {
			super(b);
			this.description = copyOf(b.description);
		}

		@Override
		public String[] description() {
			return description;
		}
	}

	public static final Csv DEFAULT = create().build();

	public static Builder create() {
		return new Builder();
	}
}
