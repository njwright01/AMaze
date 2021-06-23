package edu.wm.cs.cs301.nicholaswright.generation;

import edu.wm.cs.cs301.nicholaswright.gui.Constants;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class has the responsibility to obtain the tree of BSP nodes for a given maze.
 * 
 * This code is refactored code from MazeBuilder.java by Paul Falstad, www.falstad.com, Copyright (C) 1998, all rights reserved
 * Paul Falstad granted permission to modify and use code for teaching purposes.
 * Refactored by Peter Kemper
 *
 */
public class BSPBuilder {
	private final int width ; 				// width of maze
	private final int height ; 				// height of maze
	private final Distance dists ; 			// distance matrix
	private final Floorplan floorplan ;		// floorplan with maze layout
	private final int colchange ;			// comes from a random number, purpose unclear, 
	// colchange: reason for randomization unclear, used to determine color of wall 
	private final int expectedPartiters ; 	// comes from Constants partct array, entry chosen according to skill level
	// only usage is in updateProgressBar to estimate progress made in the BSP tree construction
	int partiters = 0 ; // relocated from MazeBuilder attribute partiters here. 
	private final Order order ; 		// current order
	/**
	 * Constructor
	 * @param order
	 * @param dists
	 * @param floorplan
	 * @param width
	 * @param height
	 * @param colchange
	 * @param expectedPartiters
	 */
	public BSPBuilder(Order order, Distance dists, Floorplan floorplan, int width, int height, int colchange, int expectedPartiters) {
		super() ;
		this.order = order ;
		this.dists = dists ;
		this.floorplan = floorplan ;
		this.width = width ;
		this.height = height ;
		this.colchange = colchange ;
		this.expectedPartiters = expectedPartiters ;

		partiters = 0 ; // counter for keeping track of progress made in BSP calculation, starts at 0
	}

	/**
	 * Method generates the nodes. In every node, it has two section, left and right. It chooses the wall
	 * which has the minimum grade value and then split this node into two nodes through this wall.
	 * If all the walls in one node are partitioned, it will stop to split.
	 * @param sl
	 * @return root node for BSP tree
	 * @throws InterruptedException 
	 */
	private BSPNode genNodes(ArrayList<Wall> sl) throws InterruptedException {
		// if there is no wall with a partition bit set to false, there is nothing else to do and we are at a leaf node
		if (countNonPartitions(sl) == 0)
			return new BSPLeaf(sl);
		// from the ones that have a partition bit set to false, pick a candidate with a low grade
		Wall pe = findPartitionCandidate(sl);
		// work on wall pe
		// mark pe as partitioned
		pe.setPartition(true);
		final int x  = pe.getStartPositionX();
		final int y  = pe.getStartPositionY();
		final int dx = pe.getExtensionX();
		final int dy = pe.getExtensionY();
		final ArrayList<Wall> lsl = new ArrayList<Wall>();
		final ArrayList<Wall> rsl = new ArrayList<Wall>();
		for (int i = 0; i != sl.size(); i++) {
		    // MEMO: code very similar to Seg.calculateGrade method
			Wall se = (Wall) sl.get(i);
			int df1x = se.getStartPositionX() - x;
			int df1y = se.getStartPositionY() - y;
			int sendx = se.getEndPositionX();
			int sendy = se.getEndPositionY();
			int df2x = sendx - x; 
			int df2y = sendy - y; 
			int nx = dy;
			int ny = -dx;
			int dot1 = df1x * nx + df1y * ny;
			int dot2 = df2x * nx + df2y * ny;
			if (getSign(dot1) != getSign(dot2)) {
				if (dot1 == 0)
					dot1 = dot2;
				else if (dot2 != 0) {
					// we need to split this
					int spx = se.getStartPositionX();
					int spy = se.getStartPositionY();
					if (dx == 0)
						spx = x;
					else
						spy = y;
					//Seg sps1 = new Seg(se.x, se.y, spx-se.x, spy-se.y, se.dist, colchange);
					//Seg sps2 = new Seg(spx, spy, sendx-spx, sendy-spy, se.dist, colchange);
					Wall sps1 = new Wall(se.getStartPositionX(), se.getStartPositionY(), spx-se.getStartPositionX(), spy-se.getStartPositionY(), se.getDistance(), colchange);
					Wall sps2 = new Wall(spx, spy, sendx-spx, sendy-spy, se.getDistance(), colchange);
					if (dot1 > 0) {
						rsl.add(sps1);
						lsl.add(sps2);
					} else {
						rsl.add(sps2);
						lsl.add(sps1);
					}
					sps1.setPartition(se.isPartition());
					sps2.setPartition(se.isPartition());
					continue;
				}
			}
			// decide where to add current wall
			if (dot1 > 0 || (dot1 == 0 && se.hasSameDirection(pe))) {
				rsl.add(se);
				if (dot1 == 0)
					se.setPartition(true);
			} else if (dot1 < 0 || (dot1 == 0 && se.hasOppositeDirection(pe))) { 
				lsl.add(se);
				if (dot1 == 0)
					se.setPartition(true);
			} else {
				dbg("error xx 1 "+dot1);
			}
		}
		if (lsl.size() == 0)
			return new BSPLeaf(rsl);
		if (rsl.size() == 0)
			return new BSPLeaf(lsl);
		return new BSPBranch(x, y, dx, dy, genNodes(lsl), genNodes(rsl)); // recursion on both branches
	}

    /**
	 * Counts how many elements in the wall vector have their partition bit set to false
	 * @param sl all walls
	 * @return number of walls where the partition flag is not set
	 */
	private static int countNonPartitions(ArrayList<Wall> sl) {
		int result = 0 ;
		for (int i = 0; i != sl.size(); i++)
		{
			if (!(sl.get(i)).isPartition())
				result++;
		}
		return result;
	}

	/**
	 * It finds the wall which has the minimum grade value.
	 * @param sl vector of walls
	 * @return wall that is best candidate according to grade partition (smallest grade)
	 * @throws InterruptedException 
	 */
	private Wall findPartitionCandidate(ArrayList<Wall> sl) throws InterruptedException {
		Wall result = null ;
		int bestgrade = 5000; // used to compute the minimum of all observed grade values, set to some high initial value
		final int maxtries = 50; // constant, only used to determine skip
		// consider a subset of walls proportional to the number of tries, here 50, seems to randomize the access a bit
		int skip = (sl.size() / maxtries);
		if (skip == 0)
			skip = 1;
		assert (0 < skip) : "Increment for loop must be positive";
		for (int i = 0; i < sl.size(); i += skip) {
			Wall element = sl.get(i);
			// skip walls where the partition flag was set
			if (element.isPartition())
				continue;
			// provide feedback for progress bar every 32 iterations
			partiters++;
			if ((partiters & 31) == 0) {
				updateProgressBar(partiters); // side effect: update progress bar
			}
			// check grade and keep track of minimum
			int grade = grade_partition(sl, element);
			if (grade < bestgrade) {
				bestgrade = grade;
				result = element; // determine wall with smallest grade
			}
		}
		return result;
	}

	/**
	 * Push information on progress into maze such that UI can update progress bar
	 * @param partiters
	 */
	private void updateProgressBar(int partiters) throws InterruptedException {
		// During maze generation, the most time consuming part needs to occasionally update the current screen
		// 
		int percentage = partiters*100/expectedPartiters ;
		if (null != order) {
			order.updateProgress(percentage) ;
			if (percentage < 100) {
				// give main thread a chance to process keyboard events
				Thread.currentThread().sleep(10);
			}
		}
	}

	/**
	 * Set the partition bit to true for walls on the border and where the direction is 0
	 * @param sl
	 */
	private void setPartitionBitForCertainWalls(ArrayList<Wall> sl) {
	    // TODO: check if seg just works with width and height or needs map_unit adjustment
	    //System.out.println("set Partition bit in BSP builder with scaled width and height values");
		for (Wall se : sl) {
			//se.updatePartitionIfBorderCase(width, height);
		    se.updatePartitionIfBorderCase(width*Constants.MAP_UNIT, height*Constants.MAP_UNIT);
		}
	}



	/**
	 * Identifies continuous sequences of wallboards on the maze and fills the wall list 
	 * @return vector of walls
	 */
	private ArrayList<Wall> generateWalls() {
		ArrayList<Wall> sl = new ArrayList<Wall>();
		// this is left over from changes made in a refactoring step
		// it is an example to show how one can operate the old and the new
		// version of the code in parallel and test if they deliver same results.
		// by flipping the condition one can run the deprecated old version
		// instead of the new variant that uses the iterator for walls
		// TODO: clean up code, remove unnecessary deprecated methods
		if (false) {
		generateWallsForHorizontalWallboards(sl); 

		generateWallsForVerticalWallboards(sl);
		}
		else {
		    generateWallsForHorizontalWallboardsNew(sl); 

	        generateWallsForVerticalWallboardsNew(sl);
		}
		// starting positions for walls seem to be chosen such that walls represent top or left wallboards
		return sl ;
	}

	/**
	 * Identify continuous sequences of wallboards in a vertical direction
	 * @param sl
	 * @deprecated
	 */
	private void generateWallsForVerticalWallboards(ArrayList<Wall> sl) {
		int x;
		int y;
		// we search for vertical wallboards, so for each row
		for (x = 0; x != width; x++) {
			y = 0;
			// TODO: change this into an iterator for Cells that gives walls
			while (y < height) {
				// find the beginning of a wall
				if (floorplan.hasNoWall(x,y, CardinalDirection.West)) {
					y++;
					continue;
				} 
				int starty = y;
				// find the end of a wall
				y = findEndOfVerticalWall(x, y, CardinalDirection.West);
				// create wall with (x,starty) being being the actual start position of the wall, y-starty being the positive length
				sl.add(new Wall(x*Constants.MAP_UNIT, starty*Constants.MAP_UNIT,
						0, (y-starty)*Constants.MAP_UNIT, dists.getDistanceValue(x, starty), colchange));
			}
			y = 0;
			while (y < height) {
				// find the beginning of a wall
				if (floorplan.hasNoWall(x,y, CardinalDirection.East)) {
					y++;
					continue;
				} 
				int starty = y;
				// find the end of a wall
				y = findEndOfVerticalWall(x, y, CardinalDirection.East);
				// create wall with (x+1,y) being being one off in both directions from the last cell in this wall, starty-y being the negative length
				// since we are looking at right wallboards, one off in the right direction (x+1) are then cells that have this wall on its left hand side
				// for some reason the end position is used as a starting position and therefore the length & direction is inverse 
				sl.add(new Wall((x+1)*Constants.MAP_UNIT, y*Constants.MAP_UNIT,
						0, (starty-y)*Constants.MAP_UNIT, dists.getDistanceValue(x, starty), colchange));
			}
		}
	}
	// new code with iterator
	// status: complete
	/**
     * Identify continuous sequences of wallboards in a vertical direction
     * @param sl
     */
	   private void generateWallsForVerticalWallboardsNew(ArrayList<Wall> sl) {
	        int x;
	        int y;
	        Iterator<int[]> it;
	        int[] cur;
	        // we search for vertical wallboards, so for each row
	        for (x = 0; x < width; x++) {
	            it = floorplan.iterator(x, 0, CardinalDirection.West);
	            while(it.hasNext()) {
	                cur = it.next();
	                int starty = cur[0];
	                y = cur[1];
	                // create wall with (x,starty) being the actual start position of the wall, 
                    // y-starty being the positive length
                    sl.add(new Wall(x*Constants.MAP_UNIT, starty*Constants.MAP_UNIT,
                            0, (y-starty)*Constants.MAP_UNIT, dists.getDistanceValue(x, starty), colchange));
	            }
	            
	            it = floorplan.iterator(x, 0, CardinalDirection.East);
                while(it.hasNext()) {
                    cur = it.next();
                    int starty = cur[0];
                    y = cur[1];
                    // create wall with (x+1,y) being being one off in both directions from the last cell in this wall, starty-y being the negative length
                    // since we are looking at right wallboards, one off in the right direction (x+1) are then cells that have this wall on its left hand side
                    // for some reason the end position is used as a starting position and therefore the length & direction is inverse 
                    sl.add(new Wall((x+1)*Constants.MAP_UNIT, y*Constants.MAP_UNIT,
                            0, (starty-y)*Constants.MAP_UNIT, dists.getDistanceValue(x, starty), colchange));
                }
	        }
	    }
	/**
	 * @deprecated
	 */
	private int findEndOfVerticalWall(int x, int y, CardinalDirection wallOnThisSide) {
		while (floorplan.hasWall(x, y, wallOnThisSide)) {
			y++;
			if (y == height)
				break;
			if (floorplan.hasWall(x, y, CardinalDirection.North))
				break;
		}
		return y;
	}
	/**
     * @deprecated
     */
	private int findEndOfHorizontalWall(int x, int y, CardinalDirection wallOnThisSide) {
		while (floorplan.hasWall(x,y, wallOnThisSide)) {
			x++;
			if (x == width)
				break;
			if (floorplan.hasWall(x,y, CardinalDirection.West))
				break;
		}
		return x;
	}
	/**
	 * Identify continuous sequences of wallboards in a horizontal direction
	 * @param sl
	 * @deprecated
	 */
	private void generateWallsForHorizontalWallboards(ArrayList<Wall> sl) {
		int x;
		int y;
		// we search for horizontal wallboards, so for each column
		for (y = 0; y != height; y++) {
			// first round through rows
			x = 0;
			while (x < width) {
				// find the beginning of a wall
				if (floorplan.hasNoWall(x,y, CardinalDirection.North)) {
					x++;
					continue;
				} 
				// found one
				int startx = x;
				// find the end of a wall
				// follow wall with wallboard on top till
				// x is the first index of a cell that has no wallboard on top
				// stop at outer bound or when hitting a wallboard (cell has wallboard on left)
				// such that length of the wall is startx-x, which is a negative value btw
				x = findEndOfHorizontalWall(x, y, CardinalDirection.North);
				// create wall with (x,y) being the end positions, startx-x being the negative length
				// note the (x,y) is not part of the wall
				sl.add(new Wall(x*Constants.MAP_UNIT, y*Constants.MAP_UNIT,
						(startx-x)*Constants.MAP_UNIT, 0, dists.getDistanceValue(startx, y), colchange));
			}
			// second round through rows, same for bottom wallboards
			x = 0;
			while (x < width) {
				// find the beginning of a wall
				if (floorplan.hasNoWall(x,y, CardinalDirection.South)) {
					x++;
					continue;
				} 
				int startx = x;
				// find the end of a wall
				x = findEndOfHorizontalWall(x, y, CardinalDirection.South);
				// create wall with (startx,y+1) being one below the start position, x-startx being the positive length
				// so this may represent a wallboard at the bottom of the wall as the top wallboard one below
				sl.add(new Wall(startx*Constants.MAP_UNIT, (y+1)*Constants.MAP_UNIT,
						(x-startx)*Constants.MAP_UNIT, 0, dists.getDistanceValue(startx, y), colchange));
			}
		}
	}

    // new code with iterator
    // status: complete
	/**
     * Identify continuous sequences of wallboards in a horizontal direction
     * @param sl
     */
    private void generateWallsForHorizontalWallboardsNew(ArrayList<Wall> sl) {
        int x;
        int y;
        Iterator<int[]> it;
        int[] cur;
        // we search for horizontal wallboards, so for each column
        for (y = 0; y < height; y++) {
            // first round through rows
            it = floorplan.iterator(0,y, CardinalDirection.North);
            while(it.hasNext()) {
                cur = it.next();
                int startx = cur[0];
                x = cur[1];
                // create wall with (x,y) being the end positions, startx-x being the negative length
                // note the (x,y) is not part of the wall
                sl.add(new Wall(x*Constants.MAP_UNIT, y*Constants.MAP_UNIT,
                        (startx-x)*Constants.MAP_UNIT, 0, dists.getDistanceValue(startx, y), colchange));
            }
            // second round through rows, same for bottom wallboards
            it = floorplan.iterator(0,y, CardinalDirection.South);
            while(it.hasNext()) {
                cur = it.next();
                int startx = cur[0];
                x = cur[1];
                // create wall with (startx,y+1) being one below the start position, x-startx being the positive length
                // so this may represent a wallboard at the bottom of the wall as the top wallboard one below
                sl.add(new Wall(startx*Constants.MAP_UNIT, (y+1)*Constants.MAP_UNIT,
                        (x-startx)*Constants.MAP_UNIT, 0, dists.getDistanceValue(startx, y), colchange));
            }
        }
    }
	/**
	 * Method called in genNodes to determine the minimum of all such grades. 
	 * The method is static, i.e. it does not update internal attributes and just calculates the returned value.
	 * @param sl vector of walls
	 * @param pe particular wall
	 * @return undocumented
	 */
	private int grade_partition(ArrayList<Wall> sl, Wall pe) {
	    // code relocated to Seg.java
	    ///* original code
		// copy attributes of parameter pe
		final int x  = pe.getStartPositionX();
		final int y  = pe.getStartPositionY();
		final int dx = pe.getExtensionX();
		final int dy = pe.getExtensionY();
		final int inc = (sl.size() >= 100) ? sl.size() / 50 : 1 ; // increment for iteration below
		// define some local counter
		int lcount = 0, rcount = 0, splits = 0;
		// check all walls, loop calculates lcount, rcount and splits
		for (int i = 0; i < sl.size(); i += inc) {
			Wall se = (Wall) sl.get(i);
			// extract information from wall
			int df1x = se.getStartPositionX() - x; // difference between beginning of wall and x
			int df1y = se.getStartPositionY() - y; // difference between beginning of wall and y
			int df2x = se.getEndPositionX() - x; // difference between end of wall and x
			int df2y = se.getEndPositionY() - y; // difference between end of wall and y
			int nx = dy;
			int ny = -dx;
			int dot1 = df1x * nx + df1y * ny;
			int dot2 = df2x * nx + df2y * ny;
			// update splits if necessary
			if (getSign(dot1) != getSign(dot2)) {
				if (dot1 == 0)
					dot1 = dot2;
				else if (dot2 != 0) {
					splits++;
					continue;
				}
			}
			// update lcount, rcount values
			if (dot1 > 0 ||
					(dot1 == 0 && se.hasSameDirection(pe))) {
				rcount++;
			} else if (dot1 < 0 ||
					(dot1 == 0 && se.hasOppositeDirection(pe))) {
				lcount++;
			} else {
				dbg("grade_partition problem: dot1 = "+dot1+", dot2 = "+dot2);
			}
		}
		int result_old = Math.abs(lcount-rcount) + splits * 3;
		//*/
	    // new code
	    int result_new = pe.calculateGrade(sl);
	    assert (result_old == result_new) : "BSPBuilder grade calculation fails";
	    return result_new;
	}
	/**
	 * Generate tree of BSP nodes for a given maze
	 * @return
	 * @throws InterruptedException 
	 */
	public BSPNode generateBSPNodes() throws InterruptedException {
		// determine walls, i.e. wallboards over multiple cells in a vertical or horizontal direction
		ArrayList<Wall> seglist = generateWalls();

		setPartitionBitForCertainWalls(seglist); // partition bit true means that those are not considered any further for node generation

		// TODO: check why this is done. It creates a top wallboard on position (0,0). This may even corrupt a maze and block its exit!
		Wallboard wallboard = new Wallboard(0, 0, CardinalDirection.North) ;
		floorplan.addWallboard(wallboard, false);

		return genNodes(seglist); // creates a data structure to quickly search for walls
	}
	/**
	 * Provides the sign of a given integer number
	 * @param num
	 * @return -1 if num < 0, 0 if num == 0, 1 if num > 0
	 */
	static int getSign(int num) {
		return (num < 0) ? -1 : (num > 0) ? 1 : 0;
	}
	/**
	 * Produce output for debugging purposes
	 * @param str
	 */
	static void dbg(String str) {
		System.out.println("BSPBuilder: "+str);
	}
}
