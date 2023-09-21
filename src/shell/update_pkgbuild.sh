#!/usr/bin/env sh
# This program updates the PKGBUILD file based on the current version of the program
# Requires pacman-contrib (updpkgsums,  makepkg) commands to update the md5sum
# This file expects itself to be two directories below the PKGBUILD file
DIR="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)"
SOURCE="$DIR/../.."

# Find current program version
OLD_VERSION="$(grep -oP "(?<=pkgver=')[^']+" "$SOURCE/PKGBUILD")" || exit 1
echo "Found existing $OLD_VERSION PKGBUILD"

NEW_VERSION="$(grep -oP '(?<=version>)[^<]+' "$SOURCE/pom.xml" | head -n 1)" || exit 1
VERSION="$(printf '%s' "$NEW_VERSION" | tr '-' '_')" || exit 1

# Do not run if already up to date
if [ "$OLD_VERSION" = "$VERSION" ] ; then
  echo "PKGBUILD is up to date" && exit 0
fi

# Replace in PKGBUILD
sed -i "s/pkgver='$OLD_VERSION'/pkgver='$VERSION'/" "$SOURCE/PKGBUILD" || exit 1

(cd "$SOURCE" && updpkgsums) || exit 1
(cd "$SOURCE" && makepkg --printsrcinfo -p "$SOURCE/PKGBUILD" > "$SOURCE/.SRCINFO") || exit 1
rm "$SOURCE/EzASM-$NEW_VERSION-full.jar"

AUR="$SOURCE/../ezasm-aur"
if [ -d "$AUR" ]; then
  cp "$SOURCE/PKGBUILD" "$AUR/"
  mv "$SOURCE/.SRCINFO" "$AUR/"
  (cd "$AUR" && git commit -am "Version bump to $NEW_VERSION" && git push) || (echo "Failed to commit/push to AUR" && exit 1)
fi

echo "PKGBUILD updated from $OLD_VERSION to $NEW_VERSION"

