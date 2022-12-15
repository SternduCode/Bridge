package com.sterndu.bridge;

import java.util.Arrays;
import java.util.stream.*;

public class BridgeUtil {

	public static final int DEFAULT_PORT = 55601;

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
			if (sp.length > 0) try {
				if (sp[0] == "") if (sp.length > 1) sp = Arrays.copyOfRange(sp, 1, sp.length);
				else return null;
				port = Integer.parseInt(sp[0]);
				if (sp.length > 1) {
					if (sp[1].equals("host")) {
						endPoint = sp[1];
						data = sp.length > 2
								? Stream.of(Arrays.copyOfRange(sp, 2, sp.length)).collect(Collectors.joining("/"))
								: "";
					} else if (sp[1].equals("join") || sp[1].equals("connect")) {
						if (sp.length > 2) {
							endPoint = sp[1];
							data = Stream.of(Arrays.copyOfRange(sp, 2, sp.length)).collect(Collectors.joining("/"));
						} else return null;
					} else return null;
				} else return null;
			} catch (NumberFormatException e) {
				port = DEFAULT_PORT;
				if (sp[0].equals("host")) {
					endPoint = sp[0];
					data = "";
				} else if (sp[0].equals("join") || sp[0].equals("connect")) {
					if (sp.length > 1) {
						endPoint = sp[0];
						data = Stream.of(Arrays.copyOfRange(sp, 1, sp.length)).collect(Collectors.joining("/"));
					} else return null;
				} else return null;
			}
			else return null;
			return new BridgeProtocolBundle(domain, port, endPoint, data);
		}
		return null;
	}

}
