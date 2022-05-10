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

import org.mockito.scalatest.MockitoSugar
import org.scalatest.{Assertion, BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationType.{API, FILE_TRANSFER}
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType.{API_PLATFORM, CORE_IF}
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, PlatformType}
import uk.gov.hmrc.integrationcatalogueadmin.AwaitTestSupport
import uk.gov.hmrc.integrationcatalogueadmin.connectors.IntegrationCatalogueConnector
import uk.gov.hmrc.integrationcatalogueadmin.data.ApiDetailTestData

import java.util.UUID
import scala.concurrent.Future

class IntegrationServiceSpec extends WordSpec
  with Matchers with GuiceOneAppPerSuite with MockitoSugar with ApiDetailTestData with AwaitTestSupport with BeforeAndAfterEach {

  val mockIntegrationCatalogueConnector: IntegrationCatalogueConnector = mock[IntegrationCatalogueConnector]
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIntegrationCatalogueConnector)
  }
  trait SetUp {
    val objInTest = new IntegrationService(mockIntegrationCatalogueConnector)
    val exampleIntegrationId: IntegrationId = IntegrationId(UUID.fromString("2840ce2d-03fa-46bb-84d9-0299402b7b32"))

    def validateFailureCall[A](f: Future[A]): Assertion ={
      val result: A =
        await(f)

      result match {
        case Left(_) => succeed
        case Right(_) => fail()
      }
    }
  }

  "deleteByIntegrationId" should {
    "return true from connector when deletion successful" in new SetUp {
      when(mockIntegrationCatalogueConnector.deleteByIntegrationId(eqTo(exampleIntegrationId))(*)).thenReturn(Future.successful(true))
      val result: Boolean = await(objInTest.deleteByIntegrationId(exampleIntegrationId))
      result shouldBe true
    }
  }

  "deleteByPlatform" should {
    "return DeleteIntegrationsSuccess from connector when deletion successful" in new SetUp {
      when(mockIntegrationCatalogueConnector.deleteByPlatform(eqTo(PlatformType.CORE_IF))(*))
        .thenReturn(Future.successful(DeleteIntegrationsSuccess(DeleteIntegrationsResponse(1))))
      val result: DeleteApiResult = await(objInTest.deleteByPlatform(PlatformType.CORE_IF))
      result shouldBe DeleteIntegrationsSuccess(DeleteIntegrationsResponse(1))
    }
  }


  "findWithFilter" should {
    "return a Right when successful" in new SetUp {
      val expectedResult = List(exampleApiDetail, exampleApiDetail2, exampleFileTransfer)
      when(mockIntegrationCatalogueConnector.findWithFilters(*)(*))
        .thenReturn(Future.successful(Right(IntegrationResponse(expectedResult.size, expectedResult))))

      val result: Either[Throwable, IntegrationResponse] =
        await(objInTest.findWithFilters(IntegrationFilter(searchText = List("search"), platforms = List.empty)))

      result match {
        case Left(_) => fail()
        case Right(integrationResponse: IntegrationResponse) => integrationResponse.results shouldBe expectedResult
      }
    }

    "return Left when error from connector" in new SetUp {
      when(mockIntegrationCatalogueConnector.findWithFilters(*)(*)).thenReturn(Future.successful(Left(new RuntimeException("some error"))))
      val integrationFilter: IntegrationFilter = IntegrationFilter(searchText = List("search"), platforms = List.empty)

      validateFailureCall(objInTest.findWithFilters(integrationFilter))
      verify(mockIntegrationCatalogueConnector).findWithFilters(eqTo(integrationFilter))(eqTo(hc))
    }
  }

  "findById" should {
    "return error from connector" in new SetUp {
      val id: IntegrationId = IntegrationId(UUID.randomUUID())
      when(mockIntegrationCatalogueConnector.findByIntegrationId(eqTo(id))(*)).thenReturn(Future.successful(Left(new RuntimeException("some error"))))

      validateFailureCall(objInTest.findByIntegrationId(id))
      verify(mockIntegrationCatalogueConnector).findByIntegrationId(eqTo(id))(eqTo(hc))
    }

     "return apidetail from connector when returned from backend" in new SetUp {
      val id: IntegrationId = IntegrationId(UUID.randomUUID())
      when(mockIntegrationCatalogueConnector.findByIntegrationId(eqTo(id))(*)).thenReturn(Future.successful(Right(exampleApiDetail)))

      val result: Either[Throwable, IntegrationDetail] =
        await(objInTest.findByIntegrationId(id))

      result match {
        case Right(apiDetail) => apiDetail shouldBe exampleApiDetail
        case Left(_) => fail()
      }

      verify(mockIntegrationCatalogueConnector).findByIntegrationId(eqTo(id))(eqTo(hc))
    }
  }


  "catalogueReport" should {
    "return error from connector" in new SetUp {
      when(mockIntegrationCatalogueConnector.catalogueReport()(*)).thenReturn(Future.successful(Left(new RuntimeException("some error"))))
      validateFailureCall(objInTest.catalogueReport())
      verify(mockIntegrationCatalogueConnector).catalogueReport()(eqTo(hc))
    }

    "return results from connector" in new SetUp {
      val reports = List(IntegrationPlatformReport(API_PLATFORM, API, 2),
        IntegrationPlatformReport(CORE_IF, API, 5),
        IntegrationPlatformReport(CORE_IF, FILE_TRANSFER, 2))
      when(mockIntegrationCatalogueConnector.catalogueReport()(*)).thenReturn(Future.successful(Right(reports)))
     await(objInTest.catalogueReport()) match{
       case Right(results) => results shouldBe reports
       case _ => fail()
     }
      verify(mockIntegrationCatalogueConnector).catalogueReport()(eqTo(hc))
    }
  }

}