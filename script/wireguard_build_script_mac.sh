# /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

brew install tar
brew install unzip
#brew install lib32stdc++6
#brew install lib32z1

# Create a new directory at specified location
rm -rf SDK
mkdir SDK
cd SDK

# Create a new directory android-sdk-latest
# Next steps are necessary to remove "SDK location not found" error
mkdir android-sdk-latest
cd android-sdk-latest
export ANDROID_HOME=${PWD}
echo $ANDROID_HOME
export PATH=$PATH:${ANDROID_HOME}/tools/:${ANDROID_HOME}/platform-tools

mkdir cmdline-tools
cd cmdline-tools
# create a new directory: android-sdk-latest/cmdline-tools/latest
#mkdir latest
# Here we are installing androidSDK tools from official source,
# after that unzipping those tools and
# then running a series of SDK manager commands to install necessary android SDK packages that’ll allow the app to build
wget https://dl.google.com/android/repository/commandlinetools-mac-9123335_latest.zip
unzip commandlinetools-mac-9123335_latest.zip
mv cmdline-tools latest
pushd latest

#cd ../..
# then move all files to new created latest directory
#mv android-sdk-latest/cmdline-tools/* android-sdk-latest/cmdline-tools/latest
#pushd android-sdk-latest/cmdline-tools/latest
yes | bin/sdkmanager --licenses || true
sh bin/sdkmanager "build-tools;30.0.3"

# export ANDROID_HOME=“${PWD}/../../../”
# Clone and build app
sudo rm -rf wireguard-android/
git clone --recurse-submodules https://git.zx2c4.com/wireguard-android
cd wireguard-android

echo "sdk.dir=$ANDROID_HOME" > local.properties
chmod +x ./gradlew
./gradlew tunnel:assembleRelease
