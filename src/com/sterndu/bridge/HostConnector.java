package com.sterndu.bridge;

import java.net.SocketException;

import com.sterndu.data.transfer.Connector;
import com.sterndu.data.transfer.secure.Socket;


// TODO: Auto-generated Javadoc
/**
 * The Class HostConnector.
 */
public class HostConnector {

	/** The connectors. */
	private final Connector announceConnector, normalConnector;

	/** The socket. */
	private final Socket sock;

	/** The code. */
	private String code;

	/**
	 * Instantiates a new host connector.
	 *
	 * @param announceConnector the announce connector
	 * @param normalConnector   the normal connector
	 * @param sock the sock
	 */
	public HostConnector(Connector announceConnector, Connector normalConnector, Socket sock) {
		this.announceConnector	= announceConnector;
		this.normalConnector	= normalConnector;
		this.sock 				= sock;
	}

	/**
	 * Sets the code.
	 *
	 * @param code the new code
	 */
	void setCode(String code) { this.code = code; }

	/**
	 * Close connection with.
	 *
	 * @param addr the addr
	 * @throws SocketException the socket exception
	 */
	public void closeConnectionWith(byte[] addr) throws SocketException {
		sock.sendData((byte) 7, addr);
	}

	/**
	 * Gets the announce connector.
	 *
	 * @return the announce connector
	 */
	public Connector getAnnounceConnector() { return announceConnector; }

	/**
	 * Gets the code.
	 *
	 * @return the code
	 */
	public String getCode() { return code; }

	/**
	 * Gets the normal connector.
	 *
	 * @return the normal connector
	 */
	public Connector getNormalConnector() { return normalConnector; }

	/**
	 * Checks if is code available.
	 *
	 * @return true, if is code available
	 */
	public boolean isCodeAvailable() {
		return code != null;
	}

}
