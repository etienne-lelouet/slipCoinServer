package server;

import java.util.ArrayList;

import dataClasses.Compte;
import dataClasses.Personne;
import dataClasses.User;
import modele.Modele;
import network.buffers.NetBuffer;
import network.tcp.TCPClient;
import network.tcp.TCPServer;
import modele.Database;
import server.ServerClient;

public class ApplicationServeur {
    public int tcpPort;
    public Database db;

    /** Ecrire un message sur la console (+ rapide à écrire !)
     * @param infoMessage message à écrire
     */
    public static void log(String infoMessage) {
            synchronized(WriteOnConsoleClass.LOCK) { System.out.println("ApplicationServeur : " + infoMessage); } //System.out.flush();
            //System.out.println("ApplicationServeur : " + infoMessage);
    }

    public static int nextClientID = 1;

    public ArrayList<ServerClient> serverClientList = new ArrayList<ServerClient>();

    public static void sleep(long millisec) {
            try { Thread.sleep(millisec); } catch (InterruptedException e1) {
                    e1.printStackTrace();
            }
    }

    TCPServer server;

    public ApplicationServeur (int tcpPort, Database db) {
        this.tcpPort = tcpPort;
        this.db = db;
    }
    
    public void start() {
        server = new TCPServer(tcpPort);
        if (server.isListening()) {
                log("Le serveur écoute sur le port " + tcpPort);
        } else {
                server.stop();
                return;
        }

        // Boucle du serveur
        while (server.isListening()) {
            // Accepter de nouveaux clients (asynchrone)
            TCPClient newTCPClient = server.accept(); // non bloquant
            if (newTCPClient != null) {
                // Nouveau client accepté !
                // Je crée le client du serveur
            
                ServerClient client = new ServerClient(newTCPClient);
                serverClientList.add(client);

                /*
                System.out.println("Serveur : nouveau client - Liste des clients :");
                for (int i = 0; i < serverClientList.size(); i++) {
                        System.out.println(serverClientList.get(i).ID);
                }*/
            }

            // Suppression des clients qui ne sont plus connectés
            int clientIndex = 0;
            while (clientIndex < serverClientList.size()) {
                ServerClient servClient = serverClientList.get(clientIndex);
                if ( ! servClient.tcpSock.isConnected() )  {
                    boolean criticalErrorOccured = servClient.tcpSock.criticalErrorOccured();
                    if (criticalErrorOccured) {
                            log("Erreur critique sur un client, déconnexion : " + servClient.tcpSock.getCriticalErrorMessage());
                    }
                    servClient.tcpSock.stop(); // facultatif
                    serverClientList.remove(clientIndex);
                } else
                    clientIndex++;
            }

            // Ecouter ce que les clients demandent
            for (clientIndex = 0; clientIndex < serverClientList.size(); clientIndex++) {
                    ServerClient servClient = serverClientList.get(clientIndex);
                    NetBuffer newMessage = servClient.tcpSock.getNewMessage();
                    //log("pret a recevoire mess");
                    if (newMessage != null) {
                        if (! newMessage.currentData_isInt()) {
                                log("ERREUR : message mal formatt�.");
                        } else {
                            int messageType = newMessage.readInteger();
                            NetBuffer reply = new NetBuffer();
                          
                            switch(messageType){
                            	case 0:
                            		 String uname = newMessage.readString();
                                     String upass = newMessage.readString();
                                     String name = newMessage.readString();
                                     String pname = newMessage.readString();
                                     String d = newMessage.readString();
                                     String c=newMessage.readString();
                                   
                                     int id_person=Modele.insertPersonne(db, uname, upass, name, pname, d, c);
                                    
                                     if (id_person>0) {
                                    	 reply.writeBoolean(true);
                              
                                     }
                                     else {
                                    	 reply.writeBoolean(false);
                                     }
                                     servClient.tcpSock.sendMessage(reply);
                                     
                                     
                            		break;
                                case 1:
                                    String username = newMessage.readString();
                                    String password = newMessage.readString();
                                    log("username="+username);
                                    User user = Modele.Connexion(db, username, password);
                                    
                                    
                                    if (user != null) { 
                                        reply.writeInt(user.getIdUser());
                                        reply.writeBool(true);
                                        //reply.writeString("Bienvenue " + username);
                                        servClient.tcpSock.sendMessage(reply);  
                                    } else {
                                        reply.writeInt(1);
                                        reply.writeBool(false);
                                        //reply.writeString("couple login/mot de passe inconnu");
                                        servClient.tcpSock.sendMessage(reply);
                                    }
                                    break;
                                case 2:
                                    
								int idPersonne=newMessage.readInt();
								Personne perso=Modele.SelectionnerPersonne(db, idPersonne);
								Compte compte=Modele.SelectionnerCompte(db,idPersonne);
								if (perso != null && compte!=null) { 
                                    reply.writeString(perso.getNom());
                                    reply.writeString(perso.getPrenom());
                                    reply.writeString(perso.getDateNaissance());
                                    reply.writeDouble(compte.getSolde());
                                    servClient.tcpSock.sendMessage(reply);  
                                } else {
                                    reply.writeInt(2);
                                    reply.writeBool(false);
                                    //reply.writeString("couple login/mot de passe inconnu");
                                    servClient.tcpSock.sendMessage(reply);
                                }
                                break;
                                case 4:
                                	int idPerso=newMessage.readInt();                      
                                	String numCompte_c=newMessage.readString();
                                	double m=newMessage.readDouble();
                                	Compte compte_d=Modele.SelectionnerCompte(db,idPerso);
                                	boolean check_transaction = false;
                                	try {
                                		check_transaction=Modele.effectuerTransaction(db, compte_d.getNumeroCompte(), numCompte_c,(float) m);
									} catch (Exception e) {
										check_transaction=false;
									}
                                
                                	reply.writeBoolean(check_transaction);
                                	servClient.tcpSock.sendMessage(reply);
                                    break;
                                default:
                                    reply.writeInt(9);
                                    reply.writeString("error");
                                    reply.writeString("Id Du message invalide");
                                    servClient.tcpSock.sendMessage(reply);
                            }

                        }
                    }
            }
            sleep(1); // 1ms entre chaque itération, minimum
        }

    }
	
	public void forceStop() {
		if (server == null) return;
		server.stop();
	}
	
}

class WriteOnConsoleClass {
	static public final Object LOCK = new Object();
}