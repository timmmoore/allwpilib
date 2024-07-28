#!/usr/bin/env python3

import os
import sys
import argparse
from pathlib import Path

def main(argv):
    script_path = Path(__file__).resolve()
    dirname = script_path.parent

    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--output_directory",
        help="Optional. If set, will output the generated files to this directory, otherwise it will use a path relative to the script",
        default=dirname / "generated/gl3w",
        type=Path,
    )
    args = parser.parse_args(argv)
    
    sys.path.append(str(dirname / "gl3w"))

    args.output_directory.mkdir(parents=True, exist_ok=True)
    os.chdir(args.output_directory)

    import gl3w_gen


if __name__ == "__main__":
    main(sys.argv[1:])