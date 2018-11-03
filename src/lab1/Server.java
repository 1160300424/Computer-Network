package lab1;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server extends Thread {
	Socket toCustom;
	Socket toServer;

	public static int CONNECT_RETRIES = 5; // ������Ŀ���������Ӵ���
	public static int CONNECT_PAUSE = 5; // ÿ�ν������ӵļ��ʱ��
	public static int TIMEOUT = 8000; // ÿ�γ������ӵ����ʱ��
	public static int BUFSIZ = 1024; // ����������ֽ���
	public static ArrayList<String> denyHost = new ArrayList(); // ��������վ
	public static ArrayList<String> denyUser = new ArrayList<>(); // �������û�
	public static Map<String, String> phishingSite = new HashMap<>(); // ������վ
	public static List<String> requestInfo = new ArrayList<String>();
	public static List<String> cacheInfo;
	public static ArrayList<String> fish = new ArrayList<String>();
	public static HashMap<String, String> cache = new HashMap<>();
	public static ArrayList<String> CustomAsk = new ArrayList<String>();
	/*
	 * ���ںͿͻ��˽������ֽ���
	 */
	FileOutputStream clientFOS = null;
	FileOutputStream cacheFOS = null;
	InputStream CIS = null;
	OutputStream COS = null;
	ArrayList<String> askFish = new ArrayList<String>();
	String serverACK = null;
	String TOCustom = null;// ���ͻ��˵���Ϣ
	String TOServer = null;// ��������������
	InputStream toSIS = null;
	OutputStream toSOS = null;
	BufferedReader CBR = null;
	BufferedReader SBR = null;
	PrintWriter SPW = null;
	PrintWriter CPW = null;

	public Server(Socket client) {
		this.toCustom = client;

		try {
			clientFOS = new FileOutputStream("clientLog.txt", true);
			cacheFOS = new FileOutputStream("cacheLog.txt", true);
			COS = toCustom.getOutputStream();
			CPW = new PrintWriter(COS);
			CIS = toCustom.getInputStream();
			CBR = new BufferedReader(new InputStreamReader(CIS));
			start();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	@Override
	public void run() {

		try {
		
			toCustom.setSoTimeout(80000);

			String askHead = CBR.readLine();
			System.out.println("�ͻ�������ͷΪ "+askHead);
			//System.out.println(askHead);
			String serverACKCache = cache.get(askHead);
			String customHost = getLocal();
			String URL = getURL(askHead);
			if (URL.equals(""))
				return;
			String host = getHost(askHead);
			System.out.println(URL);
			int port = getPort(askHead);
			if (denyUser.contains(customHost)) {
				System.out.println("*********404*******");
			}
			if (denyHost.contains(host)) {
				System.out.println("*********404********");
			}
			if (fish.contains(host)) {
				System.out.println("�ͻ�Ҫ���ʵ���վ�ǵ�����վ");
				host = phishingSite.get(host);
				URL = "http://" + host;
				askHead = "GET" + URL + "HTTP/1.1";
				askFish = fish(host);// ���ĺ�ı���;
				CustomAsk = askFish;
				port = 80;
			}

			//System.out.println("�ͻ��������ĵ�����ͷΪ " + askHead);
			System.out.println("�ͻ��������Ŀ������Ϊ " + host);
			URL = searchURL(URL);
			int retry = 5;
			while (retry-- != 0 && !host.equals("")) {
				try {
					System.out.println("�˿ںţ�" + port + "������" + host);
					System.out.println("��" + retry + "�γ��Խ��������� " + host + "�˿ں� " + port + "������\n");
					toServer = new Socket(host, port); // ���Խ�����Ŀ������������
					System.out.println("���ӽ����ɹ�");
					break;
				} catch (Exception e) {
					System.out.println("ֹͣ���߳�");
					try {
						toCustom.close();
					} catch (IOException e1) {

						e1.printStackTrace();
					}
					Thread.interrupted();
					e.printStackTrace();
				}
				Thread.sleep(5);
			}

			if (toServer != null) {
				toServer.setSoTimeout(80000);
				toSIS = toServer.getInputStream();
				toSOS = toServer.getOutputStream();
				SBR = new BufferedReader(new InputStreamReader(toSIS));
				SPW = new PrintWriter(toSOS);
				String TOServer = null;
				String tmp = askHead;
				if (!cache.containsKey(askHead)) {
					StringBuilder sb = new StringBuilder();
					sb.append(askHead);
					sb.append("\r\n");
					while (!tmp.equals("")) {
						tmp = CBR.readLine();
						sb.append(tmp);
						sb.append("\r\n");
					}
					TOServer = sb.toString();
					System.out.println("������û�б������");
					if (!askFish.isEmpty()) {
						TOServer = toString(askFish);
					}
					System.out.print("���������������" + TOServer);
					System.out.println("����������д��ͻ�����־");
					logCustomAsk(TOServer);
					SPW.write(TOServer);
					SPW.flush();
					StringBuilder sb3 = new StringBuilder();
					int length=0;
					byte bytes[] = new byte[BUFSIZ];
					while (true) {
						length = toSIS.read(bytes);
						if (length > 0) { // ��ȡ�ͻ��˵�����ת��������
							COS.write(bytes, 0, length);
							String temp = new String(bytes);
							sb3.append(temp);
						} else if (length < 0)
							break;

					}
					
					  //tmp=SBR.readLine(); 
					 // StringBuilder sb3=new StringBuilder(); 
					  //sb3.append(tmp);
					  //sb3.append("\r\n");
					 
					  /*while (!tmp.equals("")) { tmp=SBR.readLine(); sb3.append(tmp);
					 sb3.append("\r\n"); }*/
					  serverACK= sb3.toString();
				//	System.out.print("��ȡ��������Ӧ��" + serverACK);
					System.out.println("���ñ���ת�����ͻ���");
					System.out.println("������д�뻺��");
					logCache(serverACK);
					cache.put(askHead, serverACK);
					
			
				} else {
					serverACKCache = cache.get(askHead);// �õ�����
					String modifyTime = getDateInACK(serverACKCache);// �õ���������
					System.out.println("�����������������");
					System.out.println("��Ҫ�����������ȷ��ʱ��");
					StringBuilder sb = new StringBuilder();
					sb.append(askHead);
					sb.append("\r\n");
					tmp = askHead;
					while (!tmp.equals("")) {
						tmp = CBR.readLine();
						sb.append(tmp);
						sb.append("\r\n");
					}
					TOServer = sb.toString();
					TOServer = addModifyHead(TOServer, modifyTime);
					System.out.println("��Ҫ�����������ȷ��ʱ��" + TOServer);
					SPW.write(TOServer);
					SPW.flush();
					System.out.println("���ͻ�����д����־");
					logCustomAsk(TOServer);
					StringBuilder sb3 = new StringBuilder();
					tmp = SBR.readLine();
					sb3.append(tmp);
					sb3.append("\r\n");
					while (tmp.equals("")) {
						sb3.append(tmp);
						sb3.append("\r\n");
					}
					serverACK = sb3.toString();
					System.out.print("��ȡ��������Ӧ��" + serverACK);
					if (serverACK.contains("Not Modified ")) {
						System.out.println("���������°汾");
						System.out.println("���ñ���ת�����ͻ���");
						CPW.write(serverACKCache);
						CPW.flush();
					} else {
						System.out.println("cacheû�л�������߻���Ĳ���������Ϣ");
						System.out.println("���ñ���ת�����ͻ���");
						CPW.write(serverACK);
						CPW.flush();
						
						System.out.println("���ñ������°汾д�뻺�棬�滻���ɰ汾");
						cache.remove(askHead, serverACKCache);
						cache.put(askHead, serverACK);
						logCache(serverACK);
					}
				}

			}
			System.out.println("********************һ��ͨ�������********************\r\n");
			
		} catch (IOException | InterruptedException e2) {
			e2.printStackTrace();
		}
		
		
		
	}

	public String getLocal() {

		try {
			InetAddress addr = InetAddress.getLocalHost();
			String local = addr.getHostName();
			return local;
		} catch (UnknownHostException e) {
			// TODO �Զ����ɵ� catch ��
			e.printStackTrace();
		}
		return null;

	}

	public String getURL(String head) {
		String urlStr = "";
		String[] tokens = head.split(" ");
	
		
		if (!tokens[0].equals("CONNECT"))
			for (int index = 0; index < tokens.length; index++) {
				if (tokens[index].startsWith("http://")) {
					urlStr = tokens[index];
					break;
				}
			}
		return urlStr;
	}

	public String getHost(String head) {

		String urlStr = this.getURL(head);
		if (urlStr == null)
			return null;
		URL url;
		String host = "";
		try {
			url = new URL(urlStr);
			host = url.getHost();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return host;
	}

	public int getPort(String head) {
		String urlStr = this.getURL(head);
		if (urlStr == null)
			return 80;
		URL url;
		int port = 80;
		try {
			url = new URL(urlStr);
			port = url.getPort();
			if (port == -1)
				port = 80;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return port;
	}

	public ArrayList<String> fish(String fishHost) {
		System.out.println("�ͻ�Ҫ���ʵ���վ�ǵ�����վ");

		String URL = "http://" + fishHost;
		System.out.println("��ʼ�ض��� " + URL);
		String askHead = "GET " + URL + " HTTP/1.1";

		System.out.println("�ض���������ͷΪ" + askHead);
		ArrayList<String> newHTTP = new ArrayList<>();
		newHTTP.add(askHead + "\r\n");
		newHTTP.add("Host: " + fishHost + "\r\n");
		newHTTP.add(
				"User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0" + "\r\n");
		newHTTP.add("Accept: */*");
		newHTTP.add("Accept-Language: zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2" + "\r\n");
		newHTTP.add("Accept-Encoding: gzip, deflate" + "\r\n");
		newHTTP.add("Referer: " + URL + "\r\n");
		newHTTP.add("Connection: keep-alive" + "\r\n");
		newHTTP.add(
				"Cookie: lastVisit=%BC%C6%CB%E3%BB%FA%D1%A7%D4%BA2018%C4%EA%B6%C8%CA%A1%C8%FD%BA%C3%D1%A7%C9%FA%C6%C0%D1%A1%BD%E1%B9%FB%B9%AB%CA%BE%7C%2Fnews%2F2018%2F03%2D15%2F4972236130RL0%2Ehtm%7C2018%2D3%2D15+18%3A30%3A17%2C; bdshare_firstime=1521109889736; __utma=161430584.300359969.1521109890.1521109890.1521109890.1; _ga=GA1.3.300359969.1521109890"
						+ "\r\n");
		return newHTTP;
	}

	public String searchURL(String URL) {
		int n = URL.length();
		n = URL.indexOf('?');
		String realURL = URL;
		if (n != -1) {
			realURL = URL.substring(0, n);
			System.out.println("URL�к��У������ܴӻ����в���");
		}
		return realURL;
	}

	public HashMap<String, String> searchCache(String url) {
		String ask = null;
		HashMap<String, String> cache = new HashMap<>();
		String ack = null;
		cache.put(ask, ack);
		return cache;
	}

	public String addModifyHead(String TOServer, String modifyTime) {
		String[] ans = TOServer.split("\r\n");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 2; i++) {
			sb.append(ans[i]);
			sb.append("\r\n");
		}
		String tail = "If-Modified-Since: " + modifyTime + "\r\n";
		sb.append(tail);
		for (int i = 2; i < ans.length; i++) {
			sb.append(ans[i]);
			sb.append("\r\n");
		}
		return sb.toString();
	}

	public String toString(ArrayList<String> ACK) {
		StringBuilder temp = new StringBuilder();
		for (int i = 0; i < ACK.size(); i++)
			temp.append(ACK.get(i));
		return temp.toString();
	}

	public String getDateInACK(String str) {

		String tmp = null;
		String line[] = str.split("\r\n");
		String row[] = null;
		for (int i = 0; i < line.length; i++) {
			tmp = line[i];
			if (tmp.contains("Date")) {
				row = tmp.split("Date: ");
				break;
			}
		}
		return row[1];
	}

	public void logCustomAsk(String ask) throws IOException {
		clientFOS.write(ask.getBytes());
		System.out.println("�ͻ���������д����־");

	}

	public void logCache(String ACK) throws IOException {

		cacheFOS.write(ACK.getBytes());
		System.out.println("����������Ӧ��д����־");
	}

	public void pipe(InputStream CIS, InputStream toSIS, OutputStream toSOS, OutputStream COS) {
		try {
			int length;
			byte bytes[] = new byte[4096];
			while (true) {
				try {
					if ((length = CIS.read(bytes)) > 0) { // ��ȡ�ͻ��˵�����ת��������
						toSOS.write(bytes, 0, length);
					} else if (length < 0)
						break;
				} catch (SocketTimeoutException e) {
				} catch (InterruptedIOException e) {
					System.out.println("\nRequest Exception:");
					e.printStackTrace();
				}
				try {
					if ((length = toSIS.read(bytes)) > 0) {// ���ܷ���������Ӧ�ش�������Ŀͻ���
						toSOS.write(bytes, 0, length); // ��Ϊ�ǰ��ֽڶ�ȡ�����Խ��س��ͻ��з�Ҳ���ݹ�ȥ��

					}
				} catch (SocketTimeoutException e) {
				} catch (InterruptedIOException e) {
					System.out.println("\nResponse Exception:");
					e.printStackTrace();
				}
			}
		} catch (Exception e0) {
			System.out.println("Pipe�쳣: " + e0);
		}
	}

}
