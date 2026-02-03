#!/usr/bin/env python3

"""
Convert JFR recordings to Flame Graph format using jfr command and FlameGraph scripts.
"""

import subprocess
import sys
import os
from pathlib import Path

def convert_jfr_to_collapsed(jfr_file, output_file):
    """Convert JFR file to collapsed stack format suitable for FlameGraph."""
    print(f"Converting {jfr_file} to collapsed format...")
    
    # Use jfr print to extract execution samples
    cmd = [
        'jfr', 'print',
        '--events', 'jdk.ExecutionSample',
        '--stack-depth', '999',
        jfr_file
    ]
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        output = result.stdout
        
        # Parse the output to create collapsed stacks
        stacks = {}
        current_stack = []
        
        for line in output.split('\n'):
            line = line.strip()
            if not line or line.startswith('---') or line.startswith('jdk.ExecutionSample'):
                if current_stack:
                    # Build collapsed stack format: frame1;frame2;frame3 1
                    stack_str = ';'.join(reversed(current_stack))
                    stacks[stack_str] = stacks.get(stack_str, 0) + 1
                    current_stack = []
            elif line.startswith('- ') or line.startswith('stackTrace:'):
                # Skip these lines
                continue
            elif line and not line.startswith('='):
                # This is a stack frame
                # Clean up the frame name
                frame = line.strip()
                if frame and frame != 'null':
                    current_stack.append(frame)
        
        # Write collapsed stacks
        with open(output_file, 'w') as f:
            for stack, count in sorted(stacks.items(), key=lambda x: x[1], reverse=True):
                if stack:  # Only write non-empty stacks
                    f.write(f"{stack} {count}\n")
        
        print(f"  Created {output_file} with {len(stacks)} unique stacks")
        return True
        
    except subprocess.CalledProcessError as e:
        print(f"  Error running jfr command: {e}")
        print(f"  stdout: {e.stdout}")
        print(f"  stderr: {e.stderr}")
        return False

def generate_flamegraph(collapsed_file, svg_file, flamegraph_script):
    """Generate flame graph SVG from collapsed stacks."""
    print(f"Generating flame graph {svg_file}...")
    
    try:
        with open(collapsed_file, 'r') as input_file:
            result = subprocess.run(
                [flamegraph_script, '--title', 'CPU Profile'],
                stdin=input_file,
                capture_output=True,
                text=True,
                check=True
            )
            
            with open(svg_file, 'w') as output_file:
                output_file.write(result.stdout)
            
            print(f"  Created {svg_file}")
            return True
            
    except Exception as e:
        print(f"  Error generating flame graph: {e}")
        return False

def main():
    profiling_dir = Path("/home/runner/work/exposed-vertx-sql-client/exposed-vertx-sql-client/profiling-results")
    flamegraph_script = profiling_dir / "FlameGraph" / "flamegraph.pl"
    
    if not flamegraph_script.exists():
        print(f"Error: FlameGraph script not found at {flamegraph_script}")
        return 1
    
    jfr_files = [
        ("tfb-batch-update-database.jfr", "DatabaseExposedTransactionProvider"),
        ("tfb-batch-update-jdbc.jfr", "JdbcTransactionExposedTransactionProvider")
    ]
    
    for jfr_filename, name in jfr_files:
        jfr_file = profiling_dir / jfr_filename
        if not jfr_file.exists():
            print(f"Warning: {jfr_file} not found, skipping...")
            continue
        
        base_name = jfr_file.stem
        collapsed_file = profiling_dir / f"{base_name}_collapsed.txt"
        svg_file = profiling_dir / f"{base_name}_flamegraph.svg"
        
        print(f"\nProcessing {name}...")
        if convert_jfr_to_collapsed(str(jfr_file), str(collapsed_file)):
            generate_flamegraph(str(collapsed_file), str(svg_file), str(flamegraph_script))
    
    print("\nDone! Flame graphs generated:")
    for svg in profiling_dir.glob("*_flamegraph.svg"):
        print(f"  - {svg}")
    
    return 0

if __name__ == '__main__':
    sys.exit(main())
