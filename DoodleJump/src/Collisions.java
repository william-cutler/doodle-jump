
// A function object that determines if the player is colliding with the game component that parameterizes it
interface ICollisionFunc {
	boolean apply(Vector2D playerCurrPosn, Vector2D playerNextPosn, int playerWidth, int playerHeight);
}

// A function object that determines if the player is colliding with a rectangular game component at centered at a particular position
abstract class ARectCollisionFunc implements ICollisionFunc{
	Vector2D objectPosn;
	int objectWidth;
	int objectHeight;
	
	ARectCollisionFunc(Vector2D objectPosn, int objectWidth, int objectHeight) {
		this.objectPosn = objectPosn;
		this.objectWidth = objectWidth;
		this.objectHeight = objectHeight;
	}
	
	public abstract boolean apply(Vector2D playerCurrPosn, Vector2D playerNextPosn, int playerWidth, int playerHeight);

	// Is any part of the Player (based on given values) in either the same column or same row
	// as any part of the object checking collision?
	boolean linearCollision(int playerSource, int playerSize, boolean checkingColumn) {
		if(checkingColumn) {
			return this.intervalOverlap(playerSource, playerSize, this.objectPosn.x, this.objectWidth / 2);
		} else {
			return this.intervalOverlap(playerSource, playerSize, this.objectPosn.y, this.objectHeight / 2);
		}
	}
	
	// Is any part of playerSource +/- playerRange overlapping with objectSource +/- objectRange, inclusive?
	boolean intervalOverlap(int playerSource, int playerRange, int objectSource, int objectRange) {
		if(playerRange <= 0 || objectRange <= 0) {
			throw new IllegalArgumentException("Player and object ranges must be positive.");
		}
		int objectHigh = objectSource + objectRange;
		int objectLow = objectSource - objectRange;
		int playerHigh = playerSource + playerRange;
		int playerLow = playerSource - playerRange;
		
		return !(playerHigh < objectLow || playerLow > objectHigh);
	}
	
	// Is the middle value inclusively between the low and high value
	boolean inclusiveBetween(int low, int med, int high) {
		return (low <= med && med <= high);
	}
}

// A function object that takes in geometric information about an object to be applied
// by the player to determine if the player is about to land on top of the object
class WillCollideRectAbove extends ARectCollisionFunc{

	WillCollideRectAbove(Vector2D objectPosn, int objectWidth, int objectHeight) {
		super(objectPosn, objectWidth, objectHeight);
	}

	public boolean apply(Vector2D playerCurrPosn, Vector2D playerNextPosn, int playerWidth, int playerHeight) {
		// Can't hit a platform if not moving vertically (to avoid /0 in .inverseSlope())
		if(playerCurrPosn.y == playerNextPosn.y) {return false;}
		
		int playerBottomY = (playerCurrPosn.y + playerHeight / 2);
		int objectTopY = this.objectPosn.y - this.objectHeight / 2;
		// Is the player about to move from above this platform to a point below it?
		boolean aboveToBelow = this.inclusiveBetween(playerBottomY, 
				objectTopY, playerNextPosn.y + playerHeight / 2);
		
		// Calculate the x-coordinate of the player at the level of this platform
		double slope = playerCurrPosn.inverseSlope(playerNextPosn);
		int dy = objectTopY - playerBottomY;
		int xAtPlatform = playerCurrPosn.x + ((int) (slope * dy));
		
		boolean horizontalCheck = this.linearCollision(xAtPlatform, playerWidth, true);

		// Both conditions must be met
		return horizontalCheck && aboveToBelow;
	}
}

// A function object applied by the player that determines if the player is colliding with the
// object that initialized this function
class WillCollideRect extends ARectCollisionFunc{
	WillCollideRect(Vector2D objectPosn, int objectWidth, int objectHeight) {
		super(objectPosn, objectWidth, objectHeight);
	}

	public boolean apply(Vector2D playerCurrPosn, Vector2D playerNextPosn, int playerWidth, int playerHeight) {
		boolean horizontalCheck = this.linearCollision(playerCurrPosn.x, playerWidth, true);
		boolean verticalCheck = this.linearCollision(playerCurrPosn.y, playerHeight, false);

		return horizontalCheck && verticalCheck;
	}
}