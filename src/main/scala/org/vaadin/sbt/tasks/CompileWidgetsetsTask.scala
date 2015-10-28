package org.vaadin.sbt.tasks

import _root_.java.io.{ FileNotFoundException, IOException }

import org.vaadin.sbt.VaadinPlugin.{ compileVaadinWidgetsets, vaadinOptions, vaadinWidgetsets, compileWidgetSetsCacheDir }
import org.vaadin.sbt.util.ForkUtil._
import org.vaadin.sbt.util.ProjectUtil._
import sbt.Keys._
import sbt._

/**
 * @author Henri Kerola / Vaadin
 */
object CompileWidgetsetsTask {

  val compileWidgetsetsTask: Def.Initialize[Task[Seq[File]]] = (
    classDirectory in Compile, dependencyClasspath in Compile,
    resourceDirectories in Compile, vaadinWidgetsets in compileVaadinWidgetsets,
    compileWidgetSetsCacheDir in compileVaadinWidgetsets, vaadinOptions in compileVaadinWidgetsets,
    javaOptions in compileVaadinWidgetsets, target in compileVaadinWidgetsets,
    thisProject, skip in compileVaadinWidgetsets,
    state, streams).map(widgetsetCompiler)

  val compileWidgetsetsInResourceGeneratorsTask: Def.Initialize[Task[Seq[File]]] = (
    classDirectory in Compile, dependencyClasspath in Compile,
    resourceDirectories in Compile, vaadinWidgetsets in compileVaadinWidgetsets, compileWidgetSetsCacheDir in compileVaadinWidgetsets, vaadinOptions in compileVaadinWidgetsets,
    javaOptions in compileVaadinWidgetsets, target in compileVaadinWidgetsets, thisProject,
    skip in compileVaadinWidgetsets in resourceGenerators,
    state, streams) map widgetsetCompiler

  private def addIfNotInArgs(args: Seq[String], param: String, value: String) =
    if (!args.contains(param)) Seq(param, value) else Nil

  def widgetsetCompiler(
    classDir: File,
    fullCp: Classpath,
    resources: Seq[File],
    widgetsets: Seq[String],
    cd: Option[File],
    args: Seq[String],
    jvmArguments: Seq[String],
    target: File,
    p: ResolvedProject,
    skip: Boolean,
    state: State,
    s: TaskStreams): Seq[File] = {

    implicit val log = s.log

    if (skip) {
      log.info("Skipping Widgetset compilation.")
      Seq[File]()
    } else {

      IO.createDirectory(target)
      val (keepGwtUnitCache, tmpDir) = {
        cd match {
          case Some(d) => (true, d)
          case None => (false, IO.createTemporaryDirectory)
        }
      }

      try {
        val jvmArgs = Seq("-Dgwt.persistentunitcachedir=" + tmpDir.absolutePath) ++ jvmArguments

        val cmdArgs = Seq("-war", target absolutePath) ++
          addIfNotInArgs(args, "-extra", tmpDir absolutePath) ++
          addIfNotInArgs(args, "-deploy", tmpDir absolutePath) ++ args

        val exitValue = forkWidgetsetCmd(
          jvmArgs,
          getClassPath(state, Seq(classDir) ++ fullCp.files),
          "com.vaadin.tools.WidgetsetCompiler",
          cmdArgs,
          widgetsets,
          resources)

        exitValue match {
          case Left(error) => sys.error(error)
          case Right(curWS) => {
            log.debug("Deleting %s" format target / "WEB-INF")
            IO.delete(target / "WEB-INF")

            val generatedFiles: Seq[Seq[File]] = curWS map {
              widgetset => (target / widgetset ** ("*")).get
            }

            log.debug("Generated files: %s".format(generatedFiles.flatten.mkString(", ")))

            generatedFiles flatten
          }
        }
      } finally {
        if (keepGwtUnitCache) {
          log.info(s"Will not delete persistent GWT unit cache ${tmpDir.absolutePath}")
        } else {
          log.debug(s"Deleting persistent unit cache dir ${tmpDir.absolutePath}")
          try {
            deleteRecursive(tmpDir)
          } catch {
            case e: IOException => log.warn(s"Deleting ${tmpDir.absolutePath} failed with msg ${e.getLocalizedMessage}")
          }
        }
      }
    }
  }

  private def deleteRecursive(path: File): Unit = {
    if (!path.exists()) {
      throw new FileNotFoundException(path.getAbsolutePath)
    }

    if (path.isDirectory) {
      path.listFiles() foreach deleteRecursive
    }
    if (!path.delete()) {
      throw new IOException(s"Could not delete ${path.absolutePath}")
    }
  }
}
