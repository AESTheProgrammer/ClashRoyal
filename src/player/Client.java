package player;

import gameUtil.AliveTroop;
import gameUtil.Card;
import gameUtil.Troop;
import javafx.geometry.Point2D;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;

public class Client extends Player implements Runnable {
    // status of the client
    // reader which reads line of string from buffer
    // out is the outputStream who send byte streams to server
    // last respond which was received from the server
    // indicates weather player is connected to the game or not
    private Status status;
    private BufferedReader reader;
    private OutputStream out;
    private String lastRespond;
    private boolean isConnected = false;

    /** this is the main method the class which is used for running a client server. */
    @Override
    public void run() {
        try (Socket connectionSocket = new Socket("127.0.0.1", 7660)) {
            InputStream in = connectionSocket.getInputStream();
            out = connectionSocket.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(in));
            startMessageListener();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * this is a getter
     * @return status of the client
     */
    public Status getStatus () {
        return status;
    }

    /**
     * this method set status
     * @param status is the status of the client
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * this method starts the message listener
     */
    private void startMessageListener() throws InterruptedException {
        Thread listener = new Thread(this::readMessage);
        listener.start();
        listener.join();
    }

    /**
     * this method read and get message from server
     */
    private void readMessage() {
        String command;
        try {
            while((command = reader.readLine()) != null) {
                lastRespond = command;
                if (lastRespond.contains("PLAY")) {
                    System.out.println("Received");
                    String[] partitions = command.split(" ", 4);
                    Point2D point = new Point2D(
                            Double.parseDouble(partitions[1]),
                            Double.parseDouble(partitions[2]));
                    Card card = (Card)fromString(partitions[3]);
                    if (lastRespond.contains("PLAY_ALLY"))
                        addTroopToAList(status.getAliveAllyTroops(),
                                        card, point);
                    else
                        addTroopToAList(status.getEnemyStatus().getAliveAllyTroops(),
                                        card, point);
                }
            }
        } catch(IOException | ClassNotFoundException ex) {ex.printStackTrace();}
    }

    private void addTroopToAList(ArrayList<AliveTroop> aliveTroops, Card card, Point2D point) {
        for (int i = 0; i < (card instanceof Troop ? ((Troop) card).getCount() : 1); i++) {
            if (i % 2 == 0)
                aliveTroops.
                        add(new AliveTroop(card,
                        new Point2D(point.getX() + 20 * i, point.getY())));
            if (i % 2 != 0)
                aliveTroops.
                        add(new AliveTroop(card,
                        new Point2D(point.getX(), point.getY() + 20 * i)));
        }
    }

    /**
     * this method writes and send message for server
     * @param message is the message which will be sent
     */
    public void sendCommand(String message) {
        try {
            out.write((message + "\n").getBytes());
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * this method return the last respond from the server
     * @return last respond from server
     */
    public String getLastRespond() {
        return lastRespond;
    }

    /** Read the object from a Base64 string. */
    private static Object fromString( String s ) throws IOException ,
            ClassNotFoundException {
        byte [] data = Base64.getDecoder().decode( s );
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(  data ) );
        Object o  = ois.readObject();
        ois.close();
        return o;
    }

    /**
     * this method disconnect the player from the game
     */
    public void disconnect() {
        isConnected = false;
    }

    /**
     * this method connect the player to the game
     */
    public void connect() {
        isConnected = true;
    }

    /**
     * this method returns true if the client is connected to the game else flase
     */
    public boolean isConnected() {
        return isConnected;
    }
}
