import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  Random rand;

  // Default constructor
  LightEmAll(int width, int height) {
    this.width = width;
    this.height = height;
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.nodes = new ArrayList<GamePiece>();
    this.rand = new Random();
    this.radius = 8;
    this.generateBoard();
  }

  // Constructor with given Random seed
  LightEmAll(int width, int height, Random rand) {
    this.width = width;
    this.height = height;
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.nodes = new ArrayList<GamePiece>();
    this.rand = rand;
    this.radius = 8;
    generateBoard();
  }

  // Generate the board
  void generateBoard() {
    boolean ps;
    for (int col = 0; col < this.width; col++) {
      this.board.add(new ArrayList<GamePiece>());
      for (int row = 0; row < this.height; row++) {
        ps = false;
        if (col == 0 && row == 0) {
          ps = true;
          this.powerCol = 0;
          this.powerRow = 0;
        }
        GamePiece p = new GamePiece(row, col, false, false, false, false, ps);
        this.board.get(col).add(p);
        this.nodes.add(p);
      }
    }

    ArrayList<Edge> allEdges = getEdges();
    this.heapSort(allEdges);
    this.kruskals(allEdges);
    this.connectPieces();
    this.randomizeBoard();
    this.linkPieces();
    this.checkLit();

  }

  // Return an ArrayList of all possible edges
  ArrayList<Edge> getEdges() {
    ArrayList<Edge> allEdges = new ArrayList<Edge>();
    GamePiece from;
    GamePiece to;
    for (int col = 0; col < this.width; col++) {
      for (int row = 0; row < this.height; row++) {
        if (this.height - 1 != row) { // not the bottom row
          from = getPiece(col, row);
          to = getPiece(col, row + 1);
          allEdges.add(new Edge(from, to, this.rand.nextInt()));
        }
        if (this.width - col > 1) { // not the rightmost column
          from = getPiece(col, row);
          to = getPiece(col + 1, row);
          allEdges.add(new Edge(from, to, this.rand.nextInt()));
        }

      }
    }
    return allEdges;
  }

  // Sort the given ArrayList<Edge> using heaps
  public void heapSort(ArrayList<Edge> edges) {
    for (int i = (edges.size() - 1) / 2; i >= 0; i--) {
      this.downheap(edges, i, edges.size());
    }
    for (int i = (edges.size() - 1); i >= 0; i--) {
      this.removeMax(edges, i);
    }
  }

  // Recursively swap invalid parent/children positions until
  // the heap is valid
  void downheap(ArrayList<Edge> edges, int idx, int heapSize) {
    int leftIdx = 2 * idx + 1;
    int rightIdx = 2 * idx + 2;

    if (leftIdx >= heapSize) {
      // neither left nor right exist
    }
    // only left exists
    else if (rightIdx >= heapSize) {
      if (edges.get(idx).weight < edges.get(leftIdx).weight) {
        Edge prevEdge = edges.get(idx);
        edges.set(idx, edges.get(leftIdx));
        edges.set(leftIdx, prevEdge);

        this.downheap(edges, leftIdx, heapSize);
      }
    }

    // both left and right exist
    else {
      if (edges.get(idx).weight < edges.get(leftIdx).weight
          || edges.get(idx).weight < edges.get(rightIdx).weight) {
        int biggestIdx;
        if (edges.get(leftIdx).weight < edges.get(rightIdx).weight) {
          biggestIdx = rightIdx;
        }
        else {
          biggestIdx = leftIdx;
        }
        Edge prevEdge = edges.get(idx);
        edges.set(idx, edges.get(biggestIdx));
        edges.set(biggestIdx, prevEdge);
        downheap(edges, biggestIdx, heapSize);
      }
    }
  }

  // Removes the max from the heap
  void removeMax(ArrayList<Edge> edges, int idx) {
    Edge prevEdge = edges.get(0);
    edges.set(0, edges.get(idx));
    edges.set(idx, prevEdge);
    this.downheap(edges, 0, idx);
  }

  // Generates the minimum spanning tree given a list
  // of edges sorted from least to greatest by weight
  void kruskals(ArrayList<Edge> edges) {
    HashMap<GamePiece, GamePiece> representatives = new HashMap<GamePiece, GamePiece>();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    ArrayList<Edge> worklist = edges;

    for (GamePiece p : this.nodes) {
      representatives.put(p, p);
    }
    while (edgesInTree.size() < this.nodes.size() - 1 && worklist.size() > 0) {
      Edge currentEdge = worklist.get(0);
      if (find(representatives, currentEdge.fromNode) == find(representatives,
          currentEdge.toNode)) {
        worklist.remove(0); // they're already connected
      }
      else {
        edgesInTree.add(currentEdge);
        union(representatives, find(representatives, currentEdge.fromNode),
            find(representatives, currentEdge.toNode));

      }
    }
    this.mst = edgesInTree;
  }

  // Finds the representative for the given node
  GamePiece find(HashMap<GamePiece, GamePiece> representatives, GamePiece node) {
    if (representatives.get(node).equals(node)) {
      return node;
    }
    else {
      return find(representatives, representatives.get(node));
    }
  }

  void union(HashMap<GamePiece, GamePiece> representatives, GamePiece node1, GamePiece node2) {
    representatives.put(representatives.get(node1), representatives.get(node2));
  }

  // Connect all GamePieces using the minimum spanning tree
  void connectPieces() {
    // Edges are constructed from left to right
    // and top to bottom, so the fromNode will
    // always be to the left or top of the toNode

    for (Edge e : this.mst) {
      if (e.fromNode.col == e.toNode.col) {
        e.fromNode.bottom = true;
        e.toNode.top = true;
      }
      if (e.fromNode.row == e.toNode.row) {
        e.fromNode.right = true;
        e.toNode.left = true;
      }
    }
  }

  // Draw the world
  public WorldScene makeScene() {
    WorldScene ws = new WorldScene(this.width * 60, this.height * 60);

    for (int col = 0; col < this.board.size(); col++) {
      for (int row = 0; row < this.board.get(col).size(); row++) {
        ws.placeImageXY(getPiece(col, row).draw(this.radius), col * 60, row * 60);
      }
    }

    return ws;
  }

  // Randomly rotate each piece on the board
  void randomizeBoard() {
    for (ArrayList<GamePiece> col : this.board) {
      for (GamePiece piece : col) {
        for (int i = 0; i < this.rand.nextInt(4); i++) {
          piece.rotate();
        }
      }
    }
  }

  // Handles mouse clicks
  public void onMouseClicked(Posn pos) {
    for (int col = 0; col < this.board.size(); col++) {
      for (int row = 0; row < this.board.get(col).size(); row++) {
        if ((pos.x > col * 60) && (pos.x < (col + 1) * 60) && (pos.y > row * 60)
            && (pos.y < (row + 1) * 60)) {
          getPiece(col, row).rotate();
          this.checkLit();
        }
      }
    }
  }

  // Return the piece at given col and row
  GamePiece getPiece(int col, int row) {
    return this.board.get(col).get(row);
  }

  // Generates links between neighboring pieces
  void linkPieces() {

    for (int col = 0; col < board.size(); col++) {
      for (int row = 0; row < board.get(col).size(); row++) {
        if (this.height - 1 != row) { // not the bottom row
          getPiece(col, row).bottomPiece = getPiece(col, row + 1);
          // add the tile below
        }
        if (this.width - col > 1) { // not the rightmost column
          getPiece(col, row).rightPiece = getPiece(col + 1, row);
          // add the tile to the right
        }
        if (row > 0) { // not the top row
          getPiece(col, row).topPiece = getPiece(col, row - 1);
          // add the tile above
        }
        if (col > 0) { // not the leftmost column
          getPiece(col, row).leftPiece = getPiece(col - 1, row);
          // add the tile to the left
        }
      }
    }
  }

  // Lights all wires connected to the power station within this.radius
  void checkLit() {
    for (GamePiece p : this.nodes) {
      p.unlight();
    }
    getPiece(this.powerCol, this.powerRow).checkLit(this.radius);
    if (this.checkWin()) {
      this.endOfWorld("You win!");
    }
  }

  // Check if the player has connected and lit all the wires
  boolean checkWin() {
    boolean win = true;
    for (GamePiece p : this.nodes) {
      if (!(p.isLit())) {
        win = false;
      }
    }
    return win;
  }

  // Returns the win screen
  public WorldScene lastScene(String s) {
    Color c;
    int x = this.width * 60;
    int y = this.height * 60;
    WorldScene ws = new WorldScene(x, y);
    c = Color.GREEN;

    ws.placeImageXY(new TextImage(s, c), x / 2, y / 2);
    return ws;
  }

  // Handles arrow key inputs and updates the wire lighting
  public void onKeyEvent(String ke) {
    if (ke.equals("left")) {
      if (getPiece(this.powerCol, this.powerRow).left
          && getPiece(this.powerCol, this.powerRow).leftPiece != null) {
        if (getPiece(this.powerCol, this.powerRow).leftPiece.right) {
          getPiece(this.powerCol, this.powerRow).powerStation = false;
          getPiece(this.powerCol, this.powerRow).leftPiece.powerStation = true;
          this.powerCol--;
        }
      }
    }
    else if (ke.equals("right")) {
      if (getPiece(this.powerCol, this.powerRow).right
          && getPiece(this.powerCol, this.powerRow).rightPiece != null) {
        if (getPiece(this.powerCol, this.powerRow).rightPiece.left) {
          getPiece(this.powerCol, this.powerRow).powerStation = false;
          getPiece(this.powerCol, this.powerRow).rightPiece.powerStation = true;
          this.powerCol++;
        }
      }
    }
    else if (ke.equals("up")) {
      if (getPiece(this.powerCol, this.powerRow).top
          && getPiece(this.powerCol, this.powerRow).topPiece != null) {
        if (getPiece(this.powerCol, this.powerRow).topPiece.bottom) {
          getPiece(this.powerCol, this.powerRow).powerStation = false;
          getPiece(this.powerCol, this.powerRow).topPiece.powerStation = true;
          this.powerRow--;
        }
      }
    }
    else if (ke.equals("down")) {
      if (getPiece(this.powerCol, this.powerRow).bottom
          && getPiece(this.powerCol, this.powerRow).bottomPiece != null) {
        if (getPiece(this.powerCol, this.powerRow).bottomPiece.top) {
          getPiece(this.powerCol, this.powerRow).powerStation = false;
          getPiece(this.powerCol, this.powerRow).bottomPiece.powerStation = true;
          this.powerRow++;
        }
      }
    }
    checkLit();
  }

}

class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;

  // neighboring pieces, null if doesn't exist
  GamePiece leftPiece;
  GamePiece rightPiece;
  GamePiece topPiece;
  GamePiece bottomPiece;
  // whether the power station is on this piece
  boolean powerStation;
  boolean isLit;
  int litRadius;

  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.isLit = false;
    this.litRadius = 0;
  }

  // Draw this piece
  WorldImage draw(int radius) {
    RectangleImage outline = new RectangleImage(60, 60, OutlineMode.OUTLINE, Color.BLACK);
    RectangleImage fill = new RectangleImage(60, 60, OutlineMode.SOLID, Color.decode("#444444"));
    Color lit = Color.decode("#ffcc00");
    Color unlit = Color.decode("#cccccc");
    for (int i = radius; i < this.litRadius; i++) {
      lit = lit.darker();
    }
    LineImage vLine;
    LineImage hLine;
    if (this.isLit) {
      vLine = new LineImage(new Posn(0, 30), lit);
      hLine = new LineImage(new Posn(30, 0), lit);
    }
    else {
      vLine = new LineImage(new Posn(0, 30), unlit);
      hLine = new LineImage(new Posn(30, 0), unlit);
    }

    OverlayImage ret = new OverlayImage(outline, fill);
    StarImage star = new StarImage(20, OutlineMode.SOLID, Color.CYAN);

    if (this.top) {
      ret = new OverlayImage(vLine.movePinhole(0, 15), ret);
    }
    if (this.bottom) {
      ret = new OverlayImage(vLine.movePinhole(0, -15), ret);
    }
    if (this.left) {
      ret = new OverlayImage(hLine.movePinhole(15, 0), ret);
    }
    if (this.right) {
      ret = new OverlayImage(hLine.movePinhole(-15, 0), ret);
    }
    if (this.powerStation) {
      ret = new OverlayImage(star, ret);
    }
    return ret.movePinhole(-30, -30);
  }

  // Rotate this piece
  void rotate() {
    boolean prevTop = this.top;
    boolean prevBottom = this.bottom;
    boolean prevLeft = this.left;
    boolean prevRight = this.right;
    this.right = prevTop;
    this.bottom = prevRight;
    this.left = prevBottom;
    this.top = prevLeft;
  }

  // Return whether this piece is lit
  boolean isLit() {
    return this.isLit;
  }

  // Check if this piece is connected to a lit piece
  void checkLit(int radius) {
    if (radius >= 0) {
      this.isLit = true;
      if (radius > this.litRadius) {
        this.litRadius = radius;
      }
      if (this.left && this.leftPiece != null) {
        if (this.leftPiece.right) {
          // if (!this.leftPiece.isLit()) {
          this.leftPiece.checkLit(radius - 1);
          // }
        }
      }
      if (this.right && this.rightPiece != null) {
        if (this.rightPiece.left) {
          // if (!this.rightPiece.isLit()) {
          this.rightPiece.checkLit(radius - 1);
          // }
        }
      }
      if (this.top && this.topPiece != null) {
        if (this.topPiece.bottom) {
          // if (!this.topPiece.isLit()) {
          this.topPiece.checkLit(radius - 1);

          // }
        }
      }
      if (this.bottom && this.bottomPiece != null) {
        if (this.bottomPiece.top) {
          // if (!this.bottomPiece.isLit()) {
          this.bottomPiece.checkLit(radius - 1);

          // }
        }
      }
    }
  }

  // Unlight this GamePiece
  void unlight() {
    this.isLit = false;
  }
}

class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }
}

class ExamplesLightEmAll {
  LightEmAll l;
  LightEmAll g;
  LightEmAll h;
  int x;
  int y;

  void initTest() {
    x = 8;
    y = 8;
    l = new LightEmAll(x, y, new Random(0));
    g = new LightEmAll(2, 2, new Random(0));
    h = new LightEmAll(1, 2, new Random(5));
  }

  void testGenerateBoard(Tester t) {
    initTest();
    t.checkExpect(l.board.size(), l.width);
    for (int i = 0; i < l.board.size(); i++) {
      t.checkExpect(l.board.get(i).size(), l.height);
    }
    t.checkExpect(l.nodes.size(), l.width * l.height);
    t.checkExpect(l.getPiece(0, 0).powerStation, true);
  }

  void testRandomize(Tester t) {
    initTest();
    t.checkExpect(h.checkWin(), true);
    h.randomizeBoard();
    h.checkLit();
    t.checkExpect(h.checkWin(), false);
  }

  void testRotate(Tester t) {
    initTest();
    t.checkExpect(l.getPiece(0, 0).left, false);
    t.checkExpect(l.getPiece(0, 0).right, true);
    t.checkExpect(l.getPiece(0, 0).top, true);
    t.checkExpect(l.getPiece(0, 0).bottom, false);
    l.getPiece(0, 0).rotate();
    t.checkExpect(l.getPiece(0, 0).left, false);
    t.checkExpect(l.getPiece(0, 0).right, true);
    t.checkExpect(l.getPiece(0, 0).top, false);
    t.checkExpect(l.getPiece(0, 0).bottom, true);
    l.getPiece(0, 0).rotate();
    t.checkExpect(l.getPiece(0, 0).left, true);
    t.checkExpect(l.getPiece(0, 0).right, false);
    t.checkExpect(l.getPiece(0, 0).top, false);
    t.checkExpect(l.getPiece(0, 0).bottom, true);
    l.getPiece(0, 0).rotate();
    t.checkExpect(l.getPiece(0, 0).left, true);
    t.checkExpect(l.getPiece(0, 0).right, false);
    t.checkExpect(l.getPiece(0, 0).top, true);
    t.checkExpect(l.getPiece(0, 0).bottom, false);
  }

  void testIsLit(Tester t) {
    initTest();
    t.checkExpect(l.getPiece(0, 0).isLit(), true);
    t.checkExpect(l.getPiece(7, 7).isLit(), false);
  }

  void testLinkPieces(Tester t) {
    initTest();
    t.checkExpect(l.getPiece(4, 4).leftPiece, l.getPiece(3, 4));
    t.checkExpect(l.getPiece(4, 4).rightPiece, l.getPiece(5, 4));
    t.checkExpect(l.getPiece(4, 4).topPiece, l.getPiece(4, 3));
    t.checkExpect(l.getPiece(4, 4).bottomPiece, l.getPiece(4, 5));
    t.checkExpect(l.getPiece(0, 0).leftPiece, null);
    t.checkExpect(l.getPiece(0, 0).rightPiece, l.getPiece(1, 0));
    t.checkExpect(l.getPiece(0, 0).topPiece, null);
    t.checkExpect(l.getPiece(0, 0).bottomPiece, l.getPiece(0, 1));
  }

  void testOnMouseClicked(Tester t) {
    initTest();
    t.checkExpect(l.getPiece(0, 0).left, false);
    t.checkExpect(l.getPiece(0, 0).right, true);
    t.checkExpect(l.getPiece(0, 0).top, true);
    t.checkExpect(l.getPiece(0, 0).bottom, false);
    l.onMouseClicked(new Posn(10, 10));
    t.checkExpect(l.getPiece(0, 0).left, false);
    t.checkExpect(l.getPiece(0, 0).right, true);
    t.checkExpect(l.getPiece(0, 0).top, false);
    t.checkExpect(l.getPiece(0, 0).bottom, true);
    l.onMouseClicked(new Posn(10, 10));
    t.checkExpect(l.getPiece(0, 0).left, true);
    t.checkExpect(l.getPiece(0, 0).right, false);
    t.checkExpect(l.getPiece(0, 0).top, false);
    t.checkExpect(l.getPiece(0, 0).bottom, true);
    l.onMouseClicked(new Posn(10, 10));
    t.checkExpect(l.getPiece(0, 0).left, true);
    t.checkExpect(l.getPiece(0, 0).right, false);
    t.checkExpect(l.getPiece(0, 0).top, true);
    t.checkExpect(l.getPiece(0, 0).bottom, false);
  }

  void testOnKeyEvent(Tester t) {
    initTest();
    l.onMouseClicked(new Posn(70, 10));
    l.onMouseClicked(new Posn(250, 10));
    l.onMouseClicked(new Posn(250, 10));
    l.onMouseClicked(new Posn(250, 10));
    t.checkExpect(l.powerCol, 0);
    t.checkExpect(l.powerRow, 0);
    t.checkExpect(l.getPiece(0, 0).powerStation, true);
    t.checkExpect(l.getPiece(1, 0).powerStation, false);
    l.onKeyEvent("right");
    t.checkExpect(l.powerCol, 1);
    t.checkExpect(l.powerRow, 0);
    t.checkExpect(l.getPiece(0, 0).powerStation, false);
    t.checkExpect(l.getPiece(1, 0).powerStation, true);
    l.onKeyEvent("right");
    t.checkExpect(l.powerCol, 2);
    t.checkExpect(l.powerRow, 0);
    t.checkExpect(l.getPiece(1, 0).powerStation, false);
    t.checkExpect(l.getPiece(2, 0).powerStation, true);
    l.onKeyEvent("right");
    t.checkExpect(l.powerCol, 3);
    t.checkExpect(l.powerRow, 0);
    t.checkExpect(l.getPiece(2, 0).powerStation, false);
    t.checkExpect(l.getPiece(3, 0).powerStation, true);
    l.onKeyEvent("right");
    t.checkExpect(l.powerCol, 4);
    t.checkExpect(l.powerRow, 0);
    t.checkExpect(l.getPiece(3, 0).powerStation, false);
    t.checkExpect(l.getPiece(4, 0).powerStation, true);
    l.onKeyEvent("down");
    t.checkExpect(l.powerCol, 4);
    t.checkExpect(l.powerRow, 1);
    t.checkExpect(l.getPiece(4, 0).powerStation, false);
    t.checkExpect(l.getPiece(4, 1).powerStation, true);
    l.onKeyEvent("up");
    t.checkExpect(l.powerCol, 4);
    t.checkExpect(l.powerRow, 0);
    t.checkExpect(l.getPiece(4, 1).powerStation, false);
    t.checkExpect(l.getPiece(4, 0).powerStation, true);
    l.onKeyEvent("left");
    t.checkExpect(l.powerCol, 3);
    t.checkExpect(l.powerRow, 0);
    t.checkExpect(l.getPiece(4, 0).powerStation, false);
    t.checkExpect(l.getPiece(3, 0).powerStation, true);

  }

  void testCheckLit(Tester t) {
    initTest();
    t.checkExpect(l.getPiece(0, 0).isLit, true);
    t.checkExpect(l.getPiece(1, 0).isLit, false);
    t.checkExpect(l.getPiece(2, 0).isLit, false);
    l.onMouseClicked(new Posn(70, 10));
    l.onMouseClicked(new Posn(250, 10));
    l.onMouseClicked(new Posn(250, 10));
    l.onMouseClicked(new Posn(250, 10));
    t.checkExpect(l.getPiece(0, 0).isLit, true);
    t.checkExpect(l.getPiece(1, 0).isLit, true);
    t.checkExpect(l.getPiece(2, 0).isLit, true);
  }

  void testCheckWin(Tester t) {
    initTest();
    t.checkExpect(g.checkWin(), false);
    g.getPiece(0, 0).rotate();
    g.getPiece(0, 0).rotate();
    g.getPiece(0, 0).rotate();
    g.getPiece(1, 0).rotate();
    g.getPiece(1, 0).rotate();
    g.getPiece(0, 1).rotate();
    g.getPiece(0, 1).rotate();
    g.getPiece(0, 1).rotate();
    g.getPiece(1, 1).rotate();
    g.getPiece(1, 1).rotate();
    g.getPiece(1, 1).rotate();
    g.checkLit();
    t.checkExpect(g.checkWin(), true);
  }

  void testGetPiece(Tester t) {
    initTest();
    t.checkExpect(l.getPiece(0, 0), l.board.get(0).get(0));
    t.checkExpect(l.getPiece(5, 0), l.board.get(5).get(0));
    t.checkExpect(l.getPiece(0, 3), l.board.get(0).get(3));
    t.checkExpect(l.getPiece(2, 4), l.board.get(2).get(4));
  }

  void testGetEdges(Tester t) {
    initTest();
    t.checkExpect(l.getEdges().size(), 112);
  }

  void testHeapSort(Tester t) {
    initTest();
    ArrayList<Edge> edges = l.getEdges();
    boolean sorted = true;
    for (int i = 0; i < edges.size() - 1; i++) {
      if (edges.get(i).weight > edges.get(i + 1).weight) {
        sorted = false;
      }
    }
    t.checkExpect(sorted, false);
    sorted = true;
    l.heapSort(edges);
    for (int i = 0; i < edges.size() - 1; i++) {
      if (edges.get(i).weight > edges.get(i + 1).weight) {
        sorted = false;
      }
    }
    t.checkExpect(sorted, true);

  }

  void testKruskals(Tester t) {
    initTest();
    t.checkExpect(l.mst.size(), l.nodes.size() - 1);

  }

  void testFind(Tester t) {
    initTest();
    HashMap<GamePiece, GamePiece> h = new HashMap<GamePiece, GamePiece>();
    h.put(l.getPiece(0, 0), l.getPiece(0, 0));
    h.put(l.getPiece(1, 0), l.getPiece(1, 0));
    t.checkExpect(l.find(h, l.getPiece(0, 0)), l.getPiece(0, 0));
    t.checkExpect(l.find(h, l.getPiece(1, 0)), l.getPiece(1, 0));
    h.put(l.getPiece(0, 0), l.getPiece(1, 0));
    t.checkExpect(l.find(h, l.getPiece(0, 0)), l.getPiece(1, 0));
  }

  void testUnion(Tester t) {
    initTest();
    HashMap<GamePiece, GamePiece> h = new HashMap<GamePiece, GamePiece>();
    h.put(l.getPiece(0, 0), l.getPiece(0, 0));
    h.put(l.getPiece(1, 0), l.getPiece(1, 0));
    l.union(h, l.getPiece(0, 0), l.getPiece(1, 0));
    t.checkExpect(h.get(l.getPiece(0, 0)), l.getPiece(1, 0));
  }

  void testConnectPieces(Tester t) {
    initTest();
    t.checkExpect(l.mst.get(0).fromNode, l.getPiece(3, 3));
    t.checkExpect(l.mst.get(0).toNode, l.getPiece(4, 3));
    t.checkExpect(l.getPiece(3, 3).right, true);
    t.checkExpect(l.getPiece(4, 3).left, true);
    t.checkExpect(l.mst.get(1).fromNode, l.getPiece(5, 4));
    t.checkExpect(l.mst.get(1).toNode, l.getPiece(5, 5));
    t.checkExpect(l.getPiece(5, 4).bottom, true);
    // This is what the piece looks like pre-randomize
    l.getPiece(5, 5).rotate();
    t.checkExpect(l.getPiece(5, 5).top, true);
  }

  void testUnlight(Tester t) {
    initTest();
    t.checkExpect(l.getPiece(0, 0).isLit, true);
    l.getPiece(0, 0).unlight();
    t.checkExpect(l.getPiece(0, 0).isLit, false);
  }

  void testLightEmAll(Tester t) {
    initTest();
    l.bigBang(x * 60, y * 60);
  }

}