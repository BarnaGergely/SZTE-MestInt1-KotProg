///Flash,barna.gergely@stud.u-szeged.hu

import game.racetrack.Direction;
import game.racetrack.RaceTrackPlayer;
import game.racetrack.utils.Cell;
import game.racetrack.utils.Coin;
import game.racetrack.utils.PlayerState;

import java.util.*;

import static game.racetrack.RaceTrackGame.*;

/* A projekt a racetrack-10-30-as verzióját integrálja

    * Be van állítva a Run Config, így pöccre indul grafikus
      felülettel az alkalmazás és működik a debug is
        * Alapértelmezetten az Agent.Java indul el

    * src mappa: saját osztályok helye
    * docs mappa: tanár által adott feladat,leírások
    * libs mappa: game_engine.jar helye

További segédletek a kötelező programhoz: https://barnagergely.github.io/kalkulus/egy%C3%A9b/mestint1/
*/

/* Hasznos linkek:
 * Ranglista: https://www.inf.u-szeged.hu/~etasnadi/aiproj-2023/
 * Saját tesztjeim: https://www.inf.u-szeged.hu/~etasnadi/aiproj-2023/index.php?q=zK2toJ6Qk6vMmppoY86mnGCsXqewmaDIlZKgqpLCnafWqKNdlpGWbJSpo56rxqybYW5mmWmXbptnl5tmxsKVbJpxbmjFxZpqx21plmiVmm5pZg
 * Beadás: https://www.coosp.etr.u-szeged.hu/Scene-743010/Task-2924053
 *
 * Érhető és hatékony A* megvalósítás Javascript-ben: https://github.com/qiao/PathFinding.js/blob/master/src/finders/AStarFinder.js
 * A* Visulaizer: https://qiao.github.io/PathFinding.js/visual/
 * Vacak Java megvalósítás: https://stackabuse.com/graphs-in-java-a-star-algorithm/
 * Egész jó Java megvalósítás: https://github.com/psikoi/AStar-Pathfinding/tree/master
 *
 * Jó algoritmus: https://theory.stanford.edu/~amitp/GameProgramming/ImplementationNotes.html
 *
 * Nagyon jó algoritmus: https://www.sciencedirect.com/science/article/pii/S1000936116301182#:~:text=The%20octile%20distance%20is%20used,store%20all%20the%20grid%20points.
 * Octile Distance implementation: https://stackoverflow.com/questions/32622478/a-search-grid-8-directions-octile-distance-as-heuristic-not-finding-the-dir
 * Heurisztikák és octile distance: https://theory.stanford.edu/~amitp/GameProgramming/Heuristics.html
 *
 *
 * */

/*TODO:
 + Javítani a kód minőségét
 + Rájönni miért lassú
 * Optimalizálni a heurisztikákat
    * Megnézni a videóban milyen gyorsítási lehetőséget ajánlottak
    * Hozzá adni Coin heurisztikát
 * Optimalizálni a listák tipusait
 * Vektorizálni
 *
 *
 * */

public class AgentVector extends RaceTrackPlayer {
    LinkedList<Cell> path = new LinkedList<>();

    List<Coin> notCollectedCoins = new ArrayList<Coin>();

    double gCostWeight = 1;

    public AgentVector(PlayerState state, Random random, int[][] track, Coin[] coins, int color) {
        super(state, random, track, coins, color);
        for (Coin coin : coins) {
            assert notCollectedCoins != null;
            notCollectedCoins.add(coin);
        }

        Cell start = new Cell(state.i, state.j);
        Cell finish;
        /*
        while (!notCollectedCoins.isEmpty()) {
            finish = notCollectedCoins.get(nearestCoinIndex(start));
            path.addAll(FindPath(start, finish));
            start = finish;
        }
        */

        finish = findFinish();
        LinkedList<Cell> tempPath = FindPath(new PlayerState(start.i, start.j, 0, 0), finish);
        if (isNeitherWall(tempPath, track)) {
            assert tempPath != null;
            path = tempPath;
        }
    }

    private static class NodeState implements Comparable<NodeState> {
        PlayerState state;
        NodeState parent;

        /// Sum of costs
        private double fCost = Double.MAX_VALUE;

        double hCost = Double.MAX_VALUE;

        /// Distance in the path from starting node to this node
        double gCost = Double.MAX_VALUE;

        /// Distance between parent and current cell
        double wCost = 1;

        public NodeState(PlayerState state, NodeState parent) {
            this.state = state;
            this.parent = parent;
        }

        public NodeState(int i, int j, int vi, int vj, NodeState parent) {
            this(new PlayerState(i, j, vi, vj), parent);
        }

        @Override
        public int compareTo(NodeState o) {
            return Double.compare(fCost, o.fCost);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NodeState nodeState = (NodeState) o;
            return state.same(nodeState.state);
        }

        @Override
        public int hashCode() {
            return Objects.hash(state);
        }
    }

    private boolean isFinnish(Cell cell) {
        return mask(track[cell.i][cell.j], FINISH);
    }

    /// Find a finish cell
    private Cell findFinish() {
        for (int i = 0; i < track.length; i++) {
            for (int j = 0; j < track[i].length; j++) {
                if (mask(track[i][j], FINISH)) {
                    return new Cell(i, j);
                }
            }
        }
        return null;
    }

    private boolean isCoin(Cell cell) {
        for (Coin coin : notCollectedCoins) {
            if (coin.same(cell)) return true;
        }
        return false;
    }

    private int findCoinIndex(Cell cell) {
        int i = 0;
        for (i = 0; i < notCollectedCoins.size(); i++) {
            if (notCollectedCoins.get(i).same(cell)) {
                return i;
            }
        }
        return -1;
    }

    private boolean removeCoin(Cell cell) {

        for (int i = 0; i < notCollectedCoins.size(); i++) {
            if (notCollectedCoins.get(i).same(cell)) {
                notCollectedCoins.remove(i);
                return true;
            }

        }
        return false;
    }

    int nearestCoinIndex(Cell cell) {
        if (notCollectedCoins != null && !notCollectedCoins.isEmpty()) {
            double nearestDistance = Integer.MAX_VALUE;
            int nearestIndex = 0;
            for (int i = 0; i < notCollectedCoins.size(); i++) {
                double currentDistance = calculateDistance(cell, notCollectedCoins.get(i));
                if (nearestDistance > currentDistance) {
                    nearestDistance = currentDistance;
                    nearestIndex = i;
                }
            }
            return nearestIndex;
        }
        return -1;
    }


    /// calculate octile distance: https://theory.stanford.edu/~amitp/GameProgramming/Heuristics.html
    double octileDistance(Cell a, Cell b) {
        int C = 1;
        double D = Math.sqrt(2);
        int di = Math.abs(a.i - b.i);
        int dj = Math.abs(a.j - b.j);
        double value = C * (di + dj) + (D - 2 * C) * Math.min(di, dj);
        return value;
    }

    /// chebyshev octile distance: https://theory.stanford.edu/~amitp/GameProgramming/Heuristics.html
    double chebyshevDistance(Cell a, Cell b) {
        int C = 1;
        double D = 1;
        int di = Math.abs(a.i - b.i);
        int dj = Math.abs(a.j - b.j);
        double value = C * (di + dj) + (D - 2 * C) * Math.min(di, dj);
        return value;
    }

    /// A megadott algoritmussal kiszámolja az egyenes út hosszát a két pont között
    double calculateDistance(Cell a, Cell b) {
        return euclideanDistance(a, b);
    }

    /// Egyetlen lépés költsége: Hagyományosan a szülő és a gyerek távolsága,
    // de a feladat szerint konstans 1 minden lépés költsége, a méretétől függetlenül
    private double wCost(NodeState nodeState) {
        //return calculateDistance(node.cell, node.parent.cell);
        return 1;
    }

    /// A cella és a cél távolsága
    private double hCost(Cell cell, Cell finish) {
        return calculateDistance(cell, finish);
    }

    /// Költségek összege
    // TODO: ezt újra használatba venni
    private double fCost(NodeState nodeState, Cell finish) {
        double hCost = hCost(toCell(nodeState.state), finish);
        return nodeState.gCost + hCost;
    }

    /// Az adott cellából a következő lépésben elérhető cellákat adja vissza
    List<NodeState> getNeighbours(NodeState nodeState) {
        List<NodeState> neighbours = new LinkedList<>();

        for (Direction direction : DIRECTIONS) {
            int vi = nodeState.state.vi + direction.i;
            if (vi > 1) {
                vi = 1;
            } else if (vi < -1) {
                vi = -1;
            }
            int vj = nodeState.state.vj + direction.j;
            if (vj > 1) {
                vj = 1;
            } else if (vj < -1) {
                vj = -1;
            }
            int i = nodeState.state.i + vi;
            int j = nodeState.state.j + vj;

            NodeState neighbor = new NodeState(i, j, vi, vj, nodeState);

            // A falak és az eredeti cella nem kerül a listába
            if (!nodeState.equals(neighbor) && isNotWall(i, j, track) && i < track.length && j < track[0].length)
                neighbours.add(neighbor);
        }
        return neighbours;
    }

    boolean traversable(Cell from, Cell to) {
        return isNeitherWall(lineCrossing(from, to), track);
    }

    /// A* útkereső algoritmussal keres hatékony utat a két cella között
    private LinkedList<Cell> FindPath(PlayerState startState, Cell finishCell) {
        /// A nyitott Node-ok tároljója, fCost szerint növekvő sorrendberendezve.
        PriorityQueue<NodeState> open = new PriorityQueue<>();
        /// Már lezárt Node-ok halmaza. Nem kell rendezni, de gyors keresésre, beszúrásra és törlésre van benne szükség
        LinkedList<NodeState> closed = new LinkedList<>();

        /// Kiidulási Node
        NodeState startNodeState = new NodeState(startState, null);
        startNodeState.gCost = 0;
        startNodeState.hCost = hCost(toCell(startNodeState.state), finishCell);
        startNodeState.fCost = startNodeState.gCost + startNodeState.hCost;
        open.add(startNodeState);    // Hozzá adjuk a kiindulási Node-ot a nyitott halmazhoz

        // Amíg van elem a nyitott halmazban
        while (!open.isEmpty()) {
            NodeState current = open.poll(); // az open első eleme a legkisebb, így ez a legjobb ismert lehetséges lépés
            closed.add(current);

            // ha ráfutottunk egy cél cellára, leáll az algoritmus
            if (toCell(current.state).same(finishCell))
                return reconstructPathPlayerState(current);

            List<NodeState> neighbours = getNeighbours(current);
            // Végig járjuk a lehetséges lépéseket, ahová a következő körben léphetünk
            for (NodeState neighbour : neighbours) {

                // Ha már kizártuk vagy el sem lehet menni az adott szomszédba, ugorjuk át
                // ezt be lehetne integrálni a getNeighbours metódusba
                if (!traversable(toCell(current.state), toCell(neighbour.state)) || closed.contains(neighbour)) {
                    continue;
                }

                // A szomszéd új g értéke
                double nextGCost = current.gCost + wCost(neighbour);

                // check if the neighbor has not been inspected yet, or
                // can be reached with smaller cost from the current node
                if (!open.contains(neighbour) || nextGCost < neighbour.gCost) {
                    neighbour.gCost = nextGCost; // + neighbour.cCost;
                    neighbour.hCost = hCost(toCell(neighbour.state), finishCell);
                    neighbour.fCost = (gCostWeight * neighbour.gCost) + neighbour.hCost;

                    neighbour.parent = current;

                    /* Ha nem frissítem a nyílt halmazban lévő elemet, jobb az eredmény
                    if (open.contains(neighbour))
                        open.remove(neighbour);
                    open.add(neighbour);
                    */

                    if (!open.contains(neighbour))
                        open.add(neighbour);
                }

            }
        }
        // nem találtunk útvonalat
        return null;
    }

    private LinkedList<Cell> reconstructPathPlayerState(NodeState nodeState) {
        LinkedList<NodeState> debugPath = new LinkedList<>();
        LinkedList<Cell> pathRec = new LinkedList<>();

        while (nodeState != null) {
            pathRec.addFirst(toCell(nodeState.state));
            debugPath.addFirst(nodeState);
            nodeState = nodeState.parent;
        }
        return pathRec;
    }

    private Direction moveToCell(Cell cell) {
        // Player i + vi + x = cell.i
        int vi = cell.i - (state.i + state.vi);
        int vj = cell.j - (state.j + state.vj);
        if (vi > 1 || vi < -1 || vj > 1 || vj < -1)
            return null;
        Direction direction = new Direction(vi, vj);
        return direction;
    }

    @Override
    public Direction getDirection(long remainingTime) {
        if (path != null && !path.isEmpty()) {
            Cell nextStep = path.getFirst();
            path.removeFirst();
            if (!traversable(new Cell(state.i, state.j), nextStep))
                return null;
            return moveToCell(nextStep);
        } else {
            return null;
        }
    }
}