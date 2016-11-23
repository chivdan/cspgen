package ru.ifmo.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by Dmitry on 11/1/2016.
 */
public class TcpClient implements Closeable {
    private final BufferedReader myInput;
    private final DataOutputStream myOutput;
    private final Socket mySocket;

    public TcpClient(int port) throws IOException {
        mySocket = new Socket("127.0.0.1", port);
        myInput = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
        myOutput = new DataOutputStream(mySocket.getOutputStream());
    }

    public void write(String string) throws IOException {
        myOutput.writeChars(string);
        myOutput.flush();
    }

    public String readLine() throws IOException {
        return myInput.readLine();
    }

    @Override
    public void close() throws IOException {
        myOutput.close();
        myInput.close();
        mySocket.close();
    }


    public static void main(String[] args) throws Exception {
//        String testFile = args[0];
//        Random random = new Random();
//        TcpClient client = new TcpClient(7500);
//        TcpClient terminate = new TcpClient(7600); 
//        PrintWriter out = new PrintWriter(new File(testFile));
//
//        for (int i = 0; i < 10; i++) {
//            String result = (1 + random.nextInt(3)) + "";
//            System.out.println("Sending " + result);
//            client.write(result);
//            Thread.sleep(7000);
//        }
//        terminate.write("terminate");
//        client.close();
//        terminate.close();
    }
}

