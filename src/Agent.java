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

public class Agent extends RaceTrackPlayer {
    LinkedList<Cell> path = new LinkedList<>();

    List<Coin> notCollectedCoins = new ArrayList<Coin>();

    double gCostWeight = 1;

    public Agent(PlayerState state, Random random, int[][] track, Coin[] coins, int color) {
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
        LinkedList<Cell> tempPath = FindPath(start, finish);
        assert tempPath != null;
        path = tempPath;
    }

    private static class Node implements Comparable<Node> {
        Cell cell;
        Node parent;

        /// Sum of costs
        private double fCost = Double.MAX_VALUE;

        double hCost = Double.MAX_VALUE;

        /// Distance in the path from starting node to this node
        double gCost = Double.MAX_VALUE;

        /// Distance between parent and current cell
        double wCost = 1;

        double cCost = 0;

        public Node(Cell cell, Node parent) {
            this.cell = cell;
            this.parent = parent;
        }

        public Node(int i, int j, Node parent) {
            this(new Cell(i, j), parent);
        }

        @Override
        public int compareTo(Node o) {
            return Double.compare(fCost, o.fCost);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return cell.same(node.cell);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cell);
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
        return octileDistance(a, b);
    }

    // Érme költség számoló
    double cCost(Cell cell) {
        return cCostSumAvaliable(cell);
    }

    /// Legközelebbi érme költsége
    double cCostNearest(Cell cell) {
        double nearestCoinIndex = nearestCoinIndex(cell);
        if (0 <= nearestCoinIndex) {
            // a legközelebbi coin értéke osztva a (távolságával + 1) - azért kell a plusz egy hogy sose lehessen 0 a távolság és
            // csak akkor adjon max pontot, ha konkrátan rálépünk a coin-ra
            // TODO: ha elég átmenni a coin felett, újra át kell kondlni ezt
            return notCollectedCoins.get(nearestCoinIndex).value / (calculateDistance(cell, notCollectedCoins.get(nearestCoinIndex)) + 1);
        }
        return 0;
    }

        /// Összes még nem érintett érme távolság szerint súlyozott költsége összeadva
        double cCostSumAvaliable(Cell cell) {
            double cCost = 0;
            
            for(Coin coin : notCollectedCoins) {
                // a legközelebbi coin értéke osztva a (távolságával + 1) - azért kell a plusz egy hogy sose lehessen 0 a távolság és
                // csak akkor adjon max pontot, ha konkrátan rálépünk a coin-ra
                cCost +=  coin.value / (calculateDistance(cell, coin.cell) + 1);
            }
            return cCost;
        }

    /// Egyetlen lépés költsége: Hagyományosan a szülő és a gyerek távolsága,
    // de a feladat szerint konstans 1 minden lépés költsége, a méretétől függetlenül
    private double wCost(Node node) {
        //return calculateDistance(node.cell, node.parent.cell);
        return 1;
    }

    /// A cella és a cél távolsága
    private double hCost(Cell cell, Cell finish) {
        return calculateDistance(cell, finish);
    }

    /// Költségek összege
    // TODO: ezt újra használatba venni
    private double fCost(Node node, Cell finish) {
        double hCost = hCost(node.cell, finish);
        return node.gCost + hCost;
    }

    /// Az adott cellából a következő lépésben elérhető cellákat adja vissza
    List<Node> getNeighbours(Node node) {
        List<Node> neighbours = new LinkedList<>();

        for (Direction direction : DIRECTIONS) {
            int i = node.cell.i + direction.i;
            int j = node.cell.j + direction.j;
            Node neighbor = new Node(i, j, node);

            // A falak és az eredeti cella nem kerül a listába
            if (isNotWall(i, j, track) && !node.equals(neighbor)) neighbours.add(neighbor);
        }
        return neighbours;
    }

    boolean traversable(Cell from, Cell to) {
        return true;
    }

    /// A* útkereső algoritmussal keres hatékony utat a két cella között
    private LinkedList<Cell> FindPath(Cell startCell, Cell finishCell) {
        /// A nyitott Node-ok tároljója, fCost szerint növekvő sorrendberendezve.
        PriorityQueue<Node> open = new PriorityQueue<>();
        /// Már lezárt Node-ok halmaza. Nem kell rendezni, de gyors keresésre, beszúrásra és törlésre van benne szükség
        LinkedList<Node> closed = new LinkedList<>();

        /// Kiidulási Node
        Node startNode = new Node(startCell, null);
        startNode.gCost = 0;
        startNode.hCost = hCost(startNode.cell, finishCell);
        startNode.cCost = cCost(startNode.cell);
        startNode.fCost = startNode.gCost + startNode.hCost + startNode.cCost;
        open.add(startNode);    // Hozzá adjuk a kiindulási Node-ot a nyitott halmazhoz

        // Amíg van elem a nyitott halmazban
        while (!open.isEmpty()) {
            Node current = open.poll(); // az open első eleme a legkisebb, így ez a legjobb ismert lehetséges lépés
            closed.add(current);

            // ha ráfutottunk egy cél cellára, leáll az algoritmus
            if (isFinnish(current.cell))
                return reconstructPathPlayerState(current);

            // Ha ráfutottunk egy Coin-ra töröljük a meg nem érintett Coin-ok listájából
            if (isCoin(current.cell)) {
                removeCoin(current.cell);
                /* TODO: kiprobálni
                ötlet: ha velvesz egy coin-t törli a már bejárt cellák listáját
                closed.clear();
                */
            }


            List<Node> neighbours = getNeighbours(current);
            // Végig járjuk a lehetséges lépéseket, ahová a következő körben léphetünk
            for (Node neighbour : neighbours) {

                // Ha már kizártuk vagy el sem lehet menni az adott szomszédba, ugorjuk át
                // ezt be lehetne integrálni a getNeighbours metódusba
                if (!traversable(current.cell, neighbour.cell) || closed.contains(neighbour)) {
                    continue;
                }

                // A szomszéd új g értéke
                double nextGCost = current.gCost + wCost(neighbour);

                // check if the neighbor has not been inspected yet, or
                // can be reached with smaller cost from the current node
                if (!open.contains(neighbour) || nextGCost < neighbour.gCost) {
                    neighbour.gCost = nextGCost; // + neighbour.cCost;
                    neighbour.hCost = hCost(neighbour.cell, finishCell);
                    neighbour.cCost = cCost(neighbour.cell);
                    neighbour.fCost = (gCostWeight * neighbour.gCost) + neighbour.hCost + neighbour.cCost;

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

    private LinkedList<Cell> reconstructPathPlayerState(Node node) {
        LinkedList<Node> debugPath = new LinkedList<>();
        LinkedList<Cell> pathRec = new LinkedList<>();

        while (node.parent != null) {
            pathRec.addFirst(node.cell);
            debugPath.addFirst(node);
            node = node.parent;
        }
        return pathRec;
    }

    private Direction moveToCell(Cell cell) {
        // Player i + vi + x = cell.i
        Direction direction = new Direction(cell.i - state.i - state.vi, cell.j - state.j - state.vj);
        return direction;
    }

    @Override
    public Direction getDirection(long remainingTime) {
        if (path != null && !path.isEmpty()) {
            Cell nextStep = path.getFirst();
            path.removeFirst();
            return moveToCell(nextStep);
        } else {
            return new Direction(0, 0);
        }
    }
}