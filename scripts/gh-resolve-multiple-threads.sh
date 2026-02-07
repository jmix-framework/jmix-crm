#!/bin/bash

# Resolve multiple GitHub PR review threads
# Usage: ./gh-resolve-multiple-threads.sh THREAD_ID1 THREAD_ID2 THREAD_ID3 ...

if [ $# -eq 0 ]; then
    echo "Usage: $0 THREAD_ID1 THREAD_ID2 THREAD_ID3 ..."
    echo "Example: $0 PRRT_kwDOQF23H85tQaFG PRRT_kwDOQF23H85tQaSj"
    exit 1
fi

for THREAD_ID in "$@"; do
    echo "Resolving thread: $THREAD_ID"
    gh api graphql -f query="
    mutation {
      resolveReviewThread(input: {threadId: \"$THREAD_ID\"}) {
        thread { id isResolved }
      }
    }"
    echo "Thread $THREAD_ID resolved"
    echo "---"
done