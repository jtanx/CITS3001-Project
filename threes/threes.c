#include "threes.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <math.h>


//64 byte Lookup table for traversing the grid on shifts
const uint8_t g_trn[4][BOARD_SPACE] = 
{
	{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}, //Left
	{0,4,8,12,1,5,9,13,2,6,10,14,3,7,11,15}, //Up
	{15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0}, //Right
	{15,11,7,3,14,10,6,2,13,9,5,1,12,8,4,0} //Down
};

#ifdef _DEBUG
void stahp(void) {
	system("pause");
}
#endif

bool is_valid_tile(Tile t) {
	//Is 1, 2 or a multiple of 3 that is also a power of 2.
	Tile v = t/3;
	return t == 1 || t == 2 || (!(t % 3) && !(v & (v - 1)));
}

bool load_file(Board *b, char *f) {
	ArrayBuilder *ab;
	FILE *fp;
	char buf[BUFSIZ], *tok, *next;
	int i,j;

	if(!(fp = fopen(f, "r"))) {
		perror("Failed to open the input file");
		return false;
	}

	if (!ab_init(&ab, 0, sizeof(Tile))) {
		printf("Memory allocation failed");
		fclose(fp);
		return false;
	}

	//Skip the header
	for (i = 0; i < 2 && fgets(buf, BUFSIZ, fp); i++);
	//Read in the board
	for (i = 0; i < BOARD_SIZE && fgets(buf, BUFSIZ, fp); i++) {
		tok = strtok_r(buf, " ", &next);
		for (j = 0; tok != NULL && j < BOARD_SIZE; j++) {
			Tile v = strtoul(tok, NULL, 10);
			if (!is_valid_tile(v)) {
				printf("Invalid tile value: %d\n", v);
				fclose(fp);
				return false;
			}
			b->current[i * BOARD_SIZE + j] = v;
			tok = strtok_r(NULL, " ", &next);
		}
	}
	//Skip line 7
	fgets(buf, BUFSIZ, fp);
	//Read in the placement tiles
	while (fgets(buf, BUFSIZ, fp)) {
		tok = strtok_r(buf, " ", &next);
		while (tok != NULL) {
			if (isdigit(*tok)) {
				Tile v = strtoul(tok, NULL, 10);
				if (!is_valid_tile(v)) {
					printf("Invalid tile value: %d\n", v);
					fclose(fp);
					return false;
				} else if (!ab_add(ab, &v)) {
					printf("Memory allocation failure\n");
					fclose(fp);
					return false;
				}
			}
			tok = strtok_r(NULL, " ", &next);
		}
	}

	fclose(fp);

	//Load up the tile sequence
	b->sequence = ab_finalise(&ab, &b->n_sequence);
	return true;
}

void print_board(Board *b) {
	uint8_t i;
	for (i = 0; i < BOARD_SPACE; i++) {
		printf("%3d ", b->current[i]);
		if ((i+1) % BOARD_SIZE == 0)
			printf("\n");
	}
}

void print_tiles(Board *b) {
	size_t i;
	for (i = 0; i < b->n_sequence; i++)
		printf("%d ", b->sequence[i]);
	printf("\n");
}

bool shift_valid(Tile from, Tile to) {
	return (from && !to) || (from == 1  && to == 2) || 
		   (from == 2 && to == 1) || (from > 2 && from == to);
}

/**
 * To be able to insert the new tile from the sequence onto the board, the following is done:
 * Move done --> Move used when inserting tile:
 * UP --> RIGHT
 * RIGHT --> UP
 * LEFT --> DOWN
 * DOWN --> LEFT
 * E.g when an UP shift is performed, we then scan the board as if performing a RIGHT shift.
 * When scanning the board, seq_rows is used to keep track of which 'row's (or columns) are
 * still in contention for sequence insertion. Initially, this value is the bitmask of which rows
 * were shifted in the move, but transposed to work for the new search pattern.
 *
 * As the search/scan is performed, seq_rows is updated as necessary to discount rows, based on
 * what the currently observed 'minimum' value is. 
 * 
 * When all rows have been scanned, the log base 2 of seq_rows is performed to determine the 
 * insertion point.
 * O(n^2) performance, where n is the width of the board 
 */
void insert_sequence(Board *b, uint8_t seq_rows, const uint8_t *seq_trn) {
	uint8_t i, j;

	for (i = 0; i < BOARD_SIZE; i++) {
		Tile min_value = UINT_MAX;
		for (j = 0; j < BOARD_SIZE; j++) {
			if (seq_rows & (1 << j)) { //Is a row that sequence can be inserted into
				uint8_t idx = seq_trn[i * BOARD_SIZE + j];
				//Three-way comparison
				if (b->current[idx] < min_value) {
					min_value = b->current[idx];
					seq_rows &= ~((1 << j) - 1); //All rows previous are out of the running
				} else if (b->current[idx] > min_value) {
					seq_rows ^= 1 << j; //This row is no longer in the running
				}
			}
		}

		//Check if only 1 bit set === check if power of 2
		if (seq_rows && !(seq_rows & (seq_rows - 1))) {
			break;
		}
	}

	//Seems to work... May have to check for the 'most clockwise' rule
	j = 0;
	while (seq_rows >>= 1) j++;

	b->current[seq_trn[j]] = b->sequence[b->c_sequence];
	b->c_sequence = (b->c_sequence + 1) % b->n_sequence;
}

/**
 * Takes 2 * n^2 time in the worst case, where n is the width of the board.
 */
void move(Board *b, char m) {
	bool local_shift = false;
	const uint8_t *trn, *seq_trn;
	uint8_t i, j, seq_rows = 0;
	//seq_rows is a bitmask for rows that should be considered for sequence insert

	switch (tolower(m)) {
		case 'l': trn = g_trn[0], seq_trn = g_trn[3]; break;
		case 'u': trn = g_trn[1], seq_trn = g_trn[2]; break;
		case 'r': trn = g_trn[2], seq_trn = g_trn[1]; break;
		case 'd': trn = g_trn[3], seq_trn = g_trn[0]; break;
		default: return;
	}

	for (i = 0; i < BOARD_SIZE; i++) {
		for (j = 1; j < BOARD_SIZE; j++) {
			uint8_t idx = trn[i * BOARD_SIZE + j];
			uint8_t pidx = trn[i * BOARD_SIZE + j-1];

			if (local_shift) {
				b->current[pidx] = b->current[idx];
				b->current[idx] = 0;
			} else if(shift_valid(b->current[idx], b->current[pidx])) {
				seq_rows |= (1 << (BOARD_SIZE - i - 1)); //Include row which shift occurred in
				local_shift = true;
				b->current[pidx] += b->current[idx];
				b->current[idx] = 0;
			}
		}
		local_shift = false;
	}

	if (!seq_rows) //If seq_rows == 0, no rows have been shifted
		printf("!!!NO SHIFT!!!\n");
	else
		insert_sequence(b, seq_rows, seq_trn);

	print_board(b);
}

uint32_t tile_score(Tile t) {
	if (t == 1 || t == 2) {
		return 1;
	} else if (t > 2) {
		uint32_t log2 = 0;
		t /= 3;
		while (t >>= 1) log2++;
		return (uint32_t)powf(3, log2 + 1);
	}

	return 0;
}

void print_score(Board *b) {
	int i, score = 0;
	for (i = 0; i < BOARD_SPACE; i++) {
		score += tile_score(b->current[i]);
	}
	printf("%d\n", score);
}

int main(int argc, char *argv[]) {
	Board b = {0};
	char buf[BUFSIZ];

#ifdef _DEBUG
	atexit(stahp);
#endif

	if (argc != 2) {
		printf("Usage: %s initial_state\n", argv[0]);
		return 1;
	}

	if (!load_file(&b, argv[1])) {
		return 1;
	}

	print_board(&b);
	print_tiles(&b);

	printf("\nEnter 'q' to quit. Enter 'l', 'r', 'u', 'd' to move.\nMove: ");
	while (fgets(buf, BUFSIZ, stdin)) {
		if (tolower(*buf) == 'q') {
#ifdef _DEBUG
			if (buf[1] == '!')
				_exit(0);
#endif
			break;
		} else if (tolower(*buf) == 's') {
			print_score(&b);
			printf("Move: ");
		} else {
			int i = 0;
			while (buf[i]) {
				move(&b, buf[i++]);
				printf("\n");
			}
			print_score(&b);
			printf("Move: ");
		}
	}
	return 0;
}