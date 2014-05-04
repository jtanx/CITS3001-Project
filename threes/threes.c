#include "threes.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <math.h>

//Lookup table for traversing the grid on shifts
const unsigned char g_trn[4][BOARD_SPACE] = 
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
	int v = t/3;
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
			int v = strtol(tok, NULL, 10);
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
				int v = strtol(tok, NULL, 10);
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
	int i;
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

void move(Board *b, char m) {
	bool shifted = false, local_shift = false;
	bool shifted_rows[BOARD_SIZE] = {0};
	const unsigned char *trn;
	unsigned char i, j;

	switch (tolower(m)) {
		case 'l': trn = g_trn[0]; break;
		case 'u': trn = g_trn[1]; break;
		case 'r': trn = g_trn[2]; break;
		case 'd': trn = g_trn[3]; break;
		default: return;
	}

	for (i = 0; i < BOARD_SIZE; i++) {
		for (j = 1; j < BOARD_SIZE; j++) {
			unsigned char idx = trn[i * BOARD_SIZE + j];
			unsigned char pidx = trn[i * BOARD_SIZE + j-1];

			if (local_shift) {
				b->current[pidx] = b->current[idx];
				b->current[idx] = 0;
			} else if(shift_valid(b->current[idx], b->current[pidx])) {
				shifted_rows[idx/BOARD_SIZE] = true;
				local_shift = true;
				shifted = true;
				b->current[pidx] += b->current[idx];
				b->current[idx] = 0;
			}
		}
		local_shift = false;
	}

	/*for (k = 1; k < BOARD_SPACE; k++) {
		if (k % BOARD_SIZE == 0) {
			i += dir;
			k++;
			local_shift = false;
		}

		if (local_shift) {
			b->current[vec[vi][i-dir]] = b->current[vec[vi][i]];
			b->current[vec[vi][i]] = 0;
		} else if (shift_valid(b->current[vec[vi][i]], b->current[vec[vi][i-dir]])) {
			shifted_rows[k/BOARD_SIZE] = true;
			local_shift = true;
			shifted = true;
			b->current[vec[vi][i-dir]] += b->current[vec[vi][i]];
			b->current[vec[vi][i]] = 0;
		}

		i += dir;
	}*/
	if (!shifted)
		printf("!!!NO SHIFT!!!\n");

	print_board(b);
}

int tile_score(Tile t) {
	if (t == 1 || t == 2) {
		return 1;
	} else if (t > 2) {
		int log2 = 0;
		t /= 3;
		while (t >>= 1) log2++;
		return (int)powf(3, log2 + 1);
	}

	return 0;
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
		} else {
			int i = 0;
			while (buf[i]) {
				move(&b, buf[i++]);
				printf("\n");
			}
			printf("Move: ");
		}
	}
	return 0;
}