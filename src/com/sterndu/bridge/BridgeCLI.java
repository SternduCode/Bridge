package com.sterndu.bridge;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.sterndu.data.transfer.Connector;
import com.sterndu.multicore.Updater;
import com.sterndu.util.Entry;


/**
 * The Class BridgeCLI.
 */
public class BridgeCLI {

	/**
	 * The Interface ArgCollector.
	 */
	private static abstract class ArgCollector {

		/** The servers, code and target. */
		protected String code = "", server = "", target = "", announce = "";

		/** The ports. */
		protected int port = BridgeUtil.DEFAULT_PORT, localPort = 0, targetPort = 0;

		/** The ui. */
		protected boolean ui = false;

		/**
		 * Run.
		 */
		public abstract void run();

		/**
		 * Sets the announce.
		 *
		 * @param announce the new announce
		 */
		public void setAnnounce(String announce) {
			this.announce = announce;
		}

		/**
		 * Sets the code.
		 *
		 * @param code the new code
		 */
		public void setCode(String code) { this.code = code; }

		/**
		 * Sets the local port.
		 *
		 * @param port the new local port
		 */
		public void setLocalPort(String port) {
			if (!"".equals(port)) try {
				int i_port = Integer.parseInt(port);
				if (i_port < 0 || i_port > 65535) throw new NumberFormatException();
				localPort = i_port;
				return;
			} catch (NumberFormatException e) {
				System.err.println(port + " is not valid Port number");
				return;
			}
			help();

		}

		/**
		 * Setport.
		 *
		 * @param port the new port
		 */
		public void setPort(String port) {
			if (!"".equals(port)) try {
				int i_port = Integer.parseInt(port);
				if (i_port < 0 || i_port > 65535) throw new NumberFormatException();
				this.port = i_port;
				return;
			} catch (NumberFormatException e) {
				System.err.println(port + " is not valid Port number");
				return;
			}
			help();

		}

		/**
		 * Sets the server.
		 *
		 * @param server the new server
		 */
		public void setServer(String server) {
			alive: if (!"".equals(server)) if (server.contains("[")) { // IPv6 address
				String[] sp = server.split("]");

				if (sp.length < 2) break alive;

				this.server = sp[0] + "]";
				String s_port = sp[1].substring(1);
				setPort(s_port);
				return;
			} else if (server.contains(":")) { // Has port
				String s_port = server.split(":")[1];
				this.server = server.split(":")[0];
				setPort(s_port);
				return;
			}
			this.server = server; // doesn't contain port
		}

		/**
		 * Sets the target.
		 *
		 * @param target the new target
		 */
		public void setTarget(String target) {
			alive: if (!"".equals(target)) if (target.contains("[")) { // IPv6 address

				String[] sp = target.split("]");

				if (sp.length < 2) break alive;

				this.target = sp[0] + "]";
				String s_port = sp[1].substring(1);
				if (!"".equals(s_port)) try {
					int i_port = Integer.parseInt(s_port);
					if (i_port < 0 || i_port > 65535) throw new NumberFormatException();
					targetPort = i_port;
					return;
				} catch (NumberFormatException e) {
					System.err.println(s_port + " is not valid Port number");
					return;
				}
			} else if (target.contains(":")) { // Has port

				String s_port = target.split(":")[1];
				this.target = target.split(":")[0];
				if (!"".equals(s_port)) try {
					int i_port = Integer.parseInt(s_port);
					if (i_port < 0 || i_port > 65535) throw new NumberFormatException();
					targetPort = i_port;
					return;
				} catch (NumberFormatException e) {
					System.err.println(s_port + " is not valid Port number");
					return;
				}
			}
			this.target = target;

		}

		/**
		 * Sets the ui.
		 *
		 * @param ui the new ui
		 */
		public void setUI(boolean ui) { this.ui = ui; }

	}

	/**
	 * Connect. TODO
	 *
	 * @param server     the server
	 * @param port       the port
	 * @param remote     the remote
	 * @param remotePort the remote port
	 * @param localPort  the local port
	 * @param ui         the ui
	 */
	public static void connect(String server, int port, String remote, int remotePort, int localPort, boolean ui) {
		new Thread(() -> {
			try {
				List<String>			uiLi		= BridgeUI.INSTANCE.getLog("Connect " + localPort + " | "
						+ server + ":" + port + " | " + remote + ":" + remotePort);
				java.net.ServerSocket locSocket = new java.net.ServerSocket(localPort);
				if (BridgeUI.isUIEnabled() && ui)
					uiLi.add("Running on port: " + locSocket.getLocalPort());
				else
					System.out.println("Running on port: " + locSocket.getLocalPort());
				while (System.in.available()==0) {
					java.net.Socket s = locSocket.accept();
					new Thread(() -> {
						try {
							String			appendix	= "Connect" + localPort + "|"
									+ server + ":" + port + "|"
									+ s.getInetAddress().getHostAddress() + ":" + s.getPort();
							BridgeClient	bc			= new BridgeClient(server, port);
							Connector		hc			= bc.connect(remote, remotePort);
							hc.disableHandle();
							hc.getSock().setHandle((byte) hc.getType(), (type, data) -> {
								try {
									if ("true".equals(System.getProperty("debug"))) System.out.println(new String(data));
									s.getOutputStream().write(data);
									s.getOutputStream().flush();
								} catch (IOException e) {
									try {
										Updater.getInstance().remove("RecvAdapterConnect" + appendix);
										Updater.getInstance().remove("KillConnect" + appendix);
										try {
											bc.getSock().sendClose();
										} catch (final Exception ignored) {
										}
										bc.getSock().close();
										s.close();
									} catch (IOException ex) {
										ex.initCause(e);
										ex.printStackTrace();
									}
								}
							});
							Updater.getInstance().add((Runnable) () -> {
								try {
									if (s.getInputStream().available() > 0) {
										ByteArrayOutputStream baos = new ByteArrayOutputStream();
										while (s.getInputStream().available() > 0 && baos.size() <= 1073741824) {
											byte[] b_arr = new byte[s.getInputStream().available()];
											s.getInputStream().read(b_arr);
											baos.write(b_arr);
										}
										hc.getSock().sendData(hc.getType(), baos.toByteArray());
									}
								} catch (IOException e) {
									try {
										Updater.getInstance().remove("RecvAdapterConnect" + appendix);
										Updater.getInstance().remove("KillConnect" + appendix);
										try {
											bc.getSock().sendClose();
										} catch (final Exception ignored) {
										}
										bc.getSock().close();
										s.close();
									} catch (IOException ex) {
										ex.initCause(e);
										ex.printStackTrace();
									}
								}
							}, "RecvAdapterConnect" + appendix);
							Updater.getInstance().add((Runnable) () -> {
								if (!bc.getSock().isConnected() || bc.getSock().isClosed()) {
									new Thread(() -> {
										try {
											Thread.sleep(2000);
											s.close();
										} catch (InterruptedException | IOException e) {
											e.printStackTrace();
										}

									}).start();

									Updater.getInstance().remove("RecvAdapterConnect" + appendix);
									Updater.getInstance().remove("KillConnect" + appendix);
								} else if (!s.isConnected() || s.isClosed()) try {
									bc.getSock().sendClose();
									bc.getSock().close();
									Updater.getInstance().remove("RecvAdapterConnect" + appendix);
									Updater.getInstance().remove("KillConnect" + appendix);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}, "KillConnect" + appendix);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}).start();
				}
				locSocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}).start();
	}

	/**
	 * Help.
	 */
	public static void help() {
		System.out.println(
				"host -s/--server sterndu.com:55601 (-p/--port 55601) (-l/--local-port 25566) connect -s/--server sterndu.com:55601 (-p/--port 55601) -t/--target lin.sterndu.com:453 join -s/--server sterndu.com:55601 (-p/--port 55601) -c/--code hvcxdrt675 server (-p/--port 55601)");
		System.out.println("[[MODE] [OPTION]... ]...");
		System.out.println();
		System.out.println("modes:");
		System.out.println("  connect");
		System.out.println("  join");
		System.out.println("  host");
		System.out.println("  server");
		System.out.println();
		System.out.println(
				"inside the () there are all modes with which the option can bew used, if the mode is followed with an ! it is mandatory in that mode");
		System.out.println();
		System.out.println("options:");
		System.out.println("  -s, --server ADDRESS     the bridge server it should connect to, can contain the port (connect!, join!, host!)");
		System.out.println("  -p, --port NUMBER        the port on which the server is running/should run (connect, join, host, server)");
		System.out.println("  -t, --target ADDRESS     the target server you want to connect to (connect!)");
		System.out.println("  -c, --code CODE          the code that is displayed on the 'host' you want to connect to (join!)");
		System.out.println(
				"  -l, --local-port NUMBER  the port that should be used locally, in host mode it is the port that should be hosted, in the other modes its the port you connect to (connect, join, host!)");
		System.out.println("  -a, --announce ADDRESS   the address where the code, with which you can join the connection, will be sent to (host)");
		System.out.println("  -u, --ui				   show output in the ui (connect, join, host, server)");
		System.out.println();
		System.out.println("You can add the option -v / --verbose  anywhere to enable debug output in all modes");
		System.out.println();
		System.exit(0);
	}

	/**
	 * Host. TODO
	 *
	 * @param server    the server
	 * @param port      the port
	 * @param localPort the local port
	 * @param announce  the announce
	 * @param ui        the ui
	 *
	 */
	public static void host(String server, int port, int localPort, String announce, boolean ui) {
		new Thread(() -> {
			try {
				List<String> uiLi = null;
				if (BridgeUI.isUIEnabled() && ui) uiLi = BridgeUI.INSTANCE.getLog("Host " + localPort + " | "
						+ server + ":" + port + " | " + announce);
				BridgeClient	bc		= new BridgeClient(server, port);
				HostConnector	hc		= bc.host();
				if (uiLi != null) {
					uiLi.add("Hosting port: " + localPort + " via: " + hc.getCode());
				}
				else
					System.out.println("Hosting port: " + localPort + " via: " + hc.getCode());
				if (announce != null && announce.length() > 0) {
					String[] sp = announce.split(":");
					String announcePort = sp[sp.length - 1];
					String announceLocal = announce.substring(0, announce.length() - announcePort.length() - 1);
					com.sterndu.data.transfer.secure.Socket announceSocket = new com.sterndu.data.transfer.secure.Socket(announceLocal,
							Integer.parseInt(announcePort));
					announceSocket.sendData((byte) 0xa0, hc.getCode().getBytes(StandardCharsets.UTF_8));
					while (!announceSocket.getInitialized()) try {
						Thread.sleep(5L);
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(20L);
					} catch (Exception e) {
						e.printStackTrace();
					}
					announceSocket.sendClose();
					announceSocket.close();
				}
				Map<byte[], Socket> connections = new HashMap<>();
				hc.getAnnounceConnector().setHandle((type, addr) -> {
					String appendix = "Host" + localPort + "|" + server + ":" + port + "|" + new String(addr);
					if (type == 1) try {
						Socket lSock;
						connections.put(addr, lSock = new Socket("localhost", localPort));
						if ("true".equals(System.getProperty("debug"))) System.out.println(Arrays.toString(addr));
						Updater.getInstance().add((Runnable) () -> {
							try {
								if (lSock.getInputStream().available() > 0) {
									ByteArrayOutputStream baos = new ByteArrayOutputStream();
									baos.write(0);
									baos.write(0);
									baos.write(0);
									baos.write(addr.length);
									baos.write(addr);
									while (lSock.getInputStream().available() > 0 && baos.size() <= 1073741824) {
										byte[] b_arr = new byte[lSock.getInputStream().available()];
										lSock.getInputStream().read(b_arr);
										baos.write(b_arr);
									}
									if ("true".equals(System.getProperty("debug"))) System.out.println(baos);
									hc.getNormalConnector().sendData(baos.toByteArray());
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}, "RecvAdapterConnect" + appendix);
						Updater.getInstance().add((Runnable) () -> {
							if (!hc.getNormalConnector().getSock().isConnected() || hc.getNormalConnector().getSock().isClosed()) try {
								lSock.close();
								Updater.getInstance().remove("RecvAdapterConnect" + appendix);
								Updater.getInstance().remove("KillConnect" + appendix);
							} catch (IOException e) {
								e.printStackTrace();
							}
							else if (!lSock.isConnected() || lSock.isClosed()) try {
								hc.getAnnounceConnector().sendData((byte) 2, addr);
								Updater.getInstance().remove("RecvAdapterConnect" + appendix);
								Updater.getInstance().remove("KillConnect" + appendix);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}, "KillConnect" + appendix);
					} catch (Exception e) {
						e.printStackTrace();
					}
					else try {
						connections.get(addr).close();
						connections.remove(addr);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});

				hc.getNormalConnector().setHandle((type, data) -> {
					try {

						// bb.putInt(address.length + 4);
						// bb.put(address);
						// bb.putInt(s.getPort());
						// bb.put(dat, 1, dat.length - 1);

						ByteBuffer	bb		= ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
						int			length	= bb.getInt();
						byte[]		addr	= new byte[length];
						bb.get(addr);
						if ("true".equals(System.getProperty("debug"))) System.out.println(Arrays.toString(addr));
						byte[] dat = new byte[data.length - bb.position()];
						bb.get(dat);
						if ("true".equals(System.getProperty("debug"))) System.out.println(new String(dat));
						if ("true".equals(System.getProperty("debug"))) System.out.println(
								connections.entrySet().parallelStream().map(en -> new Entry<>(Arrays.toString(en.getKey()), en.getValue())).collect(Collectors.toList()));
						if ("true".equals(System.getProperty("debug"))) System.out.println(connections.get(addr));
						if ("true".equals(System.getProperty("debug"))) System.out.println(Arrays.toString(addr));
						for (Map.Entry<byte[], Socket> en : connections.entrySet()) if (Arrays.equals(en.getKey(), addr)) {
							OutputStream outputStream = en.getValue().getOutputStream();
							outputStream.write(dat);
							outputStream.flush();
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}).start();
	}

	/**
	 * Join. TODO
	 *
	 * @param server    the server
	 * @param port      the port
	 * @param code      the code
	 * @param localPort the local port
	 * @param ui        the ui
	 */
	public static void join(String server, int port, String code, int localPort, boolean ui) {
		new Thread(() -> {
			try {
				List<String>	uiLi;
				if (ui && BridgeUI.isUIEnabled())
					uiLi = BridgeUI.INSTANCE.getLog("Join " + localPort + " | "
						+ server + ":" + port + " | " + code);
				ServerSocket ss = new ServerSocket(localPort);
				System.out.println("Running on port: " + ss.getLocalPort());
				while (System.in.available() == 0) {
					java.net.Socket s = ss.accept();
					new Thread(() -> {
						try {
							String			appendix	= "Connect" + localPort + "|"
									+ server + ":" + port + "|"
									+ s.getInetAddress().getHostAddress() + ":" + s.getPort();
							BridgeClient	bc			= new BridgeClient(server, port);
							Connector		hc			= bc.join(code);
							hc.setHandle((type, data) -> {
								try {
									if ("true".equals(System.getProperty("debug"))) System.out.println(new String(data));
									s.getOutputStream().write(data);
									s.getOutputStream().flush();
								} catch (IOException e) {
									try {
										Updater.getInstance().remove("RecvAdapterConnect" + appendix);
										Updater.getInstance().remove("KillConnect" + appendix);
										try {
											bc.getSock().sendClose();
										} catch (final Exception ignored) {
										}
										bc.getSock().close();
										s.close();
									} catch (IOException ex) {
										ex.initCause(e);
										ex.printStackTrace();
									}
								}
							});
							Updater.getInstance().add((Runnable) () -> {
								try {
									if (s.getInputStream().available() > 0) {
										ByteArrayOutputStream baos = new ByteArrayOutputStream();
										while (s.getInputStream().available() > 0 && baos.size() <= 1073741824) {
											byte[] b_arr = new byte[s.getInputStream().available()];
											s.getInputStream().read(b_arr);
											baos.write(b_arr);
										}
										hc.sendData((byte) hc.getType(), baos.toByteArray());
									}
								} catch (IOException e) {
									try {
										Updater.getInstance().remove("RecvAdapterConnect" + appendix);
										Updater.getInstance().remove("KillConnect" + appendix);
										try {
											bc.getSock().sendClose();
										} catch (final Exception ignored) {
										}
										bc.getSock().close();
										s.close();
									} catch (IOException ex) {
										ex.initCause(e);
										ex.printStackTrace();
									}
								}
							}, "RecvAdapterConnect" + appendix);
							Updater.getInstance().add((Runnable) () -> {
								if (!bc.getSock().isConnected() || bc.getSock().isClosed()) {
									new Thread(() -> {
										try {
											Thread.sleep(2000);
											s.close();
										} catch (InterruptedException | IOException e) {
											e.printStackTrace();
										}

									}).start();

									Updater.getInstance().remove("RecvAdapterConnect" + appendix);
									Updater.getInstance().remove("KillConnect" + appendix);
								} else if (!s.isConnected() || s.isClosed()) try {
									bc.getSock().sendClose();
									bc.getSock().close();
									Updater.getInstance().remove("RecvAdapterConnect" + appendix);
									Updater.getInstance().remove("KillConnect" + appendix);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}, "KillConnect" + appendix);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}).start();
				}
				ss.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		// System.setProperty("debug", "true");

		if (args.length == 0) help();

		ArgCollector		argCollector	= null;
		Iterator<String>	it				= Arrays.asList(args).iterator();

		while (it.hasNext()) {
			String arg = it.next();
			System.out.println(arg);
			switch (arg) {
				case "host": {
					if (argCollector != null) argCollector.run();
					argCollector = new ArgCollector() {

						@Override
						public void run() {
							if ("".equals(server)) help();
							host(server, port, localPort, announce, ui);

						}

					};
					break;
				}
				case "join": {
					if (argCollector != null) argCollector.run();
					argCollector = new ArgCollector() {

						@Override
						public void run() {
							if ("".equals(server)) help();
							if ("".equals(code)) help();
							join(server, port, code, localPort, ui);

						}

					};
					break;
				}
				case "connect": {
					if (argCollector != null) argCollector.run();
					argCollector = new ArgCollector() {

						@Override
						public void run() {
							if ("".equals(server)) help();
							if ("".equals(target)) help();
							if (targetPort == 0) help();
							connect(server, port, target, targetPort, localPort, ui);

						}

					};
					break;
				}
				case "server": {
					if (argCollector != null) argCollector.run();
					argCollector = new ArgCollector() {

						@Override
						public void run() { new Thread(() -> {
							new BridgeServer(port, ui).start();
						}).start(); }

					};
					break;
				}
				case "-s":
				case "--server": {
					if (argCollector == null) help();
					else argCollector.setServer(it.next());
					break;
				}
				case "-p":
				case "--port": {
					if (argCollector == null) help();
					else argCollector.setPort(it.next());
					break;
				}
				case "-t":
				case "--target": {
					if (argCollector == null) help();
					else argCollector.setTarget(it.next());
					break;
				}
				case "-c":
				case "--code": {
					if (argCollector == null) help();
					else argCollector.setCode(it.next());
					break;
				}
				case "-l":
				case "--local-port": {
					if (argCollector == null) help();
					else argCollector.setLocalPort(it.next());
					break;
				}
				case "-a":
				case "--announce": {
					if (argCollector == null) help();
					else argCollector.setAnnounce(it.next());
					break;
				}
				case "-u":
				case "--ui": {
					if (argCollector == null) help();
					else argCollector.setUI(true);
					break;
				}
				case "-v":
				case "--verbose": {
					System.setProperty("debug", "true");
					break;
				}
				default: help(); break;
			}

		}
		if (argCollector != null) argCollector.run();
	}

}