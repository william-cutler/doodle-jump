import java.awt.Color;
import java.util.Random;

import javalib.impworld.WorldScene;
import javalib.worldimages.CircleImage;
import javalib.worldimages.EmptyImage;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.WorldImage;

//To represent a game piece that is a part of the environment
interface IGameComponent {
	// Move this game component downward by the given amount (for camera
	// perspective)
	void displaceDownwards(int displacement);

	// Should this game component be removed?
	boolean shouldRemove();

	// Visually depict the component onto the background
	void drawOntoScene(WorldScene background);

	// Modify this platform once a tick has passed
	void tickComponent();

	// Interact with the player on each tick, generally by determining if
	// a collision occurs and then modifying the player if so
	void interactPlayer(Player player);
}

// To represent a game component with a definite position
abstract class AGameComponent implements IGameComponent {
	// The absolute image coordinates of the center of this platform
	Vector2D position;

	AGameComponent(Vector2D position) {
		this.position = position;
	}

	// Places a visual depiction of this component at this' position
	// EFFECT: Places an image onto the given WorldScene
	public void drawOntoScene(WorldScene background) {
		background.placeImageXY(this.render(), this.position.x, this.position.y);
	}

	// A visual depiction of this component
	abstract WorldImage render();
	
	// Interact with the player on each tick, generally by determining if
	// a collision occurs and then modifying the player if so
	public abstract void interactPlayer(Player player);

	// Move this game component downward by the given amount (for camera perspective)
	// EFFECT: Modifies the y-component of this platform
	public void displaceDownwards(int displacement) {
		if (displacement <= 0) {
			throw new IllegalArgumentException("Must have positive y-displacement in image coordinates.");
		}
		this.position = this.position.addToY(displacement);
	}

	// Is this component irrelevant to game-play?
	// For most components, if it goes below the screen
	public boolean shouldRemove() {
		return this.belowScreen();
	}
	
	// Is this component below the screen (candidate for removal)
	boolean belowScreen() {
		return this.position.y > IConstant.WINDOW_HEIGHT;
	}

	// Have this platform respond to a tick (default nothing since most platforms
	// are stationary)
	public void tickComponent() {
		return;
	}
}

// A standard platform with an item that the player can grab
class EnvironmentItem extends AGameComponent {
	boolean taken;
	final IPlayerItem item;

	// Constructor initializes witht he given position and item, having not yet been taken
	EnvironmentItem(Vector2D position, IPlayerItem item) {
		super(position);
		this.item = item;
		this.taken = false;
	}
	
	// Render this item in the environment as the item itself renders
	WorldImage render() {
		return this.item.render();
	}
	
	// If the player contacts this item, the player is given the item and this item becomes taken
	// EFFECT: Modifies whether this item was taken
	public void interactPlayer(Player player) {
		if(this.taken) {
			throw new RuntimeException("Item should be removed already.");
		}
		else if(player.willCollide(new WillCollideRect(this.position, 10, 10))) {
			player.takeItem(this.item);
			this.taken = true;
		}
	}
	
	// This item should be removed if it is below the screen or taken by the player
	public boolean shouldRemove() {
		return this.belowScreen() || this.taken;
	}
}

// To represent a hazard that instantly kills the player if touched
class BlackHole extends AGameComponent {
	BlackHole(Vector2D position) {
		super(position);
	}
	
	// Depicts this as a white circle of the set radius
	WorldImage render() {
		//TODO: Fix backgrounds or have better black hole image
		return new CircleImage(IConstant.BLACK_HOLE_RADIUS, OutlineMode.SOLID, Color.WHITE);
	}
	
	// Kills the player if it is in contact with this Black Hole
	public void interactPlayer(Player player) {
		if(player.getPosition().distanceTo(this.position) + 5 < IConstant.BLACK_HOLE_RADIUS) {
			player.killPlayer();
		}
	}
}

// A moving enemy that kills the player if touched unless the player lands on top, killing this monster and
// bouncing the player
class Monster extends AGameComponent {
	int xVel;
	//TODO: Custom image
	boolean hit;
	
	// Constructor initializes this' position and a random x velocity from [-10, 9)
	Monster(Vector2D position) {
		super(position);
		this.xVel = new Random().nextInt(20) - 10;
	}
	 
	// Depicts this monster as a Magenta square
	WorldImage render() {
		//TODO: Some custom image
		return new RectangleImage(IConstant.MONSTER_DIM.x, IConstant.MONSTER_DIM.y, OutlineMode.SOLID, Color.MAGENTA);
	}
	
	// Kills this monster and bounces the player if the player lands on top of this
	// Kills the player if contacted otherwise
	public void interactPlayer(Player player) {
		if(player.willCollide(new WillCollideRectAbove(this.position, IConstant.MONSTER_DIM.x, IConstant.MONSTER_DIM.y))) {
			this.hit = true;
			player.bounce(IConstant.STD_BOUNCE_VELOCITY);
		} else if(player.willCollide(new WillCollideRect(this.position, IConstant.MONSTER_DIM.x, IConstant.MONSTER_DIM.y))) {
			player.killPlayer();
		}
	}
	
	// Moves this monster horizontally across the screen until it hits a the edge, and then it reverses direction
	// EFFECT: Modifies this' position and velocity
	public void tickComponent() {
		int nextX = this.position.x + this.xVel;
		// If about to hit right wall
		if (nextX > IConstant.WINDOW_WIDTH) {
			// Set position to be at right wall
			this.position = this.position.setX(IConstant.WINDOW_WIDTH);
			// Reverse direction
			this.xVel *= -1;
		} else if (nextX < 0) {
			this.position = this.position.setX(0);
			this.xVel *= -1;
		} else {
			// Move normally
			this.position = this.position.setX(nextX);
		}
	}
	
	// The monster should be removed from play if it is below view or was killed by the player
	public boolean shouldRemove() {
		return this.belowScreen() || this.hit;
	}
}