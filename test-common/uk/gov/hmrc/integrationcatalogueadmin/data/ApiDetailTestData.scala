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

package uk.gov.hmrc.integrationcatalogueadmin.data

import java.util.UUID

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, Maintainer, PlatformType, SpecificationType}

trait ApiDetailTestData {

  val filename               = "API-1001_1.1.0.yaml"
  val fileContents           = "{}"
  val uuid: UUID             = UUID.fromString("28c0bd67-4176-42c7-be13-53be98a4db58")
  val dateValue: DateTime    = DateTime.parse("04/11/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"))
  val reviewedDate: DateTime = DateTime.parse("24/11/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"))

  val apiPlatformMaintainer: Maintainer = Maintainer("API Platform Team", "#team-api-platform-sup")
  val coreIfMaintainer: Maintainer      = Maintainer("IF Team", "N/A", List.empty)

  val jsonMediaType = "application/json"

  val schema1: DefaultSchema = DefaultSchema(
    name = Some("referenceNumber"),
    not = None,
    `type` = Some("string"),
    pattern = Some("^[A-Z](ARN)[0-9]{7}$"),
    description = None,
    ref = None,
    properties = List.empty,
    `enum` = List.empty,
    required = List.empty,
    stringAttributes = None,
    numberAttributes = None,
    minProperties = None,
    maxProperties = None,
    format = None,
    default = None,
    example = None
  )

  val schema2: DefaultSchema = DefaultSchema(
    name = Some("referenceNumber"),
    not = None,
    `type` = Some("object"),
    pattern = None,
    description = None,
    ref = None,
    properties = List(schema1),
    `enum` = List.empty,
    required = List.empty,
    stringAttributes = None,
    numberAttributes = None,
    minProperties = None,
    maxProperties = None,
    format = None,
    default = None,
    example = None
  )

  val headerRef =
    Header(
      name = "x-platform-type",
      ref = Some("components/headers/platformType")
    )

  val platformTypeHeader =
    Header(
      name = "PlatformType",
      description = Some("this is platform type"),
      required = Some(true),
      schema = Some(
        DefaultSchema(
          name = Some("PlatFormType"),
          `type` = Some("string"),
          pattern = Some("^[A-Z](ARN)[0-9]{7}$")
        )
      )
    )

  val exampleRequest1name      = "example request 1"
  val exampleRequest1Body      = "{\"someValue\": \"abcdefg\"}"
  val exampleRequest1: Example = Example(exampleRequest1name, exampleRequest1Body)
  val exampleResponse1         = new Example("example response name", "example response body")

  val request: Request = Request(description = Some("request"), schema = Some(schema1), mediaType = Some(jsonMediaType), examples = List(exampleRequest1))

  val response: Response =
    Response(
      statusCode = "200",
      description = Some("response"),
      schema = Some(schema2),
      mediaType = Some("application/json"),
      examples = List(exampleResponse1),
      headers = List(headerRef)
    )

  val reqHeaderRef = Parameter(Some("userId"), Some("components/parameters/userId"))

  val reqHeaderParameter = Parameter(
    name = Some("userId"),
    in = Some("header"),
    description = Some("The UserId"),
    required = Some(true),
    deprecated = Some(false),
    schema = Some(
      DefaultSchema(
        `type` = Some("string"),
        pattern = Some("^[A-Z](ARN)[0-9]{7}$")
      )
    )
  )

  val endpointGetMethod: EndpointMethod = EndpointMethod("GET", Some("operationId"), Some("some summary"), Some("some description"), None, List(response))

  val endpointPutMethod: EndpointMethod =
    EndpointMethod(
      "PUT",
      Some("operationId2"),
      Some("some summary"),
      Some("some description"),
      Some(request),
      List.empty,
      List(reqHeaderRef)
    )
  val endpoint1: Endpoint               = Endpoint("/some/url", List(endpointGetMethod))
  val endpoint2: Endpoint               = Endpoint("/some/url", List(endpointPutMethod))

  val endpoints: List[Endpoint] = List(endpoint1, endpoint2, Endpoint("/some/url", List.empty))

  val exampleApiDetail: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("e2e4ce48-29b0-11eb-adc1-0242ac120002")),
    publisherReference = "API-1001",
    title = "API title",
    description = "API description",
    lastUpdated = dateValue,
    platform = PlatformType.CORE_IF,
    maintainer = coreIfMaintainer,
    version = "1.1.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("ETMP"),
    endpoints = endpoints,
    components = Components(List(schema1), List(platformTypeHeader), List(reqHeaderParameter)),
    shortDescription = None,
    apiStatus = ApiStatus.LIVE,
    reviewedDate = reviewedDate
  )

  val exampleApiDetail2: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("28c0bd67-4176-42c7-be13-53be98a4db58")),
    publisherReference = "API-1002",
    title = "API title",
    description = "API description",
    lastUpdated = dateValue,
    platform = PlatformType.CORE_IF,
    maintainer = coreIfMaintainer,
    version = "1.2.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("ETMP"),
    endpoints = endpoints,
    components = Components(List(schema1), List.empty),
    shortDescription = None,
    apiStatus = ApiStatus.LIVE,
    reviewedDate = reviewedDate
  )

  val exampleApiDetail3: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("6d5a98fc-a33a-11eb-bcbc-0242ac130002")),
    publisherReference = "API-1003",
    title = "API title",
    description = "API description",
    lastUpdated = dateValue,
    platform = PlatformType.CDS_CLASSIC,
    maintainer = coreIfMaintainer,
    version = "1.2.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("ETMP"),
    endpoints = endpoints,
    components = Components(List(schema1), List.empty),
    shortDescription = Some("short description"),
    apiStatus = ApiStatus.LIVE,
    reviewedDate = reviewedDate
  )

  val exampleFileTransfer: FileTransferDetail =
    FileTransferDetail(
      IntegrationId(UUID.fromString("e2e4ce48-29b0-11eb-adc1-0242ac120002")),
      fileTransferSpecificationVersion = "0.1",
      publisherReference = "API-1004",
      title = "File transfer title",
      description = "File transfer description",
      lastUpdated = dateValue,
      reviewedDate = reviewedDate,
      platform = PlatformType.CORE_IF,
      maintainer = coreIfMaintainer,
      sourceSystem = List("source"),
      targetSystem = List("target"),
      transports = List("S3"),
      fileTransferPattern = "pattern1"
    )

}
