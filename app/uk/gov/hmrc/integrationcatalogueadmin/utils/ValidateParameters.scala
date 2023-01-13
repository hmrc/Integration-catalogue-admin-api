/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.integrationcatalogueadmin.utils

import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponse, ErrorResponseMessage}

trait ValidateParameters {

  def validatePlatformType(platform: String): Option[PlatformType] = {
    if (PlatformType.values.map(_.toString()).contains(platform.toUpperCase)) {
      Some(PlatformType.withNameInsensitive(platform))
    } else {
      None
    }
  }

  def validateQueryParamKey(validKeys: List[String], queryParamKeys: Iterable[String]): Option[Result] = {
    if (!queryParamKeys.forall(validKeys.contains(_))) {
      Some(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Invalid query parameter key provided. It is case sensitive"))))))
    } else None
  }

}
