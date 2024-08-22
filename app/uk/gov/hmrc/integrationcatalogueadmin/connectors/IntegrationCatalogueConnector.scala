/*
 * Copyright 2024 HM Revenue & Customs
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

import java.net.{URL, URLEncoder}
import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import play.api.Logging
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters.*
import uk.gov.hmrc.integrationcatalogue.models.*
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, PlatformType}

import uk.gov.hmrc.integrationcatalogueadmin.config.AppConfig

@Singleton
class IntegrationCatalogueConnector @Inject() (http: HttpClientV2, appConfig: AppConfig)(implicit ec: ExecutionContext) extends Logging {

  private lazy val externalServiceUri = s"${appConfig.integrationCatalogueUrl}/integration-catalogue"
  private val urlEncoder = URLEncoder.encode(_: String, StandardCharsets.UTF_8.name)

  def publishApis(publishRequest: ApiPublishRequest)(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]] = {
    handleResult(
      http.put(
        URL(s"$externalServiceUri/apis/publish")
      )(treatHeaderCarrier(hc))
      .withBody(Json.toJson(publishRequest))
      .setHeader(
        (AUTHORIZATION, appConfig.internalAuthToken)
      ).execute[PublishResult]
    )
  }

  def publishFileTransfer(publishRequest: FileTransferPublishRequest)(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]] = {
    handleResult(
      http.put(
        url = URL(s"$externalServiceUri/filetransfers/publish"),
      )(treatHeaderCarrier(hc))
      .withBody(Json.toJson(publishRequest))
      .setHeader(
        (AUTHORIZATION, appConfig.internalAuthToken)
      ).execute[PublishResult]
    )
  }

  def findWithFilters(integrationFilter: IntegrationFilter)(implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationResponse]] = {
    val queryParamsValues = buildQueryParams(integrationFilter)

    handleResult(
      http.get(
        url = URL(s"$externalServiceUri/integrations$queryParamsValues"),
      )(treatHeaderCarrier(hc))
      .setHeader(
        (AUTHORIZATION, appConfig.internalAuthToken)
      ).execute[IntegrationResponse]
    )
  }

  def findByIntegrationId(id: IntegrationId)(implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationDetail]] = {
    handleResult(
      http.get(
        url = URL(s"$externalServiceUri/integrations/${id.value.toString}"),
      )(treatHeaderCarrier(hc)).setHeader(
        (AUTHORIZATION, appConfig.internalAuthToken)
      ).execute[IntegrationDetail]
    )
  }

  def deleteByIntegrationId(integrationId: IntegrationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.delete(
      url = URL(s"$externalServiceUri/integrations/${integrationId.value}"),
    )(treatHeaderCarrier(hc)).setHeader(
      (AUTHORIZATION, appConfig.internalAuthToken)
    ).execute[HttpResponse]
      .map(_.status == NO_CONTENT)
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          false
      }
  }

  def deleteByPlatform(platform: PlatformType)(implicit hc: HeaderCarrier): Future[DeleteApiResult] = {
    http.delete(
      url = URL(s"$externalServiceUri/integrations?platformFilter=${platform.toString}"),
    )(treatHeaderCarrier(hc)).setHeader((AUTHORIZATION, appConfig.internalAuthToken))
      .execute[DeleteIntegrationsResponse]
      .map(x => DeleteIntegrationsSuccess(x))
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          DeleteIntegrationsFailure(e.getMessage)
      }
  }

  def catalogueReport()(implicit hc: HeaderCarrier): Future[Either[Throwable, List[IntegrationPlatformReport]]] = {
    handleResult(
      http.get(
        URL(s"$externalServiceUri/report")
      )(treatHeaderCarrier(hc)).setHeader(
        (AUTHORIZATION, appConfig.internalAuthToken)
      ).execute[List[IntegrationPlatformReport]]
    )
  }

  private def buildQueryParams(integrationFilter: IntegrationFilter): String = {
    val searchTerms       = integrationFilter.searchText.map(x => ("searchTerm", x))
    val platformsFilters  = integrationFilter.platforms.map((x: PlatformType) => ("platformFilter", x.toString))
    val backendFilters    = integrationFilter.backends.map(x => ("backendsFilter", x))
    val queryParamsValues = searchTerms ++ platformsFilters ++ backendFilters
    queryParamsValues.map { case (name, value) => s"$name=${urlEncoder(value)}" }.mkString("?", "&", "")
  }

  private def handleResult[A](result: Future[A]): Future[Either[Throwable, A]] = {
    result.map(x => Right(x))
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          Left(e)
      }
  }

  private def treatHeaderCarrier(hc: HeaderCarrier): HeaderCarrier = {
    hc.copy(authorization = None)
  }

}
