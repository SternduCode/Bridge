package com.sterndu.bridge;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.Base64;

import com.sterndu.data.transfer.Connector;
import com.sterndu.data.transfer.secure.Socket;

// TODO: Auto-generated Javadoc
/**
 * The Class BridgeClient.
 */
public class BridgeClient {

	/** The sock. */
	private final Socket sock;

	/**
	 * Instantiates a new bridge client.
	 *
	 * @param hostname the hostname
	 *
	 * @throws UnknownHostException the unknown host exception
	 * @throws IOException          Signals that an I/O exception has occurred.
	 */
	public BridgeClient(String hostname) throws UnknownHostException, IOException { this(hostname, BridgeUtil.DEFAULT_PORT); }

	/**
	 * Instantiates a new bridge client.
	 *
	 * @param hostname the hostname
	 * @param port the port
	 * @throws UnknownHostException the unknown host exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public BridgeClient(String hostname, int port) throws UnknownHostException, IOException {
		sock = new Socket(hostname, port);
	}

	/**
	 * Gets the sock.
	 *
	 * @return the sock
	 */
	Socket getSock() { return sock; }

	/**
	 * Connect.
	 *
	 * @param remoteHostname the remote hostname
	 * @param remotePort     the remote port
	 *
	 * @return the connector
	 */
	public Connector connect(String remoteHostname, int remotePort) {

		try {
			byte[] str = remoteHostname.getBytes("UTF-8");
			byte[] data = new byte[str.length + 8];
			ByteBuffer bb = ByteBuffer.wrap(data);
			bb.order(ByteOrder.BIG_ENDIAN);
			bb.putInt(str.length);
			bb.put(str);
			bb.putInt(remotePort);
			sock.sendData((byte) 2, data);
			sock.setHandle((byte) 2, (typ, dat) -> {
				if (dat.length == 0) try {
					sock.sendClose();
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

			return new Connector(sock, (byte) 4);
		} catch (UnsupportedEncodingException | SocketException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Host.
	 *
	 * @return the host connector
	 */
	public HostConnector host() {
		try {
			sock.sendData((byte) 1, new byte[0]);
			HostConnector hc = new HostConnector(
					new Connector(sock, (byte) 6),
					new Connector(sock, (byte) 5),
					sock
			);
			sock.setHandle((byte) 1, (typ, dat) -> {
				try {
					hc.setCode(new String(Base64.getEncoder().encode(dat),"UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			});
			while (!hc.isCodeAvailable()) try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return hc;
		} catch (SocketException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Join.
	 *
	 * @param code the code
	 *
	 * @return the connector
	 */
	public Connector join(String code) {
		try {
			sock.sendData((byte) 3, code.getBytes("UTF-8"));
			sock.setHandle((byte) 3, (typ, dat) -> {
				if (dat.length == 0) try {
					sock.sendClose();
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			return new Connector(sock, (byte) 5);
		} catch (SocketException | UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}

	}

}
