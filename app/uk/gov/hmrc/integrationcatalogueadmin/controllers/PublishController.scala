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
import play.api.libs.Files
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.{Request => _, _}
import uk.gov.hmrc.integrationcatalogueadmin.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadmin.controllers.actionbuilders._
import uk.gov.hmrc.integrationcatalogueadmin.models.{HeaderKeys, ValidatedApiPublishRequest}
import uk.gov.hmrc.integrationcatalogueadmin.services.PublishService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogueadmin.utils.JsonUtils

@Singleton
class PublishController @Inject() (
    appConfig: AppConfig,
    cc: ControllerComponents,
    publishService: PublishService,
    validateApiPublishRequest: ValidateApiPublishRequestAction,
    validateAuthorizationHeaderAction: ValidateAuthorizationHeaderAction,
    validateFileTransferYamlPublishRequestAction: ValidateFileTransferYamlPublishRequestAction,
    playBodyParsers: PlayBodyParsers
  )(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging
    with JsonUtils {

  implicit val config: AppConfig = appConfig

  def publishFileTransferJson(): Action[JsValue] =
    (Action andThen validateAuthorizationHeaderAction).async(playBodyParsers.json) { implicit request =>
      val platformHeader = request.headers.get(HeaderKeys.platformKey).getOrElse("")
      handlepublishFileTransferRequest(validateAndExtractJsonString[FileTransferPublishRequest](request.body.toString()), platformHeader)
    }

  def publishFileTransferYaml(): Action[String] =
    (Action andThen validateAuthorizationHeaderAction
      andThen validateFileTransferYamlPublishRequestAction).async(playBodyParsers.tolerantText) { implicit request =>
      val platformHeader = request.headers.get(HeaderKeys.platformKey).getOrElse("")
      handlepublishFileTransferRequest(Some(request.fileTransferRequest), platformHeader)
    }

  private def handlepublishFileTransferRequest(mayBeRequest: Option[FileTransferPublishRequest], platformHeader: String)(implicit hc: HeaderCarrier) = {
    mayBeRequest match {
      case Some(validBody) => if (validatePlatformTypesMatch(validBody, platformHeader)) publishService.publishFileTransfer(validBody).map(handlePublishResult)
        else Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Invalid request body - platform type mismatch"))))))
      case None            => logger.error("Invalid request body, must be a valid publish request")
        Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Invalid request body"))))))
    }
  }

  private def validatePlatformTypesMatch(fileTransferRequest: FileTransferPublishRequest, platformHeader: String) = {
    fileTransferRequest.platformType.entryName.equalsIgnoreCase(platformHeader)
  }

  def publishApi(): Action[MultipartFormData[Files.TemporaryFile]] = (Action andThen
    validateAuthorizationHeaderAction andThen
    validateApiPublishRequest).async(playBodyParsers.multipartFormData) {
    implicit request: ValidatedApiPublishRequest[MultipartFormData[Files.TemporaryFile]] =>
      request.body.file("selectedFile") match {
        case None               =>
          logger.info("selectedFile is missing from requestBody")
          Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("selectedFile is missing from requestBody"))))))
        case Some(selectedFile) =>
          val bufferedSource = Source.fromFile(selectedFile.ref.path.toFile)
          val fileContents = bufferedSource.getLines.mkString("\r\n")
          bufferedSource.close()
          publishService.publishApi(request.publisherReference, request.platformType, request.specificationType, fileContents)
            .map(handlePublishResult)
      }

  }

  private def handlePublishResult(result: Either[Throwable, PublishResult]) = {
    result match {
      case Right(publishResult) =>
        publishResult.publishDetails match {
          case Some(details) =>
            val resultAsJson = Json.toJson(PublishDetails.toPublishResponse(details))
            if (details.isUpdate) Ok(resultAsJson) else Created(resultAsJson)
          case None          => if (publishResult.errors.nonEmpty) {
              BadRequest(Json.toJson(ErrorResponse(publishResult.errors.map(x => ErrorResponseMessage(x.message)))))
            } else {
              BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Unexpected response from /integration-catalogue")))))
            }
        }
      case Left(errorResult)    =>
        BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"Unexpected response from /integration-catalogue: ${errorResult.getMessage}")))))
    }
  }

}
