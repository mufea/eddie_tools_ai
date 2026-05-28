package com.example

import android.content.Context

object AppContextProvider {
    private var applicationContext: Context? = null

    fun init(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
    }

    val context: Context
        get() = applicationContext ?: throw IllegalStateException("AppContextProvider is not initialized!")
}
