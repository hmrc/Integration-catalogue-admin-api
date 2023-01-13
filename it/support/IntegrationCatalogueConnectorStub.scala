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

package support

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import play.api.test.Helpers.BAD_REQUEST
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId

trait IntegrationCatalogueConnectorStub {
  val publishUrl             = "/integration-catalogue/apis/publish"
  val publishFileTransferUrl = "/integration-catalogue/filetransfer/publish"
  val getApisUrl             = "/integration-catalogue/integrations"

  def deleteIntegrationByIdUrl(integrationId: String) = s"/integration-catalogue/integrations/$integrationId"

  def getIntegrationByIdUrl(id: String) = s"/integration-catalogue/integrations/$id"

  val catalogueReportUrl = "/integration-catalogue/report"

  def findWithFiltersUrl(searchTerm: String) = s"/integration-catalogue/integrations$searchTerm"

  def primeFindWithFilterReturnsBadRequestWithoutBody(searchTerm: String): StubMapping = {
    primeGETWithoutBody(BAD_REQUEST, findWithFiltersUrl(searchTerm))
  }

  def primeFindWithFilterWithBody(status: Int, responseBody: String, searchTerm: String): StubMapping = {
    primeGETWithBody(status, responseBody, findWithFiltersUrl(searchTerm))
  }

  def primePutReturnsBadRequestWithoutBody(putUrl: String): StubMapping = {
    primePUTWithoutBody(BAD_REQUEST, putUrl)
  }

  def primePutWithBody(putUrl: String, status: Int, responseBody: String): StubMapping = {
    primePUTWithBody(status, responseBody, putUrl)
  }

  def primeGetByIdWithBody(status: Int, responseBody: String, id: IntegrationId): StubMapping = {
    primeGETWithBody(status, responseBody, getIntegrationByIdUrl(id.value.toString))
  }

  def primeGetByIdReturnsBadRequest(id: IntegrationId): StubMapping = {
    primeGetByIdWithoutResponseBody(BAD_REQUEST, id.value.toString)
  }

  def primeGetByIdWithoutResponseBody(status: Int, id: String): StubMapping = {
    primeGETWithoutBody(status, getIntegrationByIdUrl(id))
  }

  def primeCatalogueReportWithBody(responseBody: String, status: Int): StubMapping = {
    primeGETWithBody(status, responseBody, catalogueReportUrl)
  }

  def primeCatalogueReportReturnsBadRequest(): StubMapping = {
    primeGETWithoutBody(BAD_REQUEST, catalogueReportUrl)
  }

  def primeDeleteByIdWithoutBody(integrationId: String, status: Int): StubMapping = {
    primeWithoutBody(delete(urlEqualTo(deleteIntegrationByIdUrl(integrationId))), status)
  }

  def primeDeleteByPlatformWithBody(platformQueryParm: String, status: Int, responseBody: String): StubMapping = {
    primeWithBody(delete(urlEqualTo(findWithFiltersUrl(platformQueryParm))), status, responseBody)
  }

  def primePUTWithoutBody(status: Int, urlResolver: => String): StubMapping = {
    primeWithoutBody(put(urlEqualTo(urlResolver)), status)
  }

  def primePUTWithBody(status: Int, responseBody: String, urlResolver: => String): StubMapping = {
    primeWithBody(put(urlEqualTo(urlResolver)), status, responseBody)
  }

  def primeGETWithoutBody(status: Int, urlResolver: => String): StubMapping = {
    primeWithoutBody(get(urlEqualTo(urlResolver)), status)
  }

  def primeGETWithBody(status: Int, responseBody: String, urlResolver: => String): StubMapping = {
    primeWithBody(get(urlEqualTo(urlResolver)), status, responseBody)
  }

  private def primeWithoutBody(x: => MappingBuilder, status: Int) = {
    stubFor(x
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
      ))
  }

  private def primeWithBody(x: MappingBuilder, status: Int, responseBody: String) = {
    stubFor(x
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type", "application/json")
          .withBody(responseBody)
      ))
  }

}
