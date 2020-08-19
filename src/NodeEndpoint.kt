import javax.annotation.concurrent.Immutable;



class NodeEndpoint {
    val id = NodeId()
    val address = Address()
}

@Immutable
class Address(@NotNull host: String, port: Int) {

    @get:Notnull
    val host:String

    var port:Int = 0

    init {
        checkNotNull(host)
        this.host = host
        this.port = port
    }

    public override fun toString(): String {
        return ("Address{" +
                "host='" + host + '\''.toString() +
                ", port=" + port +
                '}'.toString())
    }
}