#include "threes.h"

/************************ArrayBuilder************************/
#define RESIZE_FACTOR	2
struct ArrayBuilder {
	uint8_t *buffer;
	uint32_t nUsed, nTotal;
	size_t unitSize;
};

/*
 * Initialises an array builder. If the initial count is zero,
 * then an initial count of 10 is set.
 */
bool ab_init(ArrayBuilder **ab, size_t initialCount, size_t unitSize)
{
	ArrayBuilder *abt;
	if (ab == NULL || unitSize < 1)
		return false;
	else if ((abt = calloc(1, sizeof(ArrayBuilder))) == NULL)
		return false;

	if(initialCount < 1) //Give default size
		initialCount = 20;
	
	abt->buffer = malloc(initialCount * unitSize);
	if (abt->buffer != NULL) {
		abt->nTotal = initialCount;
		abt->unitSize = unitSize;
		*ab = abt;
		return true;
	}
	free(abt);
	return false;
}

/*
 * Adds an item to an array builder. Should offer amortised constant time
 * performance.
 */
bool ab_add(ArrayBuilder *ab, void *data)
{
	if (ab == NULL)
		return false;
	else if (ab->nUsed == ab->nTotal) //Must realloc
	{
		int nTotal = ab->nTotal * RESIZE_FACTOR;
		void *buffer = realloc(ab->buffer, nTotal * ab->unitSize);
		if (buffer == NULL)
			return false;
		ab->buffer = buffer;
		ab->nTotal = nTotal;
	}

	memcpy(ab->buffer + ab->nUsed * ab->unitSize, data, ab->unitSize);
	ab->nUsed++;
	return true;
}

/*
 * Retrieves the number of items currently in the array builder.
 */
int ab_length(ArrayBuilder *ab) {
	if (ab == NULL)
		return -1;
	return ab->nUsed;
}

/*
 * Finalises the array builder - the buffer is resized to the
 * exact size needed and returned to the caller. The associated
 * array builder structure is freed. The caller should free
 * the returned buffer.
 */
void *ab_finalise(ArrayBuilder **ab, size_t *count)
{
	void *buffer;
	if (ab == NULL || *ab == NULL || count == NULL)
		return NULL;
	
	*count = (*ab)->nUsed;
	//Realloc to exact size
	buffer = realloc((*ab)->buffer, (*ab)->nUsed * (*ab)->unitSize);
	if (buffer == NULL && (*ab)->nUsed > 0)
		buffer = (*ab)->buffer;
	free(*ab);
	*ab = NULL;

	return buffer;
}

/**********************End ArrayBuilder**********************/

/***************************Stack***************************/
struct Stack {
	void *item;
	struct Stack *next;
};

bool st_push(Stack **s, void *item) {
	Stack *n;
	if (!s)
		return false;

	if ((n = calloc(1, sizeof(Stack)))) {
		n->item = item;
		n->next = *s;
		*s = n;
		return true;
	}
	return false;
}

void* st_peek(Stack *s) {
	if (s) {
		return s->item;
	}
	return NULL;
}

void* st_pop(Stack **s) {
	Stack *t;
	void *item;
	if (!s && !*s)
		return NULL;

	t = *s;
	item = t->item;
	*s = t->next;

	free(t);
	return item;
}

/***************************End Stack***************************/