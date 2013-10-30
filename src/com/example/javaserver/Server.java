package com.example.javaserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Server {

	public String status="";
	public PlayerThread[] players;
	public String[] playerNames;
	/**
	 * Box scores as Integer to pass over network
	 */
	public Integer[] scores;
	/**
	 * Map of things that beat other things. For instance: beats.get("rock") returns a list containing "scissors" and "lizard". 
	 */
	public HashMap<String,List<String>> beats;
	public Server(int nPlayers, int port, String ip) throws UnknownHostException, IOException, InterruptedException{
		// build list
		String[] pairs=new String[]{
			"rock > scissors",
			"rock > lizard",
			"paper > rock",
			"paper > spock",
			"scissors > paper",
			"scissors > lizard",
			"lizard > paper",
			"lizard > spock",
			"spock > rock",
			"spock > scissors"
		};
		System.err.println("initing beats index");
		beats=new HashMap<String,List<String>>();
		for(String s: pairs){
			String[] pair=s.split(" > ");
			if(!beats.containsKey(pair[0])){
				beats.put(pair[0],new LinkedList<String>());
			}
			beats.get(pair[0]).add(pair[1]);
		}
		System.err.println("Done initing beats, starting socket.");
		// TODO: listen on some port for discovery broadcasts
		// listen for clients who know about this server
		ServerSocket s = new ServerSocket(port, 50, InetAddress.getByName(ip));
		// loop until desired number of players joins
		status="Waiting on other players";
		players=new PlayerThread[nPlayers];
		playerNames=new String[nPlayers];
		System.err.println("Server socket up, waiting for connection.");
		for(int i=0; i<nPlayers; i++){
			// accept is a blocking call
			PlayerThread player=new PlayerThread(s.accept());
			System.err.println("New player!");
			players[i]=player;
			// broadcast latest list of players
			String name = player.getPlayerName();
			playerNames[i]=name;
			for(int j=0; j<=i; j++){
				players[j].setPlayers(playerNames);
			}
		}
		// start game
		status="In progress";
		scores=new Integer[nPlayers];
		for(int i=0; i<scores.length; i++){
			scores[i]=0;
		}
		while(true){
			
			// next round: start 10 second timer
			
			for(int i=0; i<nPlayers; i++){
				players[i].nextRound(scores);
				players[i].getMove();
			}
			Thread.sleep(20000);
			// get moves and figure out points
			int maxScore=0;
			int maxScorePlayer=0;
			for(int i=0; i<nPlayers; i++){
				if(!players[i].active){
					continue;
				}
				for(int j=0; j<nPlayers; j++){
					if(!players[j].active){
						continue;
					}
					scores[i]+=compareMoves(players[i].lastMove,players[j].lastMove);
				}
				maxScore=Math.max(maxScore,scores[i]);
				if(maxScore==scores[i]){
					maxScorePlayer=i;
				}
			}
			// drop losers
			System.err.println("Dropping losers");
			int activeCount=nPlayers;
			for(int i=0; i<nPlayers; i++){
				PlayerThread player=players[i];
				if(scores[i]<maxScore){
					// loser
					player.active=false;
					player.drop(scores);
					activeCount--;
				}
			}
			
			// check win condition
			if(activeCount==1){
				// declare winner
				System.err.println("Declaring winner");
				for(int i=0; i<nPlayers; i++){
					players[i].win(maxScorePlayer);
					players[i].stop();
				}
				return;
			}
		}
	}
	/**
	 * Compares two moves, returning 1 if left beats right, 0 if not.
	 * @param left
	 * @param right
	 * @return
	 */
	public int compareMoves(String left, String right) {
		List<String> beatee=beats.get(left);
		if(beatee.contains(right)){
			// beat
			return 1;
		}else{
			// not a beat
			return 0;
		}
	}
	/**
	 * @param args: desired number of players, port to listen on, and IP to listen on.
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws NumberFormatException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException, InterruptedException {
		// read in command line params
		int nPlayers=Integer.valueOf(args[0]);
		int port=Integer.valueOf(args[1]);
		// specify host IP so Java knows which device (eth0, wifi) to bind to
		String ip=args[2];
		// start server
		new Server(nPlayers, port, ip);
	}

}
