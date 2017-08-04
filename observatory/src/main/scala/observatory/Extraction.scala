package observatory

import java.time.{LocalDate, Month}

import org.apache.commons.io.IOUtils
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConverters._
import scala.io.Source

/**
  * 1st milestone: data extraction
  */
object Extraction {

  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Int, stationsFile: String, temperaturesFile: String): Iterable[(LocalDate, Location, Double)] = {
    val stationStream = getClass.getResourceAsStream(stationsFile)
    val stationLines = Source.fromInputStream(stationStream).getLines.toSeq

    val locationsMap = stationLines.map(line => line.split(",")).filter { parts =>
      parts.length == 4 && !parts(2).isEmpty && !parts(3).isEmpty
    }.map {
      parts => ((parts(0), parts(1)), Location(parts(2).toDouble, parts(3).toDouble))
    }.toMap

    val temperatureStream = getClass.getResourceAsStream(temperaturesFile)
    val temperatureLines = Source.fromInputStream(temperatureStream).getLines.toSeq

    val temperaturesList = temperatureLines.map { line =>
      val parts = line.split(",")
      ((parts(0), parts(1)), (LocalDate.of(year, parts(2).toInt, parts(3).toInt), fahrenheitToCelsius(parts(4).toDouble)))
    }

    temperaturesList.flatMap { case (key, (date, temp)) => locationsMap.get(key).map((date, _, temp))}
  }

  def fahrenheitToCelsius(fahrenheit: Double) = ((BigDecimal.valueOf(fahrenheit) - BigDecimal.valueOf(32)) / BigDecimal.valueOf(1.8)).toDouble

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Double)]): Iterable[(Location, Double)] = {
    records.groupBy(_._2).mapValues(iter => iter.map(_._3).sum / iter.size)
  }
}