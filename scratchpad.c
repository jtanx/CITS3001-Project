char mask = 0xF;

//An UP search (e.g top down)
for (int j = 0; j < BOARD_SIZE; j++) {
	int min = INT_MAX;
	for (int i = 0; i < BOARD_SIZE; i++) {
		if (mask & (1 << i)) {
			if (board[i][j] < min) {
				mask &= mask_off_lower_bits;
				min = board[i][j];
			} else if (board[i][j] != min) {
				mask ^= 1 << i;
			}
		}
	}
}

for (int j = BOARD_SIZE - 1; j >= 0 && !(mask & (1 << j)); j--);

//insert at the j'th row.

for (int i = 0; i < BOARD_SIZE; i++) {
	int min = INT_MAX;
	for (int j = 0; j < BOARD_SIZE; j++) {
		LEFT: No transform
		UP: Rotation 90 degrees
	}
}