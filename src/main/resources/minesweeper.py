import numpy as np
from minesweeper_common import UNKNOWN, MINE, get_neighbors
import constraint as csp

RUN_TESTS = False

"""
    TODO: Improve the strategy for playing Minesweeper provided in this file.
    The provided strategy simply counts the number of unexplored mines in the neighborhood of each cells.
    If this counting concludes that a cell has to contain a mine, then it is marked.
    If it concludes that a cell cannot contain a mine, then it is explored; i.e. function Player.preprocessing returns its coordinates.
    If the simple counting algorithm does not find a unexplored cell provably without a mine,
     function Player.probability_player is called to find an unexplored cell with the minimal probability having a mine.
    A recommended approach is implementing the function Player.get_each_mine_probability.
    You can adopt this file as you like but you have to keep the interface so that your player properly works on recodex; i.e.
        * Player.__init__ is called in the beginning of every game.
        * Player.turn is called to explore one cell.
"""


class Player:
    def __init__(self, rows, columns, game, mine_prb):
        # Initialize a player for a game on a board of given size with the probability of a mine on each cell.
        self.rows = rows
        self.columns = columns
        self.mine_prb = mine_prb

        # Matrix of the game. Every cell contains a number with the following meaning:
        # - A non-negative integer for explored cells means the number of mines in neighbors
        # - MINE: A cell is marked that it contains a mine
        # - UNKNOWN: We do not know whether it contains a mine or not.
        self.game = game

        # Matrix which for every cell contains the list of all neighbors.
        self.neighbors = get_neighbors(rows, columns)

        # Matrix of numbers of missing mines in the neighborhood of every cell.
        # -1 if a cell is unexplored.
        self.mines = np.full((rows, columns), -1)

        # Matrix of the numbers of unexplored neighborhood cells, excluding known mines.
        self.unknown = np.full((rows, columns), 0)
        for i in range(self.rows):
            for j in range(self.columns):
                self.unknown[i,j] = len(self.neighbors[i,j])

        # A set of cells for which the precomputed values self.mines and self.unknown need to be updated.
        self.invalid = set()

    def turn(self):
        # Returns the position of one cell to be explored.
        pos = self.preprocessing()
        if not pos:
            pos = self.probability_player()
        self.invalidate_with_neighbors(pos)
        return pos        

    def probability_player(self):
        # Return an unexplored cell with the minimal probability of mine
        prb = self.get_each_mine_probability()
        min_prb = 1
        for i in range(self.rows):
            for j in range(self.columns):
                if self.game[i,j] == UNKNOWN:
                    if prb[i,j] > 0.9999: # Float-point arithmetics may not be exact.
                        self.game[i,j] = MINE
                        self.invalidate_with_neighbors((i,j))
                    if min_prb > prb[i,j]:
                        min_prb = prb[i,j]
                        best_position = (i,j)
        return best_position

    def invalidate_with_neighbors(self, pos):
        # Insert a given position and its neighborhood to the list of cell requiring update of precomputed information.
        self.invalid.add(pos)
        for neigh in self.neighbors[pos]:
            self.invalid.add(neigh)

    def preprocess_all(self):
        # Preprocess all cells
        self.invalid = set((i,j) for i in range(self.rows) for j in range(self.columns))
        pos = self.preprocessing()
        assert(pos == None) # Preprocessing is incomplete

    def preprocessing(self):
        """
            Update precomputed information of cells in the set self.invalid.
            Using a simple counting, check cells which have to contain a mine.
            If this simple counting finds a cell which cannot contain a mine, then returns its position.
            Otherwise, returns None.
        """
        while self.invalid:
            pos = self.invalid.pop()

            # Count the numbers of unexplored neighborhood cells, excluding known mines.
            self.unknown[pos] = sum(1 if self.game[neigh] == UNKNOWN else 0 for neigh in self.neighbors[pos])

            if self.game[pos] >= 0:
                # If the cell pos is explored, count the number of missing mines in its neighborhood.
                self.mines[pos] = self.game[pos] - sum(1 if self.game[neigh] == MINE else 0 for neigh in self.neighbors[pos])
                assert(0 <= self.mines[pos] and self.mines[pos] <= self.unknown[pos])

                if self.unknown[pos] > 0:
                    if self.mines[pos] == self.unknown[pos]:
                        # All unexplored neighbors have to contain a mine, so mark them.
                        for neigh in self.neighbors[pos]:
                            if self.game[neigh] == UNKNOWN:
                                self.game[neigh] = MINE
                                self.invalidate_with_neighbors(neigh)

                    elif self.mines[pos] == 0:
                        # All mines in the neighborhood was found, so explore the rest.
                        self.invalid.add(pos) # There may be other unexplored neighbors.
                        for neigh in self.neighbors[pos]:
                            if self.game[neigh] == UNKNOWN:
                                return neigh
                        assert(False) # There has to be at least one unexplored neighbor.

        if not RUN_TESTS:
            return None

        # If the invalid list is empty, so self.unknown and self.mines should be correct.
        # Verify it to be sure.
        for i in range(self.rows):
            for j in range(self.columns):
                assert(self.unknown[i,j] == sum(1 if self.game[neigh] == UNKNOWN else 0 for neigh in self.neighbors[i,j]))
                if self.game[i,j] >= 0:
                    assert(self.mines[i,j] == self.game[i,j] - sum(1 if self.game[neigh] == MINE else 0 for neigh in self.neighbors[i,j]))


    def get_unexplored_frontier(self):
        # Returns a list of positions of unexplored cells which are neighbors of explored cells.
        frontier = set()
        for i in range(self.rows):
            for j in range(self.columns):
                if self.game[i,j] >= 0:
                    for neigh in self.neighbors[i,j]:
                        if self.game[neigh] == UNKNOWN:
                            frontier.add(neigh)
        return list(frontier)

    def get_unexplored_neighbors(self, i, j):
        # Returns a list of positions of unexplored cells which are neighbors of the cell (i,j).
        return [neigh for neigh in self.neighbors[i,j] if self.game[neigh] == UNKNOWN]

    def get_solutions_weights(self, solutions, mine_prob):
        # Returns a list of weights of the given solutions, according to which ones are
        # more probable than others.
        weights = np.zeros(len(solutions))
        for i, sol in enumerate(solutions):
            mines_count = np.sum(list(sol.values()))
            size = len(sol)
            weights[i] = mine_prob ** mines_count * (1 - mine_prob) ** (size - mines_count)
        return weights / np.sum(weights)

    def get_each_mine_probability(self):
        # Returns a matrix of probabilities of a mine of each cell

        problem = csp.Problem()
        frontier = self.get_unexplored_frontier()
        
        # Add variable for each cell in the frontier. Domain is [0,1] indicating whether there is a mine or not.
        for item in frontier:
            problem.addVariable(item, [0,1])
        
        # Add constraints for each explored cell on the border of the frontier.
        for i in range(self.rows):
            for j in range(self.columns):
                if self.game[i,j] >= 0:
                    neighbors = self.get_unexplored_neighbors(i,j)
                    if neighbors == []:
                        continue
                    # count how many mines are in the neighborhood of (i,j)
                    neigh_mines_count = sum(1 if self.game[neigh] == MINE else 0 for neigh in self.neighbors[i,j])
                    problem.addConstraint(csp.ExactSumConstraint(self.game[i,j] - neigh_mines_count), neighbors)
        
        solutions = problem.getSolutions()

        if len(solutions) == 0:
            return np.zeros((self.rows, self.columns))
        
        probabilities = np.zeros((self.rows, self.columns))
        
        # get weights of each solution, proportional to the probability of the solution
        sol_weights = self.get_solutions_weights(solutions, self.mine_prb) 
        for i, sol in enumerate(solutions):
            for key in sol:
                probabilities[key] += sol[key] * sol_weights[i]
        
        # set probabilities of unavailable cells and mine cells
        for i in range(self.rows):
            for j in range(self.columns):
                if self.game[i,j] == UNKNOWN and (i,j) not in frontier:
                    probabilities[i,j] = self.mine_prb
                if self.game[i,j] == MINE:
                    probabilities[i,j] = 1

        return probabilities
        
