

allprojects  {


    tasks.register("hello") {
        doLast {
            println("I'm ${this.project.name}")
        }
    }
}


subprojects {
    tasks.named("hello") {
        doLast {
            println("- I am flying.")
        }
    }
}

project(":raft").tasks.named("hello") {
    doLast {
        println("- I'm the largest animal that has ever lived on this plant.")
    }
}