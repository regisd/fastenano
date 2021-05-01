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
package fastenano.frontend;

import fastenano.proto.Encoding
import nanoapi.Message

/**
 * An IPC framing request on the nano node, using flatbuffer format.
 *
 *   REQUEST  ::= HEADER PAYLOAD
 *   HEADER   ::= u8('N') ENCODING u8(0) u8(0)
 *   ENCODING ::= u8(1)
 *   PAYLOAD  ::= <encoding specific>
 *
 * See https://docs.nano.org/integration-guides/ipc-integration/#ipc-requestresponse-format
 */
data class FlatbufferIpcRequest(val payload: Message) {
}