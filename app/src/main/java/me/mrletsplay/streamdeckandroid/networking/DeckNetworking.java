package me.mrletsplay.streamdeckandroid.networking;

import android.widget.Toast;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import me.mrletsplay.mrcore.misc.FriendlyException;

public class DeckNetworking {

	private static final String KEY = "C34$2bivrqcAM@otTVgoSA9mz8qLNeGRhMwesGogF7uBakTWb$7YCnZCz!9ToY6obsdiGS^fMx%x2ySuv@Lr8!2EhGwPMxRYD5gAtNYq^q5bhJjdVWaTwUyFNwF^dDUA";

	private static String host;
	private static int port = 10238;
	private static boolean closed;

	private static Socket socket;
	private static DataOutputStream dOut;
	private static DataInputStream dIn;

	public static void setHost(String host) {
		DeckNetworking.host = host;
	}

	public static String getHost() {
		return host;
	}

	public static void setPort(int port) {
		DeckNetworking.port = port;
	}

	public static int getPort() {
		return port;
	}

	public static void ensureOpen() {
		if(socket == null || socket.isClosed() || dOut == null || dIn == null) {
			try {
				socket = new Socket();
				socket.setSoTimeout(5000);
				socket.connect(new InetSocketAddress(host, port));
				dOut = new DataOutputStream(socket.getOutputStream());
				dOut.writeUTF(KEY);
				dIn = new DataInputStream(socket.getInputStream());
			} catch(SocketTimeoutException e) {
				throw new RuntimeException("Timed out");
			} catch(ConnectException e) {
				throw new RuntimeException("Failed to connect (No internet?)");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void sendCommand(String command) {
		try {
			ensureOpen();
			dOut.writeUTF(command);
		} catch (IOException e) {
			e.printStackTrace();
			socket = null;
			ensureOpen();
			sendCommand(command);
		}
	}

	public static void requestUpdate() {
		sendCommand("{\"requestData\":true}");
	}

	public static String receiveCommand() {
		try {
			if(DeckNetworking.isClosed()) Thread.sleep(1000);
			ensureOpen();
			int len = dIn.readInt();
			System.out.println(len);
			byte[] buf = new byte[len];
			dIn.readFully(buf);
			return new String(buf, StandardCharsets.UTF_8);
		}catch(SocketTimeoutException e) {
			return null;
		} catch(IOException e) {
			if(DeckNetworking.isClosed()) return null;
			e.printStackTrace();
			socket = null;
			ensureOpen();
			return receiveCommand();
		} catch (InterruptedException e) {
			throw new FriendlyException(e);
		}
	}

	public static boolean isClosed() {
		return closed;
	}

	public static void close() {
		try {
			closed = true;
			if(socket != null) socket.close();
			socket = null;
			dIn = null;
			dOut = null;
		} catch (IOException e) {
			throw new FriendlyException(e);
		}
	}

	public static void setClosed(boolean closed) {
		DeckNetworking.closed = closed;
	}
}
