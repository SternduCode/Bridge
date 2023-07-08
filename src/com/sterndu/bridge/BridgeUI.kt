@file:JvmName("BridgeUI")
package com.sterndu.bridge

import com.sterndu.multicore.Updater
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object BridgeUI {

	private var it: Iterator<Map.Entry<Any, ArrayList<String>>>? = null

	/** The current.  */
	private var current: Pair<Any, ArrayList<String>>? = null

	/** The current time.  */
	private var currentTime: Long = 0

	/** The logs.  */
	private val logs = ConcurrentHashMap<Any, ArrayList<String>>()

	/**
	 * Instantiates a new bridge UI.
	 */
	init {
		if (isUIEnabled) {
			it = logs.entries.iterator()
			Updater.add(Runnable {
				val localIt = it!!
				if (!localIt.hasNext()) it = logs.entries.iterator() else if (System.currentTimeMillis() - currentTime >= showTime) {
					current = localIt.next().toPair()
					currentTime = System.currentTimeMillis()
				} else {
					try {
						if (checkOsWindows())
							ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor()
						else
							ProcessBuilder("clear").inheritIO().start().waitFor()
					} catch (e: InterruptedException) {
						e.printStackTrace()
					} catch (e: IOException) {
						e.printStackTrace()
					}
					println(current!!.first.toString())
					val li = current!!.second
					for (i in 0..49) if (li.size > i) println(li[i])
					System.out.flush()
				}
			}, "Bridge-UI", 1000)
		}
	}

	/**
	 * Check os windows.
	 *
	 * @return true, if successful
	 */
	private fun checkOsWindows()= System.getProperty("os.name").lowercase().contains("win")

	private fun init() {
		if (checkOsWindows()) {
			// tets ps
			// run (Get-Host).ui.rawui.windowsize
			// if error do
			// mode con lines=50
			// mode con /status
			try {
				ProcessBuilder("cmd", "/c", "mode", "con", "lines=50").inheritIO().start().waitFor()
			} catch (e: InterruptedException) {
				e.printStackTrace()
			} catch (e: IOException) {
				e.printStackTrace()
			}
		} else {
			try {
				ProcessBuilder("printf", "'\\033[8;40;50t'").inheritIO().start().waitFor()
			} catch (e: InterruptedException) {
				e.printStackTrace()
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
	}

	/**
	 * Gets the log.
	 *
	 * @param obj the obj
	 * @return the log
	 */
	@JvmStatic
	fun getLog(obj: Any): MutableList<String> {
		if (logs.isEmpty()) {
			init()
		}
		if (logs.containsKey(obj)) return logs[obj]!!
		val li = ArrayList<String>()
		logs[obj] = li
		return li
	}

		/** The Constant showTime.  */
		private const val showTime = 8000L
		@JvmStatic
		val isUIEnabled: Boolean
			get() = true

		/**
		 * The main method.
		 *
		 * @param args the arguments
		 */
		@JvmStatic
		fun main(args: Array<String>) {
			val li = getLog("                         UI")
			li.add("Uff")
			li.add("Hello")
			li.add("10.243.54.16")
		}
}
