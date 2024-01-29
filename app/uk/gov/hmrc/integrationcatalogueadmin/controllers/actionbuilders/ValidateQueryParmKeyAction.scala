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

package uk.gov.hmrc.integrationcatalogueadmin.controllers.actionbuilders

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import _root_.uk.gov.hmrc.http.HttpErrorFunctions

import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ActionFilter, Request, Result}

import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponse, ErrorResponseMessage}

import uk.gov.hmrc.integrationcatalogueadmin.utils.ValidateParameters

@Singleton
class ValidateQueryParamKeyAction @Inject() ()(implicit ec: ExecutionContext)
    extends ActionFilter[Request] with HttpErrorFunctions with ValidateParameters {
  override def executionContext: ExecutionContext = ec
  val BACKENDFILTERKEY                            = "backendsFilter"

  private def validateBackendFiltersIfPresent(request: Request[Any]): Option[Result] = {
    if (request.queryString.keys.toList.contains(BACKENDFILTERKEY)) {
      request.getQueryString(BACKENDFILTERKEY) match {
        case Some(value) if (value.nonEmpty) => None
        case _                               => Some(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("backendsFilter cannot be empty"))))))
      }
    } else None

  }

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    val validKeys      = List("platformFilter", "searchTerm", "platform", BACKENDFILTERKEY)
    val queryParamKeys = request.queryString.keys
    val result         = validateQueryParamKey(validKeys, queryParamKeys).fold(validateBackendFiltersIfPresent(request))(Some(_))
    Future.successful(result)
  }

}
