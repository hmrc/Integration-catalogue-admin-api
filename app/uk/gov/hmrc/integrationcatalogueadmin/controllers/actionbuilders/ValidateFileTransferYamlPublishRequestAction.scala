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

package uk.gov.hmrc.integrationcatalogueadmin.controllers.actionbuilders

import play.api.libs.json.{Json => ScalaJson}
import play.api.mvc.{ActionRefiner, Request, Result}
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.integrationcatalogue.models.ErrorResponse
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.integrationcatalogueadmin.models.FileTransferYamlRequest
import io.circe.{ParsingFailure, Json => CirceJson}
import uk.gov.hmrc.integrationcatalogue.models.FileTransferPublishRequest
import uk.gov.hmrc.integrationcatalogue.models.ErrorResponseMessage
import io.circe.yaml.parser
import uk.gov.hmrc.integrationcatalogueadmin.utils.JsonUtils

@Singleton
class ValidateFileTransferYamlPublishRequestAction @Inject() (implicit ec: ExecutionContext)
    extends ActionRefiner[Request, FileTransferYamlRequest]
    with HttpErrorFunctions
    with JsonUtils {
  actionName =>

  override def executionContext: ExecutionContext = ec

  override def refine[A](request: Request[A]): Future[Either[Result, FileTransferYamlRequest[A]]] = Future.successful {
    request.contentType match {
      case Some("application/x-yaml") =>
        println(request.body.toString)
        parseYamlUsingCirce(request.body.toString()) match {
          case Some(fileTransferPublishRequest) => Right(FileTransferYamlRequest[A](fileTransferPublishRequest, request))
          case None => Left(BadRequest(ScalaJson.toJson(ErrorResponse(List(ErrorResponseMessage("Error parsing yaml"))))))
        }
      case _                          => Left(BadRequest(ScalaJson.toJson(ErrorResponse(List(ErrorResponseMessage("Invalid Content-Type. Is must be application/x-yaml"))))))
    }
  }

  private def parseYamlUsingCirce(payload: String): Option[FileTransferPublishRequest] =
    parser.parse(payload) match {
      case Left(e: ParsingFailure) => None
      case Right(json: CirceJson)  => validateAndExtractJsonString[FileTransferPublishRequest](json.toString())
    }
  

}
