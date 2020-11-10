package kvstore.client


interface Command {
    val name: String?

    fun execute(arguments: String?, context: CommandContext?)
}

