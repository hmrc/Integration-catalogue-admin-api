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

package uk.gov.hmrc.integrationcatalogueadmin.controllers

import akka.stream.Materializer
import org.mockito.scalatest.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, StubBodyParserFactory}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationType.{API, FILE_TRANSFER}
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.CORE_IF
import uk.gov.hmrc.integrationcatalogue.models.{DeleteIntegrationsFailure, DeleteIntegrationsResponse, DeleteIntegrationsSuccess, IntegrationPlatformReport}
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogueadmin.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadmin.controllers.actionbuilders._
import uk.gov.hmrc.integrationcatalogueadmin.data.ApiDetailTestData
import uk.gov.hmrc.integrationcatalogueadmin.models.HeaderKeys
import uk.gov.hmrc.integrationcatalogueadmin.services.IntegrationService


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IntegrationControllerSpec
    extends WordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with MockitoSugar
    with StubBodyParserFactory
    with ApiDetailTestData
    with BeforeAndAfterEach {

  implicit lazy val mat: Materializer = app.materializer

  val mockAppConfig: AppConfig = mock[AppConfig]
  val mockIntegrationService: IntegrationService = mock[IntegrationService]

  private val validateQueryParamKeyAction = app.injector.instanceOf[ValidateQueryParamKeyAction]
  private val authAction = new ValidateAuthorizationHeaderAction(mockAppConfig)
  private val validateIntegrationIdAgainstPlatformTypeAction = new ValidateIntegrationIdAgainstParametersAction(mockIntegrationService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig)
  }

  trait Setup {

    val controller = new IntegrationController(
      mockAppConfig,
      mockIntegrationService,
      validateQueryParamKeyAction,
      authAction,
      validateIntegrationIdAgainstPlatformTypeAction,
      stubControllerComponents()
    )

    private val encodedCoreIfAuthKey = "c29tZUtleTM="
    val coreIfAuthHeader = List(HeaderNames.AUTHORIZATION -> encodedCoreIfAuthKey)
    val coreIfPlatformTypeHeader = List(HeaderKeys.platformKey -> "CORE_IF")

    private val encodedMasterAuthKey = "dGVzdC1hdXRoLWtleQ=="
    val masterKeyHeader = List(HeaderNames.AUTHORIZATION -> encodedMasterAuthKey)

  }


  "DELETE /services/integrations/{id}" should {

    "respond with 401 when platform header does not have any auth setup in app config" in new Setup {
      when(mockIntegrationService.findByIntegrationId(*[IntegrationId])(*)).thenReturn(Future.successful(Right(exampleApiDetail)))

      when(mockAppConfig.authPlatformMap).thenReturn(Map.empty)

      val deleteRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest.apply("DELETE", s"integration-catalogue-admin-api/services/integrations/${exampleApiDetail.id.value.toString}")
          .withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader: _*)

      val result: Future[Result] = controller.deleteByIntegrationId(exampleApiDetail.id)(deleteRequest)
      status(result) shouldBe UNAUTHORIZED
      contentAsString(result) shouldBe """{"errors":[{"message":"Authorisation failed"}]}"""

    }
  }

  "DELETE /services/integrations" should {
    "respond with 200 when using master key" in new Setup {

      when(mockAppConfig.authorizationKey).thenReturn("test-auth-key")

      when(mockIntegrationService.deleteByPlatform(*)(*)).thenReturn(Future.successful(DeleteIntegrationsSuccess(DeleteIntegrationsResponse(1))))

      val deleteRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest.apply("DELETE", s"integration-catalogue-admin-api/services/integrations?platforms=${PlatformType.CORE_IF.toString}")
          .withHeaders(masterKeyHeader: _*)

      val result: Future[Result] = controller.deleteByPlatform(List(PlatformType.CORE_IF))(deleteRequest)
      status(result) shouldBe OK
      contentAsString(result) shouldBe """{"numberOfIntegrationsDeleted":1}"""

    }

    "respond with 200 when using platform header for auth and this matches the platform param" in new Setup {

      when(mockAppConfig.authPlatformMap).thenReturn(Map(PlatformType.CORE_IF -> "someKey3"))

      when(mockIntegrationService.deleteByPlatform(*)(*)).thenReturn(Future.successful(DeleteIntegrationsSuccess(DeleteIntegrationsResponse(1))))

      val deleteRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest.apply("DELETE", s"integration-catalogue-admin-api/services/integrations?platforms=${PlatformType.CORE_IF.toString}")
          .withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader: _*)

      val result: Future[Result] = controller.deleteByPlatform(List(PlatformType.CORE_IF))(deleteRequest)
      status(result) shouldBe OK
      contentAsString(result) shouldBe """{"numberOfIntegrationsDeleted":1}"""

    }

    "respond with 500 when backend returns error" in new Setup {

      when(mockAppConfig.authPlatformMap).thenReturn(Map(PlatformType.CORE_IF -> "someKey3"))

      when(mockIntegrationService.deleteByPlatform(*)(*)).thenReturn(Future.successful(DeleteIntegrationsFailure("Internal Server Error")))

      val deleteRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest.apply("DELETE", s"integration-catalogue-admin-api/services/integrations?platforms=${PlatformType.CORE_IF.toString}")
          .withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader: _*)

      val result: Future[Result] = controller.deleteByPlatform(List(PlatformType.CORE_IF))(deleteRequest)
      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe """{"errors":[{"message":"Internal Server Error"}]}"""

    }

    "respond with 401 when platform header for auth does not match the platform param" in new Setup {

      when(mockAppConfig.authPlatformMap).thenReturn(Map(PlatformType.CORE_IF -> "someKey3"))

      val deleteRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest.apply("DELETE", s"integration-catalogue-admin-api/services/integrations?platforms=${PlatformType.API_PLATFORM.toString}")
          .withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader: _*)

      val result: Future[Result] = controller.deleteByPlatform(List(PlatformType.API_PLATFORM))(deleteRequest)
      status(result) shouldBe UNAUTHORIZED
      contentAsString(result) shouldBe """{"errors":[{"message":"You are not authorised to delete integrations on this Platform"}]}"""

    }

    "respond with 400 when platform param is missing" in new Setup {

      when(mockAppConfig.authPlatformMap).thenReturn(Map(PlatformType.CORE_IF -> "someKey3"))

      val deleteRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest.apply("DELETE", s"integration-catalogue-admin-api/services/integrations")
          .withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader: _*)

      val result: Future[Result] = controller.deleteByPlatform(List.empty)(deleteRequest)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe """{"errors":[{"message":"platforms query parameter is either invalid, missing or multiple have been provided"}]}"""

    }

    "respond with 400 when multiple platform params passed in" in new Setup {

      when(mockAppConfig.authPlatformMap).thenReturn(Map(PlatformType.CORE_IF -> "someKey3"))

      val deleteRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest
          .apply(
            "DELETE",
            s"integration-catalogue-admin-api/services/integrations?platforms=${PlatformType.API_PLATFORM.toString}&platforms=${PlatformType.CORE_IF.toString}"
          )
          .withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader: _*)

      val result: Future[Result] = controller.deleteByPlatform(List(PlatformType.API_PLATFORM, PlatformType.CORE_IF))(deleteRequest)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe """{"errors":[{"message":"platforms query parameter is either invalid, missing or multiple have been provided"}]}"""

    }

  }

  "GET findWithFilters" should {
    "respond with 400 when backendsFilter has no value" in new Setup {

      val getRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest.apply("GET", s"integration-catalogue-admin-api/services/integrations/?backendsFilter=")

      val result: Future[Result] = controller.findWithFilters(List.empty, List.empty, List.empty)(getRequest)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe """{"errors":[{"message":"backendsFilter cannot be empty"}]}"""

    }
  }

  "GET /report" should {
    "respond with 500 when error received from service " in new Setup {
      when(mockIntegrationService.catalogueReport()(*)).thenReturn(Future.successful(Left(new NotFoundException("some error"))))

      val getRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest.apply("GET", s"integration-catalogue-admin-api/report")

      val result: Future[Result] = controller.catalogueReport()(getRequest)
      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe """{"errors":[{"message":"catalogueReport: error retrieving the report"}]}"""
    }
    "respond with results when received from the service " in new Setup {
      val results = List(IntegrationPlatformReport(CORE_IF, API, 2), IntegrationPlatformReport(CORE_IF, FILE_TRANSFER, 5))
      when(mockIntegrationService.catalogueReport()(*)).thenReturn(Future.successful(Right(results)))

      val getRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest.apply("GET", s"integration-catalogue-admin-api/report")

      val result: Future[Result] = controller.catalogueReport()(getRequest)
      status(result) shouldBe OK
      contentAsString(result) shouldBe Json.toJson(results).toString
    }
  }
}
