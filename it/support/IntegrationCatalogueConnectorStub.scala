/*
 * Copyright 2021 HM Revenue & Customs
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


import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.Helpers.BAD_REQUEST
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
trait IntegrationCatalogueConnectorStub {
  val publishUrl = "/integration-catalogue/apis/publish"
  val publishFileTransferUrl = "/integration-catalogue/filetransfer/publish"
  val getApisUrl = "/integration-catalogue/integrations"
  def deleteintegrationByIdUrl(integrationId: String) = s"/integration-catalogue/integrations/$integrationId"
  def getIntegrationByIdUrl(id: String) = s"/integration-catalogue/integrations/$id"
  def findWithFiltersUrl(searchTerm: String) = s"/integration-catalogue/integrations$searchTerm"

    def primeIntegrationCatalogueServiceFindWithFilterWithBadRequest(searchTerm: String) = {

    stubFor(get(urlEqualTo(findWithFiltersUrl(searchTerm)))
      .willReturn(
        aResponse()
          .withStatus(BAD_REQUEST)
          .withHeader("Content-Type","application/json")
      )
    )
  }

  def primeIntegrationCatalogueServiceFindWithFilterWithBody(status : Int, responseBody : String, searchTerm: String) = {

    stubFor(get(urlEqualTo(findWithFiltersUrl(searchTerm)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type","application/json")
          .withBody(responseBody)
      )
    )
  }


  def primeIntegrationCatalogueServicePutReturnsBadRequest(putUrl: String) = {

      stubFor(put(urlEqualTo(putUrl))
      .willReturn(
        aResponse()
          .withStatus(BAD_REQUEST)
          .withHeader("Content-Type","application/json")

      )
    )
  }

    def primeIntegrationCatalogueServicePutWithBody(putUrl: String, status : Int, responseBody : String) = {

      stubFor(put(urlEqualTo(putUrl))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type","application/json")
          .withBody(responseBody)
      )
    )
  }

  def primeIntegrationCatalogueServiceGetByIdWithBody(status : Int, responseBody : String, id: IntegrationId) = {

    stubFor(get(urlEqualTo(getIntegrationByIdUrl(id.value.toString)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type","application/json")
          .withBody(responseBody)
      )
    )
  }

  def primeIntegrationCatalogueServiceGetByIdWithoutResponseBody(status : Int, id: String) = {

    stubFor(get(urlEqualTo(getIntegrationByIdUrl(id)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type","application/json")
      )
    )
  }

  def primeIntegrationCatalogueServiceGetByIdReturnsBadRequest( id: IntegrationId) = {

    stubFor(get(urlEqualTo(getIntegrationByIdUrl(id.value.toString)))
      .willReturn(
        aResponse()
          .withStatus(BAD_REQUEST)
          .withHeader("Content-Type","application/json")

      )
    )
  }

  def primeIntegrationCatalogueServiceGetWithBody(status : Int, responseBody : String) = {

    stubFor(get(urlEqualTo(getApisUrl))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type","application/json")
          .withBody(responseBody)
      )
    )
  }

  def primeIntegrationCatalogueServiceDelete(integrationId: String, status : Int) = {

    stubFor(delete(urlEqualTo(deleteintegrationByIdUrl(integrationId)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type","application/json")
      )
    )
  }

  def primeIntegrationCatalogueServiceDeleteByPlatform(platformQueryParm: String, status : Int, responseBody: String) = {

    stubFor(delete(urlEqualTo(findWithFiltersUrl(platformQueryParm)))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withHeader("Content-Type","application/json")
          .withBody(responseBody)
      )
    )
  }


}
