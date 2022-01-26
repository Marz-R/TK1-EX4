import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;

public class TimeClient {
	private static String hostUrl = "127.0.0.1";
	private static final String NTP_SERVER = "ntp.nict.jp";
	private static int PORT = 27780;
	private Double minD;
	private Double diff;  // time difference between client and server
	private NTPRequest minNTPrequest;
	private List<NTPRequest> ntpReqs = new ArrayList<NTPRequest>();
	private Socket socket;
	private OutputStream oStream;
	private ObjectOutputStream ooStream;
	private InputStream iStream;
	private ObjectInputStream oiStream;
	

	public TimeClient() throws ClassNotFoundException {
		// SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
    NTPUDPClient ntpClient = new NTPUDPClient();
		Random rnd = new Random();
		
		try {
			ntpClient.open();

			for (int i = 0; i < 10; i++) {
				System.out.println("Loop " + (i+1));

				socket = new Socket(InetAddress.getByName(hostUrl), PORT);
				System.out.println("Connected to ServerSocket!");
				oStream = socket.getOutputStream();
				ooStream = new ObjectOutputStream(oStream);
				iStream = socket.getInputStream();
				oiStream = new ObjectInputStream(iStream);

				InetAddress ntpServer = InetAddress.getByName(NTP_SERVER);
				System.out.println("Connected to NTP server!\n");
				threadSleep(rnd.nextInt(100)+10);
				TimeInfo info = ntpClient.getTime(ntpServer);
				minNTPrequest = new NTPRequest();

				info.computeDetails();
				// Date exactTime = new Date(System.currentTimeMillis() + info.getOffset());
				// System.out.println("Correct time:\n" + formatter.format(exactTime) + "\n");

				NtpV3Packet packet = info.getMessage();

				// System.out.println("[t1] (Originate TimeStamp):\n"
				// 				+ formatter.format(packet.getOriginateTimeStamp().getDate()) + "\n");
				minNTPrequest.setT1(packet.getOriginateTimeStamp().getTime());

				// System.out.println("[t2] (Receive TimeStamp):\n"
				// 				+ formatter.format(packet.getReceiveTimeStamp().getDate()) + "\n");
				//minNTPrequest.setT2(packet.getReceiveTimeStamp().getTime());

				// System.out.println("[t3] (Transmit TimeStamp):\n"
				// 				+ formatter.format(packet.getTransmitTimeStamp().getDate()) + "\n");
				//minNTPrequest.setT3(packet.getTransmitTimeStamp().getTime());

				// System.out.println("[t4] (Return TimeStamp):\n"
				// 				+ formatter.format(new Date(info.getReturnTime())) + "\n");
				minNTPrequest.setT4(info.getReturnTime());

				// System.out.println("Roundtrip Time:\n" + info.getDelay() + "ms\n");
				// System.out.println("Offset:\n" + info.getOffset() + "ms\n");

				sendNTPRequest(minNTPrequest);
				minNTPrequest = (NTPRequest) oiStream.readObject();

				minNTPrequest.calculateOandD();
				System.out.println("Delay:\n" + minNTPrequest.d + "ms\n");
				System.out.println("Offset:\n" + minNTPrequest.o + "ms\n");

				ntpReqs.add(minNTPrequest);

				// sendNTPRequest(minNTPrequest);

				//socket.close();
				
				threadSleep(350);
			}
			
			socket.close();
			ntpClient.close();

			minD = Double.MAX_VALUE;
			for (NTPRequest req : ntpReqs) {
				double tmp = minD;
				minD = Math.min(req.d, minD);
				if (tmp != minD) diff = req.o;  // Choose the value of 𝑜𝑖 that corresponds to the minimum 𝑑
			}

			System.out.println("Time difference between client and server is:\n" + diff + "ms\n");
			System.out.println("Accuracy of the approximation is calculated to be:\n±" + minD/2 + "ms\n");

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	private void sendNTPRequest(NTPRequest request) throws IOException {
		//
		System.out.println("Sending messages to the ServerSocket");
    ooStream.writeObject(request);
		System.out.println("Sent messages to the ServerSocket\n");
	}

	private void threadSleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws ClassNotFoundException {
		new TimeClient();
	}

}
