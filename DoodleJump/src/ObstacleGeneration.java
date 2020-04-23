import java.util.Random;

// An generator that produces game components after some height has been gained
interface IComponentGenerator {
	// Is the next game component ready to be produced (usually based on height gained)
	boolean hasNextComponent();
	
	// Returns a game component that this has generated
	IGameComponent nextComponent();
	
	// Updates the progress toward the next component
	// EFFECT: Modifies some notion of progress for this
	void addToHeightSoFar(int amt);
}

// A component generator with a specific height spacing between generating components
abstract class AComponentGenerator implements IComponentGenerator {
	// Height that needs to be gained
	int untilNext;
	// Height gained so far
	int heightSoFar;
	
	// Constructor initializes this with the given spacing and 0 progress so far
	AComponentGenerator(int untilNext) {
		if (untilNext <= 0) {
			throw new IllegalArgumentException("Spacing must be positive.");
		}
		this.untilNext = untilNext;
		this.heightSoFar = 0;
	}
	
	// Updates the progress toward the next component
	// EFFECT: Modifies some notion of progress for this
	public void addToHeightSoFar(int amt) {
		this.heightSoFar += amt;
	}
	
	// A new component is ready if the height gained meets the requirement
	public boolean hasNextComponent() {
		return this.heightSoFar > this.untilNext;
	}
	
	// Returns the next component generated if ready, exception if not
	// EFFECT: Resets progress made and recalculates the next progress requirement
	public IGameComponent nextComponent() {
		if(! this.hasNextComponent()) {
			throw new RuntimeException("Not ready for another component.");
		}
		this.heightSoFar = 0;
		this.untilNext = this.nextSpacing();
		return this.generateComponent();
	}
	
	// Returns the amount of progress required to generate the next obstacle
	abstract int nextSpacing();
	
	// Given that the next component is ready, produces it
	abstract IGameComponent generateComponent();

	// Returns a moving platform at the top of the screen with a random horizontal
	// position
	// and random horizontal velocity between -10 and 10
	IGameComponent createHorizMovingPlatform() {
		int xVel = new RUtils().randBetween(-10, 10);

		return new HorizontalMovingPlatform(new Vector2D(this.randomHorizontal(), 0), xVel);
	}
	
	Vector2D randomTopPosition() {
		return new Vector2D(this.randomHorizontal(), 0);
	}

	// Returns a random x coordinate within the boundaries of the window
	int randomHorizontal() {
		return new Random().nextInt(IConstant.WINDOW_WIDTH - 2 * IConstant.PLATFORM_WIDTH) 
				+ IConstant.PLATFORM_WIDTH;
	}
}

// To produce somewhat random platforms at distinct height intervals
class PlatformGenerator extends AComponentGenerator {

	// Constructor initializes this' spacing and 0 height gained
	PlatformGenerator() {
		super(new RUtils().randBetween(20, 80));
	}

	// Returns a random platform at the top of the screen according to set probabilities
	public IGameComponent generateComponent() {
		int chance = new Random().nextInt(100);
		if (chance < 10) {
			return this.createHorizMovingPlatform();
		} else if (chance < 20) {
			return new BrittlePlatform(this.randomTopPosition());
		} else if (chance < 23) {
			return new SpringPlatform(this.randomTopPosition());
		} else if (chance < 27) {
			return new EtherealPlatform(this.randomTopPosition());
		}else {
			return new StandardPlatform(this.randomTopPosition());
		}
	}
	
	// The amount of space is some random number from [20, 80)
	public int nextSpacing() {
		return new RUtils().randBetween(20, 80);
	}
}

// To generate hazards that can harm the player
class HazardGenerator extends AComponentGenerator {
		// Constructor initializes this' spacing and 0 height gained
		HazardGenerator() {
			super(new RUtils().randBetween(500, 1500));
		}

		// Returns a random platform at the top of the screen according to set probabilities
		// if sufficient height has been gained since the last obstacle (exception if not)
		public IGameComponent generateComponent() {
			int chance = new Random().nextInt(100);
			if(chance < 50) {
				return new BlackHole(this.randomTopPosition());
			}
			else {
				return new Monster(this.randomTopPosition());
			}
		}
		
		// The amount of space is some random number from [500, 1500)
		public int nextSpacing() {
			return 500;
		}
}

//To generate hazards that can harm the player
class ItemGenerator extends AComponentGenerator {
		// Constructor initializes this' spacing and 0 height gained
		ItemGenerator() {
			super(new RUtils().randBetween(2000, 4000));
		}

		// Returns a random platform at the top of the screen according to set probabilities
		// if sufficient height has been gained since the last obstacle (exception if not)
		public IGameComponent generateComponent() {
			int chance = new Random().nextInt(100);
			IPlayerItem item;
			if(chance < 40) {
				item = new PropellerHat();
			} else if (chance < 60) {
				item = new JetPack();
			}
			else {
				item = new Shield();
			}
			return new EnvironmentItem(this.randomTopPosition(), item);
		}
		
		// The amount of space is some random number from [500, 1500)
		public int nextSpacing() {
			return new RUtils().randBetween(2000, 4000);
		}
}