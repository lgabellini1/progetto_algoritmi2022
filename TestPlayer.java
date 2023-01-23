/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mnkgame;

import java.util.*;

/**
 *
 * @author asus
 */
public class TestPlayer implements MNKPlayer {
    private Random rand;
    private MNKBoard B;
    private MNKGameState myWin;
    private MNKGameState yourWin;
    private int TIMEOUT;
    private boolean amIFirstPlayer;
    
    public TestPlayer() {
    }
    
    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        B = new MNKBoard(M, N, K);
        myWin = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        amIFirstPlayer = false;
    }
    
    public boolean inTable(int i, int j) {
        if((i >= 0 && i < B.M) && (j >= 0 && j < B.N)) {
            return true;
        }
        return false;
    }
    
    public List<MNKCell> nearMoves(MNKCell[] FC, MNKCell[] MC) {
        List<MNKCell> nearYourMoves = new ArrayList<>();
        int i;
        if(amIFirstPlayer) {
            i=1;
        } else {
            i=0;
        }
        for(; i<MC.length; i+=2) {
            
            MNKCell c = MC[i];
            if(inTable(c.i+1, c.j+1) && B.B[c.i+1][c.j+1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i+1, c.j+1));
            }
            if(inTable(c.i+1, c.j) && B.B[c.i+1][c.j] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i+1, c.j));
            }
            if(inTable(c.i, c.j+1) && B.B[c.i][c.j+1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i, c.j+1));
            }
            if(inTable(c.i-1, c.j-1) && B.B[c.i-1][c.j-1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i-1, c.j-1));
            }
            if(inTable(c.i-1, c.j) && B.B[c.i-1][c.j] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i-1, c.j));
            }
            if(inTable(c.i, c.j-1) && B.B[c.i][c.j-1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i, c.j-1));
            }
            if(inTable(c.i-1, c.j+1) && B.B[c.i-1][c.j+1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i-1, c.j+1));
            }
            if(inTable(c.i+1, c.j-1) && B.B[c.i+1][c.j-1] == MNKCellState.FREE) {
                nearYourMoves.add(new MNKCell(c.i+1, c.j-1));
            }
        }
        /*
        for(int k=0; k<nearYourMoves.size(); k++) {
            System.out.println(nearYourMoves.get(k));
        }*/
        return nearYourMoves;
    }
    
    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        if(MC.length == 0) {
            amIFirstPlayer = true;
        }
        if (MC.length > 0) {
            MNKCell c = MC[MC.length - 1]; // Recover the last move from MC
            B.markCell(c.i, c.j);         // Save the last move in the local MNKBoard
        }
        
        List<MNKCell> move = nearMoves(FC,MC);
        
        
        
        MNKCell c;
        if(move.isEmpty()) {
            c = FC[rand.nextInt(FC.length)];
        } else {
            int pos = rand.nextInt(move.size());
            c = move.get(pos);
        }
        B.markCell(c.i, c.j);
        //System.out.println("mossa scelta: "+c);
        return c;
    }

    public String playerName() {
        return "Test";
    }
    
    
}
