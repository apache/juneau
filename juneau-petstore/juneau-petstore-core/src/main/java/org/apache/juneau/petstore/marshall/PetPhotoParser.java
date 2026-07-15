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
package org.apache.juneau.petstore.marshall;

import java.awt.image.*;
import java.io.*;

import javax.imageio.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;

/**
 * Parses {@code image/png}/{@code image/jpg} byte streams into {@link BufferedImage} pet photos.
 *
 * <p>
 * Petstore-local port of the deleted {@code juneau-examples-rest} image demo, relocated here (rather than
 * {@code juneau-examples-core}) so {@code juneau-petstore-core} stays free of any dependency on the examples
 * modules.  Wired into {@link org.apache.juneau.petstore.rest.PetStoreResource#putPetPhoto(long, BufferedImage)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
public class PetPhotoParser extends InputStreamParser {

	/**
	 * Constructor with default settings (consumes image/png and image/jpg).
	 */
	public PetPhotoParser() {
		super(create().consumes("image/png,image/jpg"));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override /* Parser */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for image return
	})
	public <T> T doParse(ParserSession session, ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException {
		try (var is = pipe.getInputStream()) {
			var image = ImageIO.read(is);
			return (T)image;
		}
	}
}
