package com.sterndu.bridge;

import java.io.IOException;
import com.sterndu.data.transfer.secure.*;

public class BridgeServer {
	private ServerSocket serverSocket;

	public BridgeServer() {
		try {
			serverSocket = new ServerSocket(BridgeUtil.DEFAULT_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BridgeServer(int port) {
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		try {
			serverSocket.setSoTimeout(500);
			while (System.in.available() == 0) try {
				Socket s = serverSocket.accept();
				if (s != null) {
					// Host
					s.setHandle((byte) 1, (type, data) -> {

					});
					// Connect
					s.setHandle((byte) 2, (type, data) -> {

					});
					// Join
					s.setHandle((byte) 3, (type, data) -> {

					});
				}
			} catch (Exception e) {
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
