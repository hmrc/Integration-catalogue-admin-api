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


import _root_.uk.gov.hmrc.http.HttpErrorFunctions
import play.api.mvc.{ActionFilter, Request, Result}
import uk.gov.hmrc.integrationcatalogueadmin.utils.ValidateParameters

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ValidateQueryParamKeyAction @Inject()()(implicit ec: ExecutionContext)
  extends ActionFilter[Request] with HttpErrorFunctions with ValidateParameters {
  override def executionContext: ExecutionContext = ec

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    val validKeys = List("platformFilter", "searchTerm", "platform")
    val queryParamKeys = request.queryString.keys

    Future.successful(validateQueryParamKey(validKeys, queryParamKeys))
  }


}