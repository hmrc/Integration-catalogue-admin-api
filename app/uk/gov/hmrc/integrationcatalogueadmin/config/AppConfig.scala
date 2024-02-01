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

package uk.gov.hmrc.integrationcatalogueadmin.config

import javax.inject.{Inject, Singleton}

import play.api.Configuration
import play.api.i18n.Lang
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType._

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {
  val welshLanguageSupportEnabled: Boolean = config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  val en: String            = "en"
  val cy: String            = "cy"
  val defaultLanguage: Lang = Lang(en)

  val integrationCatalogueUrl: String           = servicesConfig.baseUrl("integration-catalogue")
  val authorizationKey: String                  = servicesConfig.getString("authorizationKey")
  val cmaAuthorizationKey: String               = servicesConfig.getString("auth.authKey.cma")
  val apiPlatformAuthorizationKey: String       = servicesConfig.getString("auth.authKey.apiPlatform")
  val coreIfAuthorizationKey: String            = servicesConfig.getString("auth.authKey.coreIF")
  val desAuthorizationKey: String               = servicesConfig.getString("auth.authKey.DES")
  val cdsClassicAuthorizationKey: String        = servicesConfig.getString("auth.authKey.cdsClassic")
  val transactionEngineAuthorizationKey: String = servicesConfig.getString("auth.authKey.transactionEngine")
  val sdesAuthorizationKey: String              = servicesConfig.getString("auth.authKey.SDES")
  val digiAuthorizationKey: String              = servicesConfig.getString("auth.authKey.DIGI")
  val dapiAuthorizationKey: String              = servicesConfig.getString("auth.authKey.DAPI")
  val cipAuthorizationKey: String               = servicesConfig.getString("auth.authKey.CIP")
  val hipAuthorizationKey: String               = servicesConfig.getString("auth.authKey.HIP")

  val authPlatformMap: Map[PlatformType, String] = Map(
    CMA                -> cmaAuthorizationKey,
    API_PLATFORM       -> apiPlatformAuthorizationKey,
    CORE_IF            -> coreIfAuthorizationKey,
    DES                -> desAuthorizationKey,
    CDS_CLASSIC        -> cdsClassicAuthorizationKey,
    TRANSACTION_ENGINE -> transactionEngineAuthorizationKey,
    SDES               -> sdesAuthorizationKey,
    DIGI               -> digiAuthorizationKey,
    DAPI               -> dapiAuthorizationKey,
    CIP                -> cipAuthorizationKey,
    HIP                -> hipAuthorizationKey
  )

  val internalAuthToken: String = config.get[String]("internal-auth.token")

}
