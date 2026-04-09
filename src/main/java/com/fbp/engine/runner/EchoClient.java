package com.fbp.engine.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class EchoClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        try(Socket socket = new Socket(host, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ){
            String messageToSend = "Hello FBP";
            out.println(messageToSend);
            String response = in.readLine();
            System.out.println("클라이언트 수신: "+ response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
