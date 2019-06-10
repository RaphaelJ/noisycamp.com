// Noisycamp is a platform for booking music studios.
// Copyright (C) 2019  Raphael Javaux <raphaeljavaux@gmail.com>
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

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
  "-deprecation",         // Emit warning and location for usages of deprecated
                          // APIs.
  "-feature",             // Emit warning and location for usages of features
                          // that should be imported explicitly.
  "-unchecked",           // Enable additional warnings where generated code
                          // depends on assumptions.
  "-Xfatal-warnings",     // Fail the compilation if there are any warnings.
  "-Ywarn-adapted-args",  // Warn if an argument list is modified to match the
                          // receiver.
  "-Ywarn-dead-code",     // Warn when dead code is identified.
  "-Ywarn-inaccessible",  // Warn about inaccessible types in method signatures.

  // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-nullary-override",
  "-Ywarn-numeric-widen", // Warn when numerics are widened.

  // Play has a lot of issues with unused imports and unsued params
  // https://github.com/playframework/playframework/issues/6690
  // https://github.com/playframework/twirl/issues/105
  "-Xlint:-unused,_"
)

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  guice, // Dependency injection library required by Play

  evolutions, jdbc, // Executes database evolutions

  "com.iheart" %% "ficus" % "1.4.3",
  "com.mohiva" %% "play-silhouette" % "5.0.7",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.7",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.7",
  "com.mohiva" %% "play-silhouette-persistence" % "5.0.7",
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8",
  "com.typesafe.play" %% "play-slick" % "3.0.3",
  "joda-time" % "joda-time" % "2.10.2",
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "org.postgresql" % "postgresql" % "42.2.5"
)

// Creates Webpack bundle when compiling, based on
// https://github.com/wigahluk/play-webpack.

val webpackInstall = taskKey[Unit]("Webpack install task.")
webpackInstall := { (Process("npm install", file("./app/frontend"))).! }

// update := (update dependsOn webpackInstall).value

val webpackBuild = taskKey[Unit]("Webpack build task.")
webpackBuild := { (Process("npm run build-dev", file("./app/frontend"))).! }

val webpackBuildDev = taskKey[Unit]("Webpack build task, development mode.")
webpackBuildDev := { (Process("npm run build-dev", file("./app/frontend"))).! }

// (Compile / compile) := ((Compile / compile) dependsOn webpackBuild).value
