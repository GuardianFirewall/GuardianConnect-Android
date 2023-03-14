# GuardianConnect

A work-in-progress framework for Android applications written in Kotlin or Java to integrate with the Guardian Connect API and establish VPN connections to the Guardian Firewall infrastructure. All lower level components are exposed but the use of high level APIs in `GRDVPNHelper` are recommended.
This framework includes everything to establish a WireGuard connection through another dependency. Instructions on how to build and integrate all required components are forthcoming (see section: Sample Application)
_(The WireGuard library is not required to build this framework locally)_

For more information and a direct contact visit https://guardianapp.com/company/partners/


### Integration
A pre-built framework will be made available soon through Maven Central. For the time being it is required to build everything locally. Shell scripts to build the library are available in the `/script` directory.


### Sample Application
The project contains a sample application built to showcase & test functionality. It is also a reference implementation for the APIs provided by the GuardianConnect framework and can be run locally on any Android device
