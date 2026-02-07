#!/bin/bash

# Get review threads for jmix-crm PR
# Usage: ./gh-get-review-threads.sh [PR_NUMBER]

PR_NUMBER=${1:-3}

gh api graphql -f query="
{
  repository(owner: \"jmix-framework\", name: \"jmix-crm\") {
    pullRequest(number: $PR_NUMBER) {
      reviewThreads(first: 100) {
        nodes {
          id
          isResolved
          comments(first: 100) {
            nodes { id body author { login } path line }
          }
        }
      }
    }
  }
}"