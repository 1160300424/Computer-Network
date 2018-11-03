package lab1;

import java.io.IOException;
import java.net.*;

public class Proxy {
	
	private ServerSocket Proxy;

	public void initServer(int port) throws IOException {
		Proxy = new ServerSocket(port);
	}

	public void start() throws IOException {
		initServer(10240);
		int count=1;
		//create thread
		while (true) {
			System.out.println("************启动子线程 "+count++);
			Server server=new Server(Proxy.accept());
			server.run();
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("启动主线程 ");
		Proxy proxy = new Proxy();	
		proxy.start();
	}
}
