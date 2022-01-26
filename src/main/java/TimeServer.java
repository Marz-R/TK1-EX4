import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;

public class TimeServer {
	private static int PORT = 27780;
	private static final String NTP_SERVER = "ntp.nict.jp";
	private ServerSocket serverSocket;
	private NTPUDPClient ntpClient;
	private Random rnd;

	public TimeServer() throws InterruptedException {
		ntpClient = new NTPUDPClient();
		rnd = new Random();
		
		try {
			ntpClient.open();
			serverSocket = new ServerSocket(PORT);
			System.out.println("Server started on port: " + PORT);
			//
			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("Connection from " + socket + "!\n");
				NTPRequestHandler ntpReqHandler = new NTPRequestHandler(socket);
				ntpReqHandler.run();
			}

		} catch (IOException e) {
			e.printStackTrace();
			try {
				serverSocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

	}

	private void threadSleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		new TimeServer();
	}

	private class NTPRequestHandler implements Runnable {
		private Socket client;
		private OutputStream oStream;
		private ObjectOutputStream ooStream;
		private InputStream iStream;
		private ObjectInputStream oiStream;

		public NTPRequestHandler(Socket client) throws InterruptedException, IOException {
			this.client = client;
			iStream = client.getInputStream();
			oiStream = new ObjectInputStream(iStream);
			oStream = client.getOutputStream();
			ooStream = new ObjectOutputStream(oStream);
		}

		@Override
		public void run() {
			///
			try {
				NTPRequest ntpReq = (NTPRequest) oiStream.readObject();
				System.out.println("Received NTPRequest from: " + client);
				// System.out.println("Client Delay: " + ntpReq.d + "ms");
				// System.out.println("Client Offset: " + ntpReq.o + "ms");

				InetAddress ntpServer = InetAddress.getByName(NTP_SERVER);
				System.out.println("Connected to NTP server!\n");
				threadSleep(1100);
				threadSleep(rnd.nextInt(100)+10);
				TimeInfo info = ntpClient.getTime(ntpServer);
				info.computeDetails();
				NtpV3Packet packet = info.getMessage();

				ntpReq.setT2(packet.getReceiveTimeStamp().getTime());
				ntpReq.setT3(packet.getTransmitTimeStamp().getTime());

				sendNTPAnswer(ntpReq);
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}

		private void sendNTPAnswer(NTPRequest request) throws IOException {
			///
			System.out.println("Sending messages to the ClientSocket");
    	ooStream.writeObject(request);
			System.out.println("Sent messages to the ClientSocket");
		}

	}

}
