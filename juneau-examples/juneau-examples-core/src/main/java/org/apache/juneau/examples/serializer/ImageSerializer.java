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
package org.apache.juneau.examples.serializer;

import java.awt.image.*;
import java.io.*;

import javax.imageio.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Example serializer that converts {@link BufferedImage} objects to byte streams.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Marshalling">REST Marshalling</a>
 * </ul>
 */
@SuppressWarnings("javadoc")
public class ImageSerializer extends OutputStreamSerializer {

	public ImageSerializer() {
		super(create().produces("image/png,image/jpeg"));
	}

	@Override
	public void doSerialize(SerializerSession session, SerializerPipe pipe, Object o) throws IOException, SerializeException {
		RenderedImage image = (RenderedImage)o;
		MediaType mediaType = session.getMediaType();
		try (OutputStream os = pipe.getOutputStream()) {
			ImageIO.write(image, mediaType.getType(), os);
		}
	}
}