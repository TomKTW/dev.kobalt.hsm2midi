/*
 * dev.kobalt.hsm2midi
 * Copyright (C) 2024 Tom.K
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

package dev.kobalt.hsm2midi.web.converter

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.*
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.jvm.optionals.getOrNull

/** Repository that provides access to converter that will process submitted data. */
class ConverterRepository(
    /** Path of converter JAR file that will process submitted data.. */
    private val jarPath: Path,
) {

    /** Semaphore limiter to prevent a large amount of requests at once. */
    private val semaphore = Semaphore(5)

    /** Processes provided string data by sending it to external Java executable and writes it back to output stream. */
    private suspend fun convert(data: String, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val javaPathString = ProcessHandle.current().info().command().getOrNull()!!
        val process = ProcessBuilder(javaPathString, "-jar", jarPath.absolutePathString()).start()
        val stdout = BufferedInputStream(process.inputStream)
        val stderr = BufferedReader(InputStreamReader(process.errorStream))
        val stdin = BufferedWriter(OutputStreamWriter(process.outputStream))
        val stdinJob = launch(Dispatchers.IO) {
            stdin.write(data)
            stdin.flush()
            stdin.close()
        }
        val stderrJob = launch(Dispatchers.IO) {
            println(stderr.readText())
        }
        val stdoutJob = async(Dispatchers.IO) {
            outputStream.write(stdout.readBytes())
        }
        stdinJob.join()
        stderrJob.join()
        stdoutJob.await()
        if (process.exitValue() == 0) outputStream else throw Exception()
    }

    suspend fun submit(data: String, outputStream: OutputStream): OutputStream {
        return withContext(Dispatchers.IO) {
            semaphore.withPermit {
                withTimeout(5000) {
                    convert(data, outputStream)
                }
            }
        }
    }

    fun getInputStreamFromResources(path: String): InputStream? =
        this::class.java.classLoader.getResourceAsStream(path)

    fun getBytesFromResources(path: String): ByteArray? =
        getInputStreamFromResources(path)?.use { it.readBytes() }

    fun getTextFromResources(path: String): String? =
        getBytesFromResources(path)?.decodeToString()

    fun getIndexPageContent(): String {
        return getTextFromResources("index.html")!!
    }

    fun getFailurePageContent(): String {
        return getTextFromResources("failure.html")!!
    }

    fun getStyleCssContent(): String {
        return getTextFromResources("style.css")!!
    }

}
