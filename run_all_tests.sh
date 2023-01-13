#!/usr/bin/env bash
sbt clean compile coverage scalastyle scalafmtAll scalafixAll test it:test component:test coverageReport
