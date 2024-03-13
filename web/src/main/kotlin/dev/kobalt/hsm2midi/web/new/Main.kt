/*
 * dev.kobalt.hsm2midi
 * Copyright (C) 2022 Tom.K
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.kobalt.hsm2midi.web

import dev.kobalt.hsm2midi.web.converter.ConverterPlugin
import dev.kobalt.hsm2midi.web.converter.converter
import dev.kobalt.hsm2midi.web.converter.converterRoute
import dev.kobalt.hsm2midi.web.extension.ifLet
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

suspend fun main(args: Array<String>) {
    // Parse given arguments.
    val parser = ArgParser(
        programName = "hsm2midi"
    )
    val jarPath by parser.option(
        type = ArgType.String,
        fullName = "jarPath",
        shortName = "jar",
        description = "Path to converter JAR file"
    )
    val httpServerPort by parser.option(
        type = ArgType.Int,
        fullName = "httpServerPort",
        shortName = "hsp",
        description = "Port to host the server at"
    )
    val httpServerHost by parser.option(
        type = ArgType.String,
        fullName = "httpServerHost",
        shortName = "hsh",
        description = "Host value (127.0.0.1 for private, 0.0.0.0 for public access)"
    )
    parser.parse(args)
    ifLet(jarPath, httpServerPort, httpServerHost) { path, port, host ->
        CoroutineScope(Dispatchers.Main).async(
            context = Dispatchers.IO + NonCancellable,
            start = CoroutineStart.LAZY
        ) {
            setupServer(path, port, host).also {
                Runtime.getRuntime().addShutdownHook(thread(start = false) {
                    it.stop(0, 10, TimeUnit.SECONDS)
                })
            }.also {
                it.start(true)
            }
        }.await()
    }
}

/** Returns an instance of server with configuration from given entity .*/
fun setupServer(jarPath: String, port: Int, host: String) = embeddedServer(CIO, port, host) {
    install(ForwardedHeaders)
    install(DefaultHeaders) {
        // TODO: Check this later.
        // header("Content-Security-Policy", "frame-ancestor 'hsm2midi.kobalt.dev'" )
    }
    install(CallLogging)
    install(Compression)
    install(ConverterPlugin) {
        this.jarPath = jarPath
    }
    install(IgnoreTrailingSlash)
    install(CachingHeaders) {
        options { _, _ -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 0)) }
    }
    install(StatusPages)
    install(Routing) {
        route("style.css") {
            get {
                call.respondText(application.converter.getStyleCssContent(), ContentType.Text.CSS)
            }
        }
        converterRoute()
    }
}.also {
    Runtime.getRuntime().addShutdownHook(thread(start = false) {
        it.stop(0, 10, TimeUnit.SECONDS)
    })
}.also {
    it.start(true)
}