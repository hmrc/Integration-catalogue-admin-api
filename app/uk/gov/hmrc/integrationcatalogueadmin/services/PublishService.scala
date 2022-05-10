/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.integrationcatalogue.models.{ApiPublishRequest, PublishResult}
import uk.gov.hmrc.integrationcatalogue.models.common.{PlatformType, SpecificationType}
import uk.gov.hmrc.integrationcatalogueadmin.connectors.IntegrationCatalogueConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.integrationcatalogue.models.FileTransferPublishRequest


@Singleton
class PublishService @Inject()(integrationCatalogueConnector: IntegrationCatalogueConnector){
    
    def publishApi(publisherRef: Option[String], platformType: PlatformType, specType: SpecificationType, contents: String)
    (implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]]  ={
         integrationCatalogueConnector.publishApis(ApiPublishRequest(publisherRef, platformType, specType, contents))
    }

  def publishFileTransfer(publishRequest: FileTransferPublishRequest)
    (implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]]  ={
         integrationCatalogueConnector.publishFileTransfer(publishRequest)
    }

}


