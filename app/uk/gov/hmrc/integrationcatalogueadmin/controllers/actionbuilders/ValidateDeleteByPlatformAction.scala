/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.integrationcatalogueadmin.controllers.actionbuilders

import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{ActionRefiner, Request, Result, WrappedRequest}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponse, ErrorResponseMessage}
import uk.gov.hmrc.integrationcatalogueadmin.models.HeaderKeys
import uk.gov.hmrc.integrationcatalogueadmin.utils.ValidateParameters

import scala.concurrent.{ExecutionContext, Future}

case class ValidatedDeleteByPlatformRequest[A](platform: PlatformType, request: Request[A]) extends WrappedRequest[A](request)

object ValidateDeleteByPlatformAction extends ValidateParameters {

  def ValidatePlatformTypeParams(platforms: List[PlatformType])(implicit ec: ExecutionContext): ActionRefiner[Request, ValidatedDeleteByPlatformRequest] = {
    new ActionRefiner[Request, ValidatedDeleteByPlatformRequest] {

      override def executionContext: ExecutionContext = ec

      override def refine[A](request: Request[A]): Future[Either[Result, ValidatedDeleteByPlatformRequest[A]]] = {
        val platformTypeHeader = request.headers.get(HeaderKeys.platformKey).getOrElse("")
        if ((!platforms.isEmpty) && platforms.size == 1) {

          if (platformTypeHeader.isEmpty || validatePlatformType(platformTypeHeader) == Some(platforms.head)) {
            Future.successful(Right(ValidatedDeleteByPlatformRequest(platforms.head, request)))
          } else Future.successful(Left(Unauthorized(Json.toJson(ErrorResponse(List(ErrorResponseMessage("You are not authorised to delete integrations on this Platform")))))))
        } else
          Future.successful(Left(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("platforms query parameter is either invalid, missing or multiple have been provided")))))))

      }

    }

  }
}
