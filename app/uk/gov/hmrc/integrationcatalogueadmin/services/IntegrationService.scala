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

package uk.gov.hmrc.integrationcatalogueadmin.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogue.models.{DeleteApiResult, IntegrationDetail, IntegrationFilter, IntegrationPlatformReport, IntegrationResponse}
import uk.gov.hmrc.integrationcatalogueadmin.connectors.IntegrationCatalogueConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType


@Singleton
class IntegrationService @Inject()(integrationCatalogueConnector: IntegrationCatalogueConnector){

  def deleteByIntegrationId(integrationId: IntegrationId)(implicit hc: HeaderCarrier) : Future[Boolean] = {
    integrationCatalogueConnector.deleteByIntegrationId(integrationId)
  }

  def deleteByPlatform(platform: PlatformType)(implicit hc: HeaderCarrier) : Future[DeleteApiResult] = {
    integrationCatalogueConnector.deleteByPlatform(platform)
  }

  def findWithFilters(integrationFilter: IntegrationFilter)
  (implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationResponse]] = {
        integrationCatalogueConnector.findWithFilters(integrationFilter)
  }

  def findByIntegrationId(integrationId: IntegrationId)
    (implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationDetail]] = {
         integrationCatalogueConnector.findByIntegrationId(integrationId)
  }

  def catalogueReport()(implicit hc: HeaderCarrier): Future[Either[Throwable, List[IntegrationPlatformReport]]] ={
    integrationCatalogueConnector.catalogueReport()
  }
}


