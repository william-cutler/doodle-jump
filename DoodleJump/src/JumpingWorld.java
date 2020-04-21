import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import javalib.impworld.*;
import javalib.worldimages.*;

// A collection of global constants
interface IConstant {
	int ACC_GRAVITY = 1;
	double TICK_RATE = 1 / 30.0;
	int WINDOW_WIDTH = 400;
	int WINDOW_HEIGHT = 600;

	int CAMERA_HEIGHT = WINDOW_HEIGHT / 2;

	int PLATFORM_WIDTH = 30;
	int PLATFORM_HEIGHT = 5;
	int STD_BOUNCE_VELOCITY = -20;
	int SPRING_VELOCITY = -30;

	int PLAYER_X_SPEED = 6;
	int PLAYER_WIDTH = 10;
	int PLAYER_HEIGHT = 15;
	int TERMINAL_VELOCITY = 20;
	
	Vector2D MONSTER_DIM = new Vector2D(20, 20);

	int BLACK_HOLE_RADIUS = 15;

	Color TEXT_COLOR = Color.white;
}

// To interface between the world program and the JumpingGame,
// responding to user key presses and clock ticks
class JumpingWorld extends World {
	JumpingGame jg;
	final WorldImage spaceBackground;

	// Default constructor initializes default JumpingGame
	JumpingWorld() {
		this.jg = new JumpingGame();
		this.spaceBackground = new FromFileImage("earth-space.png");
	}

	// Provide a visual depiction of the game onto a WorldScene
	public WorldScene makeScene() {
		WorldScene scene = new WorldScene(IConstant.WINDOW_WIDTH, IConstant.WINDOW_HEIGHT);
		scene.placeImageXY(this.spaceBackground, IConstant.WINDOW_WIDTH / 2, IConstant.WINDOW_HEIGHT / 2);
		this.jg.drawScene(scene);

		return scene;
	}

	// The final scene is that of the current scene with text indicating the game is
	// lost
	WorldScene finalScene() {
		WorldScene scene = this.makeScene();
		scene.placeImageXY(new TextImage("You have lost.", IConstant.TEXT_COLOR), IConstant.WINDOW_WIDTH / 2,
				IConstant.WINDOW_HEIGHT / 2);
		return scene;
	}

	// Adjusts game elements as the result of a tick
	// EFFECT: Modifies the player, obstacles, and score in Jumping Game
	public void onTick() {
		this.jg.tickPlayer();
		this.jg.tickCamera();
		this.jg.cleanObstacles();
		this.jg.tickComponents();
		this.jg.addObstacles();
	}

	// Respond to user key presses to begin horizontal player motion
	// EFFECT: Modifies the PLayer's x velocity
	public void onKeyEvent(String key) {
		if (key.equals("left")) {
			this.jg.playerHorizontalMove("left");
		} else if (key.equals("right")) {
			this.jg.playerHorizontalMove("right");
		}
	}

	// Respond to key releases to end horizontal player motion
	// EFFECT: Sets the Player's x velocity to 0
	public void onKeyReleased(String key) {
		if (key.equals("left") || key.contentEquals("right")) {
			this.jg.playerHorizontalMove("reset");
		}
	}

	// Determines if the game is lost and depicts the final scene if so
	public WorldEnd worldEnds() {
		if (this.jg.gameOver()) {
			return new WorldEnd(true, this.finalScene());
		} else {
			return new WorldEnd(false, this.makeScene());
		}
	}
}

// To represent the player, environment, and obstacles
class JumpingGame {
	final Player player;
	// All of the platforms currently visible on the screen
	ArrayList<IGameComponent> gamePieces;

	IComponentGenerator platformGen;
	IComponentGenerator hazardGen;
	IComponentGenerator itemGen;

	int score;

	// Default constructor begins game with the player part-way up the screen and a
	// platform below
	JumpingGame() {
		Vector2D startPos = new Vector2D(IConstant.WINDOW_WIDTH / 2, 2 * IConstant.WINDOW_HEIGHT / 3);
		Vector2D startVel = new Vector2D(0, 0);
		this.player = new Player(startPos, startVel);
		this.gamePieces = this.initializePlatforms();
		this.score = 0;
		this.platformGen = new PlatformGenerator();
		this.hazardGen = new HazardGenerator();
		this.itemGen = new ItemGenerator();
	}

	// Returns an initial list of standard platforms that guarantees one just below
	// the player spawn
	ArrayList<IGameComponent> initializePlatforms() {
		ArrayList<IGameComponent> plts = new ArrayList<IGameComponent>();
		// Add a platform below the player
		plts.add(new StandardPlatform(new Vector2D(IConstant.WINDOW_WIDTH / 2, 9 * IConstant.WINDOW_HEIGHT / 10)));
		// A dummy ObstacleGenerator just to use .randomHorizontal
		PlatformGenerator og = new PlatformGenerator();
		for (int height = 1; height < 10; height += 1) {
			plts.add(new StandardPlatform(new Vector2D(og.randomHorizontal(), height * IConstant.WINDOW_HEIGHT / 10)));
		}
		
		return plts;
	}

	// Adjusts the perspective to follow the player upwards and increases the score
	// to reflect the maximum altitude reached
	// EFFECT: Displaces every environment component and the player downwards,
	// increases the score and updates the component generators
	void tickCamera() {
		int displacement = this.player.displacementFromCamera();
		// If the player has reached a new maximum
		if (displacement < 0) {
			// Displace player downwards
			this.player.displaceDownwards(-displacement);
			// Displace all game components downward
			for (IGameComponent component : this.gamePieces) {
				component.displaceDownwards(-displacement);
			}
			// Update score and obstacle generator
			this.score += -displacement;
			this.platformGen.addToHeightSoFar(-displacement);
			this.hazardGen.addToHeightSoFar(-displacement);
			this.itemGen.addToHeightSoFar(-displacement);
		}
		if (this.player.displacementFromCamera() < 0) {
			throw new RuntimeException("Player displacement not fixed.");
		}
	}

	// Have the player undergo actions for one tick by moving as normal unless
	// a collision is about to occur
	// EFFECT: Modifies the player according to a move or a collision
	void tickPlayer() {
		for (IGameComponent component : this.gamePieces) {
			component.interactPlayer(this.player);
		}
		this.player.tickPlayer();
	}

	// Begin or end horizontal movement for the player
	// Valid options are "left", "right", "reset"
	// EFFECT: Modifies the player's velocity to begin or end horizontal movement
	void playerHorizontalMove(String moveType) {
		this.player.horizontalMove(moveType);
	}

	// The game is over if the player touches the bottom of the screen or has been killed
	// by some hazard
	boolean gameOver() {
		return this.player.isDead();
	}

	// Provides a visual depiction of the player, obstacles, and environment
	// EFFECT: Places images onto the background
	void drawScene(WorldScene background) {
		// Draw player
		this.player.drawOntoScene(background);
		// Draw all game components
		for (IGameComponent component : gamePieces) {
			component.drawOntoScene(background);
		}

		// Display the score on screen
		String scoreText = "Score: " + Integer.toString(this.score);
		TextImage scoreTextImg = new TextImage(scoreText, IConstant.TEXT_COLOR);
		background.placeImageXY(scoreTextImg, 9 * IConstant.WINDOW_WIDTH / 10, 9 * IConstant.WINDOW_HEIGHT / 10);
	}

	// Removes obstacles from the game as appropriate (usually if below the screen
	// or some other obstacle-specific event occurs)
	// EFFECT: Removes elements from this' ArrayList of IPlatforms
	void cleanObstacles() {
		ArrayList<IGameComponent> componentsToKeep = new ArrayList<IGameComponent>();
		for (IGameComponent component : this.gamePieces) {
			if (!component.shouldRemove()) {
				componentsToKeep.add(component);
			}
		}
		this.gamePieces = componentsToKeep;
	}

	// Calls on the component generators to create a new platforms and hazards if enough altitude
	// has been gained since the last obstacle
	// EFFECT: Adds elements to this' ArrayList of IGameComponents
	void addObstacles() {
		if (this.platformGen.hasNextComponent()) {
			this.gamePieces.add(this.platformGen.nextComponent());
		}
		if (this.hazardGen.hasNextComponent()) {
			this.gamePieces.add(this.hazardGen.nextComponent());
		}
		if (this.itemGen.hasNextComponent()) {
			this.gamePieces.add(this.itemGen.nextComponent());
		}
	}

	// Moves obstacles that have movement
	// EFFECT: Calls the tick function on all of this' platforms
	void tickComponents() {
		for (IGameComponent comp : this.gamePieces) {
			comp.tickComponent();
		}
	}
}

// To represent a vector in 2D space, useful for position and velocity
class Vector2D {
	// Like Posn class, fields do not support mutation
	final int x;
	final int y;

	// Standard constructor initializes fields
	Vector2D(int x, int y) {
		this.x = x;
		this.y = y;
	}

	// The result of component-wise vector addition
	Vector2D addVectors(Vector2D other) {
		return new Vector2D(this.x + other.x, this.y + other.y);
	}

	// The result of component-wise vector addition with the vector <amt, 0>
	Vector2D addToX(int amt) {
		return new Vector2D(this.x + amt, this.y);
	}

	// The result of component-wise vector addition with the vector <0, amt>
	Vector2D addToY(int amt) {
		return new Vector2D(this.x, this.y + amt);
	}

	// Returns a new vector with the given x but the same y
	Vector2D setX(int x) {
		return new Vector2D(x, this.y);
	}

	// Returns a new vector with the given y but the same x
	Vector2D setY(int y) {
		return new Vector2D(this.x, y);
	}

	// Returns the euclidian distance between this vector and the other
	double distanceTo(Vector2D other) {
		return Math.sqrt(Math.pow(other.x - this.x, 2) + Math.pow(other.y - this.y, 2));
	}

	// Returns the rate of change from this vector to the other as dx / dy
	double inverseSlope(Vector2D other) {
		if (other.y == this.y) {
			throw new IllegalArgumentException("Must have differing 'y' values.");
		} else {
			return (1.0 * other.x - this.x) / (1.0 * other.y - this.y);
		}
	}
	
	// Two 2DVectors are equal if their coordinates are the same
	public boolean equals(Object o) {
		if(! (o instanceof Vector2D)) {
			return false;
		} else {
			Vector2D pt = (Vector2D) o;
			return pt.x == this.x && pt.y == this.y;
		}
	}

	// A string summary of this vector that displays the two coordinates
	public String toString() {
		return "X: " + Integer.toString(this.x) + ", Y: " + Integer.toString(this.y);
	}
}