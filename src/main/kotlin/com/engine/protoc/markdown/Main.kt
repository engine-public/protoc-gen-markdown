package com.engine.protoc.markdown

import kotlin.system.exitProcess

public fun main() {
    ProtocGenMarkdown
        .from(System.`in`)
        .compile()
        .apply {
            writeTo(System.out)
            System.out.flush()
            if (this.hasError()) {
                exitProcess(1)
            }
        }
}
