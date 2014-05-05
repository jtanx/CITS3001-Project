#ifndef _THREES_H
#define _THREES_H

#ifdef _WIN32 //MSVC compatibility
	#define PATH_SEPARATOR	'\\'
	#pragma warning (disable: 4996) //Disable 'unsafe function' warnings
	#include <Windows.h>
	#define bool BOOL
	#define false FALSE
	#define true TRUE
	#define strtok_r strtok_s
#else
	#define PATH_SEPARATOR '/'
	#include <stdbool.h>
#endif

#include <stdint.h>

typedef struct ArrayBuilder ArrayBuilder;

#define BOARD_SIZE 4
#define BOARD_SPACE (BOARD_SIZE * BOARD_SIZE)

typedef uint32_t Tile;

typedef struct Board {
	Tile it[BOARD_SPACE];
	size_t c_sequence;
	bool finished;
} Board;

typedef struct Sequence {
	Tile *it;
	size_t count;
} Sequence;

extern bool ab_init(ArrayBuilder **ab, size_t initialCount, size_t unitSize);
extern bool ab_add(ArrayBuilder *ab, void *data);
extern int ab_length(ArrayBuilder *ab);
extern void *ab_finalise(ArrayBuilder **ab, size_t *count);

#endif