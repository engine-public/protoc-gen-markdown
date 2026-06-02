package com.engine.protoc.markdown.jekyll

import kotlin.system.exitProcess

public fun main() {
    ProtocGenMarkdownJekyll
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
