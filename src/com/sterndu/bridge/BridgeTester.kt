@file:JvmName("BridgeTester")
package com.sterndu.bridge

import com.sterndu.multicore.LoggingUtil
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.system.exitProcess

object BridgeTester {
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	@JvmStatic
	fun main(args: Array<String>) {
		val logger = LoggingUtil.getLogger("BridgeTester")
		val join = "bridge://domain/join/code"
		val connect = "bridge://domain:64/connect/domain:port"
		val host = "bridge://domain:2344/host/64"
		logger.info(BridgeUtil.split(join).toString())
		logger.info(BridgeUtil.split(connect).toString())
		logger.info(BridgeUtil.split(host).toString())
		System.setProperty("debug", "true")
//		Thread({
//			try {
//				val bc = BridgeClient("localhost")
//				val conn = bc.host()
//				conn.normalConnector.handle = { typ, dat ->
//					val bb = ByteBuffer.wrap(dat)
//					val len = bb.getInt()
//					val addr = ByteArray(len)
//					bb[addr]
//					val remData = ByteArray(dat.size - len - 4)
//					bb[remData]
//					logger.info(typ.toString() + " " + String(remData))
//				}
//				logger.info(conn.code)
//			} catch (e: IOException) {
//				e.printStackTrace()
//			}
//		}, "Host").start()
//		Thread({
//			try {
//				val bc = BridgeClient("localhost")
//				while (System.`in`.available() == 0) try {
//					Thread.sleep(5)
//				} catch (e: InterruptedException) {
//					e.printStackTrace()
//				}
//				val str = ByteArray(System.`in`.available() - 2)
//				System.`in`.read(str)
//				System.`in`.read()
//				System.`in`.read()
//				val code = String(str)
//				val conn = bc.join(code)
//				conn.sendData("FFS".toByteArray(Charsets.UTF_8))
//			} catch (e: IOException) {
//				e.printStackTrace()
//			}
//		}, "Join").start()
		BridgeServer().start()
		exitProcess(0)
	}
}
