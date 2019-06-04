package application;
	
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {
	
	public static ExecutorService threadPool;
	public static Vector<Client> clients = new Vector<Client>();
	
	ServerSocket serverSocket;
	
	//サーバーを稼働させクライアントの接続を待つメソッド
	public void startServer(String IP, int port) {
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(IP, port));
		} catch (Exception e) {
			e.printStackTrace();
			if(!serverSocket.isClosed()) {
				stopServer();
			}
			return;
		}
		
		//クライアントが接続するまで待つスレッド
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						Socket socket = serverSocket.accept();
						clients.add(new Client(socket));
						System.out.println("[クライアント接続]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
					} catch (Exception e) {
						if(!serverSocket.isClosed()) {
							stopServer();
						}
						break;
					}
				}
			}
		};
		threadPool = Executors.newCachedThreadPool();
		threadPool.submit(thread);
	}
	
	//サーバーを中止させるメソッド
	public void stopServer() {
		try {
			//現在策動中の全てのソケットを終了
			Iterator<Client> iterator = clients.iterator();
			while(iterator.hasNext()) {
				Client client = iterator.next();
				client.socket.close();
				iterator.remove();
			}
			//サーバーソケットオブジェクト終了
			if(serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
			//スレッド終了
			if(threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	//UIを作り出しでプログラムを動かすメソッド
	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		TextArea textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setFont(new Font(15));
		root.setCenter(textArea);
		
		Button toggleButton = new Button("始まる");
		toggleButton.setMaxWidth(Double.MAX_VALUE);
		BorderPane.setMargin(toggleButton, new Insets(1, 0, 0, 0));
		root.setBottom(toggleButton);
		
		String IP = "127.0.0.1";
		int port = 9876;
		
		toggleButton.setOnAction(event -> {
			if(toggleButton.getText().equals("始まる")) {
				startServer(IP, port);
				Platform.runLater(() -> {
					String message = String.format("[サーバー接続]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("終了する");
				});
			} else {
				stopServer();
				Platform.runLater(() -> {
					String message = String.format("[サーバース終了]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("始まる");
				});
			}
		});
		
		
		Scene scene = new Scene(root, 400, 400);
		primaryStage.setTitle("チャットサーバー");
		primaryStage.setOnCloseRequest(event -> stopServer());
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	//プログラムの侵入
	public static void main(String[] args) {
		launch(args);
	}
}
