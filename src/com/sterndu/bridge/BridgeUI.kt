@file:JvmName("BridgeUI")
package com.sterndu.bridge

import com.sterndu.multicore.LoggingUtil
import com.sterndu.multicore.Updater
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

object BridgeUI {

	private var it: Iterator<Map.Entry<Any, ArrayList<String>>>? = null

	/** The current.  */
	private var current: Pair<Any, ArrayList<String>>? = null

	/** The current time.  */
	private var currentTime: Long = 0

	/** The logs.  */
	private val logs = ConcurrentHashMap<Any, ArrayList<String>>()

	private var lastSize = 0
	private var lastHashCode = 0

	private val logger: Logger = LoggingUtil.getLogger("BridgeUI")

	/** The Constant showTime.  */
	private const val SHOWTIME = 8000L

	@JvmStatic
	val isUIEnabled = true

	/**
	 * Instantiates a new bridge UI.
	 */
	init {
		if (isUIEnabled) {
			it = logs.entries.iterator()
			Updater.add("Bridge-UI", 1000) {
                val localIt = it!!
                if (!localIt.hasNext()) {
                    it = logs.entries.iterator()
                    lastSize = 0
                    lastHashCode = 0
                } else if (System.currentTimeMillis() - currentTime >= SHOWTIME) {
                    current = localIt.next().toPair()
                    currentTime = System.currentTimeMillis()
                    lastSize = 0
                    lastHashCode = 0
                } else {
                    if (lastSize != current!!.second.size || lastHashCode != current!!.second.hashCode()) {
                        try {
                            if (checkOsWindows())
                                ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor()
                            else
                                ProcessBuilder("clear").inheritIO().start().waitFor()
                        } catch (e: InterruptedException) {
                            logger.log(Level.WARNING, "BridgeUI", e)
                        } catch (e: IOException) {
                            logger.log(Level.WARNING, "BridgeUI", e)
                        }
                        println(current!!.first.toString())
                        logger.info(current!!.first.toString())
                        val li = current!!.second
                        for (i in 0..49) if (li.size > i) {
                            println(li[i])
                            logger.info(li[i])
                        }
                        System.out.flush()
                        lastSize = li.size
                        lastHashCode = li.hashCode()
                    }
                }
            }
        }
	}

	/**
	 * Check os windows.
	 *
	 * @return true, if successful
	 */
	private fun checkOsWindows()= System.getProperty("os.name").lowercase().contains("win")

	private fun init() {
		logger.handlers.filterIsInstance<ConsoleHandler>().onEach { it.level = Level.OFF }
		if (checkOsWindows()) {
			// test ps
			// run (Get-Host).ui.rawui.windowsize
			// if error do
			// mode con lines=50
			// mode con /status
			try {
				ProcessBuilder("cmd", "/c", "mode", "con", "lines=50").inheritIO().start().waitFor()
			} catch (e: InterruptedException) {
				logger.log(Level.WARNING, "BridgeUI", e)
			} catch (e: IOException) {
				logger.log(Level.WARNING, "BridgeUI", e)
			}
		} else {
			try {
				ProcessBuilder("printf", "'\\033[8;50;80t'").inheritIO().start().waitFor()
			} catch (e: InterruptedException) {
				logger.log(Level.WARNING, "BridgeUI", e)
			} catch (e: IOException) {
				logger.log(Level.WARNING, "BridgeUI", e)
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
