package com.example.javaserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.util.Message;

public class PlayerThread{
	public Socket socket;
	ObjectInputStream in;
	ObjectOutputStream out;
	public String status="";
	public String playerName="";
	public String lastMove="";
	boolean active;
	private ExecutorService executor;
	public PlayerThread(Socket s) throws StreamCorruptedException, IOException{
		executor=Executors.newSingleThreadExecutor();
		active=true;
		socket=s;
		out=new ObjectOutputStream(s.getOutputStream());
		out.flush();
		in=new ObjectInputStream(s.getInputStream());
	}
	public void drop(Integer[] scores){
		Message drop=new Message("drop",scores);
		try{
			sendMessage(drop);
		}catch(Exception e){
			// dropped so...
			e.printStackTrace();
		}
	}
	public void sendMessage(Message message) throws IOException {
		out.reset();
		out.writeObject(message);
	}
	public void win(Integer winner) {
		Message win=new Message("winner",new Object[]{winner});
		try{
			sendMessage(win);
		}catch(Exception e){
			// dropped so...
			e.printStackTrace();
		}
	}
	public void nextRound(Integer[] scores) {
		Message nextRound=new Message("nextRound",scores);
		try{
			sendMessage(nextRound);
		}catch(Exception e){
			// dropped so...
			e.printStackTrace();
		}
	}
	public void setPlayers(String[] playerNames) {
		// TODO Auto-generated method stub
		Message setPlayers=new Message("setPlayers",playerNames);
		try{
			sendMessage(setPlayers);
		}catch(Exception e){
			// dropped so...
			e.printStackTrace();
		}
	}
	public String getPlayerName() throws IOException {
		Message setPlayers=new Message("getPlayerName",new Object[]{});
		sendMessage(setPlayers);
		try{
			Message response=(Message) in.readObject();
			this.playerName=(String) response.args[0];
			return (String)response.args[0];
		}catch(Exception e){
			e.printStackTrace();
			return "error getting name";
		}
	}
	public void getMove() {
		Message getMove=new Message("getMove",new Object[]{});
		try{
			sendMessage(getMove);
		}catch(Exception e){
			e.printStackTrace();
		}
		// asynchronously read response
		executor.submit(new Runnable(){
			@Override
			public void run() {
				try{
					Message m = (Message)in.readObject();
					// TODO: verify it is actually a move
					lastMove = (String)m.args[0];
					System.err.println(playerName+": "+lastMove);
				}catch(Exception e){
					e.printStackTrace();
					lastMove="error";
				}
			}
			
		});
	}
	public void stop() {
		// TODO Auto-generated method stub
		executor.shutdownNow();
	}

}
