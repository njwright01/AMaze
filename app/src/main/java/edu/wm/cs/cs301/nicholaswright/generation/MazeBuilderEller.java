package edu.wm.cs.cs301.nicholaswright.generation;

import android.util.Log;

import java.util.*;

public class MazeBuilderEller extends MazeBuilder implements Runnable {

	// for logging
	private static final String TAG = "MazeBuilderEller";

	/**
	 * This is a list of sets that contain cell coordinates (as int[] tuples). The notion 
	 * of sets is central to Eller's algorithm, and this is the data container that holds 
	 * those cells. At the end of the algorithm, all cells should be in the same set (since
	 * they are connected by a path), and so the size of setList should be 1.
	 */
	List<ArrayList> setList;

	//////////////////////////////Public Constructors////////////////////////////////////
	
	public MazeBuilderEller() {
		super();
		Log.v(TAG, "MazeBuilderEller uses Eller's algorithm to generate a maze.");
	}
	
	public MazeBuilderEller(boolean deterministic) {
		super(deterministic);
		Log.v(TAG, "MazeBuilderEller uses Eller's algorithm to generate a maze.");
	}
	
	/**
	 * This method generates a maze using Eller's algorithm.
	 * 
	 * For each row in the maze, put each cell in that row within a unique set,
	 * then randomly join cells (and therefore sets) that do not belong to the same
	 * set. Then (unless it's the last row) for each set in the row, make at least one
	 * vertical connection (remove a wall) to the next row. 
	 * 
	 * This makes use of the instance variable setList
	 * 
	 */
	@Override
	protected void generatePathways() {
		// initialize progress bar
		order.updateProgress(0);


		// initialize list of sets
		setList = new ArrayList<ArrayList>();
		for(int xPos = 0; xPos < width; xPos++) {
			int[] cell = {xPos, 0};
			ArrayList<int[]> cellSet = new ArrayList<int[]>();
			cellSet.add(cell);
			setList.add(cellSet);
		}
		// iterate through each wall, except for the last one
		for(int yPos = 0; yPos < height-1; yPos++) {

			// send a message showing progress
			order.updateProgress((yPos*100)/(height-1));

			// give cells without sets a home
			for(int xPos = 0; xPos < width; xPos++) {
				// cell at (xPos, yPos) is not in a set, so give it one
				if(getSet(xPos, yPos) == null) {
					int[] cell = {xPos, yPos};
					ArrayList<int[]> cellSet = new ArrayList<int[]>();
					cellSet.add(cell);
					setList.add(cellSet);
				}
				// else: the cell has a set, so we're good
			}
			// join horizontally
			//Log.v(TAG, "Merging row " + yPos);
			joinHorizontally(yPos);
			// then join vertically to the next row
			//Log.v(TAG, "joining row " + yPos + " to next");
			joinVertically(yPos);

		}		
		// finally, join horizontally the last row
		//Log.v(TAG, "Merging final row");
		
		// give cells without sets a home
		for(int xPos = 0; xPos < width; xPos++) {
			// cell at (xPos, yPos) is not in a set, so give it one
			if(getSet(xPos, height-1) == null) {
				int[] cell = {xPos, height-1};
				ArrayList<int[]> cellSet = new ArrayList<int[]>();
				cellSet.add(cell);
				setList.add(cellSet);
			}
			// else: the cell has a set, so we're good
		}
		joinHorizontally(height-1);
		//printSetList();
	}
	
	////////////////////////////Private auxiliary methods////////////////////////////
	
	/**
	 * This method performs the first of two main phases for the algorithm. We pick a cell at
	 * random in the row and try to delete the wall to the right. If the adjoining cell is
	 * a member of a different cell set, then we delete and merge the sets. Otherwise, we pick
	 * another cell and try again. We stop when either we hit a defined number of merges, which
	 * is tied to the number of sets (two-thirds the number of sets, an arbitrary decision. Anything
	 * above zero and less than the total number of sets should be fine), or we stop after failing
	 * many times (the number of max tries is tied to the size of the maze, and therefore scales
	 * with skill level). If the row being joined is the last row, we set the number of
	 * merges to the maximum number of merges (that is, the number of remaining cell sets in the
	 * maze), so we ensure that any remaining sets are merged.
	 * 
	 * This method ignores the notion of rooms and will punch holes in the walls of rooms as if
	 * they were nothing. When this happens, a routine is called that sets every cell in the 
	 * room to be a part of the set to which the selected cell belongs. This is an effort to
	 * reduce further doors from being put into the room.
	 * @param row is the index of the current row to be merged
	 */
	private void joinHorizontally(int row) {
		// by default, merge up to 2/3 of the number of sets minus 1
		int MAXMERGES = setList.size();
		int numMerges = random.nextIntWithinInterval(1, (2*MAXMERGES)/3); 
		// for the last row, merge every remaining set
		if(row == height-1) {
			numMerges = MAXMERGES;
		}
		// initialize control variables
		int MAXTRIES = width*10;
		int tries = 0;
		int merges = 0;
		while(merges < numMerges && tries < MAXTRIES) {
			// find neighbors at (cellXCoord, row) and (cellXCoord+1, row)
			int cellXCoord = random.nextIntWithinInterval(0, width-2); // to left of wall
			// create a Wallboard object so we can delete it
			CardinalDirection dir = CardinalDirection.East;
			Wallboard wallboard = new Wallboard(cellXCoord, row, dir);
			// check to see if the wall can be deleted
			if(!areInSameSet(cellXCoord, row, cellXCoord+1, row)) {
				// delete it and merge sets
				floorplan.deleteWallboard(wallboard);
				// print statement is useful for debugging
				// Log.v(TAG, "Horizontal wall deleted");
				mergeSets(cellXCoord, row, cellXCoord+1, row);
				// if we've created a door, add those cells to the current set
				if(floorplan.isInRoom(cellXCoord+1, row)){
					addRoomToSet(cellXCoord+1, row, getSet(cellXCoord, row));
				}
				merges++;
			}
			tries++;
		}
	}
	
	/**
	 * This method performs the second of the two major steps in Eller's algorithm. For every
	 * set of cells, we must make at least one vertical pathway down into the next row (in this
	 * implementation, we do no more than one per row). This implementation iterates through
	 * the list of sets and selects a random cell from each. If it is invalid, we select another
	 * cell (so we can make at maximum n tries, where n equals the size of the set). We then
	 * delete the wall beneath it and add the cell below to the current set. If no valid cell
	 * is found within the set, we do nothing and move onto the next set.
	 * 
	 * This method ignores room borders, and will make doors as it pleases. To limit the number
	 * of doors, when a room is entered, we add all cells in that room to the current set.
	 * 
	 * @param row is the index of the current row being considered. Pathways are created downward from 
	 * cells in this row.
	 */
	private void joinVertically(int row) {
		CardinalDirection dir = CardinalDirection.South;
		Wallboard currentWallboard = null;
		// number of merges is one per set
		// iterate through each set
		for(ArrayList<int[]> coordSet : setList) {
			boolean valid = false;
			int[] currentCell = null;
			// Chooses a random cell by shuffling the set and iterating through
			Collections.shuffle(coordSet, new Random(random.getSeed()));
			for(int i = 0; i < coordSet.size(); i++) {
				currentCell = coordSet.get(i);
				// if this cell is in the right row, exit the loop; we're done here
				if(currentCell[1] == row) {
					currentWallboard = new Wallboard(currentCell[0], currentCell[1], dir);
					valid = true;
					break;
				}
			}
			// if valid, delete the wall and add adjoining cell to set
			// only invalid if we went through the last loop and found nothing
			if(valid) {
				floorplan.deleteWallboard(currentWallboard);
				//Log.v(TAG, "Vertical wall deleted");
				ArrayList<int[]> containingSet = getSet(currentCell[0], currentCell[1]);
				// wrap it up into a tuple
				int[] belowCell = {currentCell[0], currentCell[1]+1};
				containingSet.add(belowCell);
				// check to see if we made a door into a room
				if(floorplan.isInRoom(belowCell[0], belowCell[1])){
					addRoomToSet(belowCell[0], belowCell[1], containingSet);
				}
			}
			// otherwise, do nothing
			else {
//				Log.v(TAG, "no valid cell in set found to join vertically");
				continue;
			}
		}
		
		
	}
	
	/**
	 * Auxiliary method to check if two cells are members of the same set. Takes coordinates
	 * of each point and compares the sets they belong to.
	 * @param x1 the x-coordinate of the first cell considered
	 * @param y1 the y-coordinate of the first cell considered
	 * @param x2 the x-coordinate of the second cell considered
	 * @param y2 the y-coordinate of the second cell considered
	 * @return true if the cells corresponding to the inputs are members of the same 
	 * set; false otherwise
	 */
	private boolean areInSameSet(int x1, int y1, int x2, int y2) {
		ArrayList<int[]> firstSet = getSet(x1, y1);
		ArrayList<int[]> secondSet = getSet(x2, y2);

		return firstSet.equals(secondSet);
	}
	
	/**
	 * Auxiliary method that merges two sets. First, we get the sets to which each cell
	 * (at (x1, y1) and (x2, y2)) belong. Then we add all the elements of the second set
	 * to the first set and remove the second set. This modifies the class' setList 
	 * instance attribute.
	 * 
	 * @param x1 the x-coordinate of the first cell considered
	 * @param y1 the y-coordinate of the first cell considered
	 * @param x2 the x-coordinate of the second cell considered
	 * @param y2 the y-coordinate of the second cell considered
	 */
	private void mergeSets(int x1, int y1, int x2, int y2){
		ArrayList<int[]> firstSet = getSet(x1, y1);
		ArrayList<int[]> secondSet = getSet(x2, y2);

		if( firstSet == null || secondSet == null) {
			//Log.v(TAG, "Merging went horribly wrong. This cell doesn't belong to a set!");
			return;
		}
		else {
			// do the actual merge
			firstSet.addAll(secondSet);
			setList.remove(secondSet);
		}
	}
	
	/**
	 * Auxiliary method that finds the set to which a cell at (x, y) belongs. Iterates
	 * through the possible sets until it finds the right one.
	 * 
	 * @param x the x-coordinate of the cell being considered
	 * @param y the y-coordinate of the cell being considered
	 * @return the element in setList to which the cell at (x, y) belongs
	 */
	private ArrayList<int[]> getSet(int x, int y){
		//System.out.printf("getting set to which (%d, %d) belongs\n", x, y);
		ArrayList<int[]> containingSet = null;
		int[] coords = {x, y};

		for(ArrayList<int[]> possibleSet : setList) {
			
			// does this set contain the coordinate pair?
			for(int[] tup : possibleSet) {
				if(Arrays.equals(coords, tup)) {
					containingSet = possibleSet;
					break;
				}
			}

		}
		if( containingSet == null) {
			//Log.v(TAG, "In getSet: This cell doesn't belong to a set");
			return null;
		}
		else {
			return containingSet;
		}

	}
	
	/**
	 * This method adds every cell within a room containing the cell at (x, y) to 
	 * a given set. First it finds the x- and y- direction limits of the room,
	 * and then it iterates through each cell within those bounds and adds it to
	 * the set. 
	 * 
	 * @param x the x-coordinate of a cell in the room
	 * @param y the y-coordinate of a cell in the room
	 * @param set the set to which the room's cells are added
	 */
	private void addRoomToSet(int x, int y, List<int[]> set) {
		// find dimensions of room
		int maxRoomX = x;
		int minRoomX = x;
		int maxRoomY = y;
		int minRoomY = y;
		int curX = x;
		int curY = y;
		
		while(floorplan.isInRoom(curX, y)) {
			maxRoomX = curX;
			curX++;
		}
		while(floorplan.isInRoom(curX, y)) {
			minRoomX = curX;
			curX--;
		}
		while(floorplan.isInRoom(x, curY)) {
			maxRoomY = curY;
			curY++;
		}
		while(floorplan.isInRoom(x, curY)) {
			minRoomY = curY;
			curY--;
		}
		// add to set
		for(int i = minRoomX; i <= maxRoomX; i++) {
			for(int j = minRoomY; j <= maxRoomY; j++) {
				int[] tuple = {i, j};
				set.add(tuple);
			}
		}
	}
	
	/**
	 * This method is useful for debugging. For each set in setList, it prints out the
	 * members of that set.
	 */
	private void printSetList() {
		int i = 0;
		for(ArrayList<int[]> set : setList) {
			System.out.printf("Set %d: ", i);
			for(int[] tuple : set) {
				System.out.printf("(%d, %d) ", tuple[0], tuple[1]);
			}
			i++;
			System.out.println();
		}
	}

}
