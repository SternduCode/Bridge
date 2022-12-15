package com.sterndu.bridge;

import java.io.*;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.*;
import com.sterndu.data.transfer.secure.*;
import com.sterndu.multicore.Updater;

public class BridgeServer {
	private ServerSocket serverSocket;

	private final Map<String, Socket> hosts = new HashMap<>();

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
						String out;
						byte[] value;
						do {
							value = new byte[16];
							Random r = new Random();
							r.nextBytes(value);
							out = new String(Base64.getEncoder().encode(value));
						} while (hosts.containsKey(out));
						hosts.put(out, s);
						try {
							s.sendData((byte) 1, value);
						} catch (SocketException e) {
							e.printStackTrace();
						}
					});
					// Connect
					s.setHandle((byte) 2, (type, data) -> {
						try {
							ByteBuffer buff = ByteBuffer.wrap(data);
							int length = buff.getInt();
							byte[] domain = new byte[length];
							buff.get(domain);
							int port = buff.getInt();
							java.net.Socket conn = new java.net.Socket(new String(domain), port);
							s.setHandle((byte) 4, (typ, dat) -> {
								try {
									conn.getOutputStream().write(dat);
									conn.getOutputStream().flush();
								} catch (IOException e) {
									e.printStackTrace();
								}
							});
							Updater.getInstance().add((Runnable) () -> {
								try {
									if (conn.getInputStream().available() > 0) {
										ByteArrayOutputStream baos = new ByteArrayOutputStream();
										while (conn.getInputStream().available() > 0 && baos.size() <= 1073741824)
											baos.write(conn.getInputStream().read());
										s.sendData((byte) 4, baos.toByteArray());
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}, "RecvAdapter");
							Updater.getInstance().add((Runnable) () -> {
								if (!s.isConnected() || s.isClosed()) try {
									conn.close();
									Updater.getInstance().remove("RecvAdapter");
									Updater.getInstance().remove("Kill");
								} catch (IOException e) {
									e.printStackTrace();
								}
								else if (!conn.isConnected() || conn.isClosed()) try {
									s.sendClose();
									s.close();
									Updater.getInstance().remove("RecvAdapter");
									Updater.getInstance().remove("Kill");
								} catch (IOException e) {
									e.printStackTrace();
								}
							}, "Kill");
						} catch (IOException e) {
							try {
								s.sendData((byte) 2, new byte[0]);
							} catch (SocketException e1) {
								e1.printStackTrace();
							}
						}
					});
					// Join
					s.setHandle((byte) 3, (type, data) -> {

						if (hosts.containsKey(new String(data))) {
							Socket ss = hosts.get(new String(data));
							ss.setHandle((byte) 4, (typ, dat) -> {
								try {
									s.sendData((byte) 4, dat);
								} catch (SocketException e) {
									e.printStackTrace();
								}
							});
							s.setHandle((byte) 4, (typ, dat) -> {
								try {
									ss.sendData((byte) 4, dat);
								} catch (SocketException e) {
									e.printStackTrace();
								}
							});
							Updater.getInstance().add((Runnable) () -> {
								if (!s.isConnected() || s.isClosed()) try {
									ss.sendClose();
									ss.close();
									Updater.getInstance().remove("Kill");
								} catch (IOException e) {
									e.printStackTrace();
								}
								else if (!ss.isConnected() || ss.isClosed()) try {
									s.sendClose();
									s.close();
									Updater.getInstance().remove("Kill");
								} catch (IOException e) {
									e.printStackTrace();
								}
							}, "Kill");
						} else try {
							s.sendData((byte) 2, new byte[0]);
						} catch (SocketException e) {
							e.printStackTrace();
						}
					});
				}
			} catch (Exception e) {
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
