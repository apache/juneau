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
package org.apache.juneau.http;

import org.apache.juneau.http.response.*;

/**
 * Standard predefined HTTP responses.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-common}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class HttpResponses {

	/**
	 * A synonym for {@link Accepted#INSTANCE}.
	 */
	public static final Accepted ACCEPTED = Accepted.INSTANCE;

	/**
	 * A synonym for {@link AlreadyReported#INSTANCE}.
	 */
	public static final AlreadyReported ALREADY_REPORTED = AlreadyReported.INSTANCE;

	/**
	 * A synonym for {@link BadRequest#INSTANCE}.
	 */
	public static final BadRequest BAD_REQUEST = BadRequest.INSTANCE;

	/**
	 * A synonym for {@link Conflict#INSTANCE}.
	 */
	public static final Conflict CONFLICT = Conflict.INSTANCE;

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
	 * A synonym for {@link ExpectationFailed#INSTANCE}.
	 */
	public static final ExpectationFailed EXPECTATION_FAILED = ExpectationFailed.INSTANCE;

	/**
	 * A synonym for {@link FailedDependency#INSTANCE}.
	 */
	public static final FailedDependency FAILED_DEPENDENCY = FailedDependency.INSTANCE;

	/**
	 * A synonym for {@link Forbidden#INSTANCE}.
	 */
	public static final Forbidden FORBIDDEN = Forbidden.INSTANCE;

	/**
	 * A synonym for {@link Found#INSTANCE}.
	 */
	public static final Found FOUND = Found.INSTANCE;

	/**
	 * A synonym for {@link Gone#INSTANCE}.
	 */
	public static final Gone GONE = Gone.INSTANCE;

	/**
	 * A synonym for {@link HttpVersionNotSupported#INSTANCE}.
	 */
	public static final HttpVersionNotSupported HTTP_VERSION_NOT_SUPPORTED = HttpVersionNotSupported.INSTANCE;

	/**
	 * A synonym for {@link IMUsed#INSTANCE}.
	 */
	public static final IMUsed IM_USED = IMUsed.INSTANCE;

	/**
	 * A synonym for {@link InsufficientStorage#INSTANCE}.
	 */
	public static final InsufficientStorage INSUFFICIENT_STORAGE = InsufficientStorage.INSTANCE;

	/**
	 * A synonym for {@link InternalServerError#INSTANCE}.
	 */
	public static final InternalServerError INTERNAL_SERVER_ERROR = InternalServerError.INSTANCE;

	/**
	 * A synonym for {@link LengthRequired#INSTANCE}.
	 */
	public static final LengthRequired LENGTH_REQUIRED = LengthRequired.INSTANCE;

	/**
	 * A synonym for {@link Locked#INSTANCE}.
	 */
	public static final Locked LOCKED = Locked.INSTANCE;

	/**
	 * A synonym for {@link LoopDetected#INSTANCE}.
	 */
	public static final LoopDetected LOOP_DETECTED = LoopDetected.INSTANCE;

	/**
	 * A synonym for {@link MethodNotAllowed#INSTANCE}.
	 */
	public static final MethodNotAllowed METHOD_NOT_ALLOWED = MethodNotAllowed.INSTANCE;

	/**
	 * A synonym for {@link MisdirectedRequest#INSTANCE}.
	 */
	public static final MisdirectedRequest MISDIRECTED_REQUEST = MisdirectedRequest.INSTANCE;

	/**
	 * A synonym for {@link MovedPermanently#INSTANCE}.
	 */
	public static final MovedPermanently MOVED_PERMANENTLY = MovedPermanently.INSTANCE;

	/**
	 * A synonym for {@link MultiStatus#INSTANCE}.
	 */
	public static final MultiStatus MULTI_STATUS = MultiStatus.INSTANCE;

	/**
	 * A synonym for {@link MultipleChoices#INSTANCE}.
	 */
	public static final MultipleChoices MULTIPLE_CHOICES = MultipleChoices.INSTANCE;

	/**
	 * A synonym for {@link NetworkAuthenticationRequired#INSTANCE}.
	 */
	public static final NetworkAuthenticationRequired NETWORK_AUTHENTICATION_REQUIRED = NetworkAuthenticationRequired.INSTANCE;

	/**
	 * A synonym for {@link NoContent#INSTANCE}.
	 */
	public static final NoContent NO_CONTENT = NoContent.INSTANCE;

	/**
	 * A synonym for {@link NonAuthoritiveInformation#INSTANCE}.
	 */
	public static final NonAuthoritiveInformation NON_AUTHORATIVE_INFORMATION = NonAuthoritiveInformation.INSTANCE;

	/**
	 * A synonym for {@link NotAcceptable#INSTANCE}.
	 */
	public static final NotAcceptable NOT_ACCEPTABLE = NotAcceptable.INSTANCE;

	/**
	 * A synonym for {@link NotExtended#INSTANCE}.
	 */
	public static final NotExtended NOT_EXTENDED = NotExtended.INSTANCE;

	/**
	 * A synonym for {@link NotFound#INSTANCE}.
	 */
	public static final NotFound NOT_FOUND = NotFound.INSTANCE;

	/**
	 * A synonym for {@link NotImplemented#INSTANCE}.
	 */
	public static final NotImplemented NOT_IMPLEMENTED = NotImplemented.INSTANCE;

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
	 * A synonym for {@link PayloadTooLarge#INSTANCE}.
	 */
	public static final PayloadTooLarge PAYLOAD_TOO_LARGE = PayloadTooLarge.INSTANCE;

	/**
	 * A synonym for {@link PermanentRedirect#INSTANCE}.
	 */
	public static final PermanentRedirect PERMANENT_REDIRECT = PermanentRedirect.INSTANCE;

	/**
	 * A synonym for {@link PreconditionFailed#INSTANCE}.
	 */
	public static final PreconditionFailed PRECONDITION_FAILED = PreconditionFailed.INSTANCE;

	/**
	 * A synonym for {@link PreconditionRequired#INSTANCE}.
	 */
	public static final PreconditionRequired PRECONDITION_REQUIRED = PreconditionRequired.INSTANCE;

	/**
	 * A synonym for {@link Processing#INSTANCE}.
	 */
	public static final Processing PROCESSING = Processing.INSTANCE;

	/**
	 * A synonym for {@link RangeNotSatisfiable#INSTANCE}.
	 */
	public static final RangeNotSatisfiable RANGE_NOT_SATISFIABLE = RangeNotSatisfiable.INSTANCE;

	/**
	 * A synonym for {@link RequestHeaderFieldsTooLarge#INSTANCE}.
	 */
	public static final RequestHeaderFieldsTooLarge REQUEST_HEADER_FIELDS_TOO_LARGE = RequestHeaderFieldsTooLarge.INSTANCE;

	/**
	 * A synonym for {@link ResetContent#INSTANCE}.
	 */
	public static final ResetContent RESET_CONTENT = ResetContent.INSTANCE;

	/**
	 * A synonym for {@link SeeOther#INSTANCE}.
	 */
	public static final SeeOther SEE_OTHER = SeeOther.INSTANCE;

	/**
	 * A synonym for {@link ServiceUnavailable#INSTANCE}.
	 */
	public static final ServiceUnavailable SERVICE_UNAVAILABLE = ServiceUnavailable.INSTANCE;

	/**
	 * A synonym for {@link SwitchingProtocols#INSTANCE}.
	 */
	public static final SwitchingProtocols SWITCHING_PROTOCOLS = SwitchingProtocols.INSTANCE;

	/**
	 * A synonym for {@link TemporaryRedirect#INSTANCE}.
	 */
	public static final TemporaryRedirect TEMPORARY_REDIRECT = TemporaryRedirect.INSTANCE;

	/**
	 * A synonym for {@link TooManyRequests#INSTANCE}.
	 */
	public static final TooManyRequests TOO_MANY_REQUESTS = TooManyRequests.INSTANCE;

	/**
	 * A synonym for {@link Unauthorized#INSTANCE}.
	 */
	public static final Unauthorized UNAUTHORIZED = Unauthorized.INSTANCE;

	/**
	 * A synonym for {@link UnavailableForLegalReasons#INSTANCE}.
	 */
	public static final UnavailableForLegalReasons UNAVAILABLE_FOR_LEGAL_REASONS = UnavailableForLegalReasons.INSTANCE;

	/**
	 * A synonym for {@link UnprocessableEntity#INSTANCE}.
	 */
	public static final UnprocessableEntity UNPROCESSABLE_ENTITIY = UnprocessableEntity.INSTANCE;

	/**
	 * A synonym for {@link UnsupportedMediaType#INSTANCE}.
	 */
	public static final UnsupportedMediaType UNSUPPORTED_MEDIA_TYPE = UnsupportedMediaType.INSTANCE;

	/**
	 * A synonym for {@link UpgradeRequired#INSTANCE}.
	 */
	public static final UpgradeRequired UPGRADE_REQUIRED = UpgradeRequired.INSTANCE;

	/**
	 * A synonym for {@link UriTooLong#INSTANCE}.
	 */
	public static final UriTooLong URI_TOO_LONG = UriTooLong.INSTANCE;

	/**
	 * A synonym for {@link UseProxy#INSTANCE}.
	 */
	public static final UseProxy USE_PROXY = UseProxy.INSTANCE;

	/**
	 * A synonym for {@link VariantAlsoNegotiates#INSTANCE}.
	 */
	public static final VariantAlsoNegotiates VARIANT_ALSO_NEGOTIATES = VariantAlsoNegotiates.INSTANCE;


	/**
	 * A shortcut for calling {@link Continue#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<Continue> _continue() {
		return Continue.create();
	}

	/**
	 * A shortcut for calling {@link Accepted#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<Accepted> accepted() {
		return Accepted.create();
	}

	/**
	 * A shortcut for calling {@link AlreadyReported#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<AlreadyReported> alreadyReported() {
		return AlreadyReported.create();
	}

	/**
	 * A shortcut for calling {@link BadRequest#BadRequest()}.
	 *
	 * @return A new bean builder.
	 */
	public static final BadRequest badRequest() {
		return new BadRequest();
	}

	/**
	 * A shortcut for calling {@link Conflict#Conflict()}.
	 *
	 * @return A new bean builder.
	 */
	public static final Conflict conflict() {
		return new Conflict();
	}

	/**
	 * A shortcut for calling {@link Created#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<Created> created() {
		return Created.create();
	}

	/**
	 * A shortcut for calling {@link EarlyHints#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<EarlyHints> earlyHints() {
		return EarlyHints.create();
	}

	/**
	 * A shortcut for calling {@link ExpectationFailed#ExpectationFailed()}.
	 *
	 * @return A new bean builder.
	 */
	public static final ExpectationFailed expectationFailed() {
		return new ExpectationFailed();
	}

	/**
	 * A shortcut for calling {@link FailedDependency#FailedDependency()}.
	 *
	 * @return A new bean builder.
	 */
	public static final FailedDependency failedDependency() {
		return new FailedDependency();
	}

	/**
	 * A shortcut for calling {@link Forbidden#Forbidden()}.
	 *
	 * @return A new bean builder.
	 */
	public static final Forbidden forbidden() {
		return new Forbidden();
	}

	/**
	 * A shortcut for calling {@link Found#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<Found> found(String location) {
		return Found.create().location(location);
	}

	/**
	 * A shortcut for calling {@link Gone#Gone()}.
	 *
	 * @return A new bean builder.
	 */
	public static final Gone gone() {
		return new Gone();
	}

	/**
	 * A shortcut for calling {@link BasicHttpException#BasicHttpException()}.
	 *
	 * @return A new bean builder.
	 */
	public static final BasicHttpException httpException() {
		return new BasicHttpException();
	}

	/**
	 * A shortcut for calling {@link HttpVersionNotSupported#HttpVersionNotSupported()}.
	 *
	 * @return A new bean builder.
	 */
	public static final HttpVersionNotSupported httpVersionNotSupported() {
		return new HttpVersionNotSupported();
	}

	/**
	 * A shortcut for calling {@link IMUsed#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<IMUsed> imUsed() {
		return IMUsed.create();
	}

	/**
	 * A shortcut for calling {@link InsufficientStorage#InsufficientStorage()}.
	 *
	 * @return A new bean builder.
	 */
	public static final InsufficientStorage insufficientStorage() {
		return new InsufficientStorage();
	}

	/**
	 * A shortcut for calling {@link InternalServerError#InternalServerError()}.
	 *
	 * @return A new bean builder.
	 */
	public static final InternalServerError internalServerError() {
		return new InternalServerError();
	}

	/**
	 * A shortcut for calling {@link LengthRequired#LengthRequired()}.
	 *
	 * @return A new bean builder.
	 */
	public static final LengthRequired lengthRequired() {
		return new LengthRequired();
	}

	/**
	 * A shortcut for calling {@link Locked#Locked()}.
	 *
	 * @return A new bean builder.
	 */
	public static final Locked locked() {
		return new Locked();
	}

	/**
	 * A shortcut for calling {@link LoopDetected#LoopDetected()}.
	 *
	 * @return A new bean builder.
	 */
	public static final LoopDetected loopDetected() {
		return new LoopDetected();
	}

	/**
	 * A shortcut for calling {@link MethodNotAllowed#MethodNotAllowed()}.
	 *
	 * @return A new bean builder.
	 */
	public static final MethodNotAllowed methodNotAllowed() {
		return new MethodNotAllowed();
	}

	/**
	 * A shortcut for calling {@link MisdirectedRequest#MisdirectedRequest()}.
	 *
	 * @return A new bean builder.
	 */
	public static final MisdirectedRequest misdirectedRequest() {
		return new MisdirectedRequest();
	}
	/**
	 * A shortcut for calling {@link MovedPermanently#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<MovedPermanently> movedPermanently(String location) {
		return MovedPermanently.create().location(location);
	}

	/**
	 * A shortcut for calling {@link MovedPermanently#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<MultipleChoices> multipleChoices() {
		return MultipleChoices.create();
	}

	/**
	 * A shortcut for calling {@link MultiStatus#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<MultiStatus> multiStatus() {
		return MultiStatus.create();
	}

	/**
	 * A shortcut for calling {@link NetworkAuthenticationRequired#NetworkAuthenticationRequired()}.
	 *
	 * @return A new bean builder.
	 */
	public static final NetworkAuthenticationRequired networkAuthenticationRequired() {
		return new NetworkAuthenticationRequired();
	}

	/**
	 * A shortcut for calling {@link NoContent#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<NoContent> noContent() {
		return NoContent.create();
	}

	/**
	 * A shortcut for calling {@link NonAuthoritiveInformation#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<NonAuthoritiveInformation> nonAuthoritiveInformation() {
		return NonAuthoritiveInformation.create();
	}

	/**
	 * A shortcut for calling {@link NotAcceptable#NotAcceptable()}.
	 *
	 * @return A new bean builder.
	 */
	public static final NotAcceptable notAcceptable() {
		return new NotAcceptable();
	}

	/**
	 * A shortcut for calling {@link NotExtended#NotExtended()}.
	 *
	 * @return A new bean builder.
	 */
	public static final NotExtended notExtended() {
		return new NotExtended();
	}

	/**
	 * A shortcut for calling {@link NotFound#NotFound()}.
	 *
	 * @return A new bean builder.
	 */
	public static final NotFound notFound() {
		return new NotFound();
	}

	/**
	 * A shortcut for calling {@link NotImplemented#NotImplemented()}.
	 *
	 * @return A new bean builder.
	 */
	public static final NotImplemented notImplemented() {
		return new NotImplemented();
	}

	/**
	 * A shortcut for calling {@link NotModified#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<NotModified> notModified() {
		return NotModified.create();
	}

	/**
	 * A shortcut for calling {@link Ok#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<Ok> ok() {
		return Ok.create();
	}

	/**
	 * A shortcut for calling {@link PartialContent#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<PartialContent> partialContent() {
		return PartialContent.create();
	}

	/**
	 * A shortcut for calling {@link PayloadTooLarge#PayloadTooLarge()}.
	 *
	 * @return A new bean builder.
	 */
	public static final PayloadTooLarge payloadTooLarge() {
		return new PayloadTooLarge();
	}

	/**
	 * A shortcut for calling {@link PermanentRedirect#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<PermanentRedirect> permanentRedirect(String location) {
		return PermanentRedirect.create().location(location);
	}

	/**
	 * A shortcut for calling {@link PreconditionFailed#PreconditionFailed()}.
	 *
	 * @return A new bean builder.
	 */
	public static final PreconditionFailed preconditionFailed() {
		return new PreconditionFailed();
	}

	/**
	 * A shortcut for calling {@link PreconditionRequired#PreconditionRequired()}.
	 *
	 * @return A new bean builder.
	 */
	public static final PreconditionRequired preconditionRequired() {
		return new PreconditionRequired();
	}

	/**
	 * A shortcut for calling {@link Processing#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<Processing> processing() {
		return Processing.create();
	}

	/**
	 * A shortcut for calling {@link RangeNotSatisfiable#RangeNotSatisfiable()}.
	 *
	 * @return A new bean builder.
	 */
	public static final RangeNotSatisfiable rangeNotSatisfiable() {
		return new RangeNotSatisfiable();
	}

	/**
	 * A shortcut for calling {@link RequestHeaderFieldsTooLarge#RequestHeaderFieldsTooLarge()}.
	 *
	 * @return A new bean builder.
	 */
	public static final RequestHeaderFieldsTooLarge requestHeaderFieldsTooLarge() {
		return new RequestHeaderFieldsTooLarge();
	}

	/**
	 * A shortcut for calling {@link ResetContent#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<ResetContent> resetContent() {
		return ResetContent.create();
	}

	/**
	 * A shortcut for calling {@link SeeOther#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<SeeOther> seeOther(String location) {
		return SeeOther.create().location(location);
	}

	/**
	 * A shortcut for calling {@link ServiceUnavailable#ServiceUnavailable()}.
	 *
	 * @return A new bean builder.
	 */
	public static final ServiceUnavailable serviceUnavailable() {
		return new ServiceUnavailable();
	}

	/**
	 * A shortcut for calling {@link SwitchingProtocols#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<SwitchingProtocols> switchingProtocols() {
		return SwitchingProtocols.create();
	}

	/**
	 * A shortcut for calling {@link TemporaryRedirect#create()}.
	 *
	 * @param location The value for the Location header.
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<TemporaryRedirect> temporaryRedirect(String location) {
		return TemporaryRedirect.create().location(location);
	}

	/**
	 * A shortcut for calling {@link TooManyRequests#TooManyRequests()}.
	 *
	 * @return A new bean builder.
	 */
	public static final TooManyRequests tooManyRequests() {
		return new TooManyRequests();
	}

	/**
	 * A shortcut for calling {@link Unauthorized#Unauthorized()}.
	 *
	 * @return A new bean builder.
	 */
	public static final Unauthorized unauthorized() {
		return new Unauthorized();
	}

	/**
	 * A shortcut for calling {@link UnavailableForLegalReasons#UnavailableForLegalReasons()}.
	 *
	 * @return A new bean builder.
	 */
	public static final UnavailableForLegalReasons unavailableForLegalReasons() {
		return new UnavailableForLegalReasons();
	}

	/**
	 * A shortcut for calling {@link UnprocessableEntity#UnprocessableEntity()}.
	 *
	 * @return A new bean builder.
	 */
	public static final UnprocessableEntity unprocessableEntity() {
		return new UnprocessableEntity();
	}

	/**
	 * A shortcut for calling {@link UnsupportedMediaType#UnsupportedMediaType()}.
	 *
	 * @return A new bean builder.
	 */
	public static final UnsupportedMediaType unsupportedMediaType() {
		return new UnsupportedMediaType();
	}

	/**
	 * A shortcut for calling {@link UpgradeRequired#UpgradeRequired()}.
	 *
	 * @return A new bean builder.
	 */
	public static final UpgradeRequired upgradeRequired() {
		return new UpgradeRequired();
	}

	/**
	 * A shortcut for calling {@link UriTooLong#UriTooLong()}.
	 *
	 * @return A new bean builder.
	 */
	public static final UriTooLong uriTooLong() {
		return new UriTooLong();
	}

	/**
	 * A shortcut for calling {@link UseProxy#create()}.
	 *
	 * @return A new bean.
	 */
	public static final HttpResponseBuilder<UseProxy> useProxy() {
		return UseProxy.create();
	}

	/**
	 * A shortcut for calling {@link VariantAlsoNegotiates#VariantAlsoNegotiates()}.
	 *
	 * @return A new bean builder.
	 */
	public static final VariantAlsoNegotiates variantAlsoNegotiates() {
		return new VariantAlsoNegotiates();
	}
}
