#!/bin/bash
# Usage: ./set-api-url.sh https://your-ghostletter.vercel.app
if [ -z "$1" ]; then
  echo "Usage: $0 <vercel-url>"
  exit 1
fi
sed -i '' "s|https://your-ghostletter.vercel.app|$1|g" src/lib/api.ts
echo "✅ API URL updated to: $1"
