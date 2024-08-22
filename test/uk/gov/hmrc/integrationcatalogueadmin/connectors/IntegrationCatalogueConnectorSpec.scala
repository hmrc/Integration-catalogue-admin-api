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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{BadGatewayException, *}
import uk.gov.hmrc.integrationcatalogue.models.*
import uk.gov.hmrc.integrationcatalogue.models.common.*
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters.*
import uk.gov.hmrc.integrationcatalogueadmin.AwaitTestSupport
import uk.gov.hmrc.integrationcatalogueadmin.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadmin.data.ApiDetailTestData

import java.net.{URL, URLEncoder}
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class IntegrationCatalogueConnectorSpec extends AnyWordSpec
    with Matchers
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach
    with AwaitTestSupport
    with ApiDetailTestData {

  private val mockHttpClient                = mock[HttpClientV2]
  private val mockAppConfig                 = mock[AppConfig]
  private val requestBuilder                = mock[RequestBuilder]
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier    = HeaderCarrier(authorization = Some(Authorization("test-inbound-token")))
  private val fakeDomain                    = "http://foo.bar"
  private val internalAuthToken             = "test-internal-auth-token"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient)
    reset(mockAppConfig)
    reset(requestBuilder)
    when(mockAppConfig.integrationCatalogueUrl).thenReturn(fakeDomain)
    when(mockAppConfig.internalAuthToken).thenReturn(internalAuthToken)
  }

  trait SetUp {
    val headerCarrierCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])

    val connector                                         = new IntegrationCatalogueConnector(
      mockHttpClient,
      mockAppConfig
    )
    val integrationId: IntegrationId                      = IntegrationId(UUID.fromString("2840ce2d-03fa-46bb-84d9-0299402b7b32"))
    val searchTerm                                        = "API-1001"
    val outboundUrl                                       = s"${fakeDomain}/integration-catalogue/apis/publish"
    val findWithFilterUrl                                 = s"${fakeDomain}/integration-catalogue/integrations"
    val reportUrl                                         = s"${fakeDomain}/integration-catalogue/report"
    def deleteIntegrationsUrl(id: IntegrationId)          = s"${fakeDomain}/integration-catalogue/integrations/${id.value}"
    def deleteIntegrationsByPlatformUrl(platform: String) = s"${fakeDomain}/integration-catalogue/integrations?platformFilter=$platform"

    def httpCallToPublishWillSucceedWithResponse(response: PublishResult): Unit =
      when(mockHttpClient.put(eqTo(URL(outboundUrl)))(any[HeaderCarrier]))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any())(using any(), any(), any())).thenReturn(requestBuilder)
      when(requestBuilder.transform(any())).thenReturn(requestBuilder)
      when(requestBuilder.execute(using any(), any())).thenReturn(Future.successful(response))

    def httpCallToPublishWillFailWithException(exception: Throwable): Unit =
      when(mockHttpClient.put(eqTo(URL(outboundUrl)))(any[HeaderCarrier]))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any())(using any(), any(), any())).thenReturn(requestBuilder)
      when(requestBuilder.transform(any())).thenReturn(requestBuilder)
      when(requestBuilder.execute(using any(), any())).thenReturn(Future.failed(exception))

    def httpCallToFindWithFilterWillSucceedWithResponse(response: IntegrationResponse): Unit =
      httpCallToGETEndpointWillSucceed(response, findWithFilterUrl, Seq(("searchTerm", searchTerm)))

    def httpCallToFindWithFilterWillFailWithException(exception: Throwable): Unit =
      httpCallToGETEndpointWillFail(exception, findWithFilterUrl, Seq(("searchTerm", searchTerm)))

    def httpCallToDeleteApiWillSucceed(response: HttpResponse, id: IntegrationId): Unit =
      when(mockHttpClient.delete(eqTo(URL(deleteIntegrationsUrl(id))))(any[HeaderCarrier]))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
      when(requestBuilder.transform(any())).thenReturn(requestBuilder)
      when(requestBuilder.execute(using any(), any())).thenReturn(Future.successful(response))

    def httpCallToDeleteApiWillFail[A](exception: Throwable, urlParam: A, urlResolveFunction: A => String): Unit =
      when(mockHttpClient.delete(eqTo(URL(urlResolveFunction(urlParam))))(any[HeaderCarrier]))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
      when(requestBuilder.transform(any())).thenReturn(requestBuilder)
      when(requestBuilder.execute(using any(), any())).thenReturn(Future.failed(exception))

    def httpCallToDeleteByPlatformWillSucceed(response: DeleteIntegrationsResponse, platform: String): Unit =
      when(mockHttpClient.delete(eqTo(URL(deleteIntegrationsByPlatformUrl(platform))))(any[HeaderCarrier]))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
      when(requestBuilder.transform(any())).thenReturn(requestBuilder)
      when(requestBuilder.execute(using any(), any())).thenReturn(Future.successful(response))

    def httpCallToCatalogueReportWillSucceed(): Unit = {
      httpCallToGETEndpointWillSucceed(List.empty[IntegrationPlatformReport], reportUrl, Seq.empty)
    }

    def httpCallToCatalogueReportWillFail(exception: Throwable): Unit = {
      httpCallToGETEndpointWillFail(exception, reportUrl, Seq.empty)
    }

    private def queryString(queryParams: Seq[(String, String)]) =
      queryParams.headOption.map(_ =>
        queryParams.map { case (name, value) => s"$name=${URLEncoder.encode(value, StandardCharsets.UTF_8.name)}" }.mkString("?", "&", "")
      ).getOrElse("")

    def httpCallToGETEndpointWillSucceed[A](returnValue: A, urlString: String, queryParams: Seq[(String, String)]): Unit =
      when(mockHttpClient.get(eqTo(URL(s"$urlString${queryString(queryParams)}")))(any[HeaderCarrier]))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
      when(requestBuilder.transform(any())).thenReturn(requestBuilder)
      when(requestBuilder.execute(using any(), any())).thenReturn(Future.successful(returnValue))

    def httpCallToGETEndpointWillFail[A](exception: Throwable, url: String, queryParams: Seq[(String, String)]): Unit =
      when(mockHttpClient.get(eqTo(URL(s"$url${queryString(queryParams)}")))(any[HeaderCarrier]))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
      when(requestBuilder.transform(any())).thenReturn(requestBuilder)
      when(requestBuilder.execute(using any(), any())).thenReturn(Future.failed(exception))
  }

  "IntegrationCatalogueConnector send" should {

    val request: ApiPublishRequest = ApiPublishRequest(Some("publisherRef"), PlatformType.CORE_IF, SpecificationType.OAS_V3, "{}")

    "return successful result" in new SetUp {
      httpCallToPublishWillSucceedWithResponse(
        PublishResult(
          isSuccess = true,
          Some(PublishDetails(isUpdate = true, IntegrationId(UUID.randomUUID()), request.publisherReference.getOrElse(""), request.platformType)),
          List.empty
        )
      )

      val result: Either[Throwable, PublishResult] = await(connector.publishApis(request))

      result match {
        case Left(_)                             => fail()
        case Right(publishResult: PublishResult) => publishResult.isSuccess shouldBe true
      }

      verify(mockHttpClient).put(eqTo(URL(outboundUrl)))(any[HeaderCarrier])
      verify(requestBuilder).withBody(eqTo(Json.toJson(request)))(using any, any, any)

    }

    "handle exceptions" in new SetUp {
      val errorMessage = "some error"
      httpCallToPublishWillFailWithException(new BadGatewayException(errorMessage))

      val result: Either[Throwable, PublishResult] = await(connector.publishApis(request))

      result match {
        case Left(e: BadGatewayException) => e.getMessage shouldBe "some error"
        case _                            => fail()
      }

    }

  }

  "findWithFilter" should {

    "return Right when successful" in new SetUp {
      val expectedResult            = List(exampleApiDetail, exampleApiDetail2)
      httpCallToFindWithFilterWillSucceedWithResponse(IntegrationResponse(2, expectedResult))
      val filter: IntegrationFilter = IntegrationFilter(searchText = List(searchTerm), platforms = List.empty)

      await(connector.findWithFilters(filter)) match {
        case Right(integrationResponse: IntegrationResponse) => integrationResponse.results shouldBe expectedResult
        case _                                               => fail()
      }
    }

    "handle exceptions" in new SetUp {
      httpCallToFindWithFilterWillFailWithException(new BadGatewayException("some error"))
      val filter: IntegrationFilter = IntegrationFilter(searchText = List(searchTerm), platforms = List.empty)

      await(connector.findWithFilters(filter)) match {
        case Left(e: BadGatewayException) => e.getMessage shouldBe "some error"
        case _                            => fail()
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
      httpCallToDeleteApiWillFail(new NotFoundException(s"api with publisherReference: ${integrationId.value} not found"), integrationId, deleteIntegrationsUrl)
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
      httpCallToDeleteApiWillFail(new InternalServerException("Internal Server Error"), "CORE_IF", deleteIntegrationsByPlatformUrl)
      await(connector.deleteByPlatform(PlatformType.CORE_IF)) shouldBe DeleteIntegrationsFailure("Internal Server Error")
    }

  }

  "catalogueReport" should {
    "return platform reports when successful" in new SetUp {
      httpCallToCatalogueReportWillSucceed()
      await(connector.catalogueReport()) shouldBe Right(List.empty)
    }

    "handle exceptions correctly" in new SetUp {
      httpCallToCatalogueReportWillFail(new BadGatewayException("some error"))
      await(connector.catalogueReport()) match {
        case Left(e: BadGatewayException) => e.getMessage shouldBe "some error"
        case _                            => fail()
      }
    }
  }

}
