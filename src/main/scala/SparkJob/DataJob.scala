package SparkJob

import org.apache.spark.sql.SparkSession
import SparkJob.Domain._

trait DataJob[A,B] {
    case class SaveParameters(df: B, params: SparkParams)

    def run(params:SparkParams): Unit = {
        implicit val sparkParams:SparkParams = params
        implicit val spark:SparkSession = sparkInitialization().getOrCreate

        import spark.sqlContext.implicits._

        val df=read(params)
        val p = transform(df)
        save(p)
        spark.stop()
    }

    def read(params:SparkParams)(implicit spark:SparkSession):A

    def transform(df:A)(implicit spark:SparkSession, sparkParams:SparkParams): SaveParameters

    def save(p: SaveParameters): Unit

    def sparkInitialization()(implicit param:SparkParams) = {
        val ss = SparkSession.builder.appName(param.parser)
        ss
    }
}