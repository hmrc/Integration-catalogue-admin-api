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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, PlatformType}
import uk.gov.hmrc.integrationcatalogue.models.{Request => _, _}
import uk.gov.hmrc.integrationcatalogueadmin.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadmin.controllers.actionbuilders.ValidateDeleteByPlatformAction._
import uk.gov.hmrc.integrationcatalogueadmin.controllers.actionbuilders.{
  ValidateAuthorizationHeaderAction,
  ValidateIntegrationIdAgainstParametersAction,
  ValidateQueryParamKeyAction}
import uk.gov.hmrc.integrationcatalogueadmin.models.IntegrationDetailRequest
import uk.gov.hmrc.integrationcatalogueadmin.services.IntegrationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}



@Singleton
class IntegrationController @Inject()(appConfig: AppConfig,
                                      integrationService: IntegrationService,
                                      validateQueryParamKeyAction: ValidateQueryParamKeyAction,
                                      validateAuthorizationHeaderAction: ValidateAuthorizationHeaderAction,
                                      validateIntegrationIdAgainstPlatformTypeAction: ValidateIntegrationIdAgainstParametersAction,
                                      cc: ControllerComponents)
                                 (implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  implicit val config: AppConfig = appConfig

  def findWithFilters(searchTerm: List[String], platformFilter: List[PlatformType]) : Action[AnyContent] =
    (Action andThen validateQueryParamKeyAction).async { implicit request =>
    integrationService.findWithFilters(searchTerm, platformFilter)
     .map {
      case Right(response) => Ok(Json.toJson(response))
      case Left(error: Throwable) =>
        logger.error(s"findWithFilters error integration-catalogue ${error.getMessage}")
        InternalServerError(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"Unable to process your request")))))
    }
}

 def findByIntegrationId(id: IntegrationId): Action[AnyContent] =
   Action.async { implicit request =>
    integrationService.findByIntegrationId(id)map {
        case Right(response) => Ok(Json.toJson(response))
        case Left(error: UpstreamErrorResponse) if error.statusCode==NOT_FOUND =>
          NotFound(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"findByIntegrationId: The requested resource could not be found.")))))
        case Left(error: Throwable) =>
          BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage( s"findByIntegrationId error integration-catalogue ${error.getMessage}")))))
      }
 }

  def deleteByIntegrationId(integrationId: IntegrationId): Action[AnyContent] =
    (Action andThen validateAuthorizationHeaderAction
    andThen integrationDetailActionRefiner(integrationId)
    andThen validateIntegrationIdAgainstPlatformTypeAction).async { implicit request =>
      integrationService.deleteByIntegrationId(integrationId).map {
        case true => NoContent
        case false => InternalServerError(Json.toJson(ErrorResponse(List(ErrorResponseMessage("InternalServerError from integration-catalogue")))))
      }
    }

  def deleteByPlatform(platformFilter: List[PlatformType]): Action[AnyContent] =
    (Action andThen validateAuthorizationHeaderAction
    andThen ValidatePlatformTypeParams(platformFilter)).async { implicit request =>
      integrationService.deleteByPlatform(platformFilter.head).map {
        case DeleteIntegrationsSuccess(result) => Ok(Json.toJson(result))
        case DeleteIntegrationsFailure(errorMessage) => InternalServerError(Json.toJson(ErrorResponse(List(ErrorResponseMessage(errorMessage)))))
      }
    }

    private def integrationDetailActionRefiner(integrationId: IntegrationId)
                             (implicit ec: ExecutionContext) : ActionRefiner[Request, IntegrationDetailRequest] = {
      new ActionRefiner[Request, IntegrationDetailRequest] {

        override def executionContext: ExecutionContext = ec

        implicit def hc(implicit request: Request[_]): HeaderCarrier = {
          HeaderCarrierConverter.fromRequestAndSession(request, request.session)
  }
        override def refine[A](request: Request[A]): Future[Either[Result, IntegrationDetailRequest[A]]] =
         integrationService.findByIntegrationId(integrationId)(hc(request)).map {
           case Left(_) => Left(NotFound(
             Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"Integration with ID: ${integrationId.value.toString} not found"))))))
           case Right(integrationDetail: IntegrationDetail) => Right(IntegrationDetailRequest(integrationDetail, request))
         }
          
        }
      
  }
 
}
