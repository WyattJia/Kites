package kvstore.message

import java.util.*

class SetCommand(val requestId: String, val key: String, val value: ByteArray) {

    constructor(key: String, value: ByteArray) : this(UUID.randomUUID().toString(), key, value) {}

    fun toBytes(): ByteArray {
        return Protos.SetCommand.newBuilder()
            .setRequestId(requestId)
            .setKey(key)
            .setValue(ByteString.copyFrom(value)).build().toByteArray()
    }

    override fun toString(): String {
        return "SetCommand{" +
                "key='" + key + '\'' +
                ", requestId='" + requestId + '\'' +
                '}'
    }

    companion object {
        fun fromBytes(bytes: ByteArray?): SetCommand {
            return try {
                val protoCommand: SetCommand = Protos.SetCommand.parseFrom(bytes)
                SetCommand(
                    protoCommand.requestId,
                    protoCommand.key,
                    protoCommand.value.toByteArray()
                )
            } catch (e: InvalidProtocolBufferException) {
                throw IllegalStateException("failed to deserialize set command", e)
            }
        }
    }
}
