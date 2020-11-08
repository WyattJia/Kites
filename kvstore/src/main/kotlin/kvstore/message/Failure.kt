package kvstore.message


/*
Get command failure err msg
Error code:
100 general error
101 timeout
*/
class Failure(val errorCode: Int, val message: String) {
    override fun toString(): String {
        return "Failure{" +
                "errorCode=" + errorCode +
                ", message='" + message + '\'' +
                '}'
    }
}
