
RELEASE=`sed -n 's/^VERSION_NAME=\(.*\)/\1/p' < gradle.properties`


echo $RELEASE

find ./build/central-package -name '*.sha512' -delete &&
find ./build/central-package -name '*.sha256' -delete &&
find ./build/central-package -name '*metadata*' -delete &&
find ./build/central-package -name '*.asc.*' -delete

tar -cvzf release-$RELEASE.tar.gz -C build/central-package io