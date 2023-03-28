sudo apt-get --quiet update --yes
sudo apt-get --quiet install --yes wget tar unzip lib32stdc++6 lib32z1

# Create a new directory at specified location
sudo rm -rf SDK
mkdir SDK
cd SDK

# Create a new directory android-sdk-latest
# Next steps are necessary to remove "SDK location not found" error
mkdir android-sdk-latest
cd android-sdk-latest

export ANDROID_HOME=${PWD}
echo $ANDROID_HOME
export PATH=${PATH}:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
export PATH="/usr/bin:$PATH"


mkdir cmdline-tools
cd cmdline-tools

# Here we are installing androidSDK tools from official source,
# after that unzipping those tools and
# then running a series of SDK manager commands to install necessary android SDK packages that’ll allow the app to build
wget https://dl.google.com/android/repository/commandlinetools-linux-7583922_latest.zip
unzip commandlinetools-linux-7583922_latest.zip

mv cmdline-tools latest
cd latest/bin

yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses || true
sh sdkmanager "build-tools;30.0.3"

cd ../../../..

# Clone and build app
sudo rm -rf wireguard-android/
git clone --recurse-submodules https://git.zx2c4.com/wireguard-android
cd wireguard-android

echo "sdk.dir=$ANDROID_HOME" > local.properties

sudo chmod +x ./gradlew
sudo ./gradlew tunnel:assembleRelease
