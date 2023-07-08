@file:JvmName("BridgeCLI")
package com.sterndu.bridge

import com.sterndu.bridge.BridgeUI.getLog
import com.sterndu.bridge.BridgeUI.isUIEnabled
import com.sterndu.multicore.Updater.add
import com.sterndu.multicore.Updater.remove
import com.sterndu.network.balancer.Balancer
import com.sterndu.network.balancer.Tester
import com.sterndu.util.Entry
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.exitProcess

object BridgeCLI {
	/**
	 * Connect. TODO
	 *
	 * @param server     the server
	 * @param port       the port
	 * @param remote     the remote
	 * @param remotePort the remote port
	 * @param localPort  the local port
	 * @param ui         the ui
	 */
	fun connect(server: String, port: Int, remote: String, remotePort: Int, localPort: Int, ui: Boolean) {
		Thread {
			try {
				val uiLi = if (isUIEnabled && ui) getLog(
					"Connect " + localPort + " | "
							+ server + ":" + port + " | " + remote + ":" + remotePort
				) else null
				val locSocket = ServerSocket(localPort)
				uiLi?.add("Running on port: " + locSocket.localPort) ?: println("Running on port: " + locSocket.localPort)
				while (System.`in`.available() == 0) {
					val s = locSocket.accept()
					Thread {
						try {
							val appendix = ("Connect" + localPort + "|"
									+ server + ":" + port + "|"
									+ s.inetAddress.hostAddress + ":" + s.port)
							val bc = BridgeClient(server, port)
							var hc = bc.connect(remote, remotePort)
							while (hc == null) {
								hc = bc.connect(remote, remotePort)
							}
							hc.disableHandle()
							hc.sock.setHandle(hc.type) { type: Byte, data: ByteArray ->
								try {
									if ("true" == System.getProperty("debug")) println(String(data))
									s.getOutputStream().write(data)
									s.getOutputStream().flush()
								} catch (e: IOException) {
									try {
										remove("RecvAdapterConnect$appendix")
										remove("KillConnect$appendix")
										try {
											bc.sock.sendClose()
										} catch (ignored: Exception) {
											println("Socket already closed")
										}
										bc.sock.close()
										s.close()
									} catch (ex: IOException) {
										ex.initCause(e)
										ex.printStackTrace()
									}
								}
							}
							add(Runnable {
								try {
									if (s.getInputStream().available() > 0) {
										val baos = ByteArrayOutputStream()
										while (s.getInputStream().available() > 0 && baos.size() <= 1073741824) {
											val bArr = ByteArray(s.getInputStream().available().coerceAtMost(1073741824-baos.size()))
											val read = s.getInputStream().read(bArr)
											baos.write(bArr, 0 ,read)
										}
										hc.sock.sendData(hc.type, baos.toByteArray())
									}
								} catch (e: IOException) {
									try {
										remove("RecvAdapterConnect$appendix")
										remove("KillConnect$appendix")
										try {
											bc.sock.sendClose()
										} catch (ignored: Exception) {
											println("Socket already closed")
										}
										bc.sock.close()
										s.close()
									} catch (ex: IOException) {
										ex.initCause(e)
										ex.printStackTrace()
									}
								}
							}, "RecvAdapterConnect$appendix")
							add(Runnable {
								if (!bc.sock.isConnected || bc.sock.isClosed) {
									Thread {
										try {
											Thread.sleep(2000)
											s.close()
										} catch (e: InterruptedException) {
											e.printStackTrace()
										} catch (e: IOException) {
											e.printStackTrace()
										}
									}.start()
									remove("RecvAdapterConnect$appendix")
									remove("KillConnect$appendix")
								} else if (!s.isConnected || s.isClosed) try {
									bc.sock.sendClose()
									bc.sock.close()
									remove("RecvAdapterConnect$appendix")
									remove("KillConnect$appendix")
								} catch (e: IOException) {
									e.printStackTrace()
								}
							}, "KillConnect$appendix")
						} catch (e: IOException) {
							e.printStackTrace()
						}
					}.start()
				}
				locSocket.close()
			} catch (e1: IOException) {
				e1.printStackTrace()
			}
		}.start()
	}

	/**
	 * Help.
	 */
	fun help() {
		println(
			"host -s/--server sterndu.com:55601 (-p/--port 55601) (-l/--local-port 25566) connect -s/--server sterndu.com:55601 (-p/--port 55601) -t/--target lin.sterndu.com:453 join -s/--server sterndu.com:55601 (-p/--port 55601) -c/--code hvcxdrt675 server (-p/--port 55601)"
		)
		println("[[MODE] [OPTION]... ]...")
		println()
		println("modes:")
		println("  connect")
		println("  join")
		println("  host")
		println("  server")
		println("  stresstest")
		println("  announce-server")
		println("  ping")
		println()
		println(
			"Inside the () there are all modes with which the option can be used, if the mode is followed with an ! it is mandatory in that mode"
		)
		println()
		println("options:")
		println("  -s, --server ADDRESS       the bridge server it should connect to, can contain the port (connect!, join!, host!, ping!)")
		println("  -p, --port NUMBER          the port on which the server is running/should run (connect, join, host, server, announce-server, ping)")
		println("  -t, --target ADDRESS       the target server you want to connect to (connect!)")
		println("  -c, --code CODE            the code that is displayed on the 'host' you want to connect to (join!)")
		println("  -l, --local-port NUMBER    the port that should be used locally, in host mode it is the port that should be hosted," +
				" in the other modes its the port you connect to (connect, join, host!)")
		println("  -a, --announce ADDRESS     the address where the code, with which you can join the connection, will be sent to (host)")
		println("  -e, --cycles NUMBER        how many cycles the stresstest should do in the cycles phase (default: 200) (stresstest)")
		println("  -d, --duration NUMBER      how long in millis the stresstest should run in timed phase (default: 20000 (= 20sec)) (stresstest)")
		println("      --long-duration NUMBER how long in millis the stresstest should run in long duration phase (default: 50000 (= 50sec)) (stresstest)")
		println("      --timeout NUMBER       after what time in millis the stresstest is aborted (default: 80000 (= 80sec)) (stresstest)")
		println("      --wait NUMBER          how long in millis the stresstest should wait between phases (default: 2000 (= 2sec)) (stresstest)")
		println("  -x, --cores NUMBER         how many Thread/Cores the stresstest should use (default: all available (${Runtime.getRuntime().availableProcessors()})) (stresstest)")
		println("  -r, --raw                  if selected, data sent will not be encrypted (ping)")
		println("  -u, --ui                   show output in the ui (connect, join, host, server, announce-server)")
		println()
		println("You can add the option -v / --verbose  anywhere to enable debug output in all modes")
		println()
		exitProcess(0)
	}

	/**
	 * Host. TODO
	 *
	 * @param server    the server
	 * @param port      the port
	 * @param localPort the local port
	 * @param announce  the announce
	 * @param ui        the ui
	 */
	fun host(server: String, port: Int, localPort: Int, announce: String, ui: Boolean) {
		Thread {
			try {
				var uiLi: MutableList<String>? = null
				if (isUIEnabled && ui) uiLi = getLog(
					"Host " + localPort + " | "
							+ server + ":" + port + " | " + announce
				)
				val bc = BridgeClient(server, port)
				var hc: HostConnector? = null
				while (hc == null) {
					hc = bc.host()
				}
				uiLi?.add("Hosting port: " + localPort + " via: " + hc.code) ?: println("Hosting port: " + localPort + " via: " + hc.code)
				if (announce.isNotEmpty()) try {
					val sp = announce.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()
					val announcePort = sp.last()
					val announceLocal = announce.substring(0, announce.length - announcePort.length - 1)
					val announceSocket = com.sterndu.data.transfer.secure.Socket(announceLocal, announcePort.toInt())
					announceSocket.sendData(0xa0.toByte(), hc.code!!.toByteArray(Charsets.UTF_8))
					while (!announceSocket.initialized) try {
						Thread.sleep(5L)
					} catch (e: Exception) {
						e.printStackTrace()
					}
					try {
						Thread.sleep(20L)
					} catch (e: Exception) {
						e.printStackTrace()
					}
					announceSocket.sendClose()
					announceSocket.close()
				} catch (e: Exception) {
					println("$announce is not a correct hostname:port pair")
				}
				val connections: MutableMap<ByteArray, Socket> = HashMap()
				hc.announceConnector.handle = { type: Byte, addr: ByteArray ->
					val appendix = "Host" + localPort + "|" + server + ":" + port + "|" + String(addr)
					if (type.toInt() == 1) try {
						var lSock: Socket
						connections[addr] = Socket("localhost", localPort).also { lSock = it }
						if ("true" == System.getProperty("debug")) println(addr.contentToString())
						add(Runnable {
							try {
								if (lSock.getInputStream().available() > 0) {
									val baos = ByteArrayOutputStream()
									baos.write(0)
									baos.write(0)
									baos.write(0)
									baos.write(addr.size)
									baos.write(addr)
									while (lSock.getInputStream().available() > 0 && baos.size() <= 1073741824) {
										val b_arr = ByteArray(lSock.getInputStream().available().coerceAtMost(1073741824-baos.size()))
										val read = lSock.getInputStream().read(b_arr)
										baos.write(b_arr, 0, read)
									}
									if ("true" == System.getProperty("debug")) println(baos)
									hc.normalConnector.sendData(baos.toByteArray())
								}
							} catch (e: IOException) {
								e.printStackTrace()
							}
						}, "RecvAdapterConnect$appendix")
						add(Runnable {
							if (!hc.normalConnector.sock.isConnected || hc.normalConnector.sock.isClosed) try {
								lSock.close()
								remove("RecvAdapterConnect$appendix")
								remove("KillConnect$appendix")
							} catch (e: IOException) {
								e.printStackTrace()
							} else if (!lSock.isConnected || lSock.isClosed) try {
								hc.announceConnector.sendData(2.toByte(), addr)
								remove("RecvAdapterConnect$appendix")
								remove("KillConnect$appendix")
							} catch (e: IOException) {
								e.printStackTrace()
							}
						}, "KillConnect$appendix")
					} catch (e: Exception) {
						e.printStackTrace()
					} else try {
						connections[addr]!!.close()
						connections.remove(addr)
					} catch (e: IOException) {
						e.printStackTrace()
					}
				}
				hc.normalConnector.handle = { type: Byte?, data: ByteArray ->
					try {

						// bb.putInt(address.length + 4);
						// bb.put(address);
						// bb.putInt(s.getPort());
						// bb.put(dat, 1, dat.length - 1);
						val bb = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
						val length = bb.getInt()
						val addr = ByteArray(length)
						bb[addr]
						if ("true" == System.getProperty("debug")) println(addr.contentToString())
						val dat = ByteArray(data.size - bb.position())
						bb[dat]
						if ("true" == System.getProperty("debug")) println(String(dat))
						if ("true" == System.getProperty("debug")) println(
							connections.map { (key, value): Map.Entry<ByteArray, Socket> ->
								Entry(key.contentToString(), value)
							}
						)
						if ("true" == System.getProperty("debug")) println(connections[addr])
						if ("true" == System.getProperty("debug")) println(addr.contentToString())
						for ((key, value) in connections) if (key.contentEquals(addr)) {
							val outputStream = value.getOutputStream()
							outputStream.write(dat)
							outputStream.flush()
						}
					} catch (e: IOException) {
						e.printStackTrace()
					}
				}
			} catch (e1: IOException) {
				e1.printStackTrace()
			}
		}.start()
	}

	/**
	 * Join. TODO
	 *
	 * @param server    the server
	 * @param port      the port
	 * @param code      the code
	 * @param localPort the local port
	 * @param ui        the ui
	 */
	fun join(server: String, port: Int, code: String, localPort: Int, ui: Boolean) {
		Thread {
			try {
				val uiLi: MutableList<String>? = if (ui && isUIEnabled) getLog(
					"Join " + localPort + " | "
							+ server + ":" + port + " | " + code
				) else null
				val ss = ServerSocket(localPort)
				uiLi?.add("Running on port: " + ss.localPort) ?: println("Running on port: " + ss.localPort)
				while (System.`in`.available() == 0) {
					val s = ss.accept()
					Thread {
						try {
							val appendix = ("Connect" + localPort + "|"
									+ server + ":" + port + "|"
									+ s.inetAddress.hostAddress + ":" + s.port)
							val bc = BridgeClient(server, port)
							var hc = bc.join(code)
							while (hc == null) {
								hc = bc.join(code)
							}
							hc.handle = { type: Byte, data: ByteArray ->
								try {
									if ("true" == System.getProperty("debug")) println(String(data))
									s.getOutputStream().write(data)
									s.getOutputStream().flush()
								} catch (e: IOException) {
									try {
										remove("RecvAdapterConnect$appendix")
										remove("KillConnect$appendix")
										try {
											bc.sock.sendClose()
										} catch (ignored: Exception) {
											println("Socket already closed")
										}
										bc.sock.close()
										s.close()
									} catch (ex: IOException) {
										ex.initCause(e)
										ex.printStackTrace()
									}
								}
							}
							add(Runnable {
								try {
									if (s.getInputStream().available() > 0) {
										val baos = ByteArrayOutputStream()
										while (s.getInputStream().available() > 0 && baos.size() <= 1073741824) {
											val bArr = ByteArray(s.getInputStream().available().coerceAtMost(1073741824-baos.size()))
											val read = s.getInputStream().read(bArr)
											baos.write(bArr, 0, read)
										}
										hc.sendData(hc.type, baos.toByteArray())
									}
								} catch (e: IOException) {
									try {
										remove("RecvAdapterConnect$appendix")
										remove("KillConnect$appendix")
										try {
											bc.sock.sendClose()
										} catch (ignored: Exception) {
											println("Socket already closed")
										}
										bc.sock.close()
										s.close()
									} catch (ex: IOException) {
										ex.initCause(e)
										ex.printStackTrace()
									}
								}
							}, "RecvAdapterConnect$appendix")
							add(Runnable {
								if (!bc.sock.isConnected || bc.sock.isClosed) {
									Thread {
										try {
											Thread.sleep(2000)
											s.close()
										} catch (e: InterruptedException) {
											e.printStackTrace()
										} catch (e: IOException) {
											e.printStackTrace()
										}
									}.start()
									remove("RecvAdapterConnect$appendix")
									remove("KillConnect$appendix")
								} else if (!s.isConnected || s.isClosed) try {
									bc.sock.sendClose()
									bc.sock.close()
									remove("RecvAdapterConnect$appendix")
									remove("KillConnect$appendix")
								} catch (e: IOException) {
									e.printStackTrace()
								}
							}, "KillConnect$appendix")
						} catch (e: Exception) {
							e.printStackTrace()
						}
					}.start()
				}
				ss.close()
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}.start()
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	@JvmStatic
	fun main(args: Array<String>) {
		if (args.isEmpty()) help()
		var argCollector: ArgCollector? = null
		val it: Iterator<String> = args.iterator()
		while (it.hasNext()) {
			val arg = it.next()
			println(arg)
			when (arg) {
				"host" -> {
					argCollector?.run()
					argCollector = object : ArgCollector() {
						override fun run() {
							if ("" == server) help()
							host(server, port, localPort, announce, ui)
						}
					}
				}

				"join" -> {
					argCollector?.run()
					argCollector = object : ArgCollector() {
						override fun run() {
							if ("" == server) help()
							if ("" == code) help()
							join(server, port, code, localPort, ui)
						}
					}
				}

				"connect" -> {
					argCollector?.run()
					argCollector = object : ArgCollector() {
						override fun run() {
							if ("" == server) help()
							if ("" == target) help()
							if (targetPort == 0) help()
							connect(server, port, target, targetPort, localPort, ui)
						}
					}
				}

				"server" -> {
					argCollector?.run()
					argCollector = object : ArgCollector() {
						override fun run() {
							Thread {
								try {
									BridgeServer(port, ui).start()
								} catch (e: IOException) {
									e.printStackTrace()
								}
							}.start()
						}
					}
				}

				"announce-server" -> {
					argCollector?.run()
					argCollector = object : ArgCollector() {

						init {
							port = BridgeUtil.DEFAULT_PORT + 1
						}

						override fun run() {
							announceServer(port, ui)
						}
					}
				}

				"stresstest" -> {
					argCollector?.run()
					argCollector = object : ArgCollector() {
						override fun run() {
							stresstest(cycles, duration, longDuration, timeout, wait, cores)
						}
					}
				}

				"ping" -> {
					argCollector?.run()
					argCollector = object  : ArgCollector() {
						override fun run() {
							ping(server, port, raw)
						}
					}
				}

				"-s", "--server" -> {
					if (argCollector != null) argCollector.server = it.next() else help()
				}

				"-p", "--port" -> {
					argCollector?.setPort(it.next()) ?: help()
				}

				"-t", "--target" -> {
					if (argCollector != null) argCollector.target = it.next() else help()
				}

				"-c", "--code" -> {
					if (argCollector != null) argCollector.code = it.next() else help()
				}

				"-l", "--local-port" -> {
					argCollector?.setLocalPort(it.next()) ?: help()
				}

				"-a", "--announce" -> {
					if (argCollector != null) argCollector.announce = it.next() else help()
				}

				"-e", "--cycles" -> {
					argCollector?.setCycles(it.next()) ?: help()
				}

				"-d", "--duration" -> {
					argCollector?.setDuration(it.next()) ?: help()
				}

				"--long-duration" -> {
					argCollector?.setLongDuration(it.next()) ?: help()
				}

				"--timeout" -> {
					argCollector?.setTimeout(it.next()) ?: help()
				}

				"--wait" -> {
					argCollector?.setWait(it.next()) ?: help()
				}

				"-x", "--cores" -> {
					argCollector?.setCores(it.next()) ?: help()
				}

				"-r", "--raw" -> {
					if (argCollector != null) argCollector.raw = true else help()
				}

				"-u", "--ui" -> {
					if (argCollector != null) argCollector.ui = true else help()
				}

				"-v", "--verbose" -> {
					System.setProperty("debug", "true")
				}

				else -> help()
			}
		}
		argCollector?.run()
	}

	private fun ping(server: String, port: Int, raw: Boolean) {
		val sock = com.sterndu.data.transfer.secure.Socket(server, port)
		while (!sock.initialized) {
			Thread.sleep(1)
		}
		val millis = if (raw) {
			sock.internalPing()
		} else {
			sock.ping()
		}
		try {
			sock.sendClose()
		} catch (e: SocketException) {
			println("Socket already closed")
		}
		sock.close()
		println("Ping to $server:$port took ${millis}ms")
	}

	private fun stresstest(cycles: Long, duration: Long, longDuration: Long, timeout: Long, wait: Long, cores: Int) {
		println(Runtime.getRuntime().totalMemory())
		println(Runtime.getRuntime().maxMemory())
		println(Runtime.getRuntime().freeMemory())
		val value: Long = 15000
		val r = Runnable { Tester.func2(value) }
		val ds = Balancer.longDurationTest(r, longDuration, cores, 4)
		println(ds.contentToString())
		val b2 = Balancer()
		println("The test is to calculate a sliver of Mandelbrot with a max Iteration count of : $value")
		println("The test is running on $cores Threads/Cores simultaneously")
		val data = Balancer.runSelfTest(
			r, timeout, cycles, duration, cores,
			wait, System.out
		)
		println("S means Singlecore Score; M means Multicore Score")
		println(
			"B means Score is determined by performance in the first test;\nB2 means Score is determined by performance in the second Test"
		)
		println("s-b:" + data[0])
		println("s-b2:" + data[1])
		println("m-b:" + data[2])
		println("m-b2:" + data[3])
		//b2.runNetworkTest(new byte[] {5, 8, 2, 9}, new AddressPortTuple("localhost",
		// 25555));
	}

	private fun announceServer(port: Int, ui: Boolean) {
		Thread {
			var uiLi: MutableList<String>? = null
			if (isUIEnabled && ui) uiLi = getLog("Announce-Server $port")
			val server = com.sterndu.data.transfer.secure.ServerSocket(port)
			uiLi?.add("Running on Port ${server.localPort}") ?: println("Running on Port ${server.localPort}")
			while (System.`in`.available() == 0) {
				val sock = server.accept()
				sock.setHandle(0xa0.toByte()) { type, data ->
					val date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").format(Instant.now())
					uiLi?.add("[$date] ${sock.inetAddress.hostName} " + String(data, Charsets.UTF_8)) ?: println("[$date] ${sock.inetAddress.hostName} " + String(data, Charsets.UTF_8))
					try {
						sock.sendClose()
					} catch (e: Exception) {
						println("Socket already closed")
					}
					sock.close()
				}
			}
		}.start()
	}

	/**
	 * The Interface ArgCollector.
	 */
	private abstract class ArgCollector {

		protected var cycles = 200L
		protected var duration = 20000L
		protected var longDuration = 50000L
		protected var timeout = 80000L
		protected var wait = 2000L
		protected var cores = Runtime.getRuntime().availableProcessors()

		var raw = false;

		var code = ""
		var server = ""
			set(server) {
				if (server.isNotEmpty()) {
					if (server.contains("[")) { // IPv6 address
						val sp = server.split("]").dropLastWhile { it.isEmpty() }.toTypedArray()
						if (sp.size >= 2) {
							field = sp[0] + "]"
							val sPort = sp[1].substring(1)
							setPort(sPort)
							return
						}
					} else if (server.contains(":")) { // Has port
						val sPort = server.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()[1]
						field = server.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()[0]
						setPort(sPort)
						return
					}
					field = server // doesn't contain port
				}
			}
		var target = ""
			set(target) {
				if (target.isNotEmpty()) if (target.contains("[")) { // IPv6 address
					val sp = target.split("]").dropLastWhile { it.isEmpty() }.toTypedArray()
					if (sp.size >= 2) {
						field = sp[0] + "]"
						val sPort = sp[1].substring(1)
						if (sPort.isNotEmpty()) try {
							val iPort = sPort.toInt()
							if (iPort < 0 || iPort > 65535) throw NumberFormatException()
							targetPort = iPort
							return
						} catch (e: NumberFormatException) {
							System.err.println("$sPort is not valid Port number")
							return
						}
					}
				} else if (target.contains(":")) { // Has port
					val sPort = target.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()[1]
					field = target.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()[0]
					if (sPort.isNotEmpty()) try {
						val iPort = sPort.toInt()
						if (iPort < 0 || iPort > 65535) throw NumberFormatException()
						targetPort = iPort
						return
					} catch (e: NumberFormatException) {
						System.err.println("$sPort is not valid Port number")
						return
					}
				}
				field = target
			}
		var announce = ""

		/** The ports.  */
		protected var port = BridgeUtil.DEFAULT_PORT
		protected var localPort = 0
		protected var targetPort = 0

		/** The ui.  */
		var ui = false

		/**
		 * Run.
		 */
		abstract fun run()

		/**
		 * Sets the local port.
		 *
		 * @param port the new local port
		 */
		fun setLocalPort(port: String) {
			if (port.isNotEmpty()) try {
				val iPort = port.toInt()
				if (iPort < 0 || iPort > 65535) throw NumberFormatException()
				localPort = iPort
				return
			} catch (e: NumberFormatException) {
				System.err.println("$port is not valid Port number")
				return
			}
			help()
		}

		/**
		 * Setport.
		 *
		 * @param port the new port
		 */
		fun setPort(port: String) {
			if (port.isNotEmpty()) try {
				val iPort = port.toInt()
				if (iPort < 0 || iPort > 65535) throw NumberFormatException()
				this.port = iPort
				return
			} catch (e: NumberFormatException) {
				System.err.println("$port is not valid Port number")
				return
			}
			help()
		}

		fun setCycles(value: String) {
			if (value.isNotEmpty()) try {
				cycles = value.toLong()
				return
			} catch (e: java.lang.NumberFormatException) {
				System.err.println("$value is not valid number")
				return
			}
			help()
		}
		fun setDuration(value: String) {
			if (value.isNotEmpty()) try {
				duration = value.toLong()
				return
			} catch (e: java.lang.NumberFormatException) {
				System.err.println("$value is not valid number")
				return
			}
			help()
		}
		fun setLongDuration(value: String) {
			if (value.isNotEmpty()) try {
				longDuration = value.toLong()
				return
			} catch (e: java.lang.NumberFormatException) {
				System.err.println("$value is not valid number")
				return
			}
			help()
		}
		fun setTimeout(value: String) {
			if (value.isNotEmpty()) try {
				timeout = value.toLong()
				return
			} catch (e: java.lang.NumberFormatException) {
				System.err.println("$value is not valid number")
				return
			}
			help()
		}
		fun setWait(value: String) {
			if (value.isNotEmpty()) try {
				wait = value.toLong()
				return
			} catch (e: java.lang.NumberFormatException) {
				System.err.println("$value is not valid number")
				return
			}
			help()
		}

		fun setCores(value: String) {
			if (value.isNotEmpty()) try {
				cores = value.toInt()
				return
			} catch (e: java.lang.NumberFormatException) {
				System.err.println("$value is not valid number")
				return
			}
			help()
		}
	}
}
