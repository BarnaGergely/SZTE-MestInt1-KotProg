///Flash,barna.gergely@stud.u-szeged.hu

import game.racetrack.Direction;
import game.racetrack.RaceTrackPlayer;
import game.racetrack.utils.Cell;
import game.racetrack.utils.Coin;
import game.racetrack.utils.PlayerState;

import java.util.*;

import static game.racetrack.RaceTrackGame.*;

public class AgentSimple extends RaceTrackPlayer {
    LinkedList<Cell> path = new LinkedList<>();
    double gCostWeight = 1;

    public AgentSimple(PlayerState state, Random random, int[][] track, Coin[] coins, int color) {
        super(state, random, track, coins, color);

        Cell start = new Cell(state.i, state.j);
        path.addAll( FindPath(start, coins[0])); // elmegy az 1. coin-hoz
        path.addAll( FindPath(coins[0], findFinish())); // az első coin-tól meg a célba
    }

    @Override
    public Direction getDirection(long remainingTime) {
        if (path != null && !path.isEmpty()) {
            Cell nextStep = path.getFirst();
            path.removeFirst();
            return moveToCell(nextStep);
        } else {
            return null;
        }
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
        startNode.fCost = startNode.gCost + startNode.hCost;
        open.add(startNode);    // Hozzá adjuk a kiindulási Node-ot a nyitott halmazhoz

        // Amíg van elem a nyitott halmazban
        while (!open.isEmpty()) {
            Node current = open.poll(); // az open első eleme a legkisebb, így ez a legjobb ismert lehetséges lépés
            closed.add(current);

            // ha ráfutottunk egy cél cellára, leáll az algoritmus
            if (current.cell.same(finishCell))
                return reconstructPathPlayerState(current);

            List<Node> neighbours = getPossibleNodes(current);
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

    /// A megadott algoritmussal kiszámolja az egyenes út hosszát a két pont között
    double calculateDistance(Cell a, Cell b) {
        return euclideanDistance(a, b);
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

    /// Az adott Node-hoz a következő lépésben elérhető cellákat adja vissza
    List<Node> getPossibleNodes(Node node) {
        List<Node> neighbours = new LinkedList<>();

        for (Direction direction : DIRECTIONS) {
            int i = node.cell.i + direction.i;
            int j = node.cell.j + direction.j;
            Node neighbor = new Node(i, j, node);

            // sebesség vektor számítások a szomszéd validáslásához
            int parentVi = 0;
            int parentVj = 0;
            int myVi = 0;
            int myVj = 0;
            if (neighbor.parent == null) {

            } else if (neighbor.parent.parent == null) {
                myVi = neighbor.cell.i - neighbor.parent.cell.i;
                myVj = neighbor.cell.j - neighbor.parent.cell.j;
            } else {
                parentVi = neighbor.parent.cell.i - neighbor.parent.parent.cell.i;
                parentVj = neighbor.parent.cell.j - neighbor.parent.parent.cell.j;
                myVi = neighbor.cell.i - neighbor.parent.cell.i;
                myVj = neighbor.cell.j - neighbor.parent.cell.j;
            }

            // A falak és az eredeti cella nem kerül a listába
            if (!isNotWall(i, j, track) || node.equals(neighbor)) continue;

            // Azok a szomszédok, amikbe a jelenlegi sebességünk miatt nem lehet eljutni, kiszűrése
            if (1 < Math.abs(myVi - parentVi) || 1 < Math.abs(myVj - parentVj)) continue;

            neighbours.add(neighbor);
        }
        return neighbours;
    }

    boolean traversable(Cell from, Cell to) {
        return true;
    }

    private LinkedList<Cell> reconstructPathPlayerState(Node node) {
        LinkedList<Cell> pathRec = new LinkedList<>();

        int vi = node.cell.i - node.parent.cell.i;
        int vj = node.cell.j - node.parent.cell.j;
        if (vi != 0 || vj != 0) {
            node = new Node(node.cell, node);
        }

        while (node.parent != null) {
            pathRec.addFirst(node.cell);
            node = node.parent;
        }
        return pathRec;
    }

    private Direction moveToCell(Cell cell) {
        // Player i + vi + x = cell.i
        int i = cell.i - state.i - state.vi;
        int j = cell.j - state.j - state.vj;
        if (i > 1 || i < -1 || j > 1 || j < -1)
            return null;
        return new Direction(i, j);
    }
}