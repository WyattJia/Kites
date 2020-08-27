val cl = Action<Task> { println("I'm ${ this.project.name }") }

tasks.register("hello") { doLast(cl) }

project(":raft") {
    tasks.register("hello") { doLast(cl) }
}
