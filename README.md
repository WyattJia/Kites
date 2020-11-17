![](./image/kites.jpeg)

# Kites(风筝)

[![Kotlin](https://img.shields.io/badge/kotlin-1.4.10-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![travis-ci](https://travis-ci.org/wellls/Kites.svg?branch=dev)



Kites is a consistency and partition tolerance completed distributed kv store.
It's a implementation of the Raft distributed consensus protocol and Kotlin.
The currently implemented features are:

* Leader election
* Log replication
* Membership change

## Preparation

* Kotlin 1.40
* Gradle
* Protobuf
* ~~RocksDB~~

## Build

```bash
   cd path/to/kites
   ./gradlew build
```

## Todo

* Log compaction
* Cover more test cases.
* Use rocksdb as a stand-alone storage engine.
* Use akka eventbus

## Contributing

**Very eager for everyone to participate in contributing code.**

## Links

* Raft: https://raft.github.io/
* ~~RocksDB: https://rocksdb.org/~~
* Kotlin coroutines: https://kotlinlang.org/docs/reference/coroutines-overview.html
* ~~rSocket-rpc-kotlin: https://github.com/rsocket/rsocket-rpc-kotlin~~
