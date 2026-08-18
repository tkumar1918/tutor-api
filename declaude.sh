#!/usr/bin/env bash
# Remove Claude AI-attribution lines from ALL commit messages in the current repo.
# Run it from INSIDE each repo you want to clean. It rewrites history and force-pushes.
#
#   Usage:  cd ~/path/to/repo  &&  bash declaude.sh
#
# Only run on repos that are YOURS and have no other collaborators — rewriting
# history changes every commit hash after the edit.
set -euo pipefail

echo "== Commits that currently mention Claude =="
git log --all --grep="Claude" --oneline || true
echo
echo "== Commit authors in this repo (make sure these are YOU, not 'Claude') =="
git log --all --format='%an <%ae>' | sort -u
echo

read -rp "Strip the Claude lines from every commit message and force-push? [y/N] " ok
[[ "${ok:-}" == "y" || "${ok:-}" == "Y" ]] || { echo "Aborted — nothing changed."; exit 0; }

FILTER_BRANCH_SQUELCH_WARNING=1 git filter-branch --force --msg-filter \
  'sed -e "/Co-Authored-By: Claude/d" \
       -e "/Generated with .*Claude Code/d" \
       -e "/^🤖/d"' \
  -- --all

echo
echo "Rewrite done locally. Pushing..."
git push --force --all
git push --force --tags 2>/dev/null || true

echo
echo "Cleaned. Verify with:  git log --pretty=full | grep -i claude   (should print nothing)"
