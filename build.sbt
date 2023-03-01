// Noisycamp is a platform for booking music studios.
// Copyright (C) 2019-2020  Raphael Javaux <raphael@noisycamp.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

import sys.process._

name := "noisycamp"
organization := "com.noisycamp"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.10"

scalacOptions ++= Seq(
    "-deprecation",             // Emit warning and location for usages of deprecated
                                // APIs.
    "-feature",                 // Emit warning and location for usages of features
                                // that should be imported explicitly.
    "-unchecked",               // Enable additional warnings where generated code
                                // depends on assumptions.
    "-language:higherKinds",    // Higher kind types
    "-Xfatal-warnings",         // Fail the compilation if there are any warnings.
    "-Ywarn-dead-code",         // Warn when dead code is identified.
    "-Ywarn-numeric-widen",     // Warn when numerics are widened.

    // Play has a lot of issues with unused imports and unsued params
    // https://github.com/playframework/playframework/issues/6690
    // https://github.com/playframework/twirl/issues/105
    "-Xlint:-unused,_"
)

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
    guice, // Dependency injection library required by Play

    "com.github.cb372" %% "scalacache-caffeine" % "0.28.0",

    "com.iheart" %% "ficus" % "1.5.2",

    "com.mohiva" %% "play-silhouette" % "7.0.0",
    "com.mohiva" %% "play-silhouette-crypto-jca" % "7.0.0",
    "com.mohiva" %% "play-silhouette-password-bcrypt" % "7.0.0",
    "com.mohiva" %% "play-silhouette-persistence" % "7.0.0",

    "com.sksamuel.scrimage" % "scrimage-core" % "4.0.33",
    "com.sksamuel.scrimage" % "scrimage-webp" % "4.0.33",

    "com.stripe" % "stripe-java" % "20.94.0",

    "com.typesafe.play" %% "play-slick" % "5.1.0",
    "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0",

    "com.typesafe.play" %% "play-mailer" % "8.0.1",
    "com.typesafe.play" %% "play-mailer-guice" % "8.0.1",

    "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.2.0-M2",
    "com.typesafe.play" %% "play-ws-standalone-xml" % "2.2.0-M2",

    "com.typesafe.play" %% "twirl-api" % "1.5.2",

    "org.mnode.ical4j" % "ical4j" % "3.1.2",

    "org.postgresql" % "postgresql" % "42.2.8",

    "org.typelevel" %% "squants" % "1.8.3",

    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,

    "com.github.luben" % "zstd-jni" % "1.5.0-4",

    "net.codingwell" %% "scala-guice" % "5.0.1",

    "net.iakovlev" % "timeshape" % "2020d.12",

    "org.slf4j" % "slf4j-api" % "2.0.3",
    "ch.qos.logback" % "logback-classic" % "1.3.4",
    )

// Creates Webpack bundle when compiling, based on
// https://github.com/wigahluk/play-webpack.

val webpackInstall = taskKey[Unit]("Webpack install task.")
webpackInstall := { (Process("npm install", file("./app/frontend"))).! }

// update := (update dependsOn webpackInstall).value

val webpackBuild = taskKey[Unit]("Webpack build task.")
webpackBuild := { (Process("npm run build", file("./app/frontend"))).! }

val webpackBuildDev = taskKey[Unit]("Webpack build task, development mode.")
webpackBuildDev := { (Process("npm run build-dev", file("./app/frontend"))).! }

stage := (stage dependsOn webpackBuild).value
stage := (stage dependsOn webpackInstall).value
