import java.awt.Color;
import java.util.Random;

import javalib.impworld.WorldScene;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.WorldImage;

// A rectangular platform with a definite position that affects the player
// when the player lands on top of it
abstract class APlatform extends AGameComponent{
	APlatform(Vector2D position) {
		super(position);
	}
	
	// Determines if the player has landed on top of this platform,
	// and modifies the player in some way if a collision is detected
	// EFFECT: Modifies the player according to .onPlayerCollision
	public void interactPlayer(Player player) {
		if(player.willCollide(new WillCollideRectAbove(this.position, 
				IConstant.PLATFORM_WIDTH, IConstant.PLATFORM_HEIGHT))) {
			this.onPlayerCollision(player);
		}
	}
	
	// A visual depiction of this platform
	public abstract WorldImage render();

	// Returns a rectangle image of the constant dimensions with the given color
	WorldImage drawPlatform(Color c) {
		return new RectangleImage(IConstant.PLATFORM_WIDTH, IConstant.PLATFORM_HEIGHT, OutlineMode.SOLID, c);
	}

	// The width of this platform
	public int width() {
		return IConstant.PLATFORM_WIDTH;
	}

	// If the Player is to bounce on most platforms, the player will be given
	// the standard upward velocity
	// EFFECT: Modifies the player's velocity
	public void onPlayerCollision(Player player) {
		player.bounce(IConstant.STD_BOUNCE_VELOCITY, this.position.y - IConstant.PLATFORM_HEIGHT);
	}
}

// A basic platform that bounces the player
class StandardPlatform extends APlatform {
	StandardPlatform(Vector2D position) {
		super(position);
	}

	// Depicts this platform as a green rectangle with standard width and height
	public WorldImage render() {
		return this.drawPlatform(Color.GREEN);
	}
}

// To represent a platform that moves side to side
class HorizontalMovingPlatform extends APlatform {
	// The amount this platform moves horizontally on a single tick
	int xVel;

	HorizontalMovingPlatform(Vector2D position, int xVel) {
		super(position);
		this.xVel = xVel;
	}

	// Renders as a grey rectangle
	public WorldImage render() {
		return this.drawPlatform(Color.GRAY);
	}

	// Moves this platform on a tick according to the velocity,
	// unless either side is reached, in which case the velocity is reversed
	// EFFECT: Modifies this' x position and x velocity
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
}

// To represent a stationary platform that can only be jumped on once
class BrittlePlatform extends APlatform {
	// Has this platform been jumped on? (indicates removal)
	boolean hit;

	BrittlePlatform(Vector2D position) {
		super(position);
		this.hit = false;
	}

	// Renders as a red rectangle
	public WorldImage render() {
		return this.drawPlatform(Color.RED);
	}

	// If the Player is to bounce on a brittle platform, the player will be given
	// the standard upward velocity and marks this platform as hit
	public void onPlayerCollision(Player player) {
		super.onPlayerCollision(player);
		this.hit = true;
	}

	// This platform should be removed if below window or has been jumped on
	public boolean shouldRemove() {
		return this.position.y > IConstant.WINDOW_HEIGHT || this.hit;
	}
}

// To represent a stationary platform that provides a boost when jumped on
class SpringPlatform extends APlatform {
	SpringPlatform(Vector2D position) {
		super(position);
	}

	// Depicts this as a green platform with a grey square on top
	// EFFECT: Draws an image onto the background
	public void drawOntoScene(WorldScene background) {
		WorldImage basePlatform = this.drawPlatform(Color.GREEN);
		Vector2D springPosition = this.position.addVectors(new Vector2D(0, -IConstant.PLATFORM_HEIGHT));
		WorldImage spring = new RectangleImage(IConstant.PLATFORM_HEIGHT, IConstant.PLATFORM_HEIGHT, OutlineMode.SOLID,
				Color.GRAY);
		background.placeImageXY(basePlatform, this.position.x, this.position.y);
		background.placeImageXY(spring, springPosition.x, springPosition.y);
	}
	
	// Because this overrides drawOntoScene, this should never have to be rendered
	public WorldImage render() {
		throw new RuntimeException("Spring platform draws itself.");
	}

	// Have the player react to this
	// EFFECT: Modifies the player's
	public void onPlayerCollision(Player player) {
		player.bounce(IConstant.SPRING_VELOCITY, this.position.y - IConstant.PLATFORM_HEIGHT);
	}
}

// To represent a stationary platform that cycles between being solid (collision with player) and
// incorporeal (player passes right through)
class EtherealPlatform extends APlatform {
	// The total ticks that have been passed while this platform has been on screen
	// (used to cycle corporeality, period 200)
	int ticksElapsed;

	EtherealPlatform(Vector2D position) {
		super(position);
		// Initialize this at a random point in the cycle
		this.ticksElapsed = new Random().nextInt(200);
	}

	// Based on the number of ticks elapsed, will the player collide with this
	// platform?
	// Is ethereal for half of the time.
	boolean isEthereal() {
		return (this.ticksElapsed / 100) % 2 == 1;
	}

	// Depicts this platform as an outline yellow rectangle if ethereal and solid if
	// not
	public WorldImage render() {
		Color pltColor = Color.yellow;
		if (this.isEthereal()) {
			return new RectangleImage(IConstant.PLATFORM_WIDTH, IConstant.PLATFORM_HEIGHT, OutlineMode.OUTLINE,
					pltColor);
		} else {
			return this.drawPlatform(pltColor);
		}
	}

	// If this platform is solid, bounce normally, but if ethereal the player moves
	// as normal (down)
	// EFFECT: Modifies the player's velocity or position by calling .bounce()
	public void onPlayerCollision(Player player) {
		if (!this.isEthereal()) {
			super.onPlayerCollision(player);
		}
	}

	// Every tick, adds one to the count of total ticks elapsed
	// EFFECT: Adds to this.ticksElapsed
	public void tickComponent() {
		this.ticksElapsed += 1;
	}
}