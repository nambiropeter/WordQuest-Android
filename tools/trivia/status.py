#!/usr/bin/env python3
"""Prints per-tier counts and rewrites TRIVIA_PROGRESS.md (called automatically by add.py / rm.py)."""
import json, collections, datetime, os
A = '/Users/nambzzy/Developer/wordsearch_android/app/src/main/assets/content/trivia_%s.json'
THEMES = 'animals food geography history movies music science sports'.split()
TARGET = {'easy': 500, 'medium': 700, 'hard': 800}
PROG = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'TRIVIA_PROGRESS.md')

def table():
    rows = []
    for t in THEMES:
        c = collections.Counter(x['difficulty'] for x in json.load(open(A % t)))
        done = all(c[k] >= v for k, v in TARGET.items())
        rows.append(f"| {t} | {c['easy']}/500 | {c['medium']}/700 | {c['hard']}/800 | {'DONE' if done else 'in progress'} |")
    return '\n'.join(rows)

def write():
    body = open(PROG).read().split('<!-- STATUS -->')
    status = ('<!-- STATUS -->\n_Last updated: ' + datetime.datetime.now().strftime('%Y-%m-%d %H:%M') + '_\n\n'
              '| theme | easy | medium | hard | state |\n|---|---|---|---|---|\n' + table() + '\n<!-- STATUS -->')
    open(PROG, 'w').write(body[0] + status + body[2])

if __name__ == '__main__':
    write(); print(table())
