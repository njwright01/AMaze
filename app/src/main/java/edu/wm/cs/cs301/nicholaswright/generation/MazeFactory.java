package edu.wm.cs.cs301.nicholaswright.generation;


import android.util.Log;

/**
 * This class encapsulates how a maze is generated. 
 * It takes orders to produce a maze, delegates it to the matching maze builder 
 * that computes and delivers the maze. This class operates the worker thread
 * to do the computation in the background. The maze builder classes contribute
 * a run method to execute on the worker thread.
 */
public class MazeFactory implements Factory {
	// for logging
	private static final String TAG = "MazeFactory";
	// generation can be deterministic, i.e. same maze is generated each time for a given size
	private boolean deterministic;
	// factory keeps track of the current order, takes at most one order at a time
	private Order currentOrder;
	// factory has a MazeBuilder to do the work
	// note that subclasses are instantiated for specific algorithms such as Prim's
	// according to the given order
	private MazeBuilder builder;
	// 
	private Thread buildThread; // computations are performed in own separated thread with this.run()
	
	//////////////////////// Constructor ////////////////////////////////////////
	/**
	 * Constructor for a randomized maze generation
	 */
	public MazeFactory(){
		// nothing to do
	}
	/**
	 * Constructor with option to make maze generation deterministic or random
	 */
	public MazeFactory(boolean deterministic){
		this.deterministic = deterministic;
	}
	
	//////////////////////// Factory interface //////////////////////////////////
	@Override
	public boolean order(Order order) {
		// check if factory is busy
		if (null != buildThread && buildThread.isAlive()) {
			// order is currently processed, don't queue, just refuse
			Log.v(TAG, "MazeFactory.order: refusing to take order, too busy with current order");
			return false;
		}
		// idle, so accept order
		currentOrder = order;

		// get the seed value from the order and set the random seed
		int seed = order.getSeed();
		SingleRandom.setSeed(seed);

		// set builder according to order
		switch (order.getBuilder()) {
		case DFS :
			builder = deterministic? new MazeBuilder(true) : new MazeBuilder();
			buildOrder();
			break;
		case Prim:
			builder = deterministic? new MazeBuilderPrim(true) : new MazeBuilderPrim();
			buildOrder();
			break;
		case Eller:
			builder = deterministic? new MazeBuilderEller(true) : new MazeBuilderEller();
			buildOrder();
			break;
		default:
			Log.v(TAG, "MazeFactory.order: missing implementation for requested algorithm: " + order.getBuilder());
			return false;
		}
		return true ;
	}
	@Override
	public void cancel() {
		Log.v(TAG, "MazeFactory.cancel: called");
		if (null != buildThread) {
			buildThread.interrupt() ;
			buildThread = null; // allow for next order to get through
		}
		else {
			Log.v(TAG, "MazeFactory.cancel: no thread to cancel");
		}
		// clean up happens in interrupt handling in run method
		builder = null;
		currentOrder = null;
	}
	@Override
	public void waitTillDelivered() {
		if (null != buildThread) {
			try {
				buildThread.join();
			} catch (Exception e) { 
				Log.v(TAG, "MazeBuilder.wailTillDelivered: join synchronization with builder thread lead to an exception") ;
			}
		}
		else {
			Log.v(TAG, "MazeBuilder.waitTillDelivered: no thread to wait for");
		}
		builder = null;
		currentOrder = null;
	}
	
	/**
	 * returns the instance of the builder. order() should be called first
	 */
	public MazeBuilder getBuilder() {
		return builder;
	}
	
	///////////////////////// private methods ///////////////////////////////////
	/**
	 * Provide the builder with necessary input and start its execution
	 */
	private void buildOrder() { 
		if (null == builder) {
			Log.v(TAG, "builder is null!");
			return;
		}
		Log.v(TAG, "MazeFactory.buildOrder: started") ;
		builder.buildOrder(currentOrder);
		buildThread = new Thread(builder);
		buildThread.start();
	}
}
