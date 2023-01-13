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

package controllers

import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import support.{IntegrationCatalogueConnectorStub, ServerBaseISpec}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationType.API
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.CORE_IF
import uk.gov.hmrc.integrationcatalogue.models.{ApiDetail, DeleteIntegrationsResponse, IntegrationDetail, IntegrationPlatformReport, IntegrationResponse}
import uk.gov.hmrc.integrationcatalogueadmin.data.ApiDetailTestData
import uk.gov.hmrc.integrationcatalogueadmin.models.HeaderKeys

import scala.concurrent.Future

class IntegrationControllerISpec extends ServerBaseISpec
    with IntegrationCatalogueConnectorStub with ApiDetailTestData {

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                  -> wireMockPort,
        "metrics.enabled"                                  -> true,
        "auditing.enabled"                                 -> false,
        "auditing.consumer.baseUri.host"                   -> wireMockHost,
        "auditing.consumer.baseUri.port"                   -> wireMockPort,
        "microservice.services.integration-catalogue.host" -> wireMockHost,
        "microservice.services.integration-catalogue.port" -> wireMockPort
      )

  val url = s"http://localhost:$port/integration-catalogue-admin-api"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  trait Setup {

    private val encodedMasterAuthKey      = "dGVzdC1hdXRoLWtleQ=="
    private val encodedCoreIfAuthKey      = "c29tZUtleTM="
    private val encodedApiPlatformAuthKey = "c29tZUtleTI="
    val coreIfAuthHeader                  = List(HeaderNames.AUTHORIZATION -> encodedCoreIfAuthKey)
    val apiPlatformAuthHeader             = List(HeaderNames.AUTHORIZATION -> encodedApiPlatformAuthKey)
    val masterKeyHeader                   = List(HeaderNames.AUTHORIZATION -> encodedMasterAuthKey)
    val coreIfPlatformTypeHeader          = List(HeaderKeys.platformKey -> "CORE_IF")
    val apiPlatformPlatformTypeHeader     = List(HeaderKeys.platformKey -> "API_PLATFORM")

    val exampleIntegrationId                                     = "2840ce2d-03fa-46bb-84d9-0299402b7b32"

    val validGetApisRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest(Helpers.GET, "/integration-catalogue-admin-api/services/integrations")

    def validFindByIntegrationIdRequest(id: String): FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest(Helpers.GET, s"/integration-catalogue-admin-api/services/integrations/$id")

    def validFindwithFilterRequest(searchTerm: String): FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest(Helpers.GET, s"/integration-catalogue-admin-api/services/integrations$searchTerm")

    def validCatalogueReportRequest(): FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest(Helpers.GET, "/integration-catalogue-admin-api/services/report")

    def validDeleteIntegrationRequest(integrationId: String): FakeRequest[AnyContentAsEmpty.type] = {
      FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-api/services/integrations/$integrationId")
        .withHeaders(masterKeyHeader: _*)
    }

    def validDeleteIntegrationRequestWithNoHeaders(integrationId: String): FakeRequest[AnyContentAsEmpty.type] = {
      FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-api/services/integrations/$integrationId")
    }

    def validDeleteByPlatformRequest(queryParam: String): FakeRequest[AnyContentAsEmpty.type] = {
      FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-api/services/integrations/$queryParam")
        .withHeaders(masterKeyHeader: _*)
    }

    def invalidPathRequest(): FakeRequest[AnyContentAsEmpty.type] = {
      FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-api/services/iamanunknownpath")
    }
  }

  "IntegrationController" when {

    "DELETE [some unknown path]" should {
      "return blah" in new Setup {
        val response: Future[Result] = route(app, invalidPathRequest()).get
        status(response) mustBe NOT_FOUND
        contentAsString(response) mustBe """{"errors":[{"message":"Path or Http method may be wrong. "}]}"""
      }
    }

    "GET /services/integrations/{id}" should {

      "return 400 when using invalid integrationId" in new Setup {
        val unknownIntegrationId = "UNKNOWN"

        val response: Future[Result] = route(app, validFindByIntegrationIdRequest(unknownIntegrationId)).get
        status(response) mustBe 400
        contentAsString(response) mustBe """{"errors":[{"message":"Cannot accept UNKNOWN as IntegrationId"}]}"""
      }

      "return 200 and integration detail from backend" in new Setup {
        val jsonAsString: String = Json.toJson(exampleApiDetail.asInstanceOf[IntegrationDetail]).toString
        primeGetByIdWithBody(OK, jsonAsString, exampleApiDetail.id)

        val response: Future[Result] = route(app, validFindByIntegrationIdRequest(exampleApiDetail.id.value.toString)).get
        status(response) mustBe OK
        contentAsString(response) mustBe jsonAsString
      }

      "return 404 when backend returns 404" in new Setup {
        primeGetByIdWithBody(NOT_FOUND, "", exampleApiDetail.id)

        val response: Future[Result] = route(app, validFindByIntegrationIdRequest(exampleApiDetail.id.value.toString)).get
        status(response) mustBe NOT_FOUND
        contentAsString(response) mustBe """{"errors":[{"message":"findByIntegrationId: The requested resource could not be found."}]}"""
      }

      "return 400 when backend returns 400" in new Setup {
        primeGetByIdWithBody(BAD_REQUEST, "", exampleApiDetail.id)

        val response: Future[Result] = route(app, validFindByIntegrationIdRequest(exampleApiDetail.id.value.toString)).get
        status(response) mustBe BAD_REQUEST
      }

    }

    "GET /integrations" should {
      "return 200 and integration response from backend when using searchTerm" in new Setup {
        val searchTerm = "?searchTerm=API-1001"
        primeFindWithFilterWithBody(OK, Json.toJson(IntegrationResponse(0, List.empty)).toString, searchTerm)

        val response: Future[Result] = route(app, validFindwithFilterRequest(searchTerm)).get
        status(response) mustBe OK
        contentAsString(response) mustBe """{"count":0,"results":[]}"""
      }

      "return 200 and integration response from backend when using platformFilter" in new Setup {
        val platformFilter = "?platformFilter=CORE_IF"
        primeFindWithFilterWithBody(OK, Json.toJson(IntegrationResponse(0, List.empty)).toString, platformFilter)

        val response: Future[Result] = route(app, validFindwithFilterRequest(platformFilter)).get
        status(response) mustBe OK
        contentAsString(response) mustBe """{"count":0,"results":[]}"""
      }

      "return 200 and integration response from backend when using backendsFilter" in new Setup {
        val backendsFilter = "?backendsFilter=ETMP"
        primeFindWithFilterWithBody(OK, Json.toJson(IntegrationResponse(0, List.empty)).toString, backendsFilter)

        val response: Future[Result] = route(app, validFindwithFilterRequest(backendsFilter)).get
        status(response) mustBe OK
        contentAsString(response) mustBe """{"count":0,"results":[]}"""
      }

      "return 400 when using invalid filter key" in new Setup {
        val invalidFilterKey = "?invalidFilterKey=UNKNOWN"

        val response: Future[Result] = route(app, validFindwithFilterRequest(invalidFilterKey)).get
        status(response) mustBe 400
        contentAsString(response) mustBe """{"errors":[{"message":"Invalid query parameter key provided. It is case sensitive"}]}"""
      }

      "return 400 when using invalid platformFilter" in new Setup {
        val platformFilter = "?platformFilter=UNKNOWN"

        val response: Future[Result] = route(app, validFindwithFilterRequest(platformFilter)).get
        status(response) mustBe 400
        contentAsString(response) mustBe """{"errors":[{"message":"Cannot accept UNKNOWN as PlatformType"}]}"""
      }

      "return 400 when using empty platformFilter value" in new Setup {
        val platformFilter = "?platformFilter="

        val response: Future[Result] = route(app, validFindwithFilterRequest(platformFilter)).get
        status(response) mustBe 400
        contentAsString(response) mustBe """{"errors":[{"message":"platformType cannot be empty"}]}"""
      }

      "return 500 and when 404 returned from backend" in new Setup {
        val searchTerm = "?searchTerm=API-1001"
        primeFindWithFilterWithBody(NOT_FOUND, "", searchTerm)

        val response: Future[Result] = route(app, validFindwithFilterRequest(searchTerm)).get
        status(response) mustBe INTERNAL_SERVER_ERROR
        contentAsString(response) mustBe """{"errors":[{"message":"Unable to process your request"}]}"""

      }

      "return 500 and when 400 returned from backend" in new Setup {
        val searchTerm = "?searchTerm=API-1001"
        primeFindWithFilterWithBody(BAD_REQUEST, "", searchTerm)

        val response: Future[Result] = route(app, validFindwithFilterRequest(searchTerm)).get
        status(response) mustBe INTERNAL_SERVER_ERROR

      }

    }

    "DELETE /services/integrations/{id}" should {

      "respond with 200 when api results returned from backend" in new Setup {
        primeFindWithFilterWithBody(OK, Json.toJson(IntegrationResponse(1, List(exampleApiDetail3))).toString, "")

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe OK
        contentAsString(response) mustBe Json.toJson(IntegrationResponse(1, List(exampleApiDetail3))).toString
      }

      "respond with 500 when 404 returned from backend" in new Setup {
        primeFindWithFilterWithBody(NOT_FOUND, "", "")

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe INTERNAL_SERVER_ERROR
        contentAsString(response) mustBe """{"errors":[{"message":"Unable to process your request"}]}"""
      }

      "respond with 204 when deletion successful when platforms match" in new Setup {
        primeGetByIdWithBody(OK, Json.toJson(exampleApiDetail.asInstanceOf[IntegrationDetail]).toString, exampleApiDetail.id)

        primeDeleteByIdWithoutBody(exampleApiDetail.id.value.toString, NO_CONTENT)

        val response: Future[Result] =
          route(app, validDeleteIntegrationRequest(exampleApiDetail.id.value.toString).withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader: _*)).get
        status(response) mustBe NO_CONTENT
      }

      "respond with 400 when non uuid id provided" in new Setup {

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-api/services/integrations/invalidId")

        val response: Future[Result] = route(app, request).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"errors":[{"message":"Cannot accept invalidId as IntegrationId"}]}"""
      }

      "respond with 500 when backend returns an error" in new Setup {
        primeGetByIdWithBody(OK, Json.toJson(exampleApiDetail.asInstanceOf[IntegrationDetail]).toString, exampleApiDetail.id)

        primeDeleteByIdWithoutBody(exampleApiDetail.id.value.toString, BAD_REQUEST)

        val response: Future[Result] = route(app, validDeleteIntegrationRequest(exampleApiDetail.id.value.toString)).get
        status(response) mustBe INTERNAL_SERVER_ERROR
        contentAsString(response) mustBe s"""{"errors":[{"message":"InternalServerError from integration-catalogue"}]}"""
      }

      "respond with 401 when no auth header but platform type header is present" in new Setup {
        primeDeleteByIdWithoutBody(exampleIntegrationId, NOT_FOUND)

        val requestWithNoAuthHeader: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-api/services/integrations/$exampleIntegrationId")

        val response: Future[Result] = route(app, requestWithNoAuthHeader.withHeaders(coreIfPlatformTypeHeader: _*)).get
        status(response) mustBe UNAUTHORIZED

        contentAsString(response) mustBe """{"errors":[{"message":"Authorisation failed"}]}"""
      }

      "respond with 400 when auth header present but platform type header is missing" in new Setup {
        primeDeleteByIdWithoutBody(exampleIntegrationId, NOT_FOUND)

        val requestWithNoAuthHeader: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-api/services/integrations/$exampleIntegrationId")

        val response: Future[Result] = route(app, requestWithNoAuthHeader.withHeaders(coreIfAuthHeader: _*)).get
        status(response) mustBe BAD_REQUEST

        contentAsString(response) mustBe """{"errors":[{"message":"platform type header is missing or invalid"}]}"""
      }

      "respond with 400 when auth header present but platform type header is invalid" in new Setup {
        primeDeleteByIdWithoutBody(exampleIntegrationId, NOT_FOUND)

        val requestWithNoAuthHeader: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-api/services/integrations/$exampleIntegrationId")

        val response: Future[Result] =
          route(app, requestWithNoAuthHeader.withHeaders(coreIfAuthHeader ++ List(HeaderKeys.platformKey -> "INVALID_PLATFORM"): _*)).get
        status(response) mustBe BAD_REQUEST

        contentAsString(response) mustBe """{"errors":[{"message":"platform type header is missing or invalid"}]}"""
      }

      "respond with 404 when auth header and key are CORE_IF and integrationId is not found" in new Setup {
        primeGetByIdWithoutResponseBody(NOT_FOUND, exampleIntegrationId)

        val requestWithNoAuthHeader: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-api/services/integrations/$exampleIntegrationId")

        val response: Future[Result] = route(app, requestWithNoAuthHeader.withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader: _*)).get
        status(response) mustBe NOT_FOUND

        contentAsString(response) mustBe """{"errors":[{"message":"Integration with ID: 2840ce2d-03fa-46bb-84d9-0299402b7b32 not found"}]}"""
      }

      "respond with 401 when auth header and key are CORE_IF but integrationId on API_PLATFORM" in new Setup {
        val integrationWithApiPlatform: ApiDetail = exampleApiDetail.copy(platform = PlatformType.API_PLATFORM)
        primeGetByIdWithBody(OK, Json.toJson(integrationWithApiPlatform.asInstanceOf[IntegrationDetail]).toString, integrationWithApiPlatform.id)

        val requestWithNoAuthHeader: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-api/services/integrations/${integrationWithApiPlatform.id.value.toString}")

        val response: Future[Result] = route(app, requestWithNoAuthHeader.withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader: _*)).get
        status(response) mustBe UNAUTHORIZED

        contentAsString(response) mustBe """{"errors":[{"message":"Authorisation failed - CORE_IF is not authorised to delete an integration on API_PLATFORM"}]}"""
      }
    }

    "DELETE /services/integrations" should {
      "return 400 when using invalid filter key" in new Setup {
        val invalidFilterKey = "?invalidFilterKey=UNKNOWN"

        val response: Future[Result] = route(app, validDeleteByPlatformRequest(invalidFilterKey)).get
        status(response) mustBe 400
        contentAsString(response) mustBe """{"errors":[{"message":"platforms query parameter is either invalid, missing or multiple have been provided"}]}"""
      }

      "return 204 when using valid filter key" in new Setup {
        val validFilterKey = "?platformFilter=CMA"

        primeDeleteByPlatformWithBody(validFilterKey, OK, Json.toJson(DeleteIntegrationsResponse(1)).toString)

        val response: Future[Result] = route(app, validDeleteByPlatformRequest(validFilterKey)).get
        status(response) mustBe OK
        contentAsString(response) mustBe "{\"numberOfIntegrationsDeleted\":1}"
      }
    }

    "GET /report" should {
      "return report when call to backend successful" in new Setup {
        primeCatalogueReportWithBody(Json.toJson(List(IntegrationPlatformReport(CORE_IF, API, 3))).toString, OK)

        val response: Future[Result] = route(app, validCatalogueReportRequest()).get
        status(response) mustBe OK
        contentAsString(response) mustBe """[{"platformType":"CORE_IF","integrationType":"API","count":3}]"""
      }

      "return 500 when call to backend fails" in new Setup {
        primeCatalogueReportReturnsBadRequest()

        val response: Future[Result] = route(app, validCatalogueReportRequest()).get
        status(response) mustBe INTERNAL_SERVER_ERROR

      }
    }
  }
}
