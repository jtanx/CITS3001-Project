#include "threes.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

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

void move(Board *b, char *m) {
	switch (tolower(*m)) {
		case 'l': case 'r': case 'u': case 'd':
			break;
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