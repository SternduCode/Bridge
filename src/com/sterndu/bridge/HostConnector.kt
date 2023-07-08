@file:JvmName("HostConnector")
package com.sterndu.bridge

import com.sterndu.data.transfer.Connector
import com.sterndu.data.transfer.secure.Socket
import java.net.SocketException

class HostConnector(@JvmField val announceConnector: Connector, @JvmField val normalConnector: Connector, private val sock: Socket) {

	/** The code.  */
	@JvmField
	var code: String? = null

	/**
	 * Close connection with a specified Address.
	 *
	 * @param addr the addr
	 * @throws SocketException the socket exception
	 */
	@Throws(SocketException::class)
	fun closeConnectionWith(addr: ByteArray) {
		sock.sendData(7.toByte(), addr)
	}

	val codeAvailable: Boolean
		get() = code != null
}
