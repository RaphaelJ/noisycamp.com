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

name := """noisycamp"""
organization := "com.noisycamp"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  guice, // Dependency injection library required by Play

  evolutions, jdbc, // Executes database evolutions

  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "com.typesafe.play" %% "play-slick" % "3.0.3"
)

// Creates Webpack bundle when compiling, based on
// https://github.com/wigahluk/play-webpack.

val webpackInstall = taskKey[Unit]("Webpack install task.")
webpackInstall := { (Process("npm install", file("./app/frontend"))).! }

// update := (update dependsOn webpackInstall).value

val webpackBuild = taskKey[Unit]("Webpack build task.")
webpackBuild := { (Process("npm run build", file("./app/frontend"))).! }

// (Compile / compile) := ((Compile / compile) dependsOn webpackBuild).value
