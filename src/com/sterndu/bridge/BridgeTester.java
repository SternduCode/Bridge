package com.sterndu.bridge;

// TODO: Auto-generated Javadoc
/**
 * The Class BridgeTester.
 */
public class BridgeTester {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		String join = "bridge://domain/join/code";
		String connect = "bridge://domain:64/connect/domain:port";
		String host = "bridge://domain:2344/host/64";
		System.out.println(BridgeUtil.split(join));
		System.out.println(BridgeUtil.split(connect));
		System.out.println(BridgeUtil.split(host));
		System.setProperty("debug", "true");
		//		new Thread(() -> {
		//			try {
		//				BridgeClient	bc		= new BridgeClient("localhost");
		//				HostConnector	conn	= bc.host();
		//				conn.getNormalConnector().setHandle((typ, dat) -> {
		//					ByteBuffer	bb		= ByteBuffer.wrap(dat);
		//					int			len		= bb.getInt();
		//					byte[]		addr	= new byte[len];
		//					bb.get(addr);
		//					byte[] remData = new byte[dat.length - len - 4];
		//					bb.get(remData);
		//					System.out.println(typ + " " + new String(remData));
		//				});
		//				System.out.println(conn.getCode());
		//			} catch (IOException e) {
		//				e.printStackTrace();
		//			}
		//		}, "Host").start();
		//		new Thread(() -> {
		//			try {
		//				BridgeClient bc = new BridgeClient("localhost");
		//				while (System.in.available() == 0) try {
		//					Thread.sleep(5);
		//				} catch (InterruptedException e) {
		//					e.printStackTrace();
		//				}
		//				byte[] str = new byte[System.in.available() - 2];
		//				System.in.read(str);
		//				System.in.read();
		//				System.in.read();
		//				String		code	= new String(str);
		//				Connector	conn	= bc.join(code);
		//				conn.sendData("FFS".getBytes("UTF-8"));
		//			} catch (IOException e) {
		//				e.printStackTrace();
		//			}
		//		}, "Join").start();
		new BridgeServer().start();
		System.exit(0);
	}

}
