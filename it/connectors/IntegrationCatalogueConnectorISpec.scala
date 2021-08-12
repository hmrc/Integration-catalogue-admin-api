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

package connectors

import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.libs.ws.WSClient
import support.{IntegrationCatalogueConnectorStub, ServerBaseISpec}
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._

import java.util.UUID
import uk.gov.hmrc.integrationcatalogueadmin.connectors.IntegrationCatalogueConnector
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.CORE_IF
import uk.gov.hmrc.integrationcatalogueadmin.data.ApiDetailTestData

class IntegrationCatalogueConnectorISpec extends ServerBaseISpec with ApiDetailTestData with BeforeAndAfterEach with IntegrationCatalogueConnectorStub {

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "metrics.enabled" -> true,
        "auditing.enabled" -> false,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort,
        "microservice.services.integration-catalogue.host" -> wireMockHost,
        "microservice.services.integration-catalogue.port" -> wireMockPort
      )


  implicit val hc: HeaderCarrier = HeaderCarrier()
  val url = s"http://localhost:$port/integration-catalogue-admin-frontend"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  trait Setup {
    val integrationId: IntegrationId =  IntegrationId(UUID.fromString("b4e0c3ca-c19e-4c88-adf9-0e4af361076e"))
    val publisherReference =  "XXX-YYY-ZZZMonthly-pull"
    def createBackendPublishResponse(isSuccess: Boolean, isUpdate: Boolean): PublishResult = {
        val publishDetails = if(isSuccess) Some(PublishDetails(isUpdate, integrationId, publisherReference, PlatformType.CORE_IF)) else None
        val publishErrors = if(isSuccess) List.empty else List(PublishError(10000, "Some Error Message"))
        PublishResult(isSuccess, publishDetails, publishErrors)
    }
  val dateValue: DateTime = DateTime.parse("04/11/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"));

    val fileTransferPublishRequestObj: FileTransferPublishRequest = FileTransferPublishRequest(
      fileTransferSpecificationVersion = "1.0",
      publisherReference = publisherReference,
      title = publisherReference,
      description = "A file transfer",
      platformType = PlatformType.CORE_IF,
      lastUpdated =  dateValue,
      reviewedDate = reviewedDate,
      contact = ContactInformation(Some("Core IF Team"), Some("example@gmail.com")),
      sourceSystem = List("XXX"),
      targetSystem = List("YYY"),
      transports = List.empty,
      fileTransferPattern = "Corporate to corporate"
    )

    val integrationResponse: IntegrationResponse = IntegrationResponse(1, List(exampleApiDetail))

    val objInTest: IntegrationCatalogueConnector =  app.injector.instanceOf[IntegrationCatalogueConnector]

  }


  "IntegrationCatalogueConnector" when {

    "findById" should {

      "return a right with an Integration Detail when returned from backend" in new Setup{

          primeIntegrationCatalogueServiceGetByIdWithBody(OK, Json.toJson(exampleApiDetail.asInstanceOf[IntegrationDetail]).toString, exampleApiDetail.id)

          val result: Either[Throwable, IntegrationDetail] = await(objInTest.findByIntegrationId(exampleApiDetail.id))
          result match {
            case Right(_) => succeed
            case _ => fail
          }
      }
       "return Left when any error from backend" in new Setup{

          primeIntegrationCatalogueServiceGetByIdReturnsBadRequest(exampleApiDetail.id)

          val result: Either[Throwable, IntegrationDetail] = await(objInTest.findByIntegrationId(exampleApiDetail.id))
          result match {
            case Left(_) => succeed
            case _ => fail
          }
      }


    }

    "findWithFilter" should {
      "return Right with IntegrationResponse " in new Setup {
        primeIntegrationCatalogueServiceFindWithFilterWithBody(OK, Json.toJson(integrationResponse).toString(), "?searchTerm=API-1001&platformFilter=CORE_IF&backendsFilter=HODS")
        val result: Either[Throwable, IntegrationResponse] = await(objInTest.findWithFilters(IntegrationFilter(searchText = List("API-1001"), platforms = List(CORE_IF), backends = List("HODS"))))
            result match {
              case Right(_) => succeed
              case _ => fail
            }

      }
      
      "return Left with Bad Request " in new Setup {
        primeIntegrationCatalogueServiceFindWithFilterWithBadRequest("?searchTerm=API-1001")
        val result: Either[Throwable, IntegrationResponse] = await(objInTest.findWithFilters(IntegrationFilter(searchText = List("API-1001"), platforms = List.empty)))
            result match {
              case Left(_) => succeed
              case _ => fail
            }

        }
    }

    "publishFileTransfer" should {

      "return a Right containing a publish result when integration catalogue returns a publish result" in new Setup{

        val backendResponse: PublishResult = createBackendPublishResponse(isSuccess = true, isUpdate = false)
        primeIntegrationCatalogueServicePutWithBody("/integration-catalogue/filetransfer/publish", OK, Json.toJson(backendResponse).toString)

        val publishResult: Either[Throwable, PublishResult] = await(objInTest.publishFileTransfer(fileTransferPublishRequestObj))
        publishResult match {
          case Right(result: PublishResult) =>
            result.isSuccess mustBe true
            result.errors.isEmpty mustBe true
            result.publishDetails.isDefined mustBe true
            val publishDetails = result.publishDetails.head
            publishDetails.isUpdate mustBe false
            publishDetails.integrationId mustBe integrationId
            publishDetails.platformType mustBe fileTransferPublishRequestObj.platformType
            publishDetails.publisherReference mustBe publisherReference
          case Left(_) => fail
        }
      }


      "return a Left when we receive a failure from the integration connector" in new Setup{
        primeIntegrationCatalogueServicePutReturnsBadRequest("/integration-catalogue/filetransfer/publish")

        val publishResult: Either[Throwable, PublishResult] = await(objInTest.publishFileTransfer(fileTransferPublishRequestObj))
        publishResult match {
          case Right(_: PublishResult) => fail
          case Left(_) => succeed
        }
      }

  }
}

}