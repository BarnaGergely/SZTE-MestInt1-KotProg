///Nicknevem,Vezeteknev.Keresztnev@stud.u-szeged.hu

import java.util.LinkedList;
import java.util.Random;

import game.racetrack.Direction;
import game.racetrack.RaceTrackPlayer;
import game.racetrack.utils.Cell;
import game.racetrack.utils.Coin;
import game.racetrack.utils.PathCell;
import game.racetrack.utils.PlayerState;

import static game.racetrack.RaceTrackGame.BFS;
import static game.racetrack.RaceTrackGame.direction;
import static java.lang.Math.abs;

/* A projekt a racetrack-11-20-as verzióját integrálja

    * Be van állítva a Run Config, így pöccre indul grafikus
      felülettel az alkalmazás és működik a debug is
        * Alapértelmezetten az Agent.Java indul el

    * src mappa: saját osztályok helye
    * docs mappa: tanár által adott leírások
    * libs mappa: game_engine.jar helye

További segédletek a kötelező programhoz: https://barnagergely.github.io/kalkulus/egy%C3%A9b/mestint1/
*/
public class AgentMinimal extends RaceTrackPlayer {

    LinkedList<PathCell> path = new LinkedList<PathCell>();

    public AgentMinimal(PlayerState state, Random random, int[][] track, Coin[] coins, int color) {
        super(state, random, track, coins, color);

        path = (LinkedList<PathCell>) BFS(state.i, state.j, track);
        path.removeFirst(); // a kiinduló pozíció feleslegesen kerül bele
    }

    @Override
    public Direction getDirection(long remainingTime) {
        Cell current = new Cell(state.i, state.j);
        Cell next = path.getFirst();

        Direction speedChange = direction(current, next);
        int nextVi = speedChange.i-state.vi;
        int NextVj = speedChange.j-state.vj;

        int speedLimit = 1;
        boolean speedLimitExceeded = false;
        if (abs(nextVi) > speedLimit) {
            nextVi = nextVi > 0 ? speedLimit : -speedLimit;
            speedLimitExceeded = true;
        }
        if (abs(NextVj) > speedLimit) {
            NextVj = NextVj > 0 ? speedLimit : -speedLimit;
            speedLimitExceeded = true;
        }
        if (!speedLimitExceeded) path.removeFirst();

        return new Direction(nextVi, NextVj);
    }
}