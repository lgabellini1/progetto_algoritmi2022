/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mnkgame;

import java.util.Random;
import java.util.Arrays;

/**
 *
 * @author Andrea
 */
public class MyPlayer_Giacomo implements MNKPlayer {

    private Random rand;
    private MNKBoard B;
    private MNKGameState myWin;
    private MNKGameState yourWin;
    private int TIMEOUT;

    /**
     * Default empty constructor
     */
    public MyPlayer_Giacomo() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        B = new MNKBoard(M, N, K);
        myWin = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        yourWin = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        TIMEOUT = timeout_in_secs;
    }

    /**
     * Selects a random cell in <code>FC</code>
     */
    private int evaluate(MNKCell move) {
        int eval = 0;
        if (B.markCell(move.i, move.j) == myWin) {
            B.unmarkCell();
            if (myWin == MNKGameState.WINP1) {
                eval = -1;
            } else {
                eval = 1;
            }
        } else if (B.markCell(move.i, move.j) == yourWin) {
            B.unmarkCell();
            if (myWin == MNKGameState.WINP1) {
                eval = 1;
            } else {
                eval = -1;
            }
        }
        return eval;
    }

    private MNKCell[] shiftArray(MNKCell[] FC) {
        MNKCell[] shifted = new MNKCell[FC.length - 1];
        for (int k = 1; k < FC.length; k++) {
            shifted[k - 1] = FC[k];
            System.out.println("l'array shifted è: " + shifted[k - 1]);
        }
        return shifted;
    }

    // FC = celle libere, MC = celle occupate
    private int alphaBeta(MNKCell[] FC, boolean playerA, int alpha, int beta) {
        int eval;
        System.out.println("prova1");
        for (MNKCell d : FC) {
            System.out.print(d);
        }
        System.out.println(" prova2");

        if (FC.length == 1) { // se ho una sola mossa disponibile (quindi una foglia) la valuto
            // return evaluate(FC[0]); commentato
            eval = evaluate(FC[0]);
        } else if (playerA == true) { // altrimenti mi calcolo il valore del ramo
            eval = -99999;
            for (MNKCell d : FC) {
                System.out.println("giocatore che maxa");
                B.markCell(d.i, d.j);
                eval = Math.max(eval, alphaBeta(shiftArray(FC), false, alpha, beta));
                alpha = Math.max(eval, alpha);
                if (beta <= alpha) {
                    break;
                }
            }
        } else {
            eval = 99999;
            for (MNKCell d : FC) {
                System.out.println("giocatore che minimizza");
                B.markCell(d.i, d.j);
                eval = Math.min(eval, alphaBeta(shiftArray(FC), true, alpha, beta));
                beta = Math.min(eval, beta);
                if (beta <= alpha) {
                    break;
                }
            }
        }
        System.out.println("RESTITUISCO EVAL: " + eval);
        return eval;
    }

    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {

        /*
         * if (MC.length > 0) {
         * MNKCell c = MC[MC.length - 1]; // Recover the last move from MC
         * B.markCell(c.i, c.j); // Save the last move in the local MNKBoard
         * System.out.println("sono qui");
         * } else {
         * 
         * }
         */
        long start = System.currentTimeMillis();
        int value;
        // MNKCell c = MC[MC.length - 1]; // Recover the last move from MC
        // B.markCell(c.i, c.j);
        System.out.println("Entro nel SelectCell");
        if (myWin == MNKGameState.WINP1) { // minimizza
            System.out.println("player che minimizza");
            value = alphaBeta(FC, false, -1, 1);

        } else { // massimizza
            System.out.println("player che massimizza");
            value = alphaBeta(FC, true, -1, 1);

        }
        System.out.println("ciao");

        if (value == 1) {
            System.out.println("Mossa migliore trovata! " + FC[0]);
            B.markCell(FC[0].i, FC[0].j);
            return FC[0];
        }

        /*
         * for (MNKCell d : FC) {
         * // If time is running out, select a random cell
         * if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0))
         * {
         * MNKCell c = FC[rand.nextInt(FC.length)];
         * B.markCell(c.i, c.j);
         * return c;
         * } else {
         * value = alphaBeta(FC, true, 0, 0);
         * if(value == 1) {
         * return d;
         * }
         * }
         * }
         */
        // No win or loss, return the randomly selected move
        System.out.println("Mossa migliore NON trovata!");
        int pos = rand.nextInt(FC.length);
        MNKCell c = FC[pos]; // random move
        B.markCell(c.i, c.j); // mark the random position
        return c;
    }

    public String playerName() {
        return "MyPlayer";
    }
}
