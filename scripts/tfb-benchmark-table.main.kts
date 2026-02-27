#!/usr/bin/env kotlin
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

import java.net.URL
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt
import kotlinx.serialization.json.*

/**
 * Fetches TechEmpower Framework Benchmark results for a given run ID and outputs the start date
 * and the markdown table as used in the README.
 *
 * Usage: kotlin tfb-benchmark-table.main.kts [runId]
 *
 * If no run ID is provided, defaults to the run ID hardcoded below.
 */

fun fetchText(url: String): String = URL(url).readText()

fun Int.asFormattedRps() = "%,d".format(this)

fun main(runId: String) {
    // Fetch the run status page to find the raw JSON file URL
    val statusPage = fetchText("https://tfb-status.techempower.com/results/$runId")
    val rawJsonPath = Regex("""href="(/raw/results\.[^"]+\.json)"""")
        .find(statusPage)?.groupValues?.get(1)
        ?: error("Could not find raw JSON link in status page")
    val rawJsonUrl = "https://tfb-status.techempower.com$rawJsonPath"

    val root = Json.parseToJsonElement(fetchText(rawJsonUrl)).jsonObject
    val rawData = root["rawData"]!!.jsonObject
    val queryIntervals = root["queryIntervals"]!!.jsonArray.map { it.jsonPrimitive.int }
    val idx20 = queryIntervals.indexOf(20)
    require(idx20 >= 0) { "20-query interval not found in $queryIntervals" }

    val startDate = Instant.ofEpochMilli(root["startTime"]!!.jsonPrimitive.long)
        .atZone(ZoneOffset.UTC).toLocalDate()
    println("Run started on: $startDate")
    println()

    // Use integer (floor) division to match TFB website display
    fun entryRps(entry: JsonObject): Int {
        val total = entry["totalRequests"]!!.jsonPrimitive.long
        val dur = entry["endTime"]!!.jsonPrimitive.long - entry["startTime"]!!.jsonPrimitive.long
        return if (dur > 0) (total / dur).toInt() else 0
    }

    fun maxRps(testKey: String, fwKey: String): Int? =
        rawData[testKey]?.jsonObject?.get(fwKey)?.jsonArray?.maxOfOrNull { entryRps(it.jsonObject) }

    fun rpsAt20(testKey: String, fwKey: String): Int? =
        rawData[testKey]?.jsonObject?.get(fwKey)?.jsonArray?.takeIf { it.size > idx20 }
            ?.let { entryRps(it[idx20].jsonObject) }

    data class FrameworkSpec(val displayName: String, val dataKey: String, val description: String)

    val frameworks = listOf(
        FrameworkSpec("vertx-web-kotlinx-postgresql",
            "vertx-web-kotlinx-postgresql", "Vert.x baseline"),
        FrameworkSpec("vertx-web-kotlinx-exposed-vertx-sql-client-postgresql",
            "vertx-web-kotlinx-exposed-vertx-sql-client-postgresql", "Vert.x with this library"),
        FrameworkSpec("vertx-web-kotlinx-exposed-r2dbc-postgresql",
            "vertx-web-kotlinx-exposed-r2dbc-postgresql", "Vert.x with Exposed R2DBC directly (replacing the Vert.x SQL client)"),
        FrameworkSpec("vertx-web-kotlinx-r2dbc-postgresql",
            "vertx-web-kotlinx-r2dbc-postgresql", "Vert.x with R2DBC (replacing the Vert.x SQL client), for comparison"),
        FrameworkSpec("ktor-netty-exposed-jdbc-dsl",
            "ktor-exposed-jdbc-dsl", "Ktor with Exposed JDBC"),
        FrameworkSpec("ktor-netty-exposed-r2dbc-dsl",
            "ktor-exposed-r2dbc-dsl", "Ktor with Exposed R2DBC"),
    )

    data class Row(val spec: FrameworkSpec, val single: Int?, val multi: Int?, val fortune: Int?, val update: Int?)

    val rows = frameworks.map { spec ->
        Row(spec,
            single = maxRps("db", spec.dataKey),
            multi = rpsAt20("query", spec.dataKey),
            fortune = maxRps("fortune", spec.dataKey),
            update = rpsAt20("update", spec.dataKey))
    }

    val baseline = rows.first()

    fun fmtCell(value: Int?, baselineValue: Int?): String {
        if (value == null) return "N/A"
        if (baselineValue == null) return value.asFormattedRps()
        val pct = (value.toDouble() / baselineValue * 100).roundToInt()
        return "${value.asFormattedRps()} ($pct%)"
    }

    println("| Benchmark portion | Description | Single query | Multiple queries | Fortunes | Data updates |")
    println("| --- | --- | --- | --- | --- | --- |")
    rows.forEachIndexed { idx, row ->
        val (b_s, b_m, b_f, b_u) = if (idx == 0) listOf(null, null, null, null)
            else listOf(baseline.single, baseline.multi, baseline.fortune, baseline.update)
        println("| ${row.spec.displayName} | ${row.spec.description} | ${fmtCell(row.single, b_s)} | ${fmtCell(row.multi, b_m)} | ${fmtCell(row.fortune, b_f)} | ${fmtCell(row.update, b_u)} |")
    }
}

val defaultRunId = "e4388834-e02e-45e6-92ed-929bfe264a56"
val runId = args.firstOrNull() ?: defaultRunId
main(runId)
