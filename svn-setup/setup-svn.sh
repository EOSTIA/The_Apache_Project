#!/bin/bash
# StreamSync SVN Repository Setup
# Run this ONCE before starting the Config Server
# Requires: svnadmin, svn (subversion package)
# Install: sudo apt-get install subversion   (Ubuntu)
#          sudo yum install subversion        (CentOS)

set -e

REPO_PATH="/tmp/streamsync-svn-repo"
WC_PATH="/tmp/streamsync-svn-wc"

echo "=== StreamSync SVN Setup ==="

# 1. Create SVN repository
if [ -d "$REPO_PATH" ]; then
  echo "[SKIP] Repository already exists at $REPO_PATH"
else
  echo "[CREATE] Initializing SVN repository at $REPO_PATH"
  svnadmin create "$REPO_PATH"
  echo "[OK] Repository created"
fi

# 2. Check out working copy
if [ -d "$WC_PATH" ]; then
  echo "[SKIP] Working copy already exists at $WC_PATH"
else
  echo "[CHECKOUT] Checking out working copy to $WC_PATH"
  svn checkout "file://$REPO_PATH" "$WC_PATH"
  echo "[OK] Working copy checked out"
fi

# 3. Create initial directory structure
cd "$WC_PATH"

for SERVICE in payments auth inventory; do
  if [ ! -d "$SERVICE" ]; then
    mkdir -p "$SERVICE"
    svn add "$SERVICE"
    echo "[ADD] Created directory: $SERVICE/"
  fi
done

# 4. Initial commit
svn commit -m "[streamsync][init] Initial repository structure for payments, auth, inventory" \
  --username streamsync --no-auth-cache 2>/dev/null || \
svn commit -m "[streamsync][init] Initial repository structure"

echo ""
echo "=== SVN Setup Complete ==="
echo "Repository: file://$REPO_PATH"
echo "Working copy: $WC_PATH"
echo ""
echo "Useful commands:"
echo "  svn log file://$REPO_PATH          — view all commits"
echo "  svn log $WC_PATH/payments/         — view payments config history"
echo "  svn diff -r 1:HEAD $WC_PATH        — diff from first commit to now"
