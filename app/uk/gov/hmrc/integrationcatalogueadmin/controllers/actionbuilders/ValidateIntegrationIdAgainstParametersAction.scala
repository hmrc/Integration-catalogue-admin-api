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
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponse, ErrorResponseMessage, IntegrationDetail}
import uk.gov.hmrc.integrationcatalogueadmin.models.{HeaderKeys, IntegrationDetailRequest}
import uk.gov.hmrc.integrationcatalogueadmin.services.IntegrationService
import uk.gov.hmrc.integrationcatalogueadmin.utils.ValidateParameters

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ValidateIntegrationIdAgainstParametersAction @Inject()(integrationService: IntegrationService)(implicit ec: ExecutionContext)
    extends ActionFilter[IntegrationDetailRequest]
    with HttpErrorFunctions
    with ValidateParameters {

  override def executionContext: ExecutionContext = ec

  override protected def filter[A](request: IntegrationDetailRequest[A]): Future[Option[Result]] = {

    val platformTypeHeader = request.headers.get(HeaderKeys.platformKey).getOrElse("")

    validatePlatformType(platformTypeHeader) match {
      case Some(platformType) => Future.successful(validateIntegrationIdAgainstPlatform(request.integrationDetail, platformType))
      case None               => Future.successful(None)
    }
  }

  private def validateIntegrationIdAgainstPlatform(integrationDetail: IntegrationDetail, platform: PlatformType) = {

    if (integrationDetail.platform == platform) None
    else
      Some(Unauthorized(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"Authorisation failed - ${platform.toString} is not authorised to delete an integration on ${integrationDetail.platform.toString}"))))))

  }

}
