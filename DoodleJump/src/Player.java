import java.awt.Color;

import javalib.impworld.WorldScene;
import javalib.worldimages.CircleImage;
import javalib.worldimages.EmptyImage;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.OverlayImage;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.WorldImage;

// To represent a user-controlled player that jumps and collides with platforms
class Player {
	// The absolute image coordinates of the center of this player
	Vector2D position;
	// The amount that the player will move on one tick
	Vector2D velocity;
	
	IPlayerItem item;
	
	boolean isDead;

	// Standard constructor initializes fields
	Player(Vector2D position, Vector2D velocity) {
		this.position = position;
		this.velocity = velocity;
		this.item = new NoItem();
		this.isDead = false;
	}

	// Renders the player onto the background
	// EFFECT: Draws an image onto the given WorldScene
	void drawOntoScene(WorldScene background) {
		WorldImage playerImg = new RectangleImage(IConstant.PLAYER_WIDTH, IConstant.PLAYER_HEIGHT, OutlineMode.SOLID, Color.BLUE);
		WorldImage withItem = new OverlayImage(this.item.render(), playerImg);
		background.placeImageXY(withItem, this.position.x, this.position.y);
	}
	
	// Has this player's item affect the player on a tick and then move the player
	// Removes the item if finished
	// EFFECT: Modifies this according to the rules of this' item and this' item itself
	// by either removing it or ticking it
	void tickPlayer() {
		if(this.item.finished()) {
			this.item = new NoItem();
		} else {
			this.item.tickItem(this);
		}
		this.move();
	}

	// Applies the current velocity to the current position and then applies
	// acceleration to the velocity
	// EFFECT: Modifies this' position and velocity
	void move() {
		// Increase position by this' velocity
		this.position = this.position.addVectors(this.velocity);
		// Adjust position for wrap-around using modulo
		this.position = this.position.setX(Math.floorMod(this.position.x, IConstant.WINDOW_WIDTH));

		// Accelerate velocity by acceleration up to a maximum
		this.velocity = this.velocity
				.setY(Math.min(this.velocity.y + IConstant.ACC_GRAVITY, IConstant.TERMINAL_VELOCITY));
	}

	// Have the player's horizontal velocity react to the key presses
	// "reset" sets velocity to 0, "left" and "right" set velocity to the constant
	// speed in the appropriate direction
	// EFFECT: Modifies this' velocity
	void horizontalMove(String moveType) {
		if (moveType.equals("reset")) {
			this.velocity = this.velocity.setX(0);
		} else if (moveType.equals("left")) {
			this.velocity = this.velocity.setX(-IConstant.PLAYER_X_SPEED);
		} else if (moveType.equals("right")) {
			this.velocity = this.velocity.setX(IConstant.PLAYER_X_SPEED);
		} else {
			throw new IllegalArgumentException("Invalid move type.");
		}
	}

	// How far is the player from the fixed camera location vertically?
	// A positive number indicates the player appears below the camera
	// while a negative indicates above
	int displacementFromCamera() {
		return this.position.y - IConstant.CAMERA_HEIGHT;
	}

	// Moves the player down by the given amount, used for adjusting camera perspective
	// EFFECT: Increases the y-component of this' position vector
	void displaceDownwards(int displacement) {
		if (displacement <= 0) {
			throw new IllegalArgumentException("Must have positive y-displacement in image coordinates.");
		}
		this.position = this.position.addToY(displacement);
	}
	
	// According to the given collision function, is this player colliding with the game component used to parameterize it
	boolean willCollide(ICollisionFunc cf) {
		return cf.apply(this.position, this.position.addVectors(this.velocity), IConstant.PLAYER_WIDTH, IConstant.PLAYER_HEIGHT);
	}

	// Have the player's vertical velocity react to a collision with a platform,
	// bounce can only occur if already moving down
	// EFFECT: Modifies this' velocity
	void bounce(int newY_Velocity) {
		if (this.velocity.y < 0) {
			throw new RuntimeException("Player already moving upwards.");
		} else {
			this.velocity = this.velocity.setY(newY_Velocity).addToY(IConstant.ACC_GRAVITY);
		}
	}
	
	void setYVelocity(int newY_Velocity) {
		this.velocity = this.velocity.setY(newY_Velocity);
	}
	
	// Returns whether the player has been killed by a hazard or fallen
	// below the bottom of the screen
	boolean isDead() {
		return this.isDead || this.position.y >= IConstant.WINDOW_HEIGHT;
	}
	
	// Kills the player on encountering some hazard if
	// the player is not already dead
	// EFFECT: Modifies the boolean indicating whether the player has been killed
	void killPlayer() {
		if(this.isDead) {
			throw new RuntimeException("Player is already dead.");
		} 
		if(! this.item.hazardImmunity()) {
			this.isDead = true;
			this.velocity = Vector2D.ORIGIN;
		}
	}
	
	// Returns the position of the player
	Vector2D getPosition() {
		return this.position;
	}
	
	// Has the player take the given item if the player's current item is replaceable
	// EFFECT: Modifies this' item
	void takeItem(IPlayerItem item) {
		if (this.item.replaceable()) {
			this.item = item;
		}
	}
}

// To represent an item held by the player
interface IPlayerItem {
	// A visual depiction of the item while it is on the player
	WorldImage render();
	
	// Will this item be replaced by another item encountered
	boolean replaceable();
	
	// Is this item no longer in effect
	boolean finished();
	
	// Have the item perform its effects on the player each tick if applicable
	void tickItem(Player player);
	
	// Does this item provide hazard immunity to the player
	boolean hazardImmunity();
}

// The default lack of an item with no interesting properties
class NoItem implements IPlayerItem {
	// The absence of an item is invisible
	public WorldImage render() {
		return new EmptyImage();
	}
	
	// It can be replaced by any other item
	public boolean replaceable() {
		return true;
	}
	
	// It performs no action on a tick
	public void tickItem(Player player) {
		return;
	}
	
	// It is never finished (to be removed), can only be replaced
	public boolean finished() {
		return false;
	}
	
	// Does not provide hazard immunity
	public boolean hazardImmunity() {
		return false;
	}
}

// To represent an item that last for a definite number of ticks
abstract class TimeTemporaryItem implements IPlayerItem {
	int ticksSoFar;
	int totalTicks;
	
	// Constructor initializes this with the given total ticks and 0 ticks so far
	TimeTemporaryItem(int totalTicks) {
		this.ticksSoFar = 0;
		this.totalTicks = totalTicks;
	}
	
	// If the item is not finished, modifies the player and then increases the tick count
	// EFFECT: Modifies this' ticksSoFar and the player
	public void tickItem(Player player) {
		if(this.finished()) {
			throw new RuntimeException("Item should have run out.");
		}
		this.modifyPlayer(player);
		this.ticksSoFar += 1;
	}
	
	// This item is finished if sufficient number of ticks has passed
	public boolean finished() {
		return this.ticksSoFar > this.totalTicks;
	}
	
	// By default, time-limited items provide hazard immunity
	public boolean hazardImmunity() {
		return true;
	}
	
	double proportionTicksLeft() {
		return 1 - 1.0 * this.ticksSoFar / this.totalTicks;
	}
	
	// Modify the player according to specific rules
	void modifyPlayer(Player player) {
		return;
	}
}

// An item that given the player a constant, fast upward velocity for a period of time
class PropellerHat extends TimeTemporaryItem {
	// Constructor initializes the propeller hat to last for 100 ticks
	PropellerHat() {
		super(50);
	}
	// Draws this as a small cyan circle
	public WorldImage render() {
		return new CircleImage((int) (this.proportionTicksLeft() * IConstant.ITEM_SIZE / 2), 
				OutlineMode.SOLID, Color.CYAN);
	}
	
	// The propeller hat is not replaceable
	public boolean replaceable() {
		return false;
	}
	
	// Gives the player a constant upward velocity of 20 pixels per tick
	void modifyPlayer(Player player) {
		player.setYVelocity(- IConstant.TERMINAL_VELOCITY);
	}
}

//An item that given the player a constant, fast upward velocity for a period of time
class JetPack extends TimeTemporaryItem {
	// Constructor initializes the propeller hat to last for 100 ticks
	JetPack() {
		super(100);
	}
	// Draws this as a small cyan circle
	public WorldImage render() {
		return new CircleImage((int) (this.proportionTicksLeft() * IConstant.ITEM_SIZE / 2), 
				OutlineMode.SOLID, Color.RED);
	}
	
	// The propeller hat is not replaceable
	public boolean replaceable() {
		return false;
	}
	
	// Gives the player a constant upward velocity of 20 pixels per tick
	void modifyPlayer(Player player) {
		player.setYVelocity(- IConstant.TERMINAL_VELOCITY);
	}
}

//An item that given the player a constant, fast upward velocity for a period of time
class Shield extends TimeTemporaryItem {
	// Constructor initializes the propeller hat to last for 100 ticks
	Shield() {
		super(200);
	}
	// Draws this as a small orange circle
	public WorldImage render() {
		return new RectangleImage(IConstant.ITEM_SIZE, (int) (this.proportionTicksLeft() * IConstant.ITEM_SIZE), 
				OutlineMode.SOLID, Color.ORANGE);
	}
	
	// The propeller hat is not replaceable
	public boolean replaceable() {
		return true;
	}
}