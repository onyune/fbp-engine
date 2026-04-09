package com.fbp.engine.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
    public static void main(String[] args) {
        int port = 8080;

        try(ServerSocket serverSocket = new ServerSocket(port)){
            while(true){
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket){
        try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)){
            String inputLine ;
            while((inputLine = in.readLine()) != null){
                System.out.println("서버 수신: "+ inputLine);
                out.println("ECHO: "+ inputLine);
            }
        }catch (IOException e){
            System.err.println("클라이언트 통신 에러: "+ e.getMessage());
        }
    }
}
