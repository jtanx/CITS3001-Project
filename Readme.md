# An AI for a modified version of *Threes!*

This does not play the actual version of *Threes!*, but a modified version
that is completely deterministic.

## Usage
Stolen from the report~ (converted with (pandoc)[http://johnmacfarlane.net/pandoc/try/])

On most platforms, the usual way to call the program is with:

	java -jar threes.jar [options] input_file.txt

On Windows, a convenience launcher exists, such that it can be called
by:

	Threes.exe [options] input_file.txt

The output moves file is always written to `stdout`. All other messages
should be written to `stderr`.

