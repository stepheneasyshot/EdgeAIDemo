package com.stephen.smallmodel

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform