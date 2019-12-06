val scalaV = "2.13.1"
val scalaTestV = "3.0.8"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % scalaTestV % "test"
)

name := "smaps-reader"
organization := "net.virtualvoid"
version := "0.5.0"

scalaVersion := scalaV
scalacOptions ++= Seq("-feature", "-deprecation")

// docs

enablePlugins(ParadoxMaterialThemePlugin)

paradoxMaterialTheme in Compile := {
  ParadoxMaterialTheme()
    // choose from https://jonas.github.io/paradox-material-theme/getting-started.html#changing-the-color-palette
    .withColor("light-green", "amber")
    // choose from https://jonas.github.io/paradox-material-theme/getting-started.html#adding-a-logo
    .withLogoIcon("cloud")
    .withCopyright("Copyleft © Johannes Rudolph")
    .withRepository(uri("https://github.com/jrudolph/xyz"))
    .withSocial(
      uri("https://github.com/jrudolph"),
      uri("https://twitter.com/virtualvoid")
    )
}

paradoxProperties ++= Map(
  "github.base_url" -> (paradoxMaterialTheme in Compile).value.properties.getOrElse("repo", "")
)
