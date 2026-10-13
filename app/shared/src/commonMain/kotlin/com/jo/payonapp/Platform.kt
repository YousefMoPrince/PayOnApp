package com.jo.payonapp

interface Platform {
    val name: String
}
expect fun getPlatform(): Platform