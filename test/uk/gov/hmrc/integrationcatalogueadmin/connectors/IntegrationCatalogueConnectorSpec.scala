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

package uk.gov.hmrc.integrationcatalogueadmin.connectors

import org.mockito.captor.{ArgCaptor, Captor}
import org.mockito.scalatest.MockitoSugar
import org.mockito.stubbing.ScalaOngoingStubbing
import org.scalatest.{BeforeAndAfterEach, Matchers, OptionValues, WordSpec}
import play.api.libs.json.Writes
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadGatewayException, HttpClient, _}
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogueadmin.AwaitTestSupport
import uk.gov.hmrc.integrationcatalogueadmin.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadmin.data.ApiDetailTestData

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class IntegrationCatalogueConnectorSpec extends WordSpec with Matchers with OptionValues
  with MockitoSugar with BeforeAndAfterEach with AwaitTestSupport with ApiDetailTestData {

  private val mockHttpClient = mock[HttpClient]
  private val mockAppConfig = mock[AppConfig]
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier = HeaderCarrier()


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient)
  }

  trait SetUp {
    val headerCarrierCaptor: Captor[HeaderCarrier] = ArgCaptor[HeaderCarrier]

    val connector = new IntegrationCatalogueConnector(
      mockHttpClient,
      mockAppConfig)
    val integrationId: IntegrationId = IntegrationId(UUID.fromString("2840ce2d-03fa-46bb-84d9-0299402b7b32"))
    val searchTerm  = "API-1001"
    val outboundUrl = "/integration-catalogue/apis/publish"
    val findWithFilterlUrl = s"/integration-catalogue/integrations"
    def deleteIntegrationsUrl(id: IntegrationId) = s"/integration-catalogue/integrations/${id.value}"
    def deleteIntegrationsByPlatformUrl(platform: String) = s"/integration-catalogue/integrations?platformFilter=$platform"

    def httpCallToPublishWillSucceedWithResponse(response: PublishResult): ScalaOngoingStubbing[Future[PublishResult]] =
      when(mockHttpClient.PUT[ApiPublishRequest, PublishResult]
        (eqTo(outboundUrl), any[ApiPublishRequest], any[Seq[(String, String)]])
        (any[Writes[ApiPublishRequest]], any[HttpReads[PublishResult]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(response))

    def httpCallToPublishWillFailWithException(exception: Throwable): ScalaOngoingStubbing[Future[PublishResult]] =
      when(mockHttpClient.PUT[ApiPublishRequest, PublishResult]
        (eqTo(outboundUrl), any[ApiPublishRequest], any[Seq[(String, String)]])
        (any[Writes[ApiPublishRequest]], any[HttpReads[PublishResult]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(exception))

    def httpCallToFindWithFilterWillSucceedWithResponse(response: IntegrationResponse): ScalaOngoingStubbing[Future[IntegrationResponse]] =
      when(mockHttpClient.GET[IntegrationResponse]
        (eqTo(findWithFilterlUrl), eqTo(Seq(("searchTerm",searchTerm))), eqTo(Seq.empty))
        (any[HttpReads[IntegrationResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(response))

    def httpCallToFindWithFilterWillFailWithException(exception: Throwable): ScalaOngoingStubbing[Future[IntegrationResponse]] =
           when(mockHttpClient.GET[IntegrationResponse]
        (eqTo(findWithFilterlUrl), eqTo(Seq(("searchTerm",searchTerm))), eqTo(Seq.empty))
        (any[HttpReads[IntegrationResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(exception))

    def httpCallToDeleteApiWillSucceed(response: HttpResponse, id: IntegrationId): ScalaOngoingStubbing[Future[HttpResponse]] =
      when(mockHttpClient.DELETE[HttpResponse](eqTo(deleteIntegrationsUrl(id)), *)(*, *, *)).thenReturn(Future.successful(response))


    def httpCallToDeleteApiWillFailWithNotFound(exception: Throwable, id: IntegrationId): ScalaOngoingStubbing[Future[HttpResponse]] =
      when(mockHttpClient.DELETE[HttpResponse](eqTo(deleteIntegrationsUrl(id)), *)(*, *, *)).thenReturn(Future.failed(exception))

    def httpCallToDeleteByPlatformWillSucceed(response: DeleteIntegrationsResponse, platform: String):
    ScalaOngoingStubbing[Future[DeleteIntegrationsResponse]] =
      when(mockHttpClient.DELETE[DeleteIntegrationsResponse](eqTo(deleteIntegrationsByPlatformUrl(platform)), *)(*, *, *))
        .thenReturn(Future.successful(response))

    def httpCallToDeleteByPlatformWillFail(exception: Throwable, platform: String): ScalaOngoingStubbing[Future[DeleteIntegrationsResponse]] =
      when(mockHttpClient.DELETE[DeleteIntegrationsResponse](eqTo(deleteIntegrationsByPlatformUrl(platform)), *)(*, *, *)).thenReturn(Future.failed(exception))

  }

  "IntegrationCatalogueConnector send" should {

      val request: ApiPublishRequest = ApiPublishRequest(Some("publisherRef"), PlatformType.CORE_IF, SpecificationType.OAS_V3, "{}")

    "return successful result" in new SetUp {
      httpCallToPublishWillSucceedWithResponse(
        PublishResult(isSuccess = true,
          Some(PublishDetails(isUpdate = true, IntegrationId(UUID.randomUUID()),  request.publisherReference.getOrElse(""), request.platformType)), List.empty))


      val result: Either[Throwable, PublishResult] = await(connector.publishApis(request))

      result match {
        case Left(_) => fail()
        case Right(publishResult: PublishResult) => publishResult.isSuccess shouldBe true
      }

      verify(mockHttpClient).PUT(eqTo(outboundUrl), eqTo(request),
        any[Seq[(String, String)]])(any[Writes[ApiPublishRequest]], any[HttpReads[PublishResult]], headerCarrierCaptor.capture, any[ExecutionContext])

    }

    "handle exceptions" in new SetUp {
      httpCallToPublishWillFailWithException(new BadGatewayException("some error"))

      val result: Either[Throwable, PublishResult] = await(connector.publishApis(request))

      result match {
        case Left(_:BadGatewayException) => succeed
        case _ => fail()
      }

    }

  }

  "findWithFilter" should {
    "return Right when successful" in new SetUp {
      val expectedResult = List(exampleApiDetail, exampleApiDetail2)
      httpCallToFindWithFilterWillSucceedWithResponse(IntegrationResponse(2, expectedResult))

      val result: Either[Throwable, IntegrationResponse] = await(connector.findWithFilters(List(searchTerm), List.empty))

      result match {
        case Right(integrationResponse: IntegrationResponse) => integrationResponse.results shouldBe expectedResult
        case _ => fail()

      }
    }

    "handle exceptions" in new SetUp {
      httpCallToFindWithFilterWillFailWithException(new BadGatewayException("some error"))

      val result: Either[Throwable, IntegrationResponse] = await(connector.findWithFilters(List(searchTerm), List.empty))

      result match {
        case Left(_:BadGatewayException) => succeed
        case _ => fail()
      }

    }
  }

  "deleteByIntegrationId" should {

    "return true when successful and NO_CONTENT status returned" in new SetUp {
      val noContentResponse: HttpResponse = HttpResponse(NO_CONTENT, "")
      httpCallToDeleteApiWillSucceed(noContentResponse, integrationId)
      await(connector.deleteByIntegrationId(integrationId)) shouldBe true
    }

    "return false when successful but NOT_FOUND status returned" in new SetUp {
      val noContentResponse: HttpResponse = HttpResponse(NOT_FOUND, "")
      httpCallToDeleteApiWillSucceed(noContentResponse, integrationId)
      await(connector.deleteByIntegrationId(integrationId)) shouldBe false
    }

    "return false when NotFoundException is thrown" in new SetUp {
      httpCallToDeleteApiWillFailWithNotFound(new NotFoundException(s"api with publisherReference: ${integrationId.value} not found"), integrationId)
      await(connector.deleteByIntegrationId(integrationId)) shouldBe false
    }

  }

  "deleteByPlatform" should {

    "return DeleteIntegrationsSuccess when successful and OK status returned" in new SetUp {
      val response: DeleteIntegrationsResponse = DeleteIntegrationsResponse(1)
      httpCallToDeleteByPlatformWillSucceed(response, "CORE_IF")
      await(connector.deleteByPlatform(PlatformType.CORE_IF)) shouldBe DeleteIntegrationsSuccess(DeleteIntegrationsResponse(1))
    }

    "return DeleteIntegrationsFailure with error message when error is returned from backend" in new SetUp {
  
      httpCallToDeleteByPlatformWillFail(new InternalServerException("Internal Server Error"), "CORE_IF")
      await(connector.deleteByPlatform(PlatformType.CORE_IF)) shouldBe DeleteIntegrationsFailure("Internal Server Error")
    }
    
  }
  
}
