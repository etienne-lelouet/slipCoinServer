package network.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import network.buffers.NetBuffer;
import network.buffers.NetBufferData;

public class TCPClientThread implements Runnable {

	private static final int MAX_MESSAGE_SIZE = 1_000_000; // 1mo en taille de message maximale (au-delà, je considère qu'il y a erreur)
	private static final int MAX_RCV_BUFFER_SIZE = 10_000_000;
	private static final int MAX_MESSAGE_LIST_TOTAL_SIZE = 10_000_000;
	private TCPClient myClient;
	private Socket mySocket;
	private final int connectToPort;
	private final String connectToHost;
	private byte[] dataNotYetInBuffer = new byte[0];
	//private int dataNotYetInBuffer_currentLength = 0;
	private AtomicBoolean stillActive = new AtomicBoolean(true);
	private AtomicBoolean criticalErrorOccured = new AtomicBoolean(false);
	private AtomicBoolean isConnected = new AtomicBoolean(false);
	private String criticalErrorMessage = "";
	private final boolean hasWorkingSocketOnCreate;
	
	private ArrayList<NetBuffer> sendMesageList = new ArrayList<NetBuffer>(); // liste des messages à envoyer
	
	private Object sendMessage_lock = new Object();
	private Object dataNotYetInBuffer_lock = new Object(); // protection de dataNotYetInBuffer
	//private Object bufferAccessLock = new Object(); // protection de la liste des mesages reçus (NetBuffer)
	
	/** Constructeur utilisé dans une application cliente, pour se connecter à un serveur de manière non bloquante.
	 * Durant l'attente de connexion, isConnected est à false, idem pour la fonction isConnectedToHost()
	 * @param arg_myClient
	 * @param host
	 * @param port
	 */
	public TCPClientThread(TCPClient arg_myClient, String host, int port) {
		myClient = arg_myClient;
		connectToHost = host;
		connectToPort = port;
		hasWorkingSocketOnCreate = false;
		// isConnected.set(false); valeur par défaut, le client n'est pas encore connecté
	}
	/** Constructeur majoritairement utilisé par un application serveur, faisant suite à un ServerSocket.accept()
	 * @param arg_myClient 
	 * @param arg_workingSocket
	 */
	public TCPClientThread(TCPClient arg_myClient, Socket arg_workingSocket) {
		myClient = arg_myClient;
		mySocket = arg_workingSocket;
		connectToHost = "";
		connectToPort = 0;
		hasWorkingSocketOnCreate = true;
		isConnected.set(true); // je pars du principe que le socket est bien connecté
	}
	
	private boolean checkForCompleteMessage() {
		// regarde si un nouveau message est reçu
		// un message commence toujours par 4 octets indiquant sa longueur
		synchronized (dataNotYetInBuffer_lock) { // modification des données de dataNotYetInBuffer
			if ( dataNotYetInBuffer.length < 4 )
				return false; // il faut au moins 4 octets pour indiquer la taille du message à recevoir
			
			byte[] messageSizeByteArray = new byte[4];
			System.arraycopy(dataNotYetInBuffer, 0, messageSizeByteArray, 0, 4);
			int messageSize = NetBufferData.byteArrayToInt(messageSizeByteArray);
			if (messageSize >= MAX_MESSAGE_SIZE) { // erreur : message beaucoup trop gros
				stillActive.set(false);
				criticalErrorMessage = "Message de taille trop grande détécté : messageSize(" + messageSize + ") > MAX_MESSAGE_SIZE(" + MAX_MESSAGE_SIZE + ")";
				criticalErrorOccured.set(true);
				dataNotYetInBuffer = null; // superflu, mais pour bien comprendre que je supprime l'ancien buffer
				dataNotYetInBuffer = new byte[0];
				return false;
			}
			
			if (dataNotYetInBuffer.length < messageSize + 4)
				return false; // message non entièrement reçu
			// Je reçois le message
			NetBuffer messageBuffer = new NetBuffer(dataNotYetInBuffer, 4, messageSize);
			// Je supprime du tableau dataNotYetInBuffer le message que je viens de recevoir
			int newGlobalBufferSize = dataNotYetInBuffer.length - 4 - messageSize;
			byte[] newDataNotYetInBuffer = new byte[newGlobalBufferSize];
			System.arraycopy(dataNotYetInBuffer, messageSize + 4, newDataNotYetInBuffer, 0, newGlobalBufferSize);
			dataNotYetInBuffer = newDataNotYetInBuffer;
			//System.out.println("TCPClientThread.checkForCompleteMessage() : messageSize = " + messageSize);
			//System.out.println("TCPClientThread.checkForCompleteMessage() : firstInt = " + messageBuffer.readInteger());
			//messageBuffer.resetReadPosition();
			
			myClient.addReadyBufferFromThread(messageBuffer);
			return true;
		}
	}
	
	private synchronized void tryCloseSocket() {
		isConnected.set(false);
		stillActive.set(false);
		try {
			if (mySocket == null) return;
			if (mySocket.isClosed()) { mySocket = null; return; }
			mySocket.close();
			mySocket = null;
		} catch (Exception e) {
			
		}
	}
	
	@Override
	public void run() {
		// réception des octets du buffer, assemblage des buffers reçus
		
		if ( ! hasWorkingSocketOnCreate) {
			InetAddress hostAddress = null;
			try {
				hostAddress = InetAddress.getByName(connectToHost);
			} catch (UnknownHostException hostExcept) {
				stillActive.set(false);
				criticalErrorMessage = "Impossible de résoudre le nom de domaine. connectToHost(" + connectToHost + ")" + hostExcept.getMessage();
				criticalErrorOccured.set(true);
				tryCloseSocket();
				return;
			}
			
			try {
				mySocket = new Socket(hostAddress, connectToPort);
			} catch (IOException sockIOException) {
				stillActive.set(false);
				criticalErrorMessage = "Impossible d'ouvrir le socket à l'adresse connectToHost(" + connectToHost + ") " + "connectToPort(" + connectToPort + ")" + sockIOException.getMessage();
				criticalErrorOccured.set(true);
				tryCloseSocket();
				return;
			}
		} // else : mySocket déjà défini dans le constructeur
		
		if (mySocket == null) {
			stillActive.set(false);
			criticalErrorMessage = "Socket == null dès le début de TCPClientThread.run() alors qu'il ne devrait pas l'être.";
			criticalErrorOccured.set(true);
			tryCloseSocket();
			return;
		}
		
		
		if (mySocket.isConnected() == false) {
			stillActive.set(false);
			criticalErrorMessage = "Socket non connecté (mySocket.isConnected() == false) connectToHost(" + connectToHost + ") " + "connectToPort(" + connectToPort + ")";
			criticalErrorOccured.set(true);
			tryCloseSocket();
			return;
		}
		isConnected.set(true); // bien connecté
		//System.out
		
		
		while (stillActive.get()) {
			
			if (myClient == null) {
				stillActive.set(false);
				criticalErrorMessage = "Le composant TCPClient n'est plus référencé sur le thread TCPClientThread.";
				criticalErrorOccured.set(true);
				tryCloseSocket();
				return;
			}
			
			try {
				InputStream input = mySocket.getInputStream();
				byte[] newBytesArray = new byte[1024];
				int bytesReceived = input.read(newBytesArray);
				// réception des nouveaux octets
				if (bytesReceived > 0) {
					synchronized (dataNotYetInBuffer_lock) { // modification des données de dataNotYetInBuffer
						int totalBufferLen = dataNotYetInBuffer.length + bytesReceived;
						if (totalBufferLen >= MAX_RCV_BUFFER_SIZE) {
							
							stillActive.set(false);
							criticalErrorMessage = "totalBufferLen dépasse la taille maximale autorisée (trop d'octets en attente sans message reçu) totalBufferLen = " + totalBufferLen + " MAX_RCV_BUFFER_SIZE = " + MAX_RCV_BUFFER_SIZE;
							criticalErrorOccured.set(true);
							tryCloseSocket();
							return;
						}
						byte[] newBuffer = new byte[totalBufferLen];
						System.arraycopy(dataNotYetInBuffer, 0, newBuffer, 0, dataNotYetInBuffer.length);
						System.arraycopy(newBytesArray, 0, newBuffer, dataNotYetInBuffer.length, bytesReceived);
						dataNotYetInBuffer = newBuffer;
					}
					while (checkForCompleteMessage()); // Ajout de tous les messages reçus au TCPClient
				}
				
			} catch (IOException e) {
				stillActive.set(false);
				criticalErrorMessage = "Exception " + e.getMessage();
				criticalErrorOccured.set(true);
				tryCloseSocket();
				//e.printStackTrace();
			}
		}
		
		tryCloseSocket();
	}
	
	
	public boolean isStillActive() {
		return stillActive.get();
	}
	public boolean criticalErrorOccured() {
		return criticalErrorOccured.get();
	}
	public String getCriticalErrorMessage() {
		if (criticalErrorOccured.get() == false) return "";
		return criticalErrorMessage;
	}
	public boolean isConnectedToHost() {
		return isConnected.get();
	}
	
	public void addMessageToSendList(NetBuffer message) {
		if (message == null) return;
		if (mySocket == null) return;
		synchronized (sendMessage_lock) {
			try {
				//System.out.println("TCPClientThread.addMessageToSendList()");
				byte[] buffToSend = message.convertToByteArray();
				OutputStream mySocketOutStream = mySocket.getOutputStream();
				mySocketOutStream.write(buffToSend);
				//System.out.println("TCPClientThread.addMessageToSendList() : OK ! Message bien envoyé.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void stop() { // TODO faire un meilleur synchronized
		synchronized (dataNotYetInBuffer_lock) {
			if (mySocket != null) {
				try {
					mySocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
				}
				mySocket = null;
			}
		}
	}
	public synchronized void close() { // TODO faire un meilleur synchronized
		stop();
	}
	
}
/* inutile, augmenter la taille du buffer d'envoi
class TCPClientThread_sendThread implements Runnable {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
}*/
