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
package org.apache.juneau.markdown.annotation;

import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link Markdown @Markdown} annotation.
 *
 */
public class MarkdownAnnotation {

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Markdown {
		Object() {
			super(new AnnotationObject.Builder(Markdown.class));
		}

		@Override
		public String[] description() { return new String[0]; }

		@Override
		public MarkdownFormat format() { return MarkdownFormat.DEFAULT; }

		@Override
		public String heading() { return ""; }

		@Override
		public boolean noTables() { return false; }

		@Override
		public boolean noHeaders() { return false; }

		@Override
		public boolean code() { return false; }

		@Override
		public String link() { return ""; }
	}

	/** Default value */
	public static final Markdown DEFAULT = new Object();
}
