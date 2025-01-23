package com.magic.maw.util

import okhttp3.internal.proxy.NullProxySelector
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

object SwitchProxySelector : ProxySelector() {
    override fun select(uri: URI?): MutableList<Proxy> {
        return getDefaultSelector().select(uri)
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        require(!(uri == null || sa == null || ioe == null)) { "Arguments can't be null." }
    }

    private fun getDefaultSelector(): ProxySelector {
        return getDefault() ?: NullProxySelector
    }
}