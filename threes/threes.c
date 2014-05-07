#include "threes.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <math.h>

#define IS_POWER_2(v) ((v) && !((v) & ((v) - 1) ))

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
	//Is 0, 1, 2 or a multiple of 3 that is also a power of 2.
	Tile v = t/3;
	return t <= 2 || (!(t % 3) && IS_POWER_2(v));
}

bool load_file(Board *b, Sequence *s, char *f) {
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
			b->it[i * BOARD_SIZE + j] = v;
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
	s->it = ab_finalise(&ab, &s->count);
	if (!s->it) {
		printf("Memory allocation failure\n");
		return false;
	}
	return true;
}

void print_board(Board *b) {
	uint8_t i;
	for (i = 0; i < BOARD_SPACE; i++) {
		printf("%3d ", b->it[i]);
		if ((i+1) % BOARD_SIZE == 0)
			printf("\n");
	}
}

void print_tiles(Sequence *s) {
	size_t i;
	for (i = 0; i < s->count; i++)
		printf("%d ", s->it[i]);
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
void insert_sequence(Board *b, Sequence *s, uint8_t seq_rows, const uint8_t *seq_trn) {
	uint8_t i, j;

	//If seq_rows is a power of 2, then only one row left -> sub into that row immediately.
	for (i = 0; i < BOARD_SIZE && !IS_POWER_2(seq_rows); i++) {
		Tile min_value = UINT_MAX;
		for (j = 0; j < BOARD_SIZE; j++) {
			if (seq_rows & (1 << j)) { //Is a row that sequence can be inserted into
				uint8_t idx = seq_trn[i * BOARD_SIZE + j];
				if (b->it[idx] < min_value) {
					min_value = b->it[idx];
					seq_rows &= ~((1 << j) - 1); //All rows previous are out of the running
				} else if (b->it[idx] > min_value) {
					seq_rows ^= 1 << j; //This row is no longer in the running
				}
			}
		}
	}

	//Seems to work... May have to check for the 'most clockwise' rule
	j = 0;
	while (seq_rows >>= 1) j++;

	b->it[seq_trn[j]] = s->it[b->c_sequence++];
}

/**
 * Takes 2 * n^2 time in the worst case, where n is the width of the board.
 */
bool move(Board *b, Sequence *s, char m) {
	bool local_shift = false;
	const uint8_t *trn, *seq_trn;
	uint8_t i, j, seq_rows = 0;
	//seq_rows is a bitmask for rows that should be considered for sequence insert

	if (b->finished) {
		return false; //hurr durr
	}

	switch (tolower(m)) {
		case 'l': trn = g_trn[0], seq_trn = g_trn[3]; break;
		case 'u': trn = g_trn[1], seq_trn = g_trn[2]; break;
		case 'r': trn = g_trn[2], seq_trn = g_trn[1]; break;
		case 'd': trn = g_trn[3], seq_trn = g_trn[0]; break;
		default: return false;
	}

	for (i = 0; i < BOARD_SIZE; i++) {
		for (j = 1; j < BOARD_SIZE; j++) {
			uint8_t idx = trn[i * BOARD_SIZE + j];
			uint8_t pidx = trn[i * BOARD_SIZE + j-1];

			if (local_shift) {
				b->it[pidx] = b->it[idx];
				b->it[idx] = 0;
			} else if(shift_valid(b->it[idx], b->it[pidx])) {
				seq_rows |= (1 << (BOARD_SIZE - i - 1)); //Include row which shift occurred in
				local_shift = true;
				b->it[pidx] += b->it[idx];
				b->it[idx] = 0;
			}
		}
		local_shift = false;
	}

	if (!seq_rows) { //If seq_rows == 0, no rows have been shifted
		//printf("No change!\n");
		b->finished = true;
		return false;
	} else {
		insert_sequence(b, s, seq_rows, seq_trn);
		if (b->c_sequence == s->count) {
			b->finished = true; //May have to move this check to after to match the case of checking if seq_rows is 0.
		}
	}
	return true;
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

Board *board_dup(Board *b) {
	Board *n = malloc(sizeof(Board));
	memcpy(n, b, sizeof(Board));
	return n;
}

uint32_t board_score(Board *b) {
	uint32_t i, score;
	for (i = 0, score = 0; i < BOARD_SPACE; i++) {
		score += tile_score(b->it[i]);
	}
	return score;
}

uint32_t utility(Board *b, Sequence *s) {
	uint32_t score = board_score(b);
	uint32_t consecutive = 0;
	uint8_t i, j;
	uint8_t nnzc[4] = {0};
	uint8_t nnz = 0;

	bool nearing_final = ((100 * b->c_sequence) / s->count) > 95;
	bool halfway = ((100 * b->c_sequence) / s->count) > 70;

	for (i = 0; i < BOARD_SIZE-1; i++) {
		uint8_t nnzr = 0;
		for (j = 0; j < BOARD_SIZE-1; j++) {
			uint32_t c = b->it[i*BOARD_SIZE+j];
			uint32_t r = b->it[i*BOARD_SIZE+j+1];
			uint32_t d = b->it[(i+1)*BOARD_SIZE+j];
			uint32_t dg = b->it[(i+1)*BOARD_SIZE+j+1];

			nnz += c > 0;
			nnzc[j] += c > 0;
			
			consecutive += (c > 2) * ((c == r) + (c == d) + (c == dg)) * 2; //Can combine tiles
			consecutive -= (c == 1) * ((c == r) + (c == d) + (c == dg)) * 1;
			consecutive += (c > 3 && c < 24 || (c >= 24 && nearing_final)) * 4;
			consecutive -= (c > 2) && ( (r < 3) + (d < 3) + (dg < 3)) * 3;
			//consecutive += ((c == 2) * ((r == 1) + (d == 1)) + (c == 1) * ((r == 2) + (d == 2)));
			
			nnzr += c > 0;
		}
		nnzr += (b->it[i*BOARD_SIZE + j] > 0);
		nnzc[j] += (b->it[i*BOARD_SIZE + j] > 0);
		consecutive -= !halfway * (nnzr == 4);
		//if (nnzr == 4) printf("JA\n");
	}

	for (i =0; i < 4; i++) {
		nnzc[i] += b->it[(BOARD_SIZE-1)*(BOARD_SIZE)+i] > 0;
		consecutive -= !halfway * (nnzc[i] == 4);
		//if (nnzc[i] == 4) printf("JAJA\n");
	}

	//printf("%d\n", consecutive);
	return score+consecutive;
}

void print_score(Board *b) {
	printf("%d\n", board_score(b));
}

#define DEPTH_LIMIT 7

Board *solve_idfs(Board *initial, Sequence *s) {
	int j;
	Board *best = board_dup(initial);
	uint32_t best_score = utility(best, s);

	while (!best || !best->finished) {
		Stack *t = NULL;
		if (best) {
			best->depth = 0;
		}
		
		st_push(&t, board_dup(best));
		while (t != NULL) {
			Board *b = st_pop(&t);
			Board *next[4];
			const char *directions = "lurd"; 
			
			if (b->depth + 1 < DEPTH_LIMIT) {
				for (j = 0; j < 4; j++) {
					next[j] = board_dup(b);
					move(next[j], s, directions[j]);

					if (next[j]->finished) {
						//check score
						uint32_t score = utility(next[j], s);
						if (!best || score >= best_score) {
							if (best)
								free(best); //yuck
							best_score = score;
							best = next[j];
						} else {
							free(next[j]);
						}
					} else {
						next[j]->depth++;
						st_push(&t, next[j]);
					}
				}
				free(b);
			} else {
				uint32_t score = utility(b,s);
				if (!best || score >= best_score) {
					if (best)
						free(best);
					best_score = score;
					best = b;
				} else {
					free(b);
				}
			}
		}
		
	}

	print_board(best);
	print_score(best);
	return best;
}


int main(int argc, char *argv[]) {
	Board b = {0}, *cur;
	Sequence s = {0};
	char buf[BUFSIZ];

#ifdef _DEBUG
	atexit(stahp);
#endif

	if (argc != 2) {
		printf("Usage: %s initial_state\n", argv[0]);
		return 1;
	}

	if (!load_file(&b, &s, argv[1])) {
		return 1;
	}

	print_board(&b);
	print_tiles(&s);

	cur = solve_idfs(&b, &s);

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
			while (buf[i] && !b.finished) {
				move(&b, &s, buf[i++]);
				print_board(&b);
				printf("\n");
			}
			if (b.finished) {
				printf("Finished!\n");
				print_score(&b);
				break;
			} else {
				printf("Move: ");
			}
		}
	}

	free(s.it);
	return 0;
}