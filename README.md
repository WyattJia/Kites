![](./image/kites.jpeg)

# Kites

[![Kotlin](https://img.shields.io/badge/kotlin-1.4.10-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![travis-ci](https://travis-ci.org/wellls/Kites.svg?branch=dev&status=unknown)]



Kites is a consistency and partition tolerance completed distributed kv store.
It's a implementation of the Raft distributed consensus protocol and Kotlin.
The currently implemented features are:

* Leader election
* Log replication
* Membership change(Only use concurrentHashMap, it is still very simple)

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
* (Use [TitanDB](https://pingcap.com/blog/titan-storage-engine-design-and-implementation/) or [pebblesdb](https://github.com/utsaslab/pebblesdb) to solve the problem of write amplification.)

## Contributing

**Very eager for everyone to participate in contributing code.**

## Links

* Raft: https://raft.github.io/
* ~~RocksDB: https://rocksdb.org/~~
* Kotlin coroutines: https://kotlinlang.org/docs/reference/coroutines-overview.html
* ~~rSocket-rpc-kotlin: https://github.com/rsocket/rsocket-rpc-kotlin~~
