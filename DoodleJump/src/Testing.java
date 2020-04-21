import tester.Tester;

class Testing {
	void testWorld(Tester t) {
		JumpingWorld jw = new JumpingWorld();
		jw.bigBang(IConstant.WINDOW_WIDTH, IConstant.WINDOW_HEIGHT, IConstant.TICK_RATE);
	}
}
