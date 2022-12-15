package com.sterndu.bridge;

public class BridgeTester {
	public static void main(String[] args) {
		String join = "bridge://domain/join/code";
		String connect = "bridge://domain:64/connect/domain:port";
		String host = "bridge://domain:2344/host";
		System.out.println(BridgeUtil.split(join));
		System.out.println(BridgeUtil.split(connect));
		System.out.println(BridgeUtil.split(host));
		new BridgeServer().start();
	}

}
