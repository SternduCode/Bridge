@file:JvmName("BridgeServer")
package com.sterndu.bridge

import com.sterndu.bridge.BridgeUI.getLog
import com.sterndu.bridge.BridgeUI.isUIEnabled
import com.sterndu.data.transfer.secure.ServerSocket
import com.sterndu.data.transfer.secure.Socket
import com.sterndu.multicore.Updater.add
import com.sterndu.multicore.Updater.remove
import com.sterndu.util.Entry
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.Throws

/**
 * Instantiates a new bridge server.
 *
 * @param port the port
 * @param ui   the ui
 */
class BridgeServer @JvmOverloads @Throws(IOException::class) constructor(port: Int = BridgeUtil.DEFAULT_PORT, private var ui: Boolean = false) {

	/** The server socket.  */
	private var serverSocket: ServerSocket

	/** The hosts.  */
	private val hosts: MutableMap<String, Socket> = HashMap()

	/** The clis.  */
	private val clis: MutableMap<Socket, MutableMap<ByteArray, Socket>> = HashMap()

	init {
		try {
			serverSocket = ServerSocket(port)
		} catch (e: IOException) {
			throw e
		}
	}

	/**
	 * Start. TODO ui
	 */
	fun start() {
		val log = if (ui && isUIEnabled) {
			getLog("Server")
		} else {
			null
		}
		log?.add("Server init")
		try {
			serverSocket.soTimeout = 500
			while (System.`in`.available() == 0) try {
				if (log != null) {
					log[0] = "Main Loop"
					val i = AtomicInteger(1)
					clis.forEach { (so: Socket, cli: Map<ByteArray, Socket>) ->
						val out = i.get().toString() + " " + so + " " + cli.values + " " + so.getAveragePingTime() + "ms " + cli.values.map { it.getAveragePingTime() }
						if (log.size <= i.get()) {
							log.add(out)
						} else {
							log[i.get()] = out
						}
						i.getAndIncrement()
					}
					if (clis.isEmpty()) if (log.size <= i.get()) {
						log.add("")
					} else {
						log[i.get()] = ""
					}
				}
				val s = serverSocket.accept()
				if (s != null) {

					s.setupPeriodicInternalPing(1000)

					// Host
					s.setHandle(1.toByte()) { type: Byte, data: ByteArray ->
						var out: String
						var value: ByteArray
						do {
							value = ByteArray(16)
							val r = Random()
							r.nextBytes(value)
							out = String(Base64.getEncoder().encode(value), Charsets.UTF_8)
						} while (hosts.containsKey(out)) // Maybe Error
						val cli: MutableMap<ByteArray, Socket> = HashMap()
						clis[s] = cli
						hosts[out] = s
						try {
							if (!s.isClosed)
								s.sendData(1.toByte(), value)
						} catch (e: SocketException) {
							e.printStackTrace()
						}
						s.setHandle(6.toByte()) { typ: Byte, dat: ByteArray ->
							if (typ.toInt() == 2) try {
								val it: MutableIterator<Map.Entry<ByteArray, Socket>> = cli.entries.iterator()
								while (it.hasNext()) {
									val (key, value1) = it.next()
									if (key.contentEquals(dat)) {
										value1.sendClose()
										value1.close()
										it.remove()
									}
								}
							} catch (e: IOException) {
								e.printStackTrace()
							}
						}
						s.setHandle(5.toByte()) { typ: Byte, dat: ByteArray ->
							val bb = ByteBuffer.wrap(dat)
							val typee = bb.get()
							val addrLength = bb.getInt()
							val addr = ByteArray(addrLength)
							bb[addr]
							val remData = ByteArray(dat.size - addrLength - 4)
							bb[remData, 1, remData.size - 1]
							remData[0] = typee
							val wild = addr.contentEquals(BridgeUtil.WILDCARD)
							if ("true" == System.getProperty("debug")) println(String(remData))
							if ("true" == System.getProperty("debug")) println(addr.contentToString())
							if ("true" == System.getProperty("debug")) println(
								cli.map { (addr, value1): Map.Entry<ByteArray, Socket> ->
									Entry(addr.contentToString(), value1)
								}.toList()
							)
							cli.forEach { (addrL: ByteArray, ss: Socket) ->
								if (wild || addr.contentEquals(addrL)) try {
									ss.sendData(5.toByte(), remData)
								} catch (e: SocketException) {
									e.printStackTrace()
								}
							}
						}
						s.setHandle(7.toByte()) { typ: Byte, dat: ByteArray ->
							try {
								cli[dat]!!.sendClose()
								cli.remove(dat)
							} catch (e: SocketException) {
								e.printStackTrace()
							}
						}
						val appendix = out + "|" + s.inetAddress.hostAddress + ":" + s.port
						add(Runnable {
							if (!s.isConnected || s.isClosed) try {
								for ((_, value1) in cli) {
									value1.sendClose()
									value1.close()
								}
								clis.remove(s)
								hosts.remove(out)
								remove("KillHost$appendix")
							} catch (e: IOException) {
								e.printStackTrace()
							}
						}, "KillHost$appendix")
					}
					// Connect
					s.setHandle(2.toByte()) { type: Byte, data: ByteArray ->
						try {
							val buff = ByteBuffer.wrap(data)
							val length = buff.getInt()
							val domain = ByteArray(length)
							buff[domain]
							val port = buff.getInt()
							val conn = java.net.Socket(String(domain, Charsets.UTF_8), port)
							s.setHandle(4.toByte()) { typ: Byte, dat: ByteArray ->
								if (!conn.isClosed) {
									try {
										conn.getOutputStream().write(dat)
										conn.getOutputStream().flush()
									} catch (e: IOException) {
										e.printStackTrace()
									}
								}
							}
							val appendix = String(domain) + ":" + port + "|" + s.inetAddress.hostAddress + ":" + s.port
							add(Runnable {
								try {
									if (conn.getInputStream().available() > 0) {
										val baos = ByteArrayOutputStream()
										while (conn.getInputStream().available() > 0 && baos.size() <= 1073741824) {
											val bArr = ByteArray(conn.getInputStream().available().coerceAtMost(1073741824 - baos.size()))
											val read = conn.getInputStream().read(bArr)
											baos.write(bArr, 0, read)
										}
										if (!s.isClosed)
											s.sendData(4.toByte(), baos.toByteArray())
									}
								} catch (e: SocketException) {
									if ("Socket is closed" != e.message) {
										e.printStackTrace()
									}
								} catch (e: IOException) {
									e.printStackTrace()
								}
							}, "RecvAdapterConnect$appendix")
							add(Runnable {
								if (!s.isConnected || s.isClosed) try {
									conn.close()
									remove("RecvAdapterConnect$appendix")
									remove("KillConnect$appendix")
								} catch (e: IOException) {
									e.printStackTrace()
								} else if (!conn.isConnected || conn.isClosed) try {
									s.sendClose()
									s.close()
									remove("RecvAdapterConnect$appendix")
									remove("KillConnect$appendix")
								} catch (e: IOException) {
									e.printStackTrace()
								}
							}, "KillConnect$appendix")
						} catch (e: IOException) {
							try {
								if (!s.isClosed)
									s.sendData(2.toByte(), ByteArray(0))
							} catch (e1: SocketException) {
								e1.printStackTrace()
							}
						}
					}
					// Join
					s.setHandle(3.toByte()) { type: Byte, data: ByteArray ->
						if (hosts.containsKey(String(data))) {
							val ss = hosts[String(data)]!!
							val addrIP = s.inetAddress.address
							val addr = ByteArray(addrIP.size + 4)
							val bb = ByteBuffer.wrap(addr).order(ByteOrder.BIG_ENDIAN)
							bb.put(addrIP)
							bb.putInt(s.port)
							clis[ss]!![addr] = s
							try {
								val dat = ByteArray(addr.size + 1)
								val bba = ByteBuffer.wrap(dat).order(ByteOrder.BIG_ENDIAN)
								bba.put(1.toByte())
								bba.put(addr)
								ss.sendData(6.toByte(), dat)
							} catch (e1: SocketException) {
								e1.printStackTrace()
							}
							s.setHandle(5.toByte()) { typ: Byte, dat: ByteArray ->
								try {
									val out = ByteArray(4 + addr.size + dat.size)
									val bba = ByteBuffer.wrap(out)
									bba.order(ByteOrder.BIG_ENDIAN)
									bba.put(dat[0])
									bba.putInt(addr.size)
									bba.put(addr)
									bba.put(dat, 1, dat.size - 1)
									ss.sendData(5.toByte(), out)
								} catch (e: SocketException) {
									e.printStackTrace()
								}
							}
							val appendix = String(data) + "|" + s.inetAddress.hostAddress + ":" + s.port
							add(Runnable {
								if (!s.isConnected || s.isClosed) try {
									val dat = ByteArray(addr.size + 1)
									val bba = ByteBuffer.wrap(dat).order(ByteOrder.BIG_ENDIAN)
									bba.put(0.toByte())
									bba.put(addr)
									if (ss.isConnected && !ss.isClosed) ss.sendData(6.toByte(), dat)
									clis[ss]!!.remove(addr)
									remove("KillJoin$appendix")
								} catch (e: IOException) {
									e.printStackTrace()
								}
							}, "KillJoin$appendix")
						} else try {
							if (!s.isClosed)
								s.sendData(3.toByte(), ByteArray(0))
						} catch (e: SocketException) {
							e.printStackTrace()
						}
					}
				}
			} catch (ignored: SocketTimeoutException) {
				continue
			} catch (e: Exception) {
				e.printStackTrace()
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}
}
