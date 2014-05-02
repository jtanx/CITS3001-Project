#include "threes.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <math.h>

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

int load_file(Board *b, char *f) {
	ArrayBuilder *ab;
	FILE *fp;
	char buf[BUFSIZ], *tok, *next;
	int i,j;

	if(!(fp = fopen(f, "r"))) {
		perror("Failed to open the input file");
		return 0;
	}

	if (!ab_init(&ab, 0, sizeof(Tile))) {
		printf("Memory allocation failed");
		fclose(fp);
		return 0;
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
				return 0;
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
					return 0;
				} else if (!ab_add(ab, &v)) {
					printf("Memory allocation failure\n");
					fclose(fp);
					return 0;
				}
			}
			tok = strtok_r(NULL, " ", &next);
		}
	}

	fclose(fp);

	//Load up the tile sequence
	b->sequence = ab_finalise(&ab, &b->n_sequence);
	return 1;
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

bool slide_valid(Tile from, Tile to) {
	return !to || (from == 1  && to == 2) || (from == 2 && to == 1) || (from > 2 && from == to);
}

int log2(int v) {
	int r = 0;
	while (v >>= 1) r++;
	return r;
}

int tile_score(Tile t) {
	if (t == 1 || t == 2)
		return 1;
	else if (t > 2) {
		return (int)powf(3, log2(t/3) +1);
	}

	return 0;
}

void move(Board *b, char *m) {
	int k, i, vi, dir, row_score;
	int min_score = INT_MAX, min_index = -1;
	bool shifted = false, local_shift = false;
	unsigned char vec[2][BOARD_SPACE] = {
		{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15},
		{0,4,8,12,1,5,9,13,2,6,10,14,3,7,11,15}
	};

	switch (tolower(*m)) {
		case 'l': i = 1, vi = 0, dir = 1; break;
		case 'r': i = BOARD_SPACE - 2, vi = 0, dir = -1; break;
		case 'u': i = 1, vi = 1, dir = 1; break;
		case 'd': i = BOARD_SPACE - 2, vi = 1, dir = -1; break;
		default: return;
	}

	for (k = 1, row_score = 0; k < BOARD_SPACE; k++) {
		if (k % BOARD_SIZE == 0) {
			if (local_shift) {
				row_score += tile_score(b->current[vec[vi][i-dir]]);
				if (row_score <= min_score) {
					min_score = row_score;
					min_index = vec[vi][i-dir];
				}
			}
			local_shift = false;
			i += dir;
			k++;
			row_score = 0;
		}

		if (local_shift) {
			b->current[vec[vi][i-dir]] = b->current[vec[vi][i]];
			b->current[vec[vi][i]] = 0;
		} else if (slide_valid(b->current[vec[vi][i]], b->current[vec[vi][i-dir]])) {
			local_shift = true;
			shifted = true;
			b->current[vec[vi][i-dir]] += b->current[vec[vi][i]];
			b->current[vec[vi][i]] = 0;
		}

		row_score += tile_score(b->current[vec[vi][i-dir]]);

		i += dir;
	}
	if (!shifted)
		printf("!!!NO SHIFT!!!\n");
	else {
		b->current[min_index] = b->sequence[b->c_sequence];
		b->c_sequence = (b->c_sequence + 1) % b->n_sequence;
	}
	print_board(b);
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
			break;
		} else {
			move(&b, buf);
			printf("Move: ");
		}
	}
	return 0;
}