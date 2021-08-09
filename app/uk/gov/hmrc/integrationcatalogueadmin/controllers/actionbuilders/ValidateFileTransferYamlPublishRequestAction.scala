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


import play.api.libs.json.Json
import play.api.mvc.{ActionRefiner, Request, Result}
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.integrationcatalogue.models.ErrorResponse
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.integrationcatalogueadmin.models.FileTransferYamlRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import uk.gov.hmrc.integrationcatalogue.models.FileTransferPublishRequest
import uk.gov.hmrc.integrationcatalogue.models.ErrorResponseMessage



@Singleton
class ValidateFileTransferYamlPublishRequestAction @Inject()(implicit ec: ExecutionContext)
  extends ActionRefiner[Request, FileTransferYamlRequest] with HttpErrorFunctions {
  actionName =>


  override def executionContext: ExecutionContext = ec

  override def refine[A](request: Request[A]): Future[Either[Result, FileTransferYamlRequest[A]]] = Future.successful {
    request.contentType match {
      case Some("application/x-yaml") => 
      println(request.body.toString)
      Right(FileTransferYamlRequest[A](parseYaml(request.body.toString()), request))
      case _ => Left(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Some error"))))))

    }

  }

  private def parseYaml(payload: String): FileTransferPublishRequest ={
    val om = new ObjectMapper(new YAMLFactory());
    om.findAndRegisterModules();
    om.readValue(payload, classOf[FileTransferPublishRequest])
     
  }

}
