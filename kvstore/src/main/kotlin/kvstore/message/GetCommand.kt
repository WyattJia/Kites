package kvstore.message


class GetCommand(val key: String) {

    override fun toString(): String {
        return "GetCommand{" +
                "key='" + key + '\'' +
                '}'
    }
}
