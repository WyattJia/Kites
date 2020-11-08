package kvstore.message


class GetCommandResponse(val isFound: Boolean, val value: ByteArray?) {

    constructor(value: ByteArray?) : this(value != null, value) {}

    override fun toString(): String {
        return "GetCommandResponse{found=$isFound}"
    }
}