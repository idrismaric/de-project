package SparkJob

object Domain {
    case class SparkParams(
        // CsvParser
        parser: String = "",
        // Csv, Json, Avro
        inFormat: String = "",
        // parquet, csv, avro
        outFormat: String = "",
        // s3:/mybucket/abc/sample.csv,  file:///mylocal/abc/test.json
        inPath: String = "",
        // s3://mydestinationbucket/xfg/
        outPath: String = "",
        // append, overwrite 
        saveMode:String = "",
        // what is the column you will use to compute the data 
        // and control the layout of the data on disk.
        partitionColumn: String = "",
        inOptions: Map[String, String] = Map.empty[String, String],
        outOptions: Map[String, String] = Map.empty[String, String]
    )
}