import tester.Tester;

class Testing {
	void testWorld(Tester t) {
		JumpingWorld jw = new JumpingWorld();
		jw.bigBang(IConstant.WINDOW_WIDTH, IConstant.WINDOW_HEIGHT, IConstant.TICK_RATE);
	}

	void testCollisions(Tester t) {
		Vector2D p1 = new Vector2D(0, 0);
		WillCollideRect func = new WillCollideRect(p1, 0, 0);

		t.checkExpect(func.inclusiveBetween(0, 0, 0), true);
		t.checkExpect(func.inclusiveBetween(0, 1, 0), false);
		t.checkExpect(func.inclusiveBetween(0, 1, 1), true);
		t.checkExpect(func.inclusiveBetween(0, 1, 2), true);
		t.checkExpect(func.inclusiveBetween(2, 1, 0), false);
		t.checkExpect(func.inclusiveBetween(2, 1, 2), false);

		t.checkExpect(func.intervalOverlap(0, 20, 10, 5), true);
		t.checkExpect(func.intervalOverlap(0, 5, 10, 5), true);
		t.checkExpect(func.intervalOverlap(0, 4, 10, 5), false);
		t.checkExpect(func.intervalOverlap(0, 1, 10, 9), true);
		t.checkExpect(func.intervalOverlap(0, 1, 10, 10), true);

		t.checkExpect(func.intervalOverlap(10, 5, 0, 20), true);
		t.checkExpect(func.intervalOverlap(10, 5, 0, 5), true);
		t.checkExpect(func.intervalOverlap(10, 5, 0, 4), false);
		t.checkExpect(func.intervalOverlap(10, 9, 0, 1), true);
		t.checkExpect(func.intervalOverlap(10, 10, 0, 1), true);

		// For an object 20 across, 30 down, with a width of 50 and height 20
		func = new WillCollideRect(new Vector2D(20, 30), 50, 20);
		// Player right of object
		t.checkExpect(func.linearCollision(48, 5, true), true);
		t.checkExpect(func.linearCollision(50, 5, true), true);
		t.checkExpect(func.linearCollision(51, 5, true), false);
		t.checkExpect(func.linearCollision(55, 15, true), true);

		// Player completely within object, or vice versa
		t.checkExpect(func.linearCollision(20, 15, true), true);
		t.checkExpect(func.linearCollision(20, 100, true), true);

		// Player left of object
		t.checkExpect(func.linearCollision(-10, 15, true), true);
		t.checkExpect(func.linearCollision(-15, 10, true), true);
		t.checkExpect(func.linearCollision(-15, 5, true), false);
		t.checkExpect(func.linearCollision(-15, 15, true), true);

		// Player above object
		t.checkExpect(func.linearCollision(10, 15, false), true);
		t.checkExpect(func.linearCollision(10, 10, false), true);
		t.checkExpect(func.linearCollision(10, 5, false), false);
		t.checkExpect(func.linearCollision(5, 15, false), true);

		// Player completely within object, or vice versa
		t.checkExpect(func.linearCollision(20, 5, false), true);
		t.checkExpect(func.linearCollision(40, 100, false), true);
		t.checkExpect(func.linearCollision(60, 100, false), true);


		// Player below of object
		t.checkExpect(func.linearCollision(50, 15, false), true);
		t.checkExpect(func.linearCollision(55, 15, false), true);
		t.checkExpect(func.linearCollision(60, 15, false), false);
		t.checkExpect(func.linearCollision(60, 20, false), true);
		
		int w = 10;
		int h = 15;
		// Same column but player too low
		t.checkExpect(func.apply(new Vector2D(20, 80), null, w, h), false);
		// Same row, but player to far right
		t.checkExpect(func.apply(new Vector2D(80, 30), null, w, h), false);
		// Player just barely hits top-right corner
		t.checkExpect(func.apply(new Vector2D(55, 5), null, w, h), true);
		// Slight movement right means no more collision
		t.checkExpect(func.apply(new Vector2D(56, 5), null, w, h), false);
		// Just barely a hit from the bottom
		t.checkExpect(func.apply(new Vector2D(20, 55), null, w, h), true);
		// Player inside from the bottom-left corner
		t.checkExpect(func.apply(new Vector2D(-6, 39), null, w, h), true);
	}
}
