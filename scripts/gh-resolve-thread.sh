#!/bin/bash

# Resolve a GitHub PR review thread
# Usage: ./gh-resolve-thread.sh THREAD_ID

if [ $# -eq 0 ]; then
    echo "Usage: $0 THREAD_ID"
    echo "Example: $0 PRRT_kwDOQF23H85tQaFG"
    exit 1
fi

THREAD_ID=$1

gh api graphql -f query="
mutation {
  resolveReviewThread(input: {threadId: \"$THREAD_ID\"}) {
    thread { id isResolved }
  }
}"