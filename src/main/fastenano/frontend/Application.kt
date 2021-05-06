// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package fastenano.frontend

import com.google.flatbuffers.FlatBufferBuilder
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.Compression
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.features.deflate
import io.ktor.features.gzip
import io.ktor.features.minimumSize
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.timeout
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.websocket.webSocket
import java.nio.ByteBuffer
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nanoapi.AccountWeight
import nanoapi.AccountWeightResponse
import nanoapi.Envelope
import nanoapi.Message

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

/** Web application started by {@code io.ktor.server.netty.EngineMain}. */
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        get("/") {
            call.respondText("Try /ws/nano instead", contentType = ContentType.Text.Plain)
        }

        install(StatusPages) {
            exception<AuthenticationException> { cause ->
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<AuthorizationException> { cause ->
                call.respond(HttpStatusCode.Forbidden)
            }

        }

        webSocket("/ws/nano") {
            send(Frame.Text("Connected"))
            while (true) {
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    launch {
                        send(Frame.Text("Processing..."))
                        val resp = handleRequest(frame.readText())
                        // send(Frame.Text(resp))
                        send(Frame.Text("Done."))
                    }
                }
            }
        }
    }
}

suspend fun handleRequest(address: String): String {
    delay(2_000L);
    val fb = FlatBufferBuilder()
    val optCredentials = fb.createString("")
    val optCorrelationId = fb.createString("")
    val account = fb.createString(address)
    val message = AccountWeight.createAccountWeight(fb, account)
    val envelope = Envelope.createEnvelope(
        fb, /* time= */
        42L,
        optCredentials,
        optCorrelationId,
        Message.AccountWeight,
        message
    )
    fb.finish(envelope)

    val data = fb.dataBuffer()

    val tcpSocket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
        .connect("127.0.0.1", 7077)
    val input = tcpSocket.openReadChannel()
    val output = tcpSocket.openWriteChannel(autoFlush = true)

    output.writeFully(data)
    // Read the response
    val respBuffer = ByteBuffer.allocate(input.availableForRead)
    val respRead = input.readFully(respBuffer)
    val resp = AccountWeightResponse.getRootAsAccountWeightResponse(respBuffer)
    return String.format(
        "Response: Account (%s) has voting weight %s",
        address,
        resp.votingWeight()
    )
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
