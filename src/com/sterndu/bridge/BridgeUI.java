package com.sterndu.bridge;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.sterndu.multicore.Updater;


// TODO: Auto-generated Javadoc
// TODO implement ui in all modes & multicore + json must still be used from old packager
/**
 * The Class BridgeUI.
 */
public class BridgeUI {

	/** The Constant INSTANCE. */
	public static final BridgeUI INSTANCE;

	static {
		INSTANCE = new BridgeUI();
	}

	/** The Constant showTime. */
	private static final long showTime = 5000l;

	/** The it. */
	private Iterator<Entry<Object, ArrayList<String>>> it;

	/** The current. */
	private Entry<Object, ArrayList<String>> current;

	/** The current time. */
	private long currentTime;

	/** The logs. */
	private final ConcurrentHashMap<Object, ArrayList<String>> logs = new ConcurrentHashMap<>();

	/**
	 * Instantiates a new bridge UI.
	 */
	private BridgeUI() {
		if (isUIEnabled()) {
			it			= logs.entrySet().iterator();
			Updater.getInstance().add((Runnable) () -> {
				if (!it.hasNext()) it = logs.entrySet().iterator();
				else if (System.currentTimeMillis() - currentTime >= showTime) {
					current		= it.next();
					currentTime	= System.currentTimeMillis();
				} else {
					try {
						new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
					} catch (InterruptedException | IOException e) {
						e.printStackTrace();
					}
					System.console().writer().println(current.getKey().toString());
					ArrayList<String> li = current.getValue();
					for (int i = 0; i < 30; i++) if (li.size() > i) System.console().writer().println(li.get(i));
					System.console().writer().flush();
				}
			}, "Bridge-UI", 100);
		}
	}

	/**
	 * Checks if is UI enabled.
	 *
	 * @return true, if is UI enabled
	 */
	public static boolean isUIEnabled() { return System.console() != null; }

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		List<String> li = INSTANCE.getLog("                         UI");
		li.add("Uff");
		li.add("Hello");
		li.add("10.243.54.16");
	}

	/**
	 * Check os windows.
	 *
	 * @return true, if successful
	 */
	private boolean checkOsWindows() { return System.getProperty("os.name").toLowerCase().contains("win"); }

	/**
	 * Gets the log.
	 *
	 * @param obj the obj
	 * @return the log
	 */
	public List<String> getLog(Object obj) {
		if (logs.size() == 0) {
			boolean windows = checkOsWindows();
			if (windows) {
				// tets ps
				// run (Get-Host).ui.rawui.windowsize
				// if error do
				// mode con lines=30
				// mode con /status
				try {
					new ProcessBuilder("cmd", "/c", "mode", "con", "lines=30").inheritIO().start().waitFor();
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
				}
			} else {

			}
		}
		if (logs.containsKey(obj)) return logs.get(obj);
		ArrayList<String> li = new ArrayList<>();
		logs.put(obj, li);
		return li;
	}

}
