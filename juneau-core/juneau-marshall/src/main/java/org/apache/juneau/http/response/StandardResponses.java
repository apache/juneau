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
package org.apache.juneau.http.response;

/**
 * Standard predefined HTTP responses.
 */
public class StandardResponses {

	/**
	 * A synonym for {@link Accepted#INSTANCE}.
	 */
	public static final Accepted ACCEPTED = Accepted.INSTANCE;

	/**
	 * A synonym for {@link AlreadyReported#INSTANCE}.
	 */
	public static final AlreadyReported ALREADY_REPORTED = AlreadyReported.INSTANCE;

	/**
	 * A synonym for {@link Continue#INSTANCE}.
	 */
	public static final Continue CONTINUE = Continue.INSTANCE;

	/**
	 * A synonym for {@link Created#INSTANCE}.
	 */
	public static final Created CREATED = Created.INSTANCE;

	/**
	 * A synonym for {@link EarlyHints#INSTANCE}.
	 */
	public static final EarlyHints EARLY_HINTS = EarlyHints.INSTANCE;

	/**
	 * A synonym for {@link Found#INSTANCE}.
	 */
	public static final Found FOUND = Found.INSTANCE;

	/**
	 * A synonym for {@link IMUsed#INSTANCE}.
	 */
	public static final IMUsed IM_USED = IMUsed.INSTANCE;

	/**
	 * A synonym for {@link MovedPermanently#INSTANCE}.
	 */
	public static final MovedPermanently MOVED_PERMANENTLY = MovedPermanently.INSTANCE;

	/**
	 * A synonym for {@link MultipleChoices#INSTANCE}.
	 */
	public static final MultipleChoices MULTIPLE_CHOICES = MultipleChoices.INSTANCE;

	/**
	 * A synonym for {@link MultiStatus#INSTANCE}.
	 */
	public static final MultiStatus MULTI_STATUS = MultiStatus.INSTANCE;

	/**
	 * A synonym for {@link NoContent#INSTANCE}.
	 */
	public static final NoContent NO_CONTENT = NoContent.INSTANCE;

	/**
	 * A synonym for {@link NonAuthoritiveInformation#INSTANCE}.
	 */
	public static final NonAuthoritiveInformation NON_AUTHORATIVE_INFORMATION = NonAuthoritiveInformation.INSTANCE;

	/**
	 * A synonym for {@link NotModified#INSTANCE}.
	 */
	public static final NotModified NOT_MODIFIED = NotModified.INSTANCE;

	/**
	 * A synonym for {@link Ok#INSTANCE}.
	 */
	public static final Ok OK = Ok.INSTANCE;

	/**
	 * A synonym for {@link PartialContent#INSTANCE}.
	 */
	public static final PartialContent PARTIAL_CONTENT = PartialContent.INSTANCE;

	/**
	 * A synonym for {@link PermanentRedirect#INSTANCE}.
	 */
	public static final PermanentRedirect PERMANENT_REDIRECT = PermanentRedirect.INSTANCE;

	/**
	 * A synonym for {@link Processing#INSTANCE}.
	 */
	public static final Processing PROCESSING = Processing.INSTANCE;

	/**
	 * A synonym for {@link ResetContent#INSTANCE}.
	 */
	public static final ResetContent RESET_CONTENT = ResetContent.INSTANCE;

	/**
	 * A synonym for {@link SeeOther#INSTANCE}.
	 */
	public static final SeeOther SEE_OTHER = SeeOther.INSTANCE;

	/**
	 * A synonym for {@link SwitchingProtocols#INSTANCE}.
	 */
	public static final SwitchingProtocols SWITCHING_PROTOCOLS = SwitchingProtocols.INSTANCE;

	/**
	 * A synonym for {@link TemporaryRedirect#INSTANCE}.
	 */
	public static final TemporaryRedirect TEMPORARY_REDIRECT = TemporaryRedirect.INSTANCE;

	/**
	 * A synonym for {@link UseProxy#INSTANCE}.
	 */
	public static final UseProxy USE_PROXY = UseProxy.INSTANCE;

	/**
	 * A shortcut for calling {@link Accepted#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Accepted> accepted() {
		return Accepted.create();
	}

	/**
	 * A shortcut for calling {@link AlreadyReported#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<AlreadyReported> alreadyReported() {
		return AlreadyReported.create();
	}

	/**
	 * A shortcut for calling {@link Continue#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Continue> _continue() {
		return Continue.create();
	}

	/**
	 * A shortcut for calling {@link Created#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Created> created() {
		return Created.create();
	}

	/**
	 * A shortcut for calling {@link EarlyHints#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<EarlyHints> earlyHints() {
		return EarlyHints.create();
	}

	/**
	 * A shortcut for calling {@link Found#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Found> found(String location) {
		return Found.create().location(location);
	}

	/**
	 * A shortcut for calling {@link IMUsed#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<IMUsed> imUsed() {
		return IMUsed.create();
	}

	/**
	 * A shortcut for calling {@link MovedPermanently#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<MovedPermanently> movedPermanently(String location) {
		return MovedPermanently.create().location(location);
	}

	/**
	 * A shortcut for calling {@link MovedPermanently#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<MultipleChoices> multipleChoices() {
		return MultipleChoices.create();
	}

	/**
	 * A shortcut for calling {@link MultiStatus#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<MultiStatus> multiStatus() {
		return MultiStatus.create();
	}

	/**
	 * A shortcut for calling {@link NoContent#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<NoContent> noContent() {
		return NoContent.create();
	}

	/**
	 * A shortcut for calling {@link NonAuthoritiveInformation#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<NonAuthoritiveInformation> nonAuthoritiveInformation() {
		return NonAuthoritiveInformation.create();
	}

	/**
	 * A shortcut for calling {@link NotModified#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<NotModified> notModified() {
		return NotModified.create();
	}

	/**
	 * A shortcut for calling {@link Ok#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Ok> ok() {
		return Ok.create();
	}

	/**
	 * A shortcut for calling {@link PartialContent#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<PartialContent> partialContent() {
		return PartialContent.create();
	}

	/**
	 * A shortcut for calling {@link PermanentRedirect#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<PermanentRedirect> permanentRedirect(String location) {
		return PermanentRedirect.create().location(location);
	}

	/**
	 * A shortcut for calling {@link Processing#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<Processing> processing() {
		return Processing.create();
	}

	/**
	 * A shortcut for calling {@link ResetContent#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<ResetContent> resetContent() {
		return ResetContent.create();
	}

	/**
	 * A shortcut for calling {@link SeeOther#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<SeeOther> seeOther(String location) {
		return SeeOther.create().location(location);
	}

	/**
	 * A shortcut for calling {@link SwitchingProtocols#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<SwitchingProtocols> switchingProtocols() {
		return SwitchingProtocols.create();
	}

	/**
	 * A shortcut for calling {@link TemporaryRedirect#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<TemporaryRedirect> temporaryRedirect(String location) {
		return TemporaryRedirect.create().location(location);
	}

	/**
	 * A shortcut for calling {@link UseProxy#create()}.
	 *
	 * @return A new bean.
	 */
	public static HttpResponseBuilder<UseProxy> useProxy() {
		return UseProxy.create();
	}
}
