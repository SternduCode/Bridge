package com.sterndu.bridge;

import java.io.*;
import java.net.SocketException;
import java.nio.*;
import java.util.*;
import java.util.stream.Collectors;

import com.sterndu.data.transfer.secure.*;
import com.sterndu.multicore.Updater;
import com.sterndu.util.Entry;

// TODO: Auto-generated Javadoc
/**
 * The Class BridgeServer.
 */
public class BridgeServer {

	/** The server socket. */
	private ServerSocket serverSocket;

	/** The hosts. */
	private final Map<String, Socket> hosts = new HashMap<>();

	/** The clis. */
	private final Map<Socket, Map<byte[], Socket>> clis = new HashMap<>();

	/** The ui. */
	private boolean ui = false;

	/**
	 * Instantiates a new bridge server.
	 */
	public BridgeServer() {
		try {
			serverSocket = new ServerSocket(BridgeUtil.DEFAULT_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Instantiates a new bridge server.
	 *
	 * @param port the port
	 */
	public BridgeServer(int port) {
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Instantiates a new bridge server.
	 *
	 * @param port the port
	 * @param ui   the ui
	 */
	public BridgeServer(int port, boolean ui) {
		this(port);
		this.ui = ui;

	}

	/**
	 * Start. TODO ui
	 */
	public void start() {
		try {
			serverSocket.setSoTimeout(500);
			while (System.in.available() == 0) try {
				Socket s = serverSocket.accept();
				if (s != null) {
					// Host
					s.setHandle((byte) 1, (type, data) -> {
						String	out	= null;
						byte[] value;
						do {
							value = new byte[16];
							Random r = new Random();
							r.nextBytes(value);
							try {
								out = new String(Base64.getEncoder().encode(value), "UTF-8");
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
								continue;
							}
						} while (out == null || hosts.containsKey(out)); // Maybe Error
						Map<byte[], Socket> cli = new HashMap<>();
						clis.put(s, cli);
						hosts.put(out, s);
						try {
							s.sendData((byte) 1, value);
						} catch (SocketException e) {
							e.printStackTrace();
						}
						s.setHandle((byte) 6, (typ, dat) -> {
							if (typ == 2) try {
								Iterator<Map.Entry<byte[], Socket>> it = cli.entrySet().iterator();
								while (it.hasNext()) {
									Map.Entry<byte[], Socket> en = it.next();
									if (Arrays.equals(en.getKey(), dat)) {
										en.getValue().sendClose();
										en.getValue().close();
										it.remove();
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						});
						s.setHandle((byte) 5, (typ, dat) -> {
							ByteBuffer	bb			= ByteBuffer.wrap(dat);
							byte		typee		= bb.get();
							int			addrLength	= bb.getInt();
							byte[]		addr		= new byte[addrLength];
							bb.get(addr);
							byte[] remData = new byte[dat.length - addrLength - 4];
							bb.get(remData, 1, remData.length - 1);
							remData[0] = typee;
							boolean wild = Arrays.equals(addr, BridgeUtil.WILDCARD);
							if ("true".equals(System.getProperty("debug"))) System.out.println(new String(remData));
							if ("true".equals(System.getProperty("debug"))) System.out.println(Arrays.toString(addr));
							if ("true".equals(System.getProperty("debug"))) System.out.println(
									cli.entrySet().parallelStream()
									.map(en -> new Entry<>(Arrays.toString(en.getKey()), en.getValue())).collect(Collectors.toList()));
							cli.forEach((addrL, ss) -> {
								if (wild || Arrays.equals(addr, addrL)) try {
									ss.sendData((byte) 5, remData);
								} catch (SocketException e) {
									e.printStackTrace();
								}
							});
						});
						s.setHandle((byte) 7, (typ, dat) -> {
							try {
								cli.get(dat).sendClose();
								cli.remove(dat);
							} catch (SocketException e) {
								e.printStackTrace();
							}
						});
						String	appendix	= out + "|" + s.getInetAddress().getHostAddress() + ":" + s.getPort();
						String	_out		= out;
						Updater.getInstance().add((Runnable) () -> {
							if (!s.isConnected() || s.isClosed()) try {
								for (Map.Entry<byte[], Socket> ss : cli.entrySet()) {
									ss.getValue().sendClose();
									ss.getValue().close();
								}
								clis.remove(s);
								hosts.remove(_out);
								Updater.getInstance().remove("KillHost" + appendix);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}, "KillHost" + appendix);
					});
					// Connect
					s.setHandle((byte) 2, (type, data) -> {
						try {
							ByteBuffer buff = ByteBuffer.wrap(data);
							int length = buff.getInt();
							byte[]		domain	= new byte[length];
							buff.get(domain);
							int port = buff.getInt();
							java.net.Socket conn = new java.net.Socket(new String(domain, "UTF-8"), port);
							s.setHandle((byte) 4, (typ, dat) -> {
								try {
									conn.getOutputStream().write(dat);
									conn.getOutputStream().flush();
								} catch (IOException e) {
									e.printStackTrace();
								}
							});
							String appendix = new String(domain) + ":" + port + "|" + s.getInetAddress().getHostAddress() + ":" + s.getPort();
							Updater.getInstance().add((Runnable) () -> {
								try {
									if (conn.getInputStream().available() > 0) {
										ByteArrayOutputStream baos = new ByteArrayOutputStream();
										while (conn.getInputStream().available() > 0 && baos.size() <= 1073741824) {
											byte[] b_arr = new byte[conn.getInputStream().available()];
											conn.getInputStream().read(b_arr);
											baos.write(b_arr);
										}
										s.sendData((byte) 4, baos.toByteArray());
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}, "RecvAdapterConnect" + appendix);
							Updater.getInstance().add((Runnable) () -> {
								if (!s.isConnected() || s.isClosed()) try {
									conn.close();
									Updater.getInstance().remove("RecvAdapterConnect" + appendix);
									Updater.getInstance().remove("KillConnect" + appendix);
								} catch (IOException e) {
									e.printStackTrace();
								}
								else if (!conn.isConnected() || conn.isClosed()) try {
									s.sendClose();
									s.close();
									Updater.getInstance().remove("RecvAdapterConnect" + appendix);
									Updater.getInstance().remove("KillConnect" + appendix);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}, "KillConnect" + appendix);
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
							byte[]		addrIP	= s.getInetAddress().getAddress();
							byte[]		addr	= new byte[addrIP.length + 4];
							ByteBuffer	bb		= ByteBuffer.wrap(addr).order(ByteOrder.BIG_ENDIAN);
							bb.put(addrIP);
							bb.putInt(s.getPort());
							clis.get(ss).put(addr, s);
							try {
								byte[]		dat	= new byte[addr.length + 1];
								ByteBuffer	bba	= ByteBuffer.wrap(dat).order(ByteOrder.BIG_ENDIAN);
								bba.put((byte) 1);
								bba.put(addr);
								ss.sendData((byte) 6, dat);
							} catch (SocketException e1) {
								e1.printStackTrace();
							}
							s.setHandle((byte) 5, (typ, dat) -> {
								try {
									byte[]		out	= new byte[4 + addr.length + dat.length];
									ByteBuffer	bba	= ByteBuffer.wrap(out);
									bba.order(ByteOrder.BIG_ENDIAN);
									bba.put(dat[0]);
									bba.putInt(addr.length);
									bba.put(addr);
									bba.put(dat, 1, dat.length - 1);
									ss.sendData((byte) 5, out);
								} catch (SocketException e) {
									e.printStackTrace();
								}
							});
							String appendix = new String(data) + "|" + s.getInetAddress().getHostAddress() + ":" + s.getPort();
							Updater.getInstance().add((Runnable) () -> {
								if (!s.isConnected() || s.isClosed()) try {
									byte[]		dat	= new byte[addr.length + 1];
									ByteBuffer	bba	= ByteBuffer.wrap(dat).order(ByteOrder.BIG_ENDIAN);
									bba.put((byte) 0);
									bba.put(addr);
									if (ss.isConnected() && !ss.isClosed())
										ss.sendData((byte) 6, dat);
									clis.get(ss).remove(addr);
									Updater.getInstance().remove("KillJoin" + appendix);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}, "KillJoin" + appendix);
						} else try {
							s.sendData((byte) 3, new byte[0]);
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
