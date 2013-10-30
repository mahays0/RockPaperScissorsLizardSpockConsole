package com.example.javaclient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

import com.example.util.Message;

public class Client implements Runnable {

	/**
	 * @param args
	 */
	protected Socket socket;
	protected ObjectInputStream in;
	protected ObjectOutputStream out;
	protected String[] playerNames;
	protected 	String playerName;
	protected Integer[] scores;
	protected String move;
	protected boolean won=false;
	protected boolean okToMove=false;
	private boolean dropped; 
	public Client(int port, String ip, String playerName) throws UnknownHostException, IOException{
		socket = new Socket(ip,port);
		out=new ObjectOutputStream(socket.getOutputStream());
		out.flush();
		in=new ObjectInputStream(socket.getInputStream());
		this.playerName=playerName;
	}
	/**
	 * Contains the main game logic. Call from a new Thread so the app's UI doesn't lock up!
	 */
	public void run(){
		try{
		while(true){
			Message m = (Message) in.readObject();
			String cmd="";
			Object[] margs=null;
			if("getPlayerName".equals(m.command)){
				cmd="setPlayerName";
				margs=new Object[]{playerName};
			}else if("setPlayers".equals(m.command)){
				// get player names
				playerNames=(String[])m.args;
				System.out.println(playerNames.toString());
				displayPlayerNames();
			}else if("nextRound".equals(m.command)){
				// get updated scores
				scores=(Integer[])m.args;
				// display
				displayScores();
			}else if("getMove".equals(m.command)){
				// get move from user; already doing that implicitly?
				okToMove();
			}else if("winner".equals(m.command)){
				displayWinner((Integer)m.args[0]);
				won=true;
				break;
			}else if("drop".equals(m.command)){
				//break;
				dropped=true;
			}
			if(!"".equals(cmd)){
				Message response = new Message(cmd,margs);
				sendMessage(response);
			}
		}
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	protected void okToMove() {
		// TODO: call JS function in WebView to enable selecting a move/start timer/etc
		okToMove=true;
	}
	protected void displayWinner(Integer winner) {
		// TODO: call JS function in WebView to update h1 tag with winner
		System.out.println(playerNames[winner]+" won!");
	}
	/**
	 * Function that JS should call to notify the server that the player has set their move.
	 * @return
	 */
	//@JavascriptInterface
	public void sendMove(String move){
		this.move=move;
		this.okToMove=false;
		Message response = new Message("setMove",new Object[]{move});
		try{
			sendMessage(response);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	protected void displayPlayerNames() {
		// TODO: call JS function in WebView to rewrite table with correct number of players and their names.
		// relevant variable: playerNames
		System.out.println("Players now in game:");
		for(String s: playerNames){
			System.out.println(s);
		}
	}
	protected void displayScores(){
		// TODO: call JS function in WebView to update contents of table cells with new scores
		System.out.println();
		for(int i=0; i<scores.length; i++){
			System.out.print(playerNames[i]);
			System.out.print(" ");
			System.out.println(scores[i]);
		}
	}
	/**
	 * Demo of 
	 * @param args playerName port ip
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		String playerName=args[0];
		int port=Integer.valueOf(args[1]);
		String ip=args[2];
		Client c = null;
		try{
			c = new Client(port,ip,playerName);
			// to avoid locking up the app, you should run the main game loop in a Thread, like so:
			Thread t = new Thread(c);
			t.start();
			// console code to read in moves.
			// in mobile, the user would interact with a drop down.
			Scanner in = new Scanner(System.in);
			String[] moves = new String[]{
				"rock",
				"paper",
				"scissors",
				"lizard",
				"spock"
			};
			while(!c.won&&!c.dropped){
				while(!c.won&&!c.okToMove&&!c.dropped){
					Thread.sleep(500);
				}
				if(c.won||c.dropped) break;
				System.out.println("Enter a move number:");
				System.out.println("[1] Rock");
				System.out.println("[2] Paper");
				System.out.println("[3] Scissors");
				System.out.println("[4] Lizard");
				System.out.println("[5] Spock");
				String move = moves[in.nextInt()-1];
				c.sendMove(move);
			}
		}catch(UnknownHostException e){
			System.err.println("Never heard of that host...");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void setPlayerName(String playerName) throws IOException {
		Message m = new Message("setPlayerName",new String[]{playerName});
		sendMessage(m);
	}
	public void sendMessage(Message message) throws IOException {
		out.reset();
		out.writeObject(message);
	}
	public void win(Integer[] scores) {
		Message win=new Message("win",scores);
		try{
			sendMessage(win);
		}catch(Exception e){
			// dropped so...
		}
	}

}
