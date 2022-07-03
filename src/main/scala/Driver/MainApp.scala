package Driver

import scopt.OParser
import org.slf4j.LoggerFactory
import SparkJob.Domain.SparkParams
import scala.reflect.runtime.universe

object MainApp {
    val logger = LoggerFactory.getLogger(this.getClass)

    def main(args: Array[String]): Unit = {
        val sparkParams = parseCmd(args)
        val className = sparkParams match {
            case Some(x) => Some(sparkParams.get.inFormat.toString)
            case _ => None
        }

        if (className isDefined) {
            try{
                invoker(className, "run", sparkParams) 
            } 
            catch {
                case _: Exception => logger.error("class not support yet!")
            }
        } else {
            logger.error("Please register class: " + sparkParams.get.inFormat.toString)
        }

    }

    def parseCmd(args: Array[String]): Option[SparkParams] = {
        val builder = OParser.builder[SparkParams]

        val cmdParser = {
            import builder._
            OParser.sequence(
             programName("wcd_project"),
             head("wcd_parser", "0.0.1"),

             opt[String]('p', "parser").required().valueName("<Parser>").
             action((x, c) => c.copy(parser = x)).
             text(s"paramer parser is required. "),

             opt[String]('i', "input-format").required().valueName("<input-format>").
             action((x, c) => c.copy(inFormat = x)).text(s"input format is required. example: Csv, Json etc. "),
            
             opt[String]('o', "output-format").required().valueName("<output-format>").
             action((x, c) => c.copy(outFormat = x)).text("output format is required, default to parquet."),
             
             opt[String]('s', "input-path").required().valueName("<input-path>").
             action((x, c) => c.copy(inPath = x)).text("input path is required."),
             
             opt[String]('d', "output-path").required().valueName("<output-path>").
             action((x, c) => c.copy(outPath = x)).text("output path is required."),
             
             opt[String]('m', "save-mode").required().valueName("<save-mode>").
             action((x, c) => c.copy(saveMode = x)).text( """output save mode (append, overwrite, ignore)."""),
             
             opt[String]('c', "partition-column").required().valueName("<partition-column>").
             action((x, c) => c.copy(partitionColumn = x)).text( """The column(s) being used to partition on. coma seperated. For example: name,date"""),
             
             opt[Map[String, String]]("input-options").optional().valueName("k1=v1,k2=v2...").
             action((x, c) =>c.copy(inOptions = x)).text("Spark read input options. Option. Example can be header=True"),
             
             opt[Map[String, String]]("output-options").optional().valueName("k1=v1,k2=v2...").
             action((x, c) =>c.copy(outOptions = x)).text("Spark read output options"))
        }
        
        OParser.parse(cmdParser, args, SparkParams())
    }

    def invoker(className: Option[String],method: String, sparkParams: Option[SparkParams]) = {
        val packageName = "SparkJob."
        val sufix = "Job"
        val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
        val moduleSymbol = runtimeMirror.moduleSymbol(Class.forName(packageName + className.get + sufix))

        val targetMethod = moduleSymbol.typeSignature
        .members
        .filter(x => x.isMethod && x.name.toString == method)
        .head
        .asMethod

        runtimeMirror.reflect(runtimeMirror.reflectModule(moduleSymbol).instance)
        .reflectMethod(targetMethod)(sparkParams.get)
    }

}