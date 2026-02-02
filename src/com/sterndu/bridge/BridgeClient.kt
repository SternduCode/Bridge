@file:JvmName("BridgeClient")
package com.sterndu.bridge

import com.sterndu.data.transfer.Connector
import com.sterndu.data.transfer.DataTransferSocket
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.net.Socket as NetSocket

/**
 * Instantiates a new bridge client.
 *
 * @param hostname the hostname
 * @param port the port
 * @throws UnknownHostException the unknown host exception
 * @throws IOException Signals that an I/O exception has occurred.
 */
class BridgeClient	@Throws(IOException::class, UnknownHostException::class) @JvmOverloads constructor(hostname: String, port: Int = BridgeUtil.DEFAULT_PORT) {

	@JvmField
	val sock: DataTransferSocket = DataTransferSocket(NetSocket(hostname, port), true)

    /**
	 * Connect.
	 *
	 * @param remoteHostname the remote hostname
	 * @param remotePort     the remote port
	 *
	 * @return the connector
	 */
	fun connect(remoteHostname: String, remotePort: Int): Connector? {
		return try {
			val str = remoteHostname.toByteArray(Charsets.UTF_8)
			val data = ByteArray(str.size + 8)
			val bb = ByteBuffer.wrap(data)
			bb.order(ByteOrder.BIG_ENDIAN)
			bb.putInt(str.size)
			bb.put(str)
			bb.putInt(remotePort)
			sock.sendData(2.toByte(), data)
			sock.setHandle(2.toByte()) { typ: Byte, dat: ByteArray ->
				if (dat.isEmpty()) try {
					sock.sendClose()
					sock.close()
				} catch (e: IOException) {
					e.printStackTrace()
				} else println("Type2 connect " + dat.contentToString())
			}
			Connector(sock, 4.toByte())
		} catch (e: SocketException) {
			e.printStackTrace()
			null
		}
	}

	/**
	 * Host.
	 *
	 * @return the host connector
	 */
	fun host(): HostConnector? {
		return try {
			sock.sendData(1.toByte(), ByteArray(0))
			val hc = HostConnector(
				Connector(sock, 6.toByte()),
				Connector(sock, 5.toByte()),
				sock
			)
			sock.setHandle(1.toByte()) { typ: Byte, dat: ByteArray ->
				hc.code = String(Base64.getEncoder().encode(dat), Charsets.UTF_8)
			}
			while (!hc.codeAvailable) try {
				Thread.sleep(5)
			} catch (e: InterruptedException) {
				e.printStackTrace()
			}
			hc
		} catch (e: SocketException) {
			e.printStackTrace()
			null
		}
	}

	/**
	 * Join.
	 *
	 * @param code the code
	 *
	 * @return the connector
	 */
	fun join(code: String): Connector? {
		return try {
			sock.sendData(3.toByte(), code.toByteArray(Charsets.UTF_8))
			sock.setHandle(3.toByte()) { typ: Byte, dat: ByteArray ->
				if (dat.isEmpty()) try {
					sock.sendClose()
					sock.close()
				} catch (e: IOException) {
					e.printStackTrace()
				}
			}
			Connector(sock, 5.toByte())
		} catch (e: SocketException) {
			e.printStackTrace()
			null
		}
	}
}
