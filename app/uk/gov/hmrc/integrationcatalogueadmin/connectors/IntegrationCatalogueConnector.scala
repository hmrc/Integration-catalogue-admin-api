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

package uk.gov.hmrc.integrationcatalogueadmin.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import play.api.Logging
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, PlatformType}

import uk.gov.hmrc.integrationcatalogueadmin.config.AppConfig

@Singleton
class IntegrationCatalogueConnector @Inject() (http: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) extends Logging {

  private lazy val externalServiceUri = s"${appConfig.integrationCatalogueUrl}/integration-catalogue"

  def publishApis(publishRequest: ApiPublishRequest)(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]] = {
    handleResult(
      http.PUT[ApiPublishRequest, PublishResult](
        url = s"$externalServiceUri/apis/publish",
        body = publishRequest,
        headers = Seq((AUTHORIZATION, appConfig.internalAuthToken))
      )
    )
  }

  def publishFileTransfer(publishRequest: FileTransferPublishRequest)(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]] = {
    handleResult(
      http.PUT[FileTransferPublishRequest, PublishResult](
        url = s"$externalServiceUri/filetransfers/publish",
        body = publishRequest,
        headers = Seq((AUTHORIZATION, appConfig.internalAuthToken))
      )
    )
  }

  def findWithFilters(integrationFilter: IntegrationFilter)(implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationResponse]] = {
    val queryParamsValues = buildQueryParams(integrationFilter)

    handleResult(
      http.GET[IntegrationResponse](
        url = s"$externalServiceUri/integrations",
        queryParams = queryParamsValues,
        headers = Seq((AUTHORIZATION, appConfig.internalAuthToken))
      )
    )
  }

  def findByIntegrationId(id: IntegrationId)(implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationDetail]] = {
    handleResult(
      http.GET[IntegrationDetail](
        url = s"$externalServiceUri/integrations/${id.value.toString}",
        headers = Seq((AUTHORIZATION, appConfig.internalAuthToken))
      )
    )
  }

  def deleteByIntegrationId(integrationId: IntegrationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.DELETE[HttpResponse](
      url = s"$externalServiceUri/integrations/${integrationId.value}",
      headers = Seq((AUTHORIZATION, appConfig.internalAuthToken))
    )
      .map(_.status == NO_CONTENT)
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          false
      }
  }

  def deleteByPlatform(platform: PlatformType)(implicit hc: HeaderCarrier): Future[DeleteApiResult] = {
    http.DELETE[DeleteIntegrationsResponse](
      url = s"$externalServiceUri/integrations?platformFilter=${platform.toString}",
      headers = Seq((AUTHORIZATION, appConfig.internalAuthToken))
    )
      .map(x => DeleteIntegrationsSuccess(x))
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          DeleteIntegrationsFailure(e.getMessage)
      }
  }

  def catalogueReport()(implicit hc: HeaderCarrier): Future[Either[Throwable, List[IntegrationPlatformReport]]] = {
    handleResult(
      http.GET[List[IntegrationPlatformReport]](
        url = s"$externalServiceUri/report",
        headers = Seq((AUTHORIZATION, appConfig.internalAuthToken))
      )
    )
  }

  private def buildQueryParams(integrationFilter: IntegrationFilter): Seq[(String, String)] = {
    val searchTerms      = integrationFilter.searchText.map(x => ("searchTerm", x))
    val platformsFilters = integrationFilter.platforms.map((x: PlatformType) => ("platformFilter", x.toString))
    val backendFilters   = integrationFilter.backends.map(x => ("backendsFilter", x))
    searchTerms ++ platformsFilters ++ backendFilters

  }

  private def handleResult[A](result: Future[A]): Future[Either[Throwable, A]] = {
    result.map(x => Right(x))
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          Left(e)
      }
  }

}
