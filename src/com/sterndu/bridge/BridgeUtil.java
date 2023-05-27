package com.sterndu.bridge;

import java.util.Arrays;
import java.util.stream.*;

// TODO: Auto-generated Javadoc
/**
 * The Class BridgeUtil.
 */
public class BridgeUtil {

	/** The Constant DEFAULT_PORT. */
	public static final int DEFAULT_PORT = 55601;

	/** The Constant WILDCARD. */
	public static final byte[] WILDCARD = {
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	};

	/**
	 * Split.
	 *
	 * @param url the url
	 * @return the bridge protocol bundle
	 */
	public static BridgeProtocolBundle split(String url) {
		if (url.startsWith("bridge://")) {
			String domain;
			int port;
			String endPoint;
			String data;
			url = url.substring("bridge://".length());
			if (url.startsWith("[")) {
				int idxEndDomain = url.indexOf(']');
				if (idxEndDomain == -1) return null;
				domain = url.substring(0, idxEndDomain + 1);
				url = url.substring(idxEndDomain + 2);
			} else {
				int idxEndDomain = url.indexOf(':');
				boolean isSlash = false;
				if (idxEndDomain == -1) {
					idxEndDomain = url.indexOf('/');
					if (idxEndDomain == -1) return null;
					idxEndDomain--;
					isSlash = true;
				}
				domain = url.substring(0, idxEndDomain + (isSlash ? 1 : 0));
				url = url.substring(idxEndDomain + 1);
			}
			String[] sp = url.split("/");
			if (sp.length <= 0) return null;
			try {
				if (sp[0] == "") if (sp.length > 1) sp = Arrays.copyOfRange(sp, 1, sp.length);
				else return null;
				port = Integer.parseInt(sp[0]);
				if (sp.length <= 1) return null;
				if ("host".equals(sp[1])) {
					endPoint = sp[1];
					data = sp.length > 2
							? Stream.of(Arrays.copyOfRange(sp, 2, sp.length)).collect(Collectors.joining("/"))
									: "";
				} else if (("join".equals(sp[1]) || "connect".equals(sp[1])) && sp.length > 2) {
					endPoint = sp[1];
					data = Stream.of(Arrays.copyOfRange(sp, 2, sp.length)).collect(Collectors.joining("/"));
				} else return null;
			} catch (NumberFormatException e) {
				port = DEFAULT_PORT;
				if ("host".equals(sp[0])) {
					endPoint = sp[0];
					data = "";
				} else if (("join".equals(sp[0]) || "connect".equals(sp[0])) && sp.length > 1) {
					endPoint = sp[0];
					data = Stream.of(Arrays.copyOfRange(sp, 1, sp.length)).collect(Collectors.joining("/"));
				} else return null;
			}
			return new BridgeProtocolBundle(domain, port, endPoint, data);
		}
		return null;
	}

}
