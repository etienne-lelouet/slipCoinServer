package network.buffers;

//import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


public class NetBuffer { // fonctionnement synchrone, non thread-safe
	
	public ArrayList<NetBufferData> dataList = new ArrayList<NetBufferData>();
	private int currentReadPos = 0;
	
	private byte[] receivedRawData = null;
	
	public void writeInteger(int intData) {
		NetBufferData newData = new NetBufferData(intData);
		dataList.add(newData);
	}
	public void writeInt(int intData) { writeInteger(intData); }
	
	public void writeDouble(double doubleData) {
		NetBufferData newData = new NetBufferData(doubleData);
		dataList.add(newData);
	}
	
	public void writeString(String stringData) {
		NetBufferData newData = new NetBufferData(stringData);
		dataList.add(newData);
	}
	
	public void writeByteArray(byte[] byteArrayData) {
		NetBufferData newData = new NetBufferData(byteArrayData);
		dataList.add(newData);
	}
	public void writeBoolean(boolean boolData) {
		NetBufferData newData = new NetBufferData(boolData);
		dataList.add(newData);
	}
	public void writeBool(boolean boolData) { writeBoolean(boolData); }
	
	
	// Les Read retournent une exception si on tente de lire un 
	
	public int readInteger() throws IndexOutOfBoundsException {
		NetBufferData data = dataList.get(currentReadPos);
		currentReadPos++;
		if (data == null) return 0; // ne devrait pas arriver si le message est lu dans le bon ordre
		return data.integerData;
	}
	public int readInt() throws IndexOutOfBoundsException { return readInteger(); }

	public double readDouble() throws IndexOutOfBoundsException {
		NetBufferData data = dataList.get(currentReadPos);
		currentReadPos++;
		if (data == null) return 0; // ne devrait pas arriver si le message est lu dans le bon ordre
		return data.doubleData;
	}
	
	public String readString() throws IndexOutOfBoundsException {
		NetBufferData data = dataList.get(currentReadPos);
		currentReadPos++;
		if (data == null) return ""; // ne devrait pas arriver si le message est lu dans le bon ordre
		return data.stringData;
	}
	public String readStr() throws IndexOutOfBoundsException { return readString(); }
	
	public byte[] readByteArray() throws IndexOutOfBoundsException {
		NetBufferData data = dataList.get(currentReadPos);
		currentReadPos++;
		if (data == null) return new byte[0]; // ne devrait pas arriver si le message est lu dans le bon ordre
		return data.byteArrayData;
	}

	public boolean readBoolean() throws IndexOutOfBoundsException {
		NetBufferData data = dataList.get(currentReadPos);
		currentReadPos++;
		if (data == null) return false; // ne devrait pas arriver si le message est lu dans le bon ordre
		return data.booleanData;
	}
	public boolean readBool() throws IndexOutOfBoundsException { return readBoolean(); }
	
	// Vérification des types de données
	public boolean currentData_isInteger()    { NetBufferData data = dataList.get(currentReadPos); if (data.dataType.equals(NetBufferDataType.INTEGER))    return true; return false; }
	public boolean currentData_isDouble()     throws IndexOutOfBoundsException { NetBufferData data = dataList.get(currentReadPos); if (data.dataType.equals(NetBufferDataType.DOUBLE))     return true; return false; }
	public boolean currentData_isString()     throws IndexOutOfBoundsException { NetBufferData data = dataList.get(currentReadPos); if (data.dataType.equals(NetBufferDataType.STRING))     return true; return false; }
	public boolean currentData_isByteArray()  throws IndexOutOfBoundsException { NetBufferData data = dataList.get(currentReadPos); if (data.dataType.equals(NetBufferDataType.BYTE_ARRAY)) return true; return false; }
	public boolean currentData_isBoolean()    throws IndexOutOfBoundsException { NetBufferData data = dataList.get(currentReadPos); if (data.dataType.equals(NetBufferDataType.BOOLEAN))    return true; return false; }
	public boolean currentData_isInt()        { return currentData_isInteger(); }
	public boolean currentData_isStr()        throws IndexOutOfBoundsException { return currentData_isStr(); }
	public boolean currentData_isBool()       throws IndexOutOfBoundsException { return currentData_isBool(); }
	
	//public boolean currentData_isInteger()    throws IndexOutOfBoundsException { NetBufferData data = dataList.get(currentReadPos); if (data.dataType.equals(NetBufferDataType.INTEGER))    return true; return false; }
	//public boolean currentData_isInt()        throws IndexOutOfBoundsException { return currentData_isInteger(); }
	
	
	public void resetReadPosition() {
		currentReadPos = 0;
	}
	
	public NetBuffer() {
		
	}
	
	
	/** Récurérer les données brutes
	 * @param arg_receivedRawData
	 */
	public NetBuffer(byte[] arg_receivedRawData) {
		receivedRawData = arg_receivedRawData;
		readFromReceivedRawData();
	}
	
	
	/** Copier les donnes brutes du buffer de réception
	 * @param arg_threadReceivedBytesFULL
	 * @param copyStartIndex
	 * @param copyLength
	 */
	public NetBuffer(byte[] arg_threadReceivedBytesFULL, int copyStartIndex, int copyLength) {
		receivedRawData = new byte[copyLength];
		System.arraycopy(arg_threadReceivedBytesFULL, copyStartIndex, receivedRawData, 0, copyLength);
		readFromReceivedRawData();
		//System.out.println("NetBuffer create copyLength = " + copyLength);
	}
	
	
	/** Si c'est un buffer reçu, il y a des données brutes reçues
	 * 
	 * @return
	 */
	public byte[] getReceivedRawData() {
		return receivedRawData;
	}
	
	
	/** Transforme le NetBuffer en tableau d'octets
	 * @return
	 */
	public byte[] convertToByteArray() {
		// Ecriture de toutes les données dans un buffer
		// Je constitue la liste de sdonnées à envoyer
		int totalDataBufferSize = 0;
		ArrayList<byte[]> a2DataAsByteArray = new ArrayList<byte[]>();
		
		for (int iData = 0; iData < dataList.size(); iData++) {
			NetBufferData data = dataList.get(iData);
			byte[] dataToByteArray = data.toByteArray();
			a2DataAsByteArray.add(dataToByteArray);
			totalDataBufferSize += dataToByteArray.length;
		}
		
		if (totalDataBufferSize >= 1_000_000) { // Message beaucoup trop grand
			return null;
		}
		byte[] packetBuffer = new byte[4 + totalDataBufferSize];
		byte[] packetSizeBuffer = NetBufferData.intToByteArray(totalDataBufferSize);
		System.arraycopy(packetSizeBuffer, 0, packetBuffer, 0, packetSizeBuffer.length); // Taille du buffer de ce NetBuffer. packetSizeBuffer.length = 4
		int currentPosInPacketBuffer = 0 + 4; // les 4 octets indiquant la taille du message
		// Ajout des données
		for (int iData = 0; iData < a2DataAsByteArray.size(); iData++) {
			byte[] dataAsByteArray = a2DataAsByteArray.get(iData);
			System.arraycopy(dataAsByteArray, 0, packetBuffer, currentPosInPacketBuffer, dataAsByteArray.length);
			currentPosInPacketBuffer += dataAsByteArray.length;
		}
		return packetBuffer;
	}
	
	public void readFromReceivedRawData() {
		dataList.clear();
		currentReadPos = 0;
		
		if (receivedRawData == null) return;
		int currentPosInRawDataBuffer = 0;
		
		while (currentPosInRawDataBuffer < receivedRawData.length) {
			
			byte dataTypeAsByte = receivedRawData[currentPosInRawDataBuffer];
			currentPosInRawDataBuffer++;
			NetBufferDataType dataType = NetBufferDataType.getType(dataTypeAsByte);
			//NetBufferData data = null;
			switch (dataType) {
			
			case INTEGER :
				byte[] intByteArray = new byte[4];
				System.arraycopy(receivedRawData, currentPosInRawDataBuffer, intByteArray, 0, 4);
				currentPosInRawDataBuffer += 4;
				int intValue = NetBufferData.byteArrayToInt(intByteArray);
				NetBufferData intData = new NetBufferData(intValue);
				dataList.add(intData);
				break;
				
			case DOUBLE :
				byte[] doubleByteArray = new byte[8];
				System.arraycopy(receivedRawData, currentPosInRawDataBuffer, doubleByteArray, 0, 8);
				currentPosInRawDataBuffer += 8;
				double doubleValue = NetBufferData.byteArrayToDouble(doubleByteArray);
				NetBufferData doubleData = new NetBufferData(doubleValue);
				dataList.add(doubleData);
				break;
			
			case STRING :
				byte[] stringSizeByteArray = new byte[4];
				System.arraycopy(receivedRawData, currentPosInRawDataBuffer, stringSizeByteArray, 0, 4);
				currentPosInRawDataBuffer += 4;
				int stringSize = NetBufferData.byteArrayToInt(stringSizeByteArray);
				byte[] stringAsByteArray = new byte[stringSize];
				System.arraycopy(receivedRawData, currentPosInRawDataBuffer, stringAsByteArray, 0, stringSize);
				currentPosInRawDataBuffer += stringSize;
				String stringValue = new String(stringAsByteArray, StandardCharsets.UTF_8);
				NetBufferData stringData = new NetBufferData(stringValue);
				dataList.add(stringData);
				break;
			
			case BYTE_ARRAY :
				byte[] byteArraySizeByteArray = new byte[4];
				System.arraycopy(receivedRawData, currentPosInRawDataBuffer, byteArraySizeByteArray, 0, 4);
				currentPosInRawDataBuffer += 4;
				int byteArraySize = NetBufferData.byteArrayToInt(byteArraySizeByteArray);
				byte[] byteArrayValue = new byte[byteArraySize];
				System.arraycopy(receivedRawData, currentPosInRawDataBuffer, byteArrayValue, 0, byteArraySize);
				currentPosInRawDataBuffer += byteArraySize;
				NetBufferData byteArrayData = new NetBufferData(byteArrayValue);
				dataList.add(byteArrayData);
				break;

			case BOOLEAN :
				byte booleanAsByteValue = receivedRawData[currentPosInRawDataBuffer];
				currentPosInRawDataBuffer++;
				boolean booleanValue = (booleanAsByteValue == 1);
				NetBufferData boolData = new NetBufferData(booleanValue);
				dataList.add(boolData);
				break;
			default : break;
			}
		}
		
		
	}
	
	
}
