package com.sterndu.bridge;

import java.util.Objects;

public final class BridgeProtocolBundle {
	private final String domain;
	private final int port;
	private final String endPoint;
	private final String data;

	public BridgeProtocolBundle(String domain, int port, String endPoint, String data) {
		this.domain = domain;
		this.port = port;
		this.endPoint = endPoint;
		this.data = data;
	}

	public String domain() {
		return domain;
	}

	public int port() {
		return port;
	}

	public String endPoint() {
		return endPoint;
	}

	public String data() {
		return data;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		BridgeProtocolBundle that = (BridgeProtocolBundle) obj;
		return Objects.equals(this.domain, that.domain) &&
				this.port == that.port &&
				Objects.equals(this.endPoint, that.endPoint) &&
				Objects.equals(this.data, that.data);
	}

	@Override
	public int hashCode() {
		return Objects.hash(domain, port, endPoint, data);
	}

	@Override
	public String toString() {
		return "BridgeProtocolBundle[" +
				"domain=" + domain + ", " +
				"port=" + port + ", " +
				"endPoint=" + endPoint + ", " +
				"data=" + data + ']';
	}


}
