#!/usr/bin/env python3
"""Starts the TUI in a real terminal and checks that it works there.

The native binary is what users run, so it gets a real pseudo console (a pty on Unix,
ConPTY on Windows) instead of the redirection CI normally hands a program. Two things
are checked, one definitive word each: the board is drawn, and JLine did not fall back
to its dumb terminal - which is what happens when the native terminal provider cannot
load, the failure mode behind jline3#1012 on Windows.

Usage:
    scripts/tui-smoke-test.py <binary> [--keys q] [--startup-seconds 5] [--exit-seconds 6]
"""
import argparse
import os
import sys
import time

BOARD_WORD = "Console"  # the title line, drawn once the board is rendered
DUMB_WORD = "dumb"  # JLine logs this when no native terminal provider worked
RESTORED_WORD = "1049l"  # the escape that leaves the alternate screen, written on the way out


def as_text(data):
    return data.decode("utf-8", "replace") if isinstance(data, bytes) else data


class UnixTerminal:
    """A pty, which is what a terminal emulator gives the program on Unix."""

    def __init__(self, command):
        import pty

        self.output = ""
        self.pid, self.fd = pty.fork()
        if self.pid == 0:
            os.execvp(command[0], command)

    def read(self, seconds):
        import select

        deadline = time.time() + seconds
        while time.time() < deadline:
            readable = select.select([self.fd], [], [], 0.2)[0]
            if not readable:
                continue
            try:
                data = os.read(self.fd, 65536)
            except OSError:
                break  # the child closed the terminal
            if not data:
                break
            self.output += as_text(data)
        return self.output

    def write(self, keys):
        os.write(self.fd, keys.encode())

    def alive(self):
        try:
            return os.waitpid(self.pid, os.WNOHANG)[0] == 0
        except ChildProcessError:
            return False  # already reaped, so it has exited

    def terminate(self):
        if self.alive():
            os.kill(self.pid, 9)
        try:
            os.waitpid(self.pid, 0)
        except ChildProcessError:
            pass
        os.close(self.fd)


class ConPtyTerminal:
    """A ConPTY, the Windows equivalent, driven by pywinpty."""

    def __init__(self, command):
        import threading

        from winpty import PtyProcess

        self.output = ""
        self.process = PtyProcess.spawn(command)
        self.reader = threading.Thread(target=self._collect, daemon=True)
        self.reader.start()

    def _collect(self):
        while True:
            try:
                data = self.process.read(4096)
            except (EOFError, OSError):
                return
            if not data:
                return
            self.output += as_text(data)

    def read(self, seconds):
        deadline = time.time() + seconds
        while time.time() < deadline and self.process.isalive():
            time.sleep(0.2)
        return self.output

    def write(self, keys):
        self.process.write(keys)

    def alive(self):
        return self.process.isalive()

    def terminate(self):
        if self.alive():
            self.process.terminate(force=True)


def start(binary):
    if os.name == "nt":
        return ConPtyTerminal([binary, "--debug"])
    return UnixTerminal([binary, "--debug"])


def main():
    parser = argparse.ArgumentParser(description="Run the TUI in a real terminal.")
    parser.add_argument("binary")
    parser.add_argument("--keys", default="q", help="sent once the TUI has started")
    parser.add_argument("--startup-seconds", type=int, default=5)
    parser.add_argument("--exit-seconds", type=int, default=8)
    args = parser.parse_args()

    terminal = start(args.binary)
    terminal.read(args.startup_seconds)
    terminal.write(args.keys)
    output = terminal.read(args.exit_seconds)
    if terminal.alive():
        # a keystroke can be lost in a terminal that was just created; press it once more
        terminal.write(args.keys)
        output = terminal.read(args.exit_seconds)
    still_running = terminal.alive()
    terminal.terminate()

    problems = []
    if BOARD_WORD.lower() not in output.lower():
        problems.append("the board was never drawn (no {!r})".format(BOARD_WORD))
    if DUMB_WORD in output.lower():
        problems.append("JLine fell back to its dumb terminal")
    if still_running:
        problems.append("the TUI was still running after {!r} (terminal restored: {})".format(
            args.keys, RESTORED_WORD in output))

    if problems:
        print("smoke test FAILED for " + args.binary, file=sys.stderr)
        for problem in problems:
            print("  - " + problem, file=sys.stderr)
        print("--- output ---\n" + output, file=sys.stderr)
        return 1

    print("smoke test passed: " + args.binary + " drew its board in a real terminal")
    return 0


if __name__ == "__main__":
    sys.exit(main())
