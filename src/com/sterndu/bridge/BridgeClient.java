package com.sterndu.bridge;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.function.Consumer;
import com.sterndu.data.transfer.secure.Socket;

public class BridgeClient {
	private final Socket sock;

	public BridgeClient(String hostname, Consumer<byte[]> consumer) throws UnknownHostException, IOException {
		this(hostname, BridgeUtil.DEFAULT_PORT, consumer);
	}

	public BridgeClient(String hostname, int localPort) throws UnknownHostException, IOException {
		this(hostname, BridgeUtil.DEFAULT_PORT, localPort);
	}

	public BridgeClient(String hostname, int port, Consumer<byte[]> consumer) throws UnknownHostException, IOException {
		sock = new Socket(hostname, port);
		host(consumer);
	}

	public BridgeClient(String hostname, int port, int localPort) throws UnknownHostException, IOException {
		sock = new Socket(hostname, port);
		host(localPort);
	}

	public BridgeClient(String hostname, int port, String code) throws UnknownHostException, IOException {
		sock = new Socket(hostname, port);
		join(code);
	}

	public BridgeClient(String hostname, int port, String remoteHostname, int remotePort)
			throws UnknownHostException, IOException {
		sock = new Socket(hostname, port);
		connect(remoteHostname, remotePort);
	}

	public BridgeClient(String hostname, String code) throws UnknownHostException, IOException {
		this(hostname, BridgeUtil.DEFAULT_PORT, code);
	}

	public BridgeClient(String hostname, String remoteHostname, int remotePort)
			throws UnknownHostException, IOException {
		this(hostname, BridgeUtil.DEFAULT_PORT, remoteHostname, remotePort);
	}

	public void connect(String remoteHostname, int remotePort) {
		try {
			byte[] str = remoteHostname.getBytes("UTF-8");
			byte[] data = new byte[str.length + 8];
			System.arraycopy(str, 0, data, 4, str.length);
			ByteBuffer bb = ByteBuffer.wrap(data);
			bb.order(ByteOrder.BIG_ENDIAN);
			bb.putInt(0, str.length);
			bb.putInt(data.length - 5, remotePort);
			sock.sendData((byte) 2, data);
		} catch (UnsupportedEncodingException | SocketException e) {
			e.printStackTrace();
		}

	}

	public void host(Consumer<byte[]> consumer) throws SocketException {
		sock.sendData((byte) 1, new byte[0]);
	}

	public void host(int localPort) throws SocketException {

		sock.sendData((byte) 1, new byte[0]);
	}

	public void join(String code) {
		try {
			sock.sendData((byte) 3, code.getBytes("UTF-8"));
		} catch (SocketException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

}
