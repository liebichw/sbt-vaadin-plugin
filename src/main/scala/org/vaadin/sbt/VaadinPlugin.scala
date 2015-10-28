package org.vaadin.sbt

import org.vaadin.sbt.tasks._
import sbt.Keys._
import sbt._

object VaadinPlugin extends Plugin with VaadinKeys {

  val vaadinSettings = Seq(

    compileVaadinWidgetsets <<= CompileWidgetsetsTask.compileWidgetsetsTask,
    vaadinWidgetsets := Nil,
    // TODO: refactor to use a value from 'webappDest in webapp')
    target in compileVaadinWidgetsets := (target in Compile).value / "webapp" / "VAADIN" / "widgetsets",
    vaadinOptions in compileVaadinWidgetsets := Nil,
    javaOptions in compileVaadinWidgetsets := Nil,
    compileWidgetSetsCacheDir := None,
    vaadinDevMode <<= DevModeTask.devModeTask,
    vaadinWidgetsets in vaadinDevMode := (vaadinWidgetsets in compileVaadinWidgetsets).value,
    target in vaadinDevMode := (target in compileVaadinWidgetsets).value,
    vaadinOptions in vaadinDevMode := Nil,
    javaOptions in vaadinDevMode := (javaOptions in compileVaadinWidgetsets).value,

    vaadinSuperDevMode <<= SuperDevModeTask.superDevModeTask,
    vaadinWidgetsets in vaadinSuperDevMode := (vaadinWidgetsets in compileVaadinWidgetsets).value,
    vaadinOptions in vaadinSuperDevMode := Nil,
    javaOptions in vaadinSuperDevMode := (javaOptions in compileVaadinWidgetsets).value,

    compileVaadinThemes <<= CompileThemesTask.compileThemesTask,
    vaadinThemes := Nil,
    vaadinThemesDir <<= sourceDirectory(sd => Seq(sd / "main" / "webapp" / "VAADIN" / "themes")),
    // TODO: refactor to use a value from 'webappDest in webapp')
    target in compileVaadinThemes := (target in Compile).value / "webapp" / "VAADIN" / "themes",

    packageVaadinDirectoryZip <<= PackageDirectoryZipTask.packageDirectoryZipTask,
    // Include binary jar into the zip file.
    vaadinAddonMappings in packageVaadinDirectoryZip <<= (packageBin in Compile) map {
      (bin) => Seq((bin, bin.name))
    },
    // Include sources and javadoc jars into the zip file.
    mappings in packageVaadinDirectoryZip <<= (packageSrc in Compile, packageDoc in Compile) map {
      (src, doc) => Seq((src, src.name), (doc, doc.name))
    }
  )

  val vaadinAddOnSettings = vaadinSettings ++ Seq(
    packageOptions in (Compile, packageBin) <+= AddOnJarManifestTask.addOnJarManifestTask,

    // Include source files into the binary jar file. Widgetset compiler needs those.
    mappings in (Compile, packageBin) <++= (unmanagedSources in Compile, unmanagedSourceDirectories in Compile,
      baseDirectory in Compile) map { (srcs, sdirs, base) =>
        ((srcs --- sdirs --- base) pair (relativeTo(sdirs) | relativeTo(base) | flat)) toSeq
      }
  )

  val vaadinWebSettings = vaadinSettings ++ Seq(
    resourceGenerators in Compile <+= CompileWidgetsetsTask.compileWidgetsetsInResourceGeneratorsTask,
    resourceGenerators in Compile <+= compileVaadinThemes
  )

}