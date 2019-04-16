package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class SimpleDhtProvider extends ContentProvider {

    static final String[] REMOTE_PORT = {"11108","11112","11116","11120","11124"};
    String[] emulatorPorts = {"5554", "5556", "5558", "5560", "5562"};
    static final int SERVER_PORT = 10000;
    ArrayList<String> inRing = new ArrayList<String>();
    ArrayList<String> sortedNodes;
    ArrayList<String> myMessages = new ArrayList<String>();
    ArrayList<String> allNodes = new ArrayList<String>();

    ArrayList<String> chord = new ArrayList<String>();
    ArrayList<Message> chordClass = new ArrayList<Message>();
    ArrayList<String> myLocalKeys = new ArrayList<String>();

    String hashPort4 = "177ccecaec32c54b82d5aaafc18a2dadb753e3b1:11124";
    String hashPort1 = "208f7f72b198dadd244e61801abe1ec3a4857bc9:11112";
    String hashPort0 = "33d6357cfaaf0f72991b0ecd8c56da066613c089:11108";
    String hashPort2 = "abf0fd8db03e5ecb199a9b82929e9db79b909643:11116";
    String hashPort3 = "c25ddd596aa7c81fa12378fa725f706d54325d12:11120";


    String mainAvd = "11108";

    String myHashID;
    String myAvd;
    String myPort = null;

    Boolean queryerReceive = false;
    String originalQuery = "";

    String gotValue;

    String avdBefore = null;
    String avdAfter = null;
    String portBefore = null;
    String portAfter = null;
    String hashBefore;
    String hashAfter;
    String smallestNode;
    ArrayList<String> finalChordState = new ArrayList<String>();

    String globalStar= "";

    class chordComparator implements Comparator<Message>{

        @Override
        public int compare(Message x, Message y) {
            if(x.hashID.compareTo(y.hashID) < 0){
                return -1;
            }
            else if(x.hashID.compareTo(y.hashID) > 0){
                return 1;
            }
            return 0;
        }
    }


    class Message{
        String hashID = null;
        String myPort = null;
        String myAvd = null;
        Message pred = null;
        Message succ = null;

        Message(String hashID, String myPort, String myAvd, Message pred, Message succ) {
            this.hashID = hashID;
            this.myPort = myPort;
            this.myAvd = myAvd;
            this.pred = pred;
            this.succ = succ;
        }

    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    public int belongsWhere(String s){
//        Log.e(TAG, "I am: " + s);
        int avd;
        ArrayList<String> tempAll = new ArrayList<String>(allNodes);
        tempAll.add(s);
        Collections.sort(tempAll);
        Log.e(TAG,"In ring " + tempAll);
        if(allNodes.get(4) != tempAll.get(5)){
            Log.e(TAG,"belongs to AVD " + 0);

            return 0;
        }
        for(int i = 0; i < allNodes.size(); i++){
            if(tempAll.get(i) != allNodes.get(i)){
                Log.e(TAG,"belongs to AVD " + i);
                return i;
            }

        }

        return 0;
    }

    public String queryBelong(String s){
        ArrayList<String> tempAll = new ArrayList<String>(allNodes);
        tempAll.add(s);
        Collections.sort(tempAll);

        int x = tempAll.indexOf(s);

        if(x == 5){
            return allNodes.get(0);
        }
        else{
            return allNodes.get(x);
        }

    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.e(TAG,"This is selection delete: " + selection);
        File dir = getContext().getFilesDir();
        File file = new File(dir, selection);
        file.delete();
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    public void insertHelper(String filename, String value){
//        String filename = values.get("key").toString();
        String valueResult = value;
        String string = valueResult + "\n";

        FileOutputStream outputStream;

        String filePath = getContext().getFilesDir() + "/" + filename;
        File file = new File(filePath);
        try{
            if(!file.exists()){
                file.createNewFile();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        //copied from PA1
        try {
            outputStream = getContext().getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "File write failed");
        }

//        matrixC.addRow(new String[]{filename, string});
    }

    public MatrixCursor stringQuery(String selection, MatrixCursor matrixC){
        MatrixCursor haha = new MatrixCursor(new String[]{"key","value"});
        try{
            //learned how to open file from ----> https://stackoverflow.com/questions/14768191/how-do-i-read-the-file-content-from-the-internal-storage-android-app
            FileInputStream fis = getContext().openFileInput(selection);
            InputStreamReader isr = new InputStreamReader(fis);
            //basic reading using a bufferedReader
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            haha.addRow(new String[] {selection, sb.toString()});
            Log.i(TAG,"queried value: "+ sb.toString());
            return haha;


        }catch(IOException e){
            e.printStackTrace();
        }
        return haha;
    }

    public ArrayList<String> retrieveString(String selection){
        MatrixCursor haha = new MatrixCursor(new String[]{"key","value"});
        ArrayList<String> keyVal = new ArrayList<String>();

        try{
            //learned how to open file from ----> https://stackoverflow.com/questions/14768191/how-do-i-read-the-file-content-from-the-internal-storage-android-app
            FileInputStream fis = getContext().openFileInput(selection);
            InputStreamReader isr = new InputStreamReader(fis);
            //basic reading using a bufferedReader
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            keyVal.add(selection);
            keyVal.add(sb.toString());
            haha.addRow(new String[] {selection, sb.toString()});
            Log.i(TAG,"queried value: "+ sb.toString());
            return keyVal;


        }catch(IOException e){
            e.printStackTrace();
        }
        return keyVal;
    }

    public MatrixCursor localDump(MatrixCursor matrixC){
        for(int i = 0; i < myLocalKeys.size(); i++){
            try{
                //learned how to open file from ----> https://stackoverflow.com/questions/14768191/how-do-i-read-the-file-content-from-the-internal-storage-android-app
                FileInputStream fis = getContext().openFileInput(myLocalKeys.get(i));
                InputStreamReader isr = new InputStreamReader(fis);
                //basic reading using a bufferedReader
                BufferedReader bufferedReader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                matrixC.addRow(new String[] {myLocalKeys.get(i), sb.toString()});
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        return matrixC;
    }

    public String localDumpString(){
        String keysVal = "";
        for(int i = 0; i < myLocalKeys.size(); i++){
            try{
                //learned how to open file from ----> https://stackoverflow.com/questions/14768191/how-do-i-read-the-file-content-from-the-internal-storage-android-app
                FileInputStream fis = getContext().openFileInput(myLocalKeys.get(i));
                InputStreamReader isr = new InputStreamReader(fis);
                //basic reading using a bufferedReader
                BufferedReader bufferedReader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                keysVal +=  myLocalKeys.get(i) +":"+ sb.toString()+",";
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        return keysVal;
    }

    public String globalDump(String s){

        sendNodeToPort(portAfter + ":" );

        return s;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String filename = values.get("key").toString();
        String valueResult = values.get("value").toString();
        String string = valueResult + "\n";

        try {
            String hash = genHash(filename);
            Log.e(TAG,"Message " + filename + " belongs to " + belongsWhere(hash));
//            belongsWhere(hash);

//            myMessages.add(hash);
//            Log.e(TAG,"I am inserting!!!");
            if(portBefore == null && portAfter == null){
                insertHelper(filename, valueResult);
                myLocalKeys.add(filename);
            }
            else if(hash.compareTo(hashBefore) > 0 && hash.compareTo(myHashID) <= 0){
                Log.e(TAG,"I am inserting condition B");
                insertHelper(filename, valueResult);
                myLocalKeys.add(filename);

            }
            else if(myHashID.equals(smallestNode) && (hash.compareTo(myHashID)<0 || hash.compareTo(hashBefore)>0)){
                Log.e(TAG,"I am inserting condition C");

                insertHelper(filename, valueResult);
                myLocalKeys.add(filename);

            } else{
                Log.e(TAG,"I am sending to successor");

                sendToSuccessor("Insert:"+filename + ":" + hash + ":" + valueResult);
            }
        }
        catch(Exception e){
            Log.e(TAG,e.getMessage());
        }

        return uri;
    }

//    public Message locatePartition(String hashed){
//
//        return
//    }

    @Override
    public boolean onCreate() {

        try{
            allNodes.add(genHash("5554"));
            allNodes.add(genHash("5556"));
            allNodes.add(genHash("5558"));
            allNodes.add(genHash("5560"));
            allNodes.add(genHash("5562"));
//            if(myAvd == 0){
//                chord.add(genHash("11108"));
//            }
        }catch(Exception e){
            e.printStackTrace();
        }
        Collections.sort(allNodes);
        Log.e(TAG,"All nodes: "+ allNodes);

        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        myAvd = myAvdFromPort(myPort);

        sortedNodes = new ArrayList<String>();

//        avdBefore = Integer.toString(myAvd);
//        avdAfter = Integer.toString(myAvd);
//        portBefore = myPort;
//        portAfter = myPort;

//        matrixC = new MatrixCursor(new String[] {"key", "value"});

        try {
            myHashID = genHash(myAvd);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Can't generate the node id:\n" + e.getMessage());
        }

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            e.printStackTrace();
        }

        String joinParams = "Join:" + myHashID + ":" + myPort + ":" + myAvd;

        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, joinParams);



        return false;

    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        Log.e(TAG,"Query: I am: " + myPort + " requesting: " + selection);

        if(portBefore == null && portAfter == null){
            MatrixCursor matrixCurs = new MatrixCursor(new String[]{"key","value"});
            matrixCurs = localDump(matrixCurs);
            return matrixCurs;
        }
        else{
            if(selection.equals("@")){
                MatrixCursor matrixCurs = new MatrixCursor(new String[]{"key","value"});
                matrixCurs = localDump(matrixCurs);
                return  matrixCurs;
            }
            else if(selection.equals("*")){
                String allStar = localDumpString();
                String stopPort = myPort;
                String currentPort = portAfter;

                Log.e(TAG,"I am doing star: " + myPort);
                String[] temp = allStar.split(",");
                for(int i = 0; i < temp.length; i++){
                    String[] temp2 = temp[i].split(":");
                    Log.e(TAG,"My local has: " + Arrays.asList(temp2).toString());
                }
//
//                String[] allStarPair = allStar.split(",");
//
//                Log.e(TAG,"Thisis allStar: " + allStar);
//                Log.e(TAG,"This is starPair: " + Arrays.asList(allStarPair).toString());

                while(true) {
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(currentPort));
                        DataInputStream fromServ = new DataInputStream(socket.getInputStream());
                        DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                        String param = "Star:" + currentPort;

                        dataOut.writeUTF(param);
                        Log.e(TAG, "Star writing: " + param);
                        String receive = fromServ.readUTF();
                        Log.e(TAG, "I am avd: " + myPort);
                        Log.e(TAG, "Received his local " + receive);

                        String[] splitReceive = receive.split("&");
                        Log.e(TAG,"Split receive 1: " + splitReceive[1]);
                        currentPort = splitReceive[1];
                        allStar += splitReceive[0];


                        if(currentPort.equals(stopPort)){
                            break;
                        }



//                        MatrixCursor matrixCurs = new MatrixCursor(new String[]{"key", "value"});
//                        matrixCurs.addRow(new String[]{selection, receive});
//
//                        return matrixCurs;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Log.e(TAG,"Updated allStar: " + allStar);

                String[] updatedSplit = allStar.split(",");

                MatrixCursor matrixCurs =  new MatrixCursor(new String[]{"key","value"});

                for(int i = 0; i < updatedSplit.length; i++){
//                    Log.e(TAG,updatedSplit[i]);
                    String[] keyV = updatedSplit[i].split(":");
                    Log.e(TAG,"Key is: " + keyV[0] + " and val: " + keyV[1]);
                    matrixCurs.addRow(new String[]{keyV[0], keyV[1]});
                }


                Log.e(TAG, "I am returning star: " + myPort);


                return matrixCurs;
            }
            else{
                String hash;
//                Log.e(TAG,"My keys: " + myLocalKeys);

                try{
                    hash = genHash(selection);
                    Boolean gotIt = myLocalKeys.contains(selection);
                    String x = queryBelong(hash);
//                    Log.e(TAG, "For: " + selection + ", I should look at avd " + x + " or belongWhere: " + belongsWhere(hash));
//
                    if(hash.compareTo(hashBefore) > 0 && hash.compareTo(myHashID) <= 0){
                        ArrayList<String> temp =  retrieveString(selection);

                        String key = temp.get(0);
                        String val = temp.get(1);

                        Log.e(TAG,"I am querying condition B");

                        MatrixCursor matrixCurs =  new MatrixCursor(new String[]{"key","value"});
                        matrixCurs.addRow(new String[]{selection, val});

                        return matrixCurs;
                    }
                    else if(myHashID.equals(smallestNode) && (hash.compareTo(myHashID) < 0 || hash.compareTo(hashBefore)>0)){
                        ArrayList<String> temp =  retrieveString(selection);

                        String key = temp.get(0);
                        String val = temp.get(1);

                        Log.e(TAG,"I am querying condition C");

                        MatrixCursor matrixCurs =  new MatrixCursor(new String[]{"key","value"});
                        matrixCurs.addRow(new String[]{selection, val});

                        return matrixCurs;
                    } else{
//                        sendNodeToPort("mainNode:" + "11108:" + myPort + ":" + hash + ":" + selection);
                        String targetPort = "11108";
                        String sentFrom = myPort;
                        String hashedSelection = hash;

                        if(!myPort.equals("11108")) {
                            try {
                                Log.e(TAG, "sent by " + sentFrom);
                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(targetPort));
                                DataInputStream fromServ = new DataInputStream(socket.getInputStream());
                                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                                dataOut.writeUTF("mainNode:" + "11108:" + myPort + ":" + hash + ":" + selection);
                                Log.e(TAG, "Writing to mainNode to find where selection belongs");
                                String xx = fromServ.readUTF(); //receive selection belongs

                                Log.e(TAG, "Main avd said: " + selection + " belongs to " + xx);

                                try {
                                    Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                            Integer.parseInt(xx));
                                    DataInputStream fromServ1 = new DataInputStream(socket1.getInputStream());
                                    DataOutputStream dataOut1 = new DataOutputStream(socket1.getOutputStream());

                                    String param = "Direct:" + x+ ":" + myPort + ":" + selection;
//
//
                                    dataOut1.writeUTF(param);
                                    Log.e(TAG, "Writingg: " + param);
                                    String receive = fromServ1.readUTF();
                                    Log.e(TAG,"Readdd message " + receive);
                                    Log.e(TAG, "I am requesterr: " + myPort);

                                    MatrixCursor matrixCurs =  new MatrixCursor(new String[]{"key","value"});
                                    matrixCurs.addRow(new String[]{selection, receive});

                                    return matrixCurs;

                                }catch(Exception e){
                                    e.printStackTrace();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                        }else{

                            String destinationPort = "";

                            Log.e(TAG,"looking for where " + selection + " belongs in chord size: " + chordClass.size());
                            Log.e(TAG,"This is hashed selection btw: " + hashedSelection);
                            for(int i = 0; i < chordClass.size(); i++){
                                if(hashedSelection.compareTo(chordClass.get(i).pred.hashID) > 0 && hashedSelection.compareTo(chordClass.get(i).hashID) <= 0){
                                    Log.e(TAG,"Message belongs to: " + chordClass.get(i).myAvd);
                                    destinationPort = chordClass.get(i).myPort;
                                    String param = "Direct:" + destinationPort+ ":" + chordClass.get(i).myPort + ":" + selection;

                                    try {
                                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                                Integer.parseInt(destinationPort));
                                        DataInputStream fromServ = new DataInputStream(socket.getInputStream());
                                        DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                                        dataOut.writeUTF(param);
                                        Log.e(TAG, "Writing: " + param);
                                        String receive = fromServ.readUTF();
                                        Log.e(TAG,"Read message " + receive);
                                        Log.e(TAG, "I am avd: " + myPort);

                                        MatrixCursor matrixCurs =  new MatrixCursor(new String[]{"key","value"});
                                        matrixCurs.addRow(new String[]{selection, receive});

                                        return matrixCurs;

                                    }catch(Exception e){
                                        e.printStackTrace();
                                    }
                                }
                                else if(chordClass.get(i).hashID.equals(smallestNode) && (hashedSelection.compareTo(chordClass.get(i).hashID)<0 || hashedSelection.compareTo(chordClass.get(i).pred.hashID)>0)){
                                    Log.e(TAG,"Message belongs to: " + chordClass.get(i).myAvd);
                                    destinationPort = chordClass.get(i).myPort;
                                    String param = "Direct:" + destinationPort+ ":" + chordClass.get(i).myPort + ":" + selection;

                                    try {
                                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                                Integer.parseInt(destinationPort));
                                        DataInputStream fromServ = new DataInputStream(socket.getInputStream());
                                        DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                                        dataOut.writeUTF(param);
                                        Log.e(TAG, "Writing: " + param);
                                        String receive = fromServ.readUTF();
                                        Log.e(TAG,"Readd message " + receive);
                                        Log.e(TAG, "I am requester: " + myPort);

                                        MatrixCursor matrixCurs =  new MatrixCursor(new String[]{"key","value"});
                                        matrixCurs.addRow(new String[]{selection, receive});

                                        return matrixCurs;

                                    }catch(Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        Log.e(TAG,"Hope this is not running");
//                        return new MatrixCursor(new String[]{selection, gotValue});
                    }

                }catch(Exception e){
                    e.printStackTrace();
                }
                Log.e(TAG,"Hope this is not running2");

                return new MatrixCursor(new String[]{"key","value"});
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    //could not create client task directly so did it through a method
    //sends each node their succ and pred info.
    public void notifyAllNodes(){
        smallestNode = chordClass.get(0).hashID;
        Log.e(TAG,"notifying smallest node: " + smallestNode);

        for(int i = 0; i < chordClass.size(); i++){
            String command = "nodeInfo";
            Message currentNode = chordClass.get(i);
            String joinRequestParam = command + ":" +
                    currentNode.hashID + ":" +
                    currentNode.myPort + ":" +
                    currentNode.succ.hashID + ":" +
                    currentNode.pred.hashID + ":" +
                    currentNode.succ.myPort + ":" +
                    currentNode.pred.myPort + ":" +
                    smallestNode;
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, joinRequestParam);
        }
    }

    public void sendToSuccessor(String s){
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, s);
    }

    public MatrixCursor querySuccessor(String s){
        MatrixCursor matrix = new MatrixCursor(new String[]{"key","value"});

        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, s);

        return matrix;
    }
    public MatrixCursor queryPredecessor(String s){
        MatrixCursor matrix = new MatrixCursor(new String[]{"key","value"});

        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, s);

        return matrix;
    }

    public void sendNodeToPort(String s){
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, s);

    }

    public void sendBackToOriginal(String s){
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, s);

    }

    public void queryNextUsingSocket(String nextPort){

        try {
            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(nextPort));
            DataInputStream fromServ = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());



        }catch(Exception e){
            e.printStackTrace();
        }

    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        Uri uri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            while (true) {
                try {
                    Socket sock =  serverSocket.accept();
                    DataInputStream fromClient = new DataInputStream(sock.getInputStream());
                    DataOutputStream dataOut = new DataOutputStream(sock.getOutputStream());

                    String message = fromClient.readUTF();
                    String messageSplit[] = message.split(":");

                    if(messageSplit[0].equals("Join")){
                        String emuPort = messageSplit[3];
                        String nodeRequestID = messageSplit[1];
                        String nodeRequestPort = messageSplit[2];
                        Message nodeInfo = new Message(nodeRequestID, nodeRequestPort, emuPort, null, null);
                        Log.e(TAG, "Server " + myAvd + " got join request from: " + nodeRequestPort + " " + nodeRequestID);

                        chord.add(nodeRequestID);
                        Collections.sort(chord);

                        //placing node in correct position in chordClass
                        for(int i = 0; i < chord.size(); i++){
                            if(chord.get(i).equals(nodeInfo.hashID)){
                                chordClass.add(i, nodeInfo);
                            }
                        }

                        Log.e(TAG,"Chord status: " + chord);
                        Log.e(TAG, "ChordClass size: " + chordClass.size());

                        //this accounts for all avds
                        for(int i = 0; i < chord.size(); i++){
                            try {
                                chordClass.get(i).succ = chordClass.get(i + 1);
                                chordClass.get(i + 1).pred = chordClass.get(i);
                            }catch(Exception e){
                                //case of partition between last node and first node
                                chordClass.get(i).succ = chordClass.get(0);
                                chordClass.get(0).pred = chordClass.get(i);
                            }
                        }
                        //this only accounts for 5 avds and not 1-5
//                        if(chord.size() == 5){
//                            for(int i = 0; i < chord.size(); i++){
//                                if(i != 0 && i != chord.size()-1){
//                                    chordClass.get(i).succ = chordClass.get(i+1);
//                                    chordClass.get(i).pred = chordClass.get(i-1);
//                                }
//                            }
//                            chordClass.get(0).succ = chordClass.get(1);
//                            chordClass.get(0).pred = chordClass.get(chord.size()-1);
//                            chordClass.get(chord.size()-1).succ = chordClass.get(0);
//                            chordClass.get(chord.size()-1).pred = chordClass.get(chord.size()-2);
//
//
//                            for(int i = 0; i < chordClass.size(); i++){
//                                Log.e(TAG, "Test order: " + chordClass.get(i).hashID +
//                                        " ("+ myAvdNum(chordClass.get(i).myAvd) +
//                                        ") succ: " + myAvdNum(chordClass.get(i).succ.myAvd) +
//                                        " pred " + myAvdNum(chordClass.get(i).pred.myAvd));
//                            }
                            notifyAllNodes();

//                        }
                    }

                    //receiving info my avd 0 about my succ and pred
                    if(messageSplit[0].equals("nodeInfo")){
                        String nodeHash = messageSplit[1];
                        String nodePort = messageSplit[2];
                        String nodeSuccHash = messageSplit[3];
                        String nodePredHash = messageSplit[4];
                        String nodeSuccPort = messageSplit[5];
                        String nodePredPort = messageSplit[6];

                        myAvd = myAvdFromPort(nodePort);
                        portAfter = nodeSuccPort;
                        portBefore = nodePredPort;
                        hashAfter = nodeSuccHash;
                        hashBefore = nodePredHash;
                        smallestNode = messageSplit[7];

                        Log.e(TAG, "received smallest nodee is: " + smallestNode);

                        Log.e(TAG, "I am avd " + myAvd +
                                ". My succ is " + myAvdFromPort(nodeSuccPort) +
                                ". My pred is " + myAvdFromPort(nodePredPort));
                    }
                    if(messageSplit[0].equals("Insert")){
                        String filename = messageSplit[1];
                        String hashedFileName = messageSplit[2];
                        String value = messageSplit[3];


                        ContentValues values = new ContentValues();
                        values.put("key", filename);
                        values.put("value", value);

                        insert(uri, values);
                    }
                    if(messageSplit[0].equals("Query")){
                        Log.e(TAG, "Received query from pred. Attempting to query...");
                        String selection = messageSplit[1];
                        String hashedSelection = messageSplit[2];
                        String originalRequester = messageSplit[4];
                        originalQuery = originalRequester;
                        Log.e(TAG,"original requester " + originalQuery);
                        MatrixCursor temp = new MatrixCursor(new String[]{"key", "value"});

                        temp = (MatrixCursor) query(uri, null, selection, null, null);


                        if(hashedSelection.compareTo(hashBefore) > 0 && hashedSelection.compareTo(myHashID) <= 0){
                            Log.e(TAG,"I am query condition B");
                            ArrayList<String> tempVals =  retrieveString(selection);
//
                            String key = tempVals.get(0);
                            String val = tempVals.get(1);
                            sendBackToOriginal("FoundQ:" + key + ":" + val + ":" + originalRequester);

                        }
                        else if(myHashID.equals(smallestNode) && (hashedSelection.compareTo(myHashID) < 0 || hashedSelection.compareTo(hashBefore)>0)){
                            Log.e(TAG,"I am query condition C");
                            ArrayList<String> tempVals =  retrieveString(selection);
//
                            String key = tempVals.get(0);
                            String val = tempVals.get(1);
                            sendBackToOriginal("FoundQ:" + key + ":" + val + ":" + originalRequester);

                        } else{
                            Log.e(TAG,"I am querying at successor " + myAvdFromPort(portAfter));

                            querySuccessor("Query:" + selection + ":" + hashedSelection + ":" + myHashID + ":" + portAfter);
                        }
                    }

                    if(messageSplit[0].equals("FoundQ")){
                        String selection = messageSplit[1];
                        String value = messageSplit[2];
                        String originalPort = messageSplit[3];

                        Log.e(TAG,"This is server and original port is: " + originalPort);
                        if(originalPort == myPort){
                            Log.e(TAG,"Original port is " + originalPort + " and my port is " + myPort);
//                            queryerReceive = true;
                        }

                        gotValue = value;

                        Log.e(TAG,"Got value is now: " +value + " and my port is " + myPort);

                        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});

                        cursor.addRow(new String[] {selection, value});
                    }
                    if(messageSplit[0].equals("Direct")){
                        String destination = messageSplit[1];
                        String fromPort = messageSplit[2];
                        String selection = messageSplit[3];

                        ArrayList<String> keyV = retrieveString(selection);

                        dataOut.writeUTF(keyV.get(1));
                        sock.close();

                    }
                    if(messageSplit[0].equals("Direct2")){
                        String destination = messageSplit[1];
                        String fromPort = messageSplit[2];
                        String selection = messageSplit[3];

                        ArrayList<String> keyV = retrieveString(selection);

                        dataOut.writeUTF(keyV.get(1));

                    }
                    if(messageSplit[0].equals("mainNode")){
                        String targetPort = messageSplit[1];
                        String sentFrom = messageSplit[2];
                        String hashedSelection = messageSplit[3];
                        String selection = messageSplit[4];

                        Log.e(TAG,"I am main node: " + myAvdFromPort(myPort));
                        Log.e(TAG,"I am main node and I received request from " + sentFrom);
                        Log.e(TAG,"Selection is: " + selection + " and hashedSelect is: " + hashedSelection);

                        for(int i = 0; i < chordClass.size(); i++){
                            Log.e(TAG,"Running " + i);
                            if(hashedSelection.compareTo(chordClass.get(i).pred.hashID) > 0 && hashedSelection.compareTo(chordClass.get(i).hashID) <= 0){
                                Log.e(TAG,"Message belongs to: " + chordClass.get(i).myAvd);
                                String destinationPort = chordClass.get(i).myPort;
                                dataOut.writeUTF(destinationPort);
                                break;
                            }
                            else if(chordClass.get(i).hashID.equals(smallestNode) && (hashedSelection.compareTo(chordClass.get(i).hashID)<0 || hashedSelection.compareTo(chordClass.get(i).pred.hashID)>0)){
                                Log.e(TAG,"Message belongs to: " + chordClass.get(i).myAvd);
                                String destinationPort = chordClass.get(i).myPort;
                                dataOut.writeUTF(destinationPort);
                                break;

                            }
                        }
                    }
                    if(messageSplit[0].equals("Star")){
                        String originalPort = messageSplit[1];

                        String myLocal = localDumpString();
                        String[] temp = myLocal.split(",");
                        Log.e(TAG,"Local is: " + Arrays.asList(temp).toString());
//                        for(int i = 0; i < temp.length; i++){
//                            String[] temp2 = temp[i].split(":");
//                            Log.e(TAG,"My local has: " + temp2[0] + " and " + temp2[1]);
//                        }
                        Log.e(TAG,"Star dumping: " + myLocal+"&"+portAfter);
                        Log.e(TAG,"Next port is: " + portAfter);
                        dataOut.writeUTF(myLocal+"&"+portAfter);

                    }
                    sock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String[] messageSplit = msgs[0].split(":");
            String msgToSend = msgs[0];
            if(messageSplit[0].equals("Join")) {
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[0]));
                    DataInputStream fromServ = new DataInputStream(socket.getInputStream());
                    DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                    dataOut.writeUTF(msgs[0]);
                    Log.e(TAG, "Sent msgs!: " + Arrays.toString(messageSplit));

                }
                catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }

            }
            if(messageSplit[0].equals("nodeInfo")){
                String nodePort = messageSplit[2];
                try {
                    Log.e(TAG,"Receiving info from avd 0");
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(nodePort));
                    DataInputStream fromServ = new DataInputStream(socket.getInputStream());
                    DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                    dataOut.writeUTF(msgs[0]);
                    Log.e(TAG, "Sent msgs!: " + Arrays.toString(messageSplit));

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            if(messageSplit[0].equals("Insert")){
                String filename = messageSplit[1];
                String hashedFileName = messageSplit[2];
                String value = messageSplit[3];
                Log.e(TAG,"This is hashed file: " + hashedFileName);
//                Log.e(TAG,"This is smallest node: " + smallestNode);
                ArrayList<String> testPartition = new ArrayList<String>();
                testPartition.add(myHashID);
                testPartition.add(hashAfter);
                testPartition.add(hashedFileName);

                try {
                    Log.e(TAG,"Receiving insert");
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(portAfter));
                    DataInputStream fromServ = new DataInputStream(socket.getInputStream());
                    DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                    dataOut.writeUTF(msgs[0]);
                    Log.e(TAG, "Sent msgs!: " + Arrays.toString(messageSplit));

                }catch(Exception e){
                    e.printStackTrace();
                }

            }
            if(messageSplit[0].equals("Query")){
                try {
//                    Log.e(TAG,"sending query to successor");
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(portAfter));
                    DataInputStream fromServ = new DataInputStream(socket.getInputStream());
                    DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                    dataOut.writeUTF(msgs[0]);
//                    fromServ.read()
                    Log.e(TAG, "querying successor!: " + Arrays.toString(messageSplit));

                }catch(Exception e){
                    e.printStackTrace();
                }
            }

            if(messageSplit[0].equals("FoundQ")){
                try {
                    Log.e(TAG,"originalPEEP " + messageSplit[3]);
//                    Log.e(TAG,"sending query to successor");
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(messageSplit[3]));
                    DataInputStream fromServ = new DataInputStream(socket.getInputStream());
                    DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                    dataOut.writeUTF(msgs[0]);
                    Log.e(TAG, "querying pred!: " + Arrays.toString(messageSplit));

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            if(messageSplit[0].equals("Direct")){
                String destination = messageSplit[1];
                String fromPort = messageSplit[2];
                String selection = messageSplit[3];
                Log.e(TAG,"This is direct: " + Arrays.asList(messageSplit).toString());
                try {
                    Log.e(TAG,"sent by " + messageSplit[2]);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(destination));
                    DataInputStream fromServ = new DataInputStream(socket.getInputStream());
                    DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                    dataOut.writeUTF(msgs[0]);
                    Log.e(TAG, "querying direct!: " + Arrays.toString(messageSplit));

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            return null;
        }

    }

    public String myAvdFromPort(String s){
        int index =  Arrays.asList(REMOTE_PORT).indexOf(s);
        return emulatorPorts[index];
    }
    public String myAvdNum(String s){
        return Integer.toString(Arrays.asList(emulatorPorts).indexOf(s));
    }

}
