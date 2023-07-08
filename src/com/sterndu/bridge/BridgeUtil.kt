@file:JvmName("BridgeUtil")
package com.sterndu.bridge

object BridgeUtil {
	/** The Constant DEFAULT_PORT.  */
	const val DEFAULT_PORT = 55601

	/** The Constant WILDCARD.  */
	@JvmField
	val WILDCARD = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

	private fun parseIPv4(url: String): String? {
		var idxEndDomain = url.indexOf(':')
		var isSlash = false
		if (idxEndDomain == -1) {
			idxEndDomain = url.indexOf('/')
			if (idxEndDomain == -1) return null
			idxEndDomain--
			isSlash = true
		}
		return url.substring(0, idxEndDomain + if (isSlash) 1 else 0)
	}

	private fun parseIPv6(url: String): String? {
		val idxEndDomain = url.indexOf(']')
		if (idxEndDomain == -1) return null
		return url.substring(0, idxEndDomain + 1)
	}

	/**
	 * Split.
	 *
	 * @param url the url
	 * @return the bridge protocol bundle
	 *
	 * @throws UnsupportedOperationException if endPoint is not one of host, join, connect
	 */
	@Throws(UnsupportedOperationException::class)
	fun split(url: String): BridgeProtocolBundle? {
		var mutableUrl = url
		if (mutableUrl.startsWith("bridge://")) {
			val domain: String
			var port: Int
			var endPoint: String
			var data: String
			mutableUrl = mutableUrl.substring("bridge://".length)
			if (mutableUrl.startsWith("[")) {
				domain = parseIPv6(mutableUrl)?: return null
				mutableUrl = mutableUrl.substring(domain.length + 2)
			} else {
				domain = parseIPv4(mutableUrl)?: return null
				mutableUrl = mutableUrl.substring(domain.length + 1)
			}
			var sp = mutableUrl.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			if (sp.isEmpty()) return null
			try {
				if (sp[0] === "") sp = if (sp.size > 1) sp.copyOfRange(1, sp.size) else return null
				port = sp[0].toInt()
				if (sp.size <= 1) return null
				val (endPoint2, data2) = parseEndPoint(sp)
				endPoint = endPoint2
				data = data2
			} catch (e: NumberFormatException) {
				port = DEFAULT_PORT
				val (endPoint2, data2) = parseEndPoint(sp, 0)
				endPoint = endPoint2
				data = data2
			}
			return BridgeProtocolBundle(domain, port, endPoint, data)
		}
		return null
	}

	@Throws(UnsupportedOperationException::class)
	private fun parseEndPoint(sp: Array<String>, offset: Int = 1): Pair<String, String> {
		return sp[offset] to (if ("host" == sp[offset]) {
			(if (sp.size > offset+1) sp.drop(offset+1).joinToString("/") else "")
		} else if (("join" == sp[offset] || "connect" == sp[offset]) && sp.size > offset+1) {
			sp.drop(offset+1).joinToString("/")
		} else throw UnsupportedOperationException(sp[offset]))
	}
}
